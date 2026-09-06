package com.dpscalc;

import com.dpscalc.equipment.EquipmentCombatStyle;
import com.dpscalc.equipment.EquipmentContext;
import com.dpscalc.equipment.EquipmentDomainException;
import com.dpscalc.equipment.EquipmentItem;
import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.equipment.ItemVariable;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumMap;
import java.util.Map;

final class FixtureEquipmentAdapter {
    private static final int SLOT_COUNT = 14;
    private static final EquipmentPreparationFacade PREPARATION = new EquipmentPreparationFacade();

    private FixtureEquipmentAdapter() {}

    static void apply(PlayerState state, JsonObject playerJson, int monsterId) {
        JsonObject equipment = playerJson.getAsJsonObject("equipment");
        if (equipment.has("stats")) {
            applyLegacy(state, equipment, playerJson.getAsJsonObject("style"));
            return;
        }
        EquipmentLoadout loadout = rawLoadout(equipment);
        JsonObject style = playerJson.getAsJsonObject("style");
        EquipmentContext context = EquipmentContext.of(
            monsterId,
            EquipmentCombatStyle.of(style.get("type").getAsString(), style.get("stance").getAsString()),
            spellbook(playerJson.get("spell"))
        );
        PREPARATION.prepare(state, loadout, null, context);
    }

    private static EquipmentLoadout rawLoadout(JsonObject equipment) {
        Map<com.dpscalc.equipment.EquipmentSlot, EquipmentItem> items = new EnumMap<>(com.dpscalc.equipment.EquipmentSlot.class);
        for (com.dpscalc.equipment.EquipmentSlot slot : com.dpscalc.equipment.EquipmentSlot.values()) {
            JsonElement element = equipment.get(slot.name().toLowerCase(java.util.Locale.ROOT));
            if (element == null || element.isJsonNull()) continue;
            JsonObject item = element.getAsJsonObject();
            items.put(slot, EquipmentItem.raw(item.get("id").getAsInt(), itemVariables(item.getAsJsonObject("itemVars"))));
        }
        return EquipmentLoadout.of(items);
    }

    private static Map<String, ItemVariable> itemVariables(JsonObject json) {
        Map<String, ItemVariable> values = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive()) throw new EquipmentDomainException("itemVars." + entry.getKey(), "expected primitive");
            if (value.getAsJsonPrimitive().isBoolean()) values.put(entry.getKey(), ItemVariable.ofBoolean(value.getAsBoolean()));
            else if (value.getAsJsonPrimitive().isNumber()) values.put(entry.getKey(), ItemVariable.ofNumber(value.getAsDouble()));
            else if (value.getAsJsonPrimitive().isString()) values.put(entry.getKey(), ItemVariable.ofString(value.getAsString()));
            else throw new EquipmentDomainException("itemVars." + entry.getKey(), "unsupported primitive");
        }
        return values;
    }

    private static String spellbook(JsonElement spell) {
        if (spell == null || spell.isJsonNull()) return "";
        if (!spell.isJsonObject()) throw new EquipmentDomainException("spell", "expected raw spell object");
        return spell.getAsJsonObject().get("spellbook").getAsString();
    }

    private static void applyLegacy(PlayerState state, JsonObject equipment, JsonObject style) {
        state.setEquipmentStats(legacyStats(equipment.getAsJsonObject("stats")));
        state.setEquippedItemIds(legacyIds(equipment.getAsJsonObject("itemIds")));
        state.setEquippedItemNames(legacyStrings(equipment.getAsJsonObject("itemNames")));
        state.setEquippedItemVersions(legacyItemField(equipment.getAsJsonObject("slots"), "version"));
        state.setEquippedItemCategories(legacyItemField(equipment.getAsJsonObject("slots"), "category"));
        int speed = equipment.get("weaponSpeed").getAsInt();
        if ("Rapid".equals(style.get("stance").getAsString())) speed += 1;
        state.setWeaponSpeed(speed);
    }

    private static EquipmentStats legacyStats(JsonObject json) {
        JsonObject bonuses = json.getAsJsonObject("bonuses"); JsonObject offensive = json.getAsJsonObject("offensive");
        JsonObject defensive = json.getAsJsonObject("defensive"); EquipmentStats stats = new EquipmentStats();
        stats.setStabAttack(value(offensive, "stab")); stats.setSlashAttack(value(offensive, "slash"));
        stats.setCrushAttack(value(offensive, "crush")); stats.setMagicAttack(value(offensive, "magic"));
        stats.setRangedAttack(value(offensive, "ranged")); stats.setMeleeStrength(value(bonuses, "str"));
        stats.setRangedStrength(value(bonuses, "ranged_str")); stats.setMagicDamage(value(bonuses, "magic_str"));
        stats.setPrayerBonus(value(bonuses, "prayer")); stats.setStabDefence(value(defensive, "stab"));
        stats.setSlashDefence(value(defensive, "slash")); stats.setCrushDefence(value(defensive, "crush"));
        stats.setMagicDefence(value(defensive, "magic")); stats.setRangedDefence(value(defensive, "ranged"));
        return stats;
    }

    private static int[] legacyIds(JsonObject json) {
        int[] values = new int[SLOT_COUNT];
        slotNames().forEach((name, slot) -> values[slot.getIndex()] = value(json, name));
        return values;
    }

    private static String[] legacyStrings(JsonObject json) {
        String[] values = new String[SLOT_COUNT];
        slotNames().forEach((name, slot) -> values[slot.getIndex()] = nullableString(json.get(name)));
        return values;
    }

    private static String[] legacyItemField(JsonObject slots, String field) {
        String[] values = new String[SLOT_COUNT];
        slotNames().forEach((name, slot) -> {
            JsonElement item = slots.get(name);
            values[slot.getIndex()] = item == null || item.isJsonNull() ? null : nullableString(item.getAsJsonObject().get(field));
        });
        return values;
    }

    private static Map<String, com.dpscalc.state.EquipmentSlot> slotNames() {
        Map<String, com.dpscalc.state.EquipmentSlot> slots = new java.util.LinkedHashMap<>();
        slots.put("head", com.dpscalc.state.EquipmentSlot.HEAD); slots.put("cape", com.dpscalc.state.EquipmentSlot.CAPE);
        slots.put("neck", com.dpscalc.state.EquipmentSlot.AMULET); slots.put("weapon", com.dpscalc.state.EquipmentSlot.WEAPON);
        slots.put("body", com.dpscalc.state.EquipmentSlot.BODY); slots.put("shield", com.dpscalc.state.EquipmentSlot.SHIELD);
        slots.put("legs", com.dpscalc.state.EquipmentSlot.LEGS); slots.put("hands", com.dpscalc.state.EquipmentSlot.GLOVES);
        slots.put("feet", com.dpscalc.state.EquipmentSlot.BOOTS); slots.put("ring", com.dpscalc.state.EquipmentSlot.RING);
        slots.put("ammo", com.dpscalc.state.EquipmentSlot.AMMO); return slots;
    }

    private static int value(JsonObject json, String key) { JsonElement value = json.get(key); return value == null || value.isJsonNull() ? 0 : value.getAsInt(); }
    private static String nullableString(JsonElement value) { return value == null || value.isJsonNull() ? null : value.getAsString(); }

}
