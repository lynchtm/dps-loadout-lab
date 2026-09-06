package com.dpscalc.state;

public enum EquipmentSlot {
    HEAD(0),
    CAPE(1),
    AMULET(2),
    WEAPON(3),
    BODY(4),
    SHIELD(5),
    LEGS(7),
    GLOVES(9),
    BOOTS(10),
    RING(12),
    AMMO(13);

    private final int index;

    EquipmentSlot(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
