# DPS Loadout Lab

A native Java 11 RuneLite calculator: compare independent loadouts against one target,
find an owned setup, then prepare a bank layout. Independent community plugin; GPL-3.0.
See [NOTICE.md](NOTICE.md) for code, data and artwork attribution.

The calculation engine and equipment/NPC data are pinned to Wiki commit
`b6bc098dc0d742b2b763375d2e78e1b611a22070` (July 9, 2026).
Current Wiki parity is not claimed. [COVERAGE.md](COVERAGE.md) is the authoritative
capability/limitation inventory; [PARITY.md](PARITY.md) describes reference maintenance.

## Build and run

Java 11 is required. In this standalone plugin directory:

```powershell
./gradlew.bat build
./gradlew.bat run
```

On macOS/Linux use `./gradlew`. Workspace development uses `tools/test-plugin.ps1 -Name wiki-dps`
and `tools/run-plugin.ps1 -Name wiki-dps` from the workspace root. Release exports are
built from a clean committed workspace; RELEASE-SOURCE.md records their provenance.

## Compare loadouts

Enable the plugin and open its sidebar. Slate cards separate **Target**, **Loadout**,
**Results**, **Hit distribution**, and the collapsed **Templates** library at the bottom. Gold marks the active loadout and green
marks the best result values, including ties.

1. Use **+** to start from your current player, bank generation, a saved template, a Wiki setup, or a blank
   loadout. **From template** opens a picker inside Loadout. **Wiki setup** opens an add form
   inside Loadout and searches for the selected target; the query remains editable.
2. Select a numbered loadout tab and edit its name directly. The **•••** menu contains
   **Duplicate draft**, **Load current player** (replace this draft), and reorder.
   Click **×** on a comparison tab to remove that draft. The final draft cannot be removed.
3. Edit **Combat / Skills / Equipment / Prayer / Settings**. Weapon styles are restricted
   to compatible choices. Potions are multiselect; overlapping boosts take the maximum
   per stat. Prayer icons switch off conflicting prayers. Right-click gear to clear a slot.
4. Choose a target by name/ID, use the current NPC, or create a custom target. Expand
   **Monster stats** to edit its stats, attributes, reductions and conditions. The shared
   target applies to every comparison. The NPC **Calculate DPS** menu also selects a target.
5. Results update automatically. The default rows are DPS, max hit, accuracy and average
   TTK. Group DPS appears for grouped encounters; special damage appears when available.
   **Show more** reveals secondary metrics; **Expand comparison** opens a wider snapshot.
   Click a result column to select that draft.

Known formula limitations appear beside the table and mark affected columns. Full
assumptions and explanations remain in **Calculation notes**. Absence of a warning does
not establish verified accuracy. **Hit distribution** computes its chart when expanded;
other graphs are under **Advanced analysis**. Graphs and kill times remain per monster.

The **•••** menu also provides named comparisons, target presets, advanced input overrides,
JSON import/export and result/graph CSV. Native JSON is not the Wiki sharing URL format.
Advanced edits change calculator inputs only. Invalid edits show local feedback.

### Drafts, templates and live values

Working drafts recover after restart independently of the saved template library. **Save
as template** saves a copy of the selected draft. **Loadout + → From template** creates
another independent copy. The bottom **Templates** card previews saved gear, levels and inventory;
the **×** beside its selector deletes that saved copy, leaving comparison drafts intact.
Use a new name when saving a tweaked copy. Built-in starters use the selected
draft's base levels and sample gear; review their assumptions. Saved templates retain their
saved stats/settings. Existing profile data and configuration keys are preserved.

**Settings → Sync this draft with live player** assigns live updates to that draft,
including while another tab is selected. Manual overrides remain protected. Switching
character/profile or restarting turns synchronization off. **••• → Load current player**
explicitly clears overrides and reloads readable live values, including cleared gear slots.
New drafts and new live-overlay configurations default Slayer task to **yes**; existing
stored choices remain unchanged. Confirm the task actually applies to the target.

The optional overlay labels its source: **Live player** or **Selected comparison**, chosen
in RuneLite configuration. Both use ScenarioCalculator for equipment, scaling, damage and
TTK. Live values retain actual readable prayers and boosts; selecting a comparison uses
that draft's assumptions and requires Show sidebar to stay enabled. The old ineffective best-prayer/max-boost configuration keys are
retained but hidden. **Show sidebar** takes effect immediately.

Measured combat statistics apply only to the live overlay. Damage, kills and elapsed time
share one activity window, including zero-damage hits. Values decay with elapsed time;
after 10 seconds without activity they become unavailable. New activity resets both damage
and kills. These measurements include combat downtime and do not validate the theoretical model.

## Generate from your bank

Open the bank after login/hopping, choose the exact target, then open **Loadout + → Generate
from bank**. Select **Melee / Ranged / Magic**. The generator retains the draft's levels,
prayers, boosts and conditions; these assumptions are visible above the search. Change them
in the editor if needed. Magic compares supported spells/spellbooks and powered weapons
automatically, subject to level and staff compatibility.

Review the equipment preview and choose **Add to comparison** to create a new draft.
The form closes after adding. Its **×** cancels and closes the flow without creating a draft.

**Target → Encounter** selects single target or 2–9 grouped identical targets. Grouped
searches rank estimated group DPS for supported Ancient burst/barrage spells; other attacks
count once. Nechryael offers an explicit grouped shortcut. This is not a simulation of
stacking, movement, respawns, looting or mixed monster stats.

**Advanced options** contains slot locks and two independent opt-ins:

- **Include unverified requirements** permits missing requirement data; it never bypasses
  known unmet real-level requirements. Currently equipped items have observed eligibility.
- **Include known formula limitations** permits candidates flagged by COVERAGE.md. They are
  excluded by default; manual comparison still permits them with visible limitations.

Bank/inventory/equipped quantities are combined per character. Placeholders are ignored;
noted identities are normalized; charged variants remain distinct. Non-combat weapons and
inactive variants are excluded. Melee/Magic exclude projectile ammo; ranged retains
compatible ammo. Owned blessings can break DPS ties. Required monster gear is not inferred
universally. Blowpipes require an explicit loaded-dart assumption in Settings.

Small eligible pools are enumerated. Large pools use weapon/set/Wiki seeds and slot
refinement, bounded by 20,000 scores or about 15 seconds plus finalist analysis. **Best
found** is not a global maximum. **All eligible combinations checked** applies only to the
eligible pool and this model. Equal DPS uses defensive/prayer bonuses and stable ordering.

Review the visual gear, chosen attack/spell, results, visible access/charge/rune assumptions,
and expandable exclusions. **Add to comparison** explicitly creates a new draft; generation
never overwrites the starting draft or a template. Up to three distinct alternatives are
shown. Target, combat inputs, ownership, profile or locks invalidate stale suggestions.
Renaming and packing do not; insertion uses the latest packing plan. Quest unlocks, exact
charges, spellbook access and rune sufficiency remain player checks.

## Wiki setups and preparation

**Loadout → + → Wiki setup** is the entry point for Wiki guide search. It starts with the
selected target's name and runs that search. Type another activity or exact page title to
search elsewhere without changing the calculator target. Choose a method/tier/role,
preview its gear, inventory and pouch, then **Add as comparison draft**. **Prefer owned alternatives**
requires a fresh bank snapshot; turn it off to view goal equipment. Missing/unrecognized
items remain visible. Imports retain the current target and player settings. Review spells,
prayers and boosts. Source revision and notes stay with the draft.

Inventory pairing ignores standalone switch-weapon recommendations when the remaining
full loadouts and inventories have an unambiguous order (including Araxxor/Strategies).
If a guide remains ambiguous, **Inventory from guide** lets you explicitly choose its
inventory. Unknown items and quantities remain visible. Import opens **Prepare loadout**
so the inventory and pouch are immediately available; Cancel closes the add form.
To recover an older empty Araxxor import, add a fresh Wiki draft from the corrected guide.

**Equipment → Prepare loadout** contains inventory planning, rune pouch, optional switches,
supply checks, Wiki source notes and **Create Bank Tags layout**. Click a slot to edit item
and quantity. Nonstackables require separate slots; zero quantity means unknown. **Pack**
places an optional switch into an empty inventory slot. Planning is separate from observed
inventory. Supply checks combine worn, packed and pouch quantities; they cannot infer live
pouch contents, charge state, trip duration or consumable usability.

**Create Bank Tags layout** opens an eight-column preview. Keep the bank open, enable the
core Bank Tags plugin and **Use Tag Tabs**, then choose **Create bank tab**. Invalid names,
profile changes and existing tags/tabs/layouts are refused. Failures remain visible for
retry; failed writes roll back only newly created data. Success opens the new bank layout.
Positions contain item IDs, not quantities. Nothing is withdrawn or equipped automatically.

## Data and validation

The plugin reads RuneLite events; capture, ownership and profile refreshes do not use polling
UI timers. Draft/template/scan-time metadata uses profile configuration and may synchronize
through RuneLite. Full bank contents are kept in session memory; older saved bank snapshots
are retained for compatibility but are never treated as fresh availability.

Explicit Wiki requests contain search terms/page titles; no character or bank data is sent.
Calculation data is bundled, with no executable downloads or additional runtime dependencies.

`build` runs pinned fixtures, calculation/optimizer tests, fake live-state capture tests and
headless Swing interactions. There are no excluded Mockito classes. The optional candidate
snapshot test skips unless supplied through `candidateParity`. Rendered UI previews appear
in `build/ui`. [RELEASE-REVIEW.md](RELEASE-REVIEW.md) records release checks.

Automated tests do not establish in-game verification. Test the exported client for sidebar
clicks/scrolling, live/draft overlay selection, plugin/sidebar toggles, login/logout, hopping,
profile changes, fresh bank generation and Bank Tags creation. No code automates game input.

The [Collection Log boss import audit](BOSS-WIKI-AUDIT.md) records coverage and guide-specific
exceptions. Normal tests replay 61 pinned Wiki pages, 33 component-NPC searches, and every
parsed draft through inventory persistence. To regenerate the detailed offline report from
this plugin directory:

    ./gradlew.bat bossWikiAudit -PbossWikiAudit.input=src/test/resources/boss-wiki -PbossWikiAudit.output=build/boss-wiki-audit.json

To review current Wiki revisions in a separate directory (Python 3, public read-only requests):

    python tools/fetch-boss-wiki.py tools/boss-catalog.json build/boss-wiki-current --refresh --components
    ./gradlew.bat bossWikiAudit -PbossWikiAudit.input=build/boss-wiki-current -PbossWikiAudit.output=build/boss-wiki-current.json -PbossWikiAudit.itemNames=src/test/resources/boss-wiki/item-names.json

The fetcher checks that the live Collection Log Bosses category still matches the catalog.
New/removed entries require reviewing its page mappings. It never overwrites the pinned test
fixtures. Review changed pairings before updating `expected.json`; a successful parse alone
does not establish that an inventory belongs to a particular method. The optional offline
item-name snapshot lacks the client's note/placeholder/stackability metadata, so it validates
name coverage and persistence, not every live item variant or unspecified supply quantity.
