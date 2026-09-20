// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab;

import com.dpscalc.scenario.ScenarioCalculator;
import com.loadoutlab.observed.EncounterTracker;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;
import static net.runelite.api.gameval.NpcID.*;

public class LabOverlayObservedTest {
    @Test public void observedResultRendersWithoutACalculatorResult() throws Exception {
        EncounterTracker tracker = new EncounterTracker();
        Object boss = new Object();
        tracker.hit(boss, VORKATH, "Vorkath", 750, 0);
        tracker.hit(new Object(), VORKATH_SPAWN, "Zombified spawn", 38, 50);
        tracker.death(boss, VORKATH, 100);
        tracker.endTick();
        LabOverlay overlay = new LabOverlay(new SnapshotPlugin(tracker.result(100)), new Settings());
        overlay.setClearChildren(false);
        BufferedImage image = new BufferedImage(300, 220, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        Dimension dimensions = overlay.render(graphics);
        graphics.dispose();
        assertNotNull(dimensions);
        assertEquals(7, overlay.getPanelComponent().getChildren().size());
        assertTrue(dimensions.height > 0 && dimensions.height <= image.getHeight());
        assertTrue(dimensions.width <= image.getWidth());
        assertTrue(overlay.getMenuEntries().stream().anyMatch(e -> e.getOption().equals("Toggle kill summaries")));
        assertTrue(overlay.getMenuEntries().stream().anyMatch(e -> e.getOption().equals("Reset observed DPS")));
        File output = new File("build/ui/observed-encounter.png");
        output.getParentFile().mkdirs();
        ImageIO.write(image, "png", output);
    }

    @Test public void emptyOrHiddenOverlayStaysHidden() {
        BufferedImage image = new BufferedImage(300, 220, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            assertNull(new LabOverlay(new SnapshotPlugin(null), new Settings()).render(graphics));
            EncounterTracker tracker = new EncounterTracker();
            tracker.hit(new Object(), VORKATH, "Vorkath", 20, 0);
            Settings hidden = new Settings() { public boolean showOverlay() { return false; } };
            assertNull(new LabOverlay(new SnapshotPlugin(tracker.result(10)), hidden).render(graphics));
        } finally { graphics.dispose(); }
    }

    private static class Settings implements DpsLoadoutLabConfig {
        public boolean showActualDps() { return true; }
    }

    private static final class SnapshotPlugin extends DpsLoadoutLabPlugin {
        private final EncounterTracker.Result snapshot;
        SnapshotPlugin(EncounterTracker.Result snapshot) { this.snapshot = snapshot; }
        @Override boolean playerPresent() { return true; }
        @Override ScenarioCalculator.Result displayed() { return null; }
        @Override EncounterTracker.Result observedResult() { return snapshot; }
    }
}
