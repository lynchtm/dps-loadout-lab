package com.dpscalc.scenario;

import static org.junit.Assert.*;

import com.loadoutlab.model.MonsterStats;
import com.loadoutlab.equipment.EquipmentPreparationFacade;

import org.junit.Test;

public class CalculationCoordinatorTest {
    @Test
    public void metadataReusesResultButTargetPrayerAndProfileChangesDoNot() {
        Scenario s = new Scenario();
        s.target = new MonsterStats();
        s.target.setId(-1);
        s.target.setName("Target");
        s.target.setSize(1);
        s.target.setSpeed(4);
        s.target.setHitpoints(100);
        s.loadouts.get(0).player.getEquippedItemIds()[3] = 4151;
        CalculationCoordinator cache =
                new CalculationCoordinator(
                        new ScenarioCalculator(new EquipmentPreparationFacade()));
        Scenario.Loadout l = s.loadouts.get(0);
        ScenarioCalculator.Result first = cache.calculate(l, s.target, s.encounter);
        assertNull(first.error);
        l.name = "Renamed";
        l.carry.slots[0] = new CarryPlan.Entry(385, "Shark", 1);
        ScenarioCalculator.Result renamed = cache.calculate(l, s.target, s.encounter);
        assertEquals("Renamed", renamed.name);
        assertSame(first.normal, renamed.normal);
        s.target.setDefenceLevel(300);
        ScenarioCalculator.Result target = cache.calculate(l, s.target, s.encounter);
        assertNotSame(first.normal, target.normal);
        l.player.setOnSlayerTask(!l.player.isOnSlayerTask());
        assertNotSame(target.normal, cache.calculate(l, s.target, s.encounter).normal);
        cache.clear();
        assertNotSame(first.normal, cache.calculate(l, s.target, s.encounter).normal);
    }

    @Test
    public void persistenceDeduplicatesAndRejectsAnOldProfile() {
        class Storage implements ScenarioStorage {
            int writes;
            String profile = "a";

            public long profileId() {
                return 1;
            }

            public String getRSProfileKey() {
                return profile;
            }

            public String getConfiguration(String g, String p, String k) {
                return null;
            }

            public void setConfiguration(String g, String p, String k, String v) {
                writes++;
            }
        }
        Storage storage = new Storage();
        WorkspacePersistence persistence = new WorkspacePersistence(storage);
        Scenario scenario = new Scenario();
        persistence.save(scenario, "a", 1);
        persistence.save(scenario, "a", 1);
        assertEquals(1, storage.writes);
        scenario.loadouts.get(0).name = "New";
        persistence.save(scenario, "a", 1);
        assertEquals(2, storage.writes);
        storage.profile = "b";
        persistence.save(scenario, "a", 1);
        assertEquals(2, storage.writes);
        persistence.reset();
        persistence.save(scenario, "b", 1);
        assertEquals(3, storage.writes);
    }
}
