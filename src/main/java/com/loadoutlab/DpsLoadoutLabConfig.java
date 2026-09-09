// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab;

import net.runelite.client.config.*;

@ConfigGroup("dpscalc")
public interface DpsLoadoutLabConfig extends Config {
    enum OverlaySource {
        LIVE_PLAYER,
        SELECTED_COMPARISON
    }

    @ConfigItem(
            keyName = "overlaySource",
            name = "Overlay source",
            description = "Use the live player or selected comparison draft",
            position = 0)
    default OverlaySource overlaySource() {
        return OverlaySource.LIVE_PLAYER;
    }

    @ConfigItem(
            keyName = "showPanel",
            name = "Show sidebar",
            description = "Show the loadout calculator",
            position = 1)
    default boolean showPanel() {
        return true;
    }

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show overlay",
            description = "Display the calculation beside the game",
            position = 2)
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "npcMenu",
            name = "NPC calculator menu",
            description = "Add a Calculate DPS entry to NPC menus",
            position = 3)
    default boolean npcMenu() {
        return true;
    }

    @ConfigItem(
            keyName = "automaticRefresh",
            name = "Refresh live player",
            description = "Follow observed equipment, levels and prayers",
            position = 4)
    default boolean automaticRefresh() {
        return true;
    }

    @ConfigItem(
            keyName = "targetTimeout",
            name = "Target timeout",
            description = "Seconds to retain an inactive target",
            position = 5)
    default int targetTimeout() {
        return 30;
    }

    @ConfigItem(
            keyName = "alwaysShowOverlay",
            name = "Keep overlay visible",
            description = "Keep a completed target visible",
            position = 6)
    default boolean alwaysShowOverlay() {
        return false;
    }

    @ConfigItem(
            keyName = "onSlayerTask",
            name = "Live player on task",
            description =
                    "Apply Slayer equipment effects to the live overlay; drafts have their own"
                            + " setting",
            position = 7)
    default boolean onSlayerTask() {
        return true;
    }

    @ConfigItem(
            keyName = "chargeSpell",
            name = "Charge active",
            description = "Apply Charge to live god spells",
            position = 8)
    default boolean chargeSpell() {
        return false;
    }

    @ConfigItem(
            keyName = "showDps",
            name = "Show DPS",
            description = "Display expected damage per second",
            position = 9)
    default boolean showDps() {
        return true;
    }

    @ConfigItem(
            keyName = "showMaxHit",
            name = "Show max hit",
            description = "Display the largest attack outcome",
            position = 10)
    default boolean showMaxHit() {
        return true;
    }

    @ConfigItem(
            keyName = "showAccuracy",
            name = "Show accuracy",
            description = "Display attack success probability",
            position = 11)
    default boolean showAccuracy() {
        return true;
    }

    @ConfigItem(
            keyName = "showActualDps",
            name = "Track actual damage",
            description = "Track your observed hitsplats on the selected NPC",
            position = 12)
    default boolean showActualDps() {
        return false;
    }
}
