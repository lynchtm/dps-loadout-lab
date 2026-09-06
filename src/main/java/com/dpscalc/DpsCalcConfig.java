package com.dpscalc;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("dpscalc")
public interface DpsCalcConfig extends Config {
    enum OverlaySource {
        LIVE_PLAYER,
        SELECTED_COMPARISON
    }

    @ConfigItem(
            keyName = "overlaySource",
            name = "Overlay source",
            description =
                    "Live player uses observed gear, prayers and boosts. Selected comparison"
                            + " follows the active draft while Show sidebar is enabled.",
            position = 99)
    default OverlaySource overlaySource() {
        return OverlaySource.LIVE_PLAYER;
    }

    @ConfigItem(
            keyName = "automaticRefresh",
            name = "Automatic refresh",
            description = "Refresh live DPS once per game tick",
            position = 100)
    default boolean automaticRefresh() {
        return true;
    }

    @ConfigItem(
            keyName = "npcMenu",
            name = "NPC Calculate DPS menu",
            description = "Add a read-only DPS action to attackable NPC menus",
            position = 101)
    default boolean npcMenu() {
        return true;
    }

    // ==================== SECTIONS ====================

    @ConfigSection(name = "Overlay", description = "General overlay settings", position = 0)
    String overlaySection = "overlay";

    @ConfigSection(
            name = "DPS Stats",
            description = "Configure DPS-related stats to display",
            position = 1)
    String dpsSection = "dps";

    @ConfigSection(
            name = "Monster Stats",
            description = "Configure monster stats to display",
            position = 2,
            closedByDefault = true)
    String monsterSection = "monster";

    @ConfigSection(
            name = "Advanced Stats",
            description = "Configure advanced combat stats to display",
            position = 3,
            closedByDefault = true)
    String advancedSection = "advanced";

    @ConfigSection(
            name = "Special Attack",
            description = "Show special attack DPS and stats",
            position = 4,
            closedByDefault = true)
    String specSection = "spec";

    @ConfigSection(
            name = "Combat Buffs",
            description = "Configure assumed combat buffs",
            position = 5,
            closedByDefault = true)
    String buffsSection = "buffs";

    @ConfigSection(
            name = "Actual DPS",
            description = "Track real combat damage to compare theoretical vs actual DPS",
            position = 6,
            closedByDefault = true)
    String actualDpsSection = "actualDps";

    // ==================== OVERLAY SECTION ====================

    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Display the DPS overlay when targeting an NPC",
            section = overlaySection,
            position = 0)
    default boolean showOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "alwaysShowOverlay",
            name = "Always Show Overlay",
            description = "Show overlay even when not targeting an NPC",
            section = overlaySection,
            position = 1)
    default boolean alwaysShowOverlay() {
        return false;
    }

    @ConfigItem(
            keyName = "showDebugInfo",
            name = "Show Debug Info",
            description = "Display debug information (player stats, gear, prayers)",
            section = overlaySection,
            position = 2)
    default boolean showDebugInfo() {
        return false;
    }

    @ConfigItem(
            keyName = "targetTimeout",
            name = "Target Timeout (seconds)",
            description =
                    "Clear target after this many seconds of not being in combat (0 = never clear)",
            section = overlaySection,
            position = 3)
    default int targetTimeout() {
        return 0;
    }

    @ConfigItem(
            keyName = "showPanel",
            name = "Show Side Panel",
            description = "Display the DPS calculator panel in the sidebar",
            section = overlaySection,
            position = 4)
    default boolean showPanel() {
        return true;
    }

    @ConfigItem(
            keyName = "showPanelDiagnostics",
            name = "Show Panel Diagnostics",
            description = "Display side-panel diagnostic state for live DPS input troubleshooting",
            section = overlaySection,
            position = 5,
            hidden = true)
    default boolean showPanelDiagnostics() {
        return false;
    }

    // ==================== DPS STATS SECTION ====================

    @ConfigItem(
            keyName = "showDps",
            name = "Show DPS in Overlay",
            description =
                    "Display damage per second in overlay (DPS is always shown in side panel)",
            section = dpsSection,
            position = 0)
    default boolean showDps() {
        return false;
    }

    @ConfigItem(
            keyName = "showMaxHit",
            name = "Show Max Hit",
            description = "Display maximum hit",
            section = dpsSection,
            position = 1)
    default boolean showMaxHit() {
        return true;
    }

    @ConfigItem(
            keyName = "showAccuracy",
            name = "Show Accuracy",
            description = "Display hit chance percentage",
            section = dpsSection,
            position = 2)
    default boolean showAccuracy() {
        return true;
    }

    @ConfigItem(
            keyName = "showAttackSpeed",
            name = "Show Attack Speed",
            description = "Display weapon attack speed in ticks",
            section = dpsSection,
            position = 3)
    default boolean showAttackSpeed() {
        return false;
    }

    @ConfigItem(
            keyName = "showRolls",
            name = "Show Attack/Defence Rolls",
            description = "Display attack and defence roll values",
            section = dpsSection,
            position = 4)
    default boolean showRolls() {
        return false;
    }

    // ==================== MONSTER STATS SECTION ====================

    @ConfigItem(
            keyName = "showMonsterStats",
            name = "Show Monster Stats",
            description = "Enable monster stats section in overlay",
            section = monsterSection,
            position = 0)
    default boolean showMonsterStats() {
        return false;
    }

    @ConfigItem(
            keyName = "showMonsterHp",
            name = "Show HP",
            description = "Display monster hitpoints",
            section = monsterSection,
            position = 1)
    default boolean showMonsterHp() {
        return true;
    }

    @ConfigItem(
            keyName = "showMonsterCombatStats",
            name = "Show Combat Levels",
            description = "Display monster attack, strength, defence levels",
            section = monsterSection,
            position = 2)
    default boolean showMonsterCombatStats() {
        return true;
    }

    @ConfigItem(
            keyName = "showMonsterMagicLevel",
            name = "Show Magic Level",
            description = "Display monster magic level",
            section = monsterSection,
            position = 3)
    default boolean showMonsterMagicLevel() {
        return true;
    }

    @ConfigItem(
            keyName = "showMonsterRangedLevel",
            name = "Show Ranged Level",
            description = "Display monster ranged level",
            section = monsterSection,
            position = 4)
    default boolean showMonsterRangedLevel() {
        return false;
    }

    @ConfigItem(
            keyName = "showMonsterDefBonuses",
            name = "Show Defence Bonuses",
            description = "Display monster defence bonuses (stab/slash/crush/magic/ranged)",
            section = monsterSection,
            position = 5)
    default boolean showMonsterDefBonuses() {
        return false;
    }

    @ConfigItem(
            keyName = "showMonsterSize",
            name = "Show Size",
            description = "Display monster size (tiles)",
            section = monsterSection,
            position = 6)
    default boolean showMonsterSize() {
        return false;
    }

    @ConfigItem(
            keyName = "showMonsterAttributes",
            name = "Show Attributes",
            description = "Display monster attributes (demon, dragon, undead, etc.)",
            section = monsterSection,
            position = 7)
    default boolean showMonsterAttributes() {
        return false;
    }

    // ==================== ADVANCED STATS SECTION ====================

    @ConfigItem(
            keyName = "showAdvancedStats",
            name = "Show Advanced Stats",
            description = "Enable advanced stats section in overlay",
            section = advancedSection,
            position = 0)
    default boolean showAdvancedStats() {
        return false;
    }

    @ConfigItem(
            keyName = "showTimeToKill",
            name = "Show Time To Kill",
            description = "Display estimated time to kill the monster",
            section = advancedSection,
            position = 1)
    default boolean showTimeToKill() {
        return true;
    }

    @ConfigItem(
            keyName = "showExpectedHits",
            name = "Show Expected Hits",
            description = "Display expected number of hits to kill",
            section = advancedSection,
            position = 2)
    default boolean showExpectedHits() {
        return true;
    }

    @ConfigItem(
            keyName = "showAverageHit",
            name = "Show Average Hit",
            description = "Display average damage per hit",
            section = advancedSection,
            position = 3)
    default boolean showAverageHit() {
        return true;
    }

    @ConfigItem(
            keyName = "showDamagePerTick",
            name = "Show Damage Per Tick",
            description = "Display average damage per game tick",
            section = advancedSection,
            position = 4)
    default boolean showDamagePerTick() {
        return false;
    }

    @ConfigItem(
            keyName = "showKillsPerHour",
            name = "Show Kills Per Hour",
            description = "Display estimated kills per hour",
            section = advancedSection,
            position = 5)
    default boolean showKillsPerHour() {
        return false;
    }

    @ConfigItem(
            keyName = "showSpecialAttack",
            name = "Show Special Attack",
            description = "Enable special attack stats section in overlay",
            section = specSection,
            position = 0)
    default boolean showSpecialAttack() {
        return false;
    }

    @ConfigItem(
            keyName = "showSpecDps",
            name = "Show Spec DPS",
            description = "Display special attack damage per second",
            section = specSection,
            position = 1)
    default boolean showSpecDps() {
        return true;
    }

    @ConfigItem(
            keyName = "showSpecMaxHit",
            name = "Show Spec Max Hit",
            description = "Display special attack maximum hit",
            section = specSection,
            position = 2)
    default boolean showSpecMaxHit() {
        return true;
    }

    @ConfigItem(
            keyName = "showSpecAccuracy",
            name = "Show Spec Accuracy",
            description = "Display special attack hit chance",
            section = specSection,
            position = 3)
    default boolean showSpecAccuracy() {
        return true;
    }

    @ConfigItem(
            keyName = "showSpecComparison",
            name = "Show Spec vs Normal",
            description = "Display DPS difference between spec and normal attacks",
            section = specSection,
            position = 4)
    default boolean showSpecComparison() {
        return false;
    }

    // ==================== COMBAT BUFFS SECTION ====================

    @ConfigItem(
            keyName = "onSlayerTask",
            name = "On Slayer Task",
            description = "Assume you are on a slayer task for slayer helmet/black mask bonuses",
            section = buffsSection,
            position = 0)
    default boolean onSlayerTask() {
        return true;
    }

    @ConfigItem(
            keyName = "chargeSpell",
            name = "Charge Spell Active",
            description = "Assume the Charge spell is active for god spells",
            section = buffsSection,
            position = 1)
    default boolean chargeSpell() {
        return false;
    }

    @ConfigItem(
            keyName = "useBestOffensivePrayer",
            hidden = true,
            name = "Use Best Offensive Prayer",
            description =
                    "Automatically select best offensive prayer (Piety/Rigour/Augury) based on"
                            + " combat style",
            section = buffsSection,
            position = 2)
    default boolean useBestOffensivePrayer() {
        return true;
    }

    @ConfigItem(
            keyName = "assumeMaxBoosts",
            hidden = true,
            name = "Assume Max Boosts",
            description = "Calculate DPS assuming max stat boosts (+5/+6 levels)",
            section = buffsSection,
            position = 3)
    default boolean assumeMaxBoosts() {
        return true;
    }

    @ConfigItem(
            keyName = "showActualDps",
            name = "Show Actual DPS",
            description = "Enable actual DPS tracking section in overlay",
            section = actualDpsSection,
            position = 0)
    default boolean showActualDps() {
        return false;
    }

    @ConfigItem(
            keyName = "showActualDpsValue",
            name = "Show Actual DPS Value",
            description = "Display your actual damage per second from combat",
            section = actualDpsSection,
            position = 1)
    default boolean showActualDpsValue() {
        return true;
    }

    @ConfigItem(
            keyName = "showTotalDamage",
            name = "Show Total Damage",
            description = "Display total damage dealt in current session",
            section = actualDpsSection,
            position = 2)
    default boolean showTotalDamage() {
        return true;
    }

    @ConfigItem(
            keyName = "showCombatDuration",
            name = "Show Combat Duration",
            description = "Display how long you've been in combat",
            section = actualDpsSection,
            position = 3)
    default boolean showCombatDuration() {
        return true;
    }

    @ConfigItem(
            keyName = "showActualKillCount",
            name = "Show Kill Count",
            description = "Display number of kills in current session",
            section = actualDpsSection,
            position = 4)
    default boolean showActualKillCount() {
        return false;
    }

    @ConfigItem(
            keyName = "showActualKillsPerHour",
            name = "Show Actual Kills/Hour",
            description = "Display actual kills per hour based on current session",
            section = actualDpsSection,
            position = 5)
    default boolean showActualKillsPerHour() {
        return false;
    }

    @ConfigItem(
            keyName = "showDpsComparison",
            name = "Show DPS Comparison",
            description = "Display difference between theoretical and actual DPS",
            section = actualDpsSection,
            position = 6)
    default boolean showDpsComparison() {
        return true;
    }
}
