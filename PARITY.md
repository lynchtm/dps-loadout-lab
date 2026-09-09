# Independent validation and factual refresh

The replacement does not use GPL calculator output fixtures as a test oracle. `gradlew build` runs retained workflow regressions plus independently worked combat examples. `gradlew independentTest` isolates the Java-only probability foundation and exhaustively enumerates small integer contests. RULES.md records the gameplay specification; COVERAGE.md records remaining limits.

From the plugin directory, use Python 3 and a new cache for an updated snapshot:

```text
python tools/refresh-independent-data.py --cache build/facts --fetch
python tools/acquire-wiki-facts.py --cache build/facts
python tools/refresh-spell-facts.py --cache build/facts
python tools/acquire-wiki-facts.py --cache build/facts --equipment
python tools/refresh-requirement-facts.py --cache build/facts
python tools/refresh-style-facts.py --cache build/facts --fetch
python tools/audit-independent-release.py
```

Acquisition uses public RuneLite item exports and Wiki articles/Bucket facts, not either calculator's source or generated output. Existing cache files are preserved; use a new directory for new revisions. Inspect source metadata and fact diffs before committing. Requirement parsing is conservative but is not a complete quest/unlock verifier. Wiki text/data attribution remains in NOTICE.md.

For a built plugin, pass `--jar <path>` to the audit tool. It checks Java 11, required replacement classes/resources, license consistency and absence of legacy packages. It does not certify legal independence or gameplay parity.

The retained boss corpus and its separate refresh commands are documented in BOSS-WIKI-AUDIT.md and README.md. UI screenshots are generated under `build/ui`. Client testing must still cover live state, profiles, bank flows and overlays.
