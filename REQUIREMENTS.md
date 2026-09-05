Build a complete native RuneLite implementation of the current OSRS Wiki DPS calculator.

This should be treated as a full-featured product implementation, not a reduced MVP. Preserve the calculator’s breadth and behavior as completely as practical, including advanced combat mechanics, special effects, comparisons, graphs, and detailed calculation explanations.

The plugin should operate as a native RuneLite sidebar panel and allow the player to perform DPS analysis without leaving the client.

## Player state

Automatically load and update the player’s current state from RuneLite whenever possible:

- Equipped items
- Inventory items relevant to combat
- Ammunition
- Combat levels
- Current and boosted combat levels
- Hitpoints
- Prayer level and current prayer state
- Active spellbook
- Selected spell
- Current attack style
- Current target
- Slayer task state
- Relevant quest, diary, league, and unlock state
- Current equipment bonuses and weapon properties

Every automatically detected value should be manually overrideable.

The plugin should distinguish between:

- Values loaded from the live client
- Values manually overridden by the user
- Values inferred from equipment or configuration
- Values that are unavailable or unsupported

## Target management

Support complete target management functionality:

- Detect the NPC currently being attacked
- Detect the NPC currently interacting with the player
- Add “Calculate DPS” to relevant NPC right-click menus
- Select targets from a searchable NPC list
- Support NPC variants and alternate forms
- Preserve target-specific attributes and mechanics
- Load targets from Wiki-derived data
- Create and save custom NPC definitions
- Manually edit all relevant target stats

Target configuration should support:

- Name and variant
- Hitpoints
- Current hitpoints
- Attack level
- Strength level
- Defence level
- Magic level
- Stab defence
- Slash defence
- Crush defence
- Ranged defence
- Magic defence
- Ranged defensive bonuses
- Size
- Slayer category
- Undead, demon, dragon, kalphite, and other attributes
- Immunities and weaknesses
- Defence reductions
- Target-specific damage modifiers
- Raid-specific target modifiers
- Phase-specific values where applicable

The user should be able to save commonly used custom targets and restore default target values.

## Loadouts

Support multiple independent loadouts, with at least the equivalent of the web calculator’s comparison capacity and room for future expansion.

Each loadout should support:

- Name and description
- Equipment in every slot
- Inventory items relevant to calculations
- Ammunition
- Weapon and attack style
- Spell and spellbook
- Prayers
- Boosts and potions
- Special attack configuration
- Slayer-task configuration
- Raid configuration
- Player health and other situational values
- Copying from another loadout
- Loading from current RuneLite state
- Resetting to current RuneLite state
- Saving and restoring presets
- Reordering loadouts
- Removing loadouts
- Comparing loadouts side by side

Where practical, allow item searching from:

- RuneLite’s item data
- Current inventory
- Current equipment
- Bank contents
- Saved item presets

The plugin should make it easy to test questions such as:

- Whether an upgrade improves DPS
- Whether a different weapon style is better
- Whether a prayer switch is worthwhile
- Whether a defensive item costs too much DPS
- Which ammunition is optimal
- Which gear setup performs best against a specific NPC

## Combat styles

Support complete calculations for:

- Melee
- Ranged
- Magic

Melee should include:

- Stab, slash, and crush styles
- Strength and attack styles
- Weapon attack speeds
- Multi-hit weapons
- Two-handed weapons
- Halberds
- Set effects
- Special attacks
- Weapon-specific accuracy and damage behavior
- Target weaknesses
- All relevant gear and prayer bonuses

Ranged should include:

- Bow weapons
- Crossbows
- Thrown weapons
- Chinchompas and area attacks where supported
- Ammunition selection
- Bolt and arrow effects
- Toxic blowpipe behavior
- Twisted bow behavior
- Bowfa and crystal equipment
- Dragon hunter effects
- Ranged strength interactions
- Weapon-specific attack styles
- Special attacks
- Target weaknesses

Magic should include:

- Standard spellbook spells
- Ancient spells
- Lunar and Arceuus spells where relevant
- Powered staves
- Tridents
- Tumeken’s Shadow
- Special spell damage behavior
- Magic accuracy
- Magic damage
- Elemental weaknesses
- Powered staff attack speeds
- Spell-specific effects
- Special attacks and alternate attacks where applicable

## Prayers, boosts, and conditions

Support all relevant combat configuration:

- Offensive prayers
- Defensive prayers that affect calculations
- Protection prayers where relevant
- Ancient prayers
- Curses or alternate prayer systems if applicable
- Stat boosts
- Temporary drains
- Overloads and potion effects
- Divine potion behavior
- Dragon battleaxe boosts
- Forgotten brew effects
- Imbued heart effects
- Saturated heart effects
- Time-dependent boost decay
- Player current hitpoints
- Target current hitpoints
- Target defence-reduction state
- Player and target status conditions where they affect damage

Include clear controls for toggling each condition and show which modifiers are active.

## Equipment and item effects

Support the full range of relevant equipment behavior, including:

- Slayer helmet and imbued Slayer helmet
- Salve amulets
- Void and elite Void
- Crystal armor
- Inquisitor’s armor
- Dharok’s set
- Obsidian equipment
- Justiciar and defensive sets
- Barrows set effects
- Torva and other strength-scaling sets
- Dragon hunter weapons
- Arclight and Emberlight
- Keris variants
- Osmumten’s Fang
- Scythe of Vitur
- Tumeken’s Shadow
- Twisted Bow
- Toxic blowpipe
- Soulreaper Axe
- Ghrazi rapier and related weapons
- Blood fury
- Rings and imbued rings
- Capes
- Ferocious gloves and comparable upgrades
- Weapon-specific bonuses
- Item degradation or charge state where relevant
- Ornament kits and variants where stats differ

The implementation should follow the official calculator’s treatment of item effects rather than relying only on visible RuneLite equipment bonuses.

## NPC and content-specific modifiers

Support content-specific behavior wherever represented by the official calculator, including:

- Slayer task bonuses
- Undead bonuses
- Demon bonuses
- Dragon bonuses
- Wilderness weapon bonuses
- Boss-specific weaknesses
- Corporeal Beast mechanics
- Barrows mechanics
- Tombs of Amascut raid level and modifiers
- Chambers of Xeric scaling
- Theatre of Blood modifiers
- Nightmare and Phosani’s Nightmare behavior
- God Wars Dungeon mechanics
- Combat achievement or league modifiers where applicable
- Demonic or seasonal game modes where supported
- NPC phase changes where required for accurate calculations

Unsupported mechanics must be clearly identified rather than silently ignored.

## Special attacks

Support special attacks comprehensively, including:

- Special attack energy requirements
- Expected special damage
- Accuracy changes
- Defence-reduction weapons
- Multi-hit specials
- Multiple special attacks within a scenario
- Special attack frequency
- Special attacks used before normal attacks
- Loadout comparisons with and without special attacks
- Expected damage per special
- Expected DPS over a configurable rotation

Where a special attack cannot be represented exactly, expose the limitation clearly and provide the closest supported model.

## Results

Display detailed results for every loadout:

- Max hit
- Minimum hit where applicable
- Average hit
- Accuracy
- Hit chance
- Attack speed
- Attack interval in ticks
- DPS
- Damage per tick
- Expected damage per attack
- Expected special attack damage
- Average time to kill
- Kill-time variance where supported
- Damage taken per second
- Damage taken per kill
- Resource or prayer duration where supported

Support sorting and highlighting by:

- DPS
- Accuracy
- Max hit
- Time to kill
- Damage taken
- Special attack damage
- Resource efficiency

Show meaningful empty and unsupported states. Never render misleading zeroes, NaN, infinity, or incomplete results without explanation.

## Detailed formula breakdown

Provide an expandable calculation breakdown for each combat style and loadout.

Include:

- Base levels
- Boosted levels
- Prayer multipliers
- Style bonuses
- Void bonuses
- Effective attack
- Effective strength
- Attack bonus
- Strength bonus
- Attack roll
- Defence roll
- Accuracy calculation
- Max hit calculation
- Damage multipliers
- Attack speed
- Special-effect modifiers
- Final average hit
- Final DPS calculation

The breakdown should identify which equipment, prayer, NPC attribute, or condition caused each modifier.

## Hit distributions and graphs

Reproduce the calculator’s analytical visualizations where practical:

- Hit distribution
- Damage-per-hit distribution
- Loadout comparison graph
- DPS versus target defence
- DPS versus player level
- DPS versus boost duration
- Time-to-kill distribution
- Damage-taken graph
- Comparison of multiple loadouts
- Current HP and overkill behavior

Graphs should update when the target, loadout, or conditions change.

Allow copying or exporting the underlying result data where useful.

## Presets and sharing

Support:

- Saved target presets
- Saved loadout presets
- Saved combat scenarios
- Named comparisons
- Import/export of scenarios
- Copying a shareable scenario representation
- Restoring a scenario later
- Resetting all fields to current RuneLite state

Presets should survive RuneLite restarts and be associated with the appropriate RuneLite profile where possible.

## RuneLite integration

Provide convenient native interactions:

- Sidebar icon
- NPC right-click “Calculate DPS” option
- Current-target loading
- Current-equipment loading
- Current-stat loading
- Current-prayer loading
- Current-spell loading
- Optional automatic refresh
- Optional overlay showing current DPS against the active target
- Optional infobox showing DPS, accuracy, and time to kill
- Optional display of the active loadout
- Optional comparison overlay for the selected target

All overlays and automatic refresh behavior must be configurable.

The plugin must not:

- Automatically equip items
- Automatically attack NPCs
- Automatically consume items
- Automatically change prayers
- Interfere with combat inputs
- Perform bot-like actions

## Data behavior

Use the official calculator’s item, NPC, and mechanic behavior as the reference.

The plugin should provide clear attribution for Wiki-derived data and preserve all required open-source license notices.

When data is missing or outdated:

- Identify the affected item or NPC
- Explain what is unavailable
- Allow manual entry where possible
- Avoid silently falling back to incorrect generic behavior

## Parity expectations

The calculator should aim to match the current OSRS Wiki calculator’s supported behavior, including edge cases and special effects.

Maintain parity coverage for:

- Standard melee
- Standard ranged
- Standard magic
- Prayers
- Boosts
- Slayer effects
- Salve effects
- Void
- Crystal equipment
- Twisted Bow
- Tumeken’s Shadow
- Osmumten’s Fang
- Scythe of Vitur
- Bolt procs
- Special attacks
- Defence reductions
- Boss and raid modifiers
- Monster attributes
- Hit distributions
- Time-to-kill calculations
- Damage-taken calculations

Any intentional difference must be visible in documentation and, where relevant, in the UI.

## Quality expectations

Treat this as a complete, maintainable RuneLite plugin rather than a proof of concept.

Prioritize:

- Accurate behavior
- Clear calculation explanations
- Responsive UI
- Stable RuneLite integration
- Graceful handling of unsupported states
- Persistent presets
- Comprehensive test coverage
- Easy addition of future weapons, NPCs, and mechanics
- Clear user-facing error states
- Correct licensing and attribution

Do not reduce the feature set to a minimal initial release unless a specific technical limitation makes a feature impossible. If implementation must be staged, preserve the full functional requirements as the target product and sequence the work internally.