# Contributing to Folkways

English · [简体中文](CONTRIBUTING.zh-CN.md)

Bug reports, documentation, translations, building patterns and code contributions
are welcome at [IzakyL/folkways](https://github.com/IzakyL/folkways).
For installing and playing the mod, start with the [README](README.md).

[Report a bug](#reporting-bugs) · [Set up](#development-setup) ·
[Run checks](#validation) · [Open a pull request](#pull-requests) ·
[Conventions](#conventions)

## Reporting bugs

Use the [bug report form](https://github.com/IzakyL/folkways/issues/new?template=bug_report.yml)
and include what you did, what you expected, what happened, and reproduction steps.
List Minecraft, NeoForge and LDLib2 versions, every installed Folkways jar and
its version, and your other mods. Attach `logs/latest.log` and the crash report
from `crash-reports/` if the game crashed.

For new capabilities, use the
[feature request form](https://github.com/IzakyL/folkways/issues/new?template=feature_request.yml)
and explain the expected behavior, use case and alternatives you considered.

## Development setup

| Tool or platform | Requirement                                                  |
| ---------------- | ------------------------------------------------------------ |
| Java             | JDK 21                                                       |
| Node.js          | 22.14 or newer                                               |
| Gradle           | Checked-in wrapper: `./gradlew`, or `gradlew.bat` on Windows |
| Blockwright SDK  | Linux x64 via npm; Windows 11 x64 via a source-built SDK     |

[Blockwright](https://github.com/IzakyL/blockwright) has native Windows support in
its source tree. This repository pins npm version `0.1.0-alpha.1`, published
before that implementation; it declares Linux x64 only, so `npm ci` rejects it on
Windows with `EBADPLATFORM`. This repository's CI uses Ubuntu 24.04. Run the
published-package setup on Linux x64 from the repository root:

```sh
npm ci
npm run setup
./gradlew build
```

<<<<<<< ours
`npm ci` installs the locked test dependencies. `npm run setup` downloads fixtures
pinned in [`blockwright.toml`](blockwright.toml) and writes `.blockwright/mods.json`.
**Run setup before the first Gradle invocation**; rerun it after changing pinned
fixtures. Gradle compiles against optional integration jars even when a particular
test profile does not load them.

The build gathers the nine mod jars in `build/libs/` and runtime LDLib2 in `build/deps/`.
Each mod is a Gradle project under `mods/`; see [Mods](README.md#mods) for what each one does.

For Windows, prepare a compatible source-built SDK using Blockwright's
[local build instructions](https://github.com/IzakyL/blockwright/blob/master/CONTRIBUTING.md)
and [Windows requirements](https://github.com/IzakyL/blockwright/blob/master/docs/environment.md#windows-development-candidate).
Client tests require a logged-in Windows 11 x64 desktop with an OpenGL-capable GPU;
use display device `auto` or `gpu`. After preparing `.blockwright/mods.json` with
that SDK, build with `gradlew.bat`. Record the SDK revision and platform when
reporting results; local Windows tests and Linux CI are separate validation runs.

## Validation

Start with checks that do not launch Minecraft:
=======
`npm run setup` must run before the first Gradle build. The build compiles against the
optional integrations (Create, JEI, EMI, GeckoLib, Modular Golems, Sable, FTB Teams) from the jars it
downloads, as pinned in `blockwright.toml`.

## Layout

Folkways is four mods, each a Gradle project under `mods/`, all registering under the `folkways` namespace (in this fork their modids are `plaa_*`, see NOTICE.md).
`./gradlew build` builds them and gathers their jars into `build/libs/`.

| Mod | Project | What it is |
| --- | --- | --- |
| Folkways Core (`plaa_core`) | `mods/core` | The engine and the screens; on its own it gives residents no work |
| Folkways Vanilla (`plaa_vanilla`) | `mods/vanilla` | The work residents do among vanilla blocks |
| Folkways × Create (`plaa_create`) | `mods/create` | What Create adds: rail, dispatch, and Create's side of building (track joints, schematic hand-off) |
| Folkways × Modular Golems (`plaa_modulargolems`) | `mods/modulargolems` | Modular Golems as a second sort of resident |

An integration whose absence would leave the colony misreading a world that has that mod (Sable's sub-levels,
Create's contraptions in the way) belongs in the core; one that only adds something when its mod is there is a mod
of its own.

| Path | What lives there |
| --- | --- |
| `mods/core/.../base` | What every layer stands on: the declaration ledger, NBT persistence and the mixin plugin base |
| `mods/core/.../effect` | Effects on residents (stats, flags, modifiers), with perks as one source of them under `effect/perk` |
| `mods/core/.../library` | The resource library: data packs and loose files under `<gamedir>/folkways/` |
| `mods/core/.../core` | The colony engine: residents, planning, labour and travel. `core/api` is the public API; `core/shell` fits the engine to the world, including other mods' structures (Sable, Create) |
| `mods/core/.../front` | Screens, the colony book, networking and lessons. `front/api` is the public API |
| `mods/vanilla/.../plugins` | Each feature (farming, pasture, build, …) as a plugin over the two APIs |
| `mods/create/.../create` | Rail, dispatch and `create/build` |
| `mods/modulargolems/.../modulargolems` | Golems as residents |
| `mods/vanilla/src/main/resources/data/folkways/folkways/pattern` | Built-in building patterns (Starlark) |
| `mods/*/src/gametest` | NeoForge game tests, run together by the root's `runGameTestServer` |
| `tests/` | Blockwright and Playwright suites, listed below |

## Tests

| Command | What it runs |
| --- | --- |
| `npm run test:unit` | Fast Node tests for the TypeScript helpers; no game |
| `npm run test:i18n` | Language keys, placeholders and notice/refusal coverage; no game or dependencies |
| `npm run typecheck` | Type checks every TypeScript suite |
| `./gradlew check` | Compiles everything and lints for tabs and trailing whitespace |
| `./gradlew runGameTestServer` | NeoForge game tests in every `mods/*/src/gametest`, with all mods but Modular Golems loaded |
| `npm run test:e2e` | End-to-end tests against a real client and server, on the ground |
| `npm run test:sable` | The same against a [Sable](https://modrinth.com/mod/sable) ship: each twin spec's aboard test, and the ship-only specs |
| `npm run visual:take` | Screenshots of screens, panels, looks and in-world visuals |
| `npm run ponder:take` | Films every ponder scene in the colony book |
| `npm run pattern:take` | Lays out each work pattern's gallery and shoots it |
| `npm run bench:take` | Performance benchmarks on a large colony |
| `npm run promo:take` | Films the promotional takes |

The game suites download Minecraft, NeoForge and the fixture mods on first run, and their
test servers accept the [Minecraft EULA](https://www.minecraft.net/eula) through
`blockwright.toml`. Run them only if you accept it. Use `--workers=1` to limit resource
use, for example:
>>>>>>> theirs

```sh
npm run test:unit
npm run typecheck
./gradlew check
```

Add or update tests for behavioral changes. Prefer a black-box e2e spec for
behavior a player can observe. Run the relevant mod's GameTests and client
suite.

CI runs the build checks, per-mod GameTests and a focused real-client founding
smoke test. It does not run every client or capture suite. See
`.github/workflows/build.yml` for the exact matrix.

## Pull requests

1. Make the change in your fork on a focused branch.
2. Follow the conventions below and update the relevant tests and documentation.
3. Run the checks and game suites appropriate to the change.
4. Open a pull request against [IzakyL/folkways](https://github.com/IzakyL/folkways/pulls).
   Explain the problem, resulting behavior and verification. List checks you
   could not run and any remaining limitations.

Commit messages follow `type(scope): summary`, for example:

```text
fix(plan): keep rations out of cargo
feat(draft): add a dock pattern
```

## Conventions

### Code and test ownership

- Match the surrounding naming, comment density and idiom.
- Use no tabs or trailing whitespace; `./gradlew check` checks scaffold formatting.
- Keep GameTests in the module owning the behavior, including package-private
  scenarios. Shared setup belongs in Core's `src/gameTestFixtures`, without test
  registration.
- Shared TypeScript helpers belong in `tests/shared/`. A suite can import from
  there and `tests/ldlib2/`, but not from another suite.

### Translations and patterns

- Add mod-owned player-facing strings to all four shared catalogs in
  `mods/core/src/main/resources/assets/folkways/lang/`: `en_us.json`, `zh_cn.json`,
  `zh_hk.json` (Hong Kong) and `zh_tw.json` (Taiwan). Use each region's Minecraft
  terminology, rather than converting characters alone.
- Keep translated text as a `Component` until it reaches the client. Use
  `Pages.text` / `Pages.readText` for UI sync and `Line.text` for information cards.
  Calling `getString()` on the server loses the player's language.
- Arguments use `%s`, or indexed `%1$s` when reordered; a literal percent uses `%%`.
- Pattern-owned titles, controls, materials and refusal messages belong in the
  pattern's `lang` dictionary. Built-in patterns and shared libraries provide all
  four locales; matching mod catalog entries are not needed.
- Custom patterns fall back per word to another Chinese locale, then `en_us`.
  `zh_hk` and `zh_tw` prefer each other before `zh_cn`.
- Use `reject("error.key", ...)` for expected refusals, with at most four text
  or numeric arguments. Both `reject` and `translated("another.key")` resolve
  pattern and shared library words. The pattern's own words override library
  words for the same locale and key. Use `translated` for arguments such as
  material names, and `fail(...)` for script diagnostics in custom patterns.

### Documentation

Update English and Chinese pages together. Use the game's locale catalogs for UI
names and keep commands, paths and Java symbols literal. Documentation stays at the
overview level: the READMEs and contribution guides. Do not add detailed reference pages.
