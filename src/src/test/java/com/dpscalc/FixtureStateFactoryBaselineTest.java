package com.dpscalc;

import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FixtureStateFactoryBaselineTest {
    @Test
    public void currentV2FactoryCopiesEverySerializedEquipmentPath() throws Exception {
        // Given
        JsonObject player = legacyPlayer();
        JsonObject equipment = player.getAsJsonObject("equipment");
        equipment.addProperty("weaponSpeed", 3);
        player.getAsJsonObject("style").addProperty("type", "ranged");
        player.getAsJsonObject("style").addProperty("stance", "Rapid");
        equipment.add("stats", (new JsonParser()).parse("{"
            + "\"bonuses\":{\"str\":1,\"ranged_str\":2,\"magic_str\":3,\"prayer\":4},"
            + "\"offensive\":{\"stab\":5,\"slash\":6,\"crush\":7,\"magic\":8,\"ranged\":9},"
            + "\"defensive\":{\"stab\":10,\"slash\":11,\"crush\":12,\"magic\":13,\"ranged\":14}}")
            .getAsJsonObject());

        // When
        PlayerState state = FixtureStateFactory.buildPlayer(player);

        // Then
        EquipmentStats stats = state.getEquipmentStats();
        assertEquals(1, stats.getMeleeStrength());
        assertEquals(2, stats.getRangedStrength());
        assertEquals(3, stats.getMagicDamage());
        assertEquals(4, stats.getPrayerBonus());
        assertEquals(5, stats.getStabAttack());
        assertEquals(6, stats.getSlashAttack());
        assertEquals(7, stats.getCrushAttack());
        assertEquals(8, stats.getMagicAttack());
        assertEquals(9, stats.getRangedAttack());
        assertEquals(10, stats.getStabDefence());
        assertEquals(11, stats.getSlashDefence());
        assertEquals(12, stats.getCrushDefence());
        assertEquals(13, stats.getMagicDefence());
        assertEquals(14, stats.getRangedDefence());
        assertEquals(4151, state.getWeaponId());
        assertEquals("Abyssal whip", state.getWeaponName());
        assertEquals("", state.getWeaponVersion());
        assertEquals("Whip", state.getWeaponCategory());
        assertEquals(4, state.getWeaponSpeed());
        assertEquals(-1, equipment.getAsJsonObject("itemIds").get("head").getAsInt());
        assertEquals(-1, state.getEquippedItemIds()[EquipmentSlot.HEAD.getIndex()]);
    }

    private static JsonObject legacyPlayer() {
        return (new JsonParser()).parse("{"
            + "\"skills\":{\"atk\":99,\"str\":99,\"def\":99,\"ranged\":99,\"magic\":99,\"hp\":99},"
            + "\"buffs\":{},\"style\":{\"type\":\"melee\",\"stance\":\"Accurate\"},"
            + "\"spell\":null,\"prayers\":[],\"equipment\":{"
            + "\"itemIds\":{\"head\":-1,\"cape\":-1,\"neck\":-1,\"weapon\":4151,\"body\":-1,"
            + "\"shield\":-1,\"legs\":-1,\"hands\":-1,\"feet\":-1,\"ring\":-1,\"ammo\":-1},"
            + "\"itemNames\":{\"weapon\":\"Abyssal whip\"},"
            + "\"slots\":{\"weapon\":{\"version\":\"\",\"category\":\"Whip\"}}}}")
            .getAsJsonObject();
    }
}
