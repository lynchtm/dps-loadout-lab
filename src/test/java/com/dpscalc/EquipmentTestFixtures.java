package com.dpscalc;

import com.dpscalc.equipment.EquipmentItem;
import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.EquipmentSlot;
import com.dpscalc.equipment.ItemVariable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class EquipmentTestFixtures {
    private static final JsonObject DOMAIN = loadDomain();

    private EquipmentTestFixtures() {}

    static EquipmentItem item(String name) {
        return item(name, null, Map.of());
    }

    static EquipmentItem item(int id) {
        factsById(id);
        return EquipmentItem.raw(id, Map.of());
    }

    static EquipmentItem item(String name, String version) {
        return item(name, version, Map.of());
    }

    static EquipmentItem item(String name, Map<String, ItemVariable> variables) {
        return item(name, null, variables);
    }

    static EquipmentItem item(String name, String version, Map<String, ItemVariable> variables) {
        return EquipmentItem.raw(facts(name, version).get("id").getAsInt(), variables);
    }

    static EquipmentLoadout loadout(Object... slotItems) {
        Map<EquipmentSlot, EquipmentItem> items = new LinkedHashMap<>();
        for (int index = 0; index < slotItems.length; index += 2) {
            items.put((EquipmentSlot) slotItems[index], (EquipmentItem) slotItems[index + 1]);
        }
        return EquipmentLoadout.of(items);
    }

    static JsonObject facts(String name) {
        return facts(name, null);
    }

    static JsonObject ammoWithRetainedStats() {
        for (JsonElement element : DOMAIN.getAsJsonArray("equipment")) {
            JsonObject item = element.getAsJsonObject();
            if (!"ammo".equals(item.get("slot").getAsString())) continue;
            int retained = stat(item, "bonuses", "prayer")
                + stat(item, "defensive", "stab") + stat(item, "defensive", "slash")
                + stat(item, "defensive", "crush") + stat(item, "defensive", "magic")
                + stat(item, "defensive", "ranged");
            if (retained != 0) return item;
        }
        throw new AssertionError("ammo item with retained stats missing");
    }

    static JsonObject factsById(int id) {
        for (JsonElement element : DOMAIN.getAsJsonArray("equipment")) {
            JsonObject item = element.getAsJsonObject();
            if (item.get("id").getAsInt() == id) return item;
        }
        throw new AssertionError("equipment id=" + id + " missing");
    }

    static int stat(JsonObject item, String block, String field) {
        JsonElement value = item.getAsJsonObject(block).get(field);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    private static JsonObject facts(String name, String version) {
        JsonArray equipment = DOMAIN.getAsJsonArray("equipment");
        for (JsonElement element : equipment) {
            JsonObject item = element.getAsJsonObject();
            if (!name.equals(item.get("name").getAsString())) continue;
            if (version == null || version.equals(item.get("version").getAsString())) return item;
        }
        throw new AssertionError(name + " version=" + version + " missing");
    }

    private static JsonObject loadDomain() {
        try (InputStream stream = EquipmentTestFixtures.class.getResourceAsStream("/equipment-domain.json")) {
            if (stream == null) throw new AssertionError("equipment-domain.json missing");
            return (new JsonParser()).parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException error) {
            throw new AssertionError("equipment-domain.json unreadable", error);
        }
    }
}
