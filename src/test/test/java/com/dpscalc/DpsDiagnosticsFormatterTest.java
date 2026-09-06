package com.dpscalc;

import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.data.WeaknessElement;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.Prayer;
import org.junit.Test;

import java.util.EnumSet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DpsDiagnosticsFormatterTest {

    @Test
    public void format_includesLiveInputsAndRolls_whenSnapshotIsPopulated() {
        MonsterStats monster = monsterWithInputs();
        PlayerState player = playerWithEquipment();

        String diagnostics = DpsDiagnosticsFormatter.format(DpsDiagnosticsFormatter.snapshot()
            .withTarget(new DpsDiagnosticsFormatter.TargetInfo(11756, "Mokhaiotl", 42, "live"))
            .withMonster(monster)
            .withPlayer(player)
            .withSettings(new DpsDiagnosticsFormatter.SettingsInfo(true, false, true, false))
            .withPanelResult(result(6.75, 33, 0.625, 12345, 6789, 4))
            .withLiveResult(result(6.50, 32, 0.600, 12000, 6700, 4))
            .withSpecResult(result(9.25, 58, 0.710, 22000, 6700, 5)));

        assertTrue(diagnostics.contains("== Target =="));
        assertTrue(diagnostics.contains("NPC: 11756 | Mokhaiotl | idx 42 | live"));
        assertTrue(diagnostics.contains("id/name/version: 11756 | Mokhaiotl | Delve 8"));
        assertTrue(diagnostics.contains("size/speed: 5 | 4"));
        assertTrue(diagnostics.contains("levels: atk 320 | str 300 | def 260 | hp 750 | mage 240 | range 180"));
        assertTrue(diagnostics.contains("def: stab 80 | slash 90 | crush 100 | magic 110 | light 120 | std 130 | heavy 140 | flat 12"));
        assertTrue(diagnostics.contains("weakness: earth 35"));
        assertTrue(diagnostics.contains("attributes: DEMON, XERICIAN"));
        assertTrue(diagnostics.contains("toa: inv 350 | path 4"));
        assertTrue(diagnostics.contains("cox CM: true | party combat 126 | mining 297 | hp 99 | size 3"));
        assertTrue(diagnostics.contains("hp/phase/demonbane: 612 | shielded | 115"));
        assertTrue(diagnostics.contains("reductions: dwh 1 | bgs 42 | arclight 1 | emberlight 0 | tonalztic 2 | elder maul 1 | vuln 1 | accursed 1 | seercull 4 | ayak 3"));
        assertTrue(diagnostics.contains("levels: atk 99+5 | str 99+6 | def 98+0 | range 97+4 | mage 96+3 | hp 95/90"));
        assertTrue(diagnostics.contains("WEAPON: 20997 | Twisted bow | Twisted | Bow"));
        assertTrue(diagnostics.contains("AMMO: 21944 | Dragon arrows | - | Arrow"));
        assertTrue(diagnostics.contains("attack: stab 15 | slash 16 | crush 17 | magic 18 | range 19"));
        assertTrue(diagnostics.contains("strength: melee 20 | range 21 | magic 22%"));
        assertTrue(diagnostics.contains("style: Rapid | RANGED_STANDARD | Rapid | weapon speed 4"));
        assertTrue(diagnostics.contains("prayers: RIGOUR"));
        assertTrue(diagnostics.contains("settings: best prayer true | max boosts false | slayer true | charge false"));
        assertTrue(diagnostics.contains("panel: dps 6.75 | max 33 | acc 62.5% | atk 12345 | def 6789 | speed 4"));
        assertTrue(diagnostics.contains("cached live: dps 6.50 | max 32 | acc 60.0% | atk 12000 | def 6700 | speed 4"));
        assertTrue(diagnostics.contains("cached spec: dps 9.25 | max 58 | acc 71.0% | atk 22000 | def 6700 | speed 5"));
        // These hand-built scalar results have no distribution, so kill-time metrics are unavailable.
        assertTrue(diagnostics.contains("avg 16.5 | dpt 2.58 | hp 750 | hits N/A | ttk N/A | kph N/A"));
        assertTrue(diagnostics.contains("avg 16.5"));
        assertTrue(diagnostics.contains("avg 29.0"));
    }

    @Test
    public void format_usesDashForMissingLiveMetadata_whenVersionsAndCategoriesAreAbsent() {
        PlayerState player = new PlayerState();
        player.setEquippedItemIds(new int[14]);
        player.setEquippedItemNames(new String[14]);
        player.setEquippedItemVersions(new String[14]);
        player.setEquippedItemCategories(new String[14]);
        player.getEquippedItemIds()[EquipmentSlot.WEAPON.getIndex()] = 4151;
        player.getEquippedItemNames()[EquipmentSlot.WEAPON.getIndex()] = "Abyssal whip";

        String diagnostics = DpsDiagnosticsFormatter.format(DpsDiagnosticsFormatter.snapshot()
            .withTarget(new DpsDiagnosticsFormatter.TargetInfo(-1, null, -1, "none"))
            .withPlayer(player));

        assertTrue(diagnostics.contains("NPC: - | - | idx - | none"));
        assertTrue(diagnostics.contains("WEAPON: 4151 | Abyssal whip | - | -"));
        assertTrue(diagnostics.contains("panel: -"));
        assertFalse(diagnostics.contains("null"));
    }

    private static MonsterStats monsterWithInputs() {
        MonsterStats monster = new MonsterStats();
        monster.setId(11756);
        monster.setName("Mokhaiotl");
        monster.setVersion("Delve 8");
        monster.setSize(5);
        monster.setSpeed(4);
        monster.setAttackLevel(320);
        monster.setStrengthLevel(300);
        monster.setDefenceLevel(260);
        monster.setHitpoints(750);
        monster.setMagicLevel(240);
        monster.setRangedLevel(180);
        monster.setStabDefence(80);
        monster.setSlashDefence(90);
        monster.setCrushDefence(100);
        monster.setMagicDefence(110);
        monster.setLightRangedDefence(120);
        monster.setStandardRangedDefence(130);
        monster.setHeavyRangedDefence(140);
        monster.setFlatArmour(12);
        monster.setWeaknessElement(WeaknessElement.EARTH);
        monster.setWeaknessSeverity(35);
        monster.setAttributes(EnumSet.of(MonsterAttribute.DEMON, MonsterAttribute.XERICIAN));

        MonsterInputs inputs = new MonsterInputs();
        inputs.setToaInvocationLevel(350);
        inputs.setToaPathLevel(4);
        inputs.setMonsterCurrentHp(612);
        inputs.setPhase("shielded");
        inputs.setFromCoxCm(true);
        inputs.setPartyMaxCombatLevel(126);
        inputs.setPartySumMiningLevel(297);
        inputs.setPartyMaxHpLevel(99);
        inputs.setPartySize(3);
        inputs.setDemonbaneVulnerability(115);
        inputs.getDefenceReductions().setDwh(1);
        inputs.getDefenceReductions().setBgs(42);
        inputs.getDefenceReductions().setArclight(1);
        inputs.getDefenceReductions().setTonalztic(2);
        inputs.getDefenceReductions().setElderMaul(1);
        inputs.getDefenceReductions().setVulnerability(1);
        inputs.getDefenceReductions().setAccursedSceptre(1);
        inputs.getDefenceReductions().setSeercull(4);
        inputs.getDefenceReductions().setAyak(3);
        monster.setInputs(inputs);
        return monster;
    }

    private static PlayerState playerWithEquipment() {
        PlayerState player = new PlayerState();
        player.setAttackLevel(99);
        player.setStrengthLevel(99);
        player.setDefenceLevel(98);
        player.setRangedLevel(97);
        player.setMagicLevel(96);
        player.setHitpointsLevel(95);
        player.setCurrentHitpoints(90);
        player.setAttackBoost(5);
        player.setStrengthBoost(6);
        player.setRangedBoost(4);
        player.setMagicBoost(3);
        player.setCombatStyle(CombatStyle.RANGED_RAPID);
        player.setActivePrayers(EnumSet.of(Prayer.RIGOUR));
        player.setWeaponSpeed(4);

        int[] ids = new int[14];
        String[] names = new String[14];
        String[] versions = new String[14];
        String[] categories = new String[14];
        ids[EquipmentSlot.WEAPON.getIndex()] = 20997;
        names[EquipmentSlot.WEAPON.getIndex()] = "Twisted bow";
        versions[EquipmentSlot.WEAPON.getIndex()] = "Twisted";
        categories[EquipmentSlot.WEAPON.getIndex()] = "Bow";
        ids[EquipmentSlot.AMMO.getIndex()] = 21944;
        names[EquipmentSlot.AMMO.getIndex()] = "Dragon arrows";
        categories[EquipmentSlot.AMMO.getIndex()] = "Arrow";
        player.setEquippedItemIds(ids);
        player.setEquippedItemNames(names);
        player.setEquippedItemVersions(versions);
        player.setEquippedItemCategories(categories);

        EquipmentStats stats = new EquipmentStats();
        stats.setStabAttack(15);
        stats.setSlashAttack(16);
        stats.setCrushAttack(17);
        stats.setMagicAttack(18);
        stats.setRangedAttack(19);
        stats.setMeleeStrength(20);
        stats.setRangedStrength(21);
        stats.setMagicDamage(22);
        stats.setStabDefence(23);
        stats.setSlashDefence(24);
        stats.setCrushDefence(25);
        stats.setMagicDefence(26);
        stats.setRangedDefence(27);
        stats.setPrayerBonus(28);
        player.setEquipmentStats(stats);
        return player;
    }

    private static DpsResult result(double dps, int maxHit, double accuracy, int attackRoll, int defenceRoll, int speed) {
        DpsResult result = new DpsResult();
        result.setDps(dps);
        result.setMaxHit(maxHit);
        result.setAccuracy(accuracy);
        result.setAttackRoll(attackRoll);
        result.setDefenceRoll(defenceRoll);
        result.setAttackSpeed(speed);
        result.setMonsterHp(750);
        return result;
    }
}
