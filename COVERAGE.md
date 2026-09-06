# Product coverage and remaining work

The complete acceptance target is REQUIREMENTS.md. This document reports implementation
status, not a reduction of that target or a claim that unsupported features are impossible.

| Area | Present | Remaining |
|---|---|---|
| Native integration | Sidebar, attackable NPC menu, target/interacting NPC detection, configurable live overlay, event snapshots, background analysis | Infobox, selected-loadout and comparison overlays, panel-toggle changes without restart |
| Player state | Equipment, inventory/bank snapshots, levels/boosts, HP, offensive prayers, style, spellbook, wilderness, soulreaper, Kandarin hard diary | Autocast/selected spell detection, exact loaded darts/charges, target-aware Slayer matching, full quest/diary/unlock/league state |
| Overrides | Player leaf overrides survive live refresh; source labels; manual target fields; reset | Strongly typed bespoke controls for every field, per-slot reset of all related fields as one operation |
| Targets | Bundled Wiki catalog, searchable variants, custom target presets, stat/attribute/weakness/raid/reduction editing | Latest data, complete phases and content-specific mechanics, exact universally readable HP |
| Loadouts | 1–32 independent drafts, saved templates, Wiki setups, names/descriptions, copying/reordering/removal, draft-specific live sync, named comparisons, item-source searches | Web share format compatibility |
| Combat | Pinned native melee/ranged/magic engine, gear/sets, prayers, boosts, enchanted bolts, several raid modifiers | Current Wiki parity, full seasonal modes, every advanced item and NPC transform |
| Specials | Pinned engine's common special modifiers, expected damage, configurable fixed-target rotation estimate | Complete claws/dark bow/halberd/Voidwaker distributions, cost inference, sequential drains, exact timed energy schedule, weapon switching |
| Incoming damage | Manual single-attack profile, defence/prayer bonus, Elysian expectation, explicit protection assumption | Automatic NPC offensive profiles, complete defensive sets, boss patterns, healing and protection penetration |
| Results | Distribution-based DPS/expectation, min/max in histogram, accuracy, ticks, fixed-distribution TTK/variance, sorting, CSV | Full resource efficiency, prayer drain inference, dynamic HP-dependent TTK, first-attack timing controls |
| Explanation | Input snapshot, rolls, expected direct/delayed damage, interval, DPS and kill recurrence, active equipment/prayers, warnings | Step-by-step rounding trace attributing every internal multiplier to its exact source |
| Graphs | Hit PMF, comparisons, defence/level/boost sweeps, bounded kill PMF, incoming rate | Dynamic delay/HP/phase graphs, interactive point inspection, complete graph labels and legends |
| Persistence | Versioned JSON, named scenarios/custom targets, per-profile ConfigManager storage, input/workload limits | Migration beyond v1, strict unknown-field schema rejection, preset deletion UI |
| Bank optimization | Per-character quantities and scan provenance, item normalization, selectable melee/ranged/magic, automatic supported autocast spell/level/compatibility search against powered weapons, target-specific weapon/style scoring, shared single/grouped encounter objective with burst/barrage group DPS estimates, useful ammo filtering, slot locks, bounded search with set seeds, small-pool enumeration, equipment preview and independent draft insertion | Mixed-target group simulation and other area attacks, manual casting/Magic Dart/support rotations, automatic prayer/boost profiles, current complete requirements/unlocks/charge detection, exact optimization of large pools, rune/resource sufficiency, engine parity |
| Validation | 150 inherited pinned reference cases and broader tests; active live-capture, measurement-window, scenario/cache and Swing lifecycle tests; explicit candidate snapshot replay | New latest-Wiki-generated fixtures, full fuzz campaign, in-game checks |

## Known changes since the reference

Compared the inspected source at `89c3e25` with `b6bc098d`. The main combat file has 203
added and 76 removed lines. Changes include Inquisitor weighting, Soulreaper special accuracy
and minimum hit, Sanguinesti base damage/procs, Dawnbringer damage, Twisted bow clamps,
Silverlight/Darklight accuracy, Tonalztics/Rosewood specials, Maggot King behavior, Araxyte
guaranteed-hit behavior, Mad Angel phases, Glyphic Attenuation and item-name changes.
Affected setups receive visible result limitations. The optimizer excludes known affected
candidates by default, with explicit advanced inclusion. Absence of a flag is not verification. The equipment catalog remains
pinned too; merely replacing monster JSON would not establish parity.

## Modelling boundaries

TTK moments assume independent attacks with a fixed distribution and interval. They account
for overkill and misses, but not changing HP-dependent bolt procs, phases, correlated attack
delays or delayed damage. Exact moments are bounded to 10 million transitions; kill graphs
to 20 million. Unavailable metrics are omitted with a reason.

Rotation estimates assume the configured count of identical specials plus normal attacks
against unchanged target stats. Energy feasibility is a total-budget check, not a chronological
schedule. The requested cost is manual. Complex unsupported special distributions are withheld.

Boost decay assumes a 60-second cycle starting at the supplied elapsed time; preserved/divine
boosts and overloads are held while the effect is assumed active. Expiry, reapplication, HP
cost and the player's actual stat-restore timer are not modelled. Prayer duration uses a
manual unmodified drain-per-minute rate and assumes starting prayer points equal base level.

Incoming damage assumes the manually specified style, roll, max hit and interval. Protection
is an explicit full-block assumption and is not automatically applied to boss attacks.


## Consolidated calculation boundary

Live and comparison inputs both use ScenarioCalculator. CalculationInputs defines cache
identity; names, provenance and packing do not affect numerical results. CalculationCoordinator
bounds cached results; profile changes invalidate reuse. WorkspacePersistence writes drafts
immediately and suppresses identical writes, avoiding pending-save loss during profile changes.
Charts are requested on expansion; parameter sweeps use the score path without repeated TTK.
See PARITY.md for the candidate reference update/replay workflow.
