/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.equipment;

public enum EquipmentSlot {
    HEAD(0),
    CAPE(1),
    NECK(2),
    WEAPON(3),
    BODY(4),
    SHIELD(5),
    LEGS(7),
    HANDS(9),
    FEET(10),
    RING(12),
    AMMO(13);
    public final int index;

    EquipmentSlot(int index) {
        this.index = index;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
