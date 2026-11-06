import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

/**
 * Restored gate (D18): every enum that implements NoticeKind or RefusalKind.Named must derive its keys
 * from the constant name, and every derived key must exist in every shipped language.
 *
 * <p>The port lost this check when upstream deleted the file that carried it, and the next two notices we
 * added were both written against a convention nobody was enforcing. It now checks all four catalogs
 * rather than English alone, because a key that exists only in English is still a broken line for most
 * players.
 */

const ROOT = process.cwd();
const LOCALES = ["en_us", "zh_cn", "zh_hk", "zh_tw"];
const catalogs = LOCALES.map(locale =>
  JSON.parse(readFileSync(join(ROOT, `mods/core/src/main/resources/assets/folkways/lang/${locale}.json`), "utf8")));

function javaSources(dir: string, out: string[] = []): string[] {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) javaSources(full, out);
    else if (entry.endsWith(".java")) out.push(full);
  }
  return out;
}

interface Declared {
  file: string;
  name: string;
  prefix: string | null;
  constants: string[];
}

function declared(): Declared[] {
  const found: Declared[] = [];
  for (const file of javaSources(join(ROOT, "mods")).filter(path => path.includes("/src/main/"))) {
    const text = readFileSync(file, "utf8");
    for (const match of text.matchAll(/\benum\s+(\w+)\s+implements\s+([^{]+)\{([\s\S]*?);/g)) {
      const impls = match[2].split(",").map(one => one.trim());
      if (!impls.some(one => one === "NoticeKind" || one === "RefusalKind.Named" || one === "Named")) {
        continue;
      }
      const body = text.slice(match.index! + match[0].length, match.index! + match[0].length + 1200);
      let prefix: string | null = null;
      if (impls.includes("NoticeKind")) {
        const rule = body.match(/return\s+"([\w.]+)"\s*\+\s*name\(\)\.toLowerCase\(Locale\.ROOT\)/);
        prefix = rule ? rule[1] : null;
      } else {
        prefix = `folkways.refusal.${match[1].replace(/Refusal$/, "").toLowerCase()}.`;
      }
      found.push({
        file: file.replace(ROOT + "/", ""),
        name: match[1],
        prefix,
        constants: [...match[3].matchAll(/^\s*([A-Z][A-Z_0-9]*)\b/gm)].map(one => one[1]),
      });
    }
  }
  return found;
}

test("notice and refusal enums derive their keys from the constant name", () => {
  const withoutRule = declared().filter(one => one.prefix === null);
  assert.deepEqual(
    withoutRule.map(one => `${one.file}: ${one.name}`),
    [],
    "an enum that spells its own keys cannot be checked; use \"prefix\" + name().toLowerCase(Locale.ROOT)",
  );
});

test("every declared notice value has words in every shipped language", () => {
  let checked = 0;
  const missing: string[] = [];
  for (const one of declared()) {
    if (!one.prefix) continue;
    for (const constant of one.constants) {
      const key = one.prefix + constant.toLowerCase();
      checked++;
      LOCALES.forEach((locale, index) => {
        if (!Object.hasOwn(catalogs[index], key)) {
          missing.push(`${locale}: ${key} (${one.file})`);
        }
      });
    }
  }
  assert.deepEqual(missing, []);
  assert.ok(checked >= 100, `only ${checked} notice keys were checked, which is too few to trust`);
});
