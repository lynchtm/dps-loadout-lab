package com.dpscalc.scenario;

import com.dpscalc.wikisetups.wiki.InventoryParser;
import com.dpscalc.wikisetups.wiki.InventorySetup;
import com.loadoutlab.DpsLoadoutLabPlugin;
import com.loadoutlab.equipment.EquipmentPreparationFacade;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;

import javax.swing.*;

/** User-initiated Wiki browser. Every import starts a new, independent comparison tab. */
final class WikiTemplatePanel extends JPanel {
    private final JPanel body = CalculatorWorkspace.column();

    private final Supplier<Scenario> model;
    private final Supplier<String> profile;
    private final DpsLoadoutLabPlugin plugin;
    private final EquipmentPreparationFacade equipment;
    private final Consumer<Scenario.Loadout> add;
    private final ThreadPoolExecutor worker =
            new ThreadPoolExecutor(
                    1,
                    1,
                    0,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    r -> {
                        Thread t = new Thread(r, "dps-wiki-templates");
                        t.setDaemon(true);
                        return t;
                    });
    private WikiSetupService.RequestScope scope;
    private long generation;
    private boolean disposed, binding;
    private final JTextField query = new JTextField(), filter = new JTextField();
    private final JComboBox<String> pages = new JComboBox<>();
    private final JComboBox<WikiLoadouts.Choice> choices = new JComboBox<>();
    private final JCheckBox owned = new JCheckBox("Prefer owned alternatives", true);
    private final JTextArea
            status =
                    CalculatorWorkspace.note(
                            "Search an activity, then choose a method, tier or role. Your selected"
                                    + " target stays in use."),
            details = CalculatorWorkspace.note("");
    private final LoadoutPreview preview;
    private final InventoryPreview inventoryPreview;
    private final JComboBox<InventoryOption> inventories = new JComboBox<>();
    private List<InventorySetup> pageInventories = Collections.emptyList();
    private final JButton importButton;
    private List<WikiLoadouts.Choice> all = Collections.emptyList();
    private WikiSetupService.Page page;
    private Scenario.Loadout draft;
    private String fingerprint;
    private long bankRevision;

    WikiTemplatePanel(
            Supplier<Scenario> model,
            Supplier<String> profile,
            DpsLoadoutLabPlugin plugin,
            EquipmentPreparationFacade equipment,
            Consumer<Scenario.Loadout> add) {
        this.model = model;
        this.profile = profile;
        this.plugin = plugin;
        this.equipment = equipment;
        this.add = add;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(CalculatorWorkspace.BG);
        setVisible(false);
        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(
                CalculatorWorkspace.label("Add Wiki loadout", CalculatorWorkspace.GOLD),
                BorderLayout.CENTER);
        heading.add(CalculatorWorkspace.action("Cancel", this::close), BorderLayout.EAST);
        add(heading);
        add(body);
        query.addActionListener(e -> search());
        query.getAccessibleContext().setAccessibleName("Wiki activity search");
        body.add(query);
        body.add(CalculatorWorkspace.action("Search Wiki", this::search));
        pages.setEditable(true);
        body.add(pages);
        body.add(CalculatorWorkspace.action("Load page / strategies", this::load));
        body.add(CalculatorWorkspace.label("Method, tier or role", CalculatorWorkspace.GOLD));
        filter.setToolTipText("Filter setups by method, tier or role; press Enter");
        filter.getAccessibleContext().setAccessibleName("Filter Wiki methods tiers or roles");
        filter.addActionListener(e -> filter());
        body.add(filter);
        choices.getAccessibleContext().setAccessibleName("Wiki gear setup");
        body.add(choices);
        body.add(CalculatorWorkspace.label("Inventory from guide", CalculatorWorkspace.GOLD));
        inventories.getAccessibleContext().setAccessibleName("Wiki inventory selection");
        body.add(inventories);
        inventories.addActionListener(
                e -> {
                    if (!binding) invalidatePreview();
                });
        body.add(owned);
        body.add(CalculatorWorkspace.action("Preview template", this::preview));
        preview = new LoadoutPreview(plugin::getItemManager);
        preview.setVisible(false);
        body.add(preview);
        inventoryPreview = new InventoryPreview(plugin::getItemManager);
        inventoryPreview.setVisible(false);
        body.add(inventoryPreview);
        details.setVisible(false);
        body.add(details);
        importButton =
                CalculatorWorkspace.action(
                        "Add as comparison draft",
                        () -> {
                            if (!valid()) {
                                clearPreview();
                                status.setText(
                                        "Loadout or bank changed. Preview the template again.");
                                return;
                            }
                            try {
                                add.accept(draft.copy());
                                close();
                                status.setText(
                                        "Added an independent draft. Review Combat and supplies for"
                                                + " this method.");
                            } catch (RuntimeException error) {
                                clearPreview();
                                status.setText(error.getMessage());
                            }
                        });
        importButton.setEnabled(false);
        body.add(importButton);
        body.add(status);
        choices.addActionListener(
                e -> {
                    if (!binding) {
                        selectInventory();
                        invalidatePreview();
                    }
                });
        owned.addActionListener(e -> invalidatePreview());
        CalculatorWorkspace.theme(this);
    }

    private void search() {
        String q = query.getText().trim();
        run(
                request -> plugin.getWikiSetupService().search(q, request),
                titles -> {
                    pages.removeAllItems();
                    for (String title : titles) pages.addItem(title);
                    status.setText(
                            titles.isEmpty()
                                    ? "No pages found. Enter an exact Wiki title above."
                                    : "Choose a page, then load its setups.");
                });
    }

    private void load() {
        Object value = pages.getEditor().getItem();
        String title = value == null ? query.getText() : value.toString();
        run(
                request -> {
                    WikiSetupService.Page result =
                            plugin.getWikiSetupService().page(title, request);
                    List<WikiLoadouts.Choice> parsed = WikiLoadouts.parse(result.text);
                    if (parsed.isEmpty() && !title.contains("/")) {
                        try {
                            WikiSetupService.Page strategy =
                                    plugin.getWikiSetupService()
                                            .page(title + "/Strategies", request);
                            List<WikiLoadouts.Choice> other = WikiLoadouts.parse(strategy.text);
                            if (!other.isEmpty()) {
                                result = strategy;
                                parsed = other;
                            }
                        } catch (java.io.IOException ignored) {
                        }
                    }
                    return new Loaded(result, parsed);
                },
                loaded -> {
                    page = loaded.page;
                    all = loaded.choices;
                    pageInventories = new InventoryParser().parse(page.text);
                    filter();
                    status.setText(
                            (page.stale ? "Cached copy — refresh unavailable. " : "")
                                    + page.title
                                    + " · revision "
                                    + page.revision
                                    + " · "
                                    + all.size()
                                    + " setups. "
                                    + (all.isEmpty()
                                            ? "No importable loadouts on this page. Its guide may"
                                                    + " describe gear in prose."
                                            : "Choose a setup to preview."));
                    status.setToolTipText(page.url());
                });
    }

    private void filter() {
        binding = true;
        try {
            choices.removeAllItems();
            String q = filter.getText().trim().toLowerCase(Locale.ROOT);
            for (WikiLoadouts.Choice choice : all)
                if (choice.label.toLowerCase(Locale.ROOT).contains(q)) choices.addItem(choice);
        } finally {
            binding = false;
        }
        selectInventory();
        invalidatePreview();
    }

    private void selectInventory() {
        binding = true;
        try {
            inventories.removeAllItems();
            WikiLoadouts.Choice choice = (WikiLoadouts.Choice) choices.getSelectedItem();
            InventorySetup matched = choice == null ? null : choice.inventory;
            inventories.addItem(
                    new InventoryOption(
                            matched,
                            matched == null
                                    ? "No matched inventory — choose below"
                                    : "Matched: " + matched.getLabel()));
            for (int i = 0; i < pageInventories.size() && i < 100; i++)
                inventories.addItem(
                        new InventoryOption(
                                pageInventories.get(i),
                                (i + 1) + " · " + pageInventories.get(i).getLabel()));
            inventories.setSelectedIndex(0);
        } finally {
            binding = false;
        }
    }

    private void invalidatePreview() {
        generation++;
        if (scope != null) scope.cancel();
        clearPreview();
    }

    private static final class InventoryOption {
        final InventorySetup inventory;
        final String label;

        InventoryOption(InventorySetup inventory, String label) {
            this.inventory = inventory;
            this.label = label;
        }

        public String toString() {
            return label.replace('<', '‹').replace('>', '›');
        }
    }

    private void preview() {
        WikiLoadouts.Choice choice = (WikiLoadouts.Choice) choices.getSelectedItem();
        if (choice == null || page == null) {
            status.setText("Load a Wiki page and choose a setup first.");
            return;
        }
        InventoryOption inventory = (InventoryOption) inventories.getSelectedItem();
        WikiLoadouts.Choice selected =
                new WikiLoadouts.Choice(
                        choice.label,
                        choice.gear,
                        inventory == null ? choice.inventory : inventory.inventory,
                        choice.switches);
        String expected = profile.get();
        Scenario.Loadout base = model.get().loadouts.get(model.get().selected).copy();
        OwnedEquipment snapshot = plugin.getOwnedEquipment();
        String state = state();
        WikiSetupService.Page source = page;
        boolean prefer = owned.isSelected();
        run(
                request ->
                        WikiLoadouts.draft(
                                selected,
                                source,
                                base,
                                snapshot,
                                expected,
                                prefer,
                                plugin.getSetupItemIndex(),
                                plugin.getSetupItemIndex()::stackable,
                                equipment),
                result -> {
                    if (!state.equals(state())
                            || snapshot.revision != plugin.getOwnedEquipment().revision) {
                        status.setText("Loadout or bank changed. Preview again.");
                        return;
                    }
                    draft = result;
                    fingerprint = state;
                    bankRevision = snapshot.revision;
                    preview.showLoadout(draft, snapshot, expected);
                    preview.setVisible(true);
                    inventoryPreview.showPlan(draft.carry);
                    inventoryPreview.setVisible(true);
                    details.setText(String.join("\n", draft.wikiNotes));
                    details.setRows(Math.min(12, Math.max(3, draft.wikiNotes.size() * 2)));
                    details.setToolTipText(source.url());
                    details.setVisible(true);
                    importButton.setEnabled(true);
                    status.setText(
                            "Preview only · "
                                    + source.title
                                    + " revision "
                                    + source.revision
                                    + (selected.inventory == null
                                            ? ". No inventory matched; choose an inventory above if"
                                                    + " the page lists one."
                                            : ". Gear, inventory and pouch runes will be added"
                                                    + " together."));
                    resizeContent();
                });
    }

    private String state() {
        return profile.get() + "\n" + Scenario.JSON.toJson(model.get());
    }

    private boolean valid() {
        return draft != null
                && Objects.equals(fingerprint, state())
                && bankRevision == plugin.getOwnedEquipment().revision;
    }

    private String lastProfile;

    void refresh() {
        String now = profile.get();
        if (!Objects.equals(now, lastProfile)) {
            lastProfile = now;
            generation++;
            if (scope != null) scope.cancel();
            worker.getQueue().clear();
            clearPreview();
        } else if (draft != null && !valid()) clearPreview();
    }

    private void clearPreview() {
        draft = null;
        importButton.setEnabled(false);
        preview.setVisible(false);
        inventoryPreview.setVisible(false);
        details.setVisible(false);
        resizeContent();
    }

    private interface Work<T> {
        T run(WikiSetupService.RequestScope request) throws Exception;
    }

    private <T> void run(Work<T> work, Consumer<T> accept) {
        if (plugin.getWikiSetupService() == null) {
            status.setText("Wiki service unavailable in this preview client.");
            return;
        }
        clearPreview();
        long token = ++generation;
        if (scope != null) scope.cancel();
        scope = new WikiSetupService.RequestScope();
        WikiSetupService.RequestScope request = scope;
        worker.getQueue().clear();
        status.setText("Loading…");
        String expected = profile.get();
        worker.submit(
                () -> {
                    try {
                        T value = work.run(request);
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (!disposed
                                            && token == generation
                                            && Objects.equals(expected, profile.get())) {
                                        accept.accept(value);
                                        resizeContent();
                                    }
                                });
                    } catch (Exception error) {
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (!disposed
                                            && token == generation
                                            && Objects.equals(expected, profile.get()))
                                        status.setText(
                                                "Could not load: "
                                                        + Objects.toString(
                                                                error.getMessage(),
                                                                "invalid Wiki data"));
                                });
                    }
                });
    }

    private void resizeContent() {
        for (Container parent = this; parent != null; parent = parent.getParent())
            parent.invalidate();
        super.revalidate();
    }

    void open() {
        setVisible(true);
        String target =
                model.get().target == null
                        ? ""
                        : Objects.toString(model.get().target.getName(), "");
        query.setText(target);
        filter.setText("");
        page = null;
        all = Collections.emptyList();
        pageInventories = Collections.emptyList();
        pages.removeAllItems();
        choices.removeAllItems();
        selectInventory();
        clearPreview();
        query.requestFocusInWindow();
        resizeContent();
        if (!target.isEmpty()) search();
        else status.setText("Search an activity or enter a Wiki page title.");
    }

    void close() {
        generation++;
        if (scope != null) scope.cancel();
        worker.getQueue().clear();
        clearPreview();
        setVisible(false);
        resizeContent();
    }

    void dispose() {
        disposed = true;
        generation++;
        if (scope != null) scope.cancel();
        worker.shutdownNow();
    }

    private static final class Loaded {
        final WikiSetupService.Page page;
        final List<WikiLoadouts.Choice> choices;

        Loaded(WikiSetupService.Page p, List<WikiLoadouts.Choice> c) {
            page = p;
            choices = c;
        }
    }
}
