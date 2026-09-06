package com.dpscalc.equipment;

import java.util.Objects;

public final class EquipmentCombatStyle {
    private final String type;
    private final String stance;

    private EquipmentCombatStyle(String type, String stance) {
        this.type = Objects.requireNonNull(type, "type");
        this.stance = Objects.requireNonNull(stance, "stance");
    }

    public static EquipmentCombatStyle of(String type, String stance) {
        return new EquipmentCombatStyle(type, stance);
    }

    public String getType() { return type; }
    public String getStance() { return stance; }
}
