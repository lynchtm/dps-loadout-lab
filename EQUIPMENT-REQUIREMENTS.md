# Equipment requirement coverage

The September 20, 2026 review increases verified equip-level records from 1,881
to 2,328 of 5,697 catalog item IDs: 448 additions, 90 corrections, and one invalid
record removed. IDs include cosmetic, charged, minigame and historical variants;
these counts are not counts of unique usable combat items.

The bank optimizer excludes unknown requirements by default unless an item is
already equipped. Missing data is distinct from an explicitly reviewed empty
requirement map. The latter means no **skill-level** equip requirement; quest,
boss-kill, account and activity restrictions still require player confirmation.
For example, ancient rings require their respective Desert Treasure II boss kill;
their crafting requirements are not equip requirements.

## Priority coverage

Added records include Confliction gauntlets, Tormented bracelet and ornament,
all three imbued god capes, Seers/Archers/Warrior rings and imbues, ancient rings,
Ring of suffering imbues/recoil variants, Infernal cape, Barrows gloves,
Ancient sceptre, Dragon hunter lance and Neitiznot faceguard. Reviewed cosmetic
variants include Twisted ancestral, Sanguine torva, Radiant oathplate, coloured
crystal equipment, ornamented godswords and holy/sanguine raid weapons.
Max cape combinations retain 99 in all 24 skills, including Sailing.

Corrected records include shared skill requirements for Avernic treads, Void
armour, salamanders and Elidinis' ward; bonuses previously mistaken for levels
on Masori, Monk's robes, Ring of the gods, Bonecrusher necklace and other items;
and regeneration text incorrectly interpreted as Hitpoints requirements.
The Cursed goblin staff's bogus 64 Magic record was removed: that number described
splashing accuracy, not an equip requirement. Its level requirements remain unknown.

There are still 3,369 IDs without verified records. Do not mark these unrestricted
merely to improve the coverage number. The generated missing-item report supports
continued review, including activity variants and additional ordinary equipment.

## Sources and refreshing

The acquisition script retrieves the equipment catalog's 4,036 source articles
from the OSRS Wiki API and follows redirects while preserving requested and
resolved page names. Each parsed record retains the article revision. Manual
reviews in `tools/reviewed-equipment-requirements.json` retain the item revision,
review date, reasoning and supporting revision for inherited/cross-page facts.
These reviews are applied on every refresh; they are not one-off resource edits.

Supporting sources include [Armour/Highest bonuses](https://oldschool.runescape.wiki/w/Armour/Highest_bonuses?oldid=15344471),
[Elite Void Knight equipment](https://oldschool.runescape.wiki/w/Elite_Void_Knight_equipment?oldid=15260639),
[Max cape](https://oldschool.runescape.wiki/w/Max_cape?oldid=15320227),
and [God capes](https://oldschool.runescape.wiki/w/God_capes?oldid=15296608).
See NOTICE.md for Wiki attribution and licensing.

From the plugin directory, use a new cache directory for a fresh snapshot:

```powershell
python tools/acquire-wiki-facts.py --cache build/facts --equipment
python tools/refresh-requirement-facts.py --cache build/facts
python -m unittest discover -s tools -p test_requirement_facts.py
```

The refresh refuses an incomplete source cache and writes a missing-ID report
to `build/facts/requirement-coverage.json`. Review changed requirements and manual
records against their source revisions before shipping another snapshot.
Do not infer equip levels from acquisition XP, bonuses, spell casting, crafting
or passive effects. Likewise, do not copy normal-world requirements to Last Man
Standing/event variants just because they share a display name.

## Validation

Python tests cover extraction boundaries, explicit unknown/unrestricted states,
all catalog variants of 47 priority combat source pages, and the reported item IDs.
Java optimizer regressions verify that banked magic upgrades beat the reported
fallbacks with default eligibility settings, and that 89 Hitpoints still excludes
Confliction gauntlets even with unverified requirements enabled.

Run the workspace `tools/test-plugin.ps1 -Name wiki-dps` for the full Java build.
These are automated checks, not in-game verification. After installing the update,
scan the bank and regenerate with the cape/hands/ring slots unlocked and
**Include unverified requirements** disabled.
