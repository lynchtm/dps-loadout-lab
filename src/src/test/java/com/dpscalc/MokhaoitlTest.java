package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterConstants;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.*;
import org.junit.Test;

import static com.dpscalc.TestUtils.*;
import static org.junit.Assert.*;

public class MokhaoitlTest {

    /**
     * Tests Doom of Mokhaoitl Burrowing phase - 100% accuracy special case.
     * Web DPS calc shows: Max hit 11, Attack roll 6848, NPC def roll 12276, DPS 2.326
     *
     * The isGuaranteedAccuracy() method should return true when:
     * - Monster ID is in DOOM_OF_MOKHAIOTL_IDS (14707)
     * - Phase is NOT "Normal" (i.e., "Shielded" or "Burrowing")
     */
    @Test
    public void kickingVsMokhaoitlBurrowing() {
        // Create monster with proper ID and phase to trigger 100% accuracy
        MonsterStats monster = monster()
            .id(MonsterConstants.DOOM_OF_MOKHAIOTL_IDS[0])  // 14707
            .name("Doom of Mokhaoitl")
            .defenceLevel(140)  // Actual defence level - should be ignored due to 100% accuracy
            .crushDefence(50)   // Non-zero defence - should be ignored
            .hitpoints(1000)
            .build();

        // Set phase to "Burrowing" to trigger 100% accuracy
        MonsterInputs inputs = monster.getInputs();
        inputs.setPhase("Burrowing");

        // Kicking: unarmed, crush attack, aggressive stance (+3 str)
        PlayerState player = player()
            .attackLevel(99)
            .strengthLevel(99)
            .combatStyle(new CombatStyle("Kick", AttackType.CRUSH, "Aggressive", 0, 3, 0, 0, 0))
            .weaponSpeed(4)  // Unarmed is 4 ticks
            .build();

        DpsCalculator calc = new DpsCalculator(player, monster);
        DpsResult result = calc.calculate();

        System.out.println("=== Kicking vs Mokhaoitl (Burrowing) ===");
        System.out.println("Monster ID: " + monster.getId());
        System.out.println("Phase: " + monster.getInputs().getPhase());
        System.out.println("Max Hit: " + result.getMaxHit() + " (web calc: 11)");
        System.out.println("Attack Roll: " + result.getAttackRoll() + " (web calc: 6848)");
        System.out.println("Defence Roll: " + result.getDefenceRoll() + " (web calc: 12276)");
        System.out.println("Accuracy: " + String.format("%.2f%%", result.getAccuracy() * 100) + " (web calc: 100%)");
        System.out.println("DPS: " + String.format("%.3f", result.getDps()) + " (web calc: 2.326)");
        System.out.println("Attack Speed: " + result.getAttackSpeed() + " ticks");

        // CRITICAL ASSERTION: Burrowing phase should have 100% accuracy
        assertEquals("Mokhaoitl Burrowing phase should have 100% accuracy", 1.0, result.getAccuracy(), 0.001);
        assertEquals("Max hit should be 11", 11, result.getMaxHit());
    }

    /**
     * Tests that Normal phase does NOT have 100% accuracy.
     */
    @Test
    public void kickingVsMokhaoitlNormalPhase() {
        MonsterStats monster = monster()
            .id(MonsterConstants.DOOM_OF_MOKHAIOTL_IDS[0])
            .name("Doom of Mokhaoitl")
            .defenceLevel(140)
            .crushDefence(50)
            .hitpoints(1000)
            .build();

        // Set phase to "Normal" - should NOT trigger 100% accuracy
        monster.getInputs().setPhase("Normal");

        PlayerState player = player()
            .attackLevel(99)
            .strengthLevel(99)
            .combatStyle(new CombatStyle("Kick", AttackType.CRUSH, "Aggressive", 0, 3, 0, 0, 0))
            .weaponSpeed(4)
            .build();

        DpsCalculator calc = new DpsCalculator(player, monster);
        DpsResult result = calc.calculate();

        System.out.println("=== Kicking vs Mokhaoitl (Normal) ===");
        System.out.println("Accuracy: " + String.format("%.2f%%", result.getAccuracy() * 100));

        // Normal phase should NOT have 100% accuracy
        assertTrue("Mokhaoitl Normal phase should NOT have 100% accuracy", result.getAccuracy() < 1.0);
    }

    /**
     * Tests Shielded phase - should be immune to non-demonbane weapons.
     */
    @Test
    public void kickingVsMokhaoitlShieldedPhase() {
        MonsterStats monster = monster()
            .id(MonsterConstants.DOOM_OF_MOKHAIOTL_IDS[0])
            .name("Doom of Mokhaoitl")
            .defenceLevel(140)
            .crushDefence(50)
            .hitpoints(1000)
            .build();

        // Set phase to "Shielded" - should be immune to non-demonbane
        monster.getInputs().setPhase("Shielded");

        // Kicking (not demonbane) should be immune
        PlayerState player = player()
            .attackLevel(99)
            .strengthLevel(99)
            .combatStyle(new CombatStyle("Kick", AttackType.CRUSH, "Aggressive", 0, 3, 0, 0, 0))
            .weaponSpeed(4)
            .build();

        DpsCalculator calc = new DpsCalculator(player, monster);
        DpsResult result = calc.calculate();

        System.out.println("=== Kicking vs Mokhaoitl (Shielded) ===");
        System.out.println("Max Hit: " + result.getMaxHit());
        System.out.println("Accuracy: " + String.format("%.2f%%", result.getAccuracy() * 100));

        // Shielded phase should be immune to non-demonbane (0 DPS)
        assertEquals("Mokhaoitl Shielded phase should be immune to non-demonbane attacks", 0, result.getMaxHit());
    }
}
