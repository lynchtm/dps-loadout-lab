/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently authored; see PROVENANCE.md.
 */
package com.loadoutlab.model;

public enum AttackType {
    STAB,
    SLASH,
    CRUSH,
    RANGED_LIGHT,
    RANGED_STANDARD,
    RANGED_HEAVY,
    MAGIC;

    public String getKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isMelee() {
        return this == STAB || this == SLASH || this == CRUSH;
    }

    public boolean isRanged() {
        return name().startsWith("RANGED_");
    }

    public boolean isMagic() {
        return this == MAGIC;
    }
}
