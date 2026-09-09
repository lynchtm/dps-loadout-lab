/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently authored; see PROVENANCE.md.
 */
package com.loadoutlab.model;

public class EquipmentStats {
    private int stabAttack = 0;
    private int slashAttack = 0;
    private int crushAttack = 0;
    private int magicAttack = 0;
    private int rangedAttack = 0;
    private int meleeStrength = 0;
    private int rangedStrength = 0;
    private int magicDamage = 0;
    private int stabDefence = 0;
    private int slashDefence = 0;
    private int crushDefence = 0;
    private int magicDefence = 0;
    private int rangedDefence = 0;
    private int prayerBonus = 0;

    public int getStabAttack() {
        return stabAttack;
    }

    public void setStabAttack(int value) {
        stabAttack = value;
    }

    public int getSlashAttack() {
        return slashAttack;
    }

    public void setSlashAttack(int value) {
        slashAttack = value;
    }

    public int getCrushAttack() {
        return crushAttack;
    }

    public void setCrushAttack(int value) {
        crushAttack = value;
    }

    public int getMagicAttack() {
        return magicAttack;
    }

    public void setMagicAttack(int value) {
        magicAttack = value;
    }

    public int getRangedAttack() {
        return rangedAttack;
    }

    public void setRangedAttack(int value) {
        rangedAttack = value;
    }

    public int getMeleeStrength() {
        return meleeStrength;
    }

    public void setMeleeStrength(int value) {
        meleeStrength = value;
    }

    public int getRangedStrength() {
        return rangedStrength;
    }

    public void setRangedStrength(int value) {
        rangedStrength = value;
    }

    public int getMagicDamage() {
        return magicDamage;
    }

    public void setMagicDamage(int value) {
        magicDamage = value;
    }

    public int getStabDefence() {
        return stabDefence;
    }

    public void setStabDefence(int value) {
        stabDefence = value;
    }

    public int getSlashDefence() {
        return slashDefence;
    }

    public void setSlashDefence(int value) {
        slashDefence = value;
    }

    public int getCrushDefence() {
        return crushDefence;
    }

    public void setCrushDefence(int value) {
        crushDefence = value;
    }

    public int getMagicDefence() {
        return magicDefence;
    }

    public void setMagicDefence(int value) {
        magicDefence = value;
    }

    public int getRangedDefence() {
        return rangedDefence;
    }

    public void setRangedDefence(int value) {
        rangedDefence = value;
    }

    public int getPrayerBonus() {
        return prayerBonus;
    }

    public void setPrayerBonus(int value) {
        prayerBonus = value;
    }

    public void addStabAttack(int value) {
        stabAttack += value;
    }

    public void addSlashAttack(int value) {
        slashAttack += value;
    }

    public void addCrushAttack(int value) {
        crushAttack += value;
    }

    public void addMagicAttack(int value) {
        magicAttack += value;
    }

    public void addRangedAttack(int value) {
        rangedAttack += value;
    }

    public void addMeleeStrength(int value) {
        meleeStrength += value;
    }

    public void addRangedStrength(int value) {
        rangedStrength += value;
    }

    public void addMagicDamage(int value) {
        magicDamage += value;
    }

    public void addStabDefence(int value) {
        stabDefence += value;
    }

    public void addSlashDefence(int value) {
        slashDefence += value;
    }

    public void addCrushDefence(int value) {
        crushDefence += value;
    }

    public void addMagicDefence(int value) {
        magicDefence += value;
    }

    public void addRangedDefence(int value) {
        rangedDefence += value;
    }

    public void addPrayerBonus(int value) {
        prayerBonus += value;
    }

    public int getAttackBonusForType(AttackType type) {
        switch (type) {
            case STAB:
                return stabAttack;
            case SLASH:
                return slashAttack;
            case CRUSH:
                return crushAttack;
            case MAGIC:
                return magicAttack;
            default:
                return rangedAttack;
        }
    }
}
