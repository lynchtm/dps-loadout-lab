# Independent foundation specification

This specification is derived from elementary probability, without taking the
legacy engine API, fixture outputs, or OSRS calculator implementation as the design.
The implementation is an initial migration component, not game-rule parity.

## Integer contests

Choose two independent uniform integers X in [0,A] and Y in [0,D]. Success means
X > Y; equality fails. A and D are nonnegative integers. There are (A+1)(D+1)
equally likely ordered pairs. If A <= D, the successful pairs total A(A+1)/2.
Otherwise unsuccessful pairs total (D+1)(D+2)/2. Division gives the probability.
An exhaustive independent enumeration of small rectangles is the test oracle.

## Damage

A distribution stores mass at each nonnegative integer damage. It must be finite,
nonnegative and sum to one (allow 1e-10 floating-point roundoff; do not normalize
arbitrary weights silently). Maximum supported damage is 10,000 to bound memory.
A distribution owns a defensive copy of its data and exposes a defensive copy.
Trailing zero bins do not change the maximum attainable hit.

Single-hit inputs explicitly specify success probability and the inclusive
uniform damage range on success. Misses deal zero. This layer makes no assumption
about whether a particular game attack can roll a successful zero. Zero damage
and accuracy zero remain valid models. A mixture chooses one distribution; a sum
draws independently from both. Correlated hits must be supplied as an already
resolved distribution; independence must not be assumed for them.

E[damage] = sum(d * p[d]). Max hit is the largest d with positive mass. DPS is
E[damage] / attack interval seconds. Timing values must be finite and positive.

## Stationary kill time

Given fixed positive integer HP h, no healing and identically distributed
independent attacks, use the law of total expectation. h <= 0 is absorbing:
E[N_h] = 0 and E[N_h^2] = 0. For h > 0, one attack is taken and residual HP is
max(0,h-d). Solve the self-loop caused by p[0]:

E[N_h] = (1 + sum(d>0, p[d] E[N_max(0,h-d)])) / (1-p[0]).

E[N_h^2] = (1 + 2 * (p[0] E[N_h] + sum(d>0, p[d] E[N_max(0,h-d)]))
              + sum(d>0, p[d] E[N_max(0,h-d)^2])) / (1-p[0]).

Variance is second moment minus squared mean (clamp negative roundoff to zero).
Compute positive-damage mass by summation rather than subtracting p[0] from one
to preserve very small positive probabilities. A never-damaging attack returns
infinite expected attacks and undefined (NaN) variance, rather than a false DPS
or finite time. Exact damage of 1 with success chance q is a geometric check for
HP=1: E[N]=1/q and variance=(1-q)/q^2.

Expected elapsed seconds = first-hit delay + (E[N]-1)*interval. The caller
supplies the delay explicitly; first hit at time zero and after one interval are
different conventions. No travel time, regeneration or phase transition is implied.

A bounded kill probability array has one bin per attack, followed by the
probability of surviving the entire horizon. Propagate residual HP probabilities
and collect absorbed mass on each step. Do not renormalize away the tail.
Reject excessive HP/transition counts before allocation and honor interruption.

## Isolation and acceptance

The source set must compile with only the Java standard library. Tests may use
JUnit; they must not include the production plugin, GPL fixtures or its classpath.
Independent artifacts must contain no `com/dpscalc` classes or legacy JSON/images.
This foundation is not selected in the live UI until complete game-rule, data,
provenance and integration reviews succeed. Unsupported mechanics in that later
integration must be explicit, not quietly evaluated as an ordinary weapon.
