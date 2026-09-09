package com.dpscalc.scenario;

import java.util.*;

/**
 * Planned inventory is independent of the legacy observed inventory and never changes combat stats.
 */
public final class CarryPlan {
    public Entry[] slots = new Entry[28];
    public Entry[] runes = new Entry[4];
    public List<Entry> switches = new ArrayList<>();

    public static final class Entry {
        public int id, quantity;
        public boolean noted;
        public String name = "", note = "";

        public Entry() {}

        public Entry(int id, String name, int quantity) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
        }

        public Entry copy() {
            return Scenario.JSON.fromJson(Scenario.JSON.toJson(this), Entry.class);
        }
    }

    public void validate() {
        if (slots == null
                || slots.length != 28
                || runes == null
                || runes.length != 4
                || switches == null
                || switches.size() > 28)
            throw new IllegalArgumentException(
                    "Use 28 inventory slots, four pouch slots and at most 28 switches");
        List<Entry> all = new ArrayList<>();
        Collections.addAll(all, slots);
        Collections.addAll(all, runes);
        all.addAll(switches);
        for (Entry e : all)
            if (e != null
                    && (e.id < 0
                            || e.quantity < 0
                            || e.name == null
                            || e.name.length() > 200
                            || e.note == null
                            || e.note.length() > 2000))
                throw new IllegalArgumentException("Invalid planned item or quantity");
    }

    public List<String> audit(Scenario.Loadout loadout, OwnedEquipment owned, String profile) {
        validate();
        List<String> notes = new ArrayList<>();
        if (!owned.ready(profile)) {
            notes.add("Ownership unknown: open this character's bank to refresh.");
            return notes;
        }
        Map<Integer, Long> required = new LinkedHashMap<>();
        Map<Integer, String> names = new HashMap<>();
        for (int slot : BankOptimizer.SLOTS) {
            int id = loadout.player.getEquippedItemIds()[slot];
            if (id > 0) {
                required.merge(id, 1L, Long::sum);
                names.put(
                        id,
                        Objects.toString(
                                loadout.player.getEquippedItemNames()[slot], "Item " + id));
            }
        }
        List<Entry> packed = new ArrayList<>();
        Collections.addAll(packed, slots);
        Collections.addAll(packed, runes);
        for (Entry e : packed)
            if (e != null) {
                if (e.id <= 0) {
                    notes.add("Unrecognized: " + e.name);
                    continue;
                }
                if (e.noted)
                    notes.add(e.name + ": noted restock stack; unnote before using it in combat.");
                if (e.quantity == 0)
                    notes.add(
                            e.name + ": set the required quantity; supply sufficiency is unknown.");
                required.merge(e.id, (long) Math.max(1, e.quantity), Long::sum);
                names.put(e.id, e.name);
            }
        required.forEach(
                (id, count) -> {
                    long available = owned.quantities.getOrDefault(id, 0);
                    if (available < count)
                        notes.add(
                                names.get(id)
                                        + ": need "
                                        + count
                                        + ", own "
                                        + available
                                        + " (missing "
                                        + (count - available)
                                        + ")");
                });
        for (Entry e : switches)
            if (e != null && (e.id <= 0 || !owned.owns(e.id)))
                notes.add(
                        "Optional switch "
                                + e.name
                                + ": "
                                + (e.id <= 0 ? "unrecognized" : "not owned"));
        int pouchCapacity = 0;
        for (Entry e : slots)
            if (e != null && e.name != null) {
                String n = e.name.toLowerCase(Locale.ROOT);
                if (n.contains("rune pouch"))
                    pouchCapacity = Math.max(pouchCapacity, n.contains("divine") ? 4 : 3);
            }
        int runeCount = 0;
        Set<Integer> uniqueRunes = new HashSet<>();
        for (Entry e : runes)
            if (e != null) {
                runeCount++;
                if (e.id > 0 && !uniqueRunes.add(e.id))
                    notes.add("Rune pouch contains duplicate " + e.name);
            }
        if (runeCount > pouchCapacity)
            notes.add(
                    pouchCapacity == 0
                            ? "Add a rune pouch to the inventory, or move its runes to inventory"
                                    + " slots."
                            : "This pouch holds only " + pouchCapacity + " rune types.");
        if (notes.isEmpty())
            notes.add(
                    "Listed quantities are owned. Charges, spell costs and trip duration still need"
                            + " checking.");
        return notes;
    }
}
