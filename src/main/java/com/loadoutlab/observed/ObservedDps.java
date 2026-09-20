// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab.observed;

import com.loadoutlab.DpsLoadoutLabConfig;
import java.util.*;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.game.NpcUtil;
import com.loadoutlab.observed.EncounterClient.Position;
import com.loadoutlab.observed.EncounterClient.Target;

/** RuneLite event adapter. All mutations and chat insertion run on the client thread. */
public final class ObservedDps {
    private static final int PHASE_GRACE_TICKS = 50;
    private static final int ABANDON_TICKS = 200;
    private final EncounterClient client;
    private final DpsLoadoutLabConfig config;
    private final EncounterTracker tracker = new EncounterTracker();
    private final Set<Object> deathsThisTick = Collections.newSetFromMap(new IdentityHashMap<>());
    private Position origin;
    private int missingSince = -1;
    private volatile EncounterTracker.Result displayed;

    @Inject
    public ObservedDps(Client client, DpsLoadoutLabConfig config, NpcUtil npcUtil) {
        this(new EncounterClient.RuneLite(client, npcUtil), config);
    }

    ObservedDps(EncounterClient client, DpsLoadoutLabConfig config) {
        this.client = client;
        this.config = config;
    }

    public boolean enabled() { return config.showActualDps() || config.observedChatSummary(); }

    public void reset() {
        tracker.reset();
        deathsThisTick.clear();
        displayed = null;
        origin = null;
        missingSince = -1;
    }

    public void onHitsplat(HitsplatApplied event) {
        if (!enabled() || !(event.getActor() instanceof NPC)) return;
        hit(((EncounterClient.RuneLite) client).target((NPC) event.getActor()),
                event.getHitsplat().isMine(), event.getHitsplat().getAmount());
    }

    void hit(Target npc, boolean mine, int damage) {
        if (!enabled() || !mine || client.state() != GameState.LOGGED_IN || npc.position == null) return;
        int tick = client.tick();
        if (tracker.isActive() && !near(npc)) {
            tracker.interrupt(tick, "Left encounter");
            origin = null;
        }
        boolean wasActive = tracker.isActive();
        EncounterDefinition definition = EncounterDefinition.forNpc(npc.id);
        if (!wasActive && definition != null && definition.isAdd(npc.id)) {
            // Required adds can be attacked first, but post-kill cleanup must not open a fight.
            for (Target candidate : client.targets()) {
                if (npc.position.near(candidate.position, 32) && !candidate.dying
                        && definition.isBoss(candidate.id) && !definition.isDeadForm(candidate.id)) {
                    tracker.begin(candidate.actor, candidate.id, candidate.name, tick);
                    break;
                }
            }
        }
        tracker.hit(npc.actor, npc.id, npc.name, damage, tick);
        if (!tracker.isActive()) return;
        if (!wasActive || !tracker.isEncounter()) {
            origin = npc.position;
            missingSince = -1;
        }
        observeNearby();
        // ActorDeath may precede the final hitsplat, including on a one-hit kill.
        if (deathsThisTick.contains(npc.actor) || npc.dying)
            tracker.death(npc.actor, npc.id, tick);
        publish();
    }

    public void onDeath(ActorDeath event) {
        if (!enabled()) return;
        EncounterClient.RuneLite source = (EncounterClient.RuneLite) client;
        if (source.isPlayer(event.getActor())) playerDied();
        else if (event.getActor() instanceof NPC) died(source.target((NPC) event.getActor()));
    }

    void playerDied() {
        tracker.interrupt(client.tick(), "You died");
        publish();
    }

    void died(Target npc) {
        if (!enabled() || !npc.dying) return;
        if (deathsThisTick.size() < 64) deathsThisTick.add(npc.actor);
        if (near(npc)) tracker.death(npc.actor, npc.id, client.tick());
    }

    public void onChanged(NpcChanged event) {
        if (enabled()) changed(((EncounterClient.RuneLite) client).target(event.getNpc()));
    }

    void changed(Target npc) {
        if (!enabled() || !near(npc)) return;
        // Previously unseen dead forms are corpses, not evidence about this encounter.
        if (!npc.dying) tracker.observe(npc.actor, npc.id);
        tracker.changed(npc.actor, npc.id, client.tick());
        publish();
    }

    public void onDespawned(NpcDespawned event) {
        if (enabled()) despawned(((EncounterClient.RuneLite) client).target(event.getNpc()));
    }

    void despawned(Target npc) {
        // Disappearance alone is not a kill (teleports, unloading, and phase transitions).
        if (enabled() && near(npc) && npc.dying) tracker.death(npc.actor, npc.id, client.tick());
    }

    public void onGameState(GameState state) {
        if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING) reset();
        else if (state == GameState.CONNECTION_LOST) {
            tracker.interrupt(client.tick(), "Connection lost");
            publish();
        }
    }

    public void tick() {
        if (!enabled()) { reset(); return; }
        if (client.state() != GameState.LOGGED_IN) return;
        int tick = client.tick();
        boolean present = observeNearby();
        tracker.endTick();
        if (tracker.isActive()) {
            if (origin == null || !origin.near(client.player(), 48)) {
                tracker.interrupt(tick, "Left encounter");
            } else {
                if (present) missingSince = -1;
                else if (missingSince < 0) missingSince = tick;
                if (!present && (!tracker.isEncounter() || tick - missingSince >= PHASE_GRACE_TICKS))
                    tracker.interrupt(tick, "Target lost");
                else if (tick - tracker.lastHitTick() >= ABANDON_TICKS)
                    tracker.interrupt(tick, "No attacks for 2 minutes");
            }
        }
        EncounterTracker.Result result;
        while ((result = tracker.pollCompleted()) != null) {
            if (config.observedChatSummary()) client.chat(summary(result));
        }
        deathsThisTick.clear();
        publish();
    }

    private boolean observeNearby() {
        if (!tracker.isActive()) return false;
        boolean present = false;
        for (Target npc : client.targets()) {
            if (!near(npc) || !tracker.belongs(npc.actor, npc.id)) continue;
            if (!npc.dying) tracker.observe(npc.actor, npc.id);
            EncounterDefinition definition = EncounterDefinition.forNpc(npc.id);
            if (definition != null && definition.isDeadForm(npc.id))
                tracker.changed(npc.actor, npc.id, client.tick());
            if (npc.dying) tracker.death(npc.actor, npc.id, client.tick());
            if (definition == null || definition.isBoss(npc.id)) present = true;
        }
        return present;
    }

    private boolean near(Target npc) { return origin != null && origin.near(npc.position, 32); }
    private void publish() { displayed = tracker.result(client.tick()); }

    public EncounterTracker.Result displayed() {
        EncounterTracker.Result result = displayed;
        if (result == null || !config.showActualDps()) return null;
        if (result.status != EncounterTracker.Status.ACTIVE && !config.alwaysShowOverlay()
                && (client.tick() - result.endTick) * 0.6 > Math.max(1, config.targetTimeout())) return null;
        return result;
    }

    public static String formatDps(EncounterTracker.Result result) {
        return Double.isNaN(result.dps()) ? "—" : String.format(Locale.ROOT, "%.2f", result.dps());
    }

    public static String summary(EncounterTracker.Result result) {
        String name = result.name.replaceAll("<[^>]*>", "");
        return String.format(Locale.ROOT,
                "DPS Loadout Lab: %s — %s %s DPS · %,d damage%s · %.1fs since first hit%s",
                name, Double.isNaN(result.dps()) ? "N/A" : formatDps(result),
                result.encounter ? "encounter" : "NPC", result.damage(),
                result.addDamage == 0 ? "" : String.format(Locale.ROOT,
                        " (%,d boss + %,d adds)", result.bossDamage, result.addDamage),
                result.seconds(), Double.isNaN(result.dps()) ? " (too short to measure)" : "");
    }
}
