package com.dpscalc.calc;

import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.AttackType;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.PlayerState;

public class BaseCalc {

    protected final PlayerState player;
    protected final MonsterStats monster;

    public BaseCalc(PlayerState player, MonsterStats monster) {
        this.player = player;
        this.monster = monster;
    }

    public static double getNormalAccuracyRoll(int atk, int def) {
        int attack = atk;
        int defence = def;

        if (attack < 0) attack = Math.min(0, attack + 2);
        if (defence < 0) defence = Math.min(0, defence + 2);

        if (attack >= 0 && defence >= 0) {
            return standardRoll(attack, defence);
        }
        if (attack >= 0 && defence < 0) {
            return 1 - 1.0 / (-defence + 1) / (attack + 1);
        }
        if (attack < 0 && defence >= 0) {
            return 0;
        }
        if (attack < 0 && defence < 0) {
            return standardRoll(-defence, -attack);
        }
        return 0;
    }

    private static double standardRoll(int attack, int defence) {
        if (attack > defence) {
            return 1 - (defence + 2.0) / (2.0 * (attack + 1));
        }
        return attack / (2.0 * (defence + 1));
    }

    public static double getFangAccuracyRoll(int atk, int def) {
        int attack = atk;
        int defence = def;

        if (attack < 0) attack = Math.min(0, attack + 2);
        if (defence < 0) defence = Math.min(0, defence + 2);

        if (attack >= 0 && defence >= 0) {
            return fangStdRoll(attack, defence);
        }
        if (attack >= 0 && defence < 0) {
            return 1 - 1.0 / (-defence + 1) / (attack + 1);
        }
        if (attack < 0 && defence >= 0) {
            return 0;
        }
        if (attack < 0 && defence < 0) {
            return fangRvRoll(-defence, -attack);
        }
        return 0;
    }

    public static double getFixedAttackHitChance(int atk, int def) {
        int attack = atk;
        int defence = def;

        if (attack < 0) attack = Math.min(0, attack + 2);
        if (defence < 0) defence = Math.min(0, defence + 2);

        if (attack >= 0 && defence >= 0) {
            return attack > defence ? 1.0 : attack / (double) (defence + 1);
        }
        if (attack >= 0 && defence < 0) {
            return 1.0;
        }
        if (attack < 0 && defence >= 0) {
            return 0;
        }
        if (attack < 0 && defence < 0) {
            int reversedAttack = -defence;
            int reversedDefence = -attack;
            return reversedAttack > reversedDefence ? 1.0 : reversedAttack / (double) (reversedDefence + 1);
        }
        return 0;
    }

    private static double fangStdRoll(int attack, int defence) {
        if (attack > defence) {
            return 1 - (defence + 2.0) * (2.0 * defence + 3) / (attack + 1.0) / (attack + 1.0) / 6.0;
        }
        return attack * (4.0 * attack + 5) / 6.0 / (attack + 1.0) / (defence + 1.0);
    }

    private static double fangRvRoll(int attack, int defence) {
        if (attack < defence) {
            return attack * (defence * 6.0 - 2.0 * attack + 5) / 6.0 / (defence + 1.0) / (defence + 1.0);
        }
        return 1 - (defence + 2.0) * (2.0 * defence + 3) / 6.0 / (defence + 1.0) / (attack + 1.0);
    }

    protected boolean isUsingMeleeStyle() {
        AttackType type = player.getCombatStyle().getAttackType();
        return type.isMelee();
    }

    protected boolean isWearingVoidRobes() {
        return player.isWearingAny("Void knight top", "Void knight top (or)", "Elite void top", "Elite void top (or)")
            && player.isWearingAny("Void knight robe", "Void knight robe (or)", "Elite void robe", "Elite void robe (or)")
            && player.isWearing("Void knight gloves");
    }

    protected boolean isWearingEliteVoidRobes() {
        return player.isWearingAny("Elite void top", "Elite void top (or)")
            && player.isWearingAny("Elite void robe", "Elite void robe (or)")
            && player.isWearing("Void knight gloves");
    }

    protected boolean isWearingMeleeVoid() {
        return isWearingVoidRobes() && player.isWearingAny("Void melee helm", "Void melee helm (or)");
    }

    protected boolean isWearingRangedVoid() {
        return isWearingVoidRobes() && player.isWearingAny("Void ranger helm", "Void ranger helm (or)");
    }

    protected boolean isWearingEliteRangedVoid() {
        return isWearingEliteVoidRobes() && player.isWearingAny("Void ranger helm", "Void ranger helm (or)");
    }

    protected boolean isWearingMagicVoid() {
        return isWearingVoidRobes() && player.isWearingAny("Void mage helm", "Void mage helm (or)");
    }

    protected boolean isWearingEliteMagicVoid() {
        return isWearingEliteVoidRobes() && player.isWearingAny("Void mage helm", "Void mage helm (or)");
    }

    protected boolean isWearingSlayerHelmet() {
        return player.isWearingAny("Slayer helmet", "Slayer helmet (i)");
    }

    protected boolean isWearingBlackMask() {
        return isWearingImbuedBlackMask() || player.isWearingAny("Black mask", "Slayer helmet");
    }

    protected boolean isWearingImbuedBlackMask() {
        return player.isWearingAny("Black mask (i)", "Slayer helmet (i)", "V's helm");
    }

    protected boolean isWearingFang() {
        return player.isWearingAny("Osmumten's fang", "Osmumten's fang (or)");
    }

    protected boolean isWearingScythe() {
        return player.isWearing("Scythe of vitur") || player.isWearingItemContaining("of vitur");
    }

    protected boolean isWearingKeris() {
        return player.isWearingItemContaining("Keris");
    }

    protected boolean isWearingTzhaarWeapon() {
        return player.isWearingAny("Tzhaar-ket-em", "Tzhaar-ket-om", "Tzhaar-ket-om (t)",
            "Toktz-xil-ak", "Toktz-xil-ek", "Toktz-mej-tal");
    }

    protected boolean isWearingObsidian() {
        return player.isWearingAll("Obsidian helmet", "Obsidian platelegs", "Obsidian platebody");
    }

    protected boolean isWearingBerserkerNecklace() {
        return player.isWearingAny("Berserker necklace", "Berserker necklace (or)");
    }

    protected boolean isWearingCrystalBow() {
        return player.isWearing("Crystal bow") || player.isWearingItemContaining("Bow of faerdhinen");
    }

    protected boolean isWearingGodsword() {
        return player.isWearingAny("Ancient godsword", "Armadyl godsword",
            "Bandos godsword", "Saradomin godsword", "Zamorak godsword");
    }

    protected boolean isWearingDharok() {
        return player.isWearingAll("Dharok's helm", "Dharok's platebody",
            "Dharok's platelegs", "Dharok's greataxe");
    }

    protected boolean isWearingVeracs() {
        return player.isWearingAll("Verac's helm", "Verac's brassard",
            "Verac's plateskirt", "Verac's flail");
    }

    protected boolean hasAttribute(MonsterAttribute attr) {
        return monster != null && monster.hasAttribute(attr);
    }

    protected int applyFactor(int base, int numerator, int denominator) {
        return (int) ((double) base * numerator / denominator);
    }

    protected int addFactor(int base, int numerator, int denominator) {
        int addend = applyFactor(base, numerator, denominator);
        return (int) Math.floor(base + addend);
    }
}
