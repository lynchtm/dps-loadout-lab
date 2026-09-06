package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.PlayerState;
import org.junit.Test;

import static com.dpscalc.TestUtils.monster;
import static com.dpscalc.TestUtils.player;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ComplexDistributionBaselineTest {
    @Test
    public void guardedComplexAttacksPreserveCurrentScalarResults() {
        MonsterStats largeMonster = monster().name("Large boss").size(3)
            .defenceLevel(100).slashDefence(20).build();
        PlayerState scythe = player().strengthLevel(99).attackLevel(99)
            .weapon("Scythe of vitur").slashAttack(75).meleeStrength(75)
            .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH).build();

        DpsResult result = new DpsCalculator(scythe, largeMonster).calculate();

        assertNotNull(result.getAttackDistribution());
        assertEquals(39, result.getMaxHit());
        assertEquals(5.777045684098853, result.getDps(), 0.0);
    }

    @Test
    public void guardedKerisProcPreservesCurrentScalarResults() {
        MonsterStats kalphite = monster().name("Kalphite").attribute(MonsterAttribute.KALPHITE).build();
        PlayerState keris = player().strengthLevel(99).attackLevel(99)
            .weapon("Keris partisan of breaching").stabAttack(78).meleeStrength(75)
            .combatStyle(CombatStyle.MELEE_ACCURATE_STAB).build();

        DpsResult result = new DpsCalculator(keris, kalphite).calculate();

        assertNotNull(result.getAttackDistribution());
        assertTrue(result.getMaxHit() > 0);
        assertTrue(result.getDps() > 0);
    }

    @Test
    public void complexFamiliesExposeJointDistributions() {
        MonsterStats largeMonster = monster().name("Large boss").size(3)
            .defenceLevel(100).slashDefence(20).build();
        PlayerState scythe = player().strengthLevel(99).attackLevel(99)
            .weapon("Scythe of vitur").slashAttack(75).meleeStrength(75)
            .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH).build();

        DpsResult result = new DpsCalculator(scythe, largeMonster).calculate();

        assertNotNull(result.getAttackDistribution());
        assertEquals(3, result.getAttackDistribution().getDistributions().size());
        assertEquals(result.getMaxHit(), result.getAttackDistribution().getMax());
        assertEquals(result.getDps(), result.getAttackDistribution().getExpectedDamage()
            / (result.getAttackSpeed() * 0.6), 0.0);
    }

    @Test
    public void enchantedBoltExposesNormalizedDistribution() {
        MonsterStats target = monster().name("Abyssal demon").defenceLevel(100).standardRangedDefence(20).build();
        PlayerState crossbow = player().rangedLevel(99).rangedAttack(100).rangedStrength(100)
            .weapon("Dragon crossbow").ammo("Opal dragon bolts (e)")
            .combatStyle(CombatStyle.RANGED_ACCURATE).build();
        crossbow.getEquippedItemCategories()[EquipmentSlot.WEAPON.getIndex()] = "Crossbow";

        DpsResult result = new DpsCalculator(crossbow, target).calculate();

        assertNotNull(result.getAttackDistribution());
        assertEquals(1.0, result.getAttackDistribution().getJointDistribution().getTotalProbability(), 1e-12);
        assertEquals(result.getDirectMaxHit(), result.getAttackDistribution().getMax());
    }
}
