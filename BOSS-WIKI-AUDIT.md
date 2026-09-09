# Collection Log boss Wiki import audit

Historical import audit reviewed September 6, 2026. The September 9 runtime replacement reruns the pinned corpus regressions; the old total-suite counts below describe the earlier revision, not current combat coverage. Scope: Wiki guide discovery, equipment/import parsing, inventory and rune-pouch pairing, compatible item resolution, and draft persistence. This is separate from verification of combat formulas, boss mechanics, or in-game interactions.

## Coverage

- 57 Collection Log Bosses entries, from [Collection log revision 15330455](https://oldschool.runescape.wiki/w/Collection_log?oldid=15330455). Separate Wilderness and Nightmare variants bring the corpus to 61 pages.
- All 61 pages fetched successfully; 59 contain importable equipment. Bryophyta and Obor describe strategies in prose and do not provide supported equipment/inventory templates.
- All 33 component-NPC searches found their encounter guide. Captured API results replay through the actual search service without network access.
- 251 choices: 163 ranked loadouts and 88 complete equipment examples. 194 choices have an automatic inventory match; 57 are gear-only or require an explicit inventory selection.
- All 177 documented inventory templates are parsed, including 118 with rune-pouch contents. 170 appear in automatic matches; the remaining seven are available in the inventory selector.
- All 251 choices convert through the production draft builder and preserve their carry plans through scenario serialization. No parser/conversion errors or unresolved supply names in the pinned name-index audit.
- Full build: 746 tests; 745 passed, one optional candidate-parity test skipped. Pinned fixtures include explicit per-choice weapon and inventory-pair expectations, source hashes, and component discovery responses.

## Repairs made

- Import standalone equipment/inventory grids and headerless single-example tables, while retaining the existing multi-column table parser.
- Match unique named methods independently when page-wide ordering is interrupted, including inventories shown before gear. Pane names take priority over repeated combat-style names.
- Keep exact examples separate from general ranked recommendations; matching list lengths alone cannot assign a budget example to an unrelated combat style.
- Preserve explicit inventory alternatives, avoid borrowing inventories from a different tab or section, and suppress decorative gear grids that duplicate ranked equipment.
- Rank the selected boss’s strategy page ahead of unrelated search hits.
- Resolve generic equipment against the destination slot and supported equipment facts. Diary reward families can resolve to real tiers, and warm clothing resolves to clothing for the requested slot.

## Deliberate exceptions

| Guide | Expected behavior |
|---|---|
| Bryophyta; Obor | No importable templates. Build manually or use another guide; the plugin does not invent a loadout from prose. |
| Abyssal Sire | Select the shared Melee and ranged inventory explicitly; it is not automatically assigned to the guide’s magic methods. |
| Alchemical Hydra | Ranged matches automatically. Choose between banking-bones and long-trip inventories for Melee. |
| The Leviathan | Select the page-level inventory explicitly; the guide lists both ranged and magic methods. |
| Vardorvis | Choose Normal or Awakened inventory explicitly. This choice does not change the calculator target. |
| Yama | Main and contract examples import with their inventories. The additional Oathplate inventory following separate strategy text remains an explicit selection. |
| Other gear-only methods | Retain the guide’s equipment without inventing supplies. Some have prose instructions or separately selectable complete examples. |

Five choices on Barrows, Maggot King, Wintertodt and Zulrah contain recommendations absent from the pinned DPS equipment catalog (elemental amulets or polar camo clothing). The importer records an unsupported-item note and uses the next compatible recommendation from that guide. It does not add unverified combat facts. Other genuine empty slots, such as an off-hand removed for a two-handed weapon, remain intentional.

The offline RuneLite item-name snapshot has no note/placeholder or stackability metadata. It checks name coverage and carry persistence; exact live variant selection and unspecified supply quantities remain covered only by their dedicated tests and client testing. The normal plugin uses client definitions, not this test snapshot.

## Per-page results

Automatic is the number of choices with a matched inventory. A gear-only/manual choice is not necessarily an import failure; see the exceptions above.

| Collection Log entry | Wiki page / revision | Choices | Automatic | Inventory templates |
|---|---|---:|---:|---:|
| Abyssal Sire | [Abyssal Sire/Strategies · 15323806](https://oldschool.runescape.wiki/w/Abyssal_Sire/Strategies?oldid=15323806) | 8 | 0 | 1 |
| Alchemical Hydra | [Alchemical Hydra/Strategies · 15313162](https://oldschool.runescape.wiki/w/Alchemical_Hydra/Strategies?oldid=15313162) | 2 | 1 | 3 |
| Amoxliatl | [Amoxliatl/Strategies · 15329075](https://oldschool.runescape.wiki/w/Amoxliatl/Strategies?oldid=15329075) | 1 | 1 | 1 |
| Araxxor | [Araxxor/Strategies · 15322538](https://oldschool.runescape.wiki/w/Araxxor/Strategies?oldid=15322538) | 2 | 2 | 2 |
| Barrows Chests | [Barrows/Strategies · 15329276](https://oldschool.runescape.wiki/w/Barrows/Strategies?oldid=15329276) | 3 | 2 | 2 |
| Brutus | [Brutus/Strategies · 15327694](https://oldschool.runescape.wiki/w/Brutus/Strategies?oldid=15327694) | 4 | 4 | 4 |
| Bryophyta | [Bryophyta · 15319604](https://oldschool.runescape.wiki/w/Bryophyta?oldid=15319604) | 0 | 0 | 0 |
| Callisto and Artio | [Callisto/Strategies · 15318347](https://oldschool.runescape.wiki/w/Callisto/Strategies?oldid=15318347) | 2 | 2 | 2 |
| Callisto and Artio | [Artio/Strategies · 15331538](https://oldschool.runescape.wiki/w/Artio/Strategies?oldid=15331538) | 2 | 2 | 2 |
| Cerberus | [Cerberus/Strategies · 15320707](https://oldschool.runescape.wiki/w/Cerberus/Strategies?oldid=15320707) | 2 | 2 | 2 |
| Chaos Elemental | [Chaos Elemental/Strategies · 15325471](https://oldschool.runescape.wiki/w/Chaos_Elemental/Strategies?oldid=15325471) | 2 | 2 | 2 |
| Chaos Fanatic | [Chaos Fanatic/Strategies · 15325298](https://oldschool.runescape.wiki/w/Chaos_Fanatic/Strategies?oldid=15325298) | 1 | 1 | 1 |
| Commander Zilyana | [Commander Zilyana/Strategies · 15316256](https://oldschool.runescape.wiki/w/Commander_Zilyana/Strategies?oldid=15316256) | 2 | 2 | 2 |
| Corporeal Beast | [Corporeal Beast/Strategies · 15315712](https://oldschool.runescape.wiki/w/Corporeal_Beast/Strategies?oldid=15315712) | 12 | 9 | 6 |
| Crazy archaeologist | [Crazy archaeologist/Strategies · 15318138](https://oldschool.runescape.wiki/w/Crazy_archaeologist/Strategies?oldid=15318138) | 1 | 1 | 1 |
| Dagannoth Kings | [Dagannoth Kings/Strategies · 15331444](https://oldschool.runescape.wiki/w/Dagannoth_Kings/Strategies?oldid=15331444) | 7 | 4 | 4 |
| Deranged Archaeologist | [Deranged archaeologist/Strategies · 15331449](https://oldschool.runescape.wiki/w/Deranged_archaeologist/Strategies?oldid=15331449) | 1 | 0 | 0 |
| Doom of Mokhaiotl | [Doom of Mokhaiotl/Strategies · 15331161](https://oldschool.runescape.wiki/w/Doom_of_Mokhaiotl/Strategies?oldid=15331161) | 3 | 2 | 2 |
| Duke Sucellus | [Duke Sucellus/Strategies · 15329073](https://oldschool.runescape.wiki/w/Duke_Sucellus/Strategies?oldid=15329073) | 1 | 1 | 1 |
| The Fight Caves | [TzHaar Fight Cave/Strategies · 15313258](https://oldschool.runescape.wiki/w/TzHaar_Fight_Cave/Strategies?oldid=15313258) | 6 | 4 | 4 |
| Fortis Colosseum | [Fortis Colosseum/Strategies · 15327693](https://oldschool.runescape.wiki/w/Fortis_Colosseum/Strategies?oldid=15327693) | 12 | 10 | 6 |
| The Gauntlet | [The Gauntlet/Strategies · 15305612](https://oldschool.runescape.wiki/w/The_Gauntlet/Strategies?oldid=15305612) | 4 | 4 | 4 |
| General Graardor | [General Graardor/Strategies · 15328579](https://oldschool.runescape.wiki/w/General_Graardor/Strategies?oldid=15328579) | 8 | 8 | 8 |
| Giant Mole | [Giant Mole/Strategies · 15316208](https://oldschool.runescape.wiki/w/Giant_Mole/Strategies?oldid=15316208) | 5 | 5 | 5 |
| Grotesque Guardians | [Grotesque Guardians/Strategies · 15307248](https://oldschool.runescape.wiki/w/Grotesque_Guardians/Strategies?oldid=15307248) | 6 | 4 | 4 |
| Hespori | [Hespori/Strategies · 15329208](https://oldschool.runescape.wiki/w/Hespori/Strategies?oldid=15329208) | 3 | 3 | 3 |
| The Hueycoatl | [The Hueycoatl/Strategies · 15329077](https://oldschool.runescape.wiki/w/The_Hueycoatl/Strategies?oldid=15329077) | 1 | 1 | 1 |
| The Inferno | [Inferno/Strategies · 15324405](https://oldschool.runescape.wiki/w/Inferno/Strategies?oldid=15324405) | 7 | 6 | 4 |
| Kalphite Queen | [Kalphite Queen/Strategies · 15324532](https://oldschool.runescape.wiki/w/Kalphite_Queen/Strategies?oldid=15324532) | 9 | 4 | 4 |
| King Black Dragon | [King Black Dragon/Strategies · 15316224](https://oldschool.runescape.wiki/w/King_Black_Dragon/Strategies?oldid=15316224) | 11 | 11 | 6 |
| Kraken | [Kraken/Strategies · 15329069](https://oldschool.runescape.wiki/w/Kraken/Strategies?oldid=15329069) | 1 | 1 | 1 |
| Kree'arra | [Kree'arra/Strategies · 15299717](https://oldschool.runescape.wiki/w/Kree%27arra/Strategies?oldid=15299717) | 5 | 5 | 5 |
| K'ril Tsutsaroth | [K'ril Tsutsaroth/Strategies · 15317619](https://oldschool.runescape.wiki/w/K%27ril_Tsutsaroth/Strategies?oldid=15317619) | 5 | 5 | 5 |
| The Leviathan | [The Leviathan/Strategies · 15327684](https://oldschool.runescape.wiki/w/The_Leviathan/Strategies?oldid=15327684) | 2 | 0 | 1 |
| The Mad Angel | [Mad Angel/Strategies · 15329047](https://oldschool.runescape.wiki/w/Mad_Angel/Strategies?oldid=15329047) | 3 | 3 | 3 |
| Maggot King | [Maggot King/Strategies · 15329112](https://oldschool.runescape.wiki/w/Maggot_King/Strategies?oldid=15329112) | 3 | 2 | 2 |
| Moons of Peril | [Moons of Peril/Strategies · 15326184](https://oldschool.runescape.wiki/w/Moons_of_Peril/Strategies?oldid=15326184) | 1 | 1 | 1 |
| Nex | [Nex/Strategies · 15331654](https://oldschool.runescape.wiki/w/Nex/Strategies?oldid=15331654) | 5 | 4 | 2 |
| The Nightmare | [The Nightmare/Strategies · 15332135](https://oldschool.runescape.wiki/w/The_Nightmare/Strategies?oldid=15332135) | 4 | 3 | 2 |
| The Nightmare | [Phosani's Nightmare/Strategies · 15331667](https://oldschool.runescape.wiki/w/Phosani%27s_Nightmare/Strategies?oldid=15331667) | 5 | 3 | 3 |
| Obor | [Obor · 15327844](https://oldschool.runescape.wiki/w/Obor?oldid=15327844) | 0 | 0 | 0 |
| Phantom Muspah | [Phantom Muspah/Strategies · 15319454](https://oldschool.runescape.wiki/w/Phantom_Muspah/Strategies?oldid=15319454) | 3 | 3 | 3 |
| Royal Titans | [Royal Titans/Strategies · 15329439](https://oldschool.runescape.wiki/w/Royal_Titans/Strategies?oldid=15329439) | 10 | 5 | 5 |
| Sarachnis | [Sarachnis/Strategies · 15319183](https://oldschool.runescape.wiki/w/Sarachnis/Strategies?oldid=15319183) | 6 | 6 | 3 |
| Scorpia | [Scorpia/Strategies · 15317574](https://oldschool.runescape.wiki/w/Scorpia/Strategies?oldid=15317574) | 1 | 1 | 1 |
| Scurrius | [Scurrius/Strategies · 15331669](https://oldschool.runescape.wiki/w/Scurrius/Strategies?oldid=15331669) | 6 | 6 | 6 |
| Shellbane Gryphon | [Shellbane gryphon/Strategies · 15329071](https://oldschool.runescape.wiki/w/Shellbane_gryphon/Strategies?oldid=15329071) | 5 | 2 | 2 |
| Skotizo | [Skotizo/Strategies · 15316231](https://oldschool.runescape.wiki/w/Skotizo/Strategies?oldid=15316231) | 2 | 2 | 2 |
| Tempoross | [Tempoross/Strategies · 15328452](https://oldschool.runescape.wiki/w/Tempoross/Strategies?oldid=15328452) | 5 | 4 | 4 |
| Thermonuclear smoke devil | [Thermonuclear smoke devil/Strategies · 15318150](https://oldschool.runescape.wiki/w/Thermonuclear_smoke_devil/Strategies?oldid=15318150) | 4 | 4 | 4 |
| Vardorvis | [Vardorvis/Strategies · 15329074](https://oldschool.runescape.wiki/w/Vardorvis/Strategies?oldid=15329074) | 1 | 0 | 2 |
| Venenatis and Spindel | [Venenatis/Strategies · 15330927](https://oldschool.runescape.wiki/w/Venenatis/Strategies?oldid=15330927) | 4 | 2 | 2 |
| Venenatis and Spindel | [Spindel/Strategies · 15330947](https://oldschool.runescape.wiki/w/Spindel/Strategies?oldid=15330947) | 2 | 2 | 2 |
| Vet'ion and Calvar'ion | [Vet'ion/Strategies · 15313276](https://oldschool.runescape.wiki/w/Vet%27ion/Strategies?oldid=15313276) | 3 | 2 | 2 |
| Vet'ion and Calvar'ion | [Calvar'ion/Strategies · 15324725](https://oldschool.runescape.wiki/w/Calvar%27ion/Strategies?oldid=15324725) | 4 | 3 | 3 |
| Vorkath | [Vorkath/Strategies · 15328632](https://oldschool.runescape.wiki/w/Vorkath/Strategies?oldid=15328632) | 5 | 5 | 3 |
| The Whisperer | [The Whisperer/Strategies · 15326967](https://oldschool.runescape.wiki/w/The_Whisperer/Strategies?oldid=15326967) | 1 | 1 | 1 |
| Wintertodt | [Wintertodt/Strategies · 15332127](https://oldschool.runescape.wiki/w/Wintertodt/Strategies?oldid=15332127) | 2 | 1 | 1 |
| Yama | [Yama/Strategies · 15316238](https://oldschool.runescape.wiki/w/Yama/Strategies?oldid=15316238) | 14 | 13 | 13 |
| Zalcano | [Zalcano/Strategies · 15328385](https://oldschool.runescape.wiki/w/Zalcano/Strategies?oldid=15328385) | 2 | 2 | 1 |
| Zulrah | [Zulrah/Strategies · 15329317](https://oldschool.runescape.wiki/w/Zulrah/Strategies?oldid=15329317) | 9 | 5 | 5 |

## Reproduction

See [README](README.md) for the offline audit and optional live refresh commands. The pinned [manifest](src/test/resources/boss-wiki/manifest.json) records page revisions and source digests. [Expected results](src/test/resources/boss-wiki/expected.json) pin reviewed choices and inventory associations. Live refreshes write to a separate directory and stop if the Collection Log category changes.

Fixtures are attributed in [NOTICE](NOTICE.md). Public search requests contain only encounter/NPC names; no bank or character data was transmitted.
