/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.equipment;

public final class ItemVariable {
    private final int value;

    private ItemVariable(int value) {
        this.value = value;
    }

    public static ItemVariable ofNumber(int n) {
        return new ItemVariable(n);
    }

    public int intValue() {
        return value;
    }
}
