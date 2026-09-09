# Replacement implementation provenance

This revision replaces the GPL-derived runtime with newly authored Java code.
It does not retroactively relicense the previous implementation. Historical
revisions retain their original licenses.

The workspace audit at `docs/dps-provenance/` inventoried the original 377 files.
The historical file-by-file record is included in this release at
[provenance/baseline-inventory.csv](provenance/baseline-inventory.csv).
All 98 files classified as GPL-derived Java, all 10 inherited calculator/data
files, and all 59 imported images were removed. That includes the old engine,
models, equipment domain, plugin lifecycle, probability implementation and copied
output fixtures. The old calculator is not a dependency or fallback.

The replacement has these boundaries:

- `com.loadoutlab.engine`: standalone probability mathematics, authored from a
  written mathematical specification. Its tests enumerate integer contests and
  use synthetic probability distributions. No game tables or RuneLite dependency.
- `com.loadoutlab.model`, `equipment`, `data`, `calculation`: new editor contracts,
  independently acquired factual catalogs and game-rule implementation. The new
  mutable editor models preserve persisted field names and public accessor
  contracts. They are not a translation of the old calculation methods.
- `com.loadoutlab.DpsLoadoutLabPlugin`, configuration, overlay and icons: new
  event/lifecycle implementation against the public RuneLite API. No old client
  capture, overlay or bitmap bundle is retained.
- `com.dpscalc.scenario`: retained locally authored comparison UI, optimizer,
  persistence, guide-import integration and preparation workflows, adapted to the
  new contracts. The distribution adapter, style source and coverage reporting
  were replaced. A historical package name does not imply an engine dependency.
- `com.dpscalc.wikisetups`: retained BSD-2-Clause parser/resolver components,
  adapted from Jiimbones with their source and binary notices preserved.

The factual data scripts read RuneLite public exports and public Wiki articles,
Bucket rows and rendered weapon-option tables. They do not read either GPL
calculator repository or OSRSBox. Input source hashes, article revisions and
acquisition methods accompany the catalogs. Wiki text/data and Jagex assets have
separate attribution in NOTICE.md; the software license does not relicense them.

Game-rule tests use independently worked examples and invariants. Retained local
UI/optimizer tests were adapted to the new API. Two obsolete local test adapters
for the deleted GPL tracker and fixture replay were retired; copied GPL tests
were removed, not relabeled or used as an oracle. The remaining Collection Log
guide corpus tests the BSD Wiki importer and is separately attributed content.

This is an authoring/provenance record, **not a formal clean-room certification**.
The development session had prior exposure to the original project. Public API
signatures and persisted field contracts were inspected for compatibility. Fresh
code, different filenames and passing tests alone are not a legal certification.

`tools/audit-independent-release.py` checks the concrete source/resource boundary
and, optionally, a built JAR. Build-time scans supplement this record; they do not
prove copyright independence or complete combat-rule parity. RuneLite maintainer
review remains a separate step.
