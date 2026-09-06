package com.dpscalc.equipment;

import java.util.Objects;

public final class EquipmentContext {
    private final int monsterId;
    private final EquipmentCombatStyle combatStyle;
    private final String spellbook;

    private EquipmentContext(int monsterId, EquipmentCombatStyle combatStyle, String spellbook) {
        this.monsterId = monsterId;
        this.combatStyle = Objects.requireNonNull(combatStyle, "combatStyle");
        this.spellbook = Objects.requireNonNull(spellbook, "spellbook");
    }

    public static EquipmentContext of(int monsterId, EquipmentCombatStyle combatStyle, String spellbook) {
        return new EquipmentContext(monsterId, combatStyle, spellbook);
    }

    public int getMonsterId() { return monsterId; }
    public EquipmentCombatStyle getCombatStyle() { return combatStyle; }
    public String getSpellbook() { return spellbook; }
}
