package com.dpscalc.state;

public class EquipmentStats {
    private int stabAttack;
    private int slashAttack;
    private int crushAttack;
    private int magicAttack;
    private int rangedAttack;

    private int meleeStrength;
    private int rangedStrength;
    private int magicDamage;

    private int stabDefence;
    private int slashDefence;
    private int crushDefence;
    private int magicDefence;
    private int rangedDefence;

    private int prayerBonus;

    public int getStabAttack() { return stabAttack; }
    public void setStabAttack(int stabAttack) { this.stabAttack = stabAttack; }
    public void addStabAttack(int bonus) { this.stabAttack += bonus; }

    public int getSlashAttack() { return slashAttack; }
    public void setSlashAttack(int slashAttack) { this.slashAttack = slashAttack; }
    public void addSlashAttack(int bonus) { this.slashAttack += bonus; }

    public int getCrushAttack() { return crushAttack; }
    public void setCrushAttack(int crushAttack) { this.crushAttack = crushAttack; }
    public void addCrushAttack(int bonus) { this.crushAttack += bonus; }

    public int getMagicAttack() { return magicAttack; }
    public void setMagicAttack(int magicAttack) { this.magicAttack = magicAttack; }
    public void addMagicAttack(int bonus) { this.magicAttack += bonus; }

    public int getRangedAttack() { return rangedAttack; }
    public void setRangedAttack(int rangedAttack) { this.rangedAttack = rangedAttack; }
    public void addRangedAttack(int bonus) { this.rangedAttack += bonus; }

    public int getMeleeStrength() { return meleeStrength; }
    public void setMeleeStrength(int meleeStrength) { this.meleeStrength = meleeStrength; }
    public void addMeleeStrength(int bonus) { this.meleeStrength += bonus; }

    public int getRangedStrength() { return rangedStrength; }
    public void setRangedStrength(int rangedStrength) { this.rangedStrength = rangedStrength; }
    public void addRangedStrength(int bonus) { this.rangedStrength += bonus; }

    public int getMagicDamage() { return magicDamage; }
    public void setMagicDamage(int magicDamage) { this.magicDamage = magicDamage; }
    public void addMagicDamage(int bonus) { this.magicDamage += bonus; }

    public int getStabDefence() { return stabDefence; }
    public void setStabDefence(int stabDefence) { this.stabDefence = stabDefence; }
    public void addStabDefence(int bonus) { this.stabDefence += bonus; }

    public int getSlashDefence() { return slashDefence; }
    public void setSlashDefence(int slashDefence) { this.slashDefence = slashDefence; }
    public void addSlashDefence(int bonus) { this.slashDefence += bonus; }

    public int getCrushDefence() { return crushDefence; }
    public void setCrushDefence(int crushDefence) { this.crushDefence = crushDefence; }
    public void addCrushDefence(int bonus) { this.crushDefence += bonus; }

    public int getMagicDefence() { return magicDefence; }
    public void setMagicDefence(int magicDefence) { this.magicDefence = magicDefence; }
    public void addMagicDefence(int bonus) { this.magicDefence += bonus; }

    public int getRangedDefence() { return rangedDefence; }
    public void setRangedDefence(int rangedDefence) { this.rangedDefence = rangedDefence; }
    public void addRangedDefence(int bonus) { this.rangedDefence += bonus; }

    public int getPrayerBonus() { return prayerBonus; }
    public void setPrayerBonus(int prayerBonus) { this.prayerBonus = prayerBonus; }
    public void addPrayerBonus(int bonus) { this.prayerBonus += bonus; }

    public int getAttackBonusForType(AttackType type) {
        switch (type) {
            case STAB: return stabAttack;
            case SLASH: return slashAttack;
            case CRUSH: return crushAttack;
            case RANGED_LIGHT:
            case RANGED_STANDARD:
            case RANGED_HEAVY:
                return rangedAttack;
            case MAGIC: return magicAttack;
            default: return 0;
        }
    }

    @Override
    public String toString() {
        return String.format("EquipmentStats[atk: stab=%d, slash=%d, crush=%d, magic=%d, ranged=%d | str: melee=%d, ranged=%d, magic=%d%%]",
            stabAttack, slashAttack, crushAttack, magicAttack, rangedAttack,
            meleeStrength, rangedStrength, magicDamage);
    }
}
