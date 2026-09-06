package com.dpscalc;

import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterConstants;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterScaling;
import com.dpscalc.data.MonsterStats;
import org.junit.Test;

import static org.junit.Assert.*;

public class MonsterScalingTest {

    @Test
    public void scaleCopiesToaHpParityCases_whenMonsterIsFromTombsOfAmascut() {
        assertScaledHp(MonsterConstants.BABA_IDS[0], 380, 150, 0, 1, 610);
        assertScaledHp(MonsterConstants.BABA_IDS[0], 380, 300, 2, 1, 940);
        assertScaledHp(MonsterConstants.BABA_IDS[0], 380, 450, 6, 1, 1420);
        assertScaledHp(MonsterConstants.ZEBAK_IDS[0], 580, 350, 2, 1, 1570);
        assertScaledHp(MonsterConstants.TOA_WARDEN_CORE_EJECTED_IDS[0], 0, 300, 0, 1, 5850);
        assertScaledHp(MonsterConstants.TOA_WARDEN_CORE_EJECTED_IDS[0], 0, 500, 0, 1, 6750);
        assertScaledHp(MonsterConstants.TOA_OBELISK_IDS[0], 260, 300, 0, 1, 570);
    }

    @Test
    public void scaleLeavesHpUnchanged_whenMonsterIsNotFromTombsOfAmascut() {
        MonsterStats source = monster(123, 321, 500, 6, 8);

        MonsterStats scaled = MonsterScaling.scale(source);

        assertNotSame(source, scaled);
        assertEquals(321, scaled.getHitpoints());
        assertEquals(321, source.getHitpoints());
    }

    @Test
    public void scaleDoesNotMutateSource_whenMonsterHpIsScaled() {
        MonsterStats source = monster(MonsterConstants.BABA_IDS[0], 380, 300, 2, 1);

        MonsterStats scaled = MonsterScaling.scale(source);

        assertNotSame(source, scaled);
        assertEquals(380, source.getHitpoints());
        assertEquals(940, scaled.getHitpoints());
    }

    @Test
    public void scaleLeavesStatsUnchanged_whenMonsterIsNotXerician() {
        MonsterStats source = new MonsterStats();
        source.setId(123);
        source.setHitpoints(300);
        source.setAttackLevel(390);
        source.setDefenceLevel(205);
        source.setStrengthLevel(390);
        source.setMagicLevel(205);
        source.setRangedLevel(1);
        source.setInputs(new MonsterInputs());
        source.getInputs().setPartySize(2);

        MonsterStats scaled = MonsterScaling.scale(source);

        assertNotSame(source, scaled);
        assertCoxStats(scaled, 300, 390, 205, 390, 205, 1);
        assertCoxStats(source, 300, 390, 205, 390, 205, 1);
    }

    @Test
    public void scaleLeavesStatsUnchanged_whenXericianUsesDefaultInputs() {
        MonsterStats source = tekton(MonsterConstants.TEKTON_IDS[0]);

        MonsterStats scaled = MonsterScaling.scale(source);

        assertNotSame(source, scaled);
        assertCoxStats(scaled, 300, 390, 205, 390, 205, 1);
        assertCoxStats(source, 300, 390, 205, 390, 205, 1);
    }

    @Test
    public void scaleCopiesRegularTektonStats_whenPartySizeIsTwo() {
        MonsterStats source = tekton(MonsterConstants.TEKTON_IDS[0]);
        source.getInputs().setPartySize(2);

        MonsterStats scaled = MonsterScaling.scale(source);

        assertCoxStats(scaled, 600, 421, 207, 421, 207, 1);
        assertCoxStats(source, 300, 390, 205, 390, 205, 1);
    }

    @Test
    public void scaleCopiesChallengeModeTektonStats_whenPartySizeIsFour() {
        MonsterStats source = tekton(MonsterConstants.TEKTON_IDS[2]);
        source.getInputs().setPartySize(4);
        source.getInputs().setFromCoxCm(true);

        MonsterStats scaled = MonsterScaling.scale(source);

        assertCoxStats(scaled, 1350, 643, 284, 643, 284, 1);
        assertCoxStats(source, 300, 390, 205, 390, 205, 1);
    }

    @Test
    public void scaleCopiesRegularGuardianStats_whenPartySizeIsSixAndMiningSumIsNinetyThree() {
        MonsterStats source = coxMonster(MonsterConstants.GUARDIAN_IDS[0], 250, 140, 100, 140, 1, 1);
        source.getInputs().setPartySize(6);
        source.getInputs().setPartySumMiningLevel(93);

        MonsterStats scaled = MonsterScaling.scale(source);

        assertCoxStats(scaled, 664, 166, 105, 166, 1, 1);
        assertCoxStats(source, 250, 140, 100, 140, 1, 1);
    }

    @Test
    public void scaleCopiesRegularOlmMageHandStats_whenPartySizeIsFour() {
        MonsterStats source = coxMonster(MonsterConstants.OLM_MAGE_HAND_IDS[0], 600, 250, 175, 250, 87, 250);
        source.getInputs().setPartySize(4);

        MonsterStats scaled = MonsterScaling.scale(source);

        assertCoxStats(scaled, 1500, 275, 180, 275, 90, 275);
        assertCoxStats(source, 600, 250, 175, 250, 87, 250);
    }

    private static void assertScaledHp(int id, int baseHp, int raidLevel, int pathLevel, int partySize, int expectedHp) {
        MonsterStats scaled = MonsterScaling.scale(monster(id, baseHp, raidLevel, pathLevel, partySize));

        assertEquals(expectedHp, scaled.getHitpoints());
    }

    private static MonsterStats monster(int id, int hitpoints, int raidLevel, int pathLevel, int partySize) {
        MonsterStats stats = new MonsterStats();
        stats.setId(id);
        stats.setHitpoints(hitpoints);

        MonsterInputs inputs = new MonsterInputs();
        inputs.setToaInvocationLevel(raidLevel);
        inputs.setToaPathLevel(pathLevel);
        inputs.setPartySize(partySize);
        stats.setInputs(inputs);

        return stats;
    }

    private static MonsterStats tekton(int id) {
        return coxMonster(id, 300, 390, 205, 390, 205, 1);
    }

    private static MonsterStats coxMonster(int id, int hitpoints, int attack, int defence, int strength, int magic, int ranged) {
        MonsterStats stats = new MonsterStats();
        stats.setId(id);
        stats.addAttribute(MonsterAttribute.XERICIAN);
        stats.setHitpoints(hitpoints);
        stats.setAttackLevel(attack);
        stats.setDefenceLevel(defence);
        stats.setStrengthLevel(strength);
        stats.setMagicLevel(magic);
        stats.setRangedLevel(ranged);
        stats.setInputs(new MonsterInputs());
        return stats;
    }

    private static void assertCoxStats(
        MonsterStats stats,
        int hitpoints,
        int attack,
        int defence,
        int strength,
        int magic,
        int ranged
    ) {
        assertEquals(hitpoints, stats.getHitpoints());
        assertEquals(attack, stats.getAttackLevel());
        assertEquals(defence, stats.getDefenceLevel());
        assertEquals(strength, stats.getStrengthLevel());
        assertEquals(magic, stats.getMagicLevel());
        assertEquals(ranged, stats.getRangedLevel());
    }
}
