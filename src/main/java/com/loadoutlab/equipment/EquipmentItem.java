/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.equipment;

import java.util.*;

public final class EquipmentItem {
    private final int id;
    private final Map<String, ItemVariable> variables;

    private EquipmentItem(int id, Map<String, ItemVariable> variables) {
        this.id = id;
        this.variables = Collections.unmodifiableMap(new HashMap<>(variables));
    }

    public static EquipmentItem raw(int id, Map<String, ItemVariable> variables) {
        return new EquipmentItem(id, variables);
    }

    public int getOriginalId() {
        return id;
    }

    public int getCanonicalId() {
        return id;
    }

    public Map<String, ItemVariable> getItemVariables() {
        return variables;
    }
}
