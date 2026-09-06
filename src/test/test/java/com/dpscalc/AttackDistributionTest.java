package com.dpscalc;

import com.dpscalc.calc.distribution.AttackDistribution;
import com.dpscalc.calc.distribution.HitDistribution;
import com.dpscalc.calc.distribution.Hitsplat;
import com.dpscalc.calc.distribution.WeightedHit;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class AttackDistributionTest {

    @Test
    public void multiSplatJointOutcomesHaveExactCartesianWeights() {
        HitDistribution first = new HitDistribution(Arrays.asList(
            outcome(0.5, 1),
            outcome(0.5, 3)
        ));
        HitDistribution second = new HitDistribution(Arrays.asList(
            outcome(0.25, 2),
            outcome(0.75, 4)
        ));

        AttackDistribution attack = new AttackDistribution(Arrays.asList(first, second));
        HitDistribution joint = attack.getJointDistribution();

        assertEquals(4, joint.size());
        assertOutcome(joint.getOutcomes().get(0), 0.125, 1, 2);
        assertOutcome(joint.getOutcomes().get(1), 0.375, 1, 4);
        assertOutcome(joint.getOutcomes().get(2), 0.125, 3, 2);
        assertOutcome(joint.getOutcomes().get(3), 0.375, 3, 4);
        assertEquals(3, attack.getMin());
        assertEquals(7, attack.getMax());
        assertEquals(5.5, attack.getExpectedDamage(), 0.0);
        assertEquals(1.0, joint.getTotalProbability(), 0.0);
    }

    @Test
    public void cumulativeMergesEqualMultiSplatTotals() {
        HitDistribution first = new HitDistribution(Arrays.asList(outcome(0.5, 1), outcome(0.5, 3)));
        HitDistribution second = new HitDistribution(Arrays.asList(outcome(0.25, 2), outcome(0.75, 4)));

        HitDistribution cumulative = new AttackDistribution(Arrays.asList(first, second))
            .getSingleHitsplatDistribution();

        assertEquals(3, cumulative.size());
        assertEquals(3, cumulative.getOutcomes().get(0).getSum());
        assertEquals(0.125, cumulative.getOutcomes().get(0).getProbability(), 0.0);
        assertEquals(5, cumulative.getOutcomes().get(1).getSum());
        assertEquals(0.5, cumulative.getOutcomes().get(1).getProbability(), 0.0);
        assertEquals(7, cumulative.getOutcomes().get(2).getSum());
        assertEquals(0.375, cumulative.getOutcomes().get(2).getProbability(), 0.0);
    }

    @Test
    public void scaleDamageUsesIntegerTruncationAndDistributionListIsImmutable() {
        AttackDistribution attack = new AttackDistribution(Arrays.asList(
            HitDistribution.deterministic(Hitsplat.accurate(5)),
            HitDistribution.deterministic(Hitsplat.accurate(3))
        ));

        AttackDistribution scaled = attack.scaleDamage(2, 3);

        assertEquals(5, scaled.getMax());
        assertEquals(5.0, scaled.getExpectedDamage(), 0.0);
        assertThrows(UnsupportedOperationException.class, () -> attack.getDistributions().clear());
    }

    private static WeightedHit outcome(double probability, int damage) {
        return new WeightedHit(probability, Arrays.asList(Hitsplat.accurate(damage)));
    }

    private static void assertOutcome(WeightedHit outcome, double probability, int... damages) {
        assertEquals(probability, outcome.getProbability(), 0.0);
        assertEquals(damages.length, outcome.getHitsplats().size());
        for (int index = 0; index < damages.length; index++) {
            assertEquals(damages[index], outcome.getHitsplats().get(index).getDamage());
        }
    }
}
