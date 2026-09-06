package com.dpscalc.scenario;

import com.dpscalc.equipment.EquipmentPreparationFacade;

import net.runelite.client.game.ItemManager;

import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;

import javax.swing.*;

/** A generator creates a preview; only Add to comparison mutates the workspace. */
public final class BankOptimizerPanel extends JPanel {
    private final Supplier<Scenario> model;
    private final Supplier<OwnedEquipment> ownership;
    private final Supplier<String> profile;
    private final Consumer<Scenario.Loadout> addDraft;
    private final ScenarioStorage storage;
    private final BankOptimizer optimizer;
    private final ExecutorService worker =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread thread = new Thread(r, "wiki-dps-bank-search");
                        thread.setDaemon(true);
                        return thread;
                    });
    private final JPanel body = CalculatorWorkspace.column(),
            locks = CalculatorWorkspace.column(),
            preview = CalculatorWorkspace.column();
    private final JTextArea context = CalculatorWorkspace.note("Open your bank to scan equipment."),
            status = CalculatorWorkspace.note("");
    private final JTextArea metrics = CalculatorWorkspace.note(""),
            attackStyle = CalculatorWorkspace.note("");
    private final LoadoutPreview gearPreview;
    private final JComboBox<String> combatType =
            new JComboBox<>(new String[] {"Melee", "Ranged", "Magic"});
    private final JCheckBox unverified = new JCheckBox("Include unverified requirements");
    private final JCheckBox known = new JCheckBox("Include known formula limitations");
    private final JTextArea assumptions = CalculatorWorkspace.note("");
    private final JButton generate = CalculatorWorkspace.action("Generate", this::start),
            cancel = CalculatorWorkspace.action("Cancel", this::cancel);
    private final JButton add = CalculatorWorkspace.action("Add to comparison", this::add);
    private final JComboBox<String> alternatives = new JComboBox<>();
    private final Map<Integer, JCheckBox> lockBoxes = new LinkedHashMap<>();

    private Future<?> pending;
    private BankOptimizer.Request request;
    private BankOptimizer.Result result;
    private String fingerprint, savedProfile;
    private long generation, lastSavedScan = -1;
    private boolean disposed, binding;

    public BankOptimizerPanel(
            Supplier<Scenario> model,
            Supplier<OwnedEquipment> ownership,
            Supplier<String> profile,
            Consumer<Scenario.Loadout> addDraft,
            ScenarioStorage storage,
            EquipmentPreparationFacade equipment) {
        this(model, ownership, profile, addDraft, storage, equipment, () -> null);
    }

    public BankOptimizerPanel(
            Supplier<Scenario> model,
            Supplier<OwnedEquipment> ownership,
            Supplier<String> profile,
            Consumer<Scenario.Loadout> addDraft,
            ScenarioStorage storage,
            EquipmentPreparationFacade equipment,
            Supplier<ItemManager> items) {
        gearPreview = new LoadoutPreview(items);
        this.model = model;
        this.ownership = ownership;
        this.profile = profile;
        this.addDraft = addDraft;
        this.storage = storage;
        optimizer = new BankOptimizer(equipment);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(CalculatorWorkspace.BG);
        getAccessibleContext().setAccessibleName("Create from bank");
        setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CalculatorWorkspace.EDGE),
                        BorderFactory.createEmptyBorder(8, 6, 8, 6)));
        JPanel heading = new JPanel(new BorderLayout(4, 0));
        heading.setOpaque(false);
        heading.add(
                CalculatorWorkspace.label("GENERATE FROM BANK", CalculatorWorkspace.GOLD),
                BorderLayout.CENTER);
        JButton close = CalculatorWorkspace.action("×", this::close);
        close.setPreferredSize(new Dimension(30, 30));
        close.getAccessibleContext().setAccessibleName("Close bank generator");
        close.setToolTipText("Cancel and close bank generation");
        heading.add(close, BorderLayout.EAST);
        add(heading);
        add(body);
        setVisible(false);
        body.add(context);
        body.add(CalculatorWorkspace.label("Combat type", CalculatorWorkspace.MUTED));
        body.add(combatType);
        combatType.getAccessibleContext().setAccessibleName("Optimizer combat type");
        combatType.setSelectedItem(BankOptimizer.family(current()));
        combatType.addActionListener(
                e -> {
                    if (!binding) {
                        invalidateResult();
                        refresh();
                        resize();
                    }
                });
        body.add(
                CalculatorWorkspace.note(
                        "Tests this combat type against the target. Magic automatically compares"
                            + " spells and powered weapons. Keeps your draft's levels, prayers and"
                            + " boosts. Preview the result, then add it as a new comparison"
                            + " loadout."));
        JButton lockToggle = CalculatorWorkspace.action("Advanced options ▸", () -> {});
        body.add(lockToggle);
        body.add(locks);
        locks.setVisible(false);
        lockToggle.addActionListener(
                e -> {
                    locks.setVisible(!locks.isVisible());
                    lockToggle.setText(
                            locks.isVisible() ? "Advanced options ▾" : "Advanced options ▸");
                    resize();
                });
        for (int i = 0; i < BankOptimizer.SLOTS.length; i++) {
            int slot = BankOptimizer.SLOTS[i];
            JCheckBox box = new JCheckBox(BankOptimizer.SLOT_NAMES[i]);
            box.getAccessibleContext().setAccessibleName("Lock " + BankOptimizer.SLOT_NAMES[i]);
            box.addActionListener(
                    e -> {
                        invalidateResult();
                    });
            locks.add(box);
            lockBoxes.put(slot, box);
        }
        unverified.setToolTipText(
                "Allow owned items missing level-requirement data. Known unmet requirements stay"
                        + " excluded.");
        unverified.addActionListener(e -> invalidateResult());
        locks.add(unverified);
        known.addActionListener(e -> invalidateResult());
        locks.add(known);
        body.add(CalculatorWorkspace.pair(generate, cancel));
        cancel.setEnabled(false);
        body.add(status);
        preview.add(alternatives);
        alternatives.getAccessibleContext().setAccessibleName("Generated alternatives");
        alternatives.addActionListener(
                e -> {
                    if (!binding) showAlternative();
                });
        assumptions.setForeground(CalculatorWorkspace.GOLD);
        assumptions.getAccessibleContext().setAccessibleName("Suggestion assumptions");
        preview.add(assumptions);
        preview.add(metrics);
        preview.add(gearPreview);
        preview.add(attackStyle);
        preview.add(
                CalculatorWorkspace.action(
                        "Review exclusions & notes",
                        () -> {
                            if (result == null) return;
                            List<String> notes = new ArrayList<>(result.exclusions);
                            int index = alternatives.getSelectedIndex();
                            if (index >= 0) notes.addAll(result.calculations.get(index).warnings);
                            JTextArea text = CalculatorWorkspace.note(String.join("\n\n", notes));
                            JScrollPane scroll = new JScrollPane(text);
                            scroll.setPreferredSize(new Dimension(450, 400));
                            SidebarScrolling.nested(scroll);
                            JOptionPane.showMessageDialog(
                                    this, scroll, "Optimizer coverage", JOptionPane.PLAIN_MESSAGE);
                        }));
        preview.add(add);
        body.add(preview);
        preview.setVisible(false);
        CalculatorWorkspace.theme(this);
        refresh();
    }

    private Scenario.Loadout current() {
        Scenario scenario = model.get();
        return scenario.loadouts.get(scenario.selected);
    }

    public void open() {
        if (disposed) return;
        close();
        setVisible(true);
        refresh();
        combatType.requestFocusInWindow();
        resize();
    }

    public void close() {
        cancel();
        result = null;
        request = null;
        preview.setVisible(false);
        add.setEnabled(false);
        setVisible(false);
        setStatus("");
        resize();
    }

    private Set<Integer> locked() {
        Set<Integer> selected = new HashSet<>();
        lockBoxes.forEach(
                (slot, box) -> {
                    if (box.isSelected()) selected.add(slot);
                });
        return selected;
    }

    private String fingerprint() {
        return profile.get()
                + "/"
                + CalculationInputs.key(current(), model.get().target, model.get().encounter)
                + "/"
                + Scenario.JSON.toJson(current().wikiSeeds)
                + "/"
                + locked()
                + "/"
                + unverified.isSelected()
                + "/"
                + known.isSelected()
                + "/"
                + combatType.getSelectedItem();
    }

    private void start() {
        try {
            cancel();
            String key = profile.get();
            OwnedEquipment owned = ownership.get();
            if (!owned.ready(key))
                throw new IllegalArgumentException(
                        "Open your bank to scan this character's equipment.");
            if (model.get().target == null)
                throw new IllegalArgumentException("Choose a target above before generating.");
            request =
                    new BankOptimizer.Request(
                            current(),
                            model.get().target,
                            owned,
                            locked(),
                            unverified.isSelected(),
                            (String) combatType.getSelectedItem(),
                            current(),
                            model.get().encounter,
                            known.isSelected());
            fingerprint = fingerprint();
            long job = ++generation;
            result = null;
            preview.setVisible(false);
            generate.setEnabled(false);
            cancel.setEnabled(true);
            setStatus("Searching " + request.combatType.toLowerCase(Locale.ROOT) + " setups…");
            BankOptimizer.Request searchRequest = request;
            pending =
                    worker.submit(
                            () -> {
                                try {
                                    BankOptimizer.Result found =
                                            optimizer.search(
                                                    searchRequest,
                                                    count ->
                                                            SwingUtilities.invokeLater(
                                                                    () -> {
                                                                        if (!disposed
                                                                                && generation
                                                                                        == job)
                                                                            setStatus(
                                                                                    "Checked "
                                                                                            + count
                                                                                            + " setups…");
                                                                    }));
                                    SwingUtilities.invokeLater(
                                            () -> {
                                                if (disposed || generation != job) return;
                                                pending = null;
                                                cancel.setEnabled(false);
                                                generate.setEnabled(true);
                                                if (!inputsMatch()) {
                                                    setStatus("Inputs changed. Generate again.");
                                                    return;
                                                }
                                                result = found;
                                                binding = true;
                                                alternatives.removeAllItems();
                                                for (int i = 0; i < found.alternatives.size(); i++)
                                                    alternatives.addItem(
                                                            i == 0
                                                                    ? "Best setup found"
                                                                    : "Alternative " + (i + 1));
                                                binding = false;
                                                preview.setVisible(true);
                                                showAlternative();
                                                setStatus(
                                                        (found.exhaustive
                                                                        ? "All eligible"
                                                                                + " combinations"
                                                                                + " checked"
                                                                        : "Best found · bounded"
                                                                                + " search")
                                                                + " · "
                                                                + found.evaluated
                                                                + " evaluated");
                                                resize();
                                            });
                                } catch (CancellationException ignored) {
                                } catch (RuntimeException ex) {
                                    SwingUtilities.invokeLater(
                                            () -> {
                                                if (disposed || generation != job) return;
                                                pending = null;
                                                cancel.setEnabled(false);
                                                generate.setEnabled(true);
                                                setStatus(ex.getMessage());
                                            });
                                }
                            });
        } catch (RuntimeException ex) {
            setStatus(ex.getMessage());
        }
    }

    private boolean inputsMatch() {
        try {
            return request != null
                    && Objects.equals(fingerprint, fingerprint())
                    && ownership.get().revision == request.owned.revision
                    && ownership.get().ready(profile.get());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private boolean validResult() {
        return result != null && inputsMatch();
    }

    private void invalidateResult() {
        if (pending != null || result != null) {
            cancel();
            result = null;
            preview.setVisible(false);
            setStatus("Inputs changed. Generate again.");
            resize();
        }
    }

    private void cancel() {
        generation++;
        if (pending != null) {
            pending.cancel(true);
            pending = null;
            setStatus("Search cancelled.");
        }
        cancel.setEnabled(false);
        generate.setEnabled(true);
    }

    private void showAlternative() {
        int index = alternatives.getSelectedIndex();
        if (result == null || index < 0) return;
        ScenarioCalculator.Result calculation = result.calculations.get(index);
        Scenario.Loadout draft = result.alternatives.get(index);
        Double baseline = request.encounter.grouped ? result.baselineGroupDps : result.baselineDps;
        String delta =
                baseline == null
                        ? ""
                        : String.format(
                                Locale.ROOT,
                                "\n%+.3f %s vs starting draft",
                                calculation.rankingDps() - baseline,
                                request.encounter.grouped ? "group DPS" : "DPS");
        setText(
                metrics,
                String.format(
                        Locale.ROOT,
                        "DPS %.3f · Max hit %d\nAccuracy %.1f%%%s",
                        calculation.normal.getDps(),
                        calculation.normal.getMaxHit(),
                        calculation.normal.getAccuracy() * 100,
                        delta));
        if (request.encounter.grouped)
            setText(
                    metrics,
                    String.format(
                            Locale.ROOT,
                            "DPS / monster %.3f · Max hit %d\n"
                                    + "Est. group DPS %.3f\n"
                                    + "Hits %d of %d grouped targets\n"
                                    + "Accuracy %.1f%%%s",
                            calculation.normal.getDps(),
                            calculation.normal.getMaxHit(),
                            calculation.groupDps,
                            calculation.targetsHit,
                            calculation.encounterTargets,
                            calculation.normal.getAccuracy() * 100,
                            delta));
        gearPreview.showLoadout(draft, request.owned, profile.get());
        setText(
                attackStyle,
                draft.player.getCombatStyle().getName()
                        + " · "
                        + draft.player.getCombatStyle().getAttackType()
                        + (draft.player.getSpellName() == null
                                ? (request.combatType.equals("Magic")
                                        ? "\nPowered weapon attack"
                                        : "")
                                : "\n"
                                        + draft.player.getSpellName()
                                        + " · "
                                        + draft.player.getSpellbook()
                                        + " spellbook"));
        List<String> checks = new ArrayList<>(calculation.limitations);
        if (request.combatType.equals("Magic"))
            checks.add(
                    "Check spellbook access, quest unlocks and rune supply for "
                            + Objects.toString(draft.player.getSpellName(), "this powered weapon")
                            + ".");
        checks.add("Charges, loaded ammunition and quest unlocks are not fully verified.");
        setText(assumptions, String.join("\n", checks));
        add.setEnabled(validResult());
        resize();
    }

    private void add() {
        try {
            if (!validResult()) {
                invalidateResult();
                return;
            }
            Scenario.Loadout draft =
                    result.alternatives.get(alternatives.getSelectedIndex()).copy();
            draft.carry =
                    Scenario.JSON.fromJson(Scenario.JSON.toJson(current().carry), CarryPlan.class);
            addDraft.accept(draft);
            close();
            setStatus("Added an independent comparison loadout.");
            resize();
        } catch (RuntimeException ex) {
            setStatus(ex.getMessage());
        }
    }

    public void refresh() {
        if (disposed) return;
        try {
            String key = profile.get();
            OwnedEquipment owned = ownership.get();
            if (!Objects.equals(savedProfile, key)) {
                savedProfile = key;
                lastSavedScan = -1;
                lockBoxes.values().forEach(box -> box.setSelected(false));
                unverified.setSelected(false);
                known.setSelected(false);
                binding = true;
                combatType.setSelectedItem(BankOptimizer.family(current()));
                binding = false;
                invalidateResult();
            }
            if ((pending != null || result != null) && !inputsMatch()) invalidateResult();
            String scan;
            if (owned.ready(key)) {
                scan =
                        "Bank scanned "
                                + time(owned.bankScannedAt)
                                + " · "
                                + owned.quantities.size()
                                + " owned item types";
                if (lastSavedScan != owned.bankScannedAt) {
                    storage.setConfiguration(
                            "wikiDpsScenarios",
                            key,
                            "bankScanMetadataV1",
                            Long.toString(owned.bankScannedAt));
                    lastSavedScan = owned.bankScannedAt;
                }
            } else {
                String stored =
                        storage.getConfiguration("wikiDpsScenarios", key, "bankScanMetadataV1");
                if (stored == null)
                    stored = storage.getConfiguration("wikiDpsScenarios", key, "bankSnapshotV1");
                scan =
                        stored == null
                                ? "Open your bank to scan equipment."
                                : "Saved bank scan is old. Open your bank to refresh.";
            }
            String target =
                    model.get().target == null
                            ? "Choose a target above"
                            : model.get().target.getName();
            setText(
                    context,
                    target
                            + " · "
                            + combatType.getSelectedItem()
                            + "\n"
                            + model.get().encounter.summary()
                            + "\nStarting settings: "
                            + current().name
                            + "\nPrayers: "
                            + current().player.getActivePrayers()
                            + "\nPotions: "
                            + (current().potions.isEmpty() ? current().boost : current().potions)
                            + "\nSlayer task: "
                            + current().player.isOnSlayerTask()
                            + "\n"
                            + scan);
            for (int i = 0; i < BankOptimizer.SLOTS.length; i++) {
                int slot = BankOptimizer.SLOTS[i];
                String item =
                        Objects.toString(current().player.getEquippedItemNames()[slot], "Empty");
                lockBoxes
                        .get(slot)
                        .setToolTipText("Keep " + BankOptimizer.SLOT_NAMES[i] + ": " + item);
            }
            generate.setEnabled(pending == null && owned.ready(key) && model.get().target != null);
            add.setEnabled(validResult());
        } catch (RuntimeException ex) {
            invalidateResult();
            generate.setEnabled(false);
            setText(context, "Waiting for the character profile…");
        }
    }

    private static String time(long milliseconds) {
        return Instant.ofEpochMilli(milliseconds)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private void setStatus(String value) {
        setText(status, value == null ? "Search unavailable" : value);
        resize();
    }

    private static void setText(JTextArea area, String value) {
        if (value.equals(area.getText())) return;
        area.setText(value);
        int rows = 0;
        for (String line : value.split("\n", -1)) rows += Math.max(1, (line.length() + 29) / 30);
        area.setRows(rows);
    }

    private void resize() {
        for (Container p = this; p != null; p = p.getParent()) p.invalidate();
        revalidate();
    }

    public void dispose() {
        disposed = true;
        cancel();
        worker.shutdownNow();
    }
}
