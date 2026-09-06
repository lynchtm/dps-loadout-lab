# DPS Loadout Lab

A native Java 11 RuneLite sidebar for combat scenarios, Wiki-derived DPS calculations,
loadout comparison, saved targets, and analytical graphs. GPL-3.0; see [NOTICE.md](NOTICE.md).

An independent community plugin, not an official OSRS Wiki product. It uses a pinned,
Wiki-derived calculation engine; current Wiki parity is not claimed. The engine and
item/NPC data reference `b6bc098dc0d742b2b763375d2e78e1b611a22070` (July 9, 2026).
[COVERAGE.md](COVERAGE.md) lists model limitations and remaining work. Bank optimization
reports the best setup found within its eligible pool and search budget.

## Build and run

Requires Java 11. From this repository's root:

```powershell
./gradlew.bat build
./gradlew.bat run
```

On macOS/Linux use `./gradlew`. Development is maintained in a multi-plugin workspace;
this repository is a standalone export, with provenance in RELEASE-SOURCE.md.

## Use

Enable DPS Loadout Lab, open its sidebar, and choose **Load current player**. Use **+** to
start a comparison tab from a template, your current player, or a blank loadout; the **•••** menu provides duplicate, rename, reorder and delete.
The sidebar follows the Wiki layout: Templates, Loadout, Target, Results, then Hit distribution.
Orange marks active loadouts; green marks the best comparison values.

### Comparison drafts and templates

The numbered tabs under **Loadout** are independent comparison loadouts.
Click a tab to edit it; hover for its full name. Rename the selected loadout in the
name field below the tabs, then press Enter or click away. Saved templates are unaffected. Overflow tabs scroll horizontally.
**New from current player** adds a tab; **Load current player** reloads the selected tab.
The loadouts are working copies. Edit or duplicate them freely;
the workspace recovers after a restart without adding those drafts to your saved library.

Open the collapsible **Templates** section at the top, above the calculator, to pick a melee, ranged or magic starter, or one of your saved templates.
**Add to comparison** creates a separate draft and keeps the selected target. Starters use
the current draft's base levels and sample equipment; review availability, prayers and boosts.
Saved templates retain their saved equipment, stats and settings, including empty slots.
Live synchronization preserves these snapshots; use **Load current player** explicitly
if you want to replace them with live values.

On any comparison tab, click **Save as template**, enter a name and choose
**Save new template** to keep that tab for reuse. Selecting a saved
template and clicking **Replace saved** is the only way to update that template. Deleting a
template leaves existing comparison drafts intact. Templates are stored separately from
named scenarios and isolated by RuneLite/RuneScape profile. Existing scenarios are retained.

### Editing a draft

The loadout editor has five icon tabs in Wiki order. Hover for a tab's name;
its title appears above the icons. The editor height follows the active tab.

- **Combat:** choose an attack style available to the equipped weapon. Spell selection
  appears for autocast-capable weapons. Equipment changes preserve compatible styles.
- **Skills:** edit base levels and boosts; select multiple potions under **Potions & boosts**.
- **Equipment:** click a slot, search by name or ID, and double-click to equip. Right-click
  to clear a slot. Search results appear while typing. Filters include the catalog,
  inventory, equipped items, last-opened bank and comparison drafts. Two-handed weapons
  and shields exclude each other. Offensive, defensive and other bonuses are calculated
  from equipment, including before a target is selected.
- **Prayer:** toggle the Wiki prayer-book icons. Conflicting prayers switch off automatically.
- **Settings:** Slayer task, Wilderness, special attacks, loaded dart ID, live synchronization
  and advanced settings. **New from current player** adds another comparison loadout.

Below the loadout editor:

- **Target:** search by NPC name or ID, choose the current target, or create a custom target.
  **Monster stats** starts collapsed and contains icon-based skill and defence inputs,
  offensive magic accuracy, attributes, defensive reductions and monster settings.
  Changes affect the calculator target. Expanded sections remain open while editing.
  The NPC's **Calculate DPS** menu action also selects it.
- **Results:** Max hit, DPS, average TTK, accuracy and special damage appear as rows, with
  loadouts as columns. Green highlights best values and ties; lower TTK is better.
  **Show more** reveals additional metrics; **Expand comparison** opens a wider snapshot.
  Click a column to edit its loadout. **Calculation notes** contains coverage limitations
  and detailed calculation output.
- **Hit distribution:** a separate collapsible chart section. The selector also provides
  the other available analyses. Hit distributions share a damage axis across loadouts.

**Keep live stats in sync** refreshes readable client values while preserving overrides.
**Load current player** explicitly clears the selected draft's overrides and reloads live values, including cleared equipment slots. **••• → Reset from live player** does the same. Live capture does not infer unknown
quest/unlock state, Slayer-target matching, loaded darts or autocast state. Live target HP
may be estimated from a health ratio. Item bonuses use the pinned Wiki catalog.

**••• → Advanced settings** retains all player, target and condition fields, including raid
settings, attributes, elemental weaknesses, defence reductions, rotations and incoming
attacks. Double-click to edit a field; hover to inspect its path and source. Named target
presets, scenario import/export and CSV exports are also in the **•••** menu. These edits
change calculator scenarios only and never interact with game equipment or prayers.

Graphs cover damage distributions, comparisons, target defence, player levels, elapsed boost
time, bounded kill distributions and configured incoming damage. The kill graph contains
100 attack bins followed by a surviving-tail bin; it does not renormalize the visible bins.
CSV exports use series number and sample index; axis sampling is described in the graph title.

Save named scenarios, restore them, copy JSON to the clipboard, or paste JSON to import.
Data is stored through ConfigManager per RuneLite profile and RuneScape profile when known.
Profile changes reload that profile's workspace and turn off live refresh. The import format
is native to this plugin; it is not the web calculator's sharing URL format.

New loadouts default to **On a Slayer task**. Existing saved choices are preserved.
The prayer book uses the Wiki's four-column icon layout for supported combat prayers;
hover an icon for its name. Potions allow multiple selections and use the highest boost
per stat, with the shared elapsed-time and divine settings. Overlapping boosts do not add.
This follows the reference Wiki's maximum-boost model, including its omission of negative
brew penalties in multi-selection. Untouched legacy single-potion scenarios retain their
previous calculations. Target, Results and Hit distribution expand/collapse independently below the loadout editor.

## Generate equipment from your bank

Open your bank once after login or a world hop, select your target, and open
**Equipment → Generate from bank**. Choose **Melee**, **Ranged** or **Magic** before
generating. Magic automatically compares supported autocast spells across spellbooks
with powered-weapon attacks; there is no spell input. It checks the draft's boosted
Magic level and staff/spell compatibility, and shows the recommended spell and spellbook
under the equipment preview. The generator searches normal DPS and scores weapon styles against
that exact target, including custom defensive stats, weaknesses, raid scaling and
reductions, while retaining the draft's levels, HP, prayers, boosts and conditions.
It retains the draft's prayer profile and potions; adjust these for the chosen combat type.
Magic scoring considers elemental weaknesses and restricts Demonbane/Crumble Undead to
appropriate targets. Spellbook access, quests and rune supplies still need confirmation.
The spell search covers the attributed level/compatibility table and this engine's damage
model; manual casting, Magic Dart and support-spell rotations are not searched.

**Target → Encounter** selects **Single target** or **Grouped targets**. Grouped mode
shows a count of 2–9 monsters (initially 5, editable) and ranks the generator by estimated
group DPS. Supported Ancient burst/barrage spells count damage across that many identical
monsters kept in range; powered weapons and other attacks count once. Other area attacks
such as chinchompas and bouncing weapons are not modeled as group attacks yet. The Nechryael
target variants offer **Use grouped encounter** as an explicit shortcut; selecting a
monster never automatically enables multicombat assumptions.

The encounter is shared by all comparison loadouts, saved with the workspace/scenario,
and included in generator snapshots. Changing its mode or count invalidates a generated
preview. Older scenarios default to single target. In grouped mode the preview and results
table show per-monster DPS separately from estimated group DPS; CSV also includes the group
count and targets hit. Kill times, max hits and graphs remain per monster. Group estimates
exclude gathering, looting, respawn delays, mixed monster stats and attacks that miss the pile.

**Keep equipment** locks any selected slots, including empty slots. A missing or excluded
locked item stops the search with an explanation. The available pool combines observed
bank, inventory and equipped quantities; placeholders are ignored and noted items map
to their unnoted identity. Charged variants remain distinct. Snapshots are isolated by
character, saved through RuneLite profile settings for provenance, and require a new bank visit after login/hopping.
Changes to ownership, profile, the draft, target or locks invalidate pending previews.

Known skill requirements are checked against captured real player levels. Unknown
requirements are excluded unless the item is currently equipped or **Include unverified
requirements** is selected. That option never bypasses a known unmet level requirement.
The requirement dataset is pinned and incomplete; it is not proof of all current equipment
requirements. Quest unlocks, exact charge state, rune supplies and loaded darts need user
review. Uncharged/empty/inactive catalog variants are excluded. Blowpipes require the
manual dart ID in Settings; this is an assumption about loaded darts, not a charge scan.
Items absent from the pinned equipment catalog cannot be searched.
Non-combat weapons and items without a supported attack speed are excluded. Melee and
magic exclude projectile ammunition; ranged candidates only retain projectiles that their
weapon uses. Owned blessings remain eligible, with prayer bonus breaking equal-DPS ties.
Target-specific ammunition requirements are evaluated through the calculator's supported
monster mechanics.

The search runs in a cancellable background worker. Small candidate pools are enumerated;
large pools use weapon seeds, complete-set seeds and repeated slot refinement, capped at
20,000 scored combinations or approximately 15 seconds (plus finalist calculations).
No top-N per-slot filter discards set pieces. Scores use the normal DPS engine and selected
encounter objective without
TTK, graph and explanation work; finalists go through the complete calculator.
A full enumeration is identified as **All eligible combinations checked**. Otherwise the
result is **Best found**, not a proven global maximum. DPS is the primary objective;
equal DPS uses a secondary sum of defensive bonuses plus five times prayer bonus, then
stable item/style ordering. This tie-break is not an incoming-damage optimization.

Review the equipment figure (hover items for their names), DPS metrics and
**Review exclusions & notes**, confirm **I can use this setup**,
then choose **Add to comparison**. This creates a new independent draft and preserves
the original and all templates. Up to three distinct gear combinations are offered.
The winning spell is carried into each generated Magic draft and can be changed in Combat.
Generated stats/gear/styles are pinned against live synchronization until explicitly
reloaded. The result can be edited and saved as a template through the existing controls.
The DPS comparison is against the starting draft, which may itself contain unowned items.

Optimizer results inherit all engine/data limitations in COVERAGE.md. Exhaustive search
only establishes the best result within the eligible pool and the current calculator's
model; it does not establish current-Wiki parity or universal in-game usability.

## Validation

[RELEASE-REVIEW.md](RELEASE-REVIEW.md) records the Plugin Hub preflight, fixes and
remaining manual checks for the initial public release.

The Java build runs inherited deterministic fixtures and unit tests plus scenario, input,
distribution and headless Swing lifecycle tests. Three inherited Mockito-dependent integration
test classes are retained but excluded to avoid adding a new test dependency. The opt-in
external fuzz runner is skipped by default. These exclusions are not parity evidence.
The test renderer writes `build/ui/scenario-panel.png` and `build/ui/workspace-tabs.png`
for narrow-sidebar layout inspection. Interaction tests cover item search, two-handed
equipment conflicts, prayer replacement and scenario persistence.

Compilation does not verify the game. Please test login/logout, world hopping, enabling and
disabling the plugin, NPC menus, target changes, prayer/style changes, bank opening, manual
override preservation, profile switching, and all overlay toggles in RuneLite. Check the client
log for errors. No code equips, attacks, consumes items, changes prayers or automates inputs.

Release exports are generated only from a clean committed workspace using
`./tools/prepare-release.ps1 -Name wiki-dps`. A successful export build is not a Hub submission
or evidence that the full requirements or current combat parity are complete.

## Data and privacy

Equipment, inventory, bank snapshots and player levels are read from RuneLite events.
Scenario drafts, templates and bank provenance are stored using RuneLite's configuration
system and may sync through RuneLite when account synchronization is enabled. The plugin
has no direct network requests, external service, credentials, or game-input automation.
Monster and equipment reference data ship inside the plugin. Updating reference data
requires a reviewed plugin release; it is not downloaded at runtime.
