// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab;

import com.dpscalc.scenario.ScenarioCalculator;
import com.loadoutlab.observed.EncounterTracker;
import com.loadoutlab.observed.ObservedDps;

import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.components.*;

import java.awt.*;
import java.util.Locale;

final class LabOverlay extends OverlayPanel {
    private final DpsLoadoutLabPlugin plugin;
    private final DpsLoadoutLabConfig config;

    LabOverlay(DpsLoadoutLabPlugin plugin, DpsLoadoutLabConfig config) {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        panelComponent.setPreferredSize(new Dimension(260, 0));
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Toggle kill summaries", "DPS Loadout Lab",
                entry -> plugin.toggleObservedChat());
        addMenuEntry(MenuAction.RUNELITE_OVERLAY, "Reset observed DPS", "DPS Loadout Lab",
                entry -> plugin.resetObservedDps());
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay() || !plugin.playerPresent()) return null;
        ScenarioCalculator.Result result = plugin.displayed();
        EncounterTracker.Result observed = plugin.observedResult();
        if (result == null && observed == null) return null;
        panelComponent.getChildren().clear();
        panelComponent
                .getChildren()
                .add(
                        TitleComponent.builder()
                                .text("DPS Loadout Lab")
                                .color(new Color(210, 190, 140))
                                .build());
        if (result != null) {
            row("Target", plugin.targetLabel());
            row(
                    "Source",
                    config.overlaySource() == DpsLoadoutLabConfig.OverlaySource.LIVE_PLAYER
                            ? "Live player"
                            : "Selected comparison");
            if (result.error != null) row("Setup", "Check sidebar");
            else if (result.normal != null) {
                if (config.showDps())
                    row("DPS", String.format(Locale.ROOT, "%.3f", result.normal.getDps()));
                if (config.showMaxHit()) row("Max hit", Integer.toString(result.normal.getMaxHit()));
                if (config.showAccuracy())
                    row(
                            "Accuracy",
                            String.format(Locale.ROOT, "%.1f%%", result.normal.getAccuracy() * 100));
            }
        }
        if (observed != null) {
            row(observed.encounter ? "Encounter" : "Observed NPC", observed.name);
            row("Status", observed.status == EncounterTracker.Status.ACTIVE ? "In progress"
                    : observed.status == EncounterTracker.Status.COMPLETE ? "Completed" : "Interrupted");
            row("Observed DPS", observed.status == EncounterTracker.Status.INTERRUPTED ? "—"
                    : ObservedDps.formatDps(observed));
            row("Your damage", String.format(Locale.ROOT, "%,d", observed.damage()));
            if (observed.addDamage > 0)
                row("Boss / adds", String.format(Locale.ROOT, "%,d / %,d", observed.bossDamage, observed.addDamage));
            row("Since first hit", String.format(Locale.ROOT, "%.1fs", observed.seconds()));
            if (!observed.reason.isEmpty()) row("Stopped", observed.reason);
        }
        return super.render(graphics);
    }

    private void row(String label, String value) {
        panelComponent
                .getChildren()
                .add(
                        LineComponent.builder()
                                .left(label)
                                .right(value == null ? "—" : value)
                                .build());
    }
}
