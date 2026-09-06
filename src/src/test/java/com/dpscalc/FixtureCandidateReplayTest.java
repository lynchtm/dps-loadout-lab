package com.dpscalc;

import static org.junit.Assert.*;

import com.dpscalc.fixture.FixtureDocument;
import com.dpscalc.fixture.FixtureDocumentLoader;

import org.junit.*;

import java.nio.file.*;

/** Replays an explicitly supplied upstream snapshot without changing the pinned baseline. */
public class FixtureCandidateReplayTest {
    @Test
    public void candidateMatchesDeclaredReference() throws Exception {
        String file = System.getProperty("fixtureCandidate.file");
        Assume.assumeTrue("Use candidateParity with an upstream fixture snapshot", file != null);
        String sha = System.getProperty("fixtureCandidate.sha"),
                digest = System.getProperty("fixtureCandidate.digest");
        assertNotNull("Declare the candidate reference SHA", sha);
        assertNotNull("Declare the candidate equipment digest", digest);
        Path path = Paths.get(file);
        assertTrue("Fixture document exceeds 16 MiB", Files.size(path) <= 16 * 1024 * 1024);
        FixtureDocument document =
                FixtureDocumentLoader.loadString(Files.readString(path), sha, digest);
        assertTrue(document.getDeclaredCount() > 0);
        assertEquals(document.getDeclaredCount(), document.getFixtures().size());
        for (FixtureDocument.FixtureCase fixture : document.getFixtures())
            FixtureReplayAssertions.assertFixture(
                    fixture, "candidate=" + sha + " fixture=" + fixture.getId());
    }
}
