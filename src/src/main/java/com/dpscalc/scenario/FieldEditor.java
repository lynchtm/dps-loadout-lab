package com.dpscalc.scenario;

import com.google.gson.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/** Explicit JSON property paths allow a common editor without Java reflection. */
public final class FieldEditor extends JPanel {
    private final JLabel validation = new JLabel();
    private final Model model = new Model();
    private JsonObject object;
    private List<String> paths = new ArrayList<>();
    private Function<String, String> source;
    private BiConsumer<String, JsonObject> change;
    private Consumer<String> error;
    private Consumer<String> reset;
    private final JTable table =
            new JTable(model) {
                @Override
                public String getToolTipText(java.awt.event.MouseEvent event) {
                    int row = rowAtPoint(event.getPoint());
                    return row < 0 ? null : paths.get(row) + " · " + source.apply(paths.get(row));
                }
            };

    public FieldEditor() {
        super(new BorderLayout());
        table.setRowHeight(24);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getColumnModel().getColumn(0).setPreferredWidth(94);
        table.getColumnModel().getColumn(1).setPreferredWidth(58);
        table.getColumnModel().getColumn(2).setPreferredWidth(58);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(220, 320));
        add(scroll);
        JButton restore = new JButton("Clear selected override");
        restore.addActionListener(
                e -> {
                    int row = table.getSelectedRow();
                    if (row >= 0 && reset != null) reset.accept(paths.get(row));
                });
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(validation, BorderLayout.NORTH);
        footer.add(restore, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);
    }

    public void bind(
            JsonObject data,
            Function<String, String> provenance,
            BiConsumer<String, JsonObject> edited,
            Consumer<String> failed,
            Consumer<String> cleared) {
        if (table.isEditing()) return;
        object = data;
        paths = new ArrayList<>(Scenario.flatten(data).keySet());
        paths.removeIf(
                p ->
                        p.startsWith("rawEquipmentLoadout")
                                || p.startsWith("equippedItemVersions")
                                || p.startsWith("equippedItemCategories")
                                || p.equals("ammoApplicability"));
        source = provenance;
        change = edited;
        error = failed;
        reset = cleared;
        model.fireTableDataChanged();
    }

    private final class Model extends AbstractTableModel {
        public int getRowCount() {
            return paths.size();
        }

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int c) {
            return new String[] {"Field", "Value", "Source"}[c];
        }

        public Object getValueAt(int r, int c) {
            String path = paths.get(r);
            String shortName =
                    path.substring(path.lastIndexOf('.') + 1).replaceAll("([a-z])([A-Z])", "$1 $2");
            return c == 0
                    ? shortName
                    : c == 2 ? source.apply(path) : Scenario.flatten(object).get(path).toString();
        }

        public boolean isCellEditable(int r, int c) {
            return c == 1;
        }

        public void setValueAt(Object value, int row, int column) {
            String path = paths.get(row);
            try {
                JsonElement old = Scenario.flatten(object).get(path);
                String input = value.toString().trim();
                JsonElement parsed;
                if (old.isJsonNull()
                        || old.isJsonPrimitive() && old.getAsJsonPrimitive().isString())
                    parsed =
                            input.equals("null")
                                    ? JsonNull.INSTANCE
                                    : new JsonPrimitive(
                                            input.startsWith("\"")
                                                    ? new JsonParser().parse(input).getAsString()
                                                    : input);
                else parsed = new JsonParser().parse(input);
                if (old.isJsonPrimitive() && old.getAsJsonPrimitive().isNumber()) {
                    double n = parsed.getAsDouble();
                    if (!Double.isFinite(n) || Math.abs(n) > 1_000_000)
                        throw new IllegalArgumentException("Number out of range");
                }
                JsonObject candidate = new JsonParser().parse(object.toString()).getAsJsonObject();
                Scenario.put(candidate, path, parsed);
                change.accept(path, candidate);
                validation.setText("");
            } catch (RuntimeException ex) {
                String message = "Cannot edit " + path + ": " + ex.getMessage();
                validation.setText(message);
                validation.setToolTipText(message);
                error.accept(message);
            }
        }
    }
}
