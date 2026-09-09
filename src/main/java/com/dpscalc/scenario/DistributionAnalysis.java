// SPDX-License-Identifier: BSD-2-Clause
package com.dpscalc.scenario;

import com.loadoutlab.engine.DamagePmf;
import com.loadoutlab.engine.StationaryKill;

/** Adapter from the standalone probability API to the existing graphs. */
public final class DistributionAnalysis {
    private DistributionAnalysis() {}

    public static double[] histogram(DamagePmf attack) {
        return attack.probabilities();
    }

    public static double[] moments(double[] probabilities, int hp) {
        StationaryKill.Moments m = StationaryKill.moments(new DamagePmf(probabilities), hp);
        if (!Double.isFinite(m.expectedAttacks()))
            throw new IllegalArgumentException("Target cannot be killed by this distribution");
        return new double[] {m.expectedAttacks(), m.attackVariance()};
    }

    public static double[] killDistribution(double[] probabilities, int hp, int attacks) {
        return StationaryKill.horizon(new DamagePmf(probabilities), hp, attacks);
    }
}
