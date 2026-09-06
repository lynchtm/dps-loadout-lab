package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.Prayer;
import com.dpscalc.state.PlayerState;
import org.junit.Before;
import org.junit.Test;

import static com.dpscalc.TestUtils.monster;
import static com.dpscalc.TestUtils.player;
import static org.junit.Assert.assertEquals;

/**
 * Tests for low-level prayer special case where Burst of Strength and Sharp Eye
 * give +1 flat bonus instead of 5% multiplier when effective level ≤ 20.
 *
 * This matches the web calculator behavior at:
 * - PlayerVsNPCCalc.ts line 327-331 (Burst of Strength)
 * - PlayerVsNPCCalc.ts line 647-651 (Sharp Eye)
 */
public class LowLevelPrayerTest {

    private MonsterStats testMonster;

    @Before
    public void setUp() {
        testMonster = monster()
            .name("Test Monster")
            .defenceLevel(135)
            .magicLevel(1)
            .hitpoints(150)
            .stabDefence(20)
            .slashDefence(20)
            .crushDefence(20)
            .magicDefence(0)
            .standardRangedDefence(20)
            .size(1)
            .build();
    }

    /**
     * Test 1: Level 12 strength + Burst of Strength → max hit calculated with +1 flat bonus
     *
     * Calculation with +1 flat bonus (expected):
     * - Base strength level: 12
     * - After Burst of Strength (low level special case): 12 + 1 = 13
     * - After stance bonus (Aggressive +3, base 8): 13 + 8 + 3 = 24
     * - Max hit = floor((24 * (200 + 64) + 320) / 640) = floor((24 * 264 + 320) / 640) = floor(6656 / 640) = 10
     *
     * Calculation with 5% multiplier (current broken implementation):
     * - Base strength level: 12
     * - After Burst of Strength (5%): floor(12 * 1.05) = 12
     * - After stance bonus (Aggressive +3, base 8): 12 + 8 + 3 = 23
     * - Max hit = floor((23 * 264 + 320) / 640) = floor(6392 / 640) = 9
     */
    @Test
    public void testLevel12StrengthBurstOfStrength() {
        PlayerState player = player()
            .strengthLevel(12)
            .prayer(Prayer.BURST_OF_STRENGTH)
            .meleeStrength(200)
            .weapon("Abyssal whip")
            .combatStyle(CombatStyle.MELEE_AGGRESSIVE_SLASH)
            .build();

        DpsCalculator calc = new DpsCalculator(player, testMonster);
        int maxHit = calc.getMaxHit();

        // Expected with +1 flat: floor((24 * 264 + 320) / 640) = 10
        // Current broken (5%): floor((23 * 264 + 320) / 640) = 9
        assertEquals("Level 12 + Burst of Strength should give +1 flat bonus (not 5%)", 10, maxHit);
    }

    /**
     * Test 2: Level 14 strength + Burst of Strength → max hit calculated with +1 flat bonus
     *
     * Calculation with +1 flat bonus (expected):
     * - Base strength level: 14
     * - After Burst of Strength (low level special case): 14 + 1 = 15
     * - After stance bonus (Aggressive +3, base 8): 15 + 8 + 3 = 26
     * - Max hit = floor((26 * (200 + 64) + 320) / 640) = floor((26 * 264 + 320) / 640) = floor(7184 / 640) = 11
     *
     * Calculation with 5% multiplier (current broken implementation):
     * - Base strength level: 14
     * - After Burst of Strength (5%): floor(14 * 1.05) = 14
     * - After stance bonus (Aggressive +3, base 8): 14 + 8 + 3 = 25
     * - Max hit = floor((25 * 264 + 320) / 640) = floor(6920 / 640) = 10
     */
    @Test
    public void testLevel14StrengthBurstOfStrength() {
        PlayerState player = player()
            .strengthLevel(14)
            .prayer(Prayer.BURST_OF_STRENGTH)
            .meleeStrength(200)
            .weapon("Abyssal whip")
            .combatStyle(CombatStyle.MELEE_AGGRESSIVE_SLASH)
            .build();

        DpsCalculator calc = new DpsCalculator(player, testMonster);
        int maxHit = calc.getMaxHit();

        // Expected with +1 flat: floor((26 * 264 + 320) / 640) = 11
        // Current broken (5%): floor((25 * 264 + 320) / 640) = 10
        assertEquals("Level 14 + Burst of Strength should give +1 flat bonus (not 5%)", 11, maxHit);
    }

    /**
     * Test 3: Level 20 strength + Burst of Strength → max hit calculated with +1 flat bonus (boundary)
     *
     * Calculation with +1 flat bonus (expected, level ≤ 20):
     * - Base strength level: 20
     * - After Burst of Strength (low level special case): 20 + 1 = 21
     * - After stance bonus (Aggressive +3, base 8): 21 + 8 + 3 = 32
     * - Max hit = floor((32 * (200 + 64) + 320) / 640) = floor((32 * 264 + 320) / 640) = floor(8768 / 640) = 13
     *
     * Calculation with 5% multiplier (current broken implementation):
     * - Base strength level: 20
     * - After Burst of Strength (5%): floor(20 * 1.05) = 21
     * - After stance bonus (Aggressive +3, base 8): 21 + 8 + 3 = 32
     * - Max hit = floor((32 * 264 + 320) / 640) = floor(8768 / 640) = 13
     *
     * Note: At level 20 (boundary), both calculations give same result
     */
    @Test
    public void testLevel20StrengthBurstOfStrength() {
        PlayerState player = player()
            .strengthLevel(20)
            .prayer(Prayer.BURST_OF_STRENGTH)
            .meleeStrength(200)
            .weapon("Abyssal whip")
            .combatStyle(CombatStyle.MELEE_AGGRESSIVE_SLASH)
            .build();

        DpsCalculator calc = new DpsCalculator(player, testMonster);
        int maxHit = calc.getMaxHit();

        // Expected: floor((32 * 264 + 320) / 640) = 13
        assertEquals("Level 20 + Burst of Strength should give +1 flat bonus (boundary test)", 13, maxHit);
    }

    /**
     * Test 4: Level 12 ranged + Sharp Eye → max hit calculated with +1 flat bonus
     *
     * Calculation with +1 flat bonus (expected):
     * - Base ranged level: 12
     * - After Sharp Eye (low level special case): 12 + 1 = 13
     * - After stance bonus (Rapid, base 8): 13 + 8 = 21
     * - Max hit = floor((21 * (200 + 64) + 320) / 640) = floor((21 * 264 + 320) / 640) = floor(5864 / 640) = 9
     *
     * Calculation with 5% multiplier (current broken implementation):
     * - Base ranged level: 12
     * - After Sharp Eye (5%): floor(12 * 1.05) = 12
     * - After stance bonus (Rapid, base 8): 12 + 8 = 20
     * - Max hit = floor((20 * 264 + 320) / 640) = floor(5600 / 640) = 8
     */
    @Test
    public void testLevel12RangedSharpEye() {
        PlayerState player = player()
            .rangedLevel(12)
            .prayer(Prayer.SHARP_EYE)
            .rangedStrength(200)
            .weapon("Shortbow")
            .ammo("Bronze arrow")
            .combatStyle(CombatStyle.RANGED_RAPID)
            .build();

        DpsCalculator calc = new DpsCalculator(player, testMonster);
        int maxHit = calc.getMaxHit();

        // Expected with +1 flat: floor((21 * 264 + 320) / 640) = 9
        // Current broken (5%): floor((20 * 264 + 320) / 640) = 8
        assertEquals("Level 12 + Sharp Eye should give +1 flat bonus (not 5%)", 9, maxHit);
    }

    /**
     * Test 5: Level 7 ranged + Sharp Eye → max hit calculated with +1 flat bonus
     *
     * Calculation with +1 flat bonus (expected):
     * - Base ranged level: 7
     * - After Sharp Eye (low level special case): 7 + 1 = 8
     * - After stance bonus (Rapid, base 8): 8 + 8 = 16
     * - Max hit = floor((16 * (200 + 64) + 320) / 640) = floor((16 * 264 + 320) / 640) = floor(4544 / 640) = 7
     *
     * Calculation with 5% multiplier (current broken implementation):
     * - Base ranged level: 7
     * - After Sharp Eye (5%): floor(7 * 1.05) = 7
     * - After stance bonus (Rapid, base 8): 7 + 8 = 15
     * - Max hit = floor((15 * 264 + 320) / 640) = floor(4280 / 640) = 6
     */
    @Test
    public void testLevel7RangedSharpEye() {
        PlayerState player = player()
            .rangedLevel(7)
            .prayer(Prayer.SHARP_EYE)
            .rangedStrength(200)
            .weapon("Shortbow")
            .ammo("Bronze arrow")
            .combatStyle(CombatStyle.RANGED_RAPID)
            .build();

        DpsCalculator calc = new DpsCalculator(player, testMonster);
        int maxHit = calc.getMaxHit();

        // Expected with +1 flat: floor((16 * 264 + 320) / 640) = 7
        // Current broken (5%): floor((15 * 264 + 320) / 640) = 6
        assertEquals("Level 7 + Sharp Eye should give +1 flat bonus (not 5%)", 7, maxHit);
    }

    /**
     * Test 6: Level 20 ranged + Sharp Eye → max hit calculated with +1 flat bonus (boundary)
     *
     * Calculation with +1 flat bonus (expected, level ≤ 20):
     * - Base ranged level: 20
     * - After Sharp Eye (low level special case): 20 + 1 = 21
     * - After stance bonus (Rapid, base 8): 21 + 8 = 29
     * - Max hit = floor((29 * (200 + 64) + 320) / 640) = floor((29 * 264 + 320) / 640) = floor(7976 / 640) = 12
     *
     * Calculation with 5% multiplier (current broken implementation):
     * - Base ranged level: 20
     * - After Sharp Eye (5%): floor(20 * 1.05) = 21
     * - After stance bonus (Rapid, base 8): 21 + 8 = 29
     * - Max hit = floor((29 * 264 + 320) / 640) = floor(7976 / 640) = 12
     *
     * Note: At level 20 (boundary), both calculations give same result
     */
    @Test
    public void testLevel20RangedSharpEye() {
        PlayerState player = player()
            .rangedLevel(20)
            .prayer(Prayer.SHARP_EYE)
            .rangedStrength(200)
            .weapon("Shortbow")
            .ammo("Bronze arrow")
            .combatStyle(CombatStyle.RANGED_RAPID)
            .build();

        DpsCalculator calc = new DpsCalculator(player, testMonster);
        int maxHit = calc.getMaxHit();

        // Expected: floor((29 * 264 + 320) / 640) = 12
        assertEquals("Level 20 + Sharp Eye should give +1 flat bonus (boundary test)", 12, maxHit);
    }
}
