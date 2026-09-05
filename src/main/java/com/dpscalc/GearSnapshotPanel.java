package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.GearSnapshot;
import com.dpscalc.state.PlayerState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import com.dpscalc.state.Prayer;
import com.dpscalc.state.CombatStyle;
import java.util.EnumSet;
import java.util.Set;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

public class GearSnapshotPanel extends JPanel {
    private GearSnapshot snapshot;
    private final DpsCalcPlugin plugin;
    private final DpsCalcPanel parent;
    private MonsterStats currentMonster;
    private boolean expanded = false;
    private final JPanel collapsedView;
    private final JPanel expandedView;
    private DpsResult cachedDpsResult;
    private final CardLayout cardLayout;
    private JLabel dpsLabel;
    private JTabbedPane tabbedPane;
    private JPanel statsPanel;

    public GearSnapshotPanel(GearSnapshot snapshot, DpsCalcPlugin plugin, DpsCalcPanel parent, MonsterStats currentMonster) {
        this.snapshot = snapshot;
        this.plugin = plugin;
        this.parent = parent;
        this.currentMonster = currentMonster;

        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

        cardLayout = new CardLayout();
        setLayout(cardLayout);

        collapsedView = new JPanel();
        createCollapsedView();

        expandedView = new JPanel(new BorderLayout());
        expandedView.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        tabbedPane.addTab("Equipment", createEquipmentTab());
        tabbedPane.addTab("Prayer", createPrayerTab());
        tabbedPane.addTab("Style", createStyleTab());

        expandedView.add(tabbedPane, BorderLayout.CENTER);

        // Create bottom panel to hold stats and update button
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        statsPanel = createStatsPanel();
        bottomPanel.add(statsPanel);
        bottomPanel.add(Box.createVerticalStrut(10));

        // Add "Update from Current Equipment" button
        JButton updateButton = new JButton("Update from Current Equipment");
        updateButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        updateButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        updateButton.setForeground(Color.WHITE);
        updateButton.setFocusable(false);
        updateButton.addActionListener(e -> {
            PlayerState current = plugin.getCachedPlayerState();
            if (current == null) return;

            // Create updated snapshot using copyFrom (preserve name, prayers, settings, timestamp)
            GearSnapshot updated = new GearSnapshotBuilder()
                .copyFrom(snapshot)
                .withEquipment(current.getEquippedItemIds(), current.getEquippedItemNames())
                .build();

            // Update snapshot
            parent.replaceSnapshot(snapshot, updated);
            updateSnapshot(updated);
        });
        bottomPanel.add(updateButton);
        bottomPanel.add(Box.createVerticalStrut(10));

        expandedView.add(bottomPanel, BorderLayout.SOUTH);

        add(collapsedView, "collapsed");
        add(expandedView, "expanded");

        recalculateDps();
    }

    private void createCollapsedView() {
        collapsedView.setLayout(new BorderLayout());
        collapsedView.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        collapsedView.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel nameLabel = new JLabel(snapshot.getName());
        nameLabel.setForeground(Color.WHITE);
        collapsedView.add(nameLabel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        centerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        int[] keySlots = {3, 4, 7};
        for (int slot : keySlots) {
            int[] equippedIds = snapshot.getEquippedItemIds();
            if (slot < equippedIds.length) {
                int itemId = equippedIds[slot];
                if (itemId != -1) {
                    BufferedImage image = plugin.getItemManager().getImage(itemId);
                    if (image != null) {
                        BufferedImage scaled = ImageUtil.resizeImage(image, 16, 16);
                        centerPanel.add(new JLabel(new ImageIcon(scaled)));
                    }
                }
            }
        }

        int prayerCount = snapshot.getActivePrayers().size();
        if (prayerCount > 0) {
            JLabel prayerLabel = new JLabel(prayerCount + " prayers");
            prayerLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            prayerLabel.setFont(new Font("Arial", Font.PLAIN, 10));
            centerPanel.add(prayerLabel);
        }

        collapsedView.add(centerPanel, BorderLayout.CENTER);

        // Create right panel with DPS label and reorder buttons
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        dpsLabel = new JLabel("-");
        dpsLabel.setForeground(ColorScheme.BRAND_ORANGE);
        dpsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        rightPanel.add(dpsLabel, BorderLayout.CENTER);

        // Create button panel for reorder buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton upButton = new JButton("↑");
        upButton.setPreferredSize(new Dimension(20, 20));
        upButton.setFont(new Font("Arial", Font.PLAIN, 10));
        upButton.setFocusable(false);
        upButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        upButton.setForeground(Color.WHITE);
        upButton.addActionListener(e -> parent.moveSnapshotUp(snapshot));
        buttonPanel.add(upButton);

        JButton downButton = new JButton("↓");
        downButton.setPreferredSize(new Dimension(20, 20));
        downButton.setFont(new Font("Arial", Font.PLAIN, 10));
        downButton.setFocusable(false);
        downButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        downButton.setForeground(Color.WHITE);
        downButton.addActionListener(e -> parent.moveSnapshotDown(snapshot));
        buttonPanel.add(downButton);

        rightPanel.add(buttonPanel, BorderLayout.EAST);
        collapsedView.add(rightPanel, BorderLayout.EAST);

        collapsedView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                toggleExpanded();
            }
        });
    }

    public void updateMonster(MonsterStats monster) {
        this.currentMonster = monster;
        recalculateDps();
    }

    public void toggleExpanded() {
        expanded = !expanded;
        cardLayout.show(this, expanded ? "expanded" : "collapsed");
    }

    public void recalculateDps() {
        if (currentMonster == null) {
            cachedDpsResult = null;
            updateDpsLabel();
            return;
        }

        PlayerState playerState = plugin.snapshotToPlayerState(snapshot, currentMonster);
        if (playerState == null) {
            cachedDpsResult = null;
            updateDpsLabel();
            return;
        }

        DpsCalculator calculator = new DpsCalculator(playerState, currentMonster);
        cachedDpsResult = calculator.calculate();
        cachedDpsResult.setMonsterHp(currentMonster.getHitpoints());

        updateDpsLabel();
    }

    private void updateDpsLabel() {
        if (dpsLabel != null) {
            if (cachedDpsResult == null) {
                dpsLabel.setText("-");
            } else {
                dpsLabel.setText(String.format("%.2f", cachedDpsResult.getDps()));
            }
            dpsLabel.revalidate();
            dpsLabel.repaint();
        }
        updateStatsPanel();
    }

    /**
     * Updates this panel with a new snapshot, refreshing all views.
     * Called when the snapshot is modified (e.g., prayer change, style change, equipment update).
     * Preserves the currently selected tab in the expanded view.
     *
     * @param newSnapshot The updated snapshot to display
     */
    public void updateSnapshot(GearSnapshot newSnapshot) {
        this.snapshot = newSnapshot;

        collapsedView.removeAll();
        createCollapsedView();
        collapsedView.revalidate();
        collapsedView.repaint();

        if (tabbedPane != null) {
            int selectedIndex = tabbedPane.getSelectedIndex();
            tabbedPane.removeAll();
            tabbedPane.addTab("Equipment", createEquipmentTab());
            tabbedPane.addTab("Prayer", createPrayerTab());
            tabbedPane.addTab("Style", createStyleTab());
            if (selectedIndex >= 0 && selectedIndex < tabbedPane.getTabCount()) {
                tabbedPane.setSelectedIndex(selectedIndex);
            }
        }

        recalculateDps();
    }

    private JPanel createEquipmentTab() {
        JPanel panel = new JPanel(new GridLayout(5, 3, 5, 5));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // OSRS equipment layout mapping
        int[][] slotLayout = {
            {-1, 0, -1},      // Row 1: [null, HEAD, null]
            {1, 2, 13},       // Row 2: [CAPE, AMULET, AMMO]
            {3, 4, 5},        // Row 3: [WEAPON, BODY, SHIELD]
            {-1, 7, -1},      // Row 4: [null, LEGS, null]
            {9, 10, 12}       // Row 5: [GLOVES, BOOTS, RING]
        };

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 3; col++) {
                int slot = slotLayout[row][col];
                JLabel label;

                if (slot == -1) {
                    // Empty cell
                    label = new JLabel();
                    label.setPreferredSize(new Dimension(36, 36));
                } else {
                    // Equipment slot
                    int itemId = snapshot.getEquippedItemIds()[slot];
                    String itemName = snapshot.getEquippedItemNames()[slot];

                    if (itemId != -1) {
                        // Item equipped
                        BufferedImage image = plugin.getItemManager().getImage(itemId);
                        if (image != null) {
                            BufferedImage scaled = ImageUtil.resizeImage(image, 36, 36);
                            label = new JLabel(new ImageIcon(scaled));
                        } else {
                            // Fallback if image not found
                            label = createPlaceholderLabel();
                        }
                        label.setToolTipText(itemName);
                    } else {
                        // Empty slot
                        label = createPlaceholderLabel();
                        label.setToolTipText("Empty");
                    }

                    label.setPreferredSize(new Dimension(36, 36));
                    label.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
                }

                panel.add(label);
            }
        }

        return panel;
    }

    private JLabel createPlaceholderLabel() {
        BufferedImage placeholder = new BufferedImage(36, 36, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(ColorScheme.DARK_GRAY_COLOR);
        g.fillRect(0, 0, 36, 36);
        g.dispose();
        return new JLabel(new ImageIcon(placeholder));
    }

    private JPanel createPrayerTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JCheckBox useBestCheckbox = new JCheckBox("Auto-select best offensive prayer");
        useBestCheckbox.setSelected(snapshot.isUseBestOffensivePrayer());
        useBestCheckbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        useBestCheckbox.setForeground(Color.WHITE);
        useBestCheckbox.addActionListener(e -> {
            GearSnapshot updated = new GearSnapshotBuilder()
                .copyFrom(snapshot)
                .withUseBestPrayer(useBestCheckbox.isSelected())
                .build();
            parent.replaceSnapshot(snapshot, updated);
            updateSnapshot(updated);
        });
        panel.add(useBestCheckbox);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));

        addPrayerGroup(panel, "Melee Prayers", new Prayer[]{
            Prayer.BURST_OF_STRENGTH, Prayer.SUPERHUMAN_STRENGTH,
            Prayer.ULTIMATE_STRENGTH, Prayer.CHIVALRY, Prayer.PIETY
        }, useBestCheckbox);

        addPrayerGroup(panel, "Ranged Prayers", new Prayer[]{
            Prayer.SHARP_EYE, Prayer.HAWK_EYE,
            Prayer.EAGLE_EYE, Prayer.RIGOUR
        }, useBestCheckbox);

        addPrayerGroup(panel, "Magic Prayers", new Prayer[]{
            Prayer.MYSTIC_WILL, Prayer.MYSTIC_LORE,
            Prayer.MYSTIC_MIGHT, Prayer.AUGURY
        }, useBestCheckbox);

        return panel;
    }

    private void addPrayerGroup(JPanel panel, String title, Prayer[] prayers, JCheckBox useBestCheckbox) {
        JLabel groupLabel = new JLabel(title);
        groupLabel.setForeground(ColorScheme.BRAND_ORANGE);
        groupLabel.setFont(new Font("Arial", Font.BOLD, 12));
        panel.add(groupLabel);
        panel.add(Box.createVerticalStrut(5));

        for (Prayer prayer : prayers) {
            JCheckBox checkbox = new JCheckBox(prayer.toString());
            checkbox.setSelected(snapshot.getActivePrayers().contains(prayer));
            checkbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            checkbox.setForeground(Color.WHITE);
            checkbox.setEnabled(!snapshot.isUseBestOffensivePrayer());

            checkbox.addActionListener(e -> {
                Set<Prayer> newPrayers = EnumSet.copyOf(snapshot.getActivePrayers());
                if (checkbox.isSelected()) {
                    newPrayers.add(prayer);
                } else {
                    newPrayers.remove(prayer);
                }

                GearSnapshot updated = new GearSnapshotBuilder()
                    .copyFrom(snapshot)
                    .withPrayers(newPrayers.toArray(new Prayer[0]))
                    .build();
                parent.replaceSnapshot(snapshot, updated);
                updateSnapshot(updated);
            });

            panel.add(checkbox);
        }

        panel.add(Box.createVerticalStrut(10));
    }

    private JPanel createStyleTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Combat Style");
        titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));

        ButtonGroup styleGroup = new ButtonGroup();

        CombatStyle[] commonStyles = {
            CombatStyle.MELEE_ACCURATE_SLASH,
            CombatStyle.MELEE_AGGRESSIVE_SLASH,
            CombatStyle.MELEE_CONTROLLED_SLASH,
            CombatStyle.MELEE_DEFENSIVE_SLASH,
            CombatStyle.RANGED_ACCURATE,
            CombatStyle.RANGED_RAPID,
            CombatStyle.RANGED_LONGRANGE,
            CombatStyle.MAGIC_ACCURATE,
            CombatStyle.MAGIC_LONGRANGE
        };

        String currentStyleName = snapshot.getCombatStyleName();
        String currentStyleStance = snapshot.getCombatStyleStance();

        for (CombatStyle style : commonStyles) {
            JRadioButton radioButton = new JRadioButton(style.getName() + " (" + style.getStance() + ")");
            radioButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            radioButton.setForeground(Color.WHITE);

            if (style.getName().equals(currentStyleName) && style.getStance().equals(currentStyleStance)) {
                radioButton.setSelected(true);
            }

            radioButton.addActionListener(e -> {
                GearSnapshot updated = new GearSnapshotBuilder()
                    .copyFrom(snapshot)
                    .withCombatStyle(style)
                    .build();
                parent.replaceSnapshot(snapshot, updated);
                updateSnapshot(updated);
            });

            styleGroup.add(radioButton);
            panel.add(radioButton);
        }

        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));

        JLabel speedLabel = new JLabel("Attack Speed: " + getAttackSpeed() + " ticks");
        speedLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        panel.add(speedLabel);

        return panel;
    }

    private int getAttackSpeed() {
        if (cachedDpsResult != null) {
            return cachedDpsResult.getAttackSpeed();
        }
        return 4;
    }

    private void updateStatsPanel() {
        if (expandedView != null) {
            if (statsPanel != null) {
                expandedView.remove(statsPanel);
            }
            statsPanel = createStatsPanel();
            expandedView.add(statsPanel, BorderLayout.SOUTH);
            expandedView.revalidate();
            expandedView.repaint();
        }
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Combat Stats");
        titleLabel.setForeground(ColorScheme.BRAND_ORANGE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));

        if (cachedDpsResult != null) {
            addStatRow(panel, "DPS:", String.format("%.2f", cachedDpsResult.getDps()), true);

            addStatRow(panel, "Max Hit:", String.valueOf(cachedDpsResult.getMaxHit()), false);

            addStatRow(panel, "Accuracy:", String.format("%.1f%%", cachedDpsResult.getAccuracy() * 100), false);

            double ttk = cachedDpsResult.getTimeToKill();
            String ttkStr = ttk < 60 ? String.format("%.1fs", ttk) : String.format("%.1fm", ttk / 60);
            addStatRow(panel, "Time to Kill:", ttkStr, false);

            addStatRow(panel, "Kills/Hr:", String.format("%,d", (int)cachedDpsResult.getKillsPerHour()), false);

            panel.add(Box.createVerticalStrut(10));
            panel.add(new JSeparator());
            panel.add(Box.createVerticalStrut(10));

            JLabel specLabel = new JLabel("Special Attack");
            specLabel.setForeground(ColorScheme.BRAND_ORANGE);
            specLabel.setFont(new Font("Arial", Font.BOLD, 12));
            panel.add(specLabel);
            panel.add(Box.createVerticalStrut(5));

            PlayerState playerState = plugin.snapshotToPlayerState(snapshot, currentMonster);
            if (playerState != null && currentMonster != null) {
                DpsCalculator specCalc = new DpsCalculator(playerState, currentMonster, true);
                DpsResult specResult = specCalc.calculate();

                addStatRow(panel, "Spec DPS:", String.format("%.2f", specResult.getDps()), false);
                addStatRow(panel, "Spec Max Hit:", String.valueOf(specResult.getMaxHit()), false);
                addStatRow(panel, "Spec Accuracy:", String.format("%.1f%%", specResult.getAccuracy() * 100), false);
            }
        } else {
            JLabel noDataLabel = new JLabel("No DPS data available");
            noDataLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            panel.add(noDataLabel);
        }

        panel.add(Box.createVerticalStrut(10));
        panel.add(new JSeparator());
        panel.add(Box.createVerticalStrut(10));

        JCheckBox assumeBoostsCheckbox = new JCheckBox("Calculate with max boosts (+5/+6)");
        assumeBoostsCheckbox.setSelected(snapshot.isAssumeMaxBoosts());
        assumeBoostsCheckbox.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        assumeBoostsCheckbox.setForeground(Color.WHITE);
        assumeBoostsCheckbox.addActionListener(e -> {
            GearSnapshot updated = new GearSnapshotBuilder()
                .copyFrom(snapshot)
                .withAssumeMaxBoosts(assumeBoostsCheckbox.isSelected())
                .build();
            parent.replaceSnapshot(snapshot, updated);
            updateSnapshot(updated);
        });
        panel.add(assumeBoostsCheckbox);

        return panel;
    }

    private void addStatRow(JPanel panel, String label, String value, boolean highlight) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rowPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel nameLabel = new JLabel(label);
        nameLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        rowPanel.add(nameLabel);

        JLabel valueLabel = new JLabel(value);
        if (highlight) {
            valueLabel.setForeground(ColorScheme.BRAND_ORANGE);
            valueLabel.setFont(new Font("Arial", Font.BOLD, 14));
        } else {
            valueLabel.setForeground(Color.WHITE);
        }
        rowPanel.add(valueLabel);

        panel.add(rowPanel);
    }
}
