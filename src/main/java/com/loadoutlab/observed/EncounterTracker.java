// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab.observed;

import java.util.*;

/** Client-thread state machine, independent of live client objects and the calculator selection. */
public final class EncounterTracker {
    public enum Status { ACTIVE, COMPLETE, INTERRUPTED }

    public static final class Result {
        public final String name;
        public final boolean encounter;
        public final Status status;
        public final long bossDamage, addDamage;
        public final int startTick, endTick;
        public final String reason;

        private Result(Fight fight, Status status, int endTick, String reason) {
            name = fight.name;
            encounter = fight.definition != null;
            this.status = status;
            bossDamage = fight.bossDamage;
            addDamage = fight.addDamage;
            startTick = fight.startTick;
            this.endTick = Math.max(startTick, endTick);
            this.reason = reason;
        }

        public long damage() { return bossDamage + addDamage; }
        public double seconds() { return (endTick - startTick) * 0.6; }
        // One-tick kills have no measurable interval between the first hit and death.
        public double dps() { return seconds() == 0 ? Double.NaN : damage() / seconds(); }
    }

    private static final int MAX_MEMBERS = 64;
    private Fight active;
    private Result last;
    private final Deque<Result> completed = new ArrayDeque<>();

    public void reset() {
        active = null;
        last = null;
        completed.clear();
    }

    /** Start from a boss confirmed nearby when the player's first hit lands on an add. */
    public boolean begin(Object actor, int id, String name, int tick) {
        if (active != null) return false;
        EncounterDefinition definition = EncounterDefinition.forNpc(id);
        if (definition != null && (!definition.isBoss(id) || definition.isDeadForm(id))) return false;
        active = new Fight(actor, definition, definition == null ? name : definition.name, tick);
        return true;
    }

    /** Only player-owned damage/miss hitsplats enter here. An add alone cannot start a fight. */
    public void hit(Object actor, int id, String name, int amount, int tick) {
        EncounterDefinition definition = EncounterDefinition.forNpc(id);
        if (active != null && active.endTick >= 0 && tick > active.endTick) finish();
        if (active != null && !belongs(actor, id)) {
            // Incidental damage outside the encounter must not replace a boss fight.
            if (active.definition != null) return;
            interrupt(tick, "Target changed");
        }
        if (active == null) {
            if (!begin(actor, id, name, tick)) return;
        }
        if (active.endTick >= 0 && tick != active.endTick) return;
        observe(actor, id);
        if (definition != null && definition.isAdd(id)) active.addDamage += Math.max(0, amount);
        else active.bossDamage += Math.max(0, amount);
        active.lastHitTick = tick;
    }

    public boolean belongs(Object actor, int id) {
        return active != null && (active.definition == null ? active.primary == actor
                : EncounterDefinition.forNpc(id) == active.definition);
    }

    /** Observe nearby members, including bosses finished by a teammate after we switch to an add. */
    public void observe(Object actor, int id) {
        if (!belongs(actor, id) || active.members.size() >= MAX_MEMBERS) return;
        // A lingering corpse from the previous kill must not complete the new encounter.
        if (active.definition != null && (active.definition.isDeadForm(id)
                || active.definition.isResetForm(id)) && !active.members.contains(actor)) return;
        active.members.add(actor);
    }

    public void death(Object actor, int id, int tick) {
        if (active == null || !active.members.contains(actor)) return;
        if (active.definition == null || active.definition.endsOnDeath(id)
                || active.definition.isDeadForm(id)) markComplete(tick);
    }

    public void changed(Object actor, int id, int tick) {
        if (active == null || !active.members.contains(actor) || active.definition == null) return;
        if (active.definition.isDeadForm(id)) markComplete(tick);
        else if (active.definition.isResetForm(id) && active.endTick < 0)
            interrupt(tick, "Encounter reset");
    }

    private void markComplete(int tick) {
        if (active.endTick < 0) active.endTick = tick;
    }

    /** Called after the tick's hitsplats: death may be delivered before the killing hitsplat. */
    public void endTick() {
        if (active != null && active.endTick >= 0) finish();
    }

    private void finish() {
        last = new Result(active, Status.COMPLETE, active.endTick, "");
        if (completed.size() == 8) completed.removeFirst();
        completed.addLast(last);
        active = null;
    }

    public void interrupt(int tick, String reason) {
        if (active == null) return;
        if (active.endTick >= 0) { finish(); return; }
        last = new Result(active, Status.INTERRUPTED, tick, reason);
        active = null;
    }

    public Result pollCompleted() { return completed.pollFirst(); }
    public Result result(int tick) {
        return active == null ? last : new Result(active, Status.ACTIVE,
                active.endTick >= 0 ? active.endTick : tick, "");
    }
    public boolean isActive() { return active != null; }
    public boolean isEncounter() { return active != null && active.definition != null; }
    public int lastHitTick() { return active == null ? -1 : active.lastHitTick; }

    private static final class Fight {
        final Object primary;
        final EncounterDefinition definition;
        final String name;
        final int startTick;
        final Set<Object> members = Collections.newSetFromMap(new IdentityHashMap<>());
        int endTick = -1, lastHitTick;
        long bossDamage, addDamage;

        Fight(Object primary, EncounterDefinition definition, String name, int tick) {
            this.primary = primary;
            this.definition = definition;
            this.name = name == null ? "NPC" : name;
            startTick = lastHitTick = tick;
            members.add(primary);
        }
    }
}
