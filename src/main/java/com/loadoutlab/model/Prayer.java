/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Factual prayer effects: OSRS Wiki Prayer page.
 */
package com.loadoutlab.model;

/** Percentage effects, separate from prayer unlock/activation state. */
public enum Prayer {
    CLARITY_OF_THOUGHT(5, 0, 0, 0, 0, 0),
    IMPROVED_REFLEXES(10, 0, 0, 0, 0, 0),
    INCREDIBLE_REFLEXES(15, 0, 0, 0, 0, 0),
    BURST_OF_STRENGTH(0, 5, 0, 0, 0, 0),
    SUPERHUMAN_STRENGTH(0, 10, 0, 0, 0, 0),
    ULTIMATE_STRENGTH(0, 15, 0, 0, 0, 0),
    THICK_SKIN(0, 0, 5, 0, 0, 0),
    ROCK_SKIN(0, 0, 10, 0, 0, 0),
    STEEL_SKIN(0, 0, 15, 0, 0, 0),
    CHIVALRY(15, 18, 20, 0, 0, 0),
    PIETY(20, 23, 25, 0, 0, 0),
    SHARP_EYE(0, 0, 0, 5, 0, 0),
    HAWK_EYE(0, 0, 0, 10, 0, 0),
    EAGLE_EYE(0, 0, 0, 15, 0, 0),
    DEADEYE(0, 0, 5, 18, 0, 0),
    RIGOUR(0, 0, 25, 20, 0, 0),
    MYSTIC_WILL(0, 0, 0, 0, 5, 0),
    MYSTIC_LORE(0, 0, 0, 0, 10, 1),
    MYSTIC_MIGHT(0, 0, 0, 0, 15, 2),
    MYSTIC_VIGOUR(0, 0, 5, 0, 18, 3),
    AUGURY(0, 0, 25, 0, 25, 4);
    private final int attack, strength, defence, ranged, magic, damage;

    Prayer(int a, int s, int d, int r, int m, int bonus) {
        attack = a;
        strength = s;
        defence = d;
        ranged = r;
        magic = m;
        damage = bonus;
    }

    public int getAttackBonus() {
        return attack;
    }

    public int getStrengthBonus() {
        return strength;
    }

    public int getDefenceBonus() {
        return defence;
    }

    public int getRangedBonus() {
        return ranged;
    }

    public int getMagicBonus() {
        return magic;
    }

    public int getMagicDamageBonus() {
        return damage;
    }

    public int getSpriteId() {
        switch (this) {
            case CLARITY_OF_THOUGHT:
                return net.runelite.api.SpriteID.PRAYER_CLARITY_OF_THOUGHT;
            case IMPROVED_REFLEXES:
                return net.runelite.api.SpriteID.PRAYER_IMPROVED_REFLEXES;
            case INCREDIBLE_REFLEXES:
                return net.runelite.api.SpriteID.PRAYER_INCREDIBLE_REFLEXES;
            case BURST_OF_STRENGTH:
                return net.runelite.api.SpriteID.PRAYER_BURST_OF_STRENGTH;
            case SUPERHUMAN_STRENGTH:
                return net.runelite.api.SpriteID.PRAYER_SUPERHUMAN_STRENGTH;
            case ULTIMATE_STRENGTH:
                return net.runelite.api.SpriteID.PRAYER_ULTIMATE_STRENGTH;
            case THICK_SKIN:
                return net.runelite.api.SpriteID.PRAYER_THICK_SKIN;
            case ROCK_SKIN:
                return net.runelite.api.SpriteID.PRAYER_ROCK_SKIN;
            case STEEL_SKIN:
                return net.runelite.api.SpriteID.PRAYER_STEEL_SKIN;
            case CHIVALRY:
                return net.runelite.api.SpriteID.PRAYER_CHIVALRY;
            case PIETY:
                return net.runelite.api.SpriteID.PRAYER_PIETY;
            case SHARP_EYE:
                return net.runelite.api.SpriteID.PRAYER_SHARP_EYE;
            case HAWK_EYE:
                return net.runelite.api.SpriteID.PRAYER_HAWK_EYE;
            case EAGLE_EYE:
                return net.runelite.api.SpriteID.PRAYER_EAGLE_EYE;
            case DEADEYE:
                return net.runelite.api.SpriteID.PRAYER_DEADEYE;
            case RIGOUR:
                return net.runelite.api.SpriteID.PRAYER_RIGOUR;
            case MYSTIC_WILL:
                return net.runelite.api.SpriteID.PRAYER_MYSTIC_WILL;
            case MYSTIC_LORE:
                return net.runelite.api.SpriteID.PRAYER_MYSTIC_LORE;
            case MYSTIC_MIGHT:
                return net.runelite.api.SpriteID.PRAYER_MYSTIC_MIGHT;
            case MYSTIC_VIGOUR:
                return net.runelite.api.SpriteID.PRAYER_MYSTIC_VIGOUR;
            case AUGURY:
                return net.runelite.api.SpriteID.PRAYER_AUGURY;
            default:
                throw new IllegalStateException(name());
        }
    }

    public net.runelite.api.Prayer getRunelitePrayer() {
        return net.runelite.api.Prayer.valueOf(name());
    }

    public double getMeleeAttackMultiplier() {
        return 1 + attack / 100.0;
    }

    public double getMeleeStrengthMultiplier() {
        return 1 + strength / 100.0;
    }

    public double getRangedAttackMultiplier() {
        return 1 + ranged / 100.0;
    }

    public double getRangedStrengthMultiplier() {
        return 1 + (this == RIGOUR ? 23 : ranged) / 100.0;
    }

    public double getMagicAttackMultiplier() {
        return 1 + magic / 100.0;
    }

    public double getMagicDamageMultiplier() {
        return 1 + damage / 100.0;
    }

    public boolean isMeleePrayer() {
        return attack > 0 || strength > 0;
    }

    public boolean isRangedPrayer() {
        return ranged > 0;
    }

    public boolean isMagicPrayer() {
        return magic > 0;
    }
}
