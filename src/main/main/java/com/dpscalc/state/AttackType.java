package com.dpscalc.state;

/**
 * The type of attack being used (determines which defensive stat is checked).
 */
public enum AttackType {
    STAB("stab"),
    SLASH("slash"),
    CRUSH("crush"),
    RANGED_LIGHT("light"),
    RANGED_STANDARD("standard"),
    RANGED_HEAVY("heavy"),
    MAGIC("magic");

    private final String key;

    AttackType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public boolean isMelee() {
        return this == STAB || this == SLASH || this == CRUSH;
    }

    public boolean isRanged() {
        return this == RANGED_LIGHT || this == RANGED_STANDARD || this == RANGED_HEAVY;
    }

    public boolean isMagic() {
        return this == MAGIC;
    }
}
