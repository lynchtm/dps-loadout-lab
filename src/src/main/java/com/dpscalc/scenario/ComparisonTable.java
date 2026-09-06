package com.dpscalc.scenario;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.IntConsumer;

import javax.swing.*;
import javax.swing.table.*;

/** Wiki-style metrics down the left, loadouts across the top. */
public final class ComparisonTable extends JPanel {
    private static final String[] METRICS = {
        "Max hit",
        "DPS",
        "Avg. TTK (s)",
        "Accuracy %",
        "Spec damage",
        "Avg. damage",
        "Interval (t)",
        "Rotation DPS",
        "Taken / s",
        "Taken / kill",
        "Prayer (s)",
        "Kill var (s²)",
        "Status"
    };
    private List<ScenarioCalculator.Result> results = Collections.emptyList();
    private boolean expanded, grouped;
    private int selected;
    private int columnWidth = 62;
    private final AbstractTableModel model =
            new AbstractTableModel() {
                public int getRowCount() {
                    return visibleMetrics().size();
                }

                public int getColumnCount() {
                    return results.size();
                }

                public String getColumnName(int c) {
                    return (results.get(c).limitations.isEmpty() ? "" : "! ")
                            + (c + 1)
                            + " · "
                            + results.get(c).name;
                }

                public Object getValueAt(int r, int c) {
                    return value(metric(r), results.get(c));
                }
            };
    private final JTable table =
            new JTable(model) {
                public String getToolTipText(MouseEvent e) {
                    int row = rowAtPoint(e.getPoint()), col = columnAtPoint(e.getPoint());
                    if (row < 0 || col < 0) return null;
                    ScenarioCalculator.Result result = results.get(col);
                    return result.error != null
                            ? result.error
                            : result.name
                                    + " · "
                                    + metricName(row)
                                    + ": "
                                    + format(metric(row), value(metric(row), result))
                                    + (metric(row) == 13
                                            ? " (hits "
                                                    + result.targetsHit
                                                    + " of "
                                                    + result.encounterTargets
                                                    + " targets)"
                                            : "")
                                    + (best(row, col) ? " (best)" : "");
                }
            };
    private final JList<String> labels = new JList<>();
    private final JScrollPane scroll = new JScrollPane(table);
    private final IntConsumer select;

    public ComparisonTable(IntConsumer select) {
        this.select = select;
        setLayout(new BorderLayout());
        setBackground(CalculatorWorkspace.BG);
        table.getAccessibleContext().setAccessibleName("Loadout comparison");
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(27);
        table.setCellSelectionEnabled(false);
        table.setFocusable(true);
        table.setShowGrid(true);
        table.setGridColor(CalculatorWorkspace.EDGE);
        table.getTableHeader().setReorderingAllowed(false);
        table.setTableHeader(
                new JTableHeader(table.getColumnModel()) {
                    public String getToolTipText(MouseEvent e) {
                        int c = columnAtPoint(e.getPoint());
                        return c < 0
                                ? null
                                : results.get(c).name
                                        + (select == null ? "" : " — click to edit this loadout");
                    }
                });
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader()
                .setDefaultRenderer(
                        new DefaultTableCellRenderer() {
                            public Component getTableCellRendererComponent(
                                    JTable t, Object v, boolean s, boolean f, int row, int col) {
                                super.getTableCellRendererComponent(t, v, s, f, row, col);
                                setHorizontalAlignment(CENTER);
                                setForeground(CalculatorWorkspace.TEXT);
                                setBackground(
                                        col == selected
                                                ? CalculatorWorkspace.ORANGE
                                                : CalculatorWorkspace.BG);
                                setBorder(
                                        BorderFactory.createMatteBorder(
                                                0, 0, 1, 1, CalculatorWorkspace.EDGE));
                                return this;
                            }
                        });
        scroll.setColumnHeaderView(table.getTableHeader());
        MouseAdapter choose =
                new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        int col = table.columnAtPoint(e.getPoint());
                        if (select != null && col >= 0 && col < results.size()) select.accept(col);
                    }
                };
        table.addMouseListener(choose);
        table.getTableHeader().addMouseListener(choose);
        table.setDefaultRenderer(
                Object.class,
                new DefaultTableCellRenderer() {
                    public Component getTableCellRendererComponent(
                            JTable t, Object value, boolean s, boolean focus, int row, int col) {
                        super.getTableCellRendererComponent(t, value, s, focus, row, col);
                        setText(format(metric(row), value));
                        setHorizontalAlignment(CENTER);
                        boolean winner = best(row, col);
                        setForeground(winner ? CalculatorWorkspace.BEST : CalculatorWorkspace.TEXT);
                        setBackground(
                                winner
                                        ? CalculatorWorkspace.CARD
                                        : col == selected
                                                ? CalculatorWorkspace.CARD
                                                : CalculatorWorkspace.BG);
                        setFont(new Font(Font.SANS_SERIF, winner ? Font.BOLD : Font.PLAIN, 11));
                        return this;
                    }
                });
        labels.setFixedCellHeight(27);
        labels.setFixedCellWidth(83);
        labels.setBackground(CalculatorWorkspace.CARD);
        labels.setForeground(CalculatorWorkspace.TEXT);
        labels.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        labels.setEnabled(false);
        labels.setCellRenderer(
                new DefaultListCellRenderer() {
                    public Component getListCellRendererComponent(
                            JList<?> list, Object v, int index, boolean s, boolean f) {
                        super.getListCellRendererComponent(list, v, index, false, false);
                        setForeground(CalculatorWorkspace.TEXT);
                        setBackground(CalculatorWorkspace.CARD);
                        return this;
                    }
                });
        scroll.setRowHeaderView(labels);
        scroll.setCorner(
                ScrollPaneConstants.UPPER_LEFT_CORNER,
                CalculatorWorkspace.label("Metric", CalculatorWorkspace.MUTED));
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(CalculatorWorkspace.BG);
        add(scroll, BorderLayout.CENTER);
        JCheckBox more = new JCheckBox("Show more");
        more.setOpaque(false);
        more.setForeground(CalculatorWorkspace.TEXT);
        more.addActionListener(
                e -> {
                    expanded = more.isSelected();
                    refresh();
                });
        add(more, BorderLayout.SOUTH);
        refresh();
        CalculatorWorkspace.theme(this);
        SidebarScrolling.nested(scroll);
    }

    public void setColumnWidth(int width) {
        columnWidth = width;
        refresh();
    }

    public void setResults(List<ScenarioCalculator.Result> results, int selected) {
        this.results = new ArrayList<>(results);
        this.selected = selected;
        grouped = results.stream().anyMatch(r -> r.groupDps != null);
        refresh();
    }

    private void refresh() {
        int position = scroll.getHorizontalScrollBar().getValue();
        model.fireTableStructureChanged();
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidth);
            table.getColumnModel().getColumn(i).setMinWidth(60);
        }
        String[] names = new String[model.getRowCount()];
        for (int row = 0; row < names.length; row++) names[row] = metricName(row);
        labels.setListData(names);
        scroll.setPreferredSize(new Dimension(210, model.getRowCount() * 27 + 47));
        scroll.getHorizontalScrollBar().setValue(position);
        revalidate();
        repaint();
    }

    private List<Integer> visibleMetrics() {
        List<Integer> metrics = new ArrayList<>(Arrays.asList(1, 0, 3, 2));
        if (grouped) metrics.add(1, 13);
        if (results.stream().anyMatch(r -> r.special != null)) metrics.add(4);
        if (expanded)
            for (int i = 4; i < METRICS.length; i++) if (!metrics.contains(i)) metrics.add(i);
        return metrics;
    }

    private int metric(int row) {
        return visibleMetrics().get(row);
    }

    private String metricName(int row) {
        int metric = metric(row);
        return metric == 13
                ? "Group DPS (est.)"
                : grouped && metric == 1
                        ? "DPS / monster"
                        : grouped && metric == 2 ? "TTK / monster" : METRICS[metric];
    }

    public boolean best(int row, int column) {
        row = metric(row);
        if (row != 13 && (row > 9 || row == 6 || row == 8 || row == 9)) return false;
        Object raw = value(row, results.get(column));
        if (!(raw instanceof Number)) return false;
        double candidate = ((Number) raw).doubleValue();
        if (!Double.isFinite(candidate)) return false;
        boolean lower = row == 2;
        for (ScenarioCalculator.Result result : results) {
            Object other = value(row, result);
            if (other instanceof Number) {
                double n = ((Number) other).doubleValue();
                if (Double.isFinite(n) && (lower ? n < candidate : n > candidate)) return false;
            }
        }
        return true;
    }

    private static Object value(int row, ScenarioCalculator.Result r) {
        if (row == 12)
            return r.error != null
                    ? "Unavailable"
                    : !r.limitations.isEmpty()
                            ? "Known formula limitation"
                            : r.groupDps != null ? "Group estimate" : "Model result";
        if (r.error != null || r.normal == null) return null;
        switch (row) {
            case 13:
                return r.groupDps;
            case 0:
                return r.normal.getMaxHit();
            case 1:
                return r.normal.getDps();
            case 2:
                return r.ttk;
            case 3:
                return r.normal.getAccuracy() * 100;
            case 4:
                return r.special == null ? null : r.special.getExpectedDamage();
            case 5:
                return r.normal.getExpectedDamage();
            case 6:
                return r.normal.getExpectedAttackSpeed();
            case 7:
                return r.rotationDps;
            case 8:
                return r.damageTaken;
            case 9:
                return r.damageTaken == null || r.ttk == null ? null : r.damageTaken * r.ttk;
            case 10:
                return r.prayerSeconds;
            case 11:
                return r.ttkVariance;
            default:
                return null;
        }
    }

    private static String format(int row, Object value) {
        if (value == null) return "—";
        if (!(value instanceof Number)) return value.toString();
        double n = ((Number) value).doubleValue();
        if (!Double.isFinite(n)) return "—";
        return String.format(
                Locale.ROOT,
                row == 0 ? "%.0f" : row == 1 || row == 7 || row == 13 ? "%.3f" : "%.1f",
                n);
    }
}
