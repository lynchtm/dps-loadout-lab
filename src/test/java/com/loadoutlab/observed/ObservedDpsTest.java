// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab.observed;

import com.loadoutlab.DpsLoadoutLabConfig;
import com.loadoutlab.observed.EncounterClient.Position;
import com.loadoutlab.observed.EncounterClient.Target;
import java.util.*;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.*;
import static net.runelite.api.gameval.NpcID.*;

public class ObservedDpsTest {
    private final FakeClient client = new FakeClient();
    private final Settings config = new Settings();
    private final ObservedDps observed = new ObservedDps(client, config);
    private final Object boss = new Object(), add = new Object();

    @Test public void ignoresOtherPlayersAndWorksWithNoCalculatorTarget() {
        Target target = target(boss, VORKATH, false);
        observed.hit(target, false, 900);
        assertNull(observed.displayed());
        observed.hit(target, true, 0);
        client.tick = 10;
        observed.hit(target, true, 30);
        assertEquals(5, observed.displayed().dps(), 0);
        assertEquals(30, observed.displayed().damage());
    }

    @Test public void deathBeforeFirstHitsplatStillProducesOneCompletedKill() {
        Target dying = target(boss, VORKATH, true);
        observed.died(dying);
        observed.hit(dying, true, 20);
        observed.despawned(dying);
        observed.tick();
        assertEquals(EncounterTracker.Status.COMPLETE, observed.displayed().status);
        assertEquals(20, observed.displayed().damage());
        assertEquals(1, client.messages.size());
        assertTrue(client.messages.get(0).contains("N/A"));
        client.tick = 20;
        observed.tick();
        assertEquals(1, client.messages.size());
    }

    @Test public void deathBeforeLastHitsplatIncludesEveryHitInFinalTick() {
        observed.hit(target(boss, VORKATH, false), true, 100);
        client.tick = 10;
        Target dead = target(boss, VORKATH, true);
        observed.died(dead);
        observed.hit(dead, true, 25);
        observed.hit(dead, true, 15);
        observed.tick();
        assertEquals(140, observed.displayed().damage());
        assertEquals(6, observed.displayed().seconds(), 0);
        assertTrue(client.messages.get(0).contains("140 damage"));
    }

    @Test public void sceneUnloadIsInterruptedAndNeverAnnouncedAsKill() {
        Target goblin = target(boss, 999999, false);
        observed.hit(goblin, true, 10);
        observed.despawned(goblin);
        client.tick = 5;
        client.targets.clear();
        observed.tick();
        assertEquals(EncounterTracker.Status.INTERRUPTED, observed.displayed().status);
        assertTrue(client.messages.isEmpty());
    }

    @Test public void phaseDisappearanceKeepsElapsedTimeAndAcceptsReplacementActor() {
        Target first = target(boss, SNAKEBOSS_BOSS_RANGED, false);
        observed.hit(first, true, 100);
        observed.despawned(first);
        client.tick = 5;
        observed.tick();
        client.tick = 30;
        Target next = target(add, SNAKEBOSS_BOSS_MAGIC, false);
        client.targets.add(next);
        observed.hit(next, true, 100);
        observed.died(target(add, SNAKEBOSS_BOSS_MAGIC, true));
        observed.tick();
        assertEquals(200, observed.displayed().damage());
        assertEquals(18, observed.displayed().seconds(), 0);
        assertEquals(1, client.messages.size());
    }

    @Test public void phaseGraceExpiresAsInterruption() {
        observed.hit(target(boss, SNAKEBOSS_BOSS_RANGED, false), true, 100);
        client.tick = 1;
        observed.tick();
        client.tick = 51;
        observed.tick();
        assertEquals(EncounterTracker.Status.INTERRUPTED, observed.displayed().status);
        assertEquals("Target lost", observed.displayed().reason);
        assertTrue(client.messages.isEmpty());
    }

    @Test public void terminalTransformCompletesEvenWithoutActorDeath() {
        Target first = target(boss, HYDRABOSS, false);
        client.targets.add(first);
        observed.hit(first, true, 100);
        client.tick = 20;
        Target dead = target(boss, HYDRABOSS_FINALDEATH, true);
        client.targets.set(0, dead);
        observed.changed(dead);
        observed.tick();
        assertEquals(EncounterTracker.Status.COMPLETE, observed.displayed().status);
        assertEquals(12, observed.displayed().seconds(), 0);
    }

    @Test public void oldCorpseCannotFinishNewEncounter() {
        Target corpse = target(new Object(), HYDRABOSS_FINALDEATH, true);
        client.targets.add(corpse);
        Target alive = target(boss, HYDRABOSS, false);
        client.targets.add(alive);
        observed.hit(alive, true, 10);
        observed.changed(corpse);
        client.tick = 1;
        observed.tick();
        assertEquals(EncounterTracker.Status.ACTIVE, observed.displayed().status);
        assertTrue(client.messages.isEmpty());
    }

    @Test public void terminalBossFinishedByTeammateIsObservedWhileHittingAdd() {
        Target nex = target(boss, NEX, false);
        client.targets.add(nex);
        observed.hit(nex, true, 10);
        client.tick = 20;
        observed.hit(target(add, NEX_ICEMAGE, false), true, 30);
        Target dead = target(boss, NEX_DYING, true);
        observed.changed(dead);
        observed.tick();
        assertEquals(40, observed.displayed().damage());
        assertEquals(1, client.messages.size());
    }

    @Test public void addsDoNotStartEncountersAndNearbyUnrelatedNpcDoesNotResetBoss() {
        observed.hit(target(add, VORKATH_SPAWN, false), true, 38);
        assertNull(observed.displayed());
        observed.hit(target(boss, VORKATH, false), true, 100);
        observed.hit(target(new Object(), 999999, false), true, 900);
        assertEquals(100, observed.displayed().damage());
    }

    @Test public void requiredAddsCanStartWhenTheirLiveBossIsPresent() {
        client.targets.add(target(boss, ABYSSALSIRE_SIRE_STASIS_AWAKE, false));
        observed.hit(target(add, ABYSSALSIRE_LUNG, false), true, 40);
        assertEquals("Abyssal Sire", observed.displayed().name);
        assertEquals(40, observed.displayed().addDamage);
        client.tick = 30;
        observed.hit(target(boss, ABYSSALSIRE_SIRE_APOCALYPSE, false), true, 100);
        observed.died(target(boss, ABYSSALSIRE_SIRE_APOCALYPSE, true));
        observed.tick();
        assertEquals(140, observed.displayed().damage());
        assertEquals(18, observed.displayed().seconds(), 0);
    }

    @Test public void leavingOrChangingWorldViewInterruptsAndReturningStartsFresh() {
        Target first = target(boss, VORKATH, false);
        client.targets.add(first);
        observed.hit(first, true, 100);
        client.tick = 10;
        client.player = new Position(new WorldPoint(3200, 3200, 0), 2);
        observed.tick();
        assertEquals(EncounterTracker.Status.INTERRUPTED, observed.displayed().status);
        assertTrue(client.messages.isEmpty());
        client.player = first.position;
        client.tick = 50;
        observed.hit(first, true, 20);
        assertEquals(20, observed.displayed().damage());
    }

    @Test public void distantBossDeathCannotCompleteLocalFight() {
        observed.hit(target(boss, HYDRABOSS, false), true, 10);
        Target remote = new Target(add, HYDRABOSS_FINALDEATH, "Other Hydra",
                new Position(new WorldPoint(3300, 3200, 0), 0), true);
        observed.changed(remote);
        observed.died(remote);
        observed.tick();
        assertEquals(EncounterTracker.Status.ACTIVE, observed.displayed().status);
        assertTrue(client.messages.isEmpty());
    }

    @Test public void logoutAndHopClearAndConnectionLossInterrupts() {
        for (GameState state : Arrays.asList(GameState.LOGIN_SCREEN, GameState.HOPPING)) {
            observed.hit(target(boss, VORKATH, false), true, 20);
            observed.onGameState(state);
            assertNull(observed.displayed());
        }
        observed.hit(target(boss, VORKATH, false), true, 20);
        observed.onGameState(GameState.CONNECTION_LOST);
        assertEquals(EncounterTracker.Status.INTERRUPTED, observed.displayed().status);
        assertTrue(client.messages.isEmpty());
    }

    @Test public void playerDeathIsNotABossKill() {
        observed.hit(target(boss, VORKATH, false), true, 100);
        client.tick = 10;
        observed.playerDied();
        observed.tick();
        assertEquals(EncounterTracker.Status.INTERRUPTED, observed.displayed().status);
        assertEquals("You died", observed.displayed().reason);
        assertTrue(client.messages.isEmpty());
    }

    @Test public void disablingTrackingClearsAndChatCanRunWithoutOverlay() {
        observed.hit(target(boss, VORKATH, false), true, 100);
        config.show = config.chat = false;
        observed.tick();
        config.chat = true;
        observed.hit(target(boss, VORKATH, false), true, 20);
        client.tick = 10;
        observed.died(target(boss, VORKATH, true));
        observed.tick();
        assertNull(observed.displayed());
        assertEquals(1, client.messages.size());
        assertTrue(client.messages.get(0).contains("20 damage"));
    }

    @Test public void chatOptOutAndDisplayTimeoutDoNotAffectFrozenDps() {
        config.chat = false;
        observed.hit(target(boss, VORKATH, false), true, 100);
        client.tick = 10;
        observed.died(target(boss, VORKATH, true));
        observed.tick();
        double dps = observed.displayed().dps();
        client.tick = 1000;
        observed.tick();
        assertNull(observed.displayed());
        config.keep = true;
        assertEquals(dps, observed.displayed().dps(), 0);
        assertTrue(client.messages.isEmpty());
    }

    @Test public void inactivityGuardAbandonsRatherThanSubtractingTimeFromAKill() {
        Target npc = target(boss, VORKATH, false);
        client.targets.add(npc);
        observed.hit(npc, true, 100);
        client.tick = 199;
        observed.tick();
        assertEquals(119.4, observed.displayed().seconds(), 0.0001);
        client.tick = 200;
        observed.tick();
        assertEquals(EncounterTracker.Status.INTERRUPTED, observed.displayed().status);
        assertTrue(client.messages.isEmpty());
    }

    private Target target(Object actor, int id, boolean dead) {
        return new Target(actor, id, "NPC", new Position(new WorldPoint(3200, 3200, 0), 0), dead);
    }

    private static final class Settings implements DpsLoadoutLabConfig {
        boolean show = true, chat = true, keep;
        public boolean showActualDps() { return show; }
        public boolean observedChatSummary() { return chat; }
        public boolean alwaysShowOverlay() { return keep; }
    }

    private static final class FakeClient implements EncounterClient {
        int tick;
        Position player = new Position(new WorldPoint(3200, 3200, 0), 0);
        final List<Target> targets = new ArrayList<>();
        final List<String> messages = new ArrayList<>();
        public int tick() { return tick; }
        public GameState state() { return GameState.LOGGED_IN; }
        public Position player() { return player; }
        public List<Target> targets() { return targets; }
        public void chat(String message) { messages.add(message); }
    }
}
