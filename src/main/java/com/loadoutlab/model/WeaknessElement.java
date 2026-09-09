/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently authored; see PROVENANCE.md.
 */
package com.loadoutlab.model;

public enum WeaknessElement {
    AIR,
    WATER,
    EARTH,
    FIRE;

    public String getJsonName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static WeaknessElement fromJson(String value) {
        return valueOf(value.toUpperCase(java.util.Locale.ROOT));
    }
}
