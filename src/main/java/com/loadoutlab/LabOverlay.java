// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab;

import com.dpscalc.scenario.ScenarioCalculator;

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
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.showOverlay() || !plugin.playerPresent()) return null;
        ScenarioCalculator.Result result = plugin.displayed();
        if (result == null) return null;
        panelComponent.getChildren().clear();
        panelComponent
                .getChildren()
                .add(
                        TitleComponent.builder()
                                .text("DPS Loadout Lab")
                                .color(new Color(210, 190, 140))
                                .build());
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
        if (config.showActualDps())
            row("Observed DPS", String.format(Locale.ROOT, "%.2f", plugin.actualDps()));
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
