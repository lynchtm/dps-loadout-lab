# BSD runtime replacement — September 9, 2026

The runnable plugin now uses the replacement engine. The previous foundation-only milestone is superseded. No legacy engine is loaded as a fallback.

The file-by-file baseline audit identified 98 GPL-derived Java files, 10 inherited calculator data/fixture files and 59 imported images. All 167 were removed. New runtime code lives under `com.loadoutlab`; locally authored workflows and the separately attributed BSD Wiki parser were adapted to its contracts. Persisted draft/template field names and configuration group remain compatible.

The software license applies BSD-2-Clause to the replacement revision. Wiki content and game assets retain separate attribution. Historical commits are not relicensed. See PROVENANCE.md for the authoring method and its limitations; this is not a formal clean-room certification or maintainer approval.

The independent probability foundation remains separately testable, but is also under the ordinary main source tree so Plugin Hub's standard build includes it. The full plugin is the deliverable, not the foundation-only JAR.

RELEASE-REVIEW.md records automated checks. COVERAGE.md records calculation limits: replacing the runtime does not establish full OSRS Wiki calculator parity. User testing of the exported client precedes promotion to the public release and existing Plugin Hub submission.
