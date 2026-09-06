package com.dpscalc.scenario;

import com.dpscalc.calc.distribution.*;
import java.util.*;

/** Compact convolution avoids materializing the Cartesian product of individual hitsplats. */
public final class DistributionAnalysis {
    private DistributionAnalysis() {}
    public static double[] histogram(AttackDistribution attack) {
        double[] combined = {1};
        for (HitDistribution hit : attack.getDistributions()) {
            if (hit.getMax() > 10000) throw new IllegalArgumentException("Damage exceeds analysis limit");
            double[] next = new double[hit.getMax() + 1];
            for (WeightedHit w : hit.getOutcomes()) next[w.getSum()] += w.getProbability();
            double[] convolution = new double[combined.length + next.length - 1];
            for (int a = 0; a < combined.length; a++) for (int b = 0; b < next.length; b++)
                convolution[a+b] += combined[a] * next[b];
            combined = convolution;
        }
        return combined;
    }
    public static double[] moments(double[] p, int hp) {
        if (hp < 1 || hp > 100000 || p.length < 2 || 1-p[0] < 1e-12) throw new IllegalArgumentException("Target cannot be killed by this distribution");
        if ((long)hp * p.length > 10_000_000) throw new IllegalArgumentException("Exact kill-time analysis exceeds 10 million transitions");
        double[] mean = new double[hp+1], second = new double[hp+1];
        for (int h = 1; h <= hp; h++) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            double continuation = 0, varianceContinuation = 0;
            for (int d = 1; d < p.length && d <= h; d++) {
                continuation += p[d] * mean[h-d];
                varianceContinuation += p[d] * second[h-d];
            }
            mean[h] = (1+continuation)/(1-p[0]);
            second[h] = (1+2*(p[0]*mean[h]+continuation)+varianceContinuation)/(1-p[0]);
        }
        return new double[]{mean[hp], Math.max(0, second[hp]-mean[hp]*mean[hp])};
    }
    /** Bounded PMF, with surviving tail returned as the last value, never renormalized. */
    public static double[] killDistribution(double[] p, int hp, int attacks) {
        if ((long)hp * p.length * attacks > 20_000_000) throw new IllegalArgumentException("Kill-time graph exceeds 20 million transitions");
        double[] alive = new double[hp+1]; alive[hp] = 1;
        double[] kills = new double[attacks+1];
        for (int a = 0; a < attacks; a++) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            double[] next = new double[hp+1];
            for (int h = 1; h <= hp; h++) if (alive[h] > 0) {
                for (int d = 0; d < p.length; d++) {
                    double probability = alive[h]*p[d];
                    if (d >= h) kills[a] += probability; else next[h-d] += probability;
                }
            }
            alive = next;
        }
        kills[attacks] = Arrays.stream(alive).sum(); return kills;
    }
}
