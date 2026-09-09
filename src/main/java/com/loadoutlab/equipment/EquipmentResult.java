/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.equipment;

public final class EquipmentResult {
    private final EquipmentLoadout loadout;
    private final EquipmentStatTotals stats;
    private final int speed;
    private final AmmoApplicability ammo;

    EquipmentResult(
            EquipmentLoadout loadout,
            EquipmentStatTotals stats,
            int speed,
            AmmoApplicability ammo) {
        this.loadout = loadout;
        this.stats = stats;
        this.speed = speed;
        this.ammo = ammo;
    }

    public EquipmentLoadout getCanonicalLoadout() {
        return loadout;
    }

    public EquipmentStatTotals getStats() {
        return stats;
    }

    public int getAttackSpeed() {
        return speed;
    }

    public AmmoApplicability getAmmoApplicability() {
        return ammo;
    }
}
