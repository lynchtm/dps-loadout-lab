package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterDataManager;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.GearSnapshot;
import com.dpscalc.state.PlayerState;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.IconTextField;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

public class DpsComparisonDialog extends JDialog {

    private final DpsCalcPlugin plugin;
    private final DpsCalcPanel panel;
    private final MonsterDataManager monsterDataManager;
    private final List<GearSnapshot> snapshots;
    private final List<DpsComparison> comparisons;

    private final JTabbedPane tabbedPane;

    private final IconTextField searchField = new IconTextField();
    private final DefaultListModel<MonsterStats> searchListModel = new DefaultListModel<>();
    private final JList<MonsterStats> searchList = new JList<>(searchListModel);
    private final JScrollPane searchScrollPane = new JScrollPane(searchList);
    private final Timer searchTimer;
    private final JLabel topTargetLabel = new JLabel();

    private final JLabel dpsLabel = new JLabel();
    private final JLabel maxHitLabel = new JLabel();
    private final JLabel accuracyLabel = new JLabel();
    private final JLabel ttkLabel = new JLabel();
    private final JLabel killsHrLabel = new JLabel();
    private final JLabel monsterNameLabel = new JLabel("No Monster Selected");

    private JCheckBox useBestPrayerCheckbox;
    private JCheckBox assumeMaxBoostsCheckbox;

    private DefaultTableModel loadoutsModel;
    private JTable loadoutsTable;

    private final JPanel comparisonListPanel = new JPanel();
    private final JButton clearComparisonsButton = new JButton("Clear All");

    private static final Color BRAND_ORANGE = ColorScheme.BRAND_ORANGE;
    private static final Color CARD_BACKGROUND = ColorScheme.DARKER_GRAY_COLOR;
    private static final Color PANEL_BACKGROUND = ColorScheme.DARK_GRAY_COLOR;
    private static final Color VALUE_POSITIVE = new Color(0, 255, 100);
    private static final Color VALUE_NEGATIVE = new Color(255, 80, 80);
    private static final Color VALUE_HIGHLIGHT = new Color(255, 180, 60);
    private static final Color MUTED_TEXT = ColorScheme.MEDIUM_GRAY_COLOR;

    public DpsComparisonDialog(DpsCalcPlugin plugin, DpsCalcPanel panel, List<GearSnapshot> snapshots,
                               List<DpsComparison> comparisons, MonsterDataManager monsterDataManager, Window owner) {
        super(owner, "DPS Gear Comparison", ModalityType.MODELESS);
        this.plugin = plugin;
        this.panel = panel;
        this.snapshots = snapshots;
        this.comparisons = comparisons;
        this.monsterDataManager = monsterDataManager;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setBackground(PANEL_BACKGROUND);

        add(createMonsterSelectionPanel(), BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(PANEL_BACKGROUND);

        tabbedPane.addTab("Current Gear", createCurrentGearTab());
        tabbedPane.addTab("Saved Loadouts", createSavedLoadoutsTab());
        tabbedPane.addTab("Comparisons", createComparisonsTab());

        add(tabbedPane, BorderLayout.CENTER);

        pack();
        setMinimumSize(new Dimension(800, 600));
        setSize(new Dimension(900, 650));
        setLocationRelativeTo(owner);

        searchTimer = new Timer(300, e -> performSearch());
        searchTimer.setRepeats(false);
        setupSearchListeners();

        if (panel.getSelectedMonster() != null) {
            updateMonster(panel.getSelectedMonster());
        }
        rebuildComparisonList();
        updateSavedLoadoutsTable();
    }

    private JPanel createMonsterSelectionPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(PANEL_BACKGROUND);

        JPanel bar = new JPanel(new BorderLayout(10, 10));
        bar.setBackground(PANEL_BACKGROUND);
        bar.setBorder(new EmptyBorder(10, 10, 5, 10));

        searchField.setIcon(IconTextField.Icon.SEARCH);
        searchField.setPreferredSize(new Dimension(250, 30));
        searchField.setBackground(CARD_BACKGROUND);
        searchField.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        searchField.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));

        bar.add(searchField, BorderLayout.WEST);

        topTargetLabel.setForeground(Color.WHITE);
        topTargetLabel.setFont(FontManager.getRunescapeBoldFont());
        topTargetLabel.setText("Current Target: None");
        bar.add(topTargetLabel, BorderLayout.CENTER);

        container.add(bar, BorderLayout.NORTH);

        searchScrollPane.setVisible(false);
        searchScrollPane.setPreferredSize(new Dimension(250, 200));
        searchScrollPane.setBorder(new MatteBorder(1, 1, 1, 1, ColorScheme.MEDIUM_GRAY_COLOR));

        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.setBackground(PANEL_BACKGROUND);
        listWrapper.setBorder(new EmptyBorder(0, 10, 5, 10));
        listWrapper.add(searchScrollPane, BorderLayout.WEST);

        container.add(listWrapper, BorderLayout.CENTER);

        return container;
    }

    private void setupSearchListeners() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchTimer.restart(); }
        });

        searchList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        searchList.setCellRenderer(new MonsterListRenderer());
        searchList.setBackground(CARD_BACKGROUND);
        searchList.setForeground(Color.WHITE);
        searchList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    MonsterStats selected = searchList.getSelectedValue();
                    if (selected != null) {
                        selectMonster(selected);
                    }
                }
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    searchList.requestFocusInWindow();
                    searchList.setSelectedIndex(0);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                     if (searchListModel.getSize() > 0) {
                         selectMonster(searchListModel.get(0));
                     }
                }
            }
        });

        searchList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    MonsterStats selected = searchList.getSelectedValue();
                    if (selected != null) {
                        selectMonster(selected);
                    }
                }
            }
        });
    }

    private void selectMonster(MonsterStats monster) {
        searchField.setText("");
        searchScrollPane.setVisible(false);
        updateMonster(monster);
        panel.selectMonster(monster);
        pack();
    }

    private void performSearch() {
        String query = searchField.getText().toLowerCase().trim();
        searchListModel.clear();

        if (query.isEmpty()) {
            searchScrollPane.setVisible(false);
            pack();
            return;
        }

        final String searchQuery = query;
        List<MonsterStats> matches = monsterDataManager.getAllMonsters().stream()
                .filter(monster -> scoreMatch(searchText(monster), searchQuery) > 0)
                .sorted((a, b) -> Double.compare(
                    scoreMatch(searchText(b), searchQuery),
                    scoreMatch(searchText(a), searchQuery)))
                .limit(50)
                .collect(Collectors.toList());

        for (MonsterStats m : matches) {
            searchListModel.addElement(m);
        }

        searchScrollPane.setVisible(!matches.isEmpty());
        pack();
        repaint();
    }

    private double scoreMatch(String name, String query) {
        if (name.equals(query)) return 1000;
        if (name.startsWith(query)) return 500 + (100.0 * query.length() / name.length());
        String[] words = name.split("[\\s-]+");
        for (String word : words) {
            if (word.startsWith(query)) return 300 + (100.0 * query.length() / word.length());
        }
        int idx = name.indexOf(query);
        if (idx >= 0) return 100 + (50.0 * query.length() / name.length()) - (idx * 0.5);
        if (fuzzyMatch(name, query)) return 10 + (30.0 * longestCommonSubsequence(name, query) / query.length());
        return 0;
    }

    private boolean fuzzyMatch(String name, String query) {
        int nameIdx = 0;
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            boolean found = false;
            while (nameIdx < name.length()) {
                if (name.charAt(nameIdx++) == c) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private int longestCommonSubsequence(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        return dp[a.length()][b.length()];
    }

    private JPanel createCurrentGearTab() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel configPanel = createConfigPanel();
        configPanel.setPreferredSize(new Dimension(350, 500));

        JPanel statsPanel = createStatsPanel();

        panel.add(configPanel, BorderLayout.WEST);
        panel.add(statsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createConfigPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD_BACKGROUND);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 4, 0, 0, BRAND_ORANGE),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(CARD_BACKGROUND);

        JLabel title = new JLabel("CONFIGURATION");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(BRAND_ORANGE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(20));

        useBestPrayerCheckbox = new JCheckBox("Best Prayer");
        useBestPrayerCheckbox.setToolTipText("Automatically select best offensive prayer");
        useBestPrayerCheckbox.setForeground(Color.WHITE);
        useBestPrayerCheckbox.setBackground(CARD_BACKGROUND);
        useBestPrayerCheckbox.setFont(FontManager.getRunescapeFont());
        useBestPrayerCheckbox.setSelected(plugin.getConfig().useBestOffensivePrayer());
        useBestPrayerCheckbox.addActionListener(e -> {
            if (panel.getSelectedMonster() != null) {
                updateMonster(panel.getSelectedMonster());
                panel.updateDisplay(panel.getSelectedMonster());
            }
        });
        useBestPrayerCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(useBestPrayerCheckbox);
        content.add(Box.createVerticalStrut(10));

        assumeMaxBoostsCheckbox = new JCheckBox("Max Boosts");
        assumeMaxBoostsCheckbox.setToolTipText("Assume max stat boosts (+5/+6 levels)");
        assumeMaxBoostsCheckbox.setForeground(Color.WHITE);
        assumeMaxBoostsCheckbox.setBackground(CARD_BACKGROUND);
        assumeMaxBoostsCheckbox.setFont(FontManager.getRunescapeFont());
        assumeMaxBoostsCheckbox.setSelected(plugin.getConfig().assumeMaxBoosts());
        assumeMaxBoostsCheckbox.addActionListener(e -> {
             if (panel.getSelectedMonster() != null) {
                updateMonster(panel.getSelectedMonster());
                panel.updateDisplay(panel.getSelectedMonster());
             }
        });
        assumeMaxBoostsCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(assumeMaxBoostsCheckbox);

        content.add(Box.createVerticalGlue());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        buttonPanel.setBackground(CARD_BACKGROUND);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JButton saveGearButton = new JButton("Save Gear Snapshot");
        saveGearButton.setFocusable(false);
        saveGearButton.setBackground(BRAND_ORANGE.darker());
        saveGearButton.setForeground(Color.WHITE);
        saveGearButton.setFont(FontManager.getRunescapeFont());
        saveGearButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BRAND_ORANGE),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        saveGearButton.addActionListener(e -> {
            GearSnapshot snapshot = captureCurrentGear();
            if (snapshot != null) {
                snapshots.add(snapshot);
                updateSavedLoadoutsTable();
                JOptionPane.showMessageDialog(this, "Gear snapshot saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton addComparisonButton = new JButton("Add to Comparisons");
        addComparisonButton.setFocusable(false);
        addComparisonButton.setBackground(PANEL_BACKGROUND);
        addComparisonButton.setForeground(Color.WHITE);
        addComparisonButton.setFont(FontManager.getRunescapeFont());
        addComparisonButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        addComparisonButton.addActionListener(e -> {
            addComparison();
            tabbedPane.setSelectedIndex(2);
        });

        buttonPanel.add(saveGearButton);
        buttonPanel.add(addComparisonButton);
        content.add(buttonPanel);

        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createStatsPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(CARD_BACKGROUND);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(CARD_BACKGROUND);

        JLabel header = new JLabel("STATS VS CURRENT MONSTER");
        header.setFont(FontManager.getRunescapeBoldFont());
        header.setForeground(Color.WHITE);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(header);

        content.add(Box.createVerticalStrut(10));

        monsterNameLabel.setFont(FontManager.getRunescapeFont());
        monsterNameLabel.setForeground(Color.WHITE);
        monsterNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(monsterNameLabel);

        content.add(Box.createVerticalStrut(30));

        JLabel dpsTitle = new JLabel("DPS");
        dpsTitle.setFont(FontManager.getRunescapeFont());
        dpsTitle.setForeground(MUTED_TEXT);
        dpsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(dpsTitle);

        dpsLabel.setFont(FontManager.getRunescapeBoldFont());
        dpsLabel.setForeground(VALUE_POSITIVE);
        dpsLabel.setFont(dpsLabel.getFont().deriveFont(36f));
        dpsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(dpsLabel);

        content.add(Box.createVerticalStrut(30));

        JPanel grid = new JPanel(new GridLayout(2, 2, 20, 20));
        grid.setBackground(CARD_BACKGROUND);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        grid.add(createStatBox("Max Hit", maxHitLabel, VALUE_HIGHLIGHT));
        grid.add(createStatBox("Accuracy", accuracyLabel, Color.WHITE));
        grid.add(createStatBox("Time to Kill", ttkLabel, Color.WHITE));
        grid.add(createStatBox("Kills / Hour", killsHrLabel, Color.WHITE));

        content.add(grid);
        content.add(Box.createVerticalGlue());

        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createStatBox(String title, JLabel valueLabel, Color valueColor) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(PANEL_BACKGROUND);
        box.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setForeground(MUTED_TEXT);
        titleLbl.setFont(FontManager.getRunescapeSmallFont());

        valueLabel.setForeground(valueColor);
        valueLabel.setFont(FontManager.getRunescapeBoldFont());
        valueLabel.setFont(valueLabel.getFont().deriveFont(18f));

        box.add(titleLbl, BorderLayout.NORTH);
        box.add(valueLabel, BorderLayout.CENTER);
        return box;
    }

    private JPanel createSavedLoadoutsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PANEL_BACKGROUND);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] columns = {"Name", "DPS", "Max Hit", "Accuracy", "TTK", "Kills/Hr", "Action"};
        loadoutsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };

        loadoutsTable = new JTable(loadoutsModel);
        loadoutsTable.setFillsViewportHeight(true);
        loadoutsTable.setRowHeight(35);
        loadoutsTable.setFont(FontManager.getRunescapeFont());
        loadoutsTable.getTableHeader().setFont(FontManager.getRunescapeSmallFont());
        loadoutsTable.setBackground(CARD_BACKGROUND);
        loadoutsTable.setForeground(Color.WHITE);
        loadoutsTable.setGridColor(PANEL_BACKGROUND);

        loadoutsTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        loadoutsTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));

        loadoutsTable.getColumn("DPS").setCellRenderer(new ColoredCellRenderer(VALUE_POSITIVE));
        loadoutsTable.getColumn("Max Hit").setCellRenderer(new ColoredCellRenderer(VALUE_HIGHLIGHT));

        JScrollPane scrollPane = new JScrollPane(loadoutsTable);
        scrollPane.getViewport().setBackground(PANEL_BACKGROUND);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(PANEL_BACKGROUND);

        JButton refreshBtn = new JButton("Refresh Calculations");
        refreshBtn.addActionListener(e -> updateSavedLoadoutsTable());

        JButton clearAllBtn = new JButton("Clear All Snapshots");
        clearAllBtn.setBackground(new Color(100, 40, 40));
        clearAllBtn.setForeground(Color.WHITE);
        clearAllBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Delete all saved snapshots?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                snapshots.clear();
                updateSavedLoadoutsTable();
            }
        });

        buttonPanel.add(refreshBtn);
        buttonPanel.add(clearAllBtn);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateSavedLoadoutsTable() {
        loadoutsModel.setRowCount(0);
        MonsterStats monster = panel.getSelectedMonster();

        for (int i = 0; i < snapshots.size(); i++) {
            GearSnapshot snapshot = snapshots.get(i);

            String dps = "-";
            String maxHit = "-";
            String accuracy = "-";
            String ttk = "-";
            String killsHr = "-";

            if (monster != null) {
                try {
                    PlayerState state = plugin.snapshotToPlayerState(snapshot, monster);
                    if (state != null) {
                        DpsCalculator calculator = new DpsCalculator(state, monster);
                        DpsResult result = calculator.calculate();

                        if (result != null) {
                            dps = String.format("%.2f", result.getDps());
                            maxHit = String.valueOf(result.getMaxHit());
                            accuracy = String.format("%.1f%%", result.getAccuracy() * 100);

                            double ttkVal = result.getDps() > 0 ? monster.getHitpoints() / result.getDps() : 0;
                            ttk = formatTime(ttkVal);

                            int kph = ttkVal > 0 ? (int)(3600 / ttkVal) : 0;
                            killsHr = String.format("%,d", kph);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            loadoutsModel.addRow(new Object[]{
                snapshot.getName(),
                dps,
                maxHit,
                accuracy,
                ttk,
                killsHr,
                "Delete"
            });
        }
    }

    private JPanel createComparisonsTab() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PANEL_BACKGROUND);
        wrapper.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Saved Comparisons");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.WHITE);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        wrapper.add(title, BorderLayout.NORTH);

        comparisonListPanel.setLayout(new BoxLayout(comparisonListPanel, BoxLayout.Y_AXIS));
        comparisonListPanel.setBackground(PANEL_BACKGROUND);

        JScrollPane scrollPane = new JScrollPane(comparisonListPanel);
        scrollPane.setBorder(null);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(PANEL_BACKGROUND);

        clearComparisonsButton.setFocusable(false);
        clearComparisonsButton.setBackground(new Color(100, 40, 40));
        clearComparisonsButton.setForeground(Color.WHITE);
        clearComparisonsButton.addActionListener(e -> clearComparisons());
        clearComparisonsButton.setVisible(false);

        buttonPanel.add(clearComparisonsButton);
        wrapper.add(buttonPanel, BorderLayout.SOUTH);

        return wrapper;
    }

    public void updateMonster(MonsterStats monster) {
        if (monster == null) {
            resetDisplay();
            return;
        }

        monsterNameLabel.setText("Vs: " + monsterDisplayName(monster));
        topTargetLabel.setText("Target: " + monsterDisplayName(monster) + " (HP: " + monster.getHitpoints() + ")");

        DpsResult result = plugin.calculateDps(monster,
            useBestPrayerCheckbox.isSelected(),
            assumeMaxBoostsCheckbox.isSelected());

         if (result != null) {
             dpsLabel.setText(String.format("%.2f", result.getDps()));
             maxHitLabel.setText(String.valueOf(result.getMaxHit()));
             accuracyLabel.setText(String.format("%.1f%%", result.getAccuracy() * 100));

             double ttk = result.getDps() > 0 ? monster.getHitpoints() / result.getDps() : 0;
             ttkLabel.setText(formatTime(ttk));

             int killsHr = ttk > 0 ? (int)(3600 / ttk) : 0;
             killsHrLabel.setText(String.format("%,d", killsHr));
         } else {
             resetDisplay();
         }

         updateSavedLoadoutsTable();
    }

    private void resetDisplay() {
        monsterNameLabel.setText("No Monster Selected");
        topTargetLabel.setText("Target: None");
        dpsLabel.setText("-");
        maxHitLabel.setText("-");
        accuracyLabel.setText("-");
        ttkLabel.setText("-");
        killsHrLabel.setText("-");
        updateSavedLoadoutsTable();
    }

    private String formatTime(double seconds) {
        if (seconds <= 0) return "-";
        if (seconds < 60) return String.format("%.1fs", seconds);
        int mins = (int) (seconds / 60);
        int secs = (int) (seconds % 60);
        return String.format("%dm %ds", mins, secs);
    }

    private static String searchText(MonsterStats monster) {
        String version = monster.getVersion();
        if (version == null || version.isEmpty()) {
            return monster.getName().toLowerCase();
        }
        return (monster.getName() + " " + version).toLowerCase();
    }

    private static String monsterDisplayName(MonsterStats monster) {
        String version = monster.getVersion();
        if (version == null || version.isEmpty()) {
            return monster.getName();
        }
        return monster.getName() + " (" + version + ")";
    }

    private GearSnapshot captureCurrentGear() {
        PlayerState state = plugin.getCachedPlayerState();
        if (state == null) return null;
        String weaponName = state.getWeaponName();
        if (weaponName == null || weaponName.isEmpty()) weaponName = "Unarmed";

        String finalName = weaponName;
        int counter = 2;
        while (nameExists(finalName)) {
            finalName = weaponName + " " + counter++;
        }

        return new GearSnapshot(
            state.getEquippedItemIds(),
            state.getEquippedItemNames(),
            state.getActivePrayers(),
            state.getCombatStyle() != null ? state.getCombatStyle().getName() : "Unknown",
            state.getCombatStyle() != null ? state.getCombatStyle().getStance() : "Unknown",
            finalName,
            false,
            false,
            System.currentTimeMillis()
        );
    }

    private boolean nameExists(String name) {
        return snapshots.stream().anyMatch(s -> s.getName().equals(name));
    }

    private void addComparison() {
        MonsterStats selectedMonster = panel.getSelectedMonster();
        if (selectedMonster == null) return;

        DpsResult result = plugin.calculateDps(selectedMonster,
            useBestPrayerCheckbox.isSelected(),
            assumeMaxBoostsCheckbox.isSelected());

        if (result == null) return;

        PlayerState state = plugin.getCachedPlayerState();
        String weaponName = "Unknown Weapon";
        if (state != null) {
            String actualWeaponName = state.getWeaponName();
            if (actualWeaponName != null && !actualWeaponName.isEmpty()) {
                weaponName = actualWeaponName;
            } else if (state.getWeaponId() <= 0) {
                weaponName = "Unarmed";
            }
        }

        DpsComparison comparison = new DpsComparison(
            monsterDisplayName(selectedMonster),
            weaponName,
            result.getDps(),
            result.getMaxHit(),
            result.getAccuracy(),
            result.getAttackSpeed(),
            System.currentTimeMillis()
        );

        comparisons.add(comparison);
        rebuildComparisonList();
    }

    private void clearComparisons() {
        comparisons.clear();
        rebuildComparisonList();
    }

    private void removeComparison(DpsComparison comparison) {
        comparisons.remove(comparison);
        rebuildComparisonList();
    }

    private void rebuildComparisonList() {
        comparisonListPanel.removeAll();

        if (comparisons.isEmpty()) {
            JLabel emptyLabel = new JLabel("No comparisons saved");
            emptyLabel.setForeground(MUTED_TEXT);
            emptyLabel.setFont(FontManager.getRunescapeSmallFont());
            emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            comparisonListPanel.add(emptyLabel);
            clearComparisonsButton.setVisible(false);
        } else {
            double maxDps = comparisons.stream().mapToDouble(DpsComparison::getDps).max().orElse(0);
            double minDps = comparisons.stream().mapToDouble(DpsComparison::getDps).min().orElse(0);

            for (DpsComparison comp : comparisons) {
                comparisonListPanel.add(createComparisonPanel(comp, maxDps, minDps));
                comparisonListPanel.add(Box.createVerticalStrut(8));
            }
            clearComparisonsButton.setVisible(true);
        }

        comparisonListPanel.revalidate();
        comparisonListPanel.repaint();
    }

    private JPanel createComparisonPanel(DpsComparison comp, double maxDps, double minDps) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 3, 0, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            new EmptyBorder(8, 8, 8, 8)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(CARD_BACKGROUND);

        JLabel nameLabel = new JLabel(comp.getTargetName() + " (" + comp.getWeaponName() + ")");
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(Color.WHITE);
        topRow.add(nameLabel, BorderLayout.WEST);

        JButton removeBtn = new JButton("×");
        removeBtn.setPreferredSize(new Dimension(20, 20));
        removeBtn.setFont(FontManager.getRunescapeBoldFont());
        removeBtn.setMargin(new Insets(0, 0, 0, 0));
        removeBtn.setFocusable(false);
        removeBtn.setBorder(BorderFactory.createEmptyBorder());
        removeBtn.setContentAreaFilled(false);
        removeBtn.setForeground(MUTED_TEXT);
        removeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { removeBtn.setForeground(VALUE_NEGATIVE); }
            public void mouseExited(MouseEvent e) { removeBtn.setForeground(MUTED_TEXT); }
        });
        removeBtn.addActionListener(e -> removeComparison(comp));
        topRow.add(removeBtn, BorderLayout.EAST);
        panel.add(topRow, BorderLayout.NORTH);

        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 5, 2));
        statsPanel.setBackground(CARD_BACKGROUND);
        statsPanel.setBorder(new EmptyBorder(5, 0, 0, 0));

        JLabel dpsVal = new JLabel(String.format("DPS: %.2f", comp.getDps()));
        dpsVal.setFont(FontManager.getRunescapeBoldFont());
        if (comp.getDps() >= maxDps && maxDps > 0) dpsVal.setForeground(VALUE_POSITIVE);
        else if (comp.getDps() <= minDps && minDps < maxDps) dpsVal.setForeground(VALUE_NEGATIVE);
        else dpsVal.setForeground(Color.WHITE);
        statsPanel.add(dpsVal);

        JLabel maxHitVal = new JLabel("Max: " + comp.getMaxHit());
        maxHitVal.setForeground(VALUE_HIGHLIGHT);
        maxHitVal.setFont(FontManager.getRunescapeSmallFont());
        statsPanel.add(maxHitVal);

        JLabel accVal = new JLabel(String.format("Acc: %.1f%%", comp.getAccuracy() * 100));
        accVal.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        accVal.setFont(FontManager.getRunescapeSmallFont());
        statsPanel.add(accVal);

        JLabel speedVal = new JLabel(comp.getAttackSpeed() + " ticks");
        speedVal.setForeground(MUTED_TEXT);
        speedVal.setFont(FontManager.getRunescapeSmallFont());
        statsPanel.add(speedVal);

        panel.add(statsPanel, BorderLayout.CENTER);
        return panel;
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(new Color(60, 20, 20));
            setForeground(new Color(255, 100, 100));
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            currentRow = row;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                if (currentRow >= 0 && currentRow < snapshots.size()) {
                    snapshots.remove(currentRow);
                    updateSavedLoadoutsTable();
                }
            }
            isPushed = false;
            return label;
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }

    class ColoredCellRenderer extends DefaultTableCellRenderer {
        private Color targetColor;

        public ColoredCellRenderer(Color color) {
            this.targetColor = color;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(targetColor);
            return c;
        }
    }

    private static class MonsterListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof MonsterStats) {
                MonsterStats m = (MonsterStats) value;
                String version = (m.getVersion() != null && !m.getVersion().isEmpty())
                    ? " [" + m.getVersion() + "]" : "";
                setText(m.getName() + version + " (ID: " + m.getId() + ")");
                setBorder(new EmptyBorder(5, 8, 5, 8));
                if (isSelected) {
                    setBackground(BRAND_ORANGE);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(CARD_BACKGROUND);
                    setForeground(Color.WHITE);
                }
            }
            return this;
        }
    }

    public boolean isUseBestPrayer() { return useBestPrayerCheckbox.isSelected(); }
    public boolean isAssumeMaxBoosts() { return assumeMaxBoostsCheckbox.isSelected(); }

    public void rebuildSnapshotList() {
        updateSavedLoadoutsTable();
    }
}
