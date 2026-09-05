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

public class AmmoValidityCalculationTest {
    private static final int RUNE_CROSSBOW = 9185;
    private static final int RUNE_BOLTS = 9144;
    private static final int RUNE_ARROW = 892;
    private static final int BOW_OF_FAERDHINEN = 25865;
    private static final CombatStyle MANUAL_CAST = new CombatStyle(
        "Manual Cast", AttackType.MAGIC, "Manual Cast", 0, 0, 0, 0, 0);

    @Test
    public void canonicalAmmoApplicabilityControlsAttackRollAndMaxHitNotDisplayNames() {
        MonsterStats target = monster().defenceLevel(100).build();

        assertOutputs(preparedRanged(RUNE_CROSSBOW, RUNE_BOLTS, "Rune arrow"), target, 17548, 27);
        PlayerState wrongAmmo = preparedRanged(RUNE_CROSSBOW, RUNE_ARROW, "Rune bolts");
        assertOutputs(wrongAmmo, target, 0, 0);
        DpsCalculator wrongAmmoCalculator = new DpsCalculator(wrongAmmo, target);
        System.out.printf("invalidAmmo attackRoll=%d maxHit=%d expectedAttackRoll=0 expectedMaxHit=0%n",
            wrongAmmoCalculator.getMaxAttackRoll(), wrongAmmoCalculator.getMaxHit());
        assertOutputs(preparedRanged(RUNE_CROSSBOW, -1, null), target, 0, 0);
        assertOutputs(preparedRanged(BOW_OF_FAERDHINEN, -1, null), target, 17548, 27);
    }

    @Test
    public void manualCastDoesNotZeroInvalidRequiredAmmo() {
        MonsterStats target = monster().defenceLevel(100).build();
        PlayerState state = basePlayer(MANUAL_CAST);
        state.setMagicLevel(99);
        state.setSpellName("Fire Wave");
        state.setSpellMaxHit(20);
        prepare(state, RUNE_CROSSBOW, RUNE_ARROW, "Rune bolts");
        state.getEquipmentStats().setMagicAttack(100);

        assertOutputs(state, target, 17712, 20);
    }

    private static PlayerState preparedRanged(int weaponId, int ammoId, String ammoDisplayName) {
        PlayerState state = basePlayer(CombatStyle.RANGED_RAPID);
        prepare(state, weaponId, ammoId, ammoDisplayName);
        return state;
    }

    private static PlayerState basePlayer(CombatStyle style) {
        return player().rangedAttack(100).rangedStrength(100).combatStyle(style).build();
    }

    private static void prepare(PlayerState state, int weaponId, int ammoId, String ammoDisplayName) {
        int[] itemIds = new int[14];
        Arrays.fill(itemIds, -1);
        itemIds[EquipmentSlot.WEAPON.getIndex()] = weaponId;
        itemIds[EquipmentSlot.AMMO.getIndex()] = ammoId;
        String[] names = new String[14];
        names[EquipmentSlot.AMMO.getIndex()] = ammoDisplayName;
        new EquipmentPreparationFacade().prepare(state, itemIds, names,
            EquipmentPreparationFacade.context(state, 1));
        if (state.getCombatStyle().getAttackType().isRanged()) {
            state.getEquipmentStats().setRangedAttack(100);
            state.getEquipmentStats().setRangedStrength(100);
        }
    }

    private static void assertOutputs(PlayerState state, MonsterStats target,
                                      int expectedAttackRoll, int expectedMaxHit) {
        DpsCalculator calculator = new DpsCalculator(state, target);
        assertEquals(expectedAttackRoll, calculator.getMaxAttackRoll());
        assertEquals(expectedMaxHit, calculator.getMaxHit());
    }
}
