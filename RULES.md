# Independent combat-rule specification and sources

Implemented September 9, 2026. These are independently authored rules over factual
inputs, not a port of the Wiki calculator's code or its generated output fixtures.
Source article revisions are listed in `rule-sources.json`. All URLs refer to
gameplay documentation, not calculator implementation source.

## Probability and timing

An attack succeeds when an integer drawn uniformly from 0 through the attack roll
exceeds an independent integer from 0 through the defence roll. The standalone
foundation implements this contest and is tested against exhaustive ordered pairs.
Ordinary successful damage is uniform from zero to the computed maximum, with a
successful zero converted to one when the maximum is positive. Misses retain zero.
Multi-hit attacks combine independently rolled hits unless an explicit correlated
rule says otherwise. Armour/caps apply per hitsplat before sums. DPS divides mean
damage by the attack interval in ticks times 0.6 seconds.

Stationary kill analysis is a bounded recurrence over remaining HP, including
misses and overkill. The displayed estimate assumes one interval before the first
hit. It does not simulate moving, regeneration, changing HP procs or boss phases.

## Levels, equipment and ordinary combat

Melee and ranged apply visible boosts and the strongest compatible prayer bonus,
round down, add the stance bonus and eight, then apply applicable Void scaling.
The maximum hit is `floor((effective strength * (strength bonus + 64) + 320)/640)`.
NPC defence contests use the relevant target level plus nine and defence bonus
plus 64. Magic normally uses NPC Magic level, with documented exceptions for Ice
demon and Verzik. Light/standard/heavy ammunition uses its corresponding defence.
Magic accuracy applies Void before stance/eight, as specified by the Magic article.

Sources:
- https://oldschool.runescape.wiki/w/Damage_per_second/Melee
- https://oldschool.runescape.wiki/w/Damage_per_second/Ranged
- https://oldschool.runescape.wiki/w/Damage_per_second/Magic
- https://oldschool.runescape.wiki/w/Maximum_magic_hit
- https://oldschool.runescape.wiki/w/Prayer
- https://oldschool.runescape.wiki/w/Weapons/Categories

Equipment bonuses come directly from RuneLite's item-stat export. Magic damage
is stored in tenths of a percentage point; the UI displays a percentage. Ammunition
strength contributes only when the launcher consumes it. Two-handed weapons
reject shields. Blowpipes require an explicit dart type. Unknown/noncombat weapons
cannot become one-tick attacks. New item variants retain their own factual stats;
cosmetic requirement inheritance is restricted by name and RuneLite variation maps.

## Target and weapon effects

The implementation includes task/Salve exclusions, Void, Crystal armour,
Inquisitor piece weights, dragonbane/demonbane weapons, obsidian, vampyre flails,
rat-only bone staff, Dharok, Verac, Keris, fang accuracy/damage bounds, twisted-bow
scaling, scythe size-based hits, common enchanted bolt effects, shadow equipment
scaling, powered-staff base damage, Sanguinesti's additional hit damage, elemental
spell-tier scaling and weaknesses, Sunfire minimum damage, Virtus, Chaos gauntlets,
smoke staves, tomes and marked demonbane damage. Each supported effect is coded
explicitly; it is not inferred from an item's damage bonus alone.

Special attacks include the explicitly listed branches in DpsCalculator. An
unimplemented special returns a local limitation while leaving the normal attack
comparison usable. Sequential special-energy availability and sequential target
drains are not simulated by the rotation estimate.

Defence reductions operate on copies and round after each discrete drain. Raid
scaling and encounter-dependent effects are not fully verified; affected results
are flagged and excluded from bank optimization by default. See COVERAGE.md.

## Validation boundary

Hand-derived tests cover normal rolls, successful-zero mass, prayer/task rounding,
Salve exclusion, Soulreaper stacks, scythe totals, per-hit armour, Corporeal Beast,
elemental scaling, shadow, Sanguinesti, bone staff, demonbane, dark bow, drains,
special fallback and finite/unit-mass invariants. Catalog tests cover default
variants, immutable target snapshots, ammo consumption and saved-field compatibility.
Retained workflow tests cover imports, inventory pairing, bank generation, draft
isolation, persistence and Swing controls. These are not a complete game-rule
proof, a current-Wiki parity claim or in-game verification.
