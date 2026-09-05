package com.dpscalc.equipment;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class EquipmentLoadout {
    private final Map<EquipmentSlot, EquipmentItem> items;

    private EquipmentLoadout(Map<EquipmentSlot, EquipmentItem> items) {
        EnumMap<EquipmentSlot, EquipmentItem> copy = new EnumMap<>(EquipmentSlot.class);
        items.forEach((slot, item) -> copy.put(
            Objects.requireNonNull(slot, "slot"),
            Objects.requireNonNull(item, "item")
        ));
        this.items = Collections.unmodifiableMap(copy);
    }

    public static EquipmentLoadout of(Map<EquipmentSlot, EquipmentItem> items) {
        return new EquipmentLoadout(Objects.requireNonNull(items, "items"));
    }

    public EquipmentItem get(EquipmentSlot slot) {
        return items.get(slot);
    }

    public Map<EquipmentSlot, EquipmentItem> asMap() {
        return items;
    }
}
