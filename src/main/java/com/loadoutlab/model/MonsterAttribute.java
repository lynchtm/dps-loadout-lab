/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently authored; see PROVENANCE.md.
 */
package com.loadoutlab.model;

public enum MonsterAttribute {
    DEMON,
    DRAGON,
    FIERY,
    FLYING,
    GOLEM,
    KALPHITE,
    LEAFY,
    PENANCE,
    RAT,
    SHADE,
    SPECTRAL,
    UNDEAD,
    VAMPYRE,
    VAMPYRE_1,
    VAMPYRE_2,
    VAMPYRE_3,
    XERICIAN;

    public String getJsonName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static MonsterAttribute fromJson(String value) {
        return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_'));
    }

    public boolean isVampyre() {
        return name().startsWith("VAMPYRE");
    }
}
