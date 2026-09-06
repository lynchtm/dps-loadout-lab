package com.dpscalc.equipment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class EquipmentItem {
    private final int originalId;
    private final int canonicalId;
    private final Map<String, ItemVariable> itemVariables;

    private EquipmentItem(int originalId, int canonicalId, Map<String, ItemVariable> itemVariables) {
        this.originalId = originalId;
        this.canonicalId = canonicalId;
        this.itemVariables = Collections.unmodifiableMap(new LinkedHashMap<>(itemVariables));
    }

    public static EquipmentItem raw(int id, Map<String, ItemVariable> itemVariables) {
        Objects.requireNonNull(itemVariables, "itemVariables");
        for (Map.Entry<String, ItemVariable> entry : itemVariables.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "item variable key");
            Objects.requireNonNull(entry.getValue(), "item variable value");
        }
        return new EquipmentItem(id, id, itemVariables);
    }

    EquipmentItem canonicalized(int canonicalId) {
        return new EquipmentItem(originalId, canonicalId, itemVariables);
    }

    public int getOriginalId() { return originalId; }
    public int getCanonicalId() { return canonicalId; }
    public Map<String, ItemVariable> getItemVariables() { return itemVariables; }
}
