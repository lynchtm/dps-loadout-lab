package com.dpscalc.scenario;

import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.PlayerState;
import com.google.gson.*;
import java.util.*;

/** Saved snapshots have no mutable references to comparison drafts. */
public final class LoadoutLibrary {
    public static final String KEY = "loadoutLibraryV1";
    private final Map<String, Scenario.Loadout> templates = new LinkedHashMap<>();

    public List<String> names() { return new ArrayList<>(templates.keySet()); }

    public void saveNew(String name, Scenario.Loadout draft) {
        name = validName(name);
        if (templates.containsKey(name)) throw new IllegalArgumentException("Name already saved. Choose another name or use Replace saved.");
        if (templates.size() >= 64) throw new IllegalArgumentException("The library holds up to 64 saved templates.");
        templates.put(name, snapshot(name, draft));
    }

    public void replace(String name, Scenario.Loadout draft) {
        if (!templates.containsKey(name)) throw new IllegalArgumentException("Choose a saved template to replace.");
        templates.put(name, snapshot(name, draft));
    }

    public void remove(String name) { templates.remove(name); }

    public Scenario.Loadout createDraft(String name) {
        Scenario.Loadout template = templates.get(name);
        if (template == null) throw new IllegalArgumentException("Saved template unavailable.");
        return template.copy();
    }

    private static String validName(String name) {
        if (name == null || name.trim().isEmpty() || name.trim().length() > 80)
            throw new IllegalArgumentException("Use a template name between 1 and 80 characters.");
        return name.trim();
    }

    private static Scenario.Loadout snapshot(String name, Scenario.Loadout draft) {
        Scenario.validate(draft);
        Scenario.Loadout copy = draft.copy(); copy.name = name; copy.inventory = new int[0];
        // A template is a fixed starting point, including empty gear slots. Live refresh
        // must not silently replace its equipment, style, prayers or saved levels.
        for (String path : Scenario.flatten(Scenario.JSON.toJsonTree(copy.player).getAsJsonObject()).keySet()) {
            if (Scenario.isLive(path)) { copy.overrides.add(path); copy.sources.put(path, "Template snapshot"); }
        }
        return copy;
    }

    public String toJson() {
        JsonObject root = new JsonObject(); root.addProperty("schemaVersion", 1);
        root.add("templates", Scenario.JSON.toJsonTree(templates)); return Scenario.JSON.toJson(root);
    }

    public static LoadoutLibrary parse(String json) {
        LoadoutLibrary library = new LoadoutLibrary(); if (json == null) return library;
        if (json.length() > 2_000_000) throw new IllegalArgumentException("Saved template library is too large.");
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        if (root.get("schemaVersion").getAsInt() != 1) throw new IllegalArgumentException("Unsupported template library version.");
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("templates").entrySet()) {
            // Reuse the scenario parser's bounds and complete loadout validation.
            JsonObject wrapper = new JsonObject(); JsonArray loadouts = new JsonArray(); loadouts.add(entry.getValue());
            wrapper.add("loadouts", loadouts);
            Scenario.Loadout loadout = Scenario.parse(wrapper.toString()).loadouts.get(0);
            library.saveNew(entry.getKey(), loadout);
        }
        return library;
    }

    public static Scenario.Loadout starter(int index, PlayerState levels, EquipmentPreparationFacade equipment) {
        Scenario.Loadout draft = new Scenario.Loadout();
        PlayerState p = draft.player;
        p.setAttackLevel(levels.getAttackLevel()); p.setStrengthLevel(levels.getStrengthLevel());
        p.setDefenceLevel(levels.getDefenceLevel()); p.setRangedLevel(levels.getRangedLevel());
        p.setMagicLevel(levels.getMagicLevel()); p.setPrayerLevel(levels.getPrayerLevel());
        p.setHitpointsLevel(levels.getHitpointsLevel()); p.setCurrentHitpoints(levels.getHitpointsLevel());
        int[][] items;
        if (index == 0) {
            draft.name = "Whip melee"; p.setCombatStyle(CombatStyle.MELEE_ACCURATE_SLASH);
            items = new int[][]{{3,4151},{4,1127},{7,1079},{5,12954}};
        } else if (index == 1) {
            draft.name = "Crossbow ranged"; p.setCombatStyle(CombatStyle.RANGED_RAPID);
            items = new int[][]{{3,9185},{4,2503},{7,2497},{13,11875}};
        } else if (index == 2) {
            draft.name = "Trident magic"; p.setCombatStyle(CombatStyle.MAGIC_ACCURATE);
            items = new int[][]{{3,11905},{4,4091},{7,4093}};
        } else throw new IllegalArgumentException("Unknown starter template.");
        for (int[] item : items) {
            p.getEquippedItemIds()[item[0]] = item[1];
            p.getEquippedItemNames()[item[0]] = equipment.getItemFacts(item[1]).getName();
        }
        draft.description = "Starter setup. Uses your selected draft's base levels; review equipment, prayers and boosts.";
        return snapshot(draft.name, draft);
    }
}
