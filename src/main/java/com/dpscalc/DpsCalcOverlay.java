package com.dpscalc;

import com.dpscalc.calc.DpsResult;
import com.dpscalc.combat.CombatTracker;
import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterDataManager;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.PlayerStateManager;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.stream.Collectors;

import javax.inject.Inject;

public class DpsCalcOverlay extends OverlayPanel {

    private static final Color BRAND_ORANGE = ColorScheme.BRAND_ORANGE;
    private static final Color SECTION_TITLE_COLOR = new Color(255, 204, 0);
    private static final Color VALUE_POSITIVE = new Color(0, 255, 100);
    private static final Color VALUE_NEGATIVE = new Color(255, 80, 80);
    private static final Color VALUE_HIGHLIGHT = new Color(255, 180, 60);
    private static final Color VALUE_NORMAL = Color.WHITE;
    private static final Color MUTED_TEXT = ColorScheme.MEDIUM_GRAY_COLOR;

    private final DpsCalcPlugin plugin;
    private final DpsCalcConfig config;
    private final Client client;
    private final PlayerStateManager playerStateManager;
    private final MonsterDataManager monsterDataManager;

    @Inject
    public DpsCalcOverlay(
            DpsCalcPlugin plugin,
            DpsCalcConfig config,
            Client client,
            PlayerStateManager playerStateManager,
            MonsterDataManager monsterDataManager) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        this.client = client;
        this.playerStateManager = playerStateManager;
        this.monsterDataManager = monsterDataManager;

        setPosition(OverlayPosition.TOP_LEFT);
        setPriority(OverlayPriority.MED);
    }

    private DpsCalcPlugin.OverlaySnapshot displayed;

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay()) {
            return null;
        }

        if (client.getGameState() != GameState.LOGGED_IN) {
            if (config.alwaysShowOverlay()) {
                return renderNotLoggedIn(graphics);
            }
            return null;
        }

        NPC targetNpc = plugin.getTargetNpc();
        displayed = plugin.getOverlaySnapshot();
        DpsResult result =
                displayed == null || displayed.result.error != null
                        ? null
                        : displayed.result.normal;

        boolean hasTarget = displayed != null && displayed.target != null && result != null;

        if (!hasTarget && !config.alwaysShowOverlay()) {
            return null;
        }

        panelComponent
                .getChildren()
                .add(TitleComponent.builder().text("DPS Loadout Lab").color(BRAND_ORANGE).build());

        if (hasTarget) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Source:")
                                    .right(displayed.source)
                                    .rightColor(MUTED_TEXT)
                                    .build());
            if (!displayed.result.limitations.isEmpty())
                panelComponent
                        .getChildren()
                        .add(
                                LineComponent.builder()
                                        .left("Known formula limitation")
                                        .leftColor(Color.ORANGE)
                                        .build());
            if (displayed.result.groupDps != null)
                panelComponent
                        .getChildren()
                        .add(
                                LineComponent.builder()
                                        .left("Grouped encounter estimate")
                                        .leftColor(Color.ORANGE)
                                        .build());
            renderDpsInfo(targetNpc, result);
        } else {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Target:")
                                    .right("None")
                                    .rightColor(MUTED_TEXT)
                                    .build());
        }

        if (config.showDebugInfo()) {
            renderDebugInfo();
        }

        return super.render(graphics);
    }

    private Dimension renderNotLoggedIn(Graphics2D graphics) {
        panelComponent
                .getChildren()
                .add(TitleComponent.builder().text("DPS Loadout Lab").color(MUTED_TEXT).build());

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("Status:")
                                .right("Not logged in")
                                .rightColor(MUTED_TEXT)
                                .build());

        return super.render(graphics);
    }

    private void renderDpsInfo(NPC targetNpc, DpsResult result) {
        String npcName = displayed.target.getName();
        if (npcName == null) {
            npcName = "ID: " + displayed.target.getId();
        }

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("Target:")
                                .right(npcName)
                                .rightColor(VALUE_NORMAL)
                                .build());

        MonsterStats monsterStats = displayed == null ? null : displayed.target;
        if (monsterStats != null
                && monsterStats.getVersion() != null
                && !monsterStats.getVersion().isEmpty()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Version:")
                                    .right(monsterStats.getVersion())
                                    .rightColor(MUTED_TEXT)
                                    .build());
        }

        if (config.showDebugInfo()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("NPC ID:")
                                    .right(String.valueOf(displayed.target.getId()))
                                    .rightColor(MUTED_TEXT)
                                    .build());
        }

        if (config.showDps()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("DPS:")
                                    .right(result.getFormattedDps())
                                    .rightColor(VALUE_POSITIVE)
                                    .build());
        }

        if (config.showMaxHit()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Max Hit:")
                                    .right(String.valueOf(result.getMaxHit()))
                                    .rightColor(VALUE_HIGHLIGHT)
                                    .build());
        }

        if (config.showAccuracy()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Accuracy:")
                                    .right(result.getFormattedAccuracy())
                                    .rightColor(getAccuracyColor(result.getAccuracy()))
                                    .build());
        }

        if (config.showAttackSpeed()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Speed:")
                                    .right(result.getAttackSpeed() + " ticks")
                                    .rightColor(MUTED_TEXT)
                                    .build());
        }

        if (config.showRolls()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Atk Roll:")
                                    .right(formatNumber(result.getAttackRoll()))
                                    .build());

            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Def Roll:")
                                    .right(formatNumber(result.getDefenceRoll()))
                                    .build());
        }

        if (config.showMonsterStats()) {
            renderMonsterStats();
        }

        if (config.showAdvancedStats()) {
            renderAdvancedStats(result);
        }

        if (config.showSpecialAttack()) {
            renderSpecialAttack(result);
        }

        if (config.showActualDps()
                && config.overlaySource() == DpsCalcConfig.OverlaySource.LIVE_PLAYER) {
            renderActualDps(result);
        }
    }

    private void renderMonsterStats() {
        MonsterStats monster = displayed == null ? null : displayed.target;
        if (monster == null) {
            return;
        }

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("--- Monster ---")
                                .right("")
                                .leftColor(SECTION_TITLE_COLOR)
                                .build());

        if (config.showMonsterHp()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("HP:")
                                    .right(String.valueOf(monster.getHitpoints()))
                                    .rightColor(VALUE_POSITIVE)
                                    .build());
        }

        if (config.showMonsterCombatStats()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Atk/Str/Def:")
                                    .right(
                                            monster.getAttackLevel()
                                                    + "/"
                                                    + monster.getStrengthLevel()
                                                    + "/"
                                                    + monster.getDefenceLevel())
                                    .build());
        }

        if (config.showMonsterMagicLevel()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Magic:")
                                    .right(String.valueOf(monster.getMagicLevel()))
                                    .build());
        }

        if (config.showMonsterRangedLevel()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Ranged:")
                                    .right(String.valueOf(monster.getRangedLevel()))
                                    .build());
        }

        if (config.showMonsterDefBonuses()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Stab/Slash/Crush:")
                                    .right(
                                            monster.getStabDefence()
                                                    + "/"
                                                    + monster.getSlashDefence()
                                                    + "/"
                                                    + monster.getCrushDefence())
                                    .rightColor(MUTED_TEXT)
                                    .build());
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Magic/Ranged:")
                                    .right(
                                            monster.getMagicDefence()
                                                    + "/"
                                                    + monster.getStandardRangedDefence())
                                    .rightColor(MUTED_TEXT)
                                    .build());
        }

        if (config.showMonsterSize()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Size:")
                                    .right(monster.getSize() + "x" + monster.getSize())
                                    .build());
        }

        if (config.showMonsterAttributes() && !monster.getAttributes().isEmpty()) {
            String attrs =
                    monster.getAttributes().stream()
                            .map(MonsterAttribute::name)
                            .map(s -> s.toLowerCase().replace("_", " "))
                            .collect(Collectors.joining(", "));
            if (attrs.length() > 25) {
                attrs = attrs.substring(0, 22) + "...";
            }
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Attributes:")
                                    .right(attrs)
                                    .rightColor(VALUE_HIGHLIGHT)
                                    .build());
        }
    }

    private void renderAdvancedStats(DpsResult result) {
        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("--- Advanced ---")
                                .right("")
                                .leftColor(SECTION_TITLE_COLOR)
                                .build());

        if (config.showTimeToKill()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Time to Kill:")
                                    .right(metric(displayed.result.ttk, "%.1fs"))
                                    .rightColor(VALUE_NORMAL)
                                    .build());
        }

        if (config.showExpectedHits()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Expected Hits:")
                                    .right(
                                            metric(
                                                    displayed.result.ttk == null
                                                            ? null
                                                            : displayed.result.ttk
                                                                    / (result
                                                                                    .getExpectedAttackSpeed()
                                                                            * 0.6),
                                                    "%.1f"))
                                    .build());
        }

        if (config.showAverageHit()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Average Hit:")
                                    .right(result.getFormattedAverageHit())
                                    .build());
        }

        if (config.showDamagePerTick()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Dmg/Tick:")
                                    .right(result.getFormattedDamagePerTick())
                                    .build());
        }

        if (config.showKillsPerHour()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Kills/Hour:")
                                    .right(
                                            metric(
                                                    displayed.result.ttk == null
                                                                    || displayed.result.ttk <= 0
                                                            ? null
                                                            : 3600 / displayed.result.ttk,
                                                    "%.0f"))
                                    .rightColor(VALUE_HIGHLIGHT)
                                    .build());
        }
    }

    private static String metric(Double value, String format) {
        return value == null || !Double.isFinite(value)
                ? "N/A"
                : String.format(java.util.Locale.ROOT, format, value);
    }

    private void renderSpecialAttack(DpsResult normalResult) {
        DpsResult specResult = displayed == null ? null : displayed.result.special;
        if (specResult == null) {
            return;
        }

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("--- Spec Attack ---")
                                .right("")
                                .leftColor(BRAND_ORANGE)
                                .build());

        if (config.showSpecDps()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Spec DPS:")
                                    .right(specResult.getFormattedDps())
                                    .rightColor(VALUE_NORMAL)
                                    .build());
        }

        if (config.showSpecMaxHit()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Spec Max Hit:")
                                    .right(String.valueOf(specResult.getMaxHit()))
                                    .rightColor(VALUE_HIGHLIGHT)
                                    .build());
        }

        if (config.showSpecAccuracy()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Spec Accuracy:")
                                    .right(specResult.getFormattedAccuracy())
                                    .rightColor(getAccuracyColor(specResult.getAccuracy()))
                                    .build());
        }

        if (config.showSpecComparison() && normalResult != null) {
            double specDps = specResult.getDps();
            double normalDps = normalResult.getDps();

            if (specDps > 0 && normalDps > 0) {
                double diff = specDps - normalDps;
                double percentDiff = (diff / normalDps) * 100;

                String diffStr;
                Color diffColor;
                if (diff >= 0) {
                    diffStr = String.format("+%.1f%%", percentDiff);
                    diffColor = VALUE_POSITIVE;
                } else {
                    diffStr = String.format("%.1f%%", percentDiff);
                    diffColor = VALUE_NEGATIVE;
                }

                panelComponent
                        .getChildren()
                        .add(
                                LineComponent.builder()
                                        .left("vs Normal:")
                                        .right(diffStr)
                                        .rightColor(diffColor)
                                        .build());
            }
        }
    }

    private void renderActualDps(DpsResult theoreticalResult) {
        CombatTracker tracker = plugin.getCombatTracker();

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("--- Actual DPS ---")
                                .right("")
                                .leftColor(new Color(64, 224, 208))
                                .build());

        if (config.showActualDpsValue()) {
            Color dpsColor = tracker.isInCombat() ? VALUE_NORMAL : MUTED_TEXT;
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Actual DPS:")
                                    .right(tracker.getFormattedActualDps())
                                    .rightColor(dpsColor)
                                    .build());
        }

        if (config.showDpsComparison() && theoreticalResult != null) {
            double actualDps = tracker.getActualDps();
            double theoreticalDps = theoreticalResult.getDps();

            if (actualDps > 0 && theoreticalDps > 0) {
                double diff = actualDps - theoreticalDps;
                double percentDiff = (diff / theoreticalDps) * 100;

                String diffStr;
                Color diffColor;
                if (diff >= 0) {
                    diffStr = String.format("+%.1f%%", percentDiff);
                    diffColor = VALUE_POSITIVE;
                } else {
                    diffStr = String.format("%.1f%%", percentDiff);
                    diffColor = VALUE_NEGATIVE;
                }

                panelComponent
                        .getChildren()
                        .add(
                                LineComponent.builder()
                                        .left("vs Theoretical:")
                                        .right(diffStr)
                                        .rightColor(diffColor)
                                        .build());
            }
        }

        if (config.showTotalDamage()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Total Damage:")
                                    .right(formatNumber(tracker.getTotalDamage()))
                                    .build());
        }

        if (config.showCombatDuration()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Duration:")
                                    .right(tracker.getFormattedCombatDuration())
                                    .build());
        }

        if (config.showActualKillCount()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Kills:")
                                    .right(String.valueOf(tracker.getKillCount()))
                                    .build());
        }

        if (config.showActualKillsPerHour()) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Actual K/Hr:")
                                    .right(tracker.getFormattedActualKillsPerHour())
                                    .rightColor(VALUE_HIGHLIGHT)
                                    .build());
        }
    }

    private void renderDebugInfo() {
        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("--- Debug ---")
                                .right("")
                                .leftColor(VALUE_HIGHLIGHT)
                                .build());

        PlayerState state = plugin.getCachedPlayerState();
        if (state == null) return;

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("Monsters loaded:")
                                .right(String.valueOf(monsterDataManager.getMonsterCount()))
                                .rightColor(
                                        monsterDataManager.getMonsterCount() > 0
                                                ? VALUE_POSITIVE
                                                : VALUE_NEGATIVE)
                                .build());

        String combatStyle =
                state.getCombatStyle() != null ? state.getCombatStyle().toString() : "Unknown";
        panelComponent
                .getChildren()
                .add(LineComponent.builder().left("Combat Style:").right(combatStyle).build());

        String weaponName = state.getWeaponName();
        if (weaponName == null || weaponName.isEmpty()) {
            weaponName = "Unarmed";
        }
        if (weaponName.length() > 15) {
            weaponName = weaponName.substring(0, 12) + "...";
        }
        panelComponent
                .getChildren()
                .add(LineComponent.builder().left("Weapon:").right(weaponName).build());

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("Weapon ID:")
                                .right(String.valueOf(state.getWeaponId()))
                                .rightColor(MUTED_TEXT)
                                .build());

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("Weapon Speed:")
                                .right(state.getWeaponSpeed() + " ticks")
                                .build());

        StringBuilder prayers = new StringBuilder();
        if (state.getActivePrayers().isEmpty()) {
            prayers.append("None");
        } else {
            state.getActivePrayers()
                    .forEach(
                            p -> {
                                if (prayers.length() > 0) prayers.append(", ");
                                prayers.append(p.name());
                            });
        }
        String prayerStr = prayers.toString();
        if (prayerStr.length() > 20) {
            prayerStr = prayerStr.substring(0, 17) + "...";
        }
        panelComponent
                .getChildren()
                .add(LineComponent.builder().left("Prayers:").right(prayerStr).build());

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("Atk/Str/Def:")
                                .right(
                                        state.getBoostedAttack()
                                                + "/"
                                                + state.getBoostedStrength()
                                                + "/"
                                                + state.getBoostedDefence())
                                .build());

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("Ranged/Magic:")
                                .right(state.getBoostedRanged() + "/" + state.getBoostedMagic())
                                .build());

        if (state.getEquipmentStats() != null) {
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Melee Str:")
                                    .right(
                                            String.valueOf(
                                                    state.getEquipmentStats().getMeleeStrength()))
                                    .build());
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Ranged Str:")
                                    .right(
                                            String.valueOf(
                                                    state.getEquipmentStats().getRangedStrength()))
                                    .build());
            panelComponent
                    .getChildren()
                    .add(
                            LineComponent.builder()
                                    .left("Magic Dmg%:")
                                    .right(
                                            String.valueOf(
                                                    state.getEquipmentStats().getMagicDamage()))
                                    .build());
        }

        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left("Slayer Task:")
                                .right(state.isOnSlayerTask() ? "Yes" : "No")
                                .rightColor(state.isOnSlayerTask() ? VALUE_POSITIVE : MUTED_TEXT)
                                .build());
    }

    private Color getAccuracyColor(double accuracy) {
        if (accuracy >= 0.9) return VALUE_POSITIVE;
        if (accuracy >= 0.7) return VALUE_HIGHLIGHT;
        if (accuracy >= 0.5) return new Color(255, 165, 80);
        return VALUE_NEGATIVE;
    }

    private String formatNumber(int number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        }
        if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }
}
