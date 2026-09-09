package com.dpscalc;

import static org.junit.Assert.*;

import com.loadoutlab.model.*;
import com.loadoutlab.data.*;
import com.loadoutlab.equipment.*;
import com.dpscalc.scenario.*;
import com.loadoutlab.model.*;

import org.junit.Test;

import java.util.*;
import java.util.concurrent.CancellationException;

public class BankOptimizerTest {
    private final EquipmentPreparationFacade equipment = new EquipmentPreparationFacade();

    private OwnedEquipment owned(int... ids) {
        Map<Integer, Integer> items = new TreeMap<>();
        for (int id : ids) items.put(id, 1);
        return new OwnedEquipment(
                "player",
                1,
                1,
                items,
                Map.of(),
                Map.of(),
                Map.of(
                        "attack",
                        99,
                        "strength",
                        99,
                        "defence",
                        99,
                        "ranged",
                        99,
                        "magic",
                        99,
                        "hitpoints",
                        99,
                        "prayer",
                        99));
    }

    private MonsterStats target() {
        MonsterStats m = new MonsterStats();
        m.setId(-1);
        m.setName("Target");
        m.setSize(1);
        m.setSpeed(4);
        m.setHitpoints(100);
        m.setDefenceLevel(150);
        return m;
    }

    @Test
    public void knownFormulaLimitationsRequireExplicitInclusion() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.getEquippedItemIds()[3] = 4151;
        MonsterStats affected = target();
        affected.setName("Araxyte test");
        BankOptimizer optimizer = new BankOptimizer(equipment);
        try {
            optimizer.search(
                    new BankOptimizer.Request(base, affected, owned(4151), Set.of(), false),
                    n -> {});
            fail("Known affected target must be excluded");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("known formula"));
        }
        BankOptimizer.Result allowed =
                optimizer.search(
                        new BankOptimizer.Request(
                                base,
                                affected,
                                owned(4151),
                                Set.of(),
                                false,
                                "Melee",
                                base,
                                new Encounter(),
                                true),
                        n -> {});
        assertFalse(allowed.calculations.get(0).limitations.isEmpty());
        assertFalse(new ScenarioCalculator(equipment).score(base, affected).limitations.isEmpty());
    }

    @Test
    public void targetDefencesChangeWinningWeaponStyleWithoutChangingInputs() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.getEquippedItemIds()[3] = 1215;
        String before = Scenario.JSON.toJson(base);
        MonsterStats stab = target();
        stab.setStabDefence(-50);
        stab.setSlashDefence(1000);
        stab.setCrushDefence(1000);
        BankOptimizer optimizer = new BankOptimizer(equipment);
        BankOptimizer.Result first =
                optimizer.search(
                        new BankOptimizer.Request(base, stab, owned(1215), Set.of(3), false),
                        n -> {});
        assertEquals(
                AttackType.STAB, first.alternatives.get(0).player.getCombatStyle().getAttackType());
        assertTrue(first.exhaustive);
        MonsterStats slash = target();
        slash.setStabDefence(1000);
        slash.setSlashDefence(-50);
        slash.setCrushDefence(1000);
        BankOptimizer.Result second =
                optimizer.search(
                        new BankOptimizer.Request(base, slash, owned(1215), Set.of(3), false),
                        n -> {});
        assertEquals(
                AttackType.SLASH,
                second.alternatives.get(0).player.getCombatStyle().getAttackType());
        assertEquals(before, Scenario.JSON.toJson(base));
        assertEquals(150, stab.getDefenceLevel());
    }

    @Test
    public void explicitCombatTypeSearchDoesNotChangeStartingDraft() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.getEquippedItemIds()[3] = 4151;
        String before = Scenario.JSON.toJson(base);
        BankOptimizer optimizer = new BankOptimizer(equipment);
        OwnedEquipment bank = owned(4151, 9185, 9144, 11905);
        BankOptimizer.Result ranged =
                optimizer.search(
                        new BankOptimizer.Request(base, target(), bank, Set.of(), true, "Ranged"),
                        n -> {});
        assertTrue(ranged.alternatives.get(0).player.getCombatStyle().getAttackType().isRanged());
        assertEquals(9185, ranged.alternatives.get(0).player.getWeaponId());
        BankOptimizer.Result magic =
                optimizer.search(
                        new BankOptimizer.Request(base, target(), bank, Set.of(), true, "Magic"),
                        n -> {});
        assertTrue(magic.alternatives.get(0).player.getCombatStyle().getAttackType().isMagic());
        assertEquals(11905, magic.alternatives.get(0).player.getWeaponId());
        assertEquals(before, Scenario.JSON.toJson(base));
    }

    @Test
    public void nonCombatWeaponMarkersNeverBecomeOneTickAttacks() {
        Scenario.Loadout greegree = new Scenario.Loadout();
        greegree.player.getEquippedItemIds()[3] = 4031;
        ScenarioCalculator.Result invalid =
                new ScenarioCalculator(equipment).calculate(greegree, target());
        assertNotNull(invalid.error);
        assertTrue(invalid.error.contains("cannot be used as a combat weapon"));
        assertTrue(WeaponStyles.available(greegree.player, equipment).isEmpty());
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        new Scenario.Loadout(),
                                        target(),
                                        owned(4031, 4024, 751, 4151),
                                        Set.of(),
                                        true),
                                n -> {});
        assertEquals(4151, result.alternatives.get(0).player.getWeaponId());
        assertTrue(
                result.exclusions.stream()
                        .anyMatch(s -> s.contains("greegree") && s.contains("combat weapon")));
    }

    @Test
    public void magicAutomaticallyChoosesSpellForElementalWeaknessAndPreservesIt() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.setSpellName("Fire Strike");
        base.player.setSpellElement("fire");
        base.player.setSpellMaxHit(8);
        base.player.setSpellbook("standard");
        String before = Scenario.JSON.toJson(base);
        BankOptimizer optimizer = new BankOptimizer(equipment);
        for (WeaknessElement element :
                new WeaknessElement[] {WeaknessElement.WATER, WeaknessElement.FIRE}) {
            MonsterStats monster = target();
            monster.setWeaknessElement(element);
            monster.setWeaknessSeverity(100);
            BankOptimizer.Result result =
                    optimizer.search(
                            new BankOptimizer.Request(
                                    base, monster, owned(1381, 11905), Set.of(), false, "Magic"),
                            n -> {});
            Scenario.Loadout winner = result.alternatives.get(0);
            assertEquals(
                    element == WeaknessElement.WATER ? "Water Surge" : "Fire Surge",
                    winner.player.getSpellName());
            assertEquals(1381, winner.player.getWeaponId());
            double score = new ScenarioCalculator(equipment).score(winner, monster).normal.getDps();
            assertEquals(result.calculations.get(0).normal.getDps(), score, 1e-12);
            Scenario.mergeLive(winner, Scenario.defaults());
            assertEquals(element.getJsonName(), winner.player.getSpellElement());
            assertEquals("standard", winner.player.getSpellbook());
        }
        assertEquals(before, Scenario.JSON.toJson(base));
    }

    @Test
    public void automaticAncientSpellsRespectLevelAndBoosts() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.setMagicLevel(70);
        BankOptimizer optimizer = new BankOptimizer(equipment);
        BankOptimizer.Result low =
                optimizer.search(
                        new BankOptimizer.Request(
                                base, target(), owned(4675), Set.of(), false, "Magic"),
                        n -> {});
        assertEquals("Ice Burst", low.alternatives.get(0).player.getSpellName());
        assertEquals("ancient", low.alternatives.get(0).player.getSpellbook());
        base.player.setMagicLevel(90);
        base.potions.add("Magic");
        BankOptimizer.Result boosted =
                optimizer.search(
                        new BankOptimizer.Request(
                                base, target(), owned(4675), Set.of(), false, "Magic"),
                        n -> {});
        assertEquals("Ice Barrage", boosted.alternatives.get(0).player.getSpellName());
        base.potions.clear();
        BankOptimizer.Result unboosted =
                optimizer.search(
                        new BankOptimizer.Request(
                                base, target(), owned(4675), Set.of(), false, "Magic"),
                        n -> {});
        assertEquals("Shadow Barrage", unboosted.alternatives.get(0).player.getSpellName());
    }

    @Test
    public void regularStaffCannotAutocastAncientsOrIbanAndIbanRetainsItsOwnMaxHit() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.setMagicLevel(50);
        BankOptimizer optimizer = new BankOptimizer(equipment);
        BankOptimizer.Result standard =
                optimizer.search(
                        new BankOptimizer.Request(
                                base, target(), owned(1381), Set.of(), false, "Magic"),
                        n -> {});
        assertTrue(standard.alternatives.get(0).player.getSpellName().endsWith("Blast"));
        assertNotEquals("Iban Blast", standard.alternatives.get(0).player.getSpellName());
        BankOptimizer.Result iban =
                optimizer.search(
                        new BankOptimizer.Request(
                                base, target(), owned(1381, 1409), Set.of(), false, "Magic"),
                        n -> {});
        assertEquals("Iban Blast", iban.alternatives.get(0).player.getSpellName());
        assertEquals(1409, iban.alternatives.get(0).player.getWeaponId());
        assertEquals(25, iban.calculations.get(0).normal.getMaxHit());
    }

    @Test
    public void automaticMagicStillChoosesPoweredAttackWhenItWins() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.setSpellName("Ice Barrage");
        base.player.setSpellMaxHit(30);
        base.player.setSpellbook("ancient");
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        base,
                                        target(),
                                        owned(1381, 11905),
                                        Set.of(),
                                        false,
                                        "Magic"),
                                n -> {});
        assertEquals(11905, result.alternatives.get(0).player.getWeaponId());
        assertNull(result.alternatives.get(0).player.getSpellName());
        assertNull(result.alternatives.get(0).player.getSpellElement());
    }

    @Test
    public void automaticDemonbaneIsRestrictedToDemonTargets() {
        BankOptimizer optimizer = new BankOptimizer(equipment);
        MonsterStats monster = target();
        BankOptimizer.Result ordinary =
                optimizer.search(
                        new BankOptimizer.Request(
                                new Scenario.Loadout(),
                                monster,
                                owned(11791),
                                Set.of(),
                                false,
                                "Magic"),
                        n -> {});
        assertFalse(ordinary.alternatives.get(0).player.getSpellName().endsWith("Demonbane"));
        assertNotEquals("Crumble Undead", ordinary.alternatives.get(0).player.getSpellName());
        monster.getAttributes().add(MonsterAttribute.DEMON);
        BankOptimizer.Result demon =
                optimizer.search(
                        new BankOptimizer.Request(
                                new Scenario.Loadout(),
                                monster,
                                owned(11791),
                                Set.of(),
                                false,
                                "Magic"),
                        n -> {});
        assertEquals("Dark Demonbane", demon.alternatives.get(0).player.getSpellName());
    }

    @Test
    public void meleeAmmoUsesBlessingsOrEmptyInsteadOfArrows() {
        BankOptimizer optimizer = new BankOptimizer(equipment);
        BankOptimizer.Result result =
                optimizer.search(
                        new BankOptimizer.Request(
                                new Scenario.Loadout(),
                                target(),
                                owned(4151, 892, 22947, 20220),
                                Set.of(),
                                false),
                        n -> {});
        assertEquals(22947, result.alternatives.get(0).player.getEquippedItemIds()[13]);
        BankOptimizer.Result noBlessing =
                optimizer.search(
                        new BankOptimizer.Request(
                                new Scenario.Loadout(), target(), owned(4151, 892), Set.of(), true),
                        n -> {});
        assertEquals(-1, noBlessing.alternatives.get(0).player.getEquippedItemIds()[13]);
        Scenario.Loadout locked = new Scenario.Loadout();
        locked.player.getEquippedItemIds()[13] = 892;
        try {
            optimizer.search(
                    new BankOptimizer.Request(locked, target(), owned(4151, 892), Set.of(13), true),
                    n -> {});
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Locked Ammo"));
        }
    }

    @Test
    public void targetRequiredRangedAmmoWinsOverBlessingsAndWrongBolts() {
        MonsterStats leafy = target();
        leafy.getAttributes().add(MonsterAttribute.LEAFY);
        OwnedEquipment bank =
                new OwnedEquipment(
                        "player",
                        1,
                        1,
                        Map.of(9185, 1, 9144, 100, 11875, 100, 22947, 1),
                        Map.of(),
                        Map.of(),
                        Map.of("ranged", 99, "slayer", 99));
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        new Scenario.Loadout(),
                                        leafy,
                                        bank,
                                        Set.of(),
                                        true,
                                        "Ranged"),
                                n -> {});
        assertEquals(11875, result.alternatives.get(0).player.getEquippedItemIds()[13]);
        assertTrue(result.calculations.get(0).normal.getDps() > 0);
    }

    @Test
    public void selfContainedRangedWeaponsDoNotCarryUnusedArrows() {
        OwnedEquipment bank =
                new OwnedEquipment(
                        "player",
                        1,
                        1,
                        Map.of(23983, 1, 892, 100),
                        Map.of(),
                        Map.of(),
                        Map.of("ranged", 99, "agility", 99));
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        new Scenario.Loadout(),
                                        target(),
                                        bank,
                                        Set.of(),
                                        true,
                                        "Ranged"),
                                n -> {});
        assertEquals(23983, result.alternatives.get(0).player.getWeaponId());
        assertEquals(-1, result.alternatives.get(0).player.getEquippedItemIds()[13]);
    }

    @Test
    public void exhaustiveWinnerMatchesIndependentEnumerationAndUsesOwnedItems() {
        Scenario.Loadout base = new Scenario.Loadout();
        OwnedEquipment bank = owned(4151, 1215, 1127, 12954);
        MonsterStats target = target();
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(base, target, bank, Set.of(), true),
                                n -> {});
        double expected = 0;
        ScenarioCalculator calculator = new ScenarioCalculator(equipment);
        for (int weapon : new int[] {-1, 4151, 1215})
            for (int body : new int[] {-1, 1127})
                for (int shield : new int[] {-1, 12954}) {
                    Scenario.Loadout candidate = base.copy();
                    candidate.player.getEquippedItemIds()[3] = weapon;
                    candidate.player.getEquippedItemIds()[4] = body;
                    candidate.player.getEquippedItemIds()[5] = shield;
                    for (CombatStyle style : WeaponStyles.available(candidate.player, equipment)) {
                        candidate.player.setCombatStyle(style);
                        ScenarioCalculator.Result score = calculator.score(candidate, target);
                        if (score.error == null)
                            expected = Math.max(expected, score.normal.getDps());
                    }
                }
        assertTrue(result.exhaustive);
        assertEquals(expected, result.calculations.get(0).normal.getDps(), 1e-12);
        assertEquals(
                "Equal DPS should retain useful armour",
                1127,
                result.alternatives.get(0).player.getEquippedItemIds()[4]);
        for (int id : result.alternatives.get(0).player.getEquippedItemIds())
            assertTrue(bank.owns(id));
        Scenario.Loadout generated = result.alternatives.get(0);
        int weapon = generated.player.getWeaponId();
        Scenario.mergeLive(generated, Scenario.defaults());
        assertEquals(weapon, generated.player.getWeaponId());
    }

    @Test
    public void rejectsMissingLockedItemsAndRespectsTwoHandedEquipment() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.getEquippedItemIds()[3] = 11802;
        base.player.getEquippedItemIds()[5] = 12954;
        try {
            new BankOptimizer(equipment)
                    .search(
                            new BankOptimizer.Request(base, target(), owned(4151), Set.of(3), true),
                            n -> {});
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Locked Weapon"));
        }
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        base, target(), owned(11802, 12954), Set.of(3), true),
                                n -> {});
        assertEquals(11802, result.alternatives.get(0).player.getWeaponId());
        assertEquals(-1, result.alternatives.get(0).player.getEquippedItemIds()[5]);
    }

    @Test
    public void knownUnmetRequirementsAreExcludedEvenWhenUnverifiedItemsAllowed() {
        OwnedEquipment bank =
                new OwnedEquipment(
                        "player", 1, 1, Map.of(4151, 1), Map.of(), Map.of(), Map.of("attack", 1));
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        new Scenario.Loadout(), target(), bank, Set.of(), true),
                                n -> {});
        assertEquals(-1, result.alternatives.get(0).player.getWeaponId());
        assertTrue(result.exclusions.stream().anyMatch(s -> s.contains("requires 70 attack")));
    }

    @Test
    public void bankSnapshotsAreImmutableAndRequireMatchingFreshProfile() {
        Map<Integer, Integer> bank = new HashMap<>();
        bank.put(4151, 1);
        OwnedEquipment snapshot =
                new OwnedEquipment("player", 3, 10, bank, Map.of(4151, 2), Map.of(), Map.of());
        bank.clear();
        assertEquals(Integer.valueOf(3), snapshot.quantities.get(4151));
        assertTrue(snapshot.ready("player"));
        assertFalse(snapshot.ready("other"));
        assertFalse(OwnedEquipment.empty(4).ready("player"));
        try {
            snapshot.bank.clear();
            fail();
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void cancellationIsImmediate() {
        Thread.currentThread().interrupt();
        try {
            new BankOptimizer(equipment)
                    .search(
                            new BankOptimizer.Request(
                                    new Scenario.Loadout(), target(), owned(4151), Set.of(), false),
                            n -> {});
            fail();
        } catch (CancellationException expected) {
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void rangedSearchRejectsIncompatibleAmmoAndTestsAvailableStyles() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.getEquippedItemIds()[3] = 9185;
        base.player.setCombatStyle(CombatStyle.RANGED_RAPID);
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        base, target(), owned(9185, 892, 9144), Set.of(3), false),
                                n -> {});
        Scenario.Loadout winner = result.alternatives.get(0);
        assertEquals(9144, winner.player.getEquippedItemIds()[13]);
        assertEquals("Rapid", winner.player.getCombatStyle().getStance());
        assertTrue(winner.player.getCombatStyle().getAttackType().isRanged());
    }

    @Test
    public void fullSetCombinationsAreEvaluatedTogether() {
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.getEquippedItemIds()[3] = 4718;
        base.player.setCurrentHitpoints(1);
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        base,
                                        target(),
                                        owned(4718, 4716, 4720, 4722, 1127),
                                        Set.of(3),
                                        true),
                                n -> {});
        Scenario.Loadout winner = result.alternatives.get(0);
        assertEquals(4716, winner.player.getEquippedItemIds()[0]);
        assertEquals(4720, winner.player.getEquippedItemIds()[4]);
        assertEquals(4722, winner.player.getEquippedItemIds()[7]);
        assertTrue(result.exhaustive);
    }

    @Test
    public void boundedSearchNeverClaimsExhaustive() {
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(
                                        new Scenario.Loadout(),
                                        target(),
                                        owned(4151, 1215, 1127),
                                        Set.of(),
                                        true),
                                n -> {},
                                1,
                                15000);
        assertFalse(result.exhaustive);
        assertEquals(1, result.evaluated);
    }

    @Test
    public void largerPoolReturnsScoredOwnedGearWithinBudget() {
        Set<Integer> ids = new TreeSet<>();
        ids.add(1215);
        equipment.getItems().entrySet().stream()
                .filter(
                        e ->
                                e.getValue().getName().startsWith("Rune")
                                        || e.getValue().getName().startsWith("Dragon")
                                        || e.getValue().getName().startsWith("Dharok")
                                        || e.getValue().getName().startsWith("Obsidian"))
                .limit(180)
                .forEach(e -> ids.add(e.getKey()));
        Scenario.Loadout base = new Scenario.Loadout();
        base.player.getEquippedItemIds()[3] = 1215;
        OwnedEquipment bank = owned(ids.stream().mapToInt(Integer::intValue).toArray());
        long start = System.nanoTime();
        BankOptimizer.Result result =
                new BankOptimizer(equipment)
                        .search(
                                new BankOptimizer.Request(base, target(), bank, Set.of(3), true),
                                n -> {},
                                2500,
                                4000);
        assertFalse(result.exhaustive);
        assertTrue(result.evaluated <= 2500);
        assertTrue(result.calculations.get(0).normal.getDps() > 0);
        for (int id : result.alternatives.get(0).player.getEquippedItemIds())
            assertTrue(bank.owns(id));
        assertTrue("Search should remain bounded", (System.nanoTime() - start) / 1_000_000 < 10000);
        System.out.println(
                "Optimizer larger-pool check: "
                        + ids.size()
                        + " item types, "
                        + result.evaluated
                        + " scores in "
                        + ((System.nanoTime() - start) / 1_000_000)
                        + " ms");
    }
}
