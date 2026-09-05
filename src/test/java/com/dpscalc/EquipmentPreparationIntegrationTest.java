package com.dpscalc;

import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.PlayerStateManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class EquipmentPreparationIntegrationTest {
    @Test
    public void fixtureAndSnapshotAdaptersPrepareIdenticalBowfaQuiverAmmoState() throws Exception {
        // Given
        PlayerState fixture = FixtureStateFactory.buildPlayer(rawPlayer(), 415);
        PlayerState snapshot = new PlayerState();
        snapshot.setEquippedItemIds(rawIds());
        snapshot.setEquippedItemNames(fixture.getEquippedItemNames().clone());
        snapshot.setCombatStyle(CombatStyle.RANGED_RAPID);
        PlayerStateManager manager = new PlayerStateManager();
        setField(manager, "equipmentPreparation", new EquipmentPreparationFacade());

        // When
        manager.prepareEquipment(snapshot, 415);

        // Then
        assertArrayEquals(fixture.getEquippedItemIds(), snapshot.getEquippedItemIds());
        assertArrayEquals(fixture.getEquippedItemVersions(), snapshot.getEquippedItemVersions());
        assertArrayEquals(fixture.getEquippedItemCategories(), snapshot.getEquippedItemCategories());
        assertEquals(fixture.getWeaponSpeed(), snapshot.getWeaponSpeed());
        assertStatsEqual(fixture.getEquipmentStats(), snapshot.getEquipmentStats());
        boolean binaryEqual = Arrays.equals(fixture.getEquippedItemIds(), snapshot.getEquippedItemIds())
            && fixture.getEquipmentStats().toString().equals(snapshot.getEquipmentStats().toString())
            && fixture.getWeaponSpeed() == snapshot.getWeaponSpeed();
        System.out.println("Bowfa+quiver+ammo fixture totals=" + fixture.getEquipmentStats()
            + " snapshot totals=" + snapshot.getEquipmentStats() + " binaryEqual=" + binaryEqual);
        assertEquals(true, binaryEqual);
    }

    private static void assertStatsEqual(EquipmentStats expected, EquipmentStats actual) {
        assertEquals(expected.getStabAttack(), actual.getStabAttack());
        assertEquals(expected.getSlashAttack(), actual.getSlashAttack());
        assertEquals(expected.getCrushAttack(), actual.getCrushAttack());
        assertEquals(expected.getMagicAttack(), actual.getMagicAttack());
        assertEquals(expected.getRangedAttack(), actual.getRangedAttack());
        assertEquals(expected.getMeleeStrength(), actual.getMeleeStrength());
        assertEquals(expected.getRangedStrength(), actual.getRangedStrength());
        assertEquals(expected.getMagicDamage(), actual.getMagicDamage());
        assertEquals(expected.getPrayerBonus(), actual.getPrayerBonus());
        assertEquals(expected.getStabDefence(), actual.getStabDefence());
        assertEquals(expected.getSlashDefence(), actual.getSlashDefence());
        assertEquals(expected.getCrushDefence(), actual.getCrushDefence());
        assertEquals(expected.getMagicDefence(), actual.getMagicDefence());
        assertEquals(expected.getRangedDefence(), actual.getRangedDefence());
    }

    private static int[] rawIds() {
        int[] ids = new int[14];
        Arrays.fill(ids, -1);
        ids[EquipmentSlot.CAPE.getIndex()] = 28955;
        ids[EquipmentSlot.WEAPON.getIndex()] = 25867;
        ids[EquipmentSlot.AMMO.getIndex()] = 11212;
        return ids;
    }

    private static JsonObject rawPlayer() {
        return (new JsonParser()).parse("{"
            + "\"skills\":{\"atk\":99,\"str\":99,\"def\":99,\"ranged\":99,\"magic\":99,\"hp\":99},"
            + "\"buffs\":{},\"style\":{\"type\":\"ranged\",\"stance\":\"Rapid\"},"
            + "\"spell\":null,\"prayers\":[],\"equipment\":{"
            + "\"cape\":{\"id\":28955,\"itemVars\":{}},"
            + "\"weapon\":{\"id\":25867,\"itemVars\":{}},"
            + "\"ammo\":{\"id\":11212,\"itemVars\":{}}}}")
            .getAsJsonObject();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
