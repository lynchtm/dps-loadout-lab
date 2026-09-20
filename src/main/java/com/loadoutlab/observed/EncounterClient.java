// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab.observed;

import java.util.*;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.NpcUtil;

/** Read-only boundary for replaying lifecycle and event ordering without a game client. */
interface EncounterClient {
    int tick();
    GameState state();
    Position player();
    List<Target> targets();
    void chat(String message);

    final class Position {
        final WorldPoint point;
        final int worldView;
        Position(WorldPoint point, int worldView) { this.point = point; this.worldView = worldView; }
        boolean near(Position other, int distance) {
            return other != null && point != null && other.point != null
                    && worldView == other.worldView && point.distanceTo(other.point) <= distance;
        }
    }

    final class Target {
        final Object actor;
        final int id;
        final String name;
        final Position position;
        final boolean dying;
        Target(Object actor, int id, String name, Position position, boolean dying) {
            this.actor = actor;
            this.id = id;
            this.name = name;
            this.position = position;
            this.dying = dying;
        }
    }

    final class RuneLite implements EncounterClient {
        private final Client client;
        private final NpcUtil npcUtil;
        RuneLite(Client client, NpcUtil npcUtil) { this.client = client; this.npcUtil = npcUtil; }
        public int tick() { return client.getTickCount(); }
        public GameState state() { return client.getGameState(); }
        public Position player() { return position(client.getLocalPlayer()); }
        public List<Target> targets() {
            List<Target> targets = new ArrayList<>();
            for (NPC npc : client.getNpcs()) targets.add(target(npc));
            return targets;
        }
        public void chat(String message) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
        }
        Target target(NPC npc) {
            return new Target(npc, npc.getId(), npc.getName(), position(npc), npcUtil.isDying(npc));
        }
        boolean isPlayer(Actor actor) { return actor == client.getLocalPlayer(); }
        private Position position(Actor actor) {
            return actor == null || actor.getWorldView() == null ? null
                    : new Position(actor.getWorldLocation(), actor.getWorldView().getId());
        }
    }
}
