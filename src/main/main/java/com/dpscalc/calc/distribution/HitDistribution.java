package com.dpscalc.calc.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class HitDistribution {
    private final List<WeightedHit> outcomes;

    public HitDistribution(List<WeightedHit> outcomes) {
        if (outcomes == null || outcomes.isEmpty() || hasNull(outcomes)) {
            throw new IllegalArgumentException("outcomes must be non-empty and contain no nulls");
        }
        List<WeightedHit> canonical = new ArrayList<>(outcomes);
        Collections.sort(canonical);
        this.outcomes = Collections.unmodifiableList(canonical);
    }

    private static boolean hasNull(List<WeightedHit> outcomes) {
        for (WeightedHit outcome : outcomes) if (outcome == null) return true;
        return false;
    }

    public static HitDistribution deterministic(Hitsplat hitsplat) {
        return new HitDistribution(Collections.singletonList(
            new WeightedHit(1.0, Collections.singletonList(Objects.requireNonNull(hitsplat, "hitsplat")))));
    }

    public static HitDistribution linear(double accuracy, int minimum, int maximum) {
        if (!Double.isFinite(accuracy) || accuracy < 0 || accuracy > 1) {
            throw new IllegalArgumentException("accuracy must be between zero and one");
        }
        if (minimum < 0 || maximum < minimum) {
            throw new IllegalArgumentException("damage range must be non-negative and ordered");
        }
        List<WeightedHit> outcomes = new ArrayList<>();
        double hitProbability = accuracy / (maximum - minimum + 1);
        for (int damage = minimum; damage <= maximum; damage++) {
            outcomes.add(new WeightedHit(hitProbability,
                Collections.singletonList(Hitsplat.accurate(damage))));
        }
        outcomes.add(new WeightedHit(1.0 - accuracy, Collections.singletonList(Hitsplat.inaccurate())));
        return new HitDistribution(outcomes);
    }

    public static HitDistribution single(double accuracy, List<Hitsplat> hitsplats) {
        if (!Double.isFinite(accuracy) || accuracy < 0 || accuracy > 1) {
            throw new IllegalArgumentException("accuracy must be between zero and one");
        }
        List<WeightedHit> outcomes = new ArrayList<>();
        outcomes.add(new WeightedHit(accuracy, hitsplats));
        if (accuracy != 1.0) {
            outcomes.add(new WeightedHit(1.0 - accuracy, Collections.singletonList(Hitsplat.inaccurate())));
        }
        return new HitDistribution(outcomes);
    }

    public List<WeightedHit> getOutcomes() {
        return outcomes;
    }

    public int size() {
        return outcomes.size();
    }

    public double getTotalProbability() {
        double total = 0;
        for (WeightedHit outcome : outcomes) total += outcome.getProbability();
        return total;
    }

    public double getExpectedValue() {
        double expected = 0;
        for (WeightedHit outcome : outcomes) expected += outcome.getExpectedValue();
        return expected;
    }

    public int getMin() {
        int minimum = Integer.MAX_VALUE;
        for (WeightedHit outcome : outcomes) minimum = Math.min(minimum, outcome.getSum());
        return minimum;
    }

    public int getMax() {
        int maximum = Integer.MIN_VALUE;
        for (WeightedHit outcome : outcomes) maximum = Math.max(maximum, outcome.getSum());
        return maximum;
    }

    public HitDistribution scaleProbability(double factor) {
        List<WeightedHit> scaled = new ArrayList<>(outcomes.size());
        for (WeightedHit outcome : outcomes) scaled.add(outcome.scale(factor));
        return new HitDistribution(scaled);
    }

    public HitDistribution scaleDamage(int factor, int divisor) {
        if (factor < 0 || divisor <= 0) {
            throw new IllegalArgumentException("factor must be non-negative and divisor must be positive");
        }
        List<WeightedHit> scaled = new ArrayList<>(outcomes.size());
        for (WeightedHit outcome : outcomes) {
            List<Hitsplat> splats = new ArrayList<>(outcome.getHitsplats().size());
            for (Hitsplat hitsplat : outcome.getHitsplats()) {
                splats.add(new Hitsplat(hitsplat.getDamage() * factor / divisor, hitsplat.isAccurate()));
            }
            scaled.add(new WeightedHit(outcome.getProbability(), splats));
        }
        return new HitDistribution(scaled);
    }

    public HitDistribution zip(HitDistribution other) {
        Objects.requireNonNull(other, "other");
        List<WeightedHit> zipped = new ArrayList<>(outcomes.size() * other.outcomes.size());
        for (WeightedHit left : outcomes) {
            for (WeightedHit right : other.outcomes) zipped.add(left.zip(right));
        }
        return new HitDistribution(zipped);
    }

    public HitDistribution transform(Function<Hitsplat, HitDistribution> transformer) {
        return transform(transformer, true);
    }

    public HitDistribution transform(Function<Hitsplat, HitDistribution> transformer,
                                     boolean transformInaccurate) {
        Objects.requireNonNull(transformer, "transformer");
        List<WeightedHit> transformed = new ArrayList<>();
        for (WeightedHit outcome : outcomes) {
            HitDistribution current = null;
            for (Hitsplat hitsplat : outcome.getHitsplats()) {
                HitDistribution next = !hitsplat.isAccurate() && !transformInaccurate
                    ? deterministic(hitsplat)
                    : Objects.requireNonNull(transformer.apply(hitsplat), "transformed distribution");
                current = current == null ? next : current.zip(next);
            }
            for (WeightedHit value : current.outcomes) transformed.add(value.scale(outcome.getProbability()));
        }
        return new HitDistribution(transformed).flatten();
    }

    public HitDistribution flatten() {
        Map<List<Hitsplat>, Double> probabilities = new LinkedHashMap<>();
        for (WeightedHit outcome : outcomes) {
            probabilities.merge(outcome.getHitsplats(), outcome.getProbability(), Double::sum);
        }
        List<WeightedHit> flattened = new ArrayList<>();
        for (Map.Entry<List<Hitsplat>, Double> entry : probabilities.entrySet()) {
            if (entry.getValue() > 0) flattened.add(new WeightedHit(entry.getValue(), entry.getKey()));
        }
        return new HitDistribution(flattened);
    }

    public HitDistribution cumulative() {
        Map<CumulativeKey, Double> probabilities = new LinkedHashMap<>();
        for (WeightedHit outcome : outcomes) {
            CumulativeKey key = new CumulativeKey(outcome.getSum(), outcome.anyAccurate());
            probabilities.merge(key, outcome.getProbability(), Double::sum);
        }
        List<WeightedHit> cumulative = new ArrayList<>();
        for (Map.Entry<CumulativeKey, Double> entry : probabilities.entrySet()) {
            CumulativeKey key = entry.getKey();
            cumulative.add(new WeightedHit(entry.getValue(), Collections.singletonList(
                new Hitsplat(key.damage, key.accurate))));
        }
        return new HitDistribution(cumulative);
    }

    public HitDistribution normalized() {
        double total = getTotalProbability();
        if (!Double.isFinite(total) || total <= 0) {
            throw new IllegalStateException("distribution probability must have a positive finite sum");
        }
        return scaleProbability(1.0 / total);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof HitDistribution
            && outcomes.equals(((HitDistribution) object).outcomes);
    }

    @Override
    public int hashCode() {
        return outcomes.hashCode();
    }

    private static final class CumulativeKey {
        private final int damage;
        private final boolean accurate;

        private CumulativeKey(int damage, boolean accurate) {
            this.damage = damage;
            this.accurate = accurate;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof CumulativeKey)) return false;
            CumulativeKey key = (CumulativeKey) object;
            return damage == key.damage && accurate == key.accurate;
        }

        @Override
        public int hashCode() {
            return Objects.hash(damage, accurate);
        }
    }
}
