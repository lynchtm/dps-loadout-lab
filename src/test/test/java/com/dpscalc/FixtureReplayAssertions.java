package com.dpscalc;

import com.dpscalc.fixture.FixtureDocument.FixtureCase;
import com.dpscalc.parity.ComparisonResult;
import com.dpscalc.parity.ParityComparator;

import static org.junit.Assert.fail;

final class FixtureReplayAssertions {
    private static final ParityEvaluator EVALUATOR = new ParityEvaluator();

    private FixtureReplayAssertions() {}

    static void assertFixture(FixtureCase fixture, String context) {
        ComparisonResult comparison = ParityComparator.compareOutputs(
            fixture.getOutputs(), EVALUATOR.evaluate(fixture));
        if (!comparison.isMatch()) {
            fail(context + " id=" + fixture.getId() + ": " + comparison.getDiagnostic());
        }
    }
}
