package com.dpscalc.calc.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class AttackDistribution {
    private final List<HitDistribution> distributions;

    public AttackDistribution(List<HitDistribution> distributions) {
        if (distributions == null || distributions.isEmpty() || hasNull(distributions)) {
            throw new IllegalArgumentException("distributions must be non-empty and contain no nulls");
        }
        this.distributions = Collections.unmodifiableList(new ArrayList<>(distributions));
    }

    private static boolean hasNull(List<HitDistribution> distributions) {
        for (HitDistribution distribution : distributions) if (distribution == null) return true;
        return false;
    }

    public static AttackDistribution single(HitDistribution distribution) {
        return new AttackDistribution(Collections.singletonList(
            Objects.requireNonNull(distribution, "distribution")));
    }

    public List<HitDistribution> getDistributions() {
        return distributions;
    }

    public HitDistribution getJointDistribution() {
        HitDistribution joint = distributions.get(0);
        for (int index = 1; index < distributions.size(); index++) {
            joint = joint.zip(distributions.get(index));
        }
        return joint;
    }

    public HitDistribution getSingleHitsplatDistribution() {
        return getJointDistribution().cumulative();
    }

    public int getMin() {
        int minimum = 0;
        for (HitDistribution distribution : distributions) minimum += distribution.getMin();
        return minimum;
    }

    public int getMax() {
        int maximum = 0;
        for (HitDistribution distribution : distributions) maximum += distribution.getMax();
        return maximum;
    }

    public double getExpectedDamage() {
        double expected = 0;
        for (HitDistribution distribution : distributions) expected += distribution.getExpectedValue();
        return expected;
    }

    public AttackDistribution scaleProbability(double factor) {
        return map(distribution -> distribution.scaleProbability(factor));
    }

    public AttackDistribution scaleDamage(int factor, int divisor) {
        return map(distribution -> distribution.scaleDamage(factor, divisor));
    }

    public AttackDistribution transform(Function<Hitsplat, HitDistribution> transformer) {
        return map(distribution -> distribution.transform(transformer));
    }

    private AttackDistribution map(Function<HitDistribution, HitDistribution> mapper) {
        List<HitDistribution> mapped = new ArrayList<>(distributions.size());
        for (HitDistribution distribution : distributions) mapped.add(mapper.apply(distribution));
        return new AttackDistribution(mapped);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof AttackDistribution
            && distributions.equals(((AttackDistribution) object).distributions);
    }

    @Override
    public int hashCode() {
        return distributions.hashCode();
    }
}
