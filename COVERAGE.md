# Replacement runtime coverage

September 9, 2026. REQUIREMENTS.md remains the product acceptance target. This runtime replacement is not a claim of complete Wiki calculator parity.

| Area | Implemented | Limits |
|---|---|---|
| Integration | New event capture, target menu, background calculations, sidebar and live/comparison overlay | User verification of toggles, logout, hopping and profiles remains necessary |
| Loadouts | Independent drafts, templates, Wiki imports, inventory/pouch plans, Bank Tags preparation, comparison table and graphs | Wiki sharing URLs differ from native JSON; no automatic withdrawing/equipping |
| Player capture | Equipment, levels, boosts, HP, prayers, attack button, spellbook, wilderness and inventory | Selected spell, loaded darts/charges, diary completion, Soulreaper stacks and exact task applicability need explicit assumptions |
| Facts | 5,697 equipment records, 4,289 target variants, 48 spells and 99 weapon options acquired September 9 | Unknown requirements are excluded from bank generation by default; quests, unlocks and resources are not fully inferred |
| Combat | Melee/ranged/magic rolls, rounding, prayers, successful-zero damage, ammunition validity and documented gear effects | See RULES.md; tests are independently derived examples and invariants, not exhaustive gameplay parity |
| Distributions | Misses, independent multiple hits, per-hit armour/caps, selected bolt/set effects, stationary kill recurrence | No movement, respawn, regeneration, phase progression or changing-HP proc simulation |
| Bank generation | Target-specific weapon/style/spell search, owned gear preview, group burst/barrage estimates, slot locks and tie breaks | Large searches are bounded best-found searches; unverified formulas are excluded unless explicitly enabled |

## Visible calculation limitations

Chambers of Xeric scaling and special encounters; Tombs of Amascut scaling, phases and core rules; Vardorvis changing defence; Verzik, Vorkath, Kalphite Queen, Nightmare and tormented-demon phases/protection are not fully simulated. Their results are flagged estimates using the selected static stats.

Araxyte/maggot/mad-angel guaranteed-hit rules, dual macuahuitl, Torag's hammers, sulphur blades, tonalztics, eclipse atlatl, Dawnbringer, salamanders and Dinh's normal-attack mechanics remain unverified or incomplete. Moon set timing and Karil/Ahrim proc distributions are also flagged. These candidates are excluded from bank generation by default.

Unsupported specials (including correlated claw attacks) report unavailable without invalidating normal DPS. The rotation estimate does not model a sequence of changing target drains or special-energy regeneration. Ayak drains require manually entered reduced stats.

Warnings travel with results. Absence of a warning is not proof of accuracy. Guide-import boss coverage is separate from combat-rule coverage; successful Araxxor inventory import does not verify every boss mechanic.
