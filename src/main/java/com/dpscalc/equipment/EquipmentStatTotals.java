package com.dpscalc.equipment;

import java.util.Arrays;

public final class EquipmentStatTotals {
    static final int STAB_ATTACK = 0;
    static final int SLASH_ATTACK = 1;
    static final int CRUSH_ATTACK = 2;
    static final int MAGIC_ATTACK = 3;
    static final int RANGED_ATTACK = 4;
    static final int MELEE_STRENGTH = 5;
    static final int RANGED_STRENGTH = 6;
    static final int MAGIC_DAMAGE = 7;
    static final int PRAYER = 8;
    static final int STAB_DEFENCE = 9;
    static final int SLASH_DEFENCE = 10;
    static final int CRUSH_DEFENCE = 11;
    static final int MAGIC_DEFENCE = 12;
    static final int RANGED_DEFENCE = 13;
    static final int COUNT = 14;

    private final int[] values;

    EquipmentStatTotals(int[] values) {
        if (values.length != COUNT) throw new IllegalArgumentException("Expected " + COUNT + " equipment stats");
        this.values = Arrays.copyOf(values, values.length);
    }

    static EquipmentStatTotals zero() {
        return new EquipmentStatTotals(new int[COUNT]);
    }

    EquipmentStatTotals plus(EquipmentStatTotals other) {
        int[] sum = values();
        for (int index = 0; index < COUNT; index++) sum[index] += other.values[index];
        return new EquipmentStatTotals(sum);
    }

    EquipmentStatTotals withoutAmmoRangedStats() {
        int[] filtered = values();
        filtered[RANGED_ATTACK] = 0;
        filtered[RANGED_STRENGTH] = 0;
        return new EquipmentStatTotals(filtered);
    }

    int[] values() { return Arrays.copyOf(values, values.length); }

    public int getStabAttack() { return values[STAB_ATTACK]; }
    public int getSlashAttack() { return values[SLASH_ATTACK]; }
    public int getCrushAttack() { return values[CRUSH_ATTACK]; }
    public int getMagicAttack() { return values[MAGIC_ATTACK]; }
    public int getRangedAttack() { return values[RANGED_ATTACK]; }
    public int getMeleeStrength() { return values[MELEE_STRENGTH]; }
    public int getRangedStrength() { return values[RANGED_STRENGTH]; }
    public int getMagicDamage() { return values[MAGIC_DAMAGE]; }
    public int getPrayerBonus() { return values[PRAYER]; }
    public int getStabDefence() { return values[STAB_DEFENCE]; }
    public int getSlashDefence() { return values[SLASH_DEFENCE]; }
    public int getCrushDefence() { return values[CRUSH_DEFENCE]; }
    public int getMagicDefence() { return values[MAGIC_DEFENCE]; }
    public int getRangedDefence() { return values[RANGED_DEFENCE]; }
}
