package com.dpscalc.scenario;

import java.util.*;

/** A bank layout is an arrangement of item IDs, not withdrawal quantities. */
public final class BankLayoutPlan {
    public final Map<Integer, Integer> positions;

    private BankLayoutPlan(Map<Integer, Integer> positions) {
        this.positions = Collections.unmodifiableMap(positions);
    }

    public static String suggestedName(String loadout) {
        String name =
                ("DPS " + loadout)
                        .replaceAll("[^a-zA-Z0-9 _()'\\-]", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
        return name.substring(0, Math.min(60, name.length())).trim();
    }

    public static BankLayoutPlan from(Scenario.Loadout loadout) {
        Scenario.validate(loadout);
        Map<Integer, Integer> positions = new TreeMap<>();
        int[] slots = {0, 1, 2, 13, 3, 4, 5, 7, 9, 10, 12},
                cells = {1, 8, 9, 10, 16, 17, 18, 25, 32, 33, 34};
        for (int i = 0; i < slots.length; i++) {
            int id = loadout.player.getEquippedItemIds()[slots[i]];
            if (id > 0) positions.put(cells[i], id);
        }
        for (int i = 0; i < 28; i++) {
            CarryPlan.Entry entry = loadout.carry.slots[i];
            if (entry != null && entry.id > 0) positions.put((i / 4) * 8 + 4 + i % 4, entry.id);
        }
        for (int i = 0; i < 4; i++) {
            CarryPlan.Entry entry = loadout.carry.runes[i];
            if (entry != null && entry.id > 0) positions.put(40 + i, entry.id);
        }
        int cell = 64;
        for (CarryPlan.Entry entry : loadout.carry.switches)
            if (entry != null && entry.id > 0 && !positions.containsValue(entry.id))
                positions.put(cell++, entry.id);
        if (positions.isEmpty())
            throw new IllegalArgumentException(
                    "Add equipment or supplies before creating a bank layout");
        return new BankLayoutPlan(positions);
    }

    public static String tagName(String raw) {
        String name = raw.trim().toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9 _()'\\-]{1,60}"))
            throw new IllegalArgumentException(
                    "Use 1–60 letters, numbers, spaces, apostrophes, parentheses or hyphens for the"
                            + " bank tab name");
        return name;
    }
}
