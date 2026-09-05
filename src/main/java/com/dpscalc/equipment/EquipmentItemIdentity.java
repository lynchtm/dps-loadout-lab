package com.dpscalc.equipment;

import com.google.gson.JsonObject;

import java.util.Objects;

final class EquipmentItemIdentity {
    private final String name;
    private final String version;
    private final String slot;
    private final String category;
    private final boolean twoHanded;

    EquipmentItemIdentity(JsonObject item) {
        Objects.requireNonNull(item, "item");
        this.name = item.get("name").getAsString();
        this.version = item.get("version").getAsString();
        this.slot = item.get("slot").getAsString();
        this.category = item.get("category").getAsString();
        this.twoHanded = item.has("isTwoHanded") && item.get("isTwoHanded").getAsBoolean();
    }

    String getName() { return name; }
    String getVersion() { return version; }
    String getSlot() { return slot; }
    String getCategory() { return category; }
    boolean isTwoHanded() { return twoHanded; }
}
