/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. See src/independent/LICENSE.
 */
package com.loadoutlab.engine;

/** Fixed-HP target, independent identical attacks, no regeneration or phase changes. */
public final class StationaryKill {
    private static final int MAX_HP = 100_000;
    private static final long MAX_STEPS = 20_000_000L;

    private StationaryKill() {}

    public static Moments moments(DamagePmf damage, int hitpoints) {
        check(damage, hitpoints, 1);
        double positive = 0;
        for (int d = 1; d <= damage.maximum(); d++) positive += damage.probabilityAt(d);
        if (positive == 0) return new Moments(Double.POSITIVE_INFINITY, Double.NaN);
        double[] means = new double[hitpoints + 1];
        double[] seconds = new double[hitpoints + 1];
        for (int h = 1; h <= hitpoints; h++) {
            DamagePmf.interrupted();
            double residualMean = 0;
            double residualSecond = 0;
            for (int d = 1; d < h && d <= damage.maximum(); d++) {
                residualMean += damage.probabilityAt(d) * means[h - d];
                residualSecond += damage.probabilityAt(d) * seconds[h - d];
            }
            means[h] = (1 + residualMean) / positive;
            seconds[h] =
                    (1 + 2 * (damage.probabilityAt(0) * means[h] + residualMean) + residualSecond)
                            / positive;
        }
        double mean = means[hitpoints];
        return new Moments(mean, Math.max(0, seconds[hitpoints] - mean * mean));
    }

    /** Entry k is probability of kill on attack k+1; last entry is surviving mass. */
    public static double[] horizon(DamagePmf damage, int hitpoints, int attacks) {
        check(damage, hitpoints, attacks);
        double[] alive = new double[hitpoints + 1];
        alive[hitpoints] = 1;
        double[] result = new double[attacks + 1];
        for (int attack = 0; attack < attacks; attack++) {
            DamagePmf.interrupted();
            double[] next = new double[hitpoints + 1];
            for (int h = 1; h <= hitpoints; h++) {
                if (alive[h] == 0) continue;
                for (int d = 0; d <= damage.maximum(); d++) {
                    double p = alive[h] * damage.probabilityAt(d);
                    if (d >= h) result[attack] += p;
                    else next[h - d] += p;
                }
            }
            alive = next;
        }
        for (double p : alive) result[attacks] += p;
        return result;
    }

    private static void check(DamagePmf damage, int hp, int attacks) {
        if (damage == null || hp < 1 || hp > MAX_HP || attacks < 0 || attacks > 100_000) {
            throw new IllegalArgumentException("Invalid target or attack horizon");
        }
        if ((long) hp * (damage.maximum() + 1) * Math.max(1, attacks) > MAX_STEPS) {
            throw new IllegalArgumentException("Kill analysis exceeds transition limit");
        }
        DamagePmf.interrupted();
    }

    public static final class Moments {
        private final double mean;
        private final double variance;

        private Moments(double mean, double variance) {
            this.mean = mean;
            this.variance = variance;
        }

        public double expectedAttacks() {
            return mean;
        }

        public double attackVariance() {
            return variance;
        }

        public double expectedSeconds(double interval, double firstHitDelay) {
            if (!Double.isFinite(interval)
                    || interval <= 0
                    || !Double.isFinite(firstHitDelay)
                    || firstHitDelay < 0) {
                throw new IllegalArgumentException("Invalid attack timing");
            }
            return firstHitDelay + (mean - 1) * interval;
        }
    }
}
