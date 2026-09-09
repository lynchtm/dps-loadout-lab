# Sources and attribution

The Java code in this revision is distributed under BSD-2-Clause. Copyright (c)
2026 Tommy Lynch for local contributions; retained Wiki setup components preserve
Copyright (c) 2026 Jiimbones and their complete BSD notice. See LICENSE and
src/main/resources/META-INF/LICENSE-wiki-gear-setups.

This revision replaces the previous GPL-derived calculator, models, integration,
fixtures, catalogs and imported images. The license change applies to the current
replacement sources, not retroactively to earlier revisions. PROVENANCE.md records
the authoring method and its limits; this is not a formal clean-room certification.

## Separately attributed factual data and game assets

Equipment bonuses and item names were acquired directly from RuneLite's public
exports on September 9, 2026:
- https://static.runelite.net/item/stats.ids.min.json
- https://static.runelite.net/cache/item/names.json

Target stats, weapon categories, spell levels/damage and conservative equipment
requirements were independently collected from OSRS Wiki public Bucket API and
article revisions. These are game facts, not copied calculator source or its data
files. The acquisition scripts, facts-provenance.json, requirements-provenance.json
and RULES.md record their origin. No ownership or BSD license is claimed over Wiki
contributors' content or Jagex's game names and assets.

Wiki content is separately attributed to OSRS Wiki contributors under the Wiki's
applicable CC BY-NC-SA 3.0 content policy:
https://oldschool.runescape.wiki/w/RuneScape:Copyrights
https://creativecommons.org/licenses/by-nc-sa/3.0/
Preserve the applicable attribution/license when redistributing Wiki-derived data.
The software's BSD license does not relicense that content.

Item and prayer/skill/interface images are supplied by the running RuneLite client
from Jagex game assets. No imported calculator image bundle is shipped. The plugin's
navigation icon and fallback badges are original Java2D drawings.
Old School RuneScape names and assets belong to Jagex; this project is not endorsed
by Jagex, RuneLite or the OSRS Wiki.

## Wiki equipment setup components

Pure equipment/inventory/example parsers, item-name resolution and recommendation
helpers and their tests are adapted from Wiki Gear Setups, BSD-2-Clause:
https://github.com/Jiimbones/wiki-gear-setups/tree/e836b8d156f0f5f9b7e6375bb04c52f21835ac19
Copyright (c) 2026 Jiimbones. Their full notices remain in source and in the binary
third-party license. Local adaptations retain quantities, pair inventories and
connect recommendations to comparison drafts and core Bank Tags.

Wiki guide requests retain source page/revision in each draft. Wiki test fixtures
are separate content, not included in the plugin JAR. The Collection Log corpus
manifest at src/test/resources/boss-wiki/manifest.json identifies its 61 page
revisions and hashes (September 6, 2026). It covers the 57 Bosses entries from
https://oldschool.runescape.wiki/w/Collection_log?oldid=15330455 .
Additional regression pages include:
https://oldschool.runescape.wiki/w/Araxxor/Strategies?oldid=15322538
https://oldschool.runescape.wiki/w/Slayer_task/Nechryael?oldid=15330350
Those fixtures and the older imported Wiki guide examples retain the Wiki's
separate CC BY-NC-SA 3.0 attribution. Test item names come from RuneLite's public
cache export and remain Jagex game facts.

Gradle wrapper provenance is recorded in the workspace TEMPLATE-SOURCE.md. Build
tooling and dependencies retain their own licenses; no third-party runtime
library was added by this replacement.
