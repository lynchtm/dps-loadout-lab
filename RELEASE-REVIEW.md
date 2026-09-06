# Plugin Hub release review — September 5, 2026

Release name: **DPS Loadout Lab**. Independent GPL-3.0 derivative; upstream code,
fixtures, data and artwork are attributed in NOTICE.md. Existing `dpscalc` configuration
keys and profile data are retained so the rename does not discard development settings.

## Fixed submission blockers

* Replaced explicit Gson/GsonBuilder construction with RuneLite's shared Gson and a
  derived serialization configuration. The Hub disallows those constructors.
* Migrated `ItemManager.getItemStats(int, boolean)` to the supported single-argument
  client API. The old overload is explicitly disallowed for new submissions. The consolidation below removes the redundant fallback bonus calculation; ScenarioCalculator prepares equipment.
* Added LICENSE and NOTICE to main resources so they are included when Hub `build=standard`
  replaces the development Gradle script. Added the attributed 16×16 root icon.
* Removed the redundant remote monster-data/cache path, which could only accept the same
  pinned data already bundled. Calculation data comes from classpath resources. The subsequent template feature
  below adds explicit public Wiki guide requests.
* Updated public metadata and documentation, including RuneLite configuration synchronization
  behavior and the distinction between supported calculations and complete current Wiki parity.
* Replaced the legacy comparison dialog's `printStackTrace` with an SLF4J warning after
  the remote Hub scanner requested logger-based error reporting.

## Initial preflight validation

* Java 11 build: 449 tests, 448 passed, one optional external fuzz test skipped.
  Three inherited Mockito integration classes were excluded in this initial revision; the consolidation below replaces active coverage and removes those exclusions.
* Built main source using the official Plugin Hub standard Gradle template, target init
  script and v3 API recorder against the Hub's RuneLite version **1.12.38**.
* Decoded the recorder output and matched all 325 symbols against the current Hub
  `disallowed-apis.txt`, including its regular-expression rules: **zero matches**.
* Inspected the standard-build JAR: 142 Java 11 classes, 695,259 bytes, no classes in the
  reserved `net.runelite` namespace; LICENSE and NOTICE match their source files.
* Inspected the rendered 225-pixel sidebar after the rename; the title fits.

The remote build subsequently passed on [submission PR #16107](https://github.com/runelite/plugin-hub/pull/16107).
The separate Hub scanner requested the logging fix above. Each update is tested and
exported from a clean workspace commit; the PR's current checks remain authoritative.
Maintainer review is still required.

## Behavior and review scope

The plugin reads player/NPC/container state through RuneLite APIs and events. Its NPC menu
action opens a calculator target. UI choices affect calculation drafts only; it does not
equip items, change prayers, cast spells, attack, withdraw bank items or automate game input.
No reflection, subprocesses, native libraries, downloaded executable code or new third-party
runtime dependencies are used. The subsequent template feature makes explicit public Wiki
HTTPS requests through RuneLite's provided OkHttp client; character/bank data stays local.

Live analysis and the bounded/cancellable bank search run on background workers with stale
result checks. The plugin removes its overlay/navigation and shuts down workers when disabled.
Drafts, templates and bank provenance use profile configuration and may synchronize through
RuneLite. Bank availability requires a fresh visit after login or hopping.

## Remaining limitations and manual checks

* The engine/data reference is pinned to July 9, 2026. COVERAGE.md identifies later Wiki
  changes, unsupported mechanics and affected-item warnings. This is not a complete current
  Wiki port. Bank suggestions inherit those limitations and incomplete item requirements.
* Large equipment pools use a bounded heuristic. The UI distinguishes best-found searches
  from exhaustive searches. Spells, unlocks, runes and charge state require player review.
* Group DPS assumes identical monsters in range of supported burst/barrage spells; it is
  an analytical estimate, not a movement/stacking simulator or in-game area indicator.
* Show sidebar now updates immediately when configuration changes.
* Prior interactive testing was performed by the user during development. Compilation and
  headless tests do not establish in-game verification of this release. Recheck the exported
  client: load current player, generate a bank setup, switch targets, toggle overlays and
  the plugin, log out/in, hop worlds and inspect logs. No automated game inputs are used.

New submissions require RuneLite maintainer review. A successful CI build is not a promise
of approval or automatic merge; keep fixes in the same submission PR.

## Review references

* [Plugin Hub submission and build requirements](https://github.com/runelite/plugin-hub)
* [Plugin Hub tooling](https://github.com/runelite/plugin-hub-tooling)
* [Disallowed API rules](https://github.com/runelite/plugin-hub-tooling/blob/master/package/src/main/resources/net/runelite/pluginhub/packager/disallowed-apis.txt)
* [Rejected or rolled-back features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)

Official v3 tooling bundle SHA-256:
`eb0961b7cd0a1e4a351fb0f684731be1a9049167bfb49a2d91a760fea2933fd2`.

## Wiki templates / supplies follow-up

This follow-up adds attributed BSD-licensed pure parsers/resolvers, optional Wiki guide
requests, inventory quantities and safe creation of new core Bank Tags layouts. It is
prepared for local testing separately from the initial Hub submission. The new Bank Tags
write refuses disabled core integration, changed profiles, existing layouts, tabs or tags;
failed writes attempt to roll back only the new name. It does not automate game actions.

Automated coverage includes the search/preview/import UI flow, stale profile invalidation,
template copy isolation, real Wiki page fixtures, cosmetic eligibility and unmet levels,
quantity/pouch audits, packing switches, safe bank writes/rollback, bounded network input,
cache eviction and explicit offline fallback. Expanded panels are rendered at 213 pixels.
Live Wiki search and the Nechryael guide endpoint were checked directly; strategy-page
discovery uses two supported hastemplate queries, rather than an unsupported OR expression.

Follow-up validation: **648 tests, 647 passed, one optional test skipped**. The official
Hub standard build and v3 API recorder passed against RuneLite 1.12.38: **388 recorded
API symbols, zero disallowed matches**, 200 Java 11 classes. The standard-built JAR
contains the matching NOTICE and BSD attribution; no reserved RuneLite-namespace classes,
reflection, subprocesses or stdout/printStackTrace calls were found in main source.
The existing remote PR checks describe the initial submission, not this local follow-up.

The first live launch exposed a core-plugin dependency-scope error in the Bank Tags
integration. The fix resolves services lazily from the active Bank Tags plugin's own
injector, rather than creating them in this plugin's scope. It additionally requires
Bank Tags to be active as well as enabled. Repeated tests passed; the updated Hub API
record has **391 symbols and zero disallowed matches**.

The final ownership-refresh regression brings the suite to **649 tests: 648 passed,
one optional test skipped**. Ownership indicators update after bank/container changes
without modifying the comparison draft. The repeated final Hub build retains zero
disallowed API matches.

Bank layout button repair: creation now uses a persistent preview with inline errors,
retry and duplicate-click protection. Default names are shortened to the allowed limit.
The writer requires an open bank, enabled tag tabs, and a complete loaded tab list before
saving, preserving existing tabs when the core interface has not initialized. A successful
write opens the new layout through RuneLite's Bank Tags API. Regression tests cover invalid
names, retry after failure, changed profiles and incomplete tab initialization.

Bank layout repair validation: **653 tests: 652 passed, one optional test skipped**.
The official Hub packaging build records **408 API symbols, zero disallowed matches**.

Option B UI: separate slate cards for Templates, Loadout, Target, Results and Hit distribution; grouped draft actions below the editor; gold active states; labeled responsive editor tabs and readable empty slots. Native header clicks and tab geometry verified at 225 and 320 px. Full suite: **654 tests, 653 passed, one optional skip**. Official Hub build: **408 API symbols, zero disallowed matches**. Screenshots reviewed; in-game appearance still requires user verification.


## Consolidation follow-up (local QA)

Single ScenarioCalculator preparation for live/comparison output, explicit overlay source,
shared TTK, visible known limitations and conservative optimizer inclusion. One action menu,
one preparation area, four default metrics and lazy graphs. Cached calculations ignore
names/packing; optimizer insertion keeps current packing. Draft persistence is immediate
and deduplicated. Profile/container events replace UI polling timers; only scan timestamps
are newly persisted. Live sync belongs to one draft. Combat measurements use one tested
activity window, including misses, with damage and kill counts reset together.

Inactive UI classes and duplicate live calculation helpers are removed. Legacy GearSnapshot
and its builder now exist only in test fixtures. Active state capture uses a small fakeable
read boundary; formerly excluded Mockito tests are replaced or removed with the obsolete UI.
Candidate upstream snapshots have an explicit replay task and documented provenance workflow.
No new engine parity or in-game verification is claimed by this refactor.

Validation results are recorded after the final build below. This follow-up is prepared for
local testing; the existing remote PR still describes its submitted revision.

Final consolidation validation: **667 tests, 666 passed, one optional candidate test skipped**;
no excluded test classes. The candidateParity task separately replays all **150 canonical
pinned cases** successfully. The older adapter fixture copy is intentionally not used as
an authoritative replay document: strict replay rejected its missing output fields.
The final official Hub standard build records **370 API symbols, zero disallowed matches**,
**196 Java 11 classes**, and matching bundled license/attribution resources. Narrow sidebar
and generated-loadout renders were inspected. These checks establish local build/API and
regression results; they do not establish current-Wiki parity or game verification.

Comparison tab controls: each draft now has an adjacent × button; removal is no longer
in the global menu. Closing an inactive draft preserves the selected draft, closing the
selected draft chooses its neighbour, and the final draft cannot be removed. Regression
coverage includes native clicks at narrow/wide widths and saved-template isolation.
Validation: **668 tests, 667 passed, one optional candidate test skipped**.


Araxxor inventory and target-first workflow: revision 15322538 includes a standalone
Araxyte weapon recommendation before two full loadouts. The previous whole-page pairing
rejected its three-equipment/two-inventory sequence. The fallback now validates original
parser counts and pairs only retained full loadouts; orphan/ambiguous inventories remain
unpaired. The exact Wiki revision is retained as an attributed regression fixture.

Section order is Templates → Target → Loadout → Results → Hit distribution. Wiki search
is opened only through the loadout + menu; it searches the selected target by default and
permits manual queries without changing the target. Preview includes inventory/pouch and
an explicit inventory choice for ambiguous guides. Import opens preparation immediately.
Changing method/inventory or cancelling invalidates in-flight preview work.

Final validation: **671 tests, 670 passed, one optional candidate test skipped**. Tests
cover the exact Araxxor page, correct melee/ranged pairing, explicit inventory changes,
preview/insertion with pouch quantities, independent original drafts, editable search and
unchanged calculator target. Rendered narrow layouts were inspected. Official Hub standard
packaging: **370 recorded API symbols, zero disallowed matches, 198 Java 11 classes**;
license/attribution resources match. Reimport an older empty Wiki draft to recover its guide
inventory; existing plans are not overwritten automatically.
