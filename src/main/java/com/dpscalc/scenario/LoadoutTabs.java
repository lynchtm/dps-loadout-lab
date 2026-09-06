package com.dpscalc.scenario;

import java.awt.*;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/** Compact numbered tabs, with native keyboard navigation and overflow scrolling. */
final class LoadoutTabs extends JTabbedPane {
    private final IntConsumer remove;

    LoadoutTabs(IntConsumer remove) {
        super(TOP, SCROLL_TAB_LAYOUT);
        this.remove = remove;
        getAccessibleContext().setAccessibleName("Comparison loadouts");
        setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        setBackground(CalculatorWorkspace.BG);
        setForeground(CalculatorWorkspace.TEXT);
        setUI(
                new BasicTabbedPaneUI() {
                    protected int calculateTabWidth(int placement, int index, FontMetrics metrics) {
                        return 60;
                    }

                    protected int calculateTabHeight(int placement, int index, int height) {
                        return 30;
                    }

                    protected void paintTabBackground(
                            Graphics g,
                            int placement,
                            int index,
                            int x,
                            int y,
                            int w,
                            int h,
                            boolean selected) {
                        g.setColor(
                                selected ? CalculatorWorkspace.ORANGE : CalculatorWorkspace.CARD);
                        g.fillRect(x, y, w, h);
                        if (selected) {
                            g.setColor(CalculatorWorkspace.ORANGE);
                            g.fillRect(x, y + h - 3, w, 3);
                        }
                    }

                    protected void paintTabBorder(
                            Graphics g,
                            int p,
                            int i,
                            int x,
                            int y,
                            int w,
                            int h,
                            boolean selected) {}

                    protected void paintContentBorder(Graphics g, int placement, int selected) {}

                    protected void paintFocusIndicator(
                            Graphics g,
                            int p,
                            Rectangle[] r,
                            int i,
                            Rectangle icon,
                            Rectangle text,
                            boolean selected) {
                        if (hasFocus() && selected) {
                            g.setColor(CalculatorWorkspace.GOLD);
                            g.drawRect(r[i].x + 2, r[i].y + 2, r[i].width - 5, r[i].height - 6);
                        }
                    }
                });
    }

    void bind(List<Scenario.Loadout> loadouts, int selected) {
        // Retain tab components on ordinary edits so focus and scrolling remain stable.
        while (getTabCount() > loadouts.size()) removeTabAt(getTabCount() - 1);
        while (getTabCount() < loadouts.size()) {
            int index = getTabCount();
            addTab(String.valueOf(index + 1), new JPanel());
            JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            header.setOpaque(false);
            JButton select = tabButton("", 28), close = tabButton("×", 24);
            select.addActionListener(
                    e -> {
                        int tab = indexOfTabComponent(header);
                        if (tab >= 0) setSelectedIndex(tab);
                    });
            close.addActionListener(
                    e -> {
                        int tab = indexOfTabComponent(header);
                        if (tab >= 0 && getTabCount() > 1) remove.accept(tab);
                    });
            header.add(select);
            header.add(close);
            setTabComponentAt(index, header);
        }
        for (int i = 0; i < loadouts.size(); i++) {
            String name = loadouts.get(i).name;
            JPanel header = (JPanel) getTabComponentAt(i);
            JButton select = (JButton) header.getComponent(0),
                    close = (JButton) header.getComponent(1);
            select.setText(String.valueOf(i + 1));
            select.setToolTipText(name);
            select.getAccessibleContext()
                    .setAccessibleName("Select comparison loadout " + (i + 1) + ": " + name);
            close.setEnabled(loadouts.size() > 1);
            close.setToolTipText(
                    loadouts.size() > 1
                            ? "Remove " + name + " from comparison"
                            : "Keep at least one comparison loadout");
            close.getAccessibleContext()
                    .setAccessibleName("Remove comparison loadout " + (i + 1) + ": " + name);
            setToolTipTextAt(i, name);
            getComponentAt(i).getAccessibleContext().setAccessibleName(loadouts.get(i).name);
        }
        setSelectedIndex(selected);
    }

    private static JButton tabButton(String text, int width) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(width, 26));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusable(false);
        button.setForeground(CalculatorWorkspace.TEXT);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        return button;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(170, 36);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(50, 36);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 36);
    }
}
