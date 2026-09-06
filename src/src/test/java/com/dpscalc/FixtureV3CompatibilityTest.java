package com.dpscalc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FixtureV3CompatibilityTest {
    @Test
    public void replayResourceContainsOnlyRawEquipmentInputs() throws Exception {
        JsonObject root;
        try (InputStream stream = getClass().getResourceAsStream("/fixtures.json")) {
            assertNotNull("fixtures.json", stream);
            root = (new JsonParser()).parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }

        Collection<Object[]> replayCases = FixtureReplayTest.loadFixtures();
        JsonObject first = root.getAsJsonArray("fixtures").get(0).getAsJsonObject();
        JsonObject equipment = first.getAsJsonObject("inputs").getAsJsonObject("player").getAsJsonObject("equipment");

        assertEquals(3, root.get("schemaVersion").getAsInt());
        assertEquals(150, replayCases.size());
        assertFalse(equipment.has("stats"));
        assertFalse(equipment.has("weaponSpeed"));
        assertEquals(4151, equipment.getAsJsonObject("weapon").get("id").getAsInt());
        assertTrue(equipment.getAsJsonObject("weapon").has("itemVars"));
        assertTrue(first.getAsJsonObject("outputs").has("equipment"));
    }
}
