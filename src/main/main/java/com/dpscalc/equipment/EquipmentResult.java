package com.dpscalc.equipment;

import java.util.Objects;

public final class EquipmentResult {
    private final EquipmentLoadout canonicalLoadout;
    private final EquipmentStatTotals stats;
    private final int attackSpeed;
    private final AmmoApplicability ammoApplicability;

    EquipmentResult(EquipmentLoadout canonicalLoadout, EquipmentStatTotals stats, int attackSpeed,
                    AmmoApplicability ammoApplicability) {
        this.canonicalLoadout = Objects.requireNonNull(canonicalLoadout, "canonicalLoadout");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.attackSpeed = attackSpeed;
        this.ammoApplicability = Objects.requireNonNull(ammoApplicability, "ammoApplicability");
    }

    public EquipmentLoadout getCanonicalLoadout() { return canonicalLoadout; }
    public EquipmentStatTotals getStats() { return stats; }
    public int getAttackSpeed() { return attackSpeed; }
    public AmmoApplicability getAmmoApplicability() { return ammoApplicability; }
}
