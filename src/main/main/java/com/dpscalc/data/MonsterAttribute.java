package com.dpscalc.data;

/**
 * Monster attributes that affect combat calculations.
 * Matches the attributes from the OSRS Wiki.
 */
public enum MonsterAttribute {
    DEMON("demon"),
    DRAGON("dragon"),
    FIERY("fiery"),
    FLYING("flying"),
    GOLEM("golem"),
    KALPHITE("kalphite"),
    LEAFY("leafy"),
    PENANCE("penance"),
    RAT("rat"),
    SHADE("shade"),
    SPECTRAL("spectral"),
    UNDEAD("undead"),
    VAMPYRE("vampyre"),
    VAMPYRE_1("vampyre1"),
    VAMPYRE_2("vampyre2"),
    VAMPYRE_3("vampyre3"),
    XERICIAN("xerician");

    private final String jsonName;

    MonsterAttribute(String jsonName) {
        this.jsonName = jsonName;
    }

    public String getJsonName() {
        return jsonName;
    }

    public static MonsterAttribute fromJson(String name) {
        if (name == null) return null;
        String normalized = name.toLowerCase().trim();
        for (MonsterAttribute attr : values()) {
            if (attr.jsonName.equals(normalized)) {
                return attr;
            }
        }
        return null;
    }

    public boolean isVampyre() {
        return this == VAMPYRE || this == VAMPYRE_1 || this == VAMPYRE_2 || this == VAMPYRE_3;
    }
}
