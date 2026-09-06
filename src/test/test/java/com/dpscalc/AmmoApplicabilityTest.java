package com.dpscalc;

import com.dpscalc.equipment.AmmoApplicability;
import com.dpscalc.equipment.AmmoApplicabilityResult;
import com.dpscalc.equipment.EquipmentDomainCatalog;
import com.dpscalc.equipment.EquipmentItem;
import com.dpscalc.equipment.ItemVariable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AmmoApplicabilityTest {
    @Test
    public void exhaustiveOracleMatchesCanonicalizationAndEveryFiniteAmmoRule() throws IOException {
        EquipmentDomainCatalog catalog = EquipmentDomainCatalogTest.loadCatalog();
        JsonObject oracle = loadOracle();
        int canonicalCount = 0;
        for (JsonElement element : oracle.getAsJsonArray("canonicalRows")) {
            JsonObject row = element.getAsJsonObject();
            EquipmentItem canonical = catalog.canonicalize(EquipmentItem.raw(row.get("originalId").getAsInt(), itemVars(row.getAsJsonObject("itemVars"))));
            assertEquals("original=" + row.get("originalId"), row.get("canonicalId").getAsInt(), canonical.getCanonicalId());
            assertEquals(itemVars(row.getAsJsonObject("itemVars")), canonical.getItemVariables());
            canonicalCount++;
        }

        int ammoCount = 0;
        for (JsonElement element : oracle.getAsJsonArray("ammoRows")) {
            JsonObject row = element.getAsJsonObject();
            AmmoApplicabilityResult result = catalog.evaluate(item(row.get("originalWeaponId")), item(row.get("originalAmmoId")));
            assertExpected(AmmoApplicability.valueOf(row.get("expected").getAsString()), result, row.get("caseId").getAsString());
            ammoCount++;
        }
        assertEquals(oracle.get("canonicalRowCount").getAsInt(), canonicalCount);
        assertEquals(oracle.get("ammoRowCount").getAsInt(), ammoCount);
    }

    @Test
    public void corruptedBowfaWithoutAmmoIsAllowed() throws IOException {
        AmmoApplicabilityResult result = EquipmentDomainCatalogTest.loadCatalog().evaluate(
            EquipmentItem.raw(33021, Map.of("charge", ItemVariable.ofNumber(17))),
            null
        );

        assertEquals(AmmoApplicability.ALLOWED, result.getApplicability());
        assertEquals(Integer.valueOf(33021), result.getOriginalWeaponId());
        assertEquals(Integer.valueOf(25867), result.getCanonicalWeaponId());
    }

    @Test
    public void runeCrossbowWithDragonBoltsIsInvalid() throws IOException {
        AmmoApplicabilityResult result = EquipmentDomainCatalogTest.loadCatalog().evaluate(
            EquipmentItem.raw(9185, Collections.emptyMap()),
            EquipmentItem.raw(21905, Collections.emptyMap())
        );

        assertEquals(AmmoApplicability.INVALID, result.getApplicability());
    }

    @Test
    public void reportsWeaponAndAmmoIdsOnMismatch() throws IOException {
        AmmoApplicabilityResult result = EquipmentDomainCatalogTest.loadCatalog().evaluate(
            EquipmentItem.raw(26486, Collections.emptyMap()),
            EquipmentItem.raw(9244, Collections.emptyMap())
        );

        AssertionError error = assertThrows(AssertionError.class,
            () -> assertExpected(AmmoApplicability.INVALID, result, "intentional-mismatch"));
        assertTrue(error.getMessage().contains("weaponOriginal=26486"));
        assertTrue(error.getMessage().contains("weaponCanonical=9185"));
        assertTrue(error.getMessage().contains("ammoOriginal=9244"));
        assertTrue(error.getMessage().contains("ammoCanonical=9244"));
    }

    private static void assertExpected(AmmoApplicability expected, AmmoApplicabilityResult result, String caseId) {
        if (result.getApplicability() != expected) {
            throw new AssertionError(caseId + " expected=" + expected + " actual=" + result.getApplicability() + " " + result.describeIds());
        }
    }

    private static EquipmentItem item(JsonElement id) {
        return id.isJsonNull() ? null : EquipmentItem.raw(id.getAsInt(), Collections.emptyMap());
    }

    private static Map<String, ItemVariable> itemVars(JsonObject object) {
        Map<String, ItemVariable> variables = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (value.getAsJsonPrimitive().isBoolean()) variables.put(entry.getKey(), ItemVariable.ofBoolean(value.getAsBoolean()));
            else if (value.getAsJsonPrimitive().isNumber()) variables.put(entry.getKey(), ItemVariable.ofNumber(value.getAsDouble()));
            else variables.put(entry.getKey(), ItemVariable.ofString(value.getAsString()));
        }
        return variables;
    }

    private static JsonObject loadOracle() throws IOException {
        try (InputStream stream = AmmoApplicabilityTest.class.getResourceAsStream("/equipment-oracle.json")) {
            if (stream == null) throw new AssertionError("equipment-oracle.json missing");
            return (new JsonParser()).parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
