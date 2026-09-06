package com.dpscalc.scenario;

import com.dpscalc.DpsCalcPlugin;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.*;

import javax.swing.*;

/** Visual packing plan; edits apply to the selected draft only. */
final class CarryPlanPanel extends JPanel {
    private final Supplier<Scenario> model;
    private final Supplier<String> profile;
    private final Runnable changed;
    private final DpsCalcPlugin plugin;
    private final JButton[] slots = new JButton[28], runes = new JButton[4];
    private final JPanel switches = CalculatorWorkspace.column();
    private final JTextArea audit = CalculatorWorkspace.note("");
    private final JPanel body = CalculatorWorkspace.column();
    private final JButton toggle;

    CarryPlanPanel(
            Supplier<Scenario> model,
            Supplier<String> profile,
            Runnable changed,
            DpsCalcPlugin plugin) {
        this.model = model;
        this.profile = profile;
        this.changed = changed;
        this.plugin = plugin;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(CalculatorWorkspace.BG);
        toggle = CalculatorWorkspace.action("Prepare loadout ▸", () -> {});
        body.setVisible(false);
        toggle.addActionListener(
                e -> {
                    body.setVisible(!body.isVisible());
                    toggle.setText(body.isVisible() ? "Prepare loadout ▾" : "Prepare loadout ▸");
                    refresh();
                    resizeContent();
                });
        add(toggle);
        add(body);
        body.add(
                CalculatorWorkspace.note(
                        "Plan supplies for this draft. Click a slot to choose an item and quantity."
                                + " ? means the quantity needs setting."));
        body.add(
                CalculatorWorkspace.action(
                        "Wiki source & notes",
                        () -> {
                            Scenario.Loadout draft = current();
                            JTextArea text =
                                    CalculatorWorkspace.note(
                                            draft.wikiSource.isEmpty()
                                                    ? "This draft has no Wiki template source."
                                                    : draft.wikiSource
                                                            + "\nRevision "
                                                            + draft.wikiRevision
                                                            + "\n\n"
                                                            + String.join("\n\n", draft.wikiNotes));
                            JScrollPane scroll = new JScrollPane(text);
                            scroll.setPreferredSize(new Dimension(420, 350));
                            SidebarScrolling.nested(scroll);
                            JOptionPane.showMessageDialog(
                                    this, scroll, "Template provenance", JOptionPane.PLAIN_MESSAGE);
                        }));
        JPanel inventory = new JPanel(new GridLayout(7, 4, 3, 3));
        inventory.setBackground(CalculatorWorkspace.BG);
        for (int i = 0; i < 28; i++) {
            final int slot = i;
            slots[i] = cell("Inventory " + (i + 1), () -> edit(current().carry.slots, slot, false));
            inventory.add(slots[i]);
        }
        body.add(inventory);
        body.add(CalculatorWorkspace.label("Rune pouch", CalculatorWorkspace.GOLD));
        JPanel pouch = new JPanel(new GridLayout(1, 4, 3, 3));
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            runes[i] = cell("Pouch rune " + (i + 1), () -> edit(current().carry.runes, slot, true));
            pouch.add(runes[i]);
        }
        body.add(pouch);
        body.add(CalculatorWorkspace.label("Optional switches", CalculatorWorkspace.GOLD));
        body.add(
                CalculatorWorkspace.note(
                        "Switches are suggestions until you pack them into an empty inventory"
                                + " slot."));
        body.add(switches);
        body.add(
                CalculatorWorkspace.action(
                        "Add switch",
                        () -> {
                            if (current().carry.switches.size() >= 28) return;
                            CarryPlan.Entry selected = choose(null, false);
                            if (selected != null) {
                                current().carry.switches.add(selected);
                                changed.run();
                            }
                        }));
        body.add(CalculatorWorkspace.action("Check supplies", this::refresh));
        body.add(audit);
        CalculatorWorkspace.theme(this);
    }

    void open() {
        body.setVisible(true);
        toggle.setText("Prepare loadout ▾");
        refresh();
        resizeContent();
    }

    void installBankAction(JButton action) {
        body.add(action);
    }

    private Scenario.Loadout current() {
        Scenario scenario = model.get();
        return scenario.loadouts.get(scenario.selected);
    }

    private JButton cell(String name, Runnable action) {
        JButton b = CalculatorWorkspace.action("+", action);
        b.setPreferredSize(new Dimension(45, 48));
        b.setMargin(new Insets(1, 1, 1, 1));
        b.setFont(b.getFont().deriveFont(9f));
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.getAccessibleContext().setAccessibleName(name);
        return b;
    }

    private void edit(CarryPlan.Entry[] entries, int slot, boolean rune) {
        CarryPlan.Entry selected = choose(entries[slot], rune);
        if (selected != null) {
            entries[slot] = selected.id == -1 ? null : selected;
            changed.run();
        }
    }

    private CarryPlan.Entry choose(CarryPlan.Entry old, boolean rune) {
        SetupItemIndex index = plugin.getSetupItemIndex();
        if (index == null || !index.isReady()) {
            JOptionPane.showMessageDialog(this, "Item names are still loading. Try again shortly.");
            return null;
        }
        JPanel form = CalculatorWorkspace.column();
        JTextField search = new JTextField(old == null ? "" : old.name);
        JComboBox<Item> results = new JComboBox<>();
        JSpinner quantity =
                new JSpinner(
                        new SpinnerNumberModel(
                                old == null ? 1 : old.quantity, 0, Integer.MAX_VALUE, 1));
        Runnable find =
                () -> {
                    results.removeAllItems();
                    for (int id : index.search(search.getText(), 30)) {
                        String name = index.nameById(id);
                        if (!rune || name.toLowerCase(Locale.ROOT).endsWith(" rune"))
                            results.addItem(new Item(id, name));
                    }
                };
        search.addActionListener(e -> find.run());
        form.add(
                CalculatorWorkspace.label("Item name · Enter to search", CalculatorWorkspace.GOLD));
        form.add(search);
        form.add(CalculatorWorkspace.action("Search items", find));
        form.add(results);
        form.add(CalculatorWorkspace.label("Quantity (0 = unknown)", CalculatorWorkspace.MUTED));
        form.add(quantity);
        find.run();
        if (old != null)
            for (int i = 0; i < results.getItemCount(); i++)
                if (results.getItemAt(i).id == old.id) results.setSelectedIndex(i);
        int answer =
                JOptionPane.showOptionDialog(
                        this,
                        form,
                        rune ? "Plan pouch rune" : "Plan inventory item",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        new String[] {"Save", "Clear slot", "Cancel"},
                        "Save");
        if (answer == 1) return new CarryPlan.Entry(-1, "", 0);
        if (answer != 0 || results.getSelectedItem() == null) return null;
        try {
            quantity.commitEdit();
        } catch (java.text.ParseException e) {
            return null;
        }
        Item item = (Item) results.getSelectedItem();
        int count = ((Number) quantity.getValue()).intValue();
        if (!index.stackable(item.id) && count > 1) {
            JOptionPane.showMessageDialog(
                    this, "This item is not stackable. Put each copy in its own inventory slot.");
            return null;
        }
        return new CarryPlan.Entry(item.id, item.name, count);
    }

    void refresh() {
        Scenario.Loadout draft = current();
        OwnedEquipment owned = plugin.getOwnedEquipment();
        String expected = profile.get();
        for (int i = 0; i < 28; i++) paint(slots[i], draft.carry.slots[i], owned, expected);
        for (int i = 0; i < 4; i++) paint(runes[i], draft.carry.runes[i], owned, expected);
        switches.removeAll();
        for (int i = 0; i < draft.carry.switches.size(); i++) {
            final int pos = i;
            CarryPlan.Entry entry = draft.carry.switches.get(i);
            if (entry == null) continue;
            JButton pack =
                    CalculatorWorkspace.action(
                            "Pack",
                            () -> {
                                for (int slot = 0; slot < 28; slot++)
                                    if (current().carry.slots[slot] == null) {
                                        current().carry.slots[slot] = entry.copy();
                                        changed.run();
                                        return;
                                    }
                                JOptionPane.showMessageDialog(
                                        this, "Inventory is full. Clear a slot first.");
                            });
            pack.setToolTipText("Copy this switch into an empty inventory slot");
            JButton remove =
                    CalculatorWorkspace.action(
                            "×",
                            () -> {
                                current().carry.switches.remove(pos);
                                changed.run();
                            });
            JLabel name = CalculatorWorkspace.label(entry.name, CalculatorWorkspace.TEXT);
            name.setToolTipText(entry.note);
            switches.add(name);
            switches.add(CalculatorWorkspace.pair(pack, remove));
        }
        List<String> notes = new ArrayList<>(draft.carry.audit(draft, owned, expected));
        SetupItemIndex index = plugin.getSetupItemIndex();
        for (CarryPlan.Entry entry : draft.carry.slots)
            if (entry != null) {
                if (!entry.note.isEmpty()) notes.add(entry.name + ": " + entry.note);
                if (index != null
                        && index.isReady()
                        && entry.id > 0
                        && entry.quantity > 1
                        && !entry.noted
                        && !index.stackable(entry.id))
                    notes.add(
                            entry.name
                                    + ": does not stack; use separate slots (or verify a noted"
                                    + " restock stack).");
            }
        CalculatorWorkspace.theme(switches);
        audit.setText(String.join("\n", notes));
        audit.setRows(Math.min(12, Math.max(3, notes.size() * 2)));
        resizeContent();
        repaint();
    }

    private void resizeContent() {
        for (Container parent = this; parent != null; parent = parent.getParent())
            parent.invalidate();
        super.revalidate();
    }

    private void paint(
            JButton button, CarryPlan.Entry entry, OwnedEquipment owned, String expected) {
        button.setIcon(null);
        button.setText(
                entry == null
                        ? "+"
                        : (entry.quantity == 0 ? "?" : Integer.toString(entry.quantity)));
        button.setToolTipText(
                entry == null
                        ? "Empty slot — click to choose"
                        : entry.name
                                + " · "
                                + (entry.id <= 0
                                        ? "unrecognized"
                                        : !owned.ready(expected)
                                                ? "ownership unknown"
                                                : "own "
                                                        + owned.quantities.getOrDefault(
                                                                entry.id, 0))
                                + " · "
                                + entry.note);
        button.setBorder(
                BorderFactory.createLineBorder(
                        entry == null
                                ? CalculatorWorkspace.EDGE
                                : !owned.ready(expected)
                                        ? CalculatorWorkspace.MUTED
                                        : entry.id > 0 && owned.owns(entry.id)
                                                ? CalculatorWorkspace.BEST
                                                : new Color(218, 126, 106)));
        if (entry != null && entry.id > 0 && plugin.getItemManager() != null) {
            net.runelite.client.util.AsyncBufferedImage image =
                    plugin.getItemManager().getImage(entry.id);
            button.setIcon(new ImageIcon(image));
            image.onLoaded(button::repaint);
        } else if (entry != null)
            button.setText(
                    "<html><center>"
                            + CalculatorWorkspace.escape(
                                    entry.name.substring(0, Math.min(8, entry.name.length())))
                            + "<br>"
                            + button.getText()
                            + "</center></html>");
    }

    private static final class Item {
        final int id;
        final String name;

        Item(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public String toString() {
            return name + " (" + id + ")";
        }
    }
}
