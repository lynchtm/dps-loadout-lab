// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab.data;

import static org.junit.Assert.*;

import com.dpscalc.scenario.*;
import com.loadoutlab.equipment.*;
import com.loadoutlab.model.*;

import org.junit.Test;

import java.util.*;

public class FactualCatalogTest {
    @Test
    public void defaultTargetVariantIsNotAlphabeticallyFirstEnragedVersion() {
        MonsterDataManager data = new MonsterDataManager();
        MonsterStats m = data.getMonster(13668);
        assertEquals("In combat", m.getVersion());
        assertEquals(135, m.getDefenceLevel());
        assertEquals(170, data.getMonster(13668, "Enraged").getDefenceLevel());
        m.setDefenceLevel(0);
        assertEquals(135, data.getMonster(13668).getDefenceLevel());
    }

    @Test
    public void catalogHasValidCombatInputsForThousandsOfTargets() {
        List<MonsterStats> data = new MonsterDataManager().getAllMonsters();
        assertTrue(data.size() > 4000);
        for (MonsterStats m : data) {
            assertTrue(m.toString(), m.getHitpoints() > 0);
            assertTrue(m.toString(), m.getSize() > 0);
            assertNotNull(m.getAttributes());
            assertNotNull(m.getInputs());
        }
    }

    @Test
    public void equipmentPreparesExactVariantAndDoesNotCountUnusedAmmoStrength() {
        EquipmentPreparationFacade facts = new EquipmentPreparationFacade();
        PlayerState p = Scenario.defaults();
        int[] ids = p.getEquippedItemIds();
        ids[3] = 4151;
        ids[13] = 892;
        facts.prepare(p, ids, null, EquipmentPreparationFacade.context(p, 0));
        assertEquals(82, p.getEquipmentStats().getMeleeStrength());
        assertEquals(0, p.getEquipmentStats().getRangedStrength());
        assertEquals(AmmoApplicability.ALLOWED, p.getAmmoApplicability());
    }

    @Test
    public void dragonHunterCrossbowUsesRegularBoltsNotHuntersKebbitBolts() {
        EquipmentPreparationFacade facts = new EquipmentPreparationFacade();
        PlayerState p = Scenario.defaults();
        p.setCombatStyle(CombatStyle.RANGED_RAPID);
        int[] ids = p.getEquippedItemIds();
        ids[3] = 21012;
        ids[13] = 9244;
        facts.prepare(p, ids, null, EquipmentPreparationFacade.context(p, 0));
        assertEquals(AmmoApplicability.INCLUDED, p.getAmmoApplicability());
        assertTrue(p.getEquipmentStats().getRangedStrength() > 0);
    }

    @Test
    public void oldPersistedFieldNamesRetainMeaningWithoutOldJavaTypes() {
        String json =
                "{\"schemaVersion\":1,\"loadouts\":[{\"name\":\"Saved"
                    + " build\",\"player\":{\"attackLevel\":75,\"magicLevel\":82,\"onSlayerTask\":true,\"activePrayers\":[\"PIETY\"],\"currentHitpoints\":80}}]}";
        Scenario saved = Scenario.parse(json);
        assertEquals("Saved build", saved.loadouts.get(0).name);
        assertEquals(75, saved.loadouts.get(0).player.getAttackLevel());
        assertTrue(saved.loadouts.get(0).player.getActivePrayers().contains(Prayer.PIETY));
        Scenario copy = Scenario.parse(Scenario.JSON.toJson(saved));
        assertEquals(80, copy.loadouts.get(0).player.getCurrentHitpoints());
        assertEquals(82, copy.loadouts.get(0).player.getMagicLevel());
    }

    @Test
    public void unknownWeaponCannotBecomeAValidOptimizerStyle() {
        EquipmentPreparationFacade facts = new EquipmentPreparationFacade();
        PlayerState p = Scenario.defaults();
        p.getEquippedItemIds()[3] = 999999;
        assertTrue(WeaponStyles.available(p, facts).isEmpty());
    }
}
