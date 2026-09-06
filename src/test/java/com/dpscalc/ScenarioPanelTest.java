package com.dpscalc;

import static org.junit.Assert.*;

import com.dpscalc.data.*;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.scenario.*;

import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

import javax.swing.*;

public class ScenarioPanelTest {
    @Test
    public void bankGenerationStartsFromPlusAndClosesWhenAnotherStartingPathIsChosen()
            throws Exception {
        MemoryStorage storage = new MemoryStorage();
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        BankOptimizerPanel generator =
                                find(panel, BankOptimizerPanel.class, p -> true);
                        assertFalse(generator.isVisible());
                        action(panel, "Generate from bank").doClick();
                        assertTrue(generator.isVisible());
                        assertFalse(
                                find(generator, JButton.class, b -> "Generate".equals(b.getText()))
                                        .isEnabled());
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        p ->
                                                "Comparison loadouts"
                                                        .equals(
                                                                p.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals(1, tabs.getTabCount());
                        panel.setSize(225, 2400);
                        layout(panel);
                        JTabbedPane editor =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        p ->
                                                "Loadout editor"
                                                        .equals(
                                                                p.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertFalse(SwingUtilities.isDescendingFrom(generator, editor));
                        assertTrue(
                                SwingUtilities.convertPoint(
                                                        generator, 0, generator.getHeight(), panel)
                                                .y
                                        <= SwingUtilities.convertPoint(editor, 0, 0, panel).y);
                        BufferedImage image =
                                new BufferedImage(225, 2400, BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = image.createGraphics();
                        panel.printAll(graphics);
                        graphics.dispose();
                        try {
                            File output = new File("build/ui/bank-start-flow.png");
                            output.getParentFile().mkdirs();
                            javax.imageio.ImageIO.write(image, "png", output);
                        } catch (java.io.IOException e) {
                            throw new RuntimeException(e);
                        }
                        action(panel, "From template").doClick();
                        assertFalse(generator.isVisible());
                        JPanel template =
                                find(
                                        panel,
                                        JPanel.class,
                                        p ->
                                                "Create from template"
                                                        .equals(
                                                                p.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertTrue(template.isVisible());
                        action(panel, "Generate from bank").doClick();
                        assertFalse(template.isVisible());
                        find(
                                        generator,
                                        JButton.class,
                                        b ->
                                                "Close bank generator"
                                                        .equals(
                                                                b.getAccessibleContext()
                                                                        .getAccessibleName()))
                                .doClick();
                        assertFalse(generator.isVisible());
                        assertEquals(1, tabs.getTabCount());
                        action(panel, "Generate from bank").doClick();
                        action(panel, "Blank loadout").doClick();
                        assertFalse(generator.isVisible());
                        assertEquals(2, tabs.getTabCount());
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void sectionHeadersHaveFullWidthHitTargetsAndActionsStayInsideLoadout()
            throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    new MemoryStorage(),
                                    new EquipmentPreparationFacade());
                    try {
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        t ->
                                                "Loadout editor"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        JButton loadout =
                                find(
                                        panel,
                                        JButton.class,
                                        b ->
                                                "Loadout"
                                                        .equals(
                                                                b.getAccessibleContext()
                                                                        .getAccessibleName()));
                        JButton target =
                                find(
                                        panel,
                                        JButton.class,
                                        b ->
                                                "Target"
                                                        .equals(
                                                                b.getAccessibleContext()
                                                                        .getAccessibleName()));
                        JButton bank =
                                find(
                                        panel,
                                        JButton.class,
                                        b -> "Save as template".equals(b.getText()));
                        for (int width : new int[] {225, 320}) {
                            panel.setSize(width, 2600);
                            layout(panel);
                            assertTrue(
                                    SwingUtilities.convertPoint(tabs, 0, tabs.getHeight(), panel).y
                                            < SwingUtilities.convertPoint(bank, 0, 0, panel).y);
                            assertTrue(
                                    SwingUtilities.convertPoint(
                                                            target, 0, target.getHeight(), panel)
                                                    .y
                                            < SwingUtilities.convertPoint(loadout, 0, 0, panel).y);
                            for (double fraction : new double[] {.1, .5, .9}) {
                                clickAt(
                                        loadout,
                                        (int) (loadout.getWidth() * fraction),
                                        loadout.getHeight() / 2);
                                layout(panel);
                                assertEquals("▸ Loadout", loadout.getText());
                                assertFalse(tabs.getParent().isVisible());
                                clickAt(
                                        loadout,
                                        (int) (loadout.getWidth() * fraction),
                                        loadout.getHeight() / 2);
                                layout(panel);
                                assertEquals("▾ Loadout", loadout.getText());
                                assertTrue(tabs.getParent().isVisible());
                            }
                            assertTrue(
                                    tabs.getBoundsAt(4).x + tabs.getBoundsAt(4).width
                                            <= tabs.getWidth());
                            BufferedImage image =
                                    new BufferedImage(width, 2300, BufferedImage.TYPE_INT_RGB);
                            Graphics2D g = image.createGraphics();
                            panel.printAll(g);
                            g.dispose();
                            File file = new File("build/ui/runelite-cards-" + width + ".png");
                            file.getParentFile().mkdirs();
                            try {
                                javax.imageio.ImageIO.write(image, "png", file);
                            } catch (java.io.IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void groupedEncounterControlsPersistAndDriveComparisonResults() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        Scenario scenario = new Scenario();
        MonsterStats target = new MonsterStats();
        target.setId(-1);
        target.setName("Greater Nechryael");
        target.setSize(2);
        target.setSpeed(4);
        target.setHitpoints(205);
        target.setMagicLevel(1);
        target.setDefenceLevel(100);
        scenario.target = target;
        Scenario.Loadout staff = scenario.loadouts.get(0);
        staff.name = "Powered staff";
        staff.player.getEquippedItemIds()[3] = 11905;
        staff.player.setCombatStyle(com.dpscalc.state.CombatStyle.MAGIC_ACCURATE);
        Scenario.Loadout barrage = new Scenario.Loadout();
        barrage.name = "Ice Barrage";
        barrage.player.getEquippedItemIds()[3] = 4675;
        barrage.player.setCombatStyle(com.dpscalc.state.CombatStyle.MAGIC_AUTOCAST);
        barrage.player.setSpellName("Ice Barrage");
        barrage.player.setSpellbook("ancient");
        barrage.player.setSpellMaxHit(30);
        scenario.loadouts.add(barrage);
        storage.setConfiguration(
                "wikiDpsScenarios", "test-player", "workspaceV1", Scenario.JSON.toJson(scenario));
        ScenarioPanel[] panel = new ScenarioPanel[1];
        SwingUtilities.invokeAndWait(
                () -> {
                    panel[0] =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    assertEquals(
                            "Single target",
                            find(
                                            panel[0],
                                            JComboBox.class,
                                            c ->
                                                    "Encounter mode"
                                                            .equals(
                                                                    c.getAccessibleContext()
                                                                            .getAccessibleName()))
                                    .getSelectedItem());
                    JButton hint =
                            find(
                                    panel[0],
                                    JButton.class,
                                    b -> "Use grouped encounter".equals(b.getText()));
                    assertTrue(hint.isVisible());
                    hint.doClick();
                    find(
                                    panel[0],
                                    JSpinner.class,
                                    c ->
                                            "Grouped target count"
                                                    .equals(
                                                            c.getAccessibleContext()
                                                                    .getAccessibleName()))
                            .setValue(6);
                    assertTrue(saved(storage).encounter.grouped);
                    assertEquals(6, saved(storage).encounter.targets);
                    assertFalse(hint.isVisible());
                });
        try {
            boolean[] ready = {false};
            for (int i = 0; i < 150 && !ready[0]; i++) {
                SwingUtilities.invokeAndWait(
                        () -> {
                            JTable table =
                                    find(
                                            panel[0],
                                            JTable.class,
                                            t ->
                                                    "Loadout comparison"
                                                            .equals(
                                                                    t.getAccessibleContext()
                                                                            .getAccessibleName()));
                            ready[0] = table.getRowCount() == 5 && table.getColumnCount() == 2;
                        });
                if (!ready[0]) Thread.sleep(20);
            }
            assertTrue("Grouped comparison did not finish", ready[0]);
            SwingUtilities.invokeAndWait(
                    () -> {
                        JTable table =
                                find(
                                        panel[0],
                                        JTable.class,
                                        t ->
                                                "Loadout comparison"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals(
                                ((Number) table.getValueAt(0, 1)).doubleValue() * 6,
                                ((Number) table.getValueAt(1, 1)).doubleValue(),
                                1e-12);
                        assertEquals(table.getValueAt(0, 0), table.getValueAt(1, 0));
                        EncounterPanel controls = find(panel[0], EncounterPanel.class, c -> true);
                        controls.setSize(213, 220);
                        layout(controls);
                        BufferedImage image =
                                new BufferedImage(213, 220, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        controls.printAll(g);
                        g.dispose();
                        File file = new File("build/ui/group-encounter.png");
                        file.getParentFile().mkdirs();
                        try {
                            javax.imageio.ImageIO.write(image, "png", file);
                        } catch (java.io.IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        find(
                                        panel[0],
                                        JComboBox.class,
                                        c ->
                                                "Encounter mode"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()))
                                .setSelectedItem("Single target");
                        assertFalse(saved(storage).encounter.grouped);
                        assertEquals(6, saved(storage).encounter.targets);
                    });
        } finally {
            SwingUtilities.invokeAndWait(panel[0]::dispose);
        }
    }

    private static class MemoryStorage implements ScenarioStorage {
        final Map<String, String> data = new HashMap<>();
        volatile String rsProfile = "test-player";

        public long profileId() {
            return 1;
        }

        public String getRSProfileKey() {
            return rsProfile;
        }

        public String getConfiguration(String g, String p, String k) {
            return data.get(g + "/" + p + "/" + k);
        }

        public void setConfiguration(String g, String p, String k, String v) {
            data.put(g + "/" + p + "/" + k, v);
        }
    }

    @Test
    public void nativePanelRestoresSavedScenarioAndDisposesWorkers() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        Scenario scenario = new Scenario();
        scenario.loadouts.get(0).name = "Saved melee";
        storage.setConfiguration(
                "wikiDpsScenarios", "test-player", "workspaceV1", Scenario.JSON.toJson(scenario));
        EquipmentPreparationFacade equipment = new EquipmentPreparationFacade();
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    equipment);
                    try {
                        assertTrue(hasButton(panel, "Equip selected item"));
                        assertFalse(hasButton(panel, "Calculate all"));
                        assertNotNull(action(panel, "Blank loadout"));
                        panel.setSize(225, 1400);
                        layout(panel);
                        BufferedImage image =
                                new BufferedImage(225, 1400, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        panel.printAll(g);
                        g.dispose();
                        File output = new File("build/ui/scenario-panel.png");
                        output.getParentFile().mkdirs();
                        try {
                            javax.imageio.ImageIO.write(image, "png", output);
                        } catch (java.io.IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    } finally {
                        panel.dispose();
                    }
                    Scenario saved =
                            Scenario.parse(
                                    storage.getConfiguration(
                                            "wikiDpsScenarios", "test-player", "workspaceV1"));
                    assertEquals("Saved melee", saved.loadouts.get(0).name);
                });
    }

    private static boolean hasButton(Container c, String text) {
        for (Component child : c.getComponents()) {
            if (child instanceof JButton && ((JButton) child).getText().equals(text)) return true;
            if (child instanceof Container && hasButton((Container) child, text)) return true;
        }
        return false;
    }

    @Test
    public void equipmentSearchAndPrayerTogglesPersistCompatibleLoadouts() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        ScenarioPanel[] holder = new ScenarioPanel[1];
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    holder[0] = panel;
                    MonsterStats target = new MonsterStats();
                    target.setId(-1);
                    target.setName("Abyssal demon (test)");
                    target.setSize(1);
                    target.setSpeed(4);
                    target.setHitpoints(150);
                    target.setDefenceLevel(135);
                    target.setMagicLevel(1);
                    panel.acceptTarget(target);
                    JTextField search =
                            find(
                                    panel,
                                    JTextField.class,
                                    c ->
                                            "Search equipment"
                                                    .equals(
                                                            c.getAccessibleContext()
                                                                    .getAccessibleName()));
                    search.setText("Dragon defender");
                    find(panel, JButton.class, b -> "Equip selected item".equals(b.getText()))
                            .doClick();
                    assertTrue(saved(storage).loadouts.get(0).player.getEquippedItemIds()[5] > 0);
                    search.setText("Twisted bow");
                    find(panel, JButton.class, b -> "Equip selected item".equals(b.getText()))
                            .doClick();
                    assertEquals(-1, saved(storage).loadouts.get(0).player.getEquippedItemIds()[5]);
                    assertTrue(saved(storage).loadouts.get(0).player.getEquippedItemIds()[3] > 0);
                    search.setText("Dragon defender");
                    find(panel, JButton.class, b -> "Equip selected item".equals(b.getText()))
                            .doClick();
                    assertEquals(-1, saved(storage).loadouts.get(0).player.getEquippedItemIds()[3]);
                    find(
                                    panel,
                                    JToggleButton.class,
                                    b ->
                                            "Piety"
                                                    .equals(
                                                            b.getAccessibleContext()
                                                                    .getAccessibleName()))
                            .doClick();
                    find(
                                    panel,
                                    JToggleButton.class,
                                    b ->
                                            "Rigour"
                                                    .equals(
                                                            b.getAccessibleContext()
                                                                    .getAccessibleName()))
                            .doClick();
                    assertEquals(
                            Collections.singleton(com.dpscalc.state.Prayer.RIGOUR),
                            saved(storage).loadouts.get(0).player.getActivePrayers());
                    search.setText("Abyssal whip");
                    find(panel, JButton.class, b -> "Equip selected item".equals(b.getText()))
                            .doClick();
                    find(
                                    panel,
                                    JToggleButton.class,
                                    b ->
                                            "Piety"
                                                    .equals(
                                                            b.getAccessibleContext()
                                                                    .getAccessibleName()))
                            .doClick();
                });
        try {
            // Allow the analysis worker to publish its result before capturing populated tabs.
            for (int i = 0; i < 100; i++) {
                boolean[] done = {false};
                SwingUtilities.invokeAndWait(
                        () ->
                                done[0] =
                                        find(
                                                                holder[0],
                                                                JTable.class,
                                                                t ->
                                                                        "Loadout comparison"
                                                                                .equals(
                                                                                        t.getAccessibleContext()
                                                                                                .getAccessibleName()))
                                                        .getColumnCount()
                                                > 0);
                if (done[0]) break;
                Thread.sleep(20);
            }
            SwingUtilities.invokeAndWait(
                    () -> {
                        ScenarioPanel panel = holder[0];
                        assertTrue(
                                find(
                                                        panel,
                                                        JTable.class,
                                                        t ->
                                                                "Loadout comparison"
                                                                        .equals(
                                                                                t.getAccessibleContext()
                                                                                        .getAccessibleName()))
                                                .getColumnCount()
                                        > 0);
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        t ->
                                                "Loadout editor"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        BufferedImage image =
                                new BufferedImage(1125, 2000, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        for (int i = 0; i < 5; i++) {
                            tabs.setSelectedIndex(i);
                            panel.setSize(225, 2000);
                            layout(panel);
                            Graphics2D cell = (Graphics2D) g.create(i * 225, 0, 225, 2000);
                            panel.printAll(cell);
                            cell.dispose();
                        }
                        g.dispose();
                        File output = new File("build/ui/workspace-tabs.png");
                        output.getParentFile().mkdirs();
                        try {
                            javax.imageio.ImageIO.write(image, "png", output);
                        } catch (java.io.IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        } finally {
            SwingUtilities.invokeAndWait(() -> holder[0].dispose());
        }
    }

    private static Scenario saved(MemoryStorage s) {
        return Scenario.parse(s.getConfiguration("wikiDpsScenarios", "test-player", "workspaceV1"));
    }

    @Test
    public void iconTabsRespondAtTheirCentersAndEdges() throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    new MemoryStorage(),
                                    new EquipmentPreparationFacade());
                    try {
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        t ->
                                                "Loadout editor"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        for (int i = 0; i < 5; i++)
                            for (double fraction : new double[] {0.15, 0.5, 0.85}) {
                                tabs.setSelectedIndex((i + 1) % 5);
                                panel.setSize(225, 2600);
                                layout(panel);
                                Rectangle bounds = tabs.getBoundsAt(i);
                                clickAt(
                                        tabs,
                                        bounds.x + (int) (bounds.width * fraction),
                                        bounds.y + bounds.height / 2);
                                assertEquals(
                                        "Click anywhere across tab " + i + " at " + fraction,
                                        i,
                                        tabs.getSelectedIndex());
                            }
                    } finally {
                        panel.dispose();
                    }
                });
    }

    private static void clickAt(Container root, int x, int y) {
        Component receiver = SwingUtilities.getDeepestComponentAt(root, x, y);
        // Match AWT's lightweight targeting: tooltip labels can receive mouse events
        // even when their containing tab also has a mouse handler.
        while (receiver != root && receiver.getMouseListeners().length == 0)
            receiver = receiver.getParent();
        Point point = SwingUtilities.convertPoint(root, x, y, receiver);
        long time = System.currentTimeMillis();
        for (int id :
                new int[] {
                    java.awt.event.MouseEvent.MOUSE_PRESSED,
                    java.awt.event.MouseEvent.MOUSE_RELEASED,
                    java.awt.event.MouseEvent.MOUSE_CLICKED
                })
            receiver.dispatchEvent(
                    new java.awt.event.MouseEvent(
                            receiver,
                            id,
                            time,
                            0,
                            point.x,
                            point.y,
                            1,
                            false,
                            java.awt.event.MouseEvent.BUTTON1));
    }

    @Test
    public void wikiEditorTabsAndTargetChangesStayIndependentAndFitSidebar() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        MonsterStats target = new MonsterStats();
                        target.setId(-1);
                        target.setName("Custom target");
                        target.setSize(1);
                        target.setSpeed(4);
                        target.setHitpoints(150);
                        panel.acceptTarget(target);
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        t ->
                                                "Loadout editor"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals(5, tabs.getTabCount());
                        String[] names = {"Combat", "Skills", "Equipment", "Prayer", "Settings"};
                        for (int i = 0; i < 5; i++) {
                            assertEquals(names[i], tabs.getTitleAt(i));
                            assertNotNull(tabs.getIconAt(i));
                        }
                        find(panel, JButton.class, b -> "▸ Monster stats".equals(b.getText()))
                                .doClick();
                        JSpinner defence =
                                find(
                                        panel,
                                        JSpinner.class,
                                        c ->
                                                "Target: Defence"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        defence.setValue(200);
                        JSpinner slash =
                                find(
                                        panel,
                                        JSpinner.class,
                                        c ->
                                                "Target: Slash defence"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        slash.setValue(-20);
                        find(
                                        panel,
                                        JButton.class,
                                        b -> "▸ Defensive reductions".equals(b.getText()))
                                .doClick();
                        find(
                                        panel,
                                        JSpinner.class,
                                        c ->
                                                "Target: BGS damage"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()))
                                .setValue(30);
                        assertEquals(200, saved(storage).target.getDefenceLevel());
                        assertEquals(-20, saved(storage).target.getSlashDefence());
                        assertEquals(
                                30,
                                saved(storage).target.getInputs().getDefenceReductions().getBgs());
                        assertEquals(99, saved(storage).loadouts.get(0).player.getDefenceLevel());
                        assertNotNull(
                                find(
                                        panel,
                                        JButton.class,
                                        b -> "▾ Defensive reductions".equals(b.getText())));
                        find(
                                        panel,
                                        JButton.class,
                                        b -> "▾ Defensive reductions".equals(b.getText()))
                                .doClick();
                        for (int i = 0; i < 5; i++) {
                            tabs.setSelectedIndex(i);
                            panel.setSize(225, 2600);
                            layout(panel);
                            assertTrue(
                                    "All five tabs must stay on one row",
                                    tabs.getBoundsAt(4).x + tabs.getBoundsAt(4).width
                                            <= tabs.getWidth());
                            JButton targetHeading =
                                    find(panel, JButton.class, b -> "▾ Target".equals(b.getText()));
                            assertTrue(
                                    SwingUtilities.convertPoint(
                                                            targetHeading,
                                                            0,
                                                            targetHeading.getHeight(),
                                                            panel)
                                                    .y
                                            < SwingUtilities.convertPoint(tabs, 0, 0, panel).y);
                        }
                        tabs.setSelectedIndex(2);
                        panel.setSize(225, 2000);
                        layout(panel);
                        BufferedImage image =
                                new BufferedImage(225, 2000, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        panel.printAll(g);
                        g.dispose();
                        File output = new File("build/ui/wiki-target-editor.png");
                        output.getParentFile().mkdirs();
                        try {
                            javax.imageio.ImageIO.write(image, "png", output);
                        } catch (java.io.IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void equipmentBonusesAreAvailableBeforeChoosingTarget() {
        Scenario.Loadout loadout = new Scenario.Loadout();
        loadout.player.getEquippedItemIds()[3] = 4151;
        ScenarioCalculator.Result result =
                new ScenarioCalculator(new EquipmentPreparationFacade()).calculate(loadout, null);
        assertEquals("Select or create a target", result.error);
        assertNotNull(result.equipmentStats);
        assertTrue(result.equipmentStats.getSlashAttack() > 0);
        assertEquals(0, loadout.player.getEquipmentStats().getSlashAttack());
    }

    @Test
    public void prayerRefreshKeepsRuneLiteSidebarScrollPosition() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        ScenarioPanel[] holder = new ScenarioPanel[1];
        JScrollPane[] scroll = new JScrollPane[1];
        int[] position = {0};
        SwingUtilities.invokeAndWait(
                () -> {
                    holder[0] =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    JTabbedPane tabs =
                            find(
                                    holder[0],
                                    JTabbedPane.class,
                                    t ->
                                            "Loadout editor"
                                                    .equals(
                                                            t.getAccessibleContext()
                                                                    .getAccessibleName()));
                    tabs.setSelectedIndex(3);
                    JPanel wrapper = holder[0].getWrappedPanel();
                    wrapper.setSize(242, 550);
                    layout(wrapper);
                    scroll[0] =
                            find(
                                    wrapper,
                                    JScrollPane.class,
                                    s -> SwingUtilities.isDescendingFrom(holder[0], s));
                    scroll[0].getVerticalScrollBar().setValue(400);
                    position[0] = scroll[0].getVerticalScrollBar().getValue();
                    assertTrue(position[0] > 0);
                    JToggleButton prayer =
                            find(
                                    holder[0],
                                    JToggleButton.class,
                                    b ->
                                            "Piety"
                                                    .equals(
                                                            b.getAccessibleContext()
                                                                    .getAccessibleName()));
                    prayer.doClick();
                    JTextArea note =
                            find(
                                    holder[0],
                                    JTextArea.class,
                                    t ->
                                            t.getText().contains("Select a target")
                                                    || t.getText().contains("Choose a target"));
                    assertEquals(
                            javax.swing.text.DefaultCaret.NEVER_UPDATE,
                            ((javax.swing.text.DefaultCaret) note.getCaret()).getUpdatePolicy());
                });
        try {
            for (int i = 0; i < 100; i++) {
                boolean[] ready = {false};
                SwingUtilities.invokeAndWait(
                        () ->
                                ready[0] =
                                        find(
                                                                holder[0],
                                                                JTable.class,
                                                                t ->
                                                                        "Loadout comparison"
                                                                                .equals(
                                                                                        t.getAccessibleContext()
                                                                                                .getAccessibleName()))
                                                        .getColumnCount()
                                                > 0);
                if (ready[0]) break;
                Thread.sleep(20);
            }
            SwingUtilities.invokeAndWait(
                    () -> {
                        layout(holder[0].getWrappedPanel());
                        assertEquals(position[0], scroll[0].getVerticalScrollBar().getValue());
                    });
        } finally {
            SwingUtilities.invokeAndWait(() -> holder[0].dispose());
        }
    }

    @Test
    public void directRenameUpdatesOnlySelectedDraftAndMonsterStatsStayCollapsed()
            throws Exception {
        MemoryStorage storage = new MemoryStorage();
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        action(panel, "Duplicate draft").doClick();
                        JTextField name =
                                find(
                                        panel,
                                        JTextField.class,
                                        c ->
                                                "Comparison loadout name"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        name.setText("My boss setup");
                        name.postActionEvent();
                        assertEquals("My boss setup", saved(storage).loadouts.get(1).name);
                        assertEquals("Loadout 1", saved(storage).loadouts.get(0).name);
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        t ->
                                                "Comparison loadouts"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals("My boss setup", tabs.getToolTipTextAt(1));
                        tabs.setSelectedIndex(0);
                        assertEquals("Loadout 1", name.getText());
                        name.setText("   ");
                        name.postActionEvent();
                        assertEquals("Loadout 1", saved(storage).loadouts.get(0).name);
                        JButton stats =
                                find(
                                        panel,
                                        JButton.class,
                                        b -> "▸ Monster stats".equals(b.getText()));
                        stats.doClick();
                        assertEquals("▾ Monster stats", stats.getText());
                        action(panel, "Duplicate draft").doClick();
                        assertEquals("▾ Monster stats", stats.getText());
                        stats.doClick();
                        assertEquals("▸ Monster stats", stats.getText());
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void combatControlsUseIconsMultiSelectAndSeparateSections() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        t ->
                                                "Loadout editor"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        tabs.setSelectedIndex(1);
                        JCheckBox attack =
                                find(
                                        panel,
                                        JCheckBox.class,
                                        c ->
                                                "Potion: Super attack"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        JCheckBox strength =
                                find(
                                        panel,
                                        JCheckBox.class,
                                        c ->
                                                "Potion: Super strength"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        attack.doClick();
                        strength.doClick();
                        assertEquals(2, saved(storage).loadouts.get(0).potions.size());
                        assertTrue(attack.isSelected());
                        assertTrue(strength.isSelected());
                        JToggleButton piety =
                                find(
                                        panel,
                                        JToggleButton.class,
                                        b ->
                                                "Piety"
                                                        .equals(
                                                                b.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertNotNull(piety.getIcon());
                        assertEquals(4, ((GridLayout) piety.getParent().getLayout()).getColumns());
                        piety.doClick();
                        assertTrue(piety.isSelected());
                        assertTrue(
                                find(
                                                panel,
                                                JCheckBox.class,
                                                b -> "On a Slayer task".equals(b.getText()))
                                        .isSelected());
                        find(panel, JButton.class, b -> "▾ Potions & boosts".equals(b.getText()))
                                .doClick();
                        panel.setSize(225, 3600);
                        layout(panel);
                        Component extras = tabs.getSelectedComponent();
                        assertTrue(
                                "Combat controls must fit above Target",
                                SwingUtilities.convertPoint(extras, 0, extras.getHeight(), tabs).y
                                        <= tabs.getHeight());
                        JButton target =
                                find(panel, JButton.class, b -> "▾ Target".equals(b.getText()));
                        JButton results =
                                find(panel, JButton.class, b -> "▾ Results".equals(b.getText()));
                        assertFalse(SwingUtilities.isDescendingFrom(target, tabs));
                        assertFalse(SwingUtilities.isDescendingFrom(results, tabs));
                        target.doClick();
                        results.doClick();
                        assertEquals("▸ Target", target.getText());
                        assertEquals("▸ Results", results.getText());
                        panel.setSize(225, 2400);
                        layout(panel);
                        BufferedImage image =
                                new BufferedImage(225, 2400, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        panel.printAll(g);
                        g.dispose();
                        File output = new File("build/ui/combat-controls.png");
                        output.getParentFile().mkdirs();
                        try {
                            javax.imageio.ImageIO.write(image, "png", output);
                        } catch (java.io.IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void comparisonTabsSwitchIndependentLoadoutsAndSaveSelectedTab() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        com.dpscalc.state.PlayerState player = Scenario.defaults();
        player.getEquippedItemIds()[3] = 4151;
        player.getEquippedItemNames()[3] = "Abyssal whip";
        DpsCalcPlugin plugin =
                new DpsCalcPlugin() {
                    @Override
                    public com.dpscalc.state.PlayerState getCachedPlayerState() {
                        return player;
                    }
                };
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    plugin,
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        AbstractButton fromPlayer = action(panel, "From current player");
                        fromPlayer.doClick();
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        t ->
                                                "Comparison loadouts"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals(2, tabs.getTabCount());
                        assertEquals(1, tabs.getSelectedIndex());
                        assertEquals(-1, saved(storage).loadouts.get(0).player.getWeaponId());
                        tabs.setSelectedIndex(0);
                        assertEquals(0, saved(storage).selected);
                        find(panel, JButton.class, b -> "Save as template".equals(b.getText()))
                                .doClick();
                        JTextField name =
                                find(
                                        panel,
                                        JTextField.class,
                                        c ->
                                                "Template name"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertTrue(name.getParent().isVisible());
                        name.setText("Empty baseline");
                        find(panel, JButton.class, b -> "Save new template".equals(b.getText()))
                                .doClick();
                        String library =
                                storage.getConfiguration(
                                        "wikiDpsScenarios", "test-player", LoadoutLibrary.KEY);
                        assertEquals(
                                -1,
                                LoadoutLibrary.parse(library)
                                        .createDraft("Empty baseline")
                                        .player
                                        .getWeaponId());
                        tabs.setSelectedIndex(1);
                        assertEquals(
                                4151,
                                saved(storage)
                                        .loadouts
                                        .get(saved(storage).selected)
                                        .player
                                        .getWeaponId());
                        assertEquals(
                                library,
                                storage.getConfiguration(
                                        "wikiDpsScenarios", "test-player", LoadoutLibrary.KEY));
                        for (int i = 2; i < 32; i++) fromPlayer.doClick();
                        assertEquals(32, tabs.getTabCount());
                        assertEquals(31, tabs.getSelectedIndex());
                        tabs.setSelectedIndex(0);
                        tabs.setSelectedIndex(31);
                        assertEquals(31, saved(storage).selected);
                        fromPlayer.doClick();
                        assertEquals(32, tabs.getTabCount());
                        panel.setSize(225, 1500);
                        layout(panel);
                        BufferedImage image =
                                new BufferedImage(225, 1500, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        panel.printAll(g);
                        g.dispose();
                        File output = new File("build/ui/comparison-tabs.png");
                        output.getParentFile().mkdirs();
                        try {
                            javax.imageio.ImageIO.write(image, "png", output);
                        } catch (java.io.IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        JButton templateHeading =
                                find(panel, JButton.class, b -> "▸ Templates".equals(b.getText()));
                        assertTrue(
                                SwingUtilities.convertPoint(templateHeading, 0, 0, panel).y
                                        > SwingUtilities.convertPoint(tabs, 0, 0, panel).y);
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void explicitLoadCurrentPlayerRestoresClearedWeapon() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        com.dpscalc.state.PlayerState live = Scenario.defaults();
        live.getEquippedItemIds()[3] = 4151;
        live.getEquippedItemNames()[3] = "Abyssal whip";
        DpsCalcPlugin plugin =
                new DpsCalcPlugin() {
                    @Override
                    public com.dpscalc.state.PlayerState getCachedPlayerState() {
                        return live;
                    }
                };
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    plugin,
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        AbstractButton load = action(panel, "Load current player");
                        load.doClick();
                        assertEquals(4151, saved(storage).loadouts.get(0).player.getWeaponId());
                        JButton weapon =
                                find(
                                        panel,
                                        JButton.class,
                                        b ->
                                                b.getToolTipText() != null
                                                        && b.getToolTipText()
                                                                .startsWith("Weapon:"));
                        weapon.dispatchEvent(
                                new java.awt.event.MouseEvent(
                                        weapon,
                                        java.awt.event.MouseEvent.MOUSE_PRESSED,
                                        System.currentTimeMillis(),
                                        0,
                                        1,
                                        1,
                                        1,
                                        true,
                                        java.awt.event.MouseEvent.BUTTON3));
                        Scenario cleared = saved(storage);
                        assertEquals(-1, cleared.loadouts.get(0).player.getWeaponId());
                        // Background synchronization continues to respect intentional edits.
                        Scenario.mergeLive(cleared.loadouts.get(0), live);
                        assertEquals(-1, cleared.loadouts.get(0).player.getWeaponId());
                        action(panel, "Duplicate draft").doClick();
                        load.doClick();
                        Scenario restored = saved(storage);
                        assertEquals(4151, restored.loadouts.get(1).player.getWeaponId());
                        assertEquals(
                                "Abyssal whip",
                                restored.loadouts.get(1).player.getEquippedItemNames()[3]);
                        assertFalse(
                                restored.loadouts.get(1).overrides.contains("equippedItemIds.3"));
                        assertEquals(-1, restored.loadouts.get(0).player.getWeaponId());
                        assertEquals(4151, live.getWeaponId());
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void profileSwitchDoesNotWriteOldDraftIntoAnotherLibrary() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        ScenarioPanel[] holder = new ScenarioPanel[1];
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    holder[0] = panel;
                    find(
                                    panel,
                                    JTextField.class,
                                    c ->
                                            "Template name"
                                                    .equals(
                                                            c.getAccessibleContext()
                                                                    .getAccessibleName()))
                            .setText("Private template");
                    find(panel, JButton.class, b -> "Save new template".equals(b.getText()))
                            .doClick();
                    storage.rsProfile = "other-player";
                    find(panel, JButton.class, b -> "Save new template".equals(b.getText()))
                            .doClick();
                    assertNull(
                            storage.getConfiguration(
                                    "wikiDpsScenarios", "other-player", LoadoutLibrary.KEY));
                    panel.refreshClientState();
                });
        try {
            for (int i = 0; i < 100; i++) {
                boolean[] reloaded = {false};
                SwingUtilities.invokeAndWait(
                        () ->
                                reloaded[0] =
                                        find(
                                                                holder[0],
                                                                JComboBox.class,
                                                                c ->
                                                                        "Loadout templates"
                                                                                .equals(
                                                                                        c.getAccessibleContext()
                                                                                                .getAccessibleName()))
                                                        .getItemCount()
                                                == 3);
                if (reloaded[0]) break;
                Thread.sleep(25);
            }
            SwingUtilities.invokeAndWait(
                    () -> {
                        assertEquals(
                                3,
                                find(
                                                holder[0],
                                                JComboBox.class,
                                                c ->
                                                        "Loadout templates"
                                                                .equals(
                                                                        c.getAccessibleContext()
                                                                                .getAccessibleName()))
                                        .getItemCount());
                        assertEquals(
                                1,
                                LoadoutLibrary.parse(
                                                storage.getConfiguration(
                                                        "wikiDpsScenarios",
                                                        "test-player",
                                                        LoadoutLibrary.KEY))
                                        .names()
                                        .size());
                    });
        } finally {
            SwingUtilities.invokeAndWait(() -> holder[0].dispose());
        }
    }

    @Test
    public void templateActionsKeepTargetAndDoNotSaveDraftEditsToLibrary() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        action(panel, "From template").doClick();
                        JPanel picker =
                                find(
                                        panel,
                                        JPanel.class,
                                        p ->
                                                "Create from template"
                                                        .equals(
                                                                p.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertTrue(picker.isVisible());
                        MonsterStats target = new MonsterStats();
                        target.setName("Comparison target");
                        target.setId(-1);
                        target.setSize(1);
                        target.setHitpoints(100);
                        target.setSpeed(4);
                        panel.acceptTarget(target);
                        find(panel, JButton.class, b -> "Add to comparison".equals(b.getText()))
                                .doClick();
                        assertEquals(2, saved(storage).loadouts.size());
                        assertFalse(picker.isVisible());
                        assertEquals(4151, saved(storage).loadouts.get(1).player.getWeaponId());
                        find(
                                        panel,
                                        JTextField.class,
                                        c ->
                                                "Template name"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()))
                                .setText("My melee");
                        find(panel, JButton.class, b -> "Save new template".equals(b.getText()))
                                .doClick();
                        String library =
                                storage.getConfiguration(
                                        "wikiDpsScenarios", "test-player", LoadoutLibrary.KEY);
                        assertNotNull(library);
                        action(panel, "From template").doClick();
                        find(panel, JButton.class, b -> "Add to comparison".equals(b.getText()))
                                .doClick();
                        assertEquals(3, saved(storage).loadouts.size());
                        assertEquals("Comparison target", saved(storage).target.getName());
                        JTextField search =
                                find(
                                        panel,
                                        JTextField.class,
                                        c ->
                                                "Search equipment"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        search.setText("Twisted bow");
                        find(panel, JButton.class, b -> "Equip selected item".equals(b.getText()))
                                .doClick();
                        assertEquals(
                                library,
                                storage.getConfiguration(
                                        "wikiDpsScenarios", "test-player", LoadoutLibrary.KEY));
                        assertEquals(4151, saved(storage).loadouts.get(1).player.getWeaponId());
                        assertNotEquals(4151, saved(storage).loadouts.get(2).player.getWeaponId());
                        panel.setSize(225, 1800);
                        layout(panel);
                        BufferedImage image =
                                new BufferedImage(225, 1800, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = image.createGraphics();
                        panel.printAll(g);
                        g.dispose();
                        File output = new File("build/ui/template-library.png");
                        output.getParentFile().mkdirs();
                        try {
                            javax.imageio.ImageIO.write(image, "png", output);
                        } catch (java.io.IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    } finally {
                        panel.dispose();
                    }
                    ScenarioPanel reopened =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        assertEquals(3, saved(storage).loadouts.size());
                        JComboBox<?> templates =
                                find(
                                        reopened,
                                        JComboBox.class,
                                        c ->
                                                "Loadout templates"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals(4, templates.getItemCount());
                        find(reopened, JButton.class, b -> "▸ Templates".equals(b.getText()))
                                .doClick();
                        JPanel preview =
                                find(
                                        reopened,
                                        JPanel.class,
                                        p ->
                                                "Saved template equipment"
                                                        .equals(
                                                                p.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals(
                                4151,
                                find(
                                                preview,
                                                JLabel.class,
                                                l ->
                                                        l.getToolTipText() != null
                                                                && l.getToolTipText()
                                                                        .startsWith("Weapon:"))
                                        .getClientProperty("itemId"));
                        String beforeDelete = Scenario.JSON.toJson(saved(storage));
                        find(
                                        reopened,
                                        JButton.class,
                                        b ->
                                                "Delete saved template"
                                                        .equals(
                                                                b.getAccessibleContext()
                                                                        .getAccessibleName()))
                                .doClick();
                        assertEquals(beforeDelete, Scenario.JSON.toJson(saved(storage)));
                        assertEquals(3, templates.getItemCount());
                        assertTrue(
                                LoadoutLibrary.parse(
                                                storage.getConfiguration(
                                                        "wikiDpsScenarios",
                                                        "test-player",
                                                        LoadoutLibrary.KEY))
                                        .names()
                                        .isEmpty());
                        assertFalse(
                                find(
                                                reopened,
                                                JButton.class,
                                                b ->
                                                        "Delete saved template"
                                                                .equals(
                                                                        b.getAccessibleContext()
                                                                                .getAccessibleName()))
                                        .isEnabled());
                    } finally {
                        reopened.dispose();
                    }
                });
    }

    @Test
    public void templateBrowserAndCreationHaveIndependentSelectionsAndStayInTheirSections()
            throws Exception {
        MemoryStorage storage = new MemoryStorage();
        storage.setConfiguration(
                "wikiDpsScenarios",
                "test-player",
                "workspaceV1",
                Scenario.JSON.toJson(new Scenario()));
        LoadoutLibrary library = new LoadoutLibrary();
        Scenario.Loadout first =
                LoadoutLibrary.starter(0, Scenario.defaults(), new EquipmentPreparationFacade());
        first.carry.slots[0] = new CarryPlan.Entry(385, "Shark", 1);
        library.saveNew("Melee", first);
        String longName = String.join("", Collections.nCopies(80, "W"));
        library.saveNew(
                longName,
                LoadoutLibrary.starter(1, Scenario.defaults(), new EquipmentPreparationFacade()));
        storage.setConfiguration(
                "wikiDpsScenarios", "test-player", LoadoutLibrary.KEY, library.toJson());
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        JButton libraryHeading =
                                find(panel, JButton.class, b -> "▸ Templates".equals(b.getText()));
                        libraryHeading.doClick();
                        JComboBox<?> browser =
                                find(
                                        panel,
                                        JComboBox.class,
                                        c ->
                                                "Saved template library"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        JComboBox<?> picker =
                                find(
                                        panel,
                                        JComboBox.class,
                                        c ->
                                                "Loadout templates"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals(2, browser.getItemCount());
                        String unchanged = Scenario.JSON.toJson(saved(storage));
                        browser.setSelectedItem(longName);
                        assertEquals(unchanged, Scenario.JSON.toJson(saved(storage)));
                        action(panel, "From template").doClick();
                        JPanel form =
                                find(
                                        panel,
                                        JPanel.class,
                                        p ->
                                                "Create from template"
                                                        .equals(
                                                                p.getAccessibleContext()
                                                                        .getAccessibleName()));
                        find(form, JButton.class, b -> "Cancel".equals(b.getText())).doClick();
                        assertFalse(form.isVisible());
                        assertEquals(1, saved(storage).loadouts.size());
                        action(panel, "From template").doClick();
                        picker.setSelectedItem("Saved · Melee");
                        find(form, JButton.class, b -> "Add to comparison".equals(b.getText()))
                                .doClick();
                        assertEquals(4151, saved(storage).loadouts.get(1).player.getWeaponId());
                        assertEquals(385, saved(storage).loadouts.get(1).carry.slots[0].id);
                        assertEquals(longName, browser.getSelectedItem());
                        assertEquals(
                                library.toJson(),
                                storage.getConfiguration(
                                        "wikiDpsScenarios", "test-player", LoadoutLibrary.KEY));
                        find(
                                        panel,
                                        JButton.class,
                                        b ->
                                                "Delete saved template"
                                                        .equals(
                                                                b.getAccessibleContext()
                                                                        .getAccessibleName()))
                                .doClick();
                        assertEquals("Saved · Melee", picker.getSelectedItem());
                        assertEquals(1, browser.getItemCount());
                        assertEquals(2, saved(storage).loadouts.size());
                        JPanel preview =
                                find(
                                        panel,
                                        JPanel.class,
                                        p ->
                                                "Saved template preview"
                                                        .equals(
                                                                p.getAccessibleContext()
                                                                        .getAccessibleName()));
                        find(preview, JButton.class, b -> "▸ Inventory".equals(b.getText()))
                                .doClick();
                        assertEquals(
                                385,
                                find(
                                                preview,
                                                JLabel.class,
                                                l ->
                                                        "Suggested inventory 1"
                                                                .equals(
                                                                        l.getAccessibleContext()
                                                                                .getAccessibleName()))
                                        .getClientProperty("itemId"));
                        find(panel, JButton.class, b -> "▾ Loadout".equals(b.getText())).doClick();
                        panel.setSize(225, 2200);
                        layout(panel);
                        JButton distribution =
                                find(
                                        panel,
                                        JButton.class,
                                        b ->
                                                "Hit distribution"
                                                        .equals(
                                                                b.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertTrue(
                                SwingUtilities.convertPoint(libraryHeading, 0, 0, panel).y
                                        > SwingUtilities.convertPoint(
                                                        distribution,
                                                        0,
                                                        distribution.getHeight(),
                                                        panel)
                                                .y);
                        assertTrue(browser.getWidth() > 100);
                        BufferedImage image =
                                new BufferedImage(225, 2200, BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = image.createGraphics();
                        panel.printAll(graphics);
                        graphics.dispose();
                        try {
                            File output = new File("build/ui/saved-template-browser.png");
                            output.getParentFile().mkdirs();
                            javax.imageio.ImageIO.write(image, "png", output);
                        } catch (java.io.IOException e) {
                            throw new RuntimeException(e);
                        }
                        libraryHeading.doClick();
                        action(panel, "From template").doClick();
                        assertEquals("▸ Templates", libraryHeading.getText());
                        assertTrue(form.isVisible());
                        panel.setSize(225, 2200);
                        layout(panel);
                        assertTrue(
                                SwingUtilities.convertPoint(form, 0, 0, panel).y
                                        < SwingUtilities.convertPoint(libraryHeading, 0, 0, panel)
                                                .y);
                        BufferedImage pickerImage =
                                new BufferedImage(225, 2200, BufferedImage.TYPE_INT_RGB);
                        Graphics2D pickerGraphics = pickerImage.createGraphics();
                        panel.printAll(pickerGraphics);
                        pickerGraphics.dispose();
                        try {
                            javax.imageio.ImageIO.write(
                                    pickerImage, "png", new File("build/ui/template-picker.png"));
                        } catch (java.io.IOException e) {
                            throw new RuntimeException(e);
                        }
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void liveSyncBelongsToOneDraftAndStopsAtProfileChange() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        com.dpscalc.state.PlayerState player = Scenario.defaults();
        player.setAttackLevel(75);
        DpsCalcPlugin plugin =
                new DpsCalcPlugin() {
                    public com.dpscalc.state.PlayerState getCachedPlayerState() {
                        return player;
                    }
                };
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    plugin,
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        find(
                                        panel,
                                        JCheckBox.class,
                                        b -> "Sync this draft with live player".equals(b.getText()))
                                .doClick();
                        action(panel, "Blank loadout").doClick();
                        assertFalse(
                                find(
                                                panel,
                                                JCheckBox.class,
                                                b ->
                                                        "Sync this draft with live player"
                                                                .equals(b.getText()))
                                        .isSelected());
                        player.setAttackLevel(80);
                        panel.refreshClientState();
                        Scenario snapshot = saved(storage);
                        assertEquals(80, snapshot.loadouts.get(0).player.getAttackLevel());
                        assertEquals(99, snapshot.loadouts.get(1).player.getAttackLevel());
                        storage.rsProfile = "next";
                        panel.refreshClientState();
                        assertFalse(
                                find(
                                                panel,
                                                JCheckBox.class,
                                                b ->
                                                        "Sync this draft with live player"
                                                                .equals(b.getText()))
                                        .isSelected());
                    } finally {
                        panel.dispose();
                    }
                });
    }

    @Test
    public void renameReusesCalculationAndGraphsOnlyRunWhenExpanded() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        Scenario scenario = new Scenario();
        scenario.target = new MonsterStats();
        scenario.target.setId(-1);
        scenario.target.setName("Target");
        scenario.target.setHitpoints(100);
        scenario.target.setSize(1);
        scenario.target.setSpeed(4);
        scenario.loadouts.get(0).player.getEquippedItemIds()[3] = 4151;
        storage.setConfiguration(
                "wikiDpsScenarios", "test-player", "workspaceV1", Scenario.JSON.toJson(scenario));
        java.util.concurrent.atomic.AtomicReference<ScenarioCalculator.Result> published =
                new java.util.concurrent.atomic.AtomicReference<>();
        ScenarioPanel[] panel = new ScenarioPanel[1];
        SwingUtilities.invokeAndWait(
                () ->
                        panel[0] =
                                new ScenarioPanel(
                                        new DpsCalcPlugin() {
                                            public void publishComparison(
                                                    ScenarioCalculator.Result result,
                                                    MonsterStats target) {
                                                published.set(result);
                                            }
                                        },
                                        new MonsterDataManager(),
                                        storage,
                                        new EquipmentPreparationFacade()));
        try {
            for (int i = 0; i < 150 && published.get() == null; i++) Thread.sleep(20);
            assertNotNull(published.get());
            ScenarioCalculator.Result before = published.get();
            assertNull(before.error);
            SwingUtilities.invokeAndWait(
                    () -> {
                        PlotPanel plot = find(panel[0], PlotPanel.class, c -> true);
                        assertEquals(
                                "No graph data",
                                plot.getAccessibleContext().getAccessibleDescription());
                        find(
                                        panel[0],
                                        JButton.class,
                                        b ->
                                                "Hit distribution"
                                                        .equals(
                                                                b.getAccessibleContext()
                                                                        .getAccessibleName()))
                                .doClick();
                    });
            boolean[] plotted = {false};
            for (int i = 0; i < 150 && !plotted[0]; i++) {
                SwingUtilities.invokeAndWait(
                        () ->
                                plotted[0] =
                                        "Hit distribution"
                                                .equals(
                                                        find(panel[0], PlotPanel.class, c -> true)
                                                                .getAccessibleContext()
                                                                .getAccessibleName()));
                if (!plotted[0]) Thread.sleep(20);
            }
            assertTrue(plotted[0]);
            SwingUtilities.invokeAndWait(
                    () -> {
                        JTextField name =
                                find(
                                        panel[0],
                                        JTextField.class,
                                        c ->
                                                "Comparison loadout name"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        name.setText("Renamed cached draft");
                        name.postActionEvent();
                        assertSame(before.normal, published.get().normal);
                        assertEquals("Renamed cached draft", published.get().name);
                        assertEquals(
                                "Renamed cached draft",
                                find(panel[0], PlotPanel.class, c -> true)
                                        .getAccessibleContext()
                                        .getAccessibleDescription());
                        MonsterStats changed = scenario.target.copy();
                        changed.setDefenceLevel(300);
                        panel[0].acceptTarget(changed);
                        panel[0].acceptTarget(scenario.target);
                        assertSame(before.normal, published.get().normal);
                    });
            Thread.sleep(100);
            SwingUtilities.invokeAndWait(() -> assertSame(before.normal, published.get().normal));
        } finally {
            SwingUtilities.invokeAndWait(panel[0]::dispose);
        }
    }

    @Test
    public void tabCloseRemovesItsOwnDraftAndKeepsTemplatesAndSelection() throws Exception {
        MemoryStorage storage = new MemoryStorage();
        Scenario scenario = new Scenario();
        scenario.loadouts.clear();
        for (String name : new String[] {"Alpha", "Beta", "Gamma", "Delta"}) {
            Scenario.Loadout draft = new Scenario.Loadout();
            draft.name = name;
            scenario.loadouts.add(draft);
        }
        scenario.selected = 1;
        LoadoutLibrary library = new LoadoutLibrary();
        library.saveNew("Saved Beta", scenario.loadouts.get(1));
        String templates = library.toJson();
        storage.setConfiguration("wikiDpsScenarios", "test-player", LoadoutLibrary.KEY, templates);
        storage.setConfiguration(
                "wikiDpsScenarios", "test-player", "workspaceV1", Scenario.JSON.toJson(scenario));
        SwingUtilities.invokeAndWait(
                () -> {
                    ScenarioPanel panel =
                            new ScenarioPanel(
                                    new DpsCalcPlugin(),
                                    new MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    try {
                        assertNull(action(panel, "Remove loadout"));
                        JTabbedPane tabs =
                                find(
                                        panel,
                                        JTabbedPane.class,
                                        t ->
                                                "Comparison loadouts"
                                                        .equals(
                                                                t.getAccessibleContext()
                                                                        .getAccessibleName()));
                        panel.setSize(320, 2000);
                        layout(panel);
                        JButton close =
                                (JButton) ((JPanel) tabs.getTabComponentAt(1)).getComponent(1);
                        clickAt(close, close.getWidth() / 2, close.getHeight() / 2);
                        assertEquals(3, saved(storage).loadouts.size());
                        assertEquals(
                                "Gamma", saved(storage).loadouts.get(saved(storage).selected).name);
                        ((JButton) ((JPanel) tabs.getTabComponentAt(0)).getComponent(1)).doClick();
                        assertEquals(0, saved(storage).selected);
                        assertEquals("Gamma", saved(storage).loadouts.get(0).name);
                        panel.setSize(225, 2000);
                        layout(panel);
                        JPanel delta = (JPanel) tabs.getTabComponentAt(1);
                        JButton select = (JButton) delta.getComponent(0),
                                remove = (JButton) delta.getComponent(1);
                        assertFalse(select.getBounds().intersects(remove.getBounds()));
                        clickAt(select, select.getWidth() / 2, select.getHeight() / 2);
                        assertEquals(1, saved(storage).selected);
                        assertEquals(2, saved(storage).loadouts.size());
                        tabs.setSelectedIndex(0);
                        remove.doClick();
                        assertEquals("Gamma", saved(storage).loadouts.get(0).name);
                        assertEquals(0, saved(storage).selected);
                        JButton last =
                                (JButton) ((JPanel) tabs.getTabComponentAt(0)).getComponent(1);
                        assertFalse(last.isEnabled());
                        last.doClick();
                        assertEquals(1, saved(storage).loadouts.size());
                        assertEquals(
                                templates,
                                storage.getConfiguration(
                                        "wikiDpsScenarios", "test-player", LoadoutLibrary.KEY));
                    } finally {
                        panel.dispose();
                    }
                });
    }

    private static AbstractButton action(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JComponent)
                for (String property : new String[] {"loadoutActions", "loadoutStarts"}) {
                    Object popup = ((JComponent) child).getClientProperty(property);
                    if (popup instanceof JPopupMenu)
                        for (Component item : ((JPopupMenu) popup).getComponents())
                            if (item instanceof AbstractButton
                                    && text.equals(((AbstractButton) item).getText()))
                                return (AbstractButton) item;
                }
            if (child instanceof Container) {
                AbstractButton found = action((Container) child, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Component> T find(
            Container root, Class<T> type, java.util.function.Predicate<T> predicate) {
        T result = findOptional(root, type, predicate);
        assertNotNull(result);
        return result;
    }

    private static <T extends Component> T findOptional(
            Container root, Class<T> type, java.util.function.Predicate<T> predicate) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c) && predicate.test(type.cast(c))) return type.cast(c);
            if (c instanceof Container) {
                T found = findOptional((Container) c, type, predicate);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component child : c.getComponents())
            if (child instanceof Container) layout((Container) child);
    }
}
