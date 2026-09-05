package com.dpscalc;

import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.fixture.FixtureDocument;
import com.dpscalc.fixture.FixtureDocumentLoader;
import com.dpscalc.parity.ComparisonResult;
import com.dpscalc.parity.ParityComparator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParityEvaluatorTest {
    @Test
    public void evaluatesCompleteTypedFixtureOutput() {
        FixtureDocument document = FixtureDocumentLoader.loadResource(
            "/fixtures.json",
            EquipmentPreparationFacade.REFERENCE_SHA,
            EquipmentPreparationFacade.DOMAIN_DIGEST);
        FixtureDocument.FixtureCase fixture = document.getFixtures().get(0);

        ComparisonResult comparison = ParityComparator.compareOutputs(
            fixture.getOutputs(), new ParityEvaluator().evaluate(fixture));

        assertEquals(24, fixture.getOutputs().getIntegers().size());
        assertEquals(7, fixture.getOutputs().getFloats().size());
        assertTrue(comparison.getDiagnostic(), comparison.isMatch());
    }
}
