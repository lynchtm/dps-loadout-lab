/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. See PROVENANCE.md.
 */
package com.loadoutlab.model;

import java.util.*;

/** A persisted attack-button choice, independent of the equipped weapon. */
public final class CombatStyle {
    private final String name, stance;
    private final AttackType attackType;
    private final int attackBonus, strengthBonus, defenceBonus, rangedBonus, magicBonus;

    public CombatStyle(
            String name,
            AttackType type,
            String stance,
            int attack,
            int strength,
            int defence,
            int ranged,
            int magic) {
        this.name = name;
        this.attackType = type;
        this.stance = stance;
        attackBonus = attack;
        strengthBonus = strength;
        defenceBonus = defence;
        rangedBonus = ranged;
        magicBonus = magic;
    }

    public static CombatStyle button(String name, AttackType type, String stance) {
        int shared = "Controlled".equals(stance) ? 1 : 0;
        boolean accurate = "Accurate".equals(stance),
                defensive = "Defensive".equals(stance),
                longrange = "Longrange".equals(stance);
        return new CombatStyle(
                name,
                type,
                stance,
                type.isMelee() && accurate ? 3 : shared,
                type.isMelee() && "Aggressive".equals(stance) ? 3 : shared,
                defensive ? 3 : longrange ? (type.isMagic() ? 1 : 3) : shared,
                type.isRanged() && accurate ? 3 : 0,
                type.isMagic() ? (accurate ? 3 : longrange ? 1 : 0) : 0);
    }

    public String getName() {
        return name;
    }

    public String getStance() {
        return stance;
    }

    public AttackType getAttackType() {
        return attackType;
    }

    public int getAttackBonus() {
        return attackBonus;
    }

    public int getStrengthBonus() {
        return strengthBonus;
    }

    public int getDefenceBonus() {
        return defenceBonus;
    }

    public int getRangedBonus() {
        return rangedBonus;
    }

    public int getMagicBonus() {
        return magicBonus;
    }

    @Override
    public String toString() {
        return name + " · " + attackType.getKey() + " · " + stance;
    }

    public static final CombatStyle UNARMED_PUNCH = button("Punch", AttackType.CRUSH, "Accurate");
    public static final CombatStyle
            MELEE_ACCURATE_STAB = button("Stab", AttackType.STAB, "Accurate"),
            MELEE_ACCURATE_SLASH = button("Chop", AttackType.SLASH, "Accurate"),
            MELEE_ACCURATE_CRUSH = button("Pound", AttackType.CRUSH, "Accurate");
    public static final CombatStyle
            MELEE_AGGRESSIVE_SLASH = button("Slash", AttackType.SLASH, "Aggressive"),
            MELEE_AGGRESSIVE_CRUSH = button("Pummel", AttackType.CRUSH, "Aggressive"),
            MELEE_AGGRESSIVE_STAB = button("Lunge", AttackType.STAB, "Aggressive");
    public static final CombatStyle
            MELEE_CONTROLLED_STAB = button("Lunge", AttackType.STAB, "Controlled"),
            MELEE_CONTROLLED_SLASH = button("Lash", AttackType.SLASH, "Controlled");
    public static final CombatStyle
            MELEE_DEFENSIVE_STAB = button("Block", AttackType.STAB, "Defensive"),
            MELEE_DEFENSIVE_SLASH = button("Block", AttackType.SLASH, "Defensive"),
            MELEE_DEFENSIVE_CRUSH = button("Block", AttackType.CRUSH, "Defensive");
    public static final CombatStyle
            RANGED_ACCURATE = button("Accurate", AttackType.RANGED_STANDARD, "Accurate"),
            RANGED_RAPID = button("Rapid", AttackType.RANGED_STANDARD, "Rapid"),
            RANGED_LONGRANGE = button("Longrange", AttackType.RANGED_STANDARD, "Longrange");
    public static final CombatStyle
            MAGIC_ACCURATE = button("Accurate", AttackType.MAGIC, "Accurate"),
            MAGIC_LONGRANGE = button("Longrange", AttackType.MAGIC, "Longrange"),
            MAGIC_AUTOCAST = button("Autocast", AttackType.MAGIC, "Autocast"),
            MAGIC_DEFENSIVE_AUTOCAST =
                    button("Defensive Autocast", AttackType.MAGIC, "Defensive Autocast");

    public static List<CombatStyle> values() {
        return Arrays.asList(
                UNARMED_PUNCH,
                MELEE_ACCURATE_STAB,
                MELEE_ACCURATE_SLASH,
                MELEE_ACCURATE_CRUSH,
                MELEE_AGGRESSIVE_SLASH,
                MELEE_AGGRESSIVE_CRUSH,
                MELEE_AGGRESSIVE_STAB,
                MELEE_CONTROLLED_STAB,
                MELEE_CONTROLLED_SLASH,
                MELEE_DEFENSIVE_STAB,
                MELEE_DEFENSIVE_SLASH,
                MELEE_DEFENSIVE_CRUSH,
                RANGED_ACCURATE,
                RANGED_RAPID,
                RANGED_LONGRANGE,
                MAGIC_ACCURATE,
                MAGIC_LONGRANGE,
                MAGIC_AUTOCAST,
                MAGIC_DEFENSIVE_AUTOCAST);
    }
}
