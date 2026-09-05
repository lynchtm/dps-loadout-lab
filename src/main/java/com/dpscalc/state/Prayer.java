package com.dpscalc.state;

public enum Prayer {
    CLARITY_OF_THOUGHT(5, 0, 0, 0, 0, net.runelite.api.Prayer.CLARITY_OF_THOUGHT),
    IMPROVED_REFLEXES(10, 0, 0, 0, 0, net.runelite.api.Prayer.IMPROVED_REFLEXES),
    INCREDIBLE_REFLEXES(15, 0, 0, 0, 0, net.runelite.api.Prayer.INCREDIBLE_REFLEXES),

    BURST_OF_STRENGTH(0, 5, 0, 0, 0, net.runelite.api.Prayer.BURST_OF_STRENGTH),
    SUPERHUMAN_STRENGTH(0, 10, 0, 0, 0, net.runelite.api.Prayer.SUPERHUMAN_STRENGTH),
    ULTIMATE_STRENGTH(0, 15, 0, 0, 0, net.runelite.api.Prayer.ULTIMATE_STRENGTH),

    THICK_SKIN(0, 0, 5, 0, 0, net.runelite.api.Prayer.THICK_SKIN),
    ROCK_SKIN(0, 0, 10, 0, 0, net.runelite.api.Prayer.ROCK_SKIN),
    STEEL_SKIN(0, 0, 15, 0, 0, net.runelite.api.Prayer.STEEL_SKIN),

    CHIVALRY(15, 18, 20, 0, 0, net.runelite.api.Prayer.CHIVALRY),
    PIETY(20, 23, 25, 0, 0, net.runelite.api.Prayer.PIETY),

    SHARP_EYE(5, 5, 0, 5, 0, net.runelite.api.Prayer.SHARP_EYE),
    HAWK_EYE(10, 10, 0, 10, 0, net.runelite.api.Prayer.HAWK_EYE),
    EAGLE_EYE(15, 15, 0, 15, 0, 0, net.runelite.api.Prayer.EAGLE_EYE),
    DEADEYE(0, 0, 5, 18, 0, 0, net.runelite.api.Prayer.DEADEYE),
    RIGOUR(20, 23, 25, 20, 0, 0, net.runelite.api.Prayer.RIGOUR),

    MYSTIC_WILL(0, 0, 0, 0, 5, 0, net.runelite.api.Prayer.MYSTIC_WILL),
    MYSTIC_LORE(0, 0, 0, 0, 10, 10, net.runelite.api.Prayer.MYSTIC_LORE),
    MYSTIC_MIGHT(0, 0, 0, 0, 15, 20, net.runelite.api.Prayer.MYSTIC_MIGHT),
    MYSTIC_VIGOUR(0, 0, 5, 0, 18, 30, net.runelite.api.Prayer.MYSTIC_VIGOUR),
    AUGURY(0, 0, 25, 0, 25, 40, net.runelite.api.Prayer.AUGURY);

    private final int attackBonus;
    private final int strengthBonus;
    private final int defenceBonus;
    private final int rangedBonus;
    private final int magicBonus;
    private final int magicDamageBonus;
    private final net.runelite.api.Prayer runelitePrayer;

    Prayer(int attackBonus, int strengthBonus, int defenceBonus, int rangedBonus, int magicBonus,
           net.runelite.api.Prayer runelitePrayer) {
        this(attackBonus, strengthBonus, defenceBonus, rangedBonus, magicBonus, 0, runelitePrayer);
    }

    Prayer(int attackBonus, int strengthBonus, int defenceBonus, int rangedBonus, int magicBonus,
           int magicDamageBonus, net.runelite.api.Prayer runelitePrayer) {
        this.attackBonus = attackBonus;
        this.strengthBonus = strengthBonus;
        this.defenceBonus = defenceBonus;
        this.rangedBonus = rangedBonus;
        this.magicBonus = magicBonus;
        this.magicDamageBonus = magicDamageBonus;
        this.runelitePrayer = runelitePrayer;
    }

    public int getAttackBonus() { return attackBonus; }
    public int getStrengthBonus() { return strengthBonus; }
    public int getDefenceBonus() { return defenceBonus; }
    public int getRangedBonus() { return rangedBonus; }
    public int getMagicBonus() { return magicBonus; }
    public int getMagicDamageBonus() { return magicDamageBonus; }
    public net.runelite.api.Prayer getRunelitePrayer() { return runelitePrayer; }

    /**
     * Get the effective melee attack bonus from this prayer.
     */
    public double getMeleeAttackMultiplier() {
        return 1.0 + (attackBonus / 100.0);
    }

    /**
     * Get the effective melee strength bonus from this prayer.
     */
    public double getMeleeStrengthMultiplier() {
        return 1.0 + (strengthBonus / 100.0);
    }

    /**
     * Get the effective ranged attack multiplier from this prayer.
     * For ranged prayers, the ranged bonus applies to accuracy.
     */
    public double getRangedAttackMultiplier() {
        return 1.0 + (rangedBonus / 100.0);
    }

    /**
     * Get the effective ranged strength multiplier from this prayer.
     * For Rigour, strength bonus is 23%, for others it matches attack.
     */
    public double getRangedStrengthMultiplier() {
        if (this == RIGOUR) {
            return 1.23;
        }
        // For Sharp Eye, Hawk Eye, Eagle Eye, ranged bonus applies to both
        return 1.0 + (rangedBonus / 100.0);
    }

    /**
     * Get the effective magic attack multiplier from this prayer.
     */
    public double getMagicAttackMultiplier() {
        return 1.0 + (magicBonus / 100.0);
    }

    /**
     * Get the effective magic damage multiplier from this prayer.
     * Augury provides a 4% magic damage bonus in the current web calculator reference.
     */
    public double getMagicDamageMultiplier() {
        return 1.0 + (magicDamageBonus / 100.0);
    }

    /**
     * Check if this prayer is a melee prayer (affects melee stats).
     */
    public boolean isMeleePrayer() {
        return attackBonus > 0 || strengthBonus > 0;
    }

    /**
     * Check if this prayer is a ranged prayer.
     */
    public boolean isRangedPrayer() {
        return rangedBonus > 0;
    }

    /**
     * Check if this prayer is a magic prayer.
     */
    public boolean isMagicPrayer() {
        return magicBonus > 0 || magicDamageBonus > 0;
    }
}
