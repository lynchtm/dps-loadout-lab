package com.dpscalc.equipment;

import java.util.Objects;

public final class EquipmentCatalogItem {
    private final EquipmentItemIdentity identity;
    private final int speed;
    private final EquipmentStatTotals stats;

    EquipmentCatalogItem(EquipmentItemIdentity identity, int speed, EquipmentStatTotals stats) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.speed = speed;
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    public String getName() { return identity.getName(); }
    public String getVersion() { return identity.getVersion(); }
    public String getSlot() { return identity.getSlot(); }
    public String getCategory() { return identity.getCategory(); }
    public boolean isTwoHanded() { return identity.isTwoHanded(); }
    public int getSpeed() { return speed; }
    public EquipmentStatTotals getStats() { return stats; }
}
