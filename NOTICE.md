# NOTICE

**Per Lapidem ad Astra** is a fork of **Folkways**.

- Upstream: <https://github.com/IzakyL/folkways>, by **IzakyL**, MIT License.
- Baseline: upstream `master` at `3ce4d88` (tag `upstream-base-3ce4d88-20261019`).
- This repository: <https://github.com/TCCH1111/per-lapidem-ad-astra>, by **TCCH1111**, also MIT License.
- The upstream `LICENSE` and every upstream copyright header are kept unchanged. `THIRD_PARTY.md` (Starlark, Ponder,
  Flywheel and the repository assets) applies to this fork as it does upstream.

We do not claim Folkways as our own work; the colony engine, planner, screens and patterns are IzakyL's.

## What this fork changes

The first change is the modid rename (docs/plans/PLAN-001 in the Per Lapidem ad Astra docs repository):

| Upstream modid | This fork | Jar |
| --- | --- | --- |
| `folkways_core` | `plaa_core` | `plaa-core-neoforge-1.21.1-*.jar` |
| `folkways_vanilla` | `plaa_vanilla` | `plaa-vanilla-neoforge-1.21.1-*.jar` |
| `folkways_create` | `plaa_create` | `plaa-create-neoforge-1.21.1-*.jar` |
| `folkways_modulargolems` | `plaa_modulargolems` | `plaa-modulargolems-neoforge-1.21.1-*.jar` |

What did **not** change: the `folkways` resource namespace, every package and class name, every registry name, data pack
and save format. Worlds therefore carry over; only configuration files are renamed.

## Not installable next to upstream Folkways

Each of our four mods declares the four upstream mods `incompatible`: a world can only have one colony system. Installing both
makes the game refuse to start.

## Configuration files move with the modid

Settings you changed in upstream Folkways are not picked up automatically:

| Upstream | This fork |
| --- | --- |
| `serverconfig/folkways_core-server.toml` | `serverconfig/plaa_core-server.toml` |
| `serverconfig/folkways_vanilla-server.toml` | `serverconfig/plaa_vanilla-server.toml` |
| `serverconfig/folkways_create-server.toml` | `serverconfig/plaa_create-server.toml` |
| `config/folkways_core-client.toml` | `config/plaa_core-client.toml` |

Copy your values across by hand if you want to keep them; the new files start from the defaults.
