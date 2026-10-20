// PLA: ADR-023 modid — PLAN-001 T1. Guards the rename of the four mods to plaa_* (docs/plans/PLAN-001-b2-modid-rename.md §6).
import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import test from "node:test";

const ROOT = new URL("../../", import.meta.url);
const MODS = ["core", "vanilla", "create", "modulargolems"];
const OURS = new Set(["plaa_core", "plaa_vanilla", "plaa_create", "plaa_modulargolems"]);
const UPSTREAM = ["folkways_core", "folkways_vanilla", "folkways_create", "folkways_modulargolems"];
const UPSTREAM_ID = /folkways_(core|vanilla|create|modulargolems)\b/g;
const SKIP = /(^|\/)(build|node_modules|\.gradle|\.blockwright|\.blockwright-store|run)(\/|$)/;

function read(path: string): string {
  return new TextDecoder("utf-8", { fatal: true }).decode(readFileSync(new URL(path, ROOT)));
}

// Recursive readdir also lists directories, so keep regular files only.
function walk(dir: string, keep: (name: string) => boolean): string[] {
  return readdirSync(new URL(dir, ROOT), { recursive: true, encoding: "utf8" })
    .filter(name => keep(name) && !SKIP.test(name))
    .map(name => `${dir}${name}`)
    .filter(path => statSync(new URL(path, ROOT)).isFile());
}

// Split into tables at each "[[" header and keep the [[dependencies.*]] ones; no TOML library needed.
function dependencySections(toml: string): { modId: string; type: string }[] {
  return toml.split(/^(?=\[\[)/m)
    .filter(table => /^\[\[dependencies\./.test(table))
    .map(body => ({
      modId: body.match(/^modId\s*=\s*"([^"]+)"/m)?.[1] ?? "",
      type: body.match(/^type\s*=\s*"([^"]+)"/m)?.[1] ?? "",
    }));
}

test("each Folkways mod project declares a plaa_* modid", () => {
  for (const mod of MODS) {
    const gradle = read(`mods/${mod}/build.gradle`);
    const id = gradle.match(/^ext\.modId\s*=\s*"([^"]+)"/m)?.[1];
    assert.ok(id && OURS.has(id), `mods/${mod}/build.gradle: ext.modId is ${JSON.stringify(id)}, expected one of ${[...OURS].join(", ")}`);
    const name = gradle.match(/^ext\.modName\s*=\s*"([^"]+)"/m)?.[1];
    assert.equal(name, id.replace("_", "-"), `mods/${mod}/build.gradle: ext.modName must mirror ext.modId`);
  }
});

test("mods.toml dependencies point at plaa_* and declare the upstream Folkways mods incompatible", () => {
  for (const mod of MODS) {
    const path = `mods/${mod}/src/main/resources/META-INF/neoforge.mods.toml`;
    const sections = dependencySections(read(path));
    assert.ok(sections.length > 0, `${path}: no [[dependencies.*]] sections parsed`);
    for (const { modId, type } of sections) {
      if (type === "incompatible") continue;
      assert.ok(!/^folkways_(core|vanilla|create|modulargolems)$/.test(modId),
        `${path}: non-incompatible dependency still names upstream modid ${modId}`);
    }
    const incompatible = sections.filter(section => section.type === "incompatible").map(section => section.modId).sort();
    assert.deepEqual(incompatible, [...UPSTREAM].sort(), `${path}: must declare exactly the four upstream Folkways mods incompatible`);
  }
});

test("no upstream modid literal survives outside mods.toml, NOTICE and the contributor docs", () => {
  const files = [
    ...walk("mods/", name => name.endsWith(".java")),
    ...walk("", name => name.endsWith(".gradle")),
    "blockwright.toml",
    ...walk("tests/shared/", name => name.endsWith(".ts")),
  ];
  const offenders: string[] = [];
  for (const file of files) {
    const text = read(file);
    for (const hit of text.matchAll(UPSTREAM_ID)) {
      const line = text.slice(0, hit.index).split("\n").length;
      offenders.push(`${file}:${line} ${hit[0]}`);
    }
  }
  assert.deepEqual(offenders, [], `upstream modid literals remain:\n${offenders.join("\n")}`);
});
