# Plugin Hub release review — September 5, 2026

Release name: **DPS Loadout Lab**. Independent GPL-3.0 derivative; upstream code,
fixtures, data and artwork are attributed in NOTICE.md. Existing `dpscalc` configuration
keys and profile data are retained so the rename does not discard development settings.

## Fixed submission blockers

* Replaced explicit Gson/GsonBuilder construction with RuneLite's shared Gson and a
  derived serialization configuration. The Hub disallows those constructors.
* Migrated `ItemManager.getItemStats(int, boolean)` to the supported single-argument
  client API. The old overload is explicitly disallowed for new submissions. The legacy
  integer magic-bonus fallback preserves RuneLite's former adapter behavior; equipment
  preparation still applies the pinned calculation model.
* Added LICENSE and NOTICE to main resources so they are included when Hub `build=standard`
  replaces the development Gradle script. Added the attributed 16×16 root icon.
* Removed the redundant remote monster-data/cache path, which could only accept the same
  pinned data already bundled. Runtime data now comes exclusively from classpath resources.
* Updated public metadata and documentation, including RuneLite configuration synchronization
  behavior and the distinction between supported calculations and complete current Wiki parity.
* Replaced the legacy comparison dialog's `printStackTrace` with an SLF4J warning after
  the remote Hub scanner requested logger-based error reporting.

## Initial preflight validation

* Java 11 build: 449 tests, 448 passed, one optional external fuzz test skipped.
  Three inherited Mockito integration classes remain excluded, as documented in README.
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
No reflection, subprocesses, native libraries, downloaded executable code, direct network
requests or new third-party runtime dependencies were found in main source.

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
* Changing the Show sidebar configuration currently requires toggling the plugin.
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
