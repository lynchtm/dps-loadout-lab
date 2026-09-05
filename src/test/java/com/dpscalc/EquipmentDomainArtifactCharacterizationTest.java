package com.dpscalc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EquipmentDomainArtifactCharacterizationTest {
    @Test
    public void pinnedArtifactContainsExpectedFiniteRuleInputs() throws IOException {
        JsonObject domain;
        try (InputStream stream = getClass().getResourceAsStream("/equipment-domain.json")) {
            assertNotNull("equipment-domain.json", stream);
            domain = (new JsonParser()).parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }

        assertEquals(1, domain.get("schemaVersion").getAsInt());
        assertEquals("b6bc098dc0d742b2b763375d2e78e1b611a22070", domain.get("sourceSha").getAsString());
        assertEquals("070a34ce7f6267be1ae8c84cfdd3757100f5a027898ba34937eaba16536ea951", domain.get("contentDigest").getAsString());
        assertEquals(5329, domain.getAsJsonArray("equipment").size());
        assertEquals(327, domain.getAsJsonArray("aliases").size());
        assertEquals(69, domain.getAsJsonArray("weaponAmmo").size());
        assertTrue(aliasVariants(domain, 25867).contains(new com.google.gson.JsonPrimitive(33021)));
        assertTrue(aliasVariants(domain, 12926).contains(new com.google.gson.JsonPrimitive(28688)));
        assertTrue(ammoIds(domain, 9185).contains(new com.google.gson.JsonPrimitive(9244)));
    }

    private static JsonArray aliasVariants(JsonObject domain, int canonicalId) {
        return findById(domain.getAsJsonArray("aliases"), "canonicalId", canonicalId).getAsJsonArray("variantIds");
    }

    private static JsonArray ammoIds(JsonObject domain, int weaponId) {
        return findById(domain.getAsJsonArray("weaponAmmo"), "weaponId", weaponId).getAsJsonArray("includedAmmoIds");
    }

    private static JsonObject findById(JsonArray entries, String field, int id) {
        for (JsonElement entry : entries) {
            JsonObject object = entry.getAsJsonObject();
            if (object.get(field).getAsInt() == id) return object;
        }
        throw new AssertionError(field + "=" + id + " missing");
    }
}
