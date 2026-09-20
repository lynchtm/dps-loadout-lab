// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab.observed;

import org.junit.Test;
import static org.junit.Assert.*;
import static net.runelite.api.gameval.NpcID.*;

public class EncounterTrackerTest {
    private final EncounterTracker tracker = new EncounterTracker();
    private final Object boss = new Object(), add = new Object();

    @Test public void freezesAtDeathAndExcludesAllTimeBetweenKills() {
        hit(boss, VORKATH, 150, 10);
        hit(boss, VORKATH, 150, 110);
        tracker.death(boss, VORKATH, 110);
        tracker.endTick();
        EncounterTracker.Result first = tracker.result(1010);
        assertEquals(300, first.damage());
        assertEquals(60, first.seconds(), 0);
        assertEquals(5, first.dps(), 0);
        assertEquals(EncounterTracker.Status.COMPLETE, first.status);
        Object next = new Object();
        hit(next, VORKATH, 120, 1010);
        tracker.death(next, VORKATH, 1060);
        tracker.endTick();
        assertEquals(4, tracker.result(9000).dps(), 0);
        assertEquals(120, tracker.result(9000).damage());
    }

    @Test public void zeroHitsStartClockAndMechanicsAndEatingRemainInDenominator() {
        hit(boss, HESPORI, 0, 100);
        hit(add, HESPORI_HEALER_ACTIVE, 10, 120);
        hit(boss, HESPORI, 50, 200);
        tracker.death(add, HESPORI_HEALER_ACTIVE, 200);
        assertTrue(tracker.isActive());
        tracker.death(boss, HESPORI, 300);
        tracker.endTick();
        assertEquals(120, tracker.result(1000).seconds(), 0);
        assertEquals(0.5, tracker.result(1000).dps(), 0);
        assertEquals(50, tracker.result(1000).bossDamage);
        assertEquals(10, tracker.result(1000).addDamage);
    }

    @Test public void kalphiteQueenFirstDeathIsOnlyATransition() {
        hit(boss, KALPHITE_QUEEN, 255, 10);
        tracker.death(boss, KALPHITE_QUEEN, 20);
        tracker.endTick();
        assertTrue(tracker.isActive());
        tracker.changed(boss, KALPHITE_FLYINGQUEEN, 25);
        hit(boss, KALPHITE_FLYINGQUEEN, 255, 40);
        tracker.death(boss, KALPHITE_FLYINGQUEEN, 40);
        tracker.endTick();
        assertEquals(510, tracker.result(50).damage());
        assertEquals(18, tracker.result(50).seconds(), 0);
    }

    @Test public void guardiansFinishOnlyAtFinalDuskAndCountBothBosses() {
        hit(boss, GARGBOSS_DAWN_PHASE1, 100, 0);
        hit(add, GARGBOSS_DUSK_PHASE2_ATTACKING, 100, 30);
        tracker.death(boss, GARGBOSS_DAWN_PHASE3, 50);
        tracker.changed(boss, GARGBOSS_DAWN_DEATH, 50);
        tracker.endTick();
        assertTrue(tracker.isActive());
        hit(add, GARGBOSS_DUSK_PHASE4, 100, 60);
        tracker.changed(add, GARGBOSS_DUSK_DEATH, 61);
        tracker.endTick();
        assertEquals(300, tracker.result(500).bossDamage);
        assertEquals(0, tracker.result(500).addDamage);
        assertEquals(EncounterTracker.Status.COMPLETE, tracker.result(500).status);
    }

    @Test public void hydraFinalFormCompletesWithoutDespawnAndDoesNotLoseKillingHit() {
        hit(boss, HYDRABOSS, 100, 20);
        tracker.changed(boss, HYDRABOSS_P1_TRANSITION, 30);
        tracker.death(boss, HYDRABOSS_3, 40);
        tracker.endTick();
        assertTrue(tracker.isActive());
        tracker.changed(boss, HYDRABOSS_FINALDEATH, 80);
        hit(boss, HYDRABOSS_FINALDEATH, 90, 80);
        tracker.endTick();
        assertEquals(190, tracker.result(800).damage());
        assertEquals(36, tracker.result(800).seconds(), 0);
    }

    @Test public void newNpcObjectDuringZulrahPhaseKeepsOneEncounter() {
        hit(boss, SNAKEBOSS_BOSS_RANGED, 100, 10);
        Object nextForm = new Object();
        hit(nextForm, SNAKEBOSS_BOSS_MAGIC, 100, 70);
        tracker.death(nextForm, SNAKEBOSS_BOSS_MAGIC, 90);
        tracker.endTick();
        assertEquals(200, tracker.result(90).damage());
        assertEquals(48, tracker.result(90).seconds(), 0);
    }

    @Test public void vetionAndCalvarionAreSeparateAndHoundsCannotFinish() {
        hit(boss, VETION_SINGLE, 100, 0);
        hit(add, VETION_HELLHOUND_JNR_SINGLES, 30, 10);
        tracker.death(add, VETION_HELLHOUND_JNR_SINGLES, 10);
        tracker.death(boss, VETION_SINGLE, 20);
        hit(new Object(), VETION, 400, 30);
        tracker.endTick();
        assertTrue(tracker.isActive());
        hit(boss, VETION_2_SINGLE, 100, 40);
        tracker.death(boss, VETION_2_SINGLE, 40);
        tracker.endTick();
        assertEquals("Calvar'ion", tracker.result(80).name);
        assertEquals(230, tracker.result(80).damage());
    }

    @Test public void teammateCanFinishBossAfterWeSwitchToAdd() {
        hit(boss, NEX, 20, 0);
        Object finalBoss = new Object();
        tracker.observe(finalBoss, NEX_DEFLECT);
        hit(add, NEX_ICEMAGE, 50, 30);
        tracker.changed(finalBoss, NEX_DYING, 50);
        tracker.endTick();
        assertEquals(70, tracker.result(60).damage());
        assertEquals(EncounterTracker.Status.COMPLETE, tracker.result(60).status);
    }

    @Test public void addsAfterBossDeathCannotStartAnotherEncounter() {
        hit(boss, GODWARS_BANDOS_AVATAR, 100, 0);
        tracker.death(boss, GODWARS_BANDOS_AVATAR, 10);
        tracker.endTick();
        hit(add, GODWARS_SERGEANT_GOBLIN1, 50, 11);
        assertFalse(tracker.isActive());
        assertEquals(100, tracker.result(30).damage());
    }

    @Test public void unrelatedDamageAndNamesDoNotContaminateBoss() {
        hit(boss, VORKATH, 100, 0);
        tracker.hit(new Object(), 999999, "Vorkath", 999, 5);
        hit(add, VORKATH_SPAWN, 38, 10);
        assertEquals(138, tracker.result(10).damage());
        assertEquals("Vorkath", tracker.result(10).name);
    }

    @Test public void duplicateDeathAndDespawnProduceExactlyOneSummary() {
        hit(boss, VORKATH, 30, 1);
        tracker.death(boss, VORKATH, 10);
        tracker.death(boss, VORKATH, 12);
        tracker.endTick();
        tracker.death(boss, VORKATH, 15);
        tracker.endTick();
        assertEquals(10, tracker.pollCompleted().endTick);
        assertNull(tracker.pollCompleted());
    }

    @Test public void interruptionsAreFrozenAndNeverAnnouncedAsKills() {
        hit(boss, VORKATH, 100, 0);
        tracker.interrupt(10, "Left encounter");
        assertEquals(EncounterTracker.Status.INTERRUPTED, tracker.result(1000).status);
        assertEquals(6, tracker.result(1000).seconds(), 0);
        assertNull(tracker.pollCompleted());
        hit(boss, VORKATH, 25, 500);
        assertEquals(25, tracker.result(501).damage());
    }

    @Test public void sleepingVorkathResetsButDoesNotOverrideAlreadyConfirmedDeath() {
        hit(boss, VORKATH, 10, 0);
        tracker.changed(boss, VORKATH_SLEEPING, 10);
        assertEquals(EncounterTracker.Status.INTERRUPTED, tracker.result(10).status);
        hit(boss, VORKATH, 10, 100);
        tracker.death(boss, VORKATH, 110);
        tracker.changed(boss, VORKATH_SLEEPING, 110);
        tracker.endTick();
        assertEquals(EncounterTracker.Status.COMPLETE, tracker.result(110).status);
    }

    @Test public void genericNpcUsesObjectIdentityAndOneTickKillHasNoInventedDps() {
        tracker.hit(boss, 999999, "Goblin", 5, 10);
        tracker.death(new Object(), 999999, 20);
        assertTrue(tracker.isActive());
        tracker.hit(add, 999999, "Goblin", 7, 30);
        tracker.death(add, 999999, 30);
        tracker.endTick();
        assertEquals(7, tracker.result(900).damage());
        assertFalse(tracker.result(900).encounter);
        assertTrue(Double.isNaN(tracker.result(900).dps()));
        assertTrue(ObservedDps.summary(tracker.result(900)).contains("too short to measure"));
    }

    @Test public void resetClearsResultAndPendingNotifications() {
        hit(boss, VORKATH, 20, 0);
        tracker.death(boss, VORKATH, 10);
        tracker.endTick();
        tracker.reset();
        assertNull(tracker.result(1000));
        assertNull(tracker.pollCompleted());
    }

    @Test public void summaryExplainsDamageScopeAndTiming() {
        hit(boss, VORKATH, 750, 0);
        hit(add, VORKATH_SPAWN, 38, 50);
        tracker.death(boss, VORKATH, 100);
        tracker.endTick();
        String summary = ObservedDps.summary(tracker.result(100));
        assertTrue(summary.contains("13.13 encounter DPS"));
        assertTrue(summary.contains("750 boss + 38 adds"));
        assertTrue(summary.contains("60.0s since first hit"));
    }

    @Test public void allSupportedPhaseFamiliesResolveToSameDefinition() {
        int[][] groups = {
            {VORKATH, VORKATH_SPAWN}, {HYDRABOSS, HYDRABOSS_4, HYDRABOSS_FINALDEATH},
            {VETION, VETION_2, VETION_HELLHOUND_SNR},
            {ABYSSALSIRE_SIRE_STASIS_AWAKE, ABYSSALSIRE_LUNG, ABYSSALSIRE_SIRE_APOCALYPSE},
            {MUSPAH, MUSPAH_MELEE, MUSPAH_SOULSPLIT, MUSPAH_TELEPORT, MUSPAH_FINAL}
        };
        for (int[] group : groups)
            for (int id : group) assertSame(EncounterDefinition.forNpc(group[0]), EncounterDefinition.forNpc(id));
        assertNull(EncounterDefinition.forNpc(VORKATH_QUEST));
        assertNull(EncounterDefinition.forNpc(POH_MOUNTED_VORKATH));
    }

    private void hit(Object actor, int id, int damage, int tick) {
        tracker.hit(actor, id, "Test NPC", damage, tick);
    }
}
