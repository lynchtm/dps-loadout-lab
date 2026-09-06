package com.dpscalc;

import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.data.WeaknessElement;
import org.junit.Test;

import static org.junit.Assert.*;

public class MonsterStatsCopyTest {

    @Test
    public void copyKeepsScalarFields_whenSourceHasValues() {
        // Given
        MonsterStats source = sourceMonster();

        // When
        MonsterStats copy = source.copy();

        // Then
        assertNotSame(source, copy);
        assertEquals(123, copy.getId());
        assertEquals("Great Olm", copy.getName());
        assertEquals("Left claw", copy.getVersion());
        assertEquals(5, copy.getSize());
        assertEquals(4, copy.getSpeed());
        assertEquals(250, copy.getAttackLevel());
        assertEquals(251, copy.getStrengthLevel());
        assertEquals(252, copy.getDefenceLevel());
        assertEquals(253, copy.getHitpoints());
        assertEquals(254, copy.getMagicLevel());
        assertEquals(255, copy.getRangedLevel());
        assertEquals(10, copy.getStabDefence());
        assertEquals(11, copy.getSlashDefence());
        assertEquals(12, copy.getCrushDefence());
        assertEquals(13, copy.getMagicDefence());
        assertEquals(14, copy.getLightRangedDefence());
        assertEquals(15, copy.getStandardRangedDefence());
        assertEquals(16, copy.getHeavyRangedDefence());
        assertEquals(17, copy.getFlatArmour());
        assertEquals(18, copy.getOffensiveMagic());
        assertEquals(WeaknessElement.FIRE, copy.getWeaknessElement());
        assertEquals(40, copy.getWeaknessSeverity());
    }

    @Test
    public void copyHasIndependentAttributes_whenCopyIsMutated() {
        // Given
        MonsterStats source = sourceMonster();
        MonsterStats copy = source.copy();

        // When
        copy.addAttribute(MonsterAttribute.DRAGON);
        copy.getAttributes().remove(MonsterAttribute.UNDEAD);

        // Then
        assertTrue(source.hasAttribute(MonsterAttribute.UNDEAD));
        assertFalse(source.hasAttribute(MonsterAttribute.DRAGON));
        assertFalse(copy.hasAttribute(MonsterAttribute.UNDEAD));
        assertTrue(copy.hasAttribute(MonsterAttribute.DRAGON));
    }

    @Test
    public void copyHasIndependentInputs_whenCopyInputsAreMutated() {
        // Given
        MonsterStats source = sourceMonster();
        MonsterStats copy = source.copy();

        // When
        copy.getInputs().setToaInvocationLevel(450);
        copy.getInputs().setPhase("enraged");
        copy.getInputs().setPartySize(8);

        // Then
        assertEquals(300, source.getInputs().getToaInvocationLevel());
        assertEquals("normal", source.getInputs().getPhase());
        assertEquals(3, source.getInputs().getPartySize());
        assertEquals(450, copy.getInputs().getToaInvocationLevel());
        assertEquals("enraged", copy.getInputs().getPhase());
        assertEquals(8, copy.getInputs().getPartySize());
    }

    @Test
    public void copyHasIndependentDefenceReductions_whenCopyReductionsAreMutated() {
        // Given
        MonsterStats source = sourceMonster();
        MonsterStats copy = source.copy();

        // When
        copy.getInputs().getDefenceReductions().setDwh(2);
        copy.getInputs().getDefenceReductions().setBgs(64);

        // Then
        assertEquals(1, source.getInputs().getDefenceReductions().getDwh());
        assertEquals(32, source.getInputs().getDefenceReductions().getBgs());
        assertEquals(2, copy.getInputs().getDefenceReductions().getDwh());
        assertEquals(64, copy.getInputs().getDefenceReductions().getBgs());
    }

    @Test
    public void copyUsesDefaultInputs_whenSourceWasSetToNullInputs() {
        // Given
        MonsterStats source = sourceMonster();
        source.setInputs(null);

        // When
        MonsterStats copy = source.copy();

        // Then
        assertNotNull(source.getInputs());
        assertNotNull(copy.getInputs());
        assertNotSame(source.getInputs(), copy.getInputs());
        copy.getInputs().setPartySize(4);
        assertEquals(1, source.getInputs().getPartySize());
        assertEquals(4, copy.getInputs().getPartySize());
    }

    private static MonsterStats sourceMonster() {
        MonsterStats stats = new MonsterStats();
        stats.setId(123);
        stats.setName("Great Olm");
        stats.setVersion("Left claw");
        stats.setSize(5);
        stats.setSpeed(4);
        stats.setAttackLevel(250);
        stats.setStrengthLevel(251);
        stats.setDefenceLevel(252);
        stats.setHitpoints(253);
        stats.setMagicLevel(254);
        stats.setRangedLevel(255);
        stats.setStabDefence(10);
        stats.setSlashDefence(11);
        stats.setCrushDefence(12);
        stats.setMagicDefence(13);
        stats.setLightRangedDefence(14);
        stats.setStandardRangedDefence(15);
        stats.setHeavyRangedDefence(16);
        stats.setFlatArmour(17);
        stats.setOffensiveMagic(18);
        stats.addAttribute(MonsterAttribute.UNDEAD);
        stats.setWeaknessElement(WeaknessElement.FIRE);
        stats.setWeaknessSeverity(40);

        MonsterInputs inputs = new MonsterInputs();
        inputs.setToaInvocationLevel(300);
        inputs.setToaPathLevel(2);
        inputs.setMonsterCurrentHp(200);
        inputs.setPhase("normal");
        inputs.setFromCoxCm(true);
        inputs.setPartyMaxCombatLevel(126);
        inputs.setPartySumMiningLevel(297);
        inputs.setPartyMaxHpLevel(99);
        inputs.setPartySize(3);
        inputs.setDemonbaneVulnerability(120);
        inputs.getDefenceReductions().setDwh(1);
        inputs.getDefenceReductions().setBgs(32);
        stats.setInputs(inputs);

        return stats;
    }
}
