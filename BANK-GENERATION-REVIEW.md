# Bank generation fixes — 2026-09-19

The default requirements filter omitted berserker ring IDs 6737, 11773, 25264
and 26770. They now have explicit empty equip-level requirements, reviewed
against Wiki revisions 15182895 and 15345632 and retained by the data refresh
script. This does not assert acquisition or account-specific eligibility.

In bounded searches, high-ranked armour with unsupported mechanics could
invalidate every starting setup. Each weapon now also starts with only locked
gear, allowing refinement to find valid alternatives without relaxing coverage
or ownership checks. Ammunition variants continue through the existing search.

Advanced slot boxes are labelled **Lock Head**, **Lock Ring**, etc. They preserve
the starting item or empty slot; **Unlock all slots** clears them. Changing inputs
disables Add immediately, and a new search clears old alternatives before
validation so failed requests cannot leave a previous preview actionable.

Targets with known formula limitations, including Great Olm, now explain the
required **Include known formula limitations** opt-in before searching. This
change does not implement Olm damage rules or raid scaling. Opted-in results
remain labelled estimates. Automated tests cover raid failure, opt-in, and
subsequent normal-target generation; the friend's exact bank is unavailable.

## Validation

- Java 11 workspace build: 405 tests passed (393 main, 12 independent), no failures,
  errors or skips.
- New ring and bounded-search regressions both fail against the released source
  and pass with these fixes.
- Swing regression covers each advanced opt-in, an empty ring lock, clearing locks,
  an actionable raid error, and successful generation after changing targets.
- Standalone release build: the same 405 tests passed. Source/binary packaging
  audits passed with no problems.

## In-game checks before a Plugin Hub submission

On September 20, 2026, the author tested the fixed development client and reported
that things were working, then authorized publishing the release and opening the
Plugin Hub update PR. This is author-reported gameplay verification, not automated
game input. Individual checklist results were not separately recorded.

The requested local test checklist was:

1. Scan the affected bank and generate melee gear with an imbued berserker ring
   available. Confirm its actual variant appears when beneficial.
2. Toggle each advanced opt-in separately and together, then use slot locks and
   **Unlock all slots**. Confirm previews can be regenerated and added.
3. Select Great Olm's right hand: check the explicit limitation message, opt in
   to estimates, then switch to an ordinary target and generate again.
4. Confirm plugin disable/enable, overlay behavior, logout/login and world hopping
   leave the correct character's scan and generation controls usable.

Plugin Hub submission is authorized following that local test report.
