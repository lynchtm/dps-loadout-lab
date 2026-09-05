package com.dpscalc;

import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.fixture.FixtureDocument;
import com.dpscalc.fixture.FixtureDocument.FixtureCase;
import com.dpscalc.fixture.FixtureDocumentException;
import com.dpscalc.fixture.FixtureDocumentLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(Parameterized.class)
public class FixtureReplayTest {
    private final FixtureCase fixture;

    public FixtureReplayTest(String id, String name, FixtureCase fixture) {
        this.fixture = fixture;
    }

    @Parameterized.Parameters(name = "{0}: {1}")
    public static Collection<Object[]> loadFixtures() {
        return loadFixtures(System.getProperty("fixtureReplay.resource", "/fixtures.json"));
    }

    static Collection<Object[]> loadFixtures(String resourcePath) {
        FixtureDocument document = FixtureDocumentLoader.loadResource(
            resourcePath,
            EquipmentPreparationFacade.REFERENCE_SHA,
            EquipmentPreparationFacade.DOMAIN_DIGEST);
        if (document.getDeclaredCount() <= 0) {
            throw new FixtureDocumentException("$.totalScenarios", "must be positive before parameterization");
        }
        if (document.getDeclaredCount() != document.getFixtures().size()) {
            throw new FixtureDocumentException("$.totalScenarios", "declared count does not match parameters");
        }

        List<Object[]> parameters = new ArrayList<>(document.getDeclaredCount());
        for (FixtureCase fixture : document.getFixtures()) {
            parameters.add(new Object[]{fixture.getId(), fixture.getName(), fixture});
        }
        return parameters;
    }

    @Test
    public void testFixture() {
        FixtureReplayAssertions.assertFixture(
            fixture,
            String.format("[%s] %s", fixture.getId(), fixture.getName()));
    }
}
