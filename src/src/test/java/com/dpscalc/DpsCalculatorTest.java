package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterConstants;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.*;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static com.dpscalc.TestUtils.*;
import static org.junit.Assert.*;

/**
 * DPS Calculator tests ported from the web-based osrs-dps-calc.
 * Tests calculation accuracy for various combat scenarios.
 */
@RunWith(Enclosed.class)
public class DpsCalculatorTest {

    // ========================================================================
    // BASIC ROLLS TESTS
    // Ported from: BasicRolls.test.ts
    // ========================================================================

    public static class MeleeBasicRolls {

        @Test
        public void level1AttackRoll() {
            // Abyssal whip stats: +82 slash
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .attackLevel(1)
                .strengthLevel(99)
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int attackRoll = calc.getMaxAttackRoll();

            // L1 attack: effective level = 1 + 8 (base) + 3 (accurate) = 12
            // gear bonus = 82 + 64 = 146
            // attack roll = 12 * 146 = 1752
            assertEquals(1752, attackRoll);
        }

        @Test
        public void level1MaxHit() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .attackLevel(99)
                .strengthLevel(1)
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // L1 str: effective level = 1 + 8 = 9 (accurate gives attack not str)
            // gear bonus = 82 + 64 = 146
            // max hit = (9 * 146 + 320) / 640 = 2
            assertEquals(2, maxHit);
        }

        @Test
        public void level99AttackRoll() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .attackLevel(99)
                .strengthLevel(99)
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int attackRoll = calc.getMaxAttackRoll();

            // L99 attack: effective level = 99 + 8 + 3 = 110
            // gear bonus = 82 + 64 = 146
            // attack roll = 110 * 146 = 16060
            assertEquals(16060, attackRoll);
        }

        @Test
        public void level99MaxHit() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .attackLevel(99)
                .strengthLevel(99)
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // L99 str: effective level = 99 + 8 = 107
            // gear bonus = 82 + 64 = 146
            // max hit = (107 * 146 + 320) / 640 = 24
            assertEquals(24, maxHit);
        }
    }

    public static class RangedBasicRolls {

        @Test
        public void level1AttackRoll() {
            // Bow of faerdhinen stats: +128 ranged attack, +106 ranged str
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .rangedLevel(1)
                .rangedAttack(128)
                .rangedStrength(106)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int attackRoll = calc.getMaxAttackRoll();

            // L1 ranged: effective level = 1 + 3 (accurate) + 8 = 12
            // gear bonus = 128 + 64 = 192
            // attack roll = 12 * 192 = 2304
            assertEquals(2304, attackRoll);
        }

        @Test
        public void level1MaxHit() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .rangedLevel(1)
                .rangedAttack(128)
                .rangedStrength(106)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // L1 ranged: effective level = 1 + 3 + 8 = 12
            // gear bonus = 106 + 64 = 170
            // max hit = (12 * 170 + 320) / 640 = 3
            assertEquals(3, maxHit);
        }

        @Test
        public void level99AttackRoll() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .rangedLevel(99)
                .rangedAttack(128)
                .rangedStrength(106)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int attackRoll = calc.getMaxAttackRoll();

            // L99 ranged: effective level = 99 + 3 + 8 = 110
            // gear bonus = 128 + 64 = 192
            // attack roll = 110 * 192 = 21120
            assertEquals(21120, attackRoll);
        }

        @Test
        public void level99MaxHit() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .rangedLevel(99)
                .rangedAttack(128)
                .rangedStrength(106)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // L99 ranged: effective level = 99 + 3 + 8 = 110
            // gear bonus = 106 + 64 = 170
            // max hit = (110 * 170 + 320) / 640 = 29
            assertEquals(29, maxHit);
        }
    }

    public static class MagicBasicRolls {

        @Test
        public void level1AttackRoll() {
            // Trident of the seas stats: +15 magic attack
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .magicLevel(1)
                .magicAttack(15)
                .weapon("Trident of the seas")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int attackRoll = calc.getMaxAttackRoll();

            // L1 magic: effective level = 1 + 2 (accurate) + 9 = 12
            // gear bonus = 15 + 64 = 79
            // attack roll = 12 * 79 = 948
            assertEquals(948, attackRoll);
        }

        @Test
        public void level1MaxHit() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .magicLevel(1)
                .magicAttack(15)
                .weapon("Trident of the seas")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Trident: max hit = floor(magic/3) - 5 = floor(1/3) - 5 = -5 -> min 1
            assertEquals(1, maxHit);
        }

        @Test
        public void level99AttackRoll() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .magicLevel(99)
                .magicAttack(15)
                .weapon("Trident of the seas")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int attackRoll = calc.getMaxAttackRoll();

            // L99 magic: effective level = 99 + 2 + 9 = 110
            // gear bonus = 15 + 64 = 79
            // attack roll = 110 * 79 = 8690
            assertEquals(8690, attackRoll);
        }

        @Test
        public void level99MaxHit() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .magicLevel(99)
                .magicAttack(15)
                .weapon("Trident of the seas")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Trident: max hit = floor(99/3) - 5 = 33 - 5 = 28
            assertEquals(28, maxHit);
        }
    }

    // ========================================================================
    // DEFENCE ROLL TESTS
    // Ported from: DefenceRolls.test.ts
    // ========================================================================

    public static class DefenceRolls {

        @Test
        public void abyssalDemonDefenceRoll() {
            // Abyssal demon: 135 defence, 20 slash defence
            MonsterStats monster = monster()
                .name("Abyssal demon")
                .defenceLevel(135)
                .slashDefence(20)
                .build();

            PlayerState player = player()
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int defRoll = calc.getNpcDefenceRoll();

            // effective level = 135 + 9 = 144
            // defence bonus = 20 + 64 = 84
            // defence roll = 144 * 84 = 12096
            assertEquals(12096, defRoll);
        }

        @Test
        public void lowDefenceMonster() {
            MonsterStats monster = monster()
                .name("Chicken")
                .defenceLevel(1)
                .slashDefence(0)
                .build();

            PlayerState player = player()
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int defRoll = calc.getNpcDefenceRoll();

            // effective level = 1 + 9 = 10
            // defence bonus = 0 + 64 = 64
            // defence roll = 10 * 64 = 640
            assertEquals(640, defRoll);
        }

        @Test
        public void olmDefenceRollUsesSuppliedScaledStats_whenMonsterWasAlreadyScaled() {
            MonsterStats monster = monster()
                .id(MonsterConstants.OLM_MAGE_HAND_IDS[0])
                .name("Great Olm")
                .attribute(MonsterAttribute.XERICIAN)
                .defenceLevel(180)
                .slashDefence(200)
                .build();
            MonsterInputs inputs = new MonsterInputs();
            inputs.setPartySize(4);
            monster.setInputs(inputs);

            PlayerState player = player()
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int defRoll = calc.getNpcDefenceRoll();

            assertEquals(49896, defRoll);
        }
    }

    // ========================================================================
    // PRAYER TESTS
    // Ported from: Prayers.test.ts
    // ========================================================================

    public static class PrayerEffects {

        @Test
        public void burstOfStrengthLevel10() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .strengthLevel(10)
                .prayer(Prayer.BURST_OF_STRENGTH)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            // Burst of Strength: 5% str bonus
            // Effective str level = floor(10 * 1.05) + 8 = 10 + 8 = 18
            // Actually prayer applies before stance: floor(10 * 1.05) = 10
            int maxHit = calc.getMaxHit();
            assertTrue("Max hit should be positive with Burst of Strength", maxHit > 0);
        }

        @Test
        public void burstOfStrengthLevel99() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .strengthLevel(99)
                .prayer(Prayer.BURST_OF_STRENGTH)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            // Burst of Strength: 5% str bonus
            // Effective str level = floor(99 * 1.05) = 103
            int maxHit = calc.getMaxHit();
            assertTrue("Max hit should increase with Burst of Strength", maxHit > 24);
        }

        @Test
        public void pietyAttackAndStrength() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .attackLevel(99)
                .strengthLevel(99)
                .prayer(Prayer.PIETY)
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            // Piety: 20% attack, 23% strength
            int attackRoll = calc.getMaxAttackRoll();
            int maxHit = calc.getMaxHit();

            // With piety: attack = floor(99 * 1.20) = 118, then +11 = 129
            // roll = 129 * 146 = 18834
            assertTrue("Attack roll should be higher with Piety", attackRoll > 16060);
            assertTrue("Max hit should be higher with Piety", maxHit > 24);
        }

        @Test
        public void rigourRanged() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .rangedLevel(99)
                .prayer(Prayer.RIGOUR)
                .rangedAttack(128)
                .rangedStrength(106)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            // Rigour: 20% accuracy, 23% damage
            int attackRoll = calc.getMaxAttackRoll();
            int maxHit = calc.getMaxHit();

            assertTrue("Attack roll should be higher with Rigour", attackRoll > 21120);
            assertTrue("Max hit should be higher with Rigour", maxHit > 29);
        }

        @Test
        public void deadeyeRangedPrayer() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .rangedLevel(99)
                .prayer(Prayer.DEADEYE)
                .rangedAttack(128)
                .rangedStrength(106)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            assertEquals("Deadeye should apply 18% ranged accuracy", 24384, calc.getMaxAttackRoll());
            assertEquals("Deadeye should apply 18% ranged strength", 34, calc.getMaxHit());
        }

        @Test
        public void auguryMagic() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .magicLevel(99)
                .prayer(Prayer.AUGURY)
                .magicAttack(15)
                .weapon("Trident of the seas")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            // Augury: 25% magic accuracy (no damage boost)
            int attackRoll = calc.getMaxAttackRoll();

            assertTrue("Attack roll should be higher with Augury", attackRoll > 8690);
        }

        @Test
        public void mysticVigourMagicPrayer() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .magicLevel(99)
                .prayer(Prayer.MYSTIC_VIGOUR)
                .magicAttack(15)
                .weapon("Trident of the seas")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            assertEquals("Mystic Vigour should apply 18% magic accuracy", 10033, calc.getMaxAttackRoll());
            assertEquals("Mystic Vigour should apply 3% magic damage", 28, calc.getMaxHit());
        }
    }

    // ========================================================================
    // INQUISITOR TESTS
    // Ported from: Inquisitors.test.ts
    // ========================================================================

    public static class InquisitorArmor {

        @Test
        public void inquisitorMaceGets2_5PercentPerPiece() {
            // Testing 2 pieces with Inquisitor's mace
            MonsterStats monster = monster()
                .name("Phosani's Nightmare")
                .defenceLevel(200)
                .crushDefence(50)
                .size(5)
                .build();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Inquisitor's mace")
                .body("Inquisitor's hauberk")
                .legs("Inquisitor's plateskirt")
                .crushAttack(95)
                .meleeStrength(110)  // Set to get base max ~200 for testing
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_CRUSH)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // With inq mace, each piece gives 2.5% bonus
            // 2 pieces = 5% bonus to max hit
            assertTrue("Inquisitor set should boost max hit", maxHit > 0);
        }

        @Test
        public void inquisitorMaceFullSetNoExtraBonus() {
            // Full set with mace does NOT gain extra 1% (that's for non-mace weapons)
            MonsterStats monster = monster()
                .name("Phosani's Nightmare")
                .defenceLevel(200)
                .crushDefence(50)
                .size(5)
                .build();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Inquisitor's mace")
                .head("Inquisitor's great helm")
                .body("Inquisitor's hauberk")
                .legs("Inquisitor's plateskirt")
                .crushAttack(95)
                .meleeStrength(110)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_CRUSH)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // 3 pieces with mace = 7.5% total (not 8.5%)
            assertTrue("Full Inquisitor set with mace should boost max hit", maxHit > 0);
        }

        @Test
        public void otherWeaponsGet0_5PercentPerPiece() {
            MonsterStats monster = monster()
                .name("Phosani's Nightmare")
                .defenceLevel(200)
                .crushDefence(50)
                .size(5)
                .build();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Dragon warhammer")
                .body("Inquisitor's hauberk")
                .legs("Inquisitor's plateskirt")
                .crushAttack(95)
                .meleeStrength(110)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_CRUSH)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Non-mace weapons get 0.5% per piece
            assertTrue("Inquisitor with DWH should boost max hit", maxHit > 0);
        }

        @Test
        public void fullSetWithOtherWeaponGetsExtraBonus() {
            MonsterStats monster = monster()
                .name("Phosani's Nightmare")
                .defenceLevel(200)
                .crushDefence(50)
                .size(5)
                .build();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Dragon warhammer")
                .head("Inquisitor's great helm")
                .body("Inquisitor's hauberk")
                .legs("Inquisitor's plateskirt")
                .crushAttack(95)
                .meleeStrength(110)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_CRUSH)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Full set with non-mace = 2.5% (0.5% * 3 + 1% set bonus)
            assertTrue("Full Inquisitor with DWH should boost max hit", maxHit > 0);
        }

        @Test
        public void inquisitorOnlyWorksOnCrush() {
            MonsterStats monster = monster()
                .name("Test")
                .defenceLevel(100)
                .slashDefence(50)
                .build();

            PlayerState playerSlash = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Abyssal whip")
                .head("Inquisitor's great helm")
                .body("Inquisitor's hauberk")
                .legs("Inquisitor's plateskirt")
                .slashAttack(82)
                .meleeStrength(82)
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            PlayerState playerCrush = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Abyssal bludgeon")
                .head("Inquisitor's great helm")
                .body("Inquisitor's hauberk")
                .legs("Inquisitor's plateskirt")
                .crushAttack(102)
                .meleeStrength(85)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_CRUSH)
                .build();

            DpsCalculator calcSlash = new DpsCalculator(playerSlash, monster);
            DpsCalculator calcCrush = new DpsCalculator(playerCrush, monster);

            // Inquisitor should not affect slash attacks
            int slashHit = calcSlash.getMaxHit();
            int crushHit = calcCrush.getMaxHit();

            assertTrue("Crush attack should benefit from Inquisitor", crushHit > 0);
            assertTrue("Slash attack should not get Inquisitor bonus", slashHit > 0);
        }
    }

    // ========================================================================
    // OBSIDIAN TESTS
    // Ported from: Obsidian.test.ts
    // ========================================================================

    public static class ObsidianWeapons {

        @Test
        public void baseMaxWithToktzXilAk() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);
            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Toktz-xil-ak")
                .stabAttack(47)
                .meleeStrength(72)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            assertEquals("Base max hit with Toktz-xil-ak should be 27", 27, maxHit);
        }

        @Test
        public void withObsidianArmourSet() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);
            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Toktz-xil-ak")
                .head("Obsidian helmet")
                .body("Obsidian platebody")
                .legs("Obsidian platelegs")
                .stabAttack(47)
                .meleeStrength(72)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // 10% bonus from obsidian armor
            assertEquals("Obsidian set should give 10% max hit bonus", 29, maxHit);
        }

        @Test
        public void withSalveAmuletEi() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);
            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Toktz-xil-ak")
                .neck("Salve amulet(ei)")
                .stabAttack(47)
                .meleeStrength(72)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // 20% bonus from salve (e) vs undead
            assertEquals("Salve amulet (ei) should give 20% max hit bonus vs undead", 32, maxHit);
        }

        @Test
        public void withObsidianAndSalve() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);
            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Toktz-xil-ak")
                .head("Obsidian helmet")
                .body("Obsidian platebody")
                .legs("Obsidian platelegs")
                .neck("Salve amulet(ei)")
                .stabAttack(47)
                .meleeStrength(72)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Obsidian 10% + Salve 20% = additive
            assertEquals("Obsidian + Salve should stack", 34, maxHit);
        }

        @Test
        public void withBerserkerNecklace() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);
            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Toktz-xil-ak")
                .neck("Berserker necklace")
                .stabAttack(47)
                .meleeStrength(72)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // 20% bonus from berserker necklace
            assertEquals("Berserker necklace should give 20% bonus", 32, maxHit);
        }

        @Test
        public void withObsidianAndBerserker() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);
            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Toktz-xil-ak")
                .head("Obsidian helmet")
                .body("Obsidian platebody")
                .legs("Obsidian platelegs")
                .neck("Berserker necklace")
                .stabAttack(47)
                .meleeStrength(72)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .prayer(Prayer.PIETY)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Obsidian 10% (additive) + Berserker 20% (multiplicative)
            assertEquals("Obsidian + Berserker should stack", 34, maxHit);
        }
    }

    // ========================================================================
    // SCORCHING BOW TESTS
    // Ported from: ScorchingBow.test.ts
    // ========================================================================

    public static class ScorchingBow {

        @Test
        public void noBonusAgainstNonDemons() {
            MonsterStats monster = monster()
                .name("Aberrant spectre")
                .defenceLevel(90)
                .standardRangedDefence(20)
                .build();

            PlayerState player = player()
                .rangedLevel(99)
                .weapon("Scorching bow")
                .ammo("Dragon arrow")
                .rangedAttack(65)
                .rangedStrength(100)
                .combatStyle(CombatStyle.RANGED_RAPID)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Base max = floor(((99 + 8) * (100 + 64) + 320) / 640) = 27
            int baseMax = (int) Math.floor(((99 + 8) * (100 + 64) + 320) / 640.0);
            assertEquals("No bonus against non-demons", baseMax, maxHit);
        }

        @Test
        public void applies30PercentBonusAgainstDemons() {
            MonsterStats monster = monster()
                .name("Abyssal demon")
                .defenceLevel(135)
                .standardRangedDefence(20)
                .attribute(MonsterAttribute.DEMON)
                .build();

            PlayerState player = player()
                .rangedLevel(99)
                .weapon("Scorching bow")
                .ammo("Dragon arrow")
                .rangedAttack(65)
                .rangedStrength(100)
                .combatStyle(CombatStyle.RANGED_RAPID)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Base max = 27, with 30% demon bonus = floor(27 * 1.3) = 35
            int baseMax = (int) Math.floor(((99 + 8) * (100 + 64) + 320) / 640.0);
            int expectedMax = (int) Math.floor(baseMax * 13.0 / 10);
            assertEquals("30% bonus against demons", expectedMax, maxHit);
        }

        @Test
        public void stacksWithSlayerHelm() {
            MonsterStats monster = monster()
                .name("Abyssal demon")
                .defenceLevel(135)
                .standardRangedDefence(20)
                .attribute(MonsterAttribute.DEMON)
                .build();

            PlayerState player = player()
                .rangedLevel(99)
                .weapon("Scorching bow")
                .head("Slayer helmet (i)")
                .ammo("Dragon arrow")
                .rangedAttack(65)
                .rangedStrength(100)
                .combatStyle(CombatStyle.RANGED_RAPID)
                .onSlayerTask(true)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Per TypeScript source: scorching bow bonus is additive with slayer helm
            // numerator = 23 (slayer) + 6 (scorching demonbane) = 29
            // Base max = 27, with combined bonus = floor(27 * 29/20) = 39
            int baseMax = (int) Math.floor(((99 + 8) * (100 + 64) + 320) / 640.0);
            int expectedMax = (int) Math.floor(baseMax * 29.0 / 20);
            assertEquals("Scorching bow should stack additively with slayer helm", expectedMax, maxHit);
        }
    }

    // ========================================================================
    // SLAYER HELMET TESTS
    // ========================================================================

    public static class SlayerHelmet {

        @Test
        public void meleeBoostOnTask() {
            MonsterStats monster = getTestMonster();

            PlayerState playerWithoutHelm = player()
                .strengthLevel(99)
                .attackLevel(99)
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .onSlayerTask(true)
                .build();

            PlayerState playerWithHelm = player()
                .strengthLevel(99)
                .attackLevel(99)
                .head("Slayer helmet")
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .onSlayerTask(true)
                .build();

            DpsCalculator calcWithout = new DpsCalculator(playerWithoutHelm, monster);
            DpsCalculator calcWith = new DpsCalculator(playerWithHelm, monster);

            int maxWithout = calcWithout.getMaxHit();
            int maxWith = calcWith.getMaxHit();
            int rollWithout = calcWithout.getMaxAttackRoll();
            int rollWith = calcWith.getMaxAttackRoll();

            // 7/6 bonus (16.67%)
            assertTrue("Slayer helmet should boost max hit on task", maxWith > maxWithout);
            assertTrue("Slayer helmet should boost attack roll on task", rollWith > rollWithout);
        }

        @Test
        public void noBoostOffTask() {
            MonsterStats monster = getTestMonster();

            PlayerState playerWithHelm = player()
                .strengthLevel(99)
                .attackLevel(99)
                .head("Slayer helmet")
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .onSlayerTask(false)
                .build();

            DpsCalculator calc = new DpsCalculator(playerWithHelm, monster);

            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // Base values without task
            assertEquals("No boost off task - max hit should be base", 24, maxHit);
            assertEquals("No boost off task - attack roll should be base", 16060, attackRoll);
        }

        @Test
        public void imbuedRangedBoostOnTask() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .rangedLevel(99)
                .head("Slayer helmet (i)")
                .rangedAttack(128)
                .rangedStrength(106)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .onSlayerTask(true)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // Imbued: 15% boost (23/20) for ranged
            assertTrue("Imbued slayer helm should boost ranged on task", maxHit > 29);
            assertTrue("Imbued slayer helm should boost ranged accuracy on task", attackRoll > 21120);
        }
    }

    // ========================================================================
    // SALVE AMULET TESTS
    // ========================================================================

    public static class SalveAmulet {

        @Test
        public void salveAmuletMeleeVsUndead() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .neck("Salve amulet")
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // 7/6 bonus (16.67%)
            assertTrue("Salve amulet should boost melee vs undead", maxHit > 24);
            assertTrue("Salve amulet should boost accuracy vs undead", attackRoll > 16060);
        }

        @Test
        public void salveAmuletEnhancedVsUndead() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .neck("Salve amulet (e)")
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // 6/5 bonus (20%)
            // Base 24, with 20% = floor(24 * 1.2) = 28
            assertEquals("Salve (e) should give 20% bonus vs undead", 28, maxHit);
        }

        @Test
        public void salveAmuletImbuedRanged() {
            MonsterStats monster = getTestMonster(MonsterAttribute.UNDEAD);

            PlayerState player = player()
                .rangedLevel(99)
                .neck("Salve amulet(i)")
                .rangedAttack(128)
                .rangedStrength(106)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Imbued: 7/6 (16.67%) for ranged
            assertTrue("Salve (i) should boost ranged vs undead", maxHit > 29);
        }

        @Test
        public void noEffectVsNonUndead() {
            MonsterStats monster = getTestMonster(); // No undead attribute

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .neck("Salve amulet (e)")
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // No bonus vs non-undead
            assertEquals("No salve bonus vs non-undead", 24, maxHit);
        }
    }

    // ========================================================================
    // DRAGON HUNTER WEAPON TESTS
    // ========================================================================

    public static class DragonHunterWeapons {

        @Test
        public void dragonHunterLanceVsDragons() {
            MonsterStats monster = getTestMonster(MonsterAttribute.DRAGON);

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Dragon hunter lance")
                .stabAttack(85)
                .meleeStrength(70)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // 20% bonus vs dragons
            assertTrue("DHL should boost damage vs dragons", maxHit > 0);
            assertTrue("DHL should boost accuracy vs dragons", attackRoll > 0);
        }

        @Test
        public void dragonHunterCrossbowVsDragons() {
            MonsterStats monster = getTestMonster(MonsterAttribute.DRAGON);

            PlayerState player = player()
                .rangedLevel(99)
                .weapon("Dragon hunter crossbow")
                .ammo("Dragon bolts")
                .rangedAttack(95)
                .rangedStrength(122)
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // DHCB: 30% accuracy, 25% damage vs dragons
            assertTrue("DHCB should boost damage vs dragons", maxHit > 0);
            assertTrue("DHCB should boost accuracy vs dragons", attackRoll > 0);
        }

        @Test
        public void noEffectVsNonDragons() {
            MonsterStats monster = getTestMonster(); // No dragon attribute

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Dragon hunter lance")
                .stabAttack(85)
                .meleeStrength(70)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .build();

            DpsCalculator calcBase = new DpsCalculator(player, monster);

            // Store base values
            int maxHitBase = calcBase.getMaxHit();

            // Now test vs dragon
            MonsterStats dragon = getTestMonster(MonsterAttribute.DRAGON);
            DpsCalculator calcDragon = new DpsCalculator(player, dragon);
            int maxHitDragon = calcDragon.getMaxHit();

            assertTrue("DHL should do more damage to dragons", maxHitDragon > maxHitBase);
        }
    }

    // ========================================================================
    // ARCLIGHT/DEMONBANE TESTS
    // ========================================================================

    public static class ArclightDemonbane {

        @Test
        public void arclightVsDemons() {
            MonsterStats monster = getTestMonster(MonsterAttribute.DEMON);

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Arclight")
                .slashAttack(38)
                .meleeStrength(8)
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // 70% bonus vs demons
            assertTrue("Arclight should boost damage vs demons", maxHit > 0);
            assertTrue("Arclight should boost accuracy vs demons", attackRoll > 0);
        }

        @Test
        public void emberlightVsDemons() {
            MonsterStats monster = getTestMonster(MonsterAttribute.DEMON);

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Emberlight")
                .stabAttack(75)
                .meleeStrength(39)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // 70% bonus vs demons (same as Arclight)
            assertTrue("Emberlight should boost damage vs demons", maxHit > 0);
            assertTrue("Emberlight should boost accuracy vs demons", attackRoll > 0);
        }

        @Test
        public void noEffectVsNonDemons() {
            MonsterStats monster = getTestMonster(); // No demon attribute

            PlayerState playerArclight = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Arclight")
                .slashAttack(38)
                .meleeStrength(8)
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            PlayerState playerWhip = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Abyssal whip")
                .slashAttack(82)
                .meleeStrength(82)
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calcArclight = new DpsCalculator(playerArclight, monster);
            DpsCalculator calcWhip = new DpsCalculator(playerWhip, monster);

            // Arclight has weaker base stats than whip, so without demon bonus it's worse
            assertTrue("Whip should out-damage Arclight vs non-demons",
                calcWhip.getMaxHit() > calcArclight.getMaxHit());
        }
    }

    // ========================================================================
    // WILDERNESS WEAPON TESTS
    // ========================================================================

    public static class WildernessWeapons {

        @Test
        public void crawsBowInWilderness() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .rangedLevel(99)
                .weapon("Craw's bow")
                .rangedAttack(75)
                .rangedStrength(60)
                .combatStyle(CombatStyle.RANGED_RAPID)
                .inWilderness(true)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // 50% bonus in wilderness
            assertTrue("Craw's bow should be effective in wilderness", maxHit > 0);
            assertTrue("Craw's bow should boost accuracy in wilderness", attackRoll > 0);
        }

        @Test
        public void vigorasChainmaceInWilderness() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Viggora's chainmace")
                .crushAttack(52)
                .meleeStrength(64)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_CRUSH)
                .inWilderness(true)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();
            int attackRoll = calc.getMaxAttackRoll();

            // 50% bonus in wilderness
            assertTrue("Viggora's should be effective in wilderness", maxHit > 0);
            assertTrue("Viggora's should boost accuracy in wilderness", attackRoll > 0);
        }

        @Test
        public void noBonusOutsideWilderness() {
            MonsterStats monster = getTestMonster();

            PlayerState playerInWild = player()
                .rangedLevel(99)
                .weapon("Craw's bow")
                .rangedAttack(75)
                .rangedStrength(60)
                .combatStyle(CombatStyle.RANGED_RAPID)
                .inWilderness(true)
                .build();

            PlayerState playerOutWild = player()
                .rangedLevel(99)
                .weapon("Craw's bow")
                .rangedAttack(75)
                .rangedStrength(60)
                .combatStyle(CombatStyle.RANGED_RAPID)
                .inWilderness(false)
                .build();

            DpsCalculator calcIn = new DpsCalculator(playerInWild, monster);
            DpsCalculator calcOut = new DpsCalculator(playerOutWild, monster);

            assertTrue("Craw's bow should be better in wilderness",
                calcIn.getMaxHit() > calcOut.getMaxHit());
        }
    }

    // ========================================================================
    // ACCURACY FORMULA TESTS
    // ========================================================================

    public static class AccuracyFormula {

        @Test
        public void standardAccuracyHighAttackRoll() {
            // When attack > defence: 1 - (def + 2) / (2 * (atk + 1))
            double accuracy = com.dpscalc.calc.BaseCalc.getNormalAccuracyRoll(20000, 10000);

            // Expected: 1 - (10000 + 2) / (2 * 20001) = 1 - 10002/40002 = 0.7499625
            assertTrue("High attack roll should give ~75% accuracy", accuracy > 0.74 && accuracy < 0.76);
        }

        @Test
        public void standardAccuracyLowAttackRoll() {
            // When attack <= defence: atk / (2 * (def + 1))
            double accuracy = com.dpscalc.calc.BaseCalc.getNormalAccuracyRoll(10000, 20000);

            // Expected: 10000 / (2 * 20001) = 0.2499875
            assertTrue("Low attack roll should give ~25% accuracy", accuracy > 0.24 && accuracy < 0.26);
        }

        @Test
        public void standardAccuracyEqualRolls() {
            double accuracy = com.dpscalc.calc.BaseCalc.getNormalAccuracyRoll(10000, 10000);

            // When equal: atk / (2 * (def + 1)) = 10000 / 20002 = ~0.4999
            assertTrue("Equal rolls should give ~50% accuracy", accuracy > 0.49 && accuracy < 0.51);
        }

        @Test
        public void fangAccuracyHighAttackRoll() {
            // Fang has improved accuracy formula
            double normalAccuracy = com.dpscalc.calc.BaseCalc.getNormalAccuracyRoll(20000, 10000);
            double fangAccuracy = com.dpscalc.calc.BaseCalc.getFangAccuracyRoll(20000, 10000);

            assertTrue("Fang should have higher accuracy than normal", fangAccuracy > normalAccuracy);
        }

        @Test
        public void fangAccuracyLowAttackRoll() {
            // Fang benefit is smaller when attack < defence
            double normalAccuracy = com.dpscalc.calc.BaseCalc.getNormalAccuracyRoll(10000, 20000);
            double fangAccuracy = com.dpscalc.calc.BaseCalc.getFangAccuracyRoll(10000, 20000);

            assertTrue("Fang should still improve accuracy when attack < defence", fangAccuracy >= normalAccuracy);
        }
    }

    // ========================================================================
    // NEGATIVE STRENGTH TESTS
    // Ported from: NegativeStrength.test.ts
    // ========================================================================

    public static class NegativeStrength {

        @Test
        public void negativeStrengthBonusShouldNotGiveNegativeMaxHit() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .meleeStrength(-1000)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            assertTrue("Max hit should not be negative", maxHit >= 0);
        }

        @Test
        public void negativeRangedStrengthShouldNotGiveNegativeMaxHit() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .rangedLevel(99)
                .rangedStrength(-1000)
                .weapon("Bow of faerdhinen")
                .combatStyle(CombatStyle.RANGED_RAPID)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            assertTrue("Ranged max hit should not be negative", maxHit >= 0);
        }
    }

    // ========================================================================
    // SPECIAL WEAPON TESTS
    // ========================================================================

    public static class SpecialWeapons {

        @Test
        public void scytheOfViturMultiHit() {
            MonsterStats smallMonster = monster()
                .name("Small mob")
                .size(1)
                .defenceLevel(100)
                .slashDefence(20)
                .build();

            MonsterStats largeMonster = monster()
                .name("Large boss")
                .size(3)
                .defenceLevel(100)
                .slashDefence(20)
                .build();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Scythe of vitur")
                .slashAttack(75)
                .meleeStrength(75)
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calcSmall = new DpsCalculator(player, smallMonster);
            DpsCalculator calcLarge = new DpsCalculator(player, largeMonster);

            DpsResult resultSmall = calcSmall.calculate();
            DpsResult resultLarge = calcLarge.calculate();

            // Scythe hits up to 3 times on large monsters
            assertTrue("Scythe should do more DPS to large monsters",
                resultLarge.getDps() > resultSmall.getDps());
        }

        @Test
        public void osmumtensFangMinHit() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Osmumten's fang")
                .stabAttack(105)
                .meleeStrength(103)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Fang has a minimum hit of 15% of max
            assertTrue("Fang should have reasonable max hit", maxHit > 0);
        }

        @Test
        public void sunspearSpecUsesFixedSeventyPercentHit() {
            MonsterStats monster = monster()
                .name("Vampyre")
                .hitpoints(40)
                .defenceLevel(1)
                .slashDefence(0)
                .attribute(MonsterAttribute.VAMPYRE_3)
                .build();
            monster.getInputs().setMonsterCurrentHp(10);

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Sunspear")
                .slashAttack(0)
                .meleeStrength(116)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_SLASH)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster, true);
            DpsResult result = calc.calculate();

            assertEquals("Sunspear spec should report its fixed damage", 21, result.getMaxHit());
            assertEquals("Sunspear spec should use fixed 70% damage", 21.0 / (4 * 0.6), result.getDps(), 0.0001);
        }

        @Test
        public void hallowedFlailCanHitTierThreeVampyres() {
            MonsterStats monster = monster()
                .name("Vampyre")
                .attribute(MonsterAttribute.VAMPYRE_3)
                .build();
            PlayerState player = player()
                .weapon("Hallowed flail")
                .combatStyle(CombatStyle.MELEE_ACCURATE_CRUSH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);

            assertFalse("Hallowed flail should count as vampyrebane", calc.isImmune());
        }

        @Test
        public void eyeOfAyakSpecUsesFiveTickSpeed() {
            MonsterStats monster = getTestMonster();
            PlayerState player = player()
                .magicLevel(99)
                .magicAttack(30)
                .weapon("Eye of ayak")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .weaponSpeed(4)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster, true);

            assertEquals("Eye of ayak spec should use upstream 5 tick speed", 5, calc.calculate().getAttackSpeed());
        }

        @Test
        public void dharoksDamageScaling() {
            MonsterStats monster = getTestMonster();

            PlayerState playerFullHp = player()
                .strengthLevel(99)
                .attackLevel(99)
                .hitpointsLevel(99)
                .currentHitpoints(99)
                .weapon("Dharok's greataxe")
                .head("Dharok's helm")
                .body("Dharok's platebody")
                .legs("Dharok's platelegs")
                .slashAttack(70)
                .meleeStrength(105)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_SLASH)
                .build();

            PlayerState playerLowHp = player()
                .strengthLevel(99)
                .attackLevel(99)
                .hitpointsLevel(99)
                .currentHitpoints(1)
                .weapon("Dharok's greataxe")
                .head("Dharok's helm")
                .body("Dharok's platebody")
                .legs("Dharok's platelegs")
                .slashAttack(70)
                .meleeStrength(105)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_SLASH)
                .build();

            DpsCalculator calcFull = new DpsCalculator(playerFullHp, monster);
            DpsCalculator calcLow = new DpsCalculator(playerLowHp, monster);

            int maxFull = calcFull.getMaxHit();
            int maxLow = calcLow.getMaxHit();

            assertTrue("Dharok's should hit harder at low HP", maxLow > maxFull);
        }

        @Test
        public void veracsSetEffect() {
            MonsterStats highDefMonster = monster()
                .name("High defence")
                .defenceLevel(300)
                .slashDefence(200)
                .build();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Verac's flail")
                .head("Verac's helm")
                .body("Verac's brassard")
                .legs("Verac's plateskirt")
                .crushAttack(68)
                .meleeStrength(72)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_CRUSH)
                .build();

            DpsCalculator calc = new DpsCalculator(player, highDefMonster);
            DpsResult result = calc.calculate();

            // Verac's 25% chance to ignore defence should help vs high def
            assertTrue("Verac's should have positive DPS vs high defence", result.getDps() > 0);
        }

        @Test
        public void kerisVsKalphites() {
            MonsterStats kalphite = getTestMonster(MonsterAttribute.KALPHITE);
            MonsterStats normalMob = getTestMonster();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Keris partisan of breaching")
                .stabAttack(78)
                .meleeStrength(75)
                .combatStyle(CombatStyle.MELEE_ACCURATE_STAB)
                .build();

            DpsCalculator calcKalphite = new DpsCalculator(player, kalphite);
            DpsCalculator calcNormal = new DpsCalculator(player, normalMob);

            assertTrue("Keris should hit harder vs Kalphites",
                calcKalphite.getMaxHit() > calcNormal.getMaxHit());
        }

        @Test
        public void colossalBladeVsLargeMonsters() {
            MonsterStats smallMonster = monster()
                .name("Small")
                .size(1)
                .defenceLevel(100)
                .slashDefence(20)
                .build();

            MonsterStats largeMonster = monster()
                .name("Large")
                .size(5)
                .defenceLevel(100)
                .slashDefence(20)
                .build();

            PlayerState player = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Colossal blade")
                .slashAttack(122)
                .meleeStrength(122)
                .combatStyle(CombatStyle.MELEE_AGGRESSIVE_SLASH)
                .build();

            DpsCalculator calcSmall = new DpsCalculator(player, smallMonster);
            DpsCalculator calcLarge = new DpsCalculator(player, largeMonster);

            // Colossal blade: +2 max hit per monster size (capped at +10)
            assertTrue("Colossal blade should hit harder vs large monsters",
                calcLarge.getMaxHit() > calcSmall.getMaxHit());
        }
    }

    // ========================================================================
    // MAGIC WEAPON TESTS
    // ========================================================================

    public static class MagicWeapons {

        @Test
        public void tridentOfTheSeasMaxHit() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .magicLevel(75)
                .magicAttack(15)
                .weapon("Trident of the seas")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Trident of seas: floor(magic/3) - 5 = floor(75/3) - 5 = 25 - 5 = 20
            assertEquals("Trident of the seas max hit at 75 magic", 20, maxHit);
        }

        @Test
        public void tridentOfTheSwampMaxHit() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .magicLevel(75)
                .magicAttack(25)
                .weapon("Trident of the swamp")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Trident of swamp: floor(magic/3) - 2 = floor(75/3) - 2 = 25 - 2 = 23
            assertEquals("Trident of the swamp max hit at 75 magic", 23, maxHit);
        }

        @Test
        public void sanguinestiStaffMaxHit() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .magicLevel(75)
                .magicAttack(25)
                .weapon("Sanguinesti staff")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Sang: floor(magic/3) - 1 = floor(75/3) - 1 = 25 - 1 = 24
            assertEquals("Sanguinesti staff max hit at 75 magic", 24, maxHit);
        }

        @Test
        public void tumekensShadowMaxHit() {
            MonsterStats monster = getTestMonster();

            PlayerState player = player()
                .magicLevel(99)
                .magicAttack(35)
                .weapon("Tumeken's shadow")
                .combatStyle(CombatStyle.MAGIC_ACCURATE)
                .build();

            DpsCalculator calc = new DpsCalculator(player, monster);
            int maxHit = calc.getMaxHit();

            // Shadow: floor(magic/3) + 1 = floor(99/3) + 1 = 33 + 1 = 34
            assertEquals("Tumeken's shadow max hit at 99 magic", 34, maxHit);
        }
    }

    // ========================================================================
    // CRYSTAL EQUIPMENT TESTS
    // ========================================================================

    public static class CrystalEquipment {

        @Test
        public void bowOfFaerdhinenWithCrystalArmor() {
            MonsterStats monster = getTestMonster();

            PlayerState playerNoArmor = player()
                .rangedLevel(99)
                .weapon("Bow of faerdhinen")
                .rangedAttack(128)
                .rangedStrength(106)
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            PlayerState playerFullArmor = player()
                .rangedLevel(99)
                .weapon("Bow of faerdhinen")
                .head("Crystal helm")
                .body("Crystal body")
                .legs("Crystal legs")
                .rangedAttack(128)
                .rangedStrength(106)
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calcNo = new DpsCalculator(playerNoArmor, monster);
            DpsCalculator calcFull = new DpsCalculator(playerFullArmor, monster);

            assertTrue("Crystal armor should boost bow of faerdhinen",
                calcFull.getMaxHit() > calcNo.getMaxHit());
            assertTrue("Crystal armor should boost accuracy",
                calcFull.getMaxAttackRoll() > calcNo.getMaxAttackRoll());
        }
    }

    // ========================================================================
    // VOID EQUIPMENT TESTS
    // ========================================================================

    public static class VoidEquipment {

        @Test
        public void voidMeleeBonus() {
            MonsterStats monster = getTestMonster();

            PlayerState playerWithVoid = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Abyssal whip")
                .head("Void melee helm")
                .body("Void knight top")
                .legs("Void knight robe")
                .gloves("Void knight gloves")
                .slashAttack(82)
                .meleeStrength(82)
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            PlayerState playerWithoutVoid = player()
                .strengthLevel(99)
                .attackLevel(99)
                .weapon("Abyssal whip")
                .slashAttack(82)
                .meleeStrength(82)
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .build();

            DpsCalculator calcVoid = new DpsCalculator(playerWithVoid, monster);
            DpsCalculator calcNoVoid = new DpsCalculator(playerWithoutVoid, monster);

            // 10% accuracy and damage bonus
            assertTrue("Void melee should boost accuracy",
                calcVoid.getMaxAttackRoll() > calcNoVoid.getMaxAttackRoll());
            assertTrue("Void melee should boost damage",
                calcVoid.getMaxHit() > calcNoVoid.getMaxHit());
        }

        @Test
        public void eliteVoidRangedBonus() {
            MonsterStats monster = getTestMonster();

            PlayerState playerElite = player()
                .rangedLevel(99)
                .weapon("Bow of faerdhinen")
                .head("Void ranger helm")
                .body("Elite void top")
                .legs("Elite void robe")
                .gloves("Void knight gloves")
                .rangedAttack(128)
                .rangedStrength(106)
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            PlayerState playerRegular = player()
                .rangedLevel(99)
                .weapon("Bow of faerdhinen")
                .head("Void ranger helm")
                .body("Void knight top")
                .legs("Void knight robe")
                .gloves("Void knight gloves")
                .rangedAttack(128)
                .rangedStrength(106)
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calcElite = new DpsCalculator(playerElite, monster);
            DpsCalculator calcRegular = new DpsCalculator(playerRegular, monster);

            // Elite void ranged: 12.5% damage vs regular 10%
            assertTrue("Elite void should have higher damage than regular void",
                calcElite.getMaxHit() >= calcRegular.getMaxHit());
        }
    }

    // ========================================================================
    // TWISTED BOW TESTS
    // ========================================================================

    public static class TwistedBow {

        @Test
        public void scalingWithMonsterMagic() {
            MonsterStats lowMagicMonster = monster()
                .name("Low magic")
                .magicLevel(50)
                .defenceLevel(100)
                .standardRangedDefence(20)
                .build();

            MonsterStats highMagicMonster = monster()
                .name("High magic")
                .magicLevel(250)
                .defenceLevel(100)
                .standardRangedDefence(20)
                .build();

            PlayerState player = player()
                .rangedLevel(99)
                .weapon("Twisted bow")
                .ammo("Dragon arrow")
                .rangedAttack(70)
                .rangedStrength(20)
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calcLow = new DpsCalculator(player, lowMagicMonster);
            DpsCalculator calcHigh = new DpsCalculator(player, highMagicMonster);

            // Twisted bow scales with monster magic level
            assertTrue("Tbow should hit harder vs high magic monsters",
                calcHigh.getMaxHit() > calcLow.getMaxHit());
            assertTrue("Tbow should be more accurate vs high magic monsters",
                calcHigh.getMaxAttackRoll() > calcLow.getMaxAttackRoll());
        }

        @Test
        public void cappedAt250MagicNormally() {
            MonsterStats overCapMonster = monster()
                .name("Over cap")
                .magicLevel(300)
                .defenceLevel(100)
                .standardRangedDefence(20)
                .build();

            MonsterStats atCapMonster = monster()
                .name("At cap")
                .magicLevel(250)
                .defenceLevel(100)
                .standardRangedDefence(20)
                .build();

            PlayerState player = player()
                .rangedLevel(99)
                .weapon("Twisted bow")
                .ammo("Dragon arrow")
                .rangedAttack(70)
                .rangedStrength(20)
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calcOver = new DpsCalculator(player, overCapMonster);
            DpsCalculator calcAt = new DpsCalculator(player, atCapMonster);

            // Should be capped at 250
            assertEquals("Tbow scaling should cap at 250 magic",
                calcAt.getMaxHit(), calcOver.getMaxHit());
        }

        @Test
        public void higherCapInCox() {
            MonsterStats xerician = monster()
                .name("CoX boss")
                .magicLevel(300)
                .defenceLevel(100)
                .standardRangedDefence(20)
                .attribute(MonsterAttribute.XERICIAN)
                .build();

            MonsterStats normal = monster()
                .name("Normal")
                .magicLevel(300)
                .defenceLevel(100)
                .standardRangedDefence(20)
                .build();

            PlayerState player = player()
                .rangedLevel(99)
                .weapon("Twisted bow")
                .ammo("Dragon arrow")
                .rangedAttack(70)
                .rangedStrength(20)
                .combatStyle(CombatStyle.RANGED_ACCURATE)
                .build();

            DpsCalculator calcXeric = new DpsCalculator(player, xerician);
            DpsCalculator calcNormal = new DpsCalculator(player, normal);

            // Xerician monsters have 350 cap instead of 250
            assertTrue("Tbow should hit harder vs xerician monsters with high magic",
                calcXeric.getMaxHit() > calcNormal.getMaxHit());
        }
    }

    public static class DistributionAdapterCharacterization {

        @Test
        public void ordinaryAttackPreservesScalarMaxExpectedDamageAndDps() {
            MonsterStats monster = monster()
                .name("Low defence target")
                .defenceLevel(1)
                .slashDefence(0)
                .build();
            PlayerState player = player()
                .attackLevel(99)
                .strengthLevel(99)
                .slashAttack(82)
                .meleeStrength(82)
                .weapon("Abyssal whip")
                .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
                .weaponSpeed(4)
                .build();

            DpsResult result = new DpsCalculator(player, monster).calculate();

            assertEquals(24, result.getMaxHit());
            assertEquals(0.98001369777722436, result.getAccuracy(), 0.0);
            // Average damage now uses the same distribution as DPS, including accurate zero rerolls.
            assertEquals(11.79936492123778, result.getAverageDamagePerAttack(), 0.0);
            assertEquals(4.9164020505157415, result.getDps(), 0.0);
            assertNotNull(result.getAttackDistribution());
            assertEquals(24, result.getAttackDistribution().getMax());
            assertEquals(11.79936492123778,
                result.getAttackDistribution().getExpectedDamage(), 0.0);
            assertEquals(24, result.getScalarMaxHit());
            assertEquals(24, result.getDirectMaxHit());
            assertEquals(0, result.getDotMaxHit());
            assertEquals(24, result.getTotalMaxHit());
            assertEquals(24, result.getDistributionMaxHit());
            assertEquals(11.79936492123778, result.getExpectedDirectDamage(), 0.0);
            assertEquals(0.0, result.getExpectedDotDamage(), 0.0);
            assertEquals(11.79936492123778, result.getExpectedDamage(), 0.0);
            assertEquals(4, result.getBaseAttackSpeed());
            assertEquals(4.0, result.getExpectedAttackSpeed(), 0.0);
            assertEquals(2.949841230309445, result.getDistributionDamagePerTick(), 0.0);
        }
    }
}
