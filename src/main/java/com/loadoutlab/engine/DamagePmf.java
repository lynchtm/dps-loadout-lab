/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. See src/independent/LICENSE.
 */
package com.loadoutlab.engine;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

/** Immutable, finite discrete probability distribution over nonnegative damage. */
public final class DamagePmf {
    public static final int MAX_DAMAGE = 10_000;
    private static final long MAX_CONVOLUTION_STEPS = 10_000_000L;
    private final double[] mass;

    public DamagePmf(double... probabilities) {
        if (probabilities == null
                || probabilities.length == 0
                || probabilities.length > MAX_DAMAGE + 1) {
            throw new IllegalArgumentException("Supply between 1 and 10001 damage bins");
        }
        double total = 0;
        int last = 0;
        for (int i = 0; i < probabilities.length; i++) {
            double p = probabilities[i];
            probability(p);
            total += p;
            if (p > 0) last = i;
        }
        if (Math.abs(total - 1.0) > 1e-10) {
            throw new IllegalArgumentException("Probabilities must sum to one");
        }
        mass = Arrays.copyOf(probabilities, last + 1);
    }

    public static DamagePmf singleHit(double accuracy, int minimum, int maximum) {
        probability(accuracy);
        if (minimum < 0 || maximum < minimum || maximum > MAX_DAMAGE) {
            throw new IllegalArgumentException("Invalid successful damage range");
        }
        double[] bins = new double[maximum + 1];
        double each = accuracy / (maximum - minimum + 1);
        Arrays.fill(bins, minimum, maximum + 1, each);
        bins[0] += 1.0 - accuracy;
        return new DamagePmf(bins);
    }

    public double probabilityAt(int damage) {
        return damage < 0 || damage >= mass.length ? 0 : mass[damage];
    }

    public double[] probabilities() {
        return mass.clone();
    }

    public int maximum() {
        return mass.length - 1;
    }

    public double mean() {
        double sum = 0;
        for (int d = 1; d < mass.length; d++) sum += d * mass[d];
        return sum;
    }

    /** Deterministic damage transform; collisions combine their probability mass. */
    public DamagePmf map(java.util.function.IntUnaryOperator transform) {
        if (transform == null) throw new IllegalArgumentException("Missing damage transform");
        int[] mapped = new int[mass.length];
        int maximum = 0;
        for (int d = 0; d < mass.length; d++) {
            interrupted();
            if (mass[d] == 0) continue;
            int value = transform.applyAsInt(d);
            if (value < 0 || value > MAX_DAMAGE)
                throw new IllegalArgumentException("Transformed damage exceeds bounds");
            mapped[d] = value;
            maximum = Math.max(maximum, value);
        }
        double[] bins = new double[maximum + 1];
        for (int d = 0; d < mass.length; d++) if (mass[d] > 0) bins[mapped[d]] += mass[d];
        return new DamagePmf(bins);
    }

    public double dps(double intervalSeconds) {
        if (!Double.isFinite(intervalSeconds) || intervalSeconds <= 0) {
            throw new IllegalArgumentException("Attack interval must be finite and positive");
        }
        return mean() / intervalSeconds;
    }

    /** Choose this distribution with the given chance; otherwise choose other. */
    public DamagePmf mixture(double chance, DamagePmf other) {
        probability(chance);
        if (other == null) throw new IllegalArgumentException("Missing mixture distribution");
        double[] result = new double[Math.max(mass.length, other.mass.length)];
        for (int d = 0; d < result.length; d++) {
            result[d] = chance * probabilityAt(d) + (1 - chance) * other.probabilityAt(d);
        }
        return new DamagePmf(result);
    }

    /** Total damage from two independent draws, including overkill. */
    public DamagePmf independentSum(DamagePmf other) {
        if (other == null) throw new IllegalArgumentException("Missing sum distribution");
        if (maximum() + other.maximum() > MAX_DAMAGE
                || (long) mass.length * other.mass.length > MAX_CONVOLUTION_STEPS) {
            throw new IllegalArgumentException("Damage convolution exceeds analysis limit");
        }
        double[] result = new double[maximum() + other.maximum() + 1];
        for (int a = 0; a < mass.length; a++) {
            interrupted();
            for (int b = 0; b < other.mass.length; b++) result[a + b] += mass[a] * other.mass[b];
        }
        return new DamagePmf(result);
    }

    static void probability(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException(
                    "Probability must be finite and between zero and one");
        }
    }

    static void interrupted() {
        if (Thread.currentThread().isInterrupted())
            throw new CancellationException("Analysis interrupted");
    }
}
