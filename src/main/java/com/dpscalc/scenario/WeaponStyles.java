// SPDX-License-Identifier: BSD-2-Clause
package com.dpscalc.scenario;

import static com.loadoutlab.model.AttackType.*;

import com.loadoutlab.equipment.*;
import com.loadoutlab.model.*;

import java.util.*;

/** Independently specified weapon-interface facts from Wiki Combat options documentation. */
public final class WeaponStyles {
    private WeaponStyles() {}

    private static final Map<String, List<CombatStyle>> OPTIONS = load();

    private static Map<String, List<CombatStyle>> load() {
        Map<String, List<CombatStyle>> map = new HashMap<>();
        try (java.io.Reader reader =
                new java.io.InputStreamReader(
                        Objects.requireNonNull(
                                WeaponStyles.class.getResourceAsStream(
                                        "/com/loadoutlab/weapon-options.json")),
                        java.nio.charset.StandardCharsets.UTF_8)) {
            com.google.gson.JsonObject root =
                    new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : root.entrySet()) {
                List<CombatStyle> styles = new ArrayList<>();
                for (com.google.gson.JsonElement value : entry.getValue().getAsJsonArray()) {
                    com.google.gson.JsonObject row = value.getAsJsonObject(),
                            bonuses = row.getAsJsonObject("bonuses");
                    String kind = row.get("type").getAsString().toUpperCase(Locale.ROOT);
                    if (kind.equals("LIGHT") || kind.equals("STANDARD") || kind.equals("HEAVY"))
                        kind = "RANGED_" + kind;
                    if (kind.equals("RANGED")) kind = "RANGED_STANDARD";
                    styles.add(
                            new CombatStyle(
                                    row.get("name").getAsString(),
                                    AttackType.valueOf(kind),
                                    row.get("stance").getAsString(),
                                    bonus(bonuses, "attack"),
                                    bonus(bonuses, "strength"),
                                    bonus(bonuses, "defence"),
                                    bonus(bonuses, "ranged"),
                                    bonus(bonuses, "magic")));
                }
                map.put(entry.getKey(), Collections.unmodifiableList(styles));
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Weapon option facts are unavailable", e);
        }
        return Collections.unmodifiableMap(map);
    }

    private static int bonus(com.google.gson.JsonObject row, String key) {
        return row.has(key) ? row.get(key).getAsInt() : 0;
    }

    public static List<CombatStyle> available(
            PlayerState player, EquipmentPreparationFacade equipment) {
        if (player.getWeaponId() <= 0) return OPTIONS.get("unarmed");
        EquipmentCatalogItem weapon = equipment.getItemFacts(player.getWeaponId());
        return weapon == null || weapon.getSpeed() <= 0
                ? List.of()
                : OPTIONS.getOrDefault(weapon.getCategory().toLowerCase(Locale.ROOT), List.of());
    }

    public static void select(Scenario.Loadout loadout, CombatStyle style) {
        loadout.player.setCombatStyle(style);
        for (String path :
                Scenario.flatten(Scenario.JSON.toJsonTree(loadout.player).getAsJsonObject())
                        .keySet()) if (path.startsWith("combatStyle.")) loadout.overrides.add(path);
    }

    public static void normalize(Scenario.Loadout loadout, EquipmentPreparationFacade equipment) {
        List<CombatStyle> choices = available(loadout.player, equipment);
        if (choices.isEmpty()) return;
        CombatStyle current = loadout.player.getCombatStyle();
        for (CombatStyle option : choices)
            if (current != null
                    && option.getAttackType() == current.getAttackType()
                    && option.getStance().equals(current.getStance())) {
                loadout.player.setCombatStyle(option);
                return;
            }
        select(loadout, choices.get(0));
    }
}
