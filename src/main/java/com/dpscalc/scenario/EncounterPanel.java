package com.dpscalc.scenario;

import java.util.function.Supplier;

import javax.swing.*;

/** Encounter intent belongs with the target and applies to every comparison loadout. */
public final class EncounterPanel extends JPanel {
    private final Supplier<Scenario> model;
    private final Runnable changed;
    private final JComboBox<String> mode =
            new JComboBox<>(new String[] {"Single target", "Grouped targets"});
    private final JSpinner count = new JSpinner(new SpinnerNumberModel(5, 2, 9, 1));
    private final JPanel group = CalculatorWorkspace.column();
    private final JButton suggest;
    private boolean binding;

    public EncounterPanel(Supplier<Scenario> model, Runnable changed) {
        this.model = model;
        this.changed = changed;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(CalculatorWorkspace.BG);
        add(CalculatorWorkspace.label("Encounter", CalculatorWorkspace.GOLD));
        add(mode);
        mode.getAccessibleContext().setAccessibleName("Encounter mode");
        count.getAccessibleContext().setAccessibleName("Grouped target count");
        count.setToolTipText("Number of monsters kept together within burst/barrage range");
        group.add(
                CalculatorWorkspace.pair(
                        CalculatorWorkspace.label("Targets", CalculatorWorkspace.TEXT), count));
        JTextArea note =
                CalculatorWorkspace.note(
                        "Assumes identical monsters grouped in burst/barrage range in multicombat."
                                + " Group DPS excludes gathering and looting time.");
        note.setRows(5);
        group.add(note);
        add(group);
        suggest =
                CalculatorWorkspace.action(
                        "Use grouped encounter",
                        () -> edit(() -> model.get().encounter.grouped = true));
        suggest.setToolTipText(
                "Bursting these monsters together in multicombat? Use a grouped encounter and set"
                        + " the target count.");
        add(suggest);
        mode.addActionListener(
                e -> edit(() -> model.get().encounter.grouped = mode.getSelectedIndex() == 1));
        count.addChangeListener(
                e -> edit(() -> model.get().encounter.targets = (Integer) count.getValue()));
        CalculatorWorkspace.theme(this);
        refresh();
    }

    private void edit(Runnable edit) {
        if (binding) return;
        edit.run();
        model.get().encounter.validate();
        refresh();
        changed.run();
    }

    public void refresh() {
        binding = true;
        try {
            Encounter encounter = model.get().encounter;
            mode.setSelectedIndex(encounter.grouped ? 1 : 0);
            count.setValue(encounter.targets);
            group.setVisible(encounter.grouped);
            String name = model.get().target == null ? "" : model.get().target.getName();
            suggest.setVisible(
                    !encounter.grouped
                            && (name.equalsIgnoreCase("Nechryael")
                                    || name.equalsIgnoreCase("Greater Nechryael")));
        } finally {
            binding = false;
        }
        for (java.awt.Container parent = this; parent != null; parent = parent.getParent())
            parent.invalidate();
        revalidate();
        repaint();
    }
}
