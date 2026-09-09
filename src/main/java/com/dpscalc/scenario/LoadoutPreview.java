package com.dpscalc.scenario;

import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

import java.awt.*;
import java.util.*;
import java.util.function.Supplier;

import javax.swing.*;

/** Read-only equipment figure with the same slot arrangement as the loadout editor. */
public final class LoadoutPreview extends JPanel {
    private static final int[] LAYOUT = {-1, 0, -1, 1, 2, 13, 3, 4, 5, -1, 7, -1, 9, 10, 12};
    private static final String[] ICONS = {
        "head", "cape", "neck", "weapon", "body", "shield", "legs", "hands", "feet", "ring", "ammo"
    };
    private final Map<Integer, JLabel> slots = new LinkedHashMap<>();
    private final Supplier<ItemManager> items;

    public LoadoutPreview(Supplier<ItemManager> items) {
        this.items = items;
        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 4));
        setBackground(CalculatorWorkspace.BG);
        getAccessibleContext().setAccessibleName("Generated equipment preview");
        JPanel grid = new JPanel(new GridLayout(5, 3, 5, 5));
        grid.setBackground(CalculatorWorkspace.BG);
        grid.setPreferredSize(new Dimension(154, 240));
        for (int slot : LAYOUT) {
            if (slot < 0) {
                JPanel blank = new JPanel();
                blank.setOpaque(false);
                grid.add(blank);
                continue;
            }
            JLabel label = new JLabel("", SwingConstants.CENTER);
            label.setOpaque(true);
            label.setBackground(CalculatorWorkspace.CARD);
            label.setForeground(CalculatorWorkspace.TEXT);
            label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
            label.setHorizontalTextPosition(SwingConstants.CENTER);
            label.setVerticalTextPosition(SwingConstants.BOTTOM);
            label.setBorder(BorderFactory.createLineBorder(CalculatorWorkspace.EDGE));
            slots.put(slot, label);
            grid.add(label);
        }
        add(grid);
    }

    public void showLoadout(Scenario.Loadout loadout) {
        showLoadout(loadout, OwnedEquipment.empty(0), null);
    }

    public void showLoadout(Scenario.Loadout loadout, OwnedEquipment owned, String profile) {
        for (int i = 0; i < BankOptimizer.SLOTS.length; i++) {
            int slot = BankOptimizer.SLOTS[i], id = loadout.player.getEquippedItemIds()[slot];
            JLabel label = slots.get(slot);
            String name =
                    id > 0
                            ? Objects.toString(
                                    loadout.player.getEquippedItemNames()[slot], "Item " + id)
                            : "Empty";
            String description = BankOptimizer.SLOT_NAMES[i] + ": " + name;
            label.setToolTipText(description);
            label.getAccessibleContext().setAccessibleName("Suggested " + description);
            label.putClientProperty("itemId", id);
            label.setText("");
            label.setIcon(CalculatorWorkspace.icon("slots/" + ICONS[i], 30));
            if (id > 0) {
                String ownership =
                        !owned.ready(profile) ? "Unknown" : owned.owns(id) ? "Owned" : "Missing";
                label.setText(ownership);
                label.setBorder(
                        BorderFactory.createLineBorder(
                                !owned.ready(profile)
                                        ? CalculatorWorkspace.EDGE
                                        : owned.owns(id)
                                                ? CalculatorWorkspace.BEST
                                                : new Color(218, 126, 106)));
                label.setToolTipText(
                        description
                                + " — "
                                + ownership.toLowerCase(Locale.ROOT)
                                + (owned.ready(profile)
                                        ? " (" + owned.quantities.getOrDefault(id, 0) + ")"
                                        : "; open this character's bank"));
                ItemManager manager = items.get();
                if (manager != null) {
                    AsyncBufferedImage sprite = manager.getImage(id);
                    label.setIcon(new ImageIcon(sprite));
                    sprite.onLoaded(label::repaint);
                } else {
                    label.setIcon(CalculatorWorkspace.icon("slots/" + ICONS[i], 16));
                    label.setText(
                            "<html><center>"
                                    + CalculatorWorkspace.escape(
                                            name.substring(0, Math.min(8, name.length())))
                                    + "<br>"
                                    + ownership
                                    + "</center></html>");
                }
            }
        }
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
