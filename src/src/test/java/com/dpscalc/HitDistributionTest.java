package com.dpscalc;

import com.dpscalc.calc.distribution.HitDistribution;
import com.dpscalc.calc.distribution.Hitsplat;
import com.dpscalc.calc.distribution.WeightedHit;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class HitDistributionTest {

    @Test
    public void accurateAndInaccurateZeroRemainDistinctCanonicalOutcomes() {
        HitDistribution distribution = new HitDistribution(Arrays.asList(
            new WeightedHit(0.4, Arrays.asList(Hitsplat.inaccurate())),
            new WeightedHit(0.6, Arrays.asList(Hitsplat.accurate(0)))
        ));

        assertEquals(2, distribution.getOutcomes().size());
        assertTrue(distribution.getOutcomes().get(0).getHitsplats().get(0).isAccurate());
        assertFalse(distribution.getOutcomes().get(1).getHitsplats().get(0).isAccurate());
        assertNotEquals(distribution.getOutcomes().get(0), distribution.getOutcomes().get(1));
    }

    @Test
    public void linearDistributionHasWeightedOutcomesAndNormalizedProbability() {
        HitDistribution distribution = HitDistribution.linear(0.75, 0, 2);

        assertEquals(4, distribution.size());
        assertEquals(0, distribution.getMin());
        assertEquals(2, distribution.getMax());
        assertEquals(0.75, distribution.getExpectedValue(), 0.0);
        assertEquals(1.0, distribution.getTotalProbability(), 0.0);
        assertEquals(0.25, distribution.getOutcomes().get(0).getProbability(), 0.0);
        assertTrue(distribution.getOutcomes().get(0).anyAccurate());
        assertFalse(distribution.getOutcomes().get(3).anyAccurate());
    }

    @Test
    public void scaleZipAndTransformPreserveJointProbabilities() {
        HitDistribution left = HitDistribution.single(0.5, Arrays.asList(Hitsplat.accurate(2)));
        HitDistribution right = HitDistribution.deterministic(Hitsplat.accurate(3));

        HitDistribution zipped = left.scaleProbability(0.5).zip(right);
        HitDistribution transformed = zipped.transform(hitsplat ->
            HitDistribution.deterministic(new Hitsplat(hitsplat.getDamage() * 2, hitsplat.isAccurate())));

        assertEquals(2, transformed.size());
        assertEquals(0.5, transformed.getTotalProbability(), 0.0);
        assertEquals(Arrays.asList(Hitsplat.accurate(4), Hitsplat.accurate(6)),
            transformed.getOutcomes().get(0).getHitsplats());
        assertEquals(Arrays.asList(Hitsplat.inaccurate(), Hitsplat.accurate(6)),
            transformed.getOutcomes().get(1).getHitsplats());
    }

    @Test
    public void transformCanPreserveInaccurateZero() {
        HitDistribution distribution = HitDistribution.linear(0.5, 0, 0)
            .transform(hitsplat -> HitDistribution.deterministic(Hitsplat.accurate(7)), false);

        assertEquals(Hitsplat.accurate(7), distribution.getOutcomes().get(0).getHitsplats().get(0));
        assertEquals(Hitsplat.inaccurate(), distribution.getOutcomes().get(1).getHitsplats().get(0));
    }

    @Test
    public void flattenSortsDeterministicallyAndMergesIdenticalOutcomes() {
        WeightedHit high = new WeightedHit(0.2, Arrays.asList(Hitsplat.accurate(4)));
        WeightedHit low = new WeightedHit(0.3, Arrays.asList(Hitsplat.accurate(1)));
        WeightedHit duplicateLow = new WeightedHit(0.5, Arrays.asList(Hitsplat.accurate(1)));

        HitDistribution first = new HitDistribution(Arrays.asList(high, low, duplicateLow)).flatten();
        HitDistribution second = new HitDistribution(Arrays.asList(duplicateLow, high, low)).flatten();

        assertEquals(first, second);
        assertEquals(2, first.size());
        assertEquals(1, first.getOutcomes().get(0).getSum());
        assertEquals(0.8, first.getOutcomes().get(0).getProbability(), 0.0);
        assertEquals(4, first.getOutcomes().get(1).getSum());
    }

    @Test
    public void normalizedReturnsUnitProbabilityWithoutMutatingSource() {
        HitDistribution source = new HitDistribution(Arrays.asList(
            new WeightedHit(2.0, Arrays.asList(Hitsplat.accurate(1))),
            new WeightedHit(1.0, Arrays.asList(Hitsplat.accurate(2)))
        ));

        HitDistribution normalized = source.normalized();

        assertEquals(3.0, source.getTotalProbability(), 0.0);
        assertEquals(1.0, normalized.getTotalProbability(), 0.0);
        assertEquals(2.0 / 3.0, normalized.getOutcomes().get(0).getProbability(), 0.0);
        assertEquals(1.0 / 3.0, normalized.getOutcomes().get(1).getProbability(), 0.0);
    }

    @Test
    public void publicCollectionsAreImmutableDefensiveCopies() {
        List<Hitsplat> splats = new ArrayList<>(Arrays.asList(Hitsplat.accurate(1)));
        WeightedHit weightedHit = new WeightedHit(1.0, splats);
        List<WeightedHit> outcomes = new ArrayList<>(Arrays.asList(weightedHit));
        HitDistribution distribution = new HitDistribution(outcomes);
        splats.add(Hitsplat.accurate(9));
        outcomes.clear();

        assertEquals(1, weightedHit.getHitsplats().size());
        assertEquals(1, distribution.size());
        assertThrows(UnsupportedOperationException.class,
            () -> weightedHit.getHitsplats().add(Hitsplat.accurate(2)));
        assertThrows(UnsupportedOperationException.class,
            () -> distribution.getOutcomes().clear());
    }

    @Test
    public void malformedDomainValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Hitsplat.accurate(-1));
        assertThrows(IllegalArgumentException.class,
            () -> new WeightedHit(Double.NaN, Arrays.asList(Hitsplat.accurate(1))));
        HitDistribution zeroSum = new HitDistribution(Arrays.asList(
            new WeightedHit(0.0, Arrays.asList(Hitsplat.accurate(1)))));
        assertThrows(IllegalStateException.class, zeroSum::normalized);
    }
}
