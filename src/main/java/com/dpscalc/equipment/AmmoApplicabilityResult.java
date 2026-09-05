package com.dpscalc.equipment;

import java.util.Objects;

public final class AmmoApplicabilityResult {
    private final AmmoApplicability applicability;
    private final Integer originalWeaponId;
    private final Integer canonicalWeaponId;
    private final Integer originalAmmoId;
    private final Integer canonicalAmmoId;

    AmmoApplicabilityResult(AmmoApplicability applicability, EquipmentItem weapon, EquipmentItem ammo) {
        this.applicability = Objects.requireNonNull(applicability, "applicability");
        this.originalWeaponId = weapon == null ? null : weapon.getOriginalId();
        this.canonicalWeaponId = weapon == null ? null : weapon.getCanonicalId();
        this.originalAmmoId = ammo == null ? null : ammo.getOriginalId();
        this.canonicalAmmoId = ammo == null ? null : ammo.getCanonicalId();
    }

    public AmmoApplicability getApplicability() { return applicability; }
    public Integer getOriginalWeaponId() { return originalWeaponId; }
    public Integer getCanonicalWeaponId() { return canonicalWeaponId; }
    public Integer getOriginalAmmoId() { return originalAmmoId; }
    public Integer getCanonicalAmmoId() { return canonicalAmmoId; }

    public String describeIds() {
        return "weaponOriginal=" + originalWeaponId
            + " weaponCanonical=" + canonicalWeaponId
            + " ammoOriginal=" + originalAmmoId
            + " ammoCanonical=" + canonicalAmmoId;
    }
}
