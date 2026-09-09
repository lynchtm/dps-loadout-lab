/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.equipment;

import java.util.*;

public final class EquipmentLoadout {
    private final Map<EquipmentSlot, EquipmentItem> slots;

    private EquipmentLoadout(Map<EquipmentSlot, EquipmentItem> values) {
        EnumMap<EquipmentSlot, EquipmentItem> copy = new EnumMap<>(EquipmentSlot.class);
        copy.putAll(values);
        slots = Collections.unmodifiableMap(copy);
    }

    public static EquipmentLoadout of(Map<EquipmentSlot, EquipmentItem> values) {
        return new EquipmentLoadout(values);
    }

    public EquipmentItem get(EquipmentSlot slot) {
        return slots.get(slot);
    }

    public Map<EquipmentSlot, EquipmentItem> asMap() {
        return slots;
    }
}
