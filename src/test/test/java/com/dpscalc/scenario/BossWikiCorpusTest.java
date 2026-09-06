package com.dpscalc.scenario;

import static org.junit.Assert.*;

import com.dpscalc.wikisetups.wiki.*;
import com.google.gson.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Reviewed real-page fixtures: network access is never part of the normal test suite. */
@RunWith(Parameterized.class)
public class BossWikiCorpusTest {
    private final JsonObject page;

    public BossWikiCorpusTest(String key, JsonObject page) {
        this.page = page;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> pages() throws Exception {
        JsonArray pages = json("manifest.json").getAsJsonArray("pages");
        assertEquals(61, pages.size());
        Set<String> logEntries = new HashSet<>();
        List<Object[]> rows = new ArrayList<>();
        for (JsonElement element : pages) {
            JsonObject page = element.getAsJsonObject();
            logEntries.add(page.get("logEntry").getAsString());
            rows.add(new Object[] {page.get("key").getAsString(), page});
        }
        assertEquals(57, logEntries.size());
        return rows;
    }

    @Test
    public void reviewedGearAndEveryAutomaticInventoryPairRemainStable() throws Exception {
        String text = resource(page.get("file").getAsString());
        StringBuilder digest = new StringBuilder();
        for (byte b :
                MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)))
            digest.append(String.format("%02x", b & 255));
        assertEquals(
                "Source revision integrity", page.get("sha256").getAsString(), digest.toString());
        JsonObject expected = json("expected.json").getAsJsonObject(page.get("key").getAsString());
        List<WikiLoadouts.Choice> choices = WikiLoadouts.parse(text);
        List<InventorySetup> inventories = new InventoryParser().parse(text);
        assertEquals(
                expected.getAsJsonObject("counts").get("inventories").getAsInt(),
                inventories.size());
        JsonArray expectedChoices = expected.getAsJsonArray("choices");
        assertEquals(expectedChoices.size(), choices.size());
        for (int i = 0; i < choices.size(); i++) {
            WikiLoadouts.Choice choice = choices.get(i);
            JsonObject want = expectedChoices.get(i).getAsJsonObject();
            assertEquals(want.get("label").getAsString(), choice.label);
            assertEquals(
                    choice.label, want.get("gearSlots").getAsInt(), choice.gear.getSlots().size());
            List<String> weapons = new ArrayList<>();
            for (RankedItem weapon :
                    choice.gear.getSlots().getOrDefault(EquipSlot.WEAPON, List.of()))
                weapons.add(weapon.getName());
            assertEquals(choice.label, strings(want.getAsJsonArray("weapons")), weapons);
            int inventoryIndex = want.get("inventory").getAsInt();
            if (inventoryIndex < 0) assertNull(choice.label, choice.inventory);
            else {
                assertNotNull(choice.label, choice.inventory);
                InventorySetup inventory = inventories.get(inventoryIndex);
                assertEquals(choice.label, inventory.getSlots(), choice.inventory.getSlots());
                assertEquals(choice.label, inventory.getRunes(), choice.inventory.getRunes());
                for (int slot = 0; slot < 28; slot++) {
                    assertEquals(
                            choice.label,
                            inventory.quantity(slot, false),
                            choice.inventory.quantity(slot, false));
                    assertEquals(choice.label, inventory.noted(slot), choice.inventory.noted(slot));
                }
                for (int rune = 0; rune < 4; rune++)
                    assertEquals(
                            choice.label,
                            inventory.quantity(rune, true),
                            choice.inventory.quantity(rune, true));
            }
        }
        List<String> results =
                WikiSetupService.rankSearch(
                        page.get("query").getAsString(),
                        strings(page.getAsJsonArray("searchTitles")));
        if (expected.get("searchContainsPage").getAsBoolean())
            assertEquals(page.get("title").getAsString(), results.get(0));
        else
            assertTrue(
                    "Only prose-only guides lack an importable search result",
                    Set.of("bryophyta", "obor").contains(page.get("key").getAsString()));
    }

    static JsonObject json(String name) throws IOException {
        return new JsonParser().parse(resource(name)).getAsJsonObject();
    }

    static String resource(String name) throws IOException {
        try (InputStream in = BossWikiCorpusTest.class.getResourceAsStream("/boss-wiki/" + name)) {
            if (in == null) throw new IOException("Missing boss fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) values.add(element.getAsString());
        return values;
    }
}
