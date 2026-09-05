package com.dpscalc.state;

/**
 * Combat style information combining the stance and attack type.
 * Based on the weapon type varbit and attack style varplayer.
 */
public class CombatStyle {
    private final String name;
    private final AttackType attackType;
    private final String stance; // "Accurate", "Aggressive", "Controlled", "Defensive", "Rapid", "Longrange", "Autocast"
    private final int attackBonus;  // Invisible level boost
    private final int strengthBonus;
    private final int defenceBonus;
    private final int rangedBonus;
    private final int magicBonus;

    public CombatStyle(String name, AttackType attackType, String stance,
                       int attackBonus, int strengthBonus, int defenceBonus,
                       int rangedBonus, int magicBonus) {
        this.name = name;
        this.attackType = attackType;
        this.stance = stance;
        this.attackBonus = attackBonus;
        this.strengthBonus = strengthBonus;
        this.defenceBonus = defenceBonus;
        this.rangedBonus = rangedBonus;
        this.magicBonus = magicBonus;
    }

    public String getName() { return name; }
    public AttackType getAttackType() { return attackType; }
    public String getStance() { return stance; }
    public int getAttackBonus() { return attackBonus; }
    public int getStrengthBonus() { return strengthBonus; }
    public int getDefenceBonus() { return defenceBonus; }
    public int getRangedBonus() { return rangedBonus; }
    public int getMagicBonus() { return magicBonus; }

    // Common styles for melee weapons
    public static final CombatStyle MELEE_ACCURATE_STAB = new CombatStyle("Stab", AttackType.STAB, "Accurate", 3, 0, 0, 0, 0);
    public static final CombatStyle MELEE_ACCURATE_SLASH = new CombatStyle("Slash", AttackType.SLASH, "Accurate", 3, 0, 0, 0, 0);
    public static final CombatStyle MELEE_ACCURATE_CRUSH = new CombatStyle("Crush", AttackType.CRUSH, "Accurate", 3, 0, 0, 0, 0);
    public static final CombatStyle MELEE_AGGRESSIVE_SLASH = new CombatStyle("Slash", AttackType.SLASH, "Aggressive", 0, 3, 0, 0, 0);
    public static final CombatStyle MELEE_AGGRESSIVE_CRUSH = new CombatStyle("Crush", AttackType.CRUSH, "Aggressive", 0, 3, 0, 0, 0);
    public static final CombatStyle MELEE_AGGRESSIVE_STAB = new CombatStyle("Stab", AttackType.STAB, "Aggressive", 0, 3, 0, 0, 0);
    public static final CombatStyle MELEE_CONTROLLED_STAB = new CombatStyle("Stab", AttackType.STAB, "Controlled", 1, 1, 1, 0, 0);
    public static final CombatStyle MELEE_CONTROLLED_SLASH = new CombatStyle("Slash", AttackType.SLASH, "Controlled", 1, 1, 1, 0, 0);
    public static final CombatStyle MELEE_DEFENSIVE_STAB = new CombatStyle("Stab", AttackType.STAB, "Defensive", 0, 0, 3, 0, 0);
    public static final CombatStyle MELEE_DEFENSIVE_SLASH = new CombatStyle("Slash", AttackType.SLASH, "Defensive", 0, 0, 3, 0, 0);
    public static final CombatStyle MELEE_DEFENSIVE_CRUSH = new CombatStyle("Crush", AttackType.CRUSH, "Defensive", 0, 0, 3, 0, 0);

    // Ranged styles
    public static final CombatStyle RANGED_ACCURATE = new CombatStyle("Accurate", AttackType.RANGED_STANDARD, "Accurate", 0, 0, 0, 3, 0);
    public static final CombatStyle RANGED_RAPID = new CombatStyle("Rapid", AttackType.RANGED_STANDARD, "Rapid", 0, 0, 0, 0, 0);
    public static final CombatStyle RANGED_LONGRANGE = new CombatStyle("Longrange", AttackType.RANGED_STANDARD, "Longrange", 0, 0, 3, 0, 0);

    // Magic styles
    public static final CombatStyle MAGIC_ACCURATE = new CombatStyle("Accurate", AttackType.MAGIC, "Accurate", 0, 0, 0, 0, 3);
    public static final CombatStyle MAGIC_LONGRANGE = new CombatStyle("Longrange", AttackType.MAGIC, "Longrange", 0, 0, 1, 0, 1);
    public static final CombatStyle MAGIC_AUTOCAST = new CombatStyle("Autocast", AttackType.MAGIC, "Autocast", 0, 0, 0, 0, 0);
    public static final CombatStyle MAGIC_DEFENSIVE_AUTOCAST = new CombatStyle("Defensive Autocast", AttackType.MAGIC, "Defensive Autocast", 0, 0, 3, 0, 0);

    // Default/fallback
    public static final CombatStyle UNARMED_PUNCH = new CombatStyle("Punch", AttackType.CRUSH, "Accurate", 3, 0, 0, 0, 0);

    @Override
    public String toString() {
        return name + " (" + stance + ")";
    }

    public static java.util.List<CombatStyle> values() { return java.util.List.of(MELEE_ACCURATE_STAB, MELEE_ACCURATE_SLASH, MELEE_ACCURATE_CRUSH, MELEE_AGGRESSIVE_SLASH, MELEE_AGGRESSIVE_CRUSH, MELEE_AGGRESSIVE_STAB, MELEE_CONTROLLED_STAB, MELEE_CONTROLLED_SLASH, MELEE_DEFENSIVE_STAB, MELEE_DEFENSIVE_SLASH, MELEE_DEFENSIVE_CRUSH, RANGED_ACCURATE, RANGED_RAPID, RANGED_LONGRANGE, MAGIC_ACCURATE, MAGIC_LONGRANGE, MAGIC_AUTOCAST, MAGIC_DEFENSIVE_AUTOCAST, UNARMED_PUNCH); }
}
