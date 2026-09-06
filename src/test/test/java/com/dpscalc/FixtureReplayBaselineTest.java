package com.dpscalc;

import com.dpscalc.fixture.FixtureDocument.FixtureCase;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class FixtureReplayBaselineTest {
    @Test
    public void currentReplayExecutesAllDeclaredFixtures() {
        Collection<Object[]> fixtures = FixtureReplayTest.loadFixtures();

        assertEquals(150, fixtures.size());
        for (Object[] fixture : fixtures) {
            FixtureReplayAssertions.assertFixture(
                (FixtureCase) fixture[2],
                String.format("[%s] %s", fixture[0], fixture[1]));
        }
    }
}
