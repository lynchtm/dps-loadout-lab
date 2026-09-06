package com.dpscalc;

import com.dpscalc.calc.distribution.HitDistribution;
import com.dpscalc.calc.distribution.Hitsplat;
import com.dpscalc.calc.distribution.WeightedHit;
import com.dpscalc.calc.distribution.AttackDistribution;
import com.dpscalc.parity.ParityComparator;
import com.dpscalc.parity.ParityOutput;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParityComparatorTest {
    @Test
    public void acceptsAndRejectsAtStrictFloatBoundary() {
        double expected = 1024.0;
        double accepted = expected;
        for (int index = 0; index < 8; index++) accepted = Math.nextUp(accepted);
        double rejected = Math.nextUp(accepted);

        assertTrue(ParityComparator.compareFloat("outputs.accuracy", expected, accepted).isMatch());
        assertFalse(ParityComparator.compareFloat("outputs.accuracy", expected, rejected).isMatch());
    }

    @Test
    public void rejectsExactDiscreteMismatchWithPath() {
        String diagnostic = ParityComparator.compareInteger("outputs.maxHit", 12, 13).getDiagnostic();

        assertTrue(diagnostic.contains("outputs.maxHit"));
        assertTrue(diagnostic.contains("expected=12"));
        assertTrue(diagnostic.contains("actual=13"));
    }

    @Test
    public void rejectsProbabilityDifferenceEvenWhenMaxAndExpectedMatch() {
        HitDistribution expected = new HitDistribution(List.of(
            new WeightedHit(0.25, List.of(Hitsplat.accurate(0))),
            new WeightedHit(0.5, List.of(Hitsplat.accurate(1))),
            new WeightedHit(0.25, List.of(Hitsplat.accurate(2)))));
        HitDistribution actual = new HitDistribution(List.of(
            new WeightedHit(0.4, List.of(Hitsplat.accurate(0))),
            new WeightedHit(0.2, List.of(Hitsplat.accurate(1))),
            new WeightedHit(0.4, List.of(Hitsplat.accurate(2)))));

        String diagnostic = ParityComparator.compareDistribution(
            "outputs.normalizedDistribution", expected, actual).getDiagnostic();

        assertTrue(diagnostic.contains("outputs.normalizedDistribution.outcomes[0].probability"));
    }

    @Test
    public void canonicalJointComparisonAcceptsSplitEquivalentOutcomes() {
        HitDistribution split = new HitDistribution(List.of(
            new WeightedHit(0.25, List.of(Hitsplat.accurate(1))),
            new WeightedHit(0.25, List.of(Hitsplat.accurate(1))),
            new WeightedHit(0.5, List.of(Hitsplat.inaccurate()))));
        HitDistribution flattened = new HitDistribution(List.of(
            new WeightedHit(0.5, List.of(Hitsplat.accurate(1))),
            new WeightedHit(0.5, List.of(Hitsplat.inaccurate()))));

        assertTrue(ParityComparator.compareDistribution("outputs.normalizedDistribution",
            AttackDistribution.single(split), AttackDistribution.single(flattened)).isMatch());
    }

    @Test
    public void rejectsEveryExpandedIntegerOutputWithPathAndValues() {
        ParityOutput expected = sampleOutput();
        for (Map.Entry<String, Integer> field : expected.getIntegers().entrySet()) {
            int actualValue = field.getValue() + 1;
            ParityOutput actual = expected.toBuilder().putInteger(field.getKey(), actualValue).build();

            String diagnostic = ParityComparator.compareOutputs(expected, actual).getDiagnostic();

            assertTrue(field.getKey(), diagnostic.contains(field.getKey()));
            assertTrue(field.getKey(), diagnostic.contains("expected=" + field.getValue()));
            assertTrue(field.getKey(), diagnostic.contains("actual=" + actualValue));
        }
    }

    @Test
    public void rejectsEveryExpandedFloatOutputWithPathAndValues() {
        ParityOutput expected = sampleOutput();
        for (Map.Entry<String, Double> field : expected.getFloats().entrySet()) {
            double actualValue = field.getValue() + 1.0;
            ParityOutput actual = expected.toBuilder().putFloat(field.getKey(), actualValue).build();

            String diagnostic = ParityComparator.compareOutputs(expected, actual).getDiagnostic();

            assertTrue(field.getKey(), diagnostic.contains(field.getKey()));
            assertTrue(field.getKey(), diagnostic.contains("expected=" + field.getValue()));
            assertTrue(field.getKey(), diagnostic.contains("actual=" + actualValue));
        }
    }

    @Test
    public void rejectsExpandedDistributionWithPathAndValues() {
        ParityOutput expected = sampleOutput();
        HitDistribution changed = new HitDistribution(List.of(
            new WeightedHit(1.0, List.of(Hitsplat.accurate(99)))));

        String diagnostic = ParityComparator.compareOutputs(
            expected, expected.toBuilder().distribution(changed).build()).getDiagnostic();

        assertTrue(diagnostic.contains("outputs.normalizedDistribution.outcomes[0].hitsplats[0].damage"));
        assertTrue(diagnostic.contains("expected=1"));
        assertTrue(diagnostic.contains("actual=99"));
    }

    @Test
    public void rejectsMissingAndUnexpectedSnapshotPaths() {
        ParityOutput expected = sampleOutput();
        ParityOutput missing = ParityOutput.builder()
            .putIntegers(Map.of())
            .putFloats(expected.getFloats())
            .distribution(expected.getDistribution())
            .build();
        String missingDiagnostic = ParityComparator.compareOutputs(expected, missing).getDiagnostic();
        assertTrue(missingDiagnostic.contains("outputs.equipment.bonuses.str"));
        assertTrue(missingDiagnostic.contains("actual=missing"));

        ParityOutput extra = expected.toBuilder().putInteger("outputs.unexpected", 99).build();
        String extraDiagnostic = ParityComparator.compareOutputs(expected, extra).getDiagnostic();
        assertTrue(extraDiagnostic.contains("outputs.unexpected"));
        assertTrue(extraDiagnostic.contains("expected=absent"));
        assertTrue(extraDiagnostic.contains("actual=99"));
    }

    @Test
    public void distributionLengthAndAccuracyDiagnosticsContainPathAndValues() {
        HitDistribution expected = new HitDistribution(List.of(
            new WeightedHit(1.0, List.of(Hitsplat.accurate(1)))));
        HitDistribution extraOutcome = new HitDistribution(List.of(
            new WeightedHit(0.5, List.of(Hitsplat.accurate(1))),
            new WeightedHit(0.5, List.of(Hitsplat.accurate(2)))));
        String length = ParityComparator.compareDistribution(
            "outputs.normalizedDistribution", expected, extraOutcome).getDiagnostic();
        assertTrue(length.contains("outputs.normalizedDistribution.outcomes.length"));
        assertTrue(length.contains("expected=1"));
        assertTrue(length.contains("actual=2"));

        HitDistribution accurateZero = new HitDistribution(List.of(
            new WeightedHit(1.0, List.of(Hitsplat.accurate(0)))));
        HitDistribution inaccurate = new HitDistribution(List.of(
            new WeightedHit(1.0, List.of(Hitsplat.inaccurate()))));
        String accuracy = ParityComparator.compareDistribution(
            "outputs.normalizedDistribution", accurateZero, inaccurate).getDiagnostic();
        assertTrue(accuracy.contains("outputs.normalizedDistribution.outcomes[0].hitsplats[0].accurate"));
        assertTrue(accuracy.contains("expected=true"));
        assertTrue(accuracy.contains("actual=false"));
    }

    private static ParityOutput sampleOutput() {
        ParityOutput.Builder output = ParityOutput.builder();
        String[] integerPaths = {
            "outputs.equipment.bonuses.str", "outputs.equipment.bonuses.magic_str",
            "outputs.equipment.bonuses.ranged_str", "outputs.equipment.bonuses.prayer",
            "outputs.equipment.offensive.stab", "outputs.equipment.offensive.slash",
            "outputs.equipment.offensive.crush", "outputs.equipment.offensive.magic",
            "outputs.equipment.offensive.ranged", "outputs.equipment.defensive.stab",
            "outputs.equipment.defensive.slash", "outputs.equipment.defensive.crush",
            "outputs.equipment.defensive.magic", "outputs.equipment.defensive.ranged",
            "outputs.equipment.attackSpeed", "outputs.maxHit", "outputs.maxAttackRoll",
            "outputs.npcDefRoll", "outputs.scalarMax", "outputs.directMax", "outputs.dotMax",
            "outputs.totalMax", "outputs.distributionMax", "outputs.baseAttackSpeed"
        };
        String[] floatPaths = {
            "outputs.accuracy", "outputs.expectedDirectDamage", "outputs.expectedDotDamage",
            "outputs.expectedDamage", "outputs.expectedAttackSpeed", "outputs.dpt", "outputs.dps"
        };
        for (int index = 0; index < integerPaths.length; index++) output.putInteger(integerPaths[index], index + 1);
        for (int index = 0; index < floatPaths.length; index++) output.putFloat(floatPaths[index], index + 0.25);
        return output.distribution(new HitDistribution(List.of(
            new WeightedHit(1.0, List.of(Hitsplat.accurate(1)))))).build();
    }
}
