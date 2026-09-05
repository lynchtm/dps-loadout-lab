package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.PlayerState;
import org.junit.Test;

import static com.dpscalc.TestUtils.monster;
import static com.dpscalc.TestUtils.player;
import static org.junit.Assert.assertEquals;

public class RangedDefenceTypeTest {
    private static final int DRAGON_DART = 11230;
    private static final int BOW_OF_FAERDHINEN = 25865;
    private static final int RUNE_CROSSBOW = 9185;
    private static final int RED_CHINCHOMPA = 10034;
    private static final int BLACK_SALAMANDER = 10148;

    @Test
    public void preparedCanonicalCategoriesSelectExactReferenceDefenceBuckets() {
        MonsterStats target = monster().defenceLevel(100).lightRangedDefence(232)
            .standardRangedDefence(158).heavyRangedDefence(43).build();

        assertDefenceRoll(target, DRAGON_DART, "Thrown", 32264);
        assertDefenceRoll(target, BOW_OF_FAERDHINEN, "Bow", 24198);
        assertDefenceRoll(target, RUNE_CROSSBOW, "Crossbow", 11663);
        assertDefenceRoll(target, RED_CHINCHOMPA, "Chinchompas", 11663);
        assertDefenceRoll(target, BLACK_SALAMANDER, "Salamander", 22672);
    }

    private static void assertDefenceRoll(MonsterStats target, int weaponId,
                                          String expectedCategory, int expectedRoll) {
        PlayerState state = player().combatStyle(CombatStyle.RANGED_RAPID).build();
        int[] itemIds = emptyItemIds();
        itemIds[EquipmentSlot.WEAPON.getIndex()] = weaponId;
        new EquipmentPreparationFacade().prepare(state, itemIds, null,
            EquipmentPreparationFacade.context(state, target.getId()));

        assertEquals(expectedCategory, state.getWeaponCategory());
        int actualRoll = new DpsCalculator(state, target).getNpcDefenceRoll();
        System.out.printf("category=%s npcDefenceRoll=%d expected=%d%n",
            expectedCategory, actualRoll, expectedRoll);
        assertEquals(expectedRoll, actualRoll);
    }

    private static int[] emptyItemIds() {
        int[] itemIds = new int[14];
        java.util.Arrays.fill(itemIds, -1);
        return itemIds;
    }
}
