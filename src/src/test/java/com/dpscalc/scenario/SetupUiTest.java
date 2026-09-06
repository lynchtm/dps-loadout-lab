package com.dpscalc.scenario;

import static org.junit.Assert.*;

import com.dpscalc.DpsCalcPlugin;
import com.dpscalc.equipment.EquipmentPreparationFacade;

import okhttp3.*;

import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import javax.swing.*;

public class SetupUiTest {
    @Test
    public void ownershipIndicatorsRefreshAfterBankScanWithoutEditingTheDraft() throws Exception {
        AtomicReference<OwnedEquipment> snapshot = new AtomicReference<>(OwnedEquipment.empty(0));
        Map<String, String> data = new HashMap<>();
        Scenario scenario = new Scenario();
        scenario.loadouts.get(0).player.getEquippedItemIds()[3] = 4151;
        scenario.loadouts.get(0).player.getEquippedItemNames()[3] = "Abyssal whip";
        data.put("workspaceV1", Scenario.JSON.toJson(scenario));
        ScenarioStorage storage =
                new ScenarioStorage() {
                    public long profileId() {
                        return 1;
                    }

                    public String getRSProfileKey() {
                        return "player";
                    }

                    public String getConfiguration(String group, String profile, String key) {
                        return data.get(key);
                    }

                    public void setConfiguration(
                            String group, String profile, String key, String value) {
                        data.put(key, value);
                    }
                };
        ScenarioPanel[] panel = new ScenarioPanel[1];
        SwingUtilities.invokeAndWait(
                () ->
                        panel[0] =
                                new ScenarioPanel(
                                        new DpsCalcPlugin() {
                                            public OwnedEquipment getOwnedEquipment() {
                                                return snapshot.get();
                                            }
                                        },
                                        new com.dpscalc.data.MonsterDataManager(),
                                        storage,
                                        new EquipmentPreparationFacade()));
        try {
            snapshot.set(
                    new OwnedEquipment(
                            "player", 1, 1, Map.of(4151, 1), Map.of(), Map.of(), Map.of()));
            SwingUtilities.invokeAndWait(panel[0]::refreshClientState);
            await(
                    () ->
                            find(
                                            panel[0],
                                            JButton.class,
                                            b ->
                                                    b.getToolTipText() != null
                                                            && b.getToolTipText()
                                                                    .contains("Abyssal whip")
                                                            && b.getToolTipText()
                                                                    .contains("Owned (1)"))
                                    != null);
            assertEquals(
                    4151,
                    Scenario.parse(data.get("workspaceV1")).loadouts.get(0).player.getWeaponId());
        } finally {
            SwingUtilities.invokeAndWait(panel[0]::dispose);
        }
    }

    static final String WIKI =
            "==Melee==\n"
                    + "{{Recommended equipment|style=Melee|weapon1={{plink|Abyssal"
                    + " whip}}|body1={{plink|Rune platebody}}|shield1={{plink|Dragon"
                    + " defender}}}}{{Inventory|1=Shark|2=Shark|3=Rune pouch}}{{Rune pouch|1=Blood"
                    + " rune\\16000}}";

    @Test
    public void wikiBrowserSearchPreviewImportAndProfileInvalidationAreIndependent()
            throws Exception {
        Scenario scenario = new Scenario();
        AtomicReference<String> profile = new AtomicReference<>("player");
        AtomicReference<Scenario.Loadout> imported = new AtomicReference<>();
        DpsCalcPlugin plugin = plugin();
        WikiTemplatePanel[] panel = new WikiTemplatePanel[1];
        SwingUtilities.invokeAndWait(
                () -> {
                    panel[0] =
                            new WikiTemplatePanel(
                                    () -> scenario,
                                    profile::get,
                                    plugin,
                                    new EquipmentPreparationFacade(),
                                    imported::set);
                    panel[0].refresh();
                    panel[0].open();
                    find(
                                    panel[0],
                                    JTextField.class,
                                    c ->
                                            "Wiki activity search"
                                                    .equals(
                                                            c.getAccessibleContext()
                                                                    .getAccessibleName()))
                            .setText("Nechryael");
                    button(panel[0], "Search Wiki").doClick();
                });
        try {
            await(() -> find(panel[0], JComboBox.class, c -> c.isEditable()).getItemCount() > 0);
            SwingUtilities.invokeAndWait(
                    () -> button(panel[0], "Load page / strategies").doClick());
            await(() -> find(panel[0], JComboBox.class, c -> !c.isEditable()).getItemCount() > 0);
            SwingUtilities.invokeAndWait(() -> button(panel[0], "Preview template").doClick());
            await(() -> button(panel[0], "Add as comparison draft").isEnabled());
            SwingUtilities.invokeAndWait(
                    () -> {
                        render(panel[0], "wiki-templates");
                        button(panel[0], "Add as comparison draft").doClick();
                    });
            assertNotNull(imported.get());
            assertEquals(4151, imported.get().player.getWeaponId());
            assertEquals(-1, scenario.loadouts.get(0).player.getWeaponId());
            assertEquals(16000, imported.get().carry.runes[0].quantity);
            SwingUtilities.invokeAndWait(
                    () -> {
                        assertFalse(panel[0].isVisible());
                        panel[0].setVisible(true);
                        button(panel[0], "Preview template").doClick();
                    });
            await(() -> button(panel[0], "Add as comparison draft").isEnabled());
            SwingUtilities.invokeAndWait(
                    () -> {
                        profile.set("other");
                        panel[0].refresh();
                        assertFalse(button(panel[0], "Add as comparison draft").isEnabled());
                    });
        } finally {
            SwingUtilities.invokeAndWait(panel[0]::dispose);
        }
    }

    @Test
    public void inventorySwitchPackingUsesSelectedDraftAndDoesNotMutateOtherDraft()
            throws Exception {
        Scenario scenario = new Scenario();
        scenario.loadouts.add(new Scenario.Loadout());
        scenario.selected = 1;
        Scenario.Loadout draft = scenario.loadouts.get(1);
        draft.carry.slots[0] = new CarryPlan.Entry(385, "Shark", 1);
        draft.carry.slots[1] = new CarryPlan.Entry(12791, "Rune pouch", 1);
        draft.carry.runes[0] = new CarryPlan.Entry(565, "Blood rune", 16000);
        draft.carry.switches.add(new CarryPlan.Entry(1215, "Dragon dagger", 1));
        AtomicInteger changes = new AtomicInteger();
        SwingUtilities.invokeAndWait(
                () -> {
                    CarryPlanPanel panel =
                            new CarryPlanPanel(
                                    () -> scenario,
                                    () -> "player",
                                    changes::incrementAndGet,
                                    plugin());
                    button(panel, "Prepare loadout ▸").doClick();
                    button(panel, "Pack").doClick();
                    assertEquals(1215, draft.carry.slots[2].id);
                    assertNull(scenario.loadouts.get(0).carry.slots[2]);
                    assertEquals(1, changes.get());
                    panel.refresh();
                    render(panel, "inventory-plan");
                });
    }

    private static DpsCalcPlugin plugin() {
        return plugin(WIKI, new java.util.concurrent.CopyOnWriteArrayList<>());
    }

    private static DpsCalcPlugin plugin(String wiki, java.util.List<String> searches) {
        Map<Integer, String> names =
                new java.util.HashMap<>(
                        Map.of(
                                4151,
                                "Abyssal whip",
                                1127,
                                "Rune platebody",
                                12954,
                                "Dragon defender",
                                385,
                                "Shark",
                                12791,
                                "Rune pouch",
                                565,
                                "Blood rune",
                                1215,
                                "Dragon dagger"));
        names.put(21003, "Elder maul");
        names.put(27281, "Divine rune pouch");
        names.put(554, "Fire rune");
        names.put(560, "Death rune");
        SetupItemIndex index = new SetupItemIndex(names, Set.of(565, 554, 560));
        OwnedEquipment owned =
                new OwnedEquipment(
                        "player",
                        1,
                        1,
                        Map.of(4151, 1, 1127, 1, 12954, 1, 385, 1, 12791, 1, 565, 15000),
                        Map.of(),
                        Map.of(),
                        Map.of(),
                        names);
        OkHttpClient http =
                new OkHttpClient.Builder()
                        .addInterceptor(
                                chain -> {
                                    if ("query"
                                            .equals(chain.request().url().queryParameter("action")))
                                        searches.add(
                                                Objects.toString(
                                                        chain.request()
                                                                .url()
                                                                .queryParameter("srsearch"),
                                                        ""));
                                    String json =
                                            chain.request()
                                                            .url()
                                                            .queryParameter("action")
                                                            .equals("query")
                                                    ? "{\"query\":{\"search\":[{\"title\":\"Test/Strategies\"}]}}"
                                                    : "{\"parse\":{\"title\":\"Test/Strategies\",\"revid\":123,\"wikitext\":{\"*\":"
                                                            + Scenario.JSON.toJson(wiki)
                                                            + "}}}";
                                    return new Response.Builder()
                                            .request(chain.request())
                                            .protocol(Protocol.HTTP_1_1)
                                            .code(200)
                                            .message("OK")
                                            .body(
                                                    ResponseBody.create(
                                                            MediaType.parse("application/json"),
                                                            json))
                                            .build();
                                })
                        .build();
        WikiSetupService service = new WikiSetupService(http);
        return new DpsCalcPlugin() {
            public SetupItemIndex getSetupItemIndex() {
                return index;
            }

            public WikiSetupService getWikiSetupService() {
                return service;
            }

            public OwnedEquipment getOwnedEquipment() {
                return owned;
            }
        };
    }

    @Test
    public void araxxorInventoryImportsThroughTargetScopedAddFlow() throws Exception {
        String wiki;
        try (java.io.InputStream stream =
                getClass().getResourceAsStream("/fixtures/araxxor-strategies.wikitext")) {
            assertNotNull(stream);
            wiki = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        java.util.List<String> searches = new java.util.concurrent.CopyOnWriteArrayList<>();
        DpsCalcPlugin plugin = plugin(wiki, searches);
        Scenario scenario = new Scenario();
        scenario.target = new com.dpscalc.data.MonsterStats();
        scenario.target.setId(-1);
        scenario.target.setName("Araxxor");
        scenario.target.setSize(5);
        scenario.target.setSpeed(4);
        scenario.target.setHitpoints(1020);
        Map<String, String> data = new HashMap<>();
        data.put("workspaceV1", Scenario.JSON.toJson(scenario));
        ScenarioStorage storage =
                new ScenarioStorage() {
                    public long profileId() {
                        return 1;
                    }

                    public String getRSProfileKey() {
                        return "player";
                    }

                    public String getConfiguration(String g, String p, String k) {
                        return data.get(k);
                    }

                    public void setConfiguration(String g, String p, String k, String v) {
                        data.put(k, v);
                    }
                };
        ScenarioPanel[] panel = new ScenarioPanel[1];
        WikiTemplatePanel[] browser = new WikiTemplatePanel[1];
        SwingUtilities.invokeAndWait(
                () -> {
                    panel[0] =
                            new ScenarioPanel(
                                    plugin,
                                    new com.dpscalc.data.MonsterDataManager(),
                                    storage,
                                    new EquipmentPreparationFacade());
                    browser[0] = find(panel[0], WikiTemplatePanel.class, c -> true);
                    assertFalse(browser[0].isVisible());
                    JButton plus =
                            find(
                                    panel[0],
                                    JButton.class,
                                    b -> b.getClientProperty("loadoutStarts") != null);
                    JPopupMenu menu = (JPopupMenu) plus.getClientProperty("loadoutStarts");
                    for (Component entry : menu.getComponents())
                        if (entry instanceof JMenuItem
                                && "Wiki setup".equals(((JMenuItem) entry).getText()))
                            ((JMenuItem) entry).doClick();
                    assertTrue(browser[0].isVisible());
                    assertEquals(
                            "Araxxor",
                            find(
                                            browser[0],
                                            JTextField.class,
                                            c ->
                                                    "Wiki activity search"
                                                            .equals(
                                                                    c.getAccessibleContext()
                                                                            .getAccessibleName()))
                                    .getText());
                    assertNull(button(panel[0], "Wiki encounter templates ▸"));
                });
        try {
            await(() -> find(browser[0], JComboBox.class, c -> c.isEditable()).getItemCount() > 0);
            assertTrue(searches.stream().anyMatch(q -> q.contains("Araxxor")));
            SwingUtilities.invokeAndWait(
                    () -> {
                        JTextField query =
                                find(
                                        browser[0],
                                        JTextField.class,
                                        c ->
                                                "Wiki activity search"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        query.setText("Vorkath");
                        query.postActionEvent();
                    });
            await(() -> searches.stream().anyMatch(q -> q.contains("Vorkath")));
            // Wait until the manual search completes before selecting its page.
            await(
                    () ->
                            find(
                                            browser[0],
                                            JTextArea.class,
                                            c -> c.getText().contains("Choose a page"))
                                    != null);
            SwingUtilities.invokeAndWait(
                    () -> button(browser[0], "Load page / strategies").doClick());
            await(
                    () ->
                            find(
                                                    browser[0],
                                                    JComboBox.class,
                                                    c ->
                                                            "Wiki gear setup"
                                                                    .equals(
                                                                            c.getAccessibleContext()
                                                                                    .getAccessibleName()))
                                            .getItemCount()
                                    == 2);
            SwingUtilities.invokeAndWait(() -> button(browser[0], "Preview template").doClick());
            await(() -> button(browser[0], "Add as comparison draft").isEnabled());
            SwingUtilities.invokeAndWait(
                    () -> {
                        JComboBox<?> inventory =
                                find(
                                        browser[0],
                                        JComboBox.class,
                                        c ->
                                                "Wiki inventory selection"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        inventory.setSelectedIndex(2);
                        assertFalse(button(browser[0], "Add as comparison draft").isEnabled());
                        button(browser[0], "Preview template").doClick();
                    });
            await(() -> button(browser[0], "Add as comparison draft").isEnabled());
            SwingUtilities.invokeAndWait(
                    () -> {
                        JLabel second =
                                find(
                                        find(browser[0], InventoryPreview.class, c -> true),
                                        JLabel.class,
                                        c ->
                                                "Suggested inventory 2"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertTrue(second.getToolTipText().startsWith("Heavy ballista"));
                        find(
                                        browser[0],
                                        JComboBox.class,
                                        c ->
                                                "Wiki inventory selection"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()))
                                .setSelectedIndex(0);
                        button(browser[0], "Preview template").doClick();
                    });
            await(() -> button(browser[0], "Add as comparison draft").isEnabled());
            SwingUtilities.invokeAndWait(
                    () -> {
                        InventoryPreview preview =
                                find(browser[0], InventoryPreview.class, c -> true);
                        assertTrue(preview.isVisible());
                        JLabel first =
                                find(
                                        preview,
                                        JLabel.class,
                                        c ->
                                                "Suggested inventory 1"
                                                        .equals(
                                                                c.getAccessibleContext()
                                                                        .getAccessibleName()));
                        assertEquals(21003, first.getClientProperty("itemId"));
                        render(browser[0], "araxxor-wiki-preview");
                        button(browser[0], "Add as comparison draft").doClick();
                        Scenario saved = Scenario.parse(data.get("workspaceV1"));
                        assertEquals(2, saved.loadouts.size());
                        assertEquals("Araxxor", saved.target.getName());
                        assertNull(saved.loadouts.get(0).carry.slots[0]);
                        assertEquals(21003, saved.loadouts.get(1).carry.slots[0].id);
                        assertEquals(27281, saved.loadouts.get(1).carry.slots[27].id);
                        assertEquals(565, saved.loadouts.get(1).carry.runes[0].id);
                        assertEquals(0, saved.loadouts.get(1).carry.runes[0].quantity);
                        assertFalse(browser[0].isVisible());
                        assertNotNull(button(panel[0], "Prepare loadout ▾"));
                        render(panel[0], "araxxor-imported-loadout");
                    });
        } finally {
            SwingUtilities.invokeAndWait(panel[0]::dispose);
        }
    }

    private static void await(BooleanSupplier test) throws Exception {
        for (int i = 0; i < 200; i++) {
            AtomicBoolean ready = new AtomicBoolean();
            SwingUtilities.invokeAndWait(() -> ready.set(test.getAsBoolean()));
            if (ready.get()) return;
            Thread.sleep(15);
        }
        fail("UI worker did not finish");
    }

    private static JButton button(Container root, String text) {
        return find(root, JButton.class, b -> text.equals(b.getText()));
    }

    private static <T extends Component> T find(Container root, Class<T> type, Predicate<T> test) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && test.test(type.cast(child))) return type.cast(child);
            if (child instanceof Container) {
                T nested = find((Container) child, type, test);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static void render(JPanel panel, String name) {
        panel.setSize(213, 2000);
        layout(panel);
        int height = Math.min(2600, Math.max(200, panel.getPreferredSize().height));
        panel.setSize(213, height);
        layout(panel);
        BufferedImage image = new BufferedImage(213, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        File output = new File("build/ui/" + name + ".png");
        output.getParentFile().mkdirs();
        try {
            javax.imageio.ImageIO.write(image, "png", output);
        } catch (java.io.IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents())
            if (child instanceof Container) layout((Container) child);
    }
}
