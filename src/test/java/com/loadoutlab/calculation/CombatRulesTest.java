// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Tommy Lynch.
package com.loadoutlab.calculation;

import static org.junit.Assert.*;

import com.loadoutlab.data.*;
import com.loadoutlab.engine.*;
import com.loadoutlab.model.*;

import org.junit.Test;

import java.util.*;

/** Hand-derived examples and invariants; no calculator outputs are used as an oracle. */
public class CombatRulesTest {
    private PlayerState player(String weapon, CombatStyle style) {
        PlayerState p = new PlayerState();
        p.setCombatStyle(style);
        p.getEquippedItemNames()[3] = weapon;
        p.setWeaponSpeed(4);
        return p;
    }

    private MonsterStats target() {
        MonsterStats m = new MonsterStats();
        m.setName("Synthetic target");
        m.setHitpoints(100);
        m.setDefenceLevel(1);
        m.setMagicLevel(1);
        return m;
    }

    private DpsResult calc(PlayerState p, MonsterStats m) {
        return new DpsCalculator(p, m).calculate();
    }

    private void wear(PlayerState p, int slot, String name) {
        p.getEquippedItemNames()[slot] = name;
    }

    @Test
    public void chargeRequiresTheMatchingGodCape() {
        PlayerState p = player("Guthix staff", CombatStyle.MAGIC_ACCURATE);
        p.setSpellName("Claws of Guthix");
        p.setSpellMaxHit(20);
        p.setSpellbook("standard");
        p.setChargeSpellActive(true);
        assertEquals(20, calc(p, target()).getMaxHit());
        wear(p, 1, "Imbued zamorak cape");
        assertEquals(20, calc(p, target()).getMaxHit());
        wear(p, 1, "Imbued guthix cape");
        assertEquals(30, calc(p, target()).getMaxHit());
    }

    @Test
    public void zaryteRubySpecialHasOnlyAProcOrMiss() {
        PlayerState p = player("Zaryte crossbow", CombatStyle.RANGED_ACCURATE);
        wear(p, 13, "Ruby dragon bolts (e)");
        MonsterStats m = target();
        m.setHitpoints(1000);
        m.setDefenceLevel(300);
        DpsResult r = new DpsCalculator(p, m, true).calculate();
        double proc = .06 + .94 * r.getAccuracy();
        assertEquals(110 * proc, r.getExpectedDamage(), 1e-9);
    }

    @Test
    public void magicSalveAddsToGearDamageAndVoidHasItsOwnRoundingOrder() {
        PlayerState p = player("Trident of the Seas", CombatStyle.MAGIC_ACCURATE);
        p.getEquipmentStats().setMagicDamage(200);
        wear(p, 2, "Salve amulet(ei)");
        MonsterStats m = target();
        m.addAttribute(MonsterAttribute.UNDEAD);
        assertEquals(39, calc(p, m).getMaxHit());
        wear(p, 2, null);
        p.getEquipmentStats().setMagicDamage(0);
        wear(p, 0, "Void mage helm");
        wear(p, 4, "Elite void top");
        wear(p, 7, "Elite void robe");
        wear(p, 9, "Void knight gloves");
        p.getActivePrayers().add(Prayer.AUGURY);
        DpsResult r = calc(p, target());
        assertEquals(189 * 64, r.getAttackRoll(), 0);
        assertEquals(30, r.getMaxHit());
    }

    @Test
    public void fangAccuracyMatchesExhaustiveTwoAttacksAgainstOneDefence() {
        for (int a = 0; a <= 12; a++)
            for (int d = 0; d <= 12; d++) {
                int successes = 0;
                for (int x = 0; x <= a; x++)
                    for (int y = 0; y <= a; y++)
                        for (int defence = 0; defence <= d; defence++)
                            if (x > defence || y > defence) successes++;
                assertEquals(
                        successes / ((a + 1.0) * (a + 1) * (d + 1)),
                        DpsCalculator.doubleAttackAgainstOneDefence(a, d),
                        1e-12);
            }
    }

    @Test
    public void unarmedUsesDocumentedIntegerRollsAndSuccessfulZeroConversion() {
        DpsResult r = calc(player("", CombatStyle.UNARMED_PUNCH), target());
        assertEquals(110 * 64, r.getAttackRoll(), 0);
        assertEquals(10 * 64, r.getDefenceRoll(), 0);
        assertEquals(11, r.getMaxHit());
        double accuracy = 1 - 642.0 / (2 * 7041);
        assertEquals(accuracy, r.getAccuracy(), 1e-12);
        assertEquals(accuracy * (5.5 + 1.0 / 12) / 2.4, r.getDps(), 1e-12);
        assertEquals(1 - accuracy, r.getAttackDistribution().probabilityAt(0), 1e-12);
        assertEquals(accuracy * 2 / 12, r.getAttackDistribution().probabilityAt(1), 1e-12);
    }

    @Test
    public void prayersRoundLevelsBeforeEquipmentAndTaskModifiers() {
        PlayerState p = player("Abyssal whip", CombatStyle.MELEE_ACCURATE_SLASH);
        p.getEquipmentStats().setSlashAttack(82);
        p.getEquipmentStats().setMeleeStrength(82);
        p.getActivePrayers().add(Prayer.PIETY);
        DpsResult r = calc(p, target());
        assertEquals(129 * 146, r.getAttackRoll(), 0);
        assertEquals(29, r.getMaxHit());
        p.setOnSlayerTask(true);
        wear(p, 0, "Slayer helmet");
        r = calc(p, target());
        assertEquals(129 * 146 * 7 / 6, r.getAttackRoll(), 0);
        assertEquals(29 * 7 / 6, r.getMaxHit());
    }

    @Test
    public void salveAndSlayerDoNotStack() {
        PlayerState p = player("Abyssal whip", CombatStyle.MELEE_ACCURATE_SLASH);
        p.getEquipmentStats().setMeleeStrength(82);
        p.setOnSlayerTask(true);
        wear(p, 0, "Slayer helmet (i)");
        wear(p, 2, "Salve amulet (ei)");
        MonsterStats m = target();
        m.addAttribute(MonsterAttribute.UNDEAD);
        DpsResult both = calc(p, m);
        wear(p, 0, null);
        DpsResult salve = calc(p, m);
        assertEquals(salve.getDps(), both.getDps(), 0);
    }

    @Test
    public void soulStacksAddToPrayerRatherThanMultiplyingThePrayerBoost() {
        PlayerState p = player("Soulreaper axe", CombatStyle.MELEE_AGGRESSIVE_SLASH);
        p.getEquipmentStats().setMeleeStrength(100);
        p.setSoulreaperStacks(5);
        p.getActivePrayers().add(Prayer.PIETY);
        // floor(99 * (1 + .23 + .30)) + 3 + 8 = 162; floor((162*164+320)/640) = 42.
        assertEquals(42, calc(p, target()).getMaxHit());
    }

    @Test
    public void scytheUsesFloorHalfAndQuarterWithoutInventedExtraHits() {
        PlayerState p = player("Scythe of Vitur", CombatStyle.MELEE_AGGRESSIVE_SLASH);
        p.getEquipmentStats().setMeleeStrength(215);
        MonsterStats m = target();
        assertEquals(48, calc(p, m).getMaxHit());
        m.setSize(2);
        assertEquals(72, calc(p, m).getMaxHit());
        m.setSize(3);
        assertEquals(84, calc(p, m).getMaxHit());
        m.setFlatArmour(5);
        assertEquals(69, calc(p, m).getMaxHit());
    }

    @Test
    public void dragonDaggerMitigatesEachHitSeparately() {
        PlayerState p = player("Dragon dagger", CombatStyle.MELEE_AGGRESSIVE_SLASH);
        p.getEquipmentStats().setMeleeStrength(100);
        MonsterStats m = target();
        DpsResult normal = new DpsCalculator(p, m, true).calculate();
        m.setFlatArmour(4);
        DpsResult armour = new DpsCalculator(p, m, true).calculate();
        assertEquals(normal.getMaxHit() - 8, armour.getMaxHit());
    }

    @Test
    public void corpseRequiresStabWithCorpbaneWeapon() {
        PlayerState p = player("Zamorakian spear", CombatStyle.MELEE_AGGRESSIVE_STAB);
        p.getEquippedItemCategories()[3] = "Spear";
        p.getEquipmentStats().setMeleeStrength(100);
        MonsterStats m = target();
        m.setName("Corporeal Beast");
        int stab = calc(p, m).getMaxHit();
        p.setCombatStyle(CombatStyle.MELEE_AGGRESSIVE_SLASH);
        assertEquals(stab / 2, calc(p, m).getMaxHit());
    }

    @Test
    public void elementalSpellsScaleWithinTheirTierAndApplyWeakness() {
        PlayerState p = player("Staff of water", CombatStyle.MAGIC_AUTOCAST);
        p.setSpellName("Water Surge");
        p.setSpellElement("water");
        p.setSpellbook("standard");
        p.setSpellMaxHit(22);
        assertEquals(24, calc(p, target()).getMaxHit());
        MonsterStats m = target();
        m.setWeaknessElement(WeaknessElement.WATER);
        m.setWeaknessSeverity(100);
        assertEquals(48, calc(p, m).getMaxHit());
        p.setMagicLevel(85);
        assertEquals(44, calc(p, m).getMaxHit());
    }

    @Test
    public void shadowOnlyMultipliesEquipmentForItsBuiltInSpell() {
        PlayerState p = player("Tumeken's shadow", CombatStyle.MAGIC_ACCURATE);
        p.getEquipmentStats().setMagicAttack(35);
        p.getEquipmentStats().setMagicDamage(200);
        assertEquals(54, calc(p, target()).getMaxHit());
        MonsterStats toa = target();
        toa.setName("Akkha");
        assertEquals(61, calc(p, toa).getMaxHit());
        p.setSpellName("Fire Surge");
        p.setSpellElement("fire");
        p.setSpellbook("standard");
        p.setSpellMaxHit(24);
        assertEquals(28, calc(p, target()).getMaxHit());
    }

    @Test
    public void sangAdditionalDamageIsConditionalOnSuccess() {
        PlayerState p = player("Sanguinesti staff", CombatStyle.MAGIC_ACCURATE);
        DpsResult r = calc(p, target());
        assertEquals(41, r.getMaxHit());
        assertEquals(r.getAccuracy() * (16.5 + 1.0 / 34 + 1.6), r.getExpectedDamage(), 1e-10);
        assertEquals(1 - r.getAccuracy(), r.getAttackDistribution().probabilityAt(0), 1e-12);
    }

    @Test
    public void boneStaffCannotAttackNonRats() {
        PlayerState p = player("Bone staff", CombatStyle.MAGIC_ACCURATE);
        MonsterStats m = target();
        assertThrows(IllegalArgumentException.class, () -> calc(p, m));
        m.addAttribute(MonsterAttribute.RAT);
        assertEquals(38, calc(p, m).getMaxHit());
    }

    @Test
    public void markedDemonbaneAddsDamageAfterGear() {
        PlayerState p = player("Staff of the Dead", CombatStyle.MAGIC_AUTOCAST);
        p.setSpellName("Dark Demonbane");
        p.setSpellbook("arceuus");
        p.setSpellMaxHit(30);
        p.getEquipmentStats().setMagicDamage(150);
        MonsterStats m = target();
        m.addAttribute(MonsterAttribute.DEMON);
        assertEquals(34, calc(p, m).getMaxHit());
        p.setMarkOfDarknessActive(true);
        assertEquals(42, calc(p, m).getMaxHit());
    }

    @Test
    public void darkBowShootsTwiceAndItsSpecialCapsEachArrow() {
        PlayerState p = player("Dark bow", CombatStyle.RANGED_RAPID);
        p.getEquippedItemCategories()[3] = "Bow";
        wear(p, 13, "Dragon arrow");
        p.getEquipmentStats().setRangedStrength(200);
        DpsResult normal = calc(p, target());
        assertEquals(88, normal.getMaxHit());
        DpsResult spec = new DpsCalculator(p, target(), true).calculate();
        assertEquals(96, spec.getMaxHit());
        assertEquals(0, spec.getAttackDistribution().probabilityAt(0), 0);
    }

    @Test
    public void defenceDrainsCopyTheirInputsAndUseSequentialIntegerRounding() {
        MonsterStats m = target();
        m.setDefenceLevel(101);
        m.getInputs().getDefenceReductions().setDwh(2);
        MonsterStats reduced = MonsterScaling.scale(m);
        assertEquals(50, reduced.getDefenceLevel());
        assertEquals(101, m.getDefenceLevel());
        m.getInputs().getDefenceReductions().setBgs(20);
        assertEquals(30, MonsterScaling.scale(m).getDefenceLevel());
    }

    @Test
    public void unsupportedSpecialDoesNotSuppressNormalComparison() {
        com.dpscalc.scenario.Scenario.Loadout loadout = new com.dpscalc.scenario.Scenario.Loadout();
        loadout.specialAttack = true;
        com.dpscalc.scenario.ScenarioCalculator.Result result =
                new com.dpscalc.scenario.ScenarioCalculator(
                                new com.loadoutlab.equipment.EquipmentPreparationFacade())
                        .calculate(loadout, target());
        assertNull(result.error);
        assertNotNull(result.normal);
        assertNull(result.special);
        assertTrue(
                result.warnings.stream().anyMatch(w -> w.startsWith("Special result unavailable")));
    }

    @Test
    public void allDamageOutcomesHaveUnitMassAndNonnegativeFiniteDps() {
        for (int level : new int[] {1, 25, 50, 99, 126})
            for (int defence : new int[] {0, 1, 50, 250, 1000})
                for (CombatStyle style :
                        List.of(
                                CombatStyle.MELEE_AGGRESSIVE_SLASH,
                                CombatStyle.RANGED_RAPID,
                                CombatStyle.MAGIC_ACCURATE)) {
                    PlayerState p =
                            player(
                                    style.getAttackType().isMagic()
                                            ? "Trident of the Seas"
                                            : "Synthetic weapon",
                                    style);
                    p.setAttackLevel(level);
                    p.setStrengthLevel(level);
                    p.setRangedLevel(level);
                    p.setMagicLevel(level);
                    MonsterStats m = target();
                    m.setDefenceLevel(defence);
                    m.setMagicLevel(defence);
                    DpsResult r = calc(p, m);
                    assertEquals(
                            1,
                            Arrays.stream(r.getAttackDistribution().probabilities()).sum(),
                            1e-10);
                    assertTrue(Double.isFinite(r.getDps()) && r.getDps() >= 0);
                }
    }
}
