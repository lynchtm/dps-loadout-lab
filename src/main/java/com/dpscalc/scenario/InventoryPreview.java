package com.dpscalc.scenario;

import net.runelite.client.game.ItemManager;
import net.runelite.client.util.AsyncBufferedImage;

import java.awt.*;
import java.util.function.Supplier;

import javax.swing.*;

/** The guide's exact packing plan, including unresolved items and unknown quantities. */
final class InventoryPreview extends JPanel {
    private final Supplier<ItemManager> items;
    private final String heading;

    InventoryPreview(Supplier<ItemManager> items) {
        this(items, "Inventory from guide");
    }

    InventoryPreview(Supplier<ItemManager> items, String heading) {
        this.items = items;
        this.heading = heading;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(CalculatorWorkspace.BG);
        getAccessibleContext()
                .setAccessibleName(
                        heading.equals("Inventory from guide")
                                ? "Wiki inventory preview"
                                : heading);
    }

    void showPlan(CarryPlan plan) {
        removeAll();
        add(CalculatorWorkspace.label(heading, CalculatorWorkspace.GOLD));
        JPanel inventory = new JPanel(new GridLayout(7, 4, 3, 3));
        inventory.setOpaque(false);
        for (int i = 0; i < 28; i++)
            inventory.add(cell(plan.slots[i], "Suggested inventory " + (i + 1)));
        add(inventory);
        add(CalculatorWorkspace.label("Rune pouch", CalculatorWorkspace.GOLD));
        JPanel runes = new JPanel(new GridLayout(1, 4, 3, 3));
        runes.setOpaque(false);
        for (int i = 0; i < 4; i++)
            runes.add(cell(plan.runes[i], "Suggested pouch rune " + (i + 1)));
        add(runes);
        revalidate();
        repaint();
    }

    private JLabel cell(CarryPlan.Entry entry, String name) {
        JLabel cell = new JLabel("—", SwingConstants.CENTER);
        cell.setPreferredSize(new Dimension(42, 48));
        cell.setOpaque(true);
        cell.setBackground(CalculatorWorkspace.CARD);
        cell.setForeground(CalculatorWorkspace.TEXT);
        cell.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 9));
        cell.setBorder(BorderFactory.createLineBorder(CalculatorWorkspace.EDGE));
        cell.setHorizontalTextPosition(SwingConstants.CENTER);
        cell.setVerticalTextPosition(SwingConstants.BOTTOM);
        cell.getAccessibleContext().setAccessibleName(name);
        if (entry == null) {
            cell.setToolTipText("Empty");
            return cell;
        }
        String quantity = entry.quantity == 0 ? "?" : String.valueOf(entry.quantity);
        cell.putClientProperty("itemId", entry.id);
        cell.putClientProperty("quantity", entry.quantity);
        cell.setToolTipText(
                entry.name
                        + " × "
                        + quantity
                        + (entry.noted ? " (noted)" : "")
                        + (entry.id <= 0 ? " — unrecognized item" : ""));
        ItemManager manager = items.get();
        if (entry.id > 0 && manager != null) {
            AsyncBufferedImage sprite = manager.getImage(entry.id);
            cell.setIcon(new ImageIcon(sprite));
            sprite.onLoaded(cell::repaint);
            cell.setText(quantity);
        } else
            cell.setText(
                    "<html><center>"
                            + CalculatorWorkspace.escape(
                                    entry.name.substring(0, Math.min(entry.name.length(), 8)))
                            + "<br>"
                            + quantity
                            + "</center></html>");
        return cell;
    }
}
