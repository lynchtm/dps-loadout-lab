package com.dpscalc.parity;

import com.dpscalc.calc.distribution.HitDistribution;
import com.dpscalc.calc.distribution.Hitsplat;
import com.dpscalc.calc.distribution.WeightedHit;
import com.dpscalc.calc.distribution.AttackDistribution;

import java.util.List;
import java.util.Map;

public final class ParityComparator {
    private ParityComparator() {}

    public static ComparisonResult compareInteger(String path, int expected, int actual) {
        if (expected == actual) return ComparisonResult.match();
        return mismatch(path, expected, actual);
    }

    public static ComparisonResult compareFloat(String path, double expected, double actual) {
        double tolerance = Math.max(1e-12, 8 * Math.ulp(expected));
        if (Double.isFinite(expected) && Double.isFinite(actual)
            && Math.abs(actual - expected) <= tolerance) {
            return ComparisonResult.match();
        }
        return ComparisonResult.mismatch(path + ": expected=" + expected + ", actual=" + actual
            + ", tolerance=" + tolerance);
    }

    public static ComparisonResult compareOutputs(ParityOutput expected, ParityOutput actual) {
        ComparisonResult integers = compareIntegers(expected.getIntegers(), actual.getIntegers());
        if (!integers.isMatch()) return integers;
        ComparisonResult floats = compareFloats(expected.getFloats(), actual.getFloats());
        if (!floats.isMatch()) return floats;
        return compareDistribution("outputs.normalizedDistribution",
            expected.getDistribution(), actual.getDistribution());
    }

    public static ComparisonResult compareDistribution(
        String path, HitDistribution expected, HitDistribution actual) {
        List<WeightedHit> expectedOutcomes = expected.flatten().normalized().getOutcomes();
        List<WeightedHit> actualOutcomes = actual.flatten().normalized().getOutcomes();
        ComparisonResult size = compareInteger(path + ".outcomes.length",
            expectedOutcomes.size(), actualOutcomes.size());
        if (!size.isMatch()) return size;
        for (int outcomeIndex = 0; outcomeIndex < expectedOutcomes.size(); outcomeIndex++) {
            WeightedHit expectedOutcome = expectedOutcomes.get(outcomeIndex);
            WeightedHit actualOutcome = actualOutcomes.get(outcomeIndex);
            String outcomePath = path + ".outcomes[" + outcomeIndex + "]";
            ComparisonResult probability = compareFloat(outcomePath + ".probability",
                expectedOutcome.getProbability(), actualOutcome.getProbability());
            if (!probability.isMatch()) return probability;
            ComparisonResult hitsplats = compareHitsplats(outcomePath + ".hitsplats",
                expectedOutcome.getHitsplats(), actualOutcome.getHitsplats());
            if (!hitsplats.isMatch()) return hitsplats;
        }
        return ComparisonResult.match();
    }

    public static ComparisonResult compareDistribution(
        String path, AttackDistribution expected, AttackDistribution actual) {
        return compareDistribution(path,
            expected.getJointDistribution(), actual.getJointDistribution());
    }

    private static ComparisonResult compareHitsplats(
        String path, List<Hitsplat> expected, List<Hitsplat> actual) {
        ComparisonResult size = compareInteger(path + ".length", expected.size(), actual.size());
        if (!size.isMatch()) return size;
        for (int index = 0; index < expected.size(); index++) {
            Hitsplat expectedSplat = expected.get(index);
            Hitsplat actualSplat = actual.get(index);
            ComparisonResult damage = compareInteger(path + "[" + index + "].damage",
                expectedSplat.getDamage(), actualSplat.getDamage());
            if (!damage.isMatch()) return damage;
            if (expectedSplat.isAccurate() != actualSplat.isAccurate()) {
                return mismatch(path + "[" + index + "].accurate",
                    expectedSplat.isAccurate(), actualSplat.isAccurate());
            }
        }
        return ComparisonResult.match();
    }

    private static ComparisonResult compareIntegers(Map<String, Integer> expected, Map<String, Integer> actual) {
        for (Map.Entry<String, Integer> entry : expected.entrySet()) {
            Integer actualValue = actual.get(entry.getKey());
            if (actualValue == null) return mismatch(entry.getKey(), entry.getValue(), "missing");
            ComparisonResult comparison = compareInteger(entry.getKey(), entry.getValue(), actualValue);
            if (!comparison.isMatch()) return comparison;
        }
        for (Map.Entry<String, Integer> entry : actual.entrySet()) {
            if (!expected.containsKey(entry.getKey())) return mismatch(entry.getKey(), "absent", entry.getValue());
        }
        return ComparisonResult.match();
    }

    private static ComparisonResult compareFloats(Map<String, Double> expected, Map<String, Double> actual) {
        for (Map.Entry<String, Double> entry : expected.entrySet()) {
            Double actualValue = actual.get(entry.getKey());
            if (actualValue == null) return mismatch(entry.getKey(), entry.getValue(), "missing");
            ComparisonResult comparison = compareFloat(entry.getKey(), entry.getValue(), actualValue);
            if (!comparison.isMatch()) return comparison;
        }
        for (Map.Entry<String, Double> entry : actual.entrySet()) {
            if (!expected.containsKey(entry.getKey())) return mismatch(entry.getKey(), "absent", entry.getValue());
        }
        return ComparisonResult.match();
    }

    private static ComparisonResult mismatch(String path, Object expected, Object actual) {
        return ComparisonResult.mismatch(path + ": expected=" + expected + ", actual=" + actual);
    }
}
