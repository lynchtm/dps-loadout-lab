package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.state.AttackType;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.PlayerState;
import org.junit.Test;

import java.util.Arrays;

import static com.dpscalc.TestUtils.monster;
import static com.dpscalc.TestUtils.player;
import static org.junit.Assert.assertEquals;

public class RangedAmmoBaselineTest {
    private static final CombatStyle MANUAL_CAST = new CombatStyle(
        "Manual Cast", AttackType.MAGIC, "Manual Cast", 0, 0, 0, 0, 0);

    @Test
    public void existingThrownAndBowCategoryOutputsRemainStable() {
        MonsterStats target = distinctRangedTarget();

        assertEquals(32264, calculatorWithCategory(target, "Thrown").getNpcDefenceRoll());
        assertEquals(24198, calculatorWithCategory(target, "Bow").getNpcDefenceRoll());
    }

    @Test
    public void currentNameAndIdAmmoChecksProduceStableRollAndHitOutputs() {
        MonsterStats target = distinctRangedTarget();

        assertOutputs(ammoPlayer(9185, "Rune crossbow", "Crossbow", 9144, "Rune bolts"), target, 17548, 27);
        assertOutputs(ammoPlayer(9185, "Rune crossbow", "Crossbow", 892, "Rune arrow"), target, 0, 0);
        assertOutputs(ammoPlayer(9185, "Rune crossbow", "Crossbow", -1, null), target, 0, 0);
        assertOutputs(ammoPlayer(25865, "Bow of faerdhinen", "Bow", -1, null), target, 17548, 27);

        PlayerState manual = ammoPlayer(9185, "Rune crossbow", "Crossbow", 892, "Rune arrow");
        manual.setCombatStyle(MANUAL_CAST);
        manual.setMagicLevel(99);
        manual.getEquipmentStats().setMagicAttack(100);
        manual.setSpellName("Fire Wave");
        manual.setSpellMaxHit(20);
        assertOutputs(manual, target, 17712, 20);
    }

    private static DpsCalculator calculatorWithCategory(MonsterStats target, String category) {
        PlayerState state = player().combatStyle(CombatStyle.RANGED_RAPID).build();
        state.getEquippedItemCategories()[EquipmentSlot.WEAPON.getIndex()] = category;
        return new DpsCalculator(state, target);
    }

    private static PlayerState ammoPlayer(int weaponId, String weaponName, String category,
                                          int ammoId, String ammoName) {
        PlayerState state = player().rangedAttack(100).rangedStrength(100)
            .combatStyle(CombatStyle.RANGED_RAPID).build();
        int[] itemIds = new int[14];
        Arrays.fill(itemIds, -1);
        String[] names = new String[14];
        int weaponIndex = EquipmentSlot.WEAPON.getIndex();
        int ammoIndex = EquipmentSlot.AMMO.getIndex();
        itemIds[weaponIndex] = weaponId;
        itemIds[ammoIndex] = ammoId;
        names[weaponIndex] = weaponName;
        names[ammoIndex] = ammoName;
        new EquipmentPreparationFacade().prepare(state, itemIds, names,
            EquipmentPreparationFacade.context(state, 1));
        assertEquals(category, state.getWeaponCategory());
        state.getEquipmentStats().setRangedAttack(100);
        state.getEquipmentStats().setRangedStrength(100);
        return state;
    }

    private static MonsterStats distinctRangedTarget() {
        return monster().defenceLevel(100).lightRangedDefence(232)
            .standardRangedDefence(158).heavyRangedDefence(43).build();
    }

    private static void assertOutputs(PlayerState state, MonsterStats target,
                                      int expectedAttackRoll, int expectedMaxHit) {
        DpsCalculator calculator = new DpsCalculator(state, target);
        assertEquals(expectedAttackRoll, calculator.getMaxAttackRoll());
        assertEquals(expectedMaxHit, calculator.getMaxHit());
    }
}
