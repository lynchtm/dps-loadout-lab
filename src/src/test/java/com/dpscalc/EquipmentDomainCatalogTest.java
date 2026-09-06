package com.dpscalc;

import com.dpscalc.equipment.EquipmentDomainCatalog;
import com.dpscalc.equipment.EquipmentDomainException;
import com.dpscalc.equipment.EquipmentItem;
import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.EquipmentSlot;
import com.dpscalc.equipment.ItemVariable;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class EquipmentDomainCatalogTest {
    @Test
    public void loadsPinnedFiniteCatalogRules() throws IOException {
        EquipmentDomainCatalog catalog = loadCatalog();

        assertEquals(5329, catalog.getEquipmentCount());
        assertEquals(327, catalog.getAliasGroupCount());
        assertEquals(69, catalog.getWeaponRuleCount());
    }

    @Test
    public void canonicalizationPreservesOriginalIdAndItemVariables() throws IOException {
        EquipmentDomainCatalog catalog = loadCatalog();
        Map<String, ItemVariable> variables = Map.of(
            "charge", ItemVariable.ofNumber(42),
            "preserved", ItemVariable.ofBoolean(true)
        );
        EquipmentItem original = EquipmentItem.raw(33021, variables);

        EquipmentItem canonical = catalog.canonicalize(original);

        assertNotSame(original, canonical);
        assertEquals(33021, canonical.getOriginalId());
        assertEquals(25867, canonical.getCanonicalId());
        assertEquals(variables, canonical.getItemVariables());
        assertThrows(UnsupportedOperationException.class,
            () -> canonical.getItemVariables().put("mutate", ItemVariable.ofString("blocked")));
    }

    @Test
    public void canonicalizesImmutableSlotKeyedLoadout() throws IOException {
        EquipmentLoadout original = EquipmentLoadout.of(Map.of(
            EquipmentSlot.WEAPON, EquipmentItem.raw(33021, Map.of("charge", ItemVariable.ofNumber(7))),
            EquipmentSlot.AMMO, EquipmentItem.raw(9244, Map.of())
        ));

        EquipmentLoadout canonical = loadCatalog().canonicalize(original);

        assertEquals(25867, canonical.get(EquipmentSlot.WEAPON).getCanonicalId());
        assertEquals(7.0, canonical.get(EquipmentSlot.WEAPON).getItemVariables().get("charge").getNumberValue(), 0.0);
        assertEquals(9244, canonical.get(EquipmentSlot.AMMO).getCanonicalId());
        assertThrows(UnsupportedOperationException.class,
            () -> canonical.asMap().put(EquipmentSlot.HEAD, EquipmentItem.raw(1, Map.of())));
    }

    @Test
    public void rejectsMalformedStaleAndDuplicateRules() throws IOException {
        EquipmentDomainException malformed = assertThrows(EquipmentDomainException.class,
            () -> EquipmentDomainCatalog.load(bytes("{"), referenceSha(), domainDigest()));
        assertTrue(malformed.getMessage().contains("$: malformed JSON"));

        try (InputStream stream = getClass().getResourceAsStream("/equipment-domain.json")) {
            EquipmentDomainException stale = assertThrows(EquipmentDomainException.class,
                () -> EquipmentDomainCatalog.load(stream, referenceSha(), "0".repeat(64)));
            assertTrue(stale.getMessage().contains("$.contentDigest"));
        }

        JsonObject duplicate = loadDomainJson();
        duplicate.getAsJsonArray("weaponAmmo").add(duplicate.getAsJsonArray("weaponAmmo").get(0).deepCopy());
        EquipmentDomainException duplicateRule = assertThrows(EquipmentDomainException.class,
            () -> EquipmentDomainCatalog.load(bytes(duplicate.toString()), referenceSha(), domainDigest()));
        assertTrue(duplicateRule.getMessage().contains("$.weaponAmmo[69].weaponId: duplicate"));
    }

    static EquipmentDomainCatalog loadCatalog() throws IOException {
        try (InputStream stream = EquipmentDomainCatalogTest.class.getResourceAsStream("/equipment-domain.json")) {
            if (stream == null) throw new AssertionError("equipment-domain.json missing");
            return EquipmentDomainCatalog.load(
                stream,
                referenceSha(),
                domainDigest()
            );
        }
    }

    private static JsonObject loadDomainJson() throws IOException {
        try (InputStream stream = EquipmentDomainCatalogTest.class.getResourceAsStream("/equipment-domain.json")) {
            if (stream == null) throw new AssertionError("equipment-domain.json missing");
            return (new JsonParser()).parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static ByteArrayInputStream bytes(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String referenceSha() {
        return "b6bc098dc0d742b2b763375d2e78e1b611a22070";
    }

    private static String domainDigest() {
        return "070a34ce7f6267be1ae8c84cfdd3757100f5a027898ba34937eaba16536ea951";
    }
}
