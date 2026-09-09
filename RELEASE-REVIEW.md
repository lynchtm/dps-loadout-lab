# Replacement release checks — September 9, 2026

This report covers the replacement runtime in this source revision. It supersedes the previous GPL-runtime release report; it is not a record of Plugin Hub approval.

## Completed automated checks

- Clean Java 11 compilation and build; final workspace `tools/test-plugin.ps1 -Name wiki-dps` passes.
- 389 main tests and 12 isolated probability-foundation tests: 401 passed, zero failures/errors/skips. This includes saved-field compatibility, optimizer/UI/storage workflows, the pinned Collection Log guide corpus and independently derived combat examples.
- Headless Swing render inspection preserves Target, Loadout, Results, Hit distribution and bottom Templates cards. RuneLite supplies actual game sprites at runtime; headless renders use original fallback badges.
- Plugin Hub standard Gradle build and API recorder pass against RuneLite 1.12.38, using ordinary `src/main` without custom build source sets.
- Recorded 354 API symbols with zero disallowed matches against rules SHA-256 `a6d191d29c805b869d6a545fa88771e08979b199eb6800af0cc56b6deb77dec4`.
- Source/resource audit checks 230 relevant files with zero boundary violations. The standard-build JAR contains 163 Java 11 classes and 194 entries, no legacy engine packages, no reserved RuneLite classes, and all required replacement resources/notices. JAR SHA-256: `9221576bdba99f6cf02f3233e66a518c9a0fa21ee9861425ad2328342c62d954`.
- All 167 files identified for removal in the baseline provenance audit are absent. Historical revisions keep their original licenses. PROVENANCE.md documents retained BSD/local components and authoring-method limits.

## Release procedure and remaining checks

Export from a clean committed workspace with `tools/prepare-release.ps1 -Name wiki-dps`. Its generated RELEASE-SOURCE.md identifies the source commit, and its build reruns the full suite. Launch the exported dev client for user testing before public promotion. The public repository and Plugin Hub PR still reference the earlier release until explicitly updated.

Automated validation does not establish in-game verification. Manually test live gear refresh, saved templates, comparison edits, bank generation, Araxxor Wiki inventory import, Bank Tags creation, overlay/sidebar/plugin toggles, logout/login, hopping and character/profile changes. No game input is automated.

COVERAGE.md is part of this review: several raid, phase, advanced weapon/set and special-attack mechanics remain flagged or unavailable. The rewrite removes the old runtime; it does not establish complete Wiki formula parity or legal certification. Maintainer review remains separate.

## User testing

On September 9, 2026, the author reported that the requested client checks looked good after testing the exported replacement (`6a552c0d602480238aa2825dcc4a84e274623a2f`) on Cornnnnnn. This is user-reported smoke-test acceptance, not independent verification of every combat formula. Subsequent release changes add this record and the baseline audit metadata only.
