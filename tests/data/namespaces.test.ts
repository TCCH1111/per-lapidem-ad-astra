import assert from "node:assert/strict";
import { readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";
import test from "node:test";

/**
 * A data pack directory under data/<namespace>/ is addressed by that namespace, and for our own
 * shelves the namespace is the mod id: the pattern book reads data/<mod id>/folkways/pattern/.
 *
 * <p>The port once shipped patterns under data/folkways_chiselsandbits/ while the code asked for
 * plaa_chiselsandbits, so seven carving patterns silently did not exist in game, and the local suite
 * stayed green because those tests are behind a property the default run does not set.
 *
 * <p>The rule here is deliberately narrow: a namespace that looks like ours must be either the shared
 * one or exactly the module's mod id. Third-party namespaces were not ours to constrain, and a rule
 * that guessed at them would fail on the day a module legitimately ships data for another mod.
 */

const ROOT = process.cwd();
const SHARED = new Set(["folkways"]);

function modules(): { name: string; modId: string }[] {
    const modsDir = join(ROOT, "mods");
    const found: { name: string; modId: string }[] = [];
    for (const entry of readdirSync(modsDir)) {
        const build = join(modsDir, entry, "build.gradle");
        let text: string;
        try {
            text = readFileSync(build, "utf8");
        } catch {
            continue;
        }
        const match = text.match(/ext\.modId\s*=\s*"([^"]+)"/);
        if (match) found.push({ name: entry, modId: match[1] });
    }
    return found;
}

function namespacesOf(module: string): string[] {
    const dataDir = join(ROOT, "mods", module, "src", "main", "resources", "data");
    let entries: string[];
    try {
        entries = readdirSync(dataDir);
    } catch {
        return [];
    }
    return entries.filter((one) => statSync(join(dataDir, one)).isDirectory());
}

test("our data namespaces are either shared or the module's own mod id", () => {
    const all = modules();
    const violations: string[] = [];
    let checked = 0;

    for (const module of all) {
        for (const namespace of namespacesOf(module.name)) {
            if (!/^(folkways|plaa)/.test(namespace)) {
                continue; // 第三方命名空间（create / minecraft / c …）不属本条规则
            }
            checked++;
            if (SHARED.has(namespace) || namespace === module.modId) {
                continue;
            }
            violations.push(`${module.name}: data/${namespace} (expected ${module.modId} or folkways)`);
        }
    }

    assert.deepEqual(violations, []);
    assert.ok(checked >= 4, `only ${checked} own namespaces were checked, which is too few to trust`);
});

test("every module declares a mod id that matches the plaa convention", () => {
    const all = modules();
    assert.ok(all.length >= 10, `expected at least ten modules, found ${all.length}`);
    for (const module of all) {
        assert.match(module.modId, /^plaa_/, `${module.name} should use a plaa_* id, got ${module.modId}`);
    }
});
