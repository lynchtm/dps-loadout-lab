/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.equipment;

/** Independently acquired equipment facts; see facts-provenance.json. */
public final class EquipmentCatalogItem {
    private int id, speed;
    private String name, version, slot, category;
    private boolean twoHanded;
    private EquipmentStatTotals stats;

    public int getId() {
        return id;
    }

    public int getSpeed() {
        return speed;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getSlot() {
        return slot;
    }

    public String getCategory() {
        return category;
    }

    public boolean isTwoHanded() {
        return twoHanded;
    }

    public EquipmentStatTotals getStats() {
        return stats;
    }
}
