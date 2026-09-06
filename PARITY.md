# Reference maintenance

The committed fixtures and equipment catalog identify their exact reference SHA and domain
digest. A passing pinned replay establishes agreement for those cases, not agreement with
an unspecified latest Wiki. COVERAGE.md owns the capability inventory; CoverageWarnings
attaches known drift to affected results and optimizer eligibility.

## Candidate update workflow

1. Select and record an exact upstream Wiki commit in a separate checkout outside the release
   repository. Review upstream engine, equipment/NPC data, spells and item normalization as
   one change. Do not silently relabel the old model with a newer data SHA.
2. Generate a schema-v3 fixture snapshot from that upstream engine (never from this Java
   implementation). Include its `webCalcCommit`, `equipmentDomainDigest`, declared counts,
   raw inputs and expected rolls/distributions. Retain generator source, command and seed
   with the update evidence. The upstream generator may need adaptation; this project does
   not pretend that an absent `yarn sync-fuzz-fixtures` command is runnable locally.
3. Replay the candidate separately, without overwriting pinned test resources:

```powershell
./gradlew.bat candidateParity `
  -PfixtureCandidate.file=C:/fixtures/candidate.json `
  -PfixtureCandidate.sha=<exact-upstream-sha> `
  -PfixtureCandidate.digest=<equipment-domain-digest>
```

This task requires all three properties, verifies schema/provenance/counts, and reports
fixture failures with candidate SHA and case ID. Its report is in
`build/reports/tests/candidateParity/index.html`. An unrecognized schema or source is an
error, not a zero-case success. No network access or external generator is needed for replay.

4. Reproduce each mismatch as a small named case. Cover affected weapons against ordinary,
   resistant, weak, Slayer, raid-scaled and reduced-defence targets, plus relevant style,
   spell, ammo, set and special branches. Keep known drift flagged while unresolved.
5. Update formulas/catalogs, their provenance/digest, fixtures, NOTICE and COVERAGE together.
   Remove a CoverageWarnings rule only after focused upstream-generated regressions pass.
   Shared live/comparison and optimizer regressions must also pass.
6. Run the full build, prepare a clean standalone export, repeat Hub API packaging checks,
   then perform manual game verification. Preserve both pinned and candidate reports in the
   release evidence. Do not call exhaustive optimizer search proof of combat-model parity.

The consolidation pass changes orchestration and confidence reporting. It does not port the
newer Wiki formulas. Its replay task was validated using the already pinned v3 snapshot;
no new latest-Wiki fixture generation is claimed.

## Source formatting

Changed active Java sources use four-space AOSP formatting from the standalone
[google-java-format 1.17.0 release](https://github.com/google/google-java-format/releases/tag/v1.17.0):
`java -jar google-java-format-1.17.0-all-deps.jar --aosp --replace <files>`.
The formatter is a local development tool, not a plugin or build dependency.
