package com.dpscalc.data;

public enum WeaknessElement {
    AIR("air"),
    WATER("water"),
    EARTH("earth"),
    FIRE("fire");

    private final String jsonName;

    WeaknessElement(String jsonName) {
        this.jsonName = jsonName;
    }

    public String getJsonName() {
        return jsonName;
    }

    public static WeaknessElement fromJson(String name) {
        if (name == null) return null;
        String normalized = name.toLowerCase().trim();
        for (WeaknessElement elem : values()) {
            if (elem.jsonName.equals(normalized)) {
                return elem;
            }
        }
        return null;
    }
}
