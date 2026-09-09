# Independent probability foundation

Created September 9, 2026 for Tommy Lynch, DPS Loadout Lab. The LICENSE in this
directory applies only to these new foundation sources and the accompanying
`src/independentTest` sources, not the existing plugin or its data.

These files implement the mathematical specification in `SPEC.md` alongside this
record. They use
Java standard-library types only. No legacy calculator types, resources, expected
output fixtures, equipment IDs, spell lists, monster tables or upstream source
fragments are inputs to this implementation or its tests. Tests use synthetic
integer contests, Bernoulli trials, deterministic damage and enumerated examples.

This is independent authoring, **not a clean-room certification**: the development
session already had access to the existing project and its provenance. A new
package name or a rewrite alone cannot certify copyright independence. This
record describes the actual implementation method and review boundary.

Scope: a strict integer roll contest; discrete nonnegative damage probabilities;
independent sums and mixtures; expected damage, maximum damage, stationary attack
counts and a bounded kill-time distribution. No OSRS skill/prayer/equipment rules
or encounter mechanics are implemented here. Inputs are already-resolved rolls
and damage ranges, and the caller must explicitly choose those ranges.

Build: `gradlew independentTest independentJar`. The separate JAR includes only
this source set and its notices. It is a foundation library, not a runnable plugin
or a Plugin Hub submission. The runtime plugin now uses this foundation through
the independently specified game-rule adapter. Foundation source is under
`src/main/java/com/loadoutlab/engine` for compatibility with Hub standard builds;
its Gradle source set still excludes all game/RuneLite dependencies.
