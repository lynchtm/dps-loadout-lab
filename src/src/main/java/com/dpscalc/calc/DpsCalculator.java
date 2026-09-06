package com.dpscalc.calc;

import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterConstants;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.equipment.AmmoApplicability;
import com.dpscalc.calc.distribution.AttackDistribution;
import com.dpscalc.calc.distribution.HitDistribution;
import com.dpscalc.calc.distribution.Hitsplat;
import com.dpscalc.calc.distribution.WeightedHit;
import com.dpscalc.state.AttackType;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.Prayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DpsCalculator extends BaseCalc {

    private static final double SECONDS_PER_TICK = 0.6;

    private boolean usingSpecialAttack;
    private int fangMinimumHit;

    public DpsCalculator(PlayerState player, MonsterStats monster) {
        super(player, monster);
    }

    public DpsCalculator(PlayerState player, MonsterStats monster, boolean usingSpecialAttack) {
        super(player, monster);
        this.usingSpecialAttack = usingSpecialAttack;
    }

    public DpsResult calculate() {
        DpsResult result = new DpsResult();

        if (monster == null) {
            return result;
        }

        int attackRoll = getMaxAttackRoll();
        int defenceRoll = getNpcDefenceRoll();
        double hitChance = calculateHitChance(attackRoll, defenceRoll);
        int attackSpeed = getAttackSpeed();

        if (isImmune()) {
            result.setAttackRoll(attackRoll);
            result.setDefenceRoll(defenceRoll);
            result.setAttackSpeed(attackSpeed);
            result.setAttackDistribution(getBaseAttackDistribution(hitChance, 0));
            result.setAccuracy(hitChance);
            result.setExpectedAttackSpeed(attackSpeed);
            return result;
        }

        int scalarMaxHit = getScalarMaxHit();

        AttackDistribution attackDistribution = getAttackDistribution(hitChance, scalarMaxHit);
        int directMaxHit = attackDistribution.getMax();
        double expectedDirectDamage = attackDistribution.getExpectedDamage();
        int dotMaxHit = getDotMaxHit();
        double expectedDotDamage = getExpectedDotDamage(hitChance);
        double expectedAttackSpeed = attackSpeed;
        double expectedDamage = expectedDirectDamage + expectedDotDamage;

        double damagePerTick = expectedDamage / expectedAttackSpeed;
        double dps = damagePerTick / SECONDS_PER_TICK;

        result.setMaxHit(directMaxHit + dotMaxHit);
        result.setAccuracy(hitChance);
        result.setAttackRoll(attackRoll);
        result.setDefenceRoll(defenceRoll);
        result.setAttackSpeed(attackSpeed);
        result.setAttackDistribution(attackDistribution);
        result.setScalarMaxHit(directMaxHit + dotMaxHit);
        result.setDirectMaxHit(directMaxHit);
        result.setDotMaxHit(dotMaxHit);
        result.setExpectedDirectDamage(expectedDirectDamage);
        result.setExpectedDotDamage(expectedDotDamage);
        result.setExpectedAttackSpeed(expectedAttackSpeed);
        result.setDistributionDamagePerTick(damagePerTick);
        result.setDps(dps);

        return result;
    }

    /**
     * Get the average damage per successful hit, accounting for the "min hit 1" mechanic.
     * In OSRS, when you pass the accuracy check, if you roll 0 damage it becomes 1.
     * This changes the average from maxHit/2 to maxHit/2 + 1/(maxHit+1).
     */
    private double getAverageHit(int maxHit) {
        if (maxHit <= 0) return 0;
        return maxHit / 2.0 + 1.0 / (maxHit + 1);
    }

    private double calculateScytheDps(double hitChance, int maxHit) {
        int size = monster.getSize();
        double totalDamage = 0;

        totalDamage += hitChance * getAverageHit(maxHit);

        if (size >= 2) {
            int hit2Max = maxHit / 2;
            totalDamage += hitChance * getAverageHit(hit2Max);
        }

        if (size >= 3) {
            int hit3Max = maxHit / 4;
            totalDamage += hitChance * getAverageHit(hit3Max);
        }

        return totalDamage;
    }

    private double calculateVeracsDps(double hitChance, int maxHit, int defenceRoll) {
        double normalDamage = hitChance * getAverageHit(maxHit);
        // Verac's special effect: 25% chance to ignore defense and hit 1 to maxHit+1
        double veracDamage = (maxHit + 2) / 2.0;
        return 0.75 * normalDamage + 0.25 * veracDamage;
    }

    public int getMaxAttackRoll() {
        if (!isManualCast() && isAmmoInvalid()) {
            return 0;
        }
        CombatStyle style = player.getCombatStyle();
        AttackType attackType = style.getAttackType();

        if (attackType.isMelee()) {
            return getMeleeAttackRoll();
        } else if (attackType.isRanged()) {
            return getRangedAttackRoll();
        } else if (attackType.isMagic()) {
            return getMagicAttackRoll();
        }
        return 0;
    }

    private int getMeleeAttackRoll() {
        CombatStyle style = player.getCombatStyle();

        int effectiveLevel = player.getBoostedAttack();
        effectiveLevel = applyPrayerBonus(effectiveLevel, true);

        int stanceBonus = 8;
        if ("Accurate".equals(style.getStance())) {
            stanceBonus += 3;
        } else if ("Controlled".equals(style.getStance())) {
            stanceBonus += 1;
        }
        effectiveLevel += stanceBonus;

        if (isWearingMeleeVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 11, 10);
        }

        int gearBonus = getAttackBonusForStyle() + 64;
        int baseRoll = effectiveLevel * gearBonus;
        int attackRoll = baseRoll;

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            int factor = player.isForinthrySurgeActive() ? 27 : 24;
            attackRoll = applyFactor(attackRoll, factor, 20);
        } else if (player.isWearingAny("Salve amulet (e)", "Salve amulet(ei)")
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            attackRoll = applyFactor(attackRoll, 6, 5);
        } else if (player.isWearingAny("Salve amulet", "Salve amulet(i)")
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            attackRoll = applyFactor(attackRoll, 7, 6);
        } else if (isWearingBlackMask() && player.isOnSlayerTask()) {
            attackRoll = applyFactor(attackRoll, 7, 6);
        }

        if (isWearingTzhaarWeapon() && isWearingObsidian()) {
            int obsidianBonus = applyFactor(baseRoll, 1, 10);
            attackRoll += obsidianBonus;
        }

        if (isRevWeaponApplicable()) {
            attackRoll = applyFactor(attackRoll, 3, 2);
        }

        if (player.isWearingAny("Arclight", "Emberlight") && hasAttribute(MonsterAttribute.DEMON)) {
            attackRoll = addFactor(attackRoll, demonbaneFactor(70), 100);
        }

        if (player.isWearingAny("Bone claws", "Burning claws") && hasAttribute(MonsterAttribute.DEMON)) {
            attackRoll = addFactor(attackRoll, demonbaneFactor(5), 100);
        }

        if (player.isWearing("Dragon hunter lance") && hasAttribute(MonsterAttribute.DRAGON)) {
            attackRoll = applyFactor(attackRoll, 6, 5);
        } else if (player.isWearing("Dragon hunter wand") && hasAttribute(MonsterAttribute.DRAGON)) {
            attackRoll = applyFactor(attackRoll, 7, 4);
        }

        if (player.isWearing("Keris partisan of breaching") && hasAttribute(MonsterAttribute.KALPHITE)) {
            attackRoll = applyFactor(attackRoll, 133, 100);
        }

        if (player.isWearing("Keris partisan of the sun")
            && MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId())
            && monster.getInputs().getMonsterCurrentHp() < monster.getHitpoints() / 4) {
            attackRoll = applyFactor(attackRoll, 5, 4);
        }

        if (player.isWearingAny("Blisterwood flail", "Blisterwood sickle") && isVampyre()) {
            attackRoll = applyFactor(attackRoll, 21, 20);
        }

        if (player.isWearingAny("Hallowed flail", "Sunspear") && isVampyre()) {
            attackRoll = applyFactor(attackRoll, 125, 100);
        }

        if (isWearingSilverWeapon() && player.isWearing("Efaritay's aid") && isVampyre()) {
            attackRoll = applyFactor(attackRoll, 23, 20);
        }

        if (player.isWearing("Granite hammer") && hasAttribute(MonsterAttribute.GOLEM)) {
            attackRoll = applyFactor(attackRoll, 13, 10);
        }

        if (style.getAttackType() == AttackType.CRUSH) {
            int inqPieces = countInquisitorPieces();
            if (inqPieces > 0) {
                if (player.isWearing("Inquisitor's mace")) {
                    inqPieces *= 5;
                } else if (inqPieces == 3) {
                    inqPieces = 5;
                }
                attackRoll = applyFactor(attackRoll, 200 + inqPieces, 200);
            }
        }

        if (usingSpecialAttack) {
            attackRoll = applyMeleeSpecAttackRollBonus(attackRoll);
        }

        return attackRoll;
    }

    private int applyMeleeSpecAttackRollBonus(int attackRoll) {
        if (isWearingGodsword()) {
            return applyFactor(attackRoll, 2, 1);
        } else if (isWearingFang() || player.isWearingAny("Arkan blade", "Granite hammer")) {
            return applyFactor(attackRoll, 3, 2);
        } else if (player.isWearingAny("Elder maul", "Dragon mace", "Dragon sword", "Dragon scimitar", "Abyssal whip")) {
            return applyFactor(attackRoll, 5, 4);
        } else if (player.isWearing("Dragon dagger")) {
            return applyFactor(attackRoll, 23, 20);
        } else if (player.isWearing("Abyssal dagger")) {
            return applyFactor(attackRoll, 5, 4);
        } else if (player.isWearing("Soulreaper axe")) {
            int stacks = Math.max(0, Math.min(5, player.getSoulreaperStacks()));
            return applyFactor(attackRoll, 100 + 6 * stacks, 100);
        } else if (player.isWearing("Brine sabre")) {
            return applyFactor(attackRoll, 2, 1);
        } else if (player.isWearing("Barrelchest anchor")) {
            return applyFactor(attackRoll, 2, 1);
        } else if (isSunspearFinisher()) {
            return applyFactor(attackRoll, 7, 10);
        }
        return attackRoll;
    }

    private int getRangedAttackRoll() {
        CombatStyle style = player.getCombatStyle();

        int effectiveLevel = player.getBoostedRanged();
        effectiveLevel = applyPrayerBonus(effectiveLevel, true);

        if ("Accurate".equals(style.getStance())) {
            effectiveLevel += 3;
        }
        effectiveLevel += 8;

        if (isWearingRangedVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 11, 10);
        }

        EquipmentStats stats = player.getEquipmentStats();
        int attackRoll = effectiveLevel * (stats.getRangedAttack() + 64);

        if (isWearingCrystalBow()) {
            int crystalPieces = countCrystalPieces();
            attackRoll = applyFactor(attackRoll, 20 + crystalPieces, 20);
        }

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            int factor = player.isForinthrySurgeActive() ? 27 : 24;
            attackRoll = applyFactor(attackRoll, factor, 20);
        } else if (player.isWearing("Salve amulet(ei)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            attackRoll = applyFactor(attackRoll, 6, 5);
        } else if (player.isWearing("Salve amulet(i)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            attackRoll = applyFactor(attackRoll, 7, 6);
        } else if (isWearingImbuedBlackMask() && player.isOnSlayerTask()) {
            attackRoll = applyFactor(attackRoll, 23, 20);
        }

        if (player.isWearing("Twisted bow")) {
            attackRoll = applyTwistedBowScaling(attackRoll, true);
            if (MonsterConstants.isP2Warden(monster.getId())) {
                attackRoll = applyTwistedBowScaling(attackRoll, true);
            }
        }

        if (player.isWearing("Dragon hunter crossbow") && hasAttribute(MonsterAttribute.DRAGON)) {
            attackRoll = applyFactor(attackRoll, 13, 10);
        }

        if (isRevWeaponApplicable()) {
            attackRoll = applyFactor(attackRoll, 3, 2);
        }

        if (player.isWearing("Scorching bow") && hasAttribute(MonsterAttribute.DEMON)) {
            attackRoll = addFactor(attackRoll, demonbaneFactor(30), 100);
        }

        if (usingSpecialAttack) {
            attackRoll = applyRangedSpecAttackRollBonus(attackRoll);
        }

        if (MonsterConstants.isTitanBoss(monster.getId()) && "Out of Melee Range".equals(monster.getInputs().getPhase())) {
            attackRoll = applyFactor(attackRoll, 6, 1);
        }

        return attackRoll;
    }

    private int applyRangedSpecAttackRollBonus(int attackRoll) {
        if (player.isWearingAny("Zaryte crossbow", "Webweaver bow") || isWearingBlowpipe()) {
            return applyFactor(attackRoll, 2, 1);
        } else if (isWearingMsb()) {
            return applyFactor(attackRoll, 10, 7);
        } else if (player.isWearingAny("Heavy ballista", "Light ballista")) {
            return applyFactor(attackRoll, 5, 4);
        } else if (player.isWearing("Rosewood blowpipe")) {
            return applyFactor(attackRoll, 4, 5);
        }
        return attackRoll;
    }

    private int getMagicAttackRoll() {
        CombatStyle style = player.getCombatStyle();

        int effectiveLevel = player.getBoostedMagic();
        effectiveLevel = applyPrayerBonus(effectiveLevel, true);

        if ("Accurate".equals(style.getStance())) {
            effectiveLevel += 2;
        }
        effectiveLevel += 9;

        if (isWearingMagicVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 29, 20);
        }

        EquipmentStats stats = player.getEquipmentStats();
        int baseRoll = effectiveLevel * (stats.getMagicAttack() + 64);
        int attackRoll = baseRoll;

        int additiveBonus = 0;
        boolean blackMaskBonus = false;

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            additiveBonus += player.isForinthrySurgeActive() ? 35 : 20;
        } else if (player.isWearing("Salve amulet(ei)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            additiveBonus += 20;
        } else if (player.isWearing("Salve amulet(i)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            additiveBonus += 15;
        } else if (isWearingImbuedBlackMask() && player.isOnSlayerTask()) {
            blackMaskBonus = true;
        }

        if (player.isWearing("Efaritay's aid") && isVampyre() && isWearingSilverWeapon()) {
            additiveBonus += 15;
        }

        if (isWearingSmokeStaff()) {
            additiveBonus += 10;
        }

        if (additiveBonus > 0) {
            attackRoll = applyFactor(attackRoll, 100 + additiveBonus, 100);
        }

        if (hasAttribute(MonsterAttribute.DRAGON)) {
            if (player.isWearing("Dragon hunter crossbow")) {
                attackRoll = applyFactor(attackRoll, 13, 10);
            } else if (player.isWearing("Dragon hunter lance")) {
                attackRoll = applyFactor(attackRoll, 6, 5);
            } else if (player.isWearing("Dragon hunter wand")) {
                attackRoll = applyFactor(attackRoll, 7, 4);
            }
        }

        if (blackMaskBonus) {
            attackRoll = applyFactor(attackRoll, 23, 20);
        }

        if (isRevWeaponApplicable()) {
            attackRoll = applyFactor(attackRoll, 3, 2);
        }

        if (player.isWearing("Tome of water")) {
            attackRoll = applyFactor(attackRoll, 6, 5);
        }

        if (usingSpecialAttack) {
            attackRoll = applyMagicSpecAttackRollBonus(attackRoll);
        }

        if (isSpellElementMatchingWeakness()) {
            attackRoll += applyFactor(baseRoll, monster.getWeaknessSeverity(), 100);
        }

        return attackRoll;
    }

    private int applyMagicSpecAttackRollBonus(int attackRoll) {
        if (isWearingAccursedSceptre()) {
            return applyFactor(attackRoll, 3, 2);
        } else if (player.isWearing("Volatile nightmare staff")) {
            return applyFactor(attackRoll, 3, 2);
        } else if (player.isWearing("Eye of ayak")) {
            return applyFactor(attackRoll, 2, 1);
        }
        return attackRoll;
    }

    public int getNpcDefenceRoll() {
        if (monster == null) return 0;

        CombatStyle style = player.getCombatStyle();
        AttackType defenceStyle = getDefenceStyleForAttack(style.getAttackType());

        ReducedMonsterStats reducedStats = getReducedMonsterStats();
        int defenceLevel;
        if (defenceStyle.isMagic() && !monster.usesDefenceForMagicDefence()) {
            defenceLevel = reducedStats.magicLevel;
        } else {
            defenceLevel = reducedStats.defenceLevel;
        }

        int effectiveLevel = defenceLevel + 9;
        int defenceBonus = getReducedDefenceBonus(defenceStyle, reducedStats) + 64;
        int defenceRoll = effectiveLevel * defenceBonus;

        boolean isCustomMonster = monster.getId() == -1;
        int toaInvocationLevel = monster.getInputs().getToaInvocationLevel();

        if (toaInvocationLevel > 0) {
            boolean isToaMonster = MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId());
            boolean isKephriOverlord = MonsterConstants.isKephriOverlord(monster.getId());

            if ((isToaMonster && !isKephriOverlord) || isCustomMonster) {
                defenceRoll = applyFactor(defenceRoll, 250 + toaInvocationLevel, 250);
            }
        }

        return defenceRoll;
    }

    private int getReducedDefenceBonus(AttackType defenceStyle, ReducedMonsterStats reducedStats) {
        if (defenceStyle.isMagic()) {
            return reducedStats.magicDefence;
        }
        if (defenceStyle.isRanged() && "Salamander".equals(player.getWeaponCategory())) {
            return (monster.getLightRangedDefence() + monster.getStandardRangedDefence()
                + monster.getHeavyRangedDefence()) / 3;
        }
        return monster.getDefenceForStyle(defenceStyle.getKey());
    }

    private ReducedMonsterStats getReducedMonsterStats() {
        ReducedMonsterStats stats = new ReducedMonsterStats(monster);
        MonsterInputs.DefenceReductions reductions = monster.getInputs().getDefenceReductions();
        int defenceFloor = getDefenceReductionFloor();

        if (reductions.getAccursedSceptre() > 0) {
            stats.defenceLevel = reduceDefenceToFloor(stats.defenceLevel * 17 / 20, defenceFloor);
            stats.magicLevel = stats.magicLevel * 17 / 20;
        } else if (reductions.getVulnerability() > 0) {
            stats.defenceLevel = reduceDefenceToFloor(stats.defenceLevel * 9 / 10, defenceFloor);
        }

        for (int i = 0; i < reductions.getElderMaul(); i++) {
            stats.defenceLevel = reduceDefenceToFloor(stats.defenceLevel - stats.defenceLevel * 35 / 100, defenceFloor);
        }
        for (int i = 0; i < reductions.getDwh(); i++) {
            stats.defenceLevel = reduceDefenceToFloor(stats.defenceLevel - stats.defenceLevel * 3 / 10, defenceFloor);
        }

        applyDemonbaneReduction(stats, reductions.getArclight(), hasAttribute(MonsterAttribute.DEMON) ? 2 : 1, 20, defenceFloor);
        applyDemonbaneReduction(stats, reductions.getEmberlight(), hasAttribute(MonsterAttribute.DEMON) ? 3 : 1, 20, defenceFloor);

        for (int i = 0; i < reductions.getTonalztic(); i++) {
            stats.defenceLevel = reduceDefenceToFloor(stats.defenceLevel - stats.magicLevel / 10, defenceFloor);
        }

        if (reductions.getSeercull() > 0) {
            stats.magicLevel -= reductions.getSeercull();
        }

        applyBgsReduction(stats, reductions.getBgs(), defenceFloor);

        if (reductions.getAyak() > 0 && stats.magicDefence > 0) {
            stats.magicDefence = Math.max(0, stats.magicDefence - reductions.getAyak());
        }

        return stats;
    }

    private void applyDemonbaneReduction(ReducedMonsterStats stats, int count, int numerator, int denominator, int defenceFloor) {
        if (count <= 0) {
            return;
        }
        stats.attackLevel -= count * (monster.getAttackLevel() * numerator / denominator + 1);
        stats.strengthLevel -= count * (monster.getStrengthLevel() * numerator / denominator + 1);
        stats.defenceLevel = reduceDefenceToFloor(
            stats.defenceLevel - count * (monster.getDefenceLevel() * numerator / denominator + 1),
            defenceFloor
        );
    }

    private void applyBgsReduction(ReducedMonsterStats stats, int damage, int defenceFloor) {
        int remainingDamage = damage;
        remainingDamage = reduceBgsDefence(stats, remainingDamage, defenceFloor);
        remainingDamage = reduceBgsSkill(stats.strengthLevel, remainingDamage, value -> stats.strengthLevel = value);
        remainingDamage = reduceBgsSkill(stats.attackLevel, remainingDamage, value -> stats.attackLevel = value);
        remainingDamage = reduceBgsSkill(stats.magicLevel, remainingDamage, value -> stats.magicLevel = value);
        reduceBgsSkill(stats.rangedLevel, remainingDamage, value -> stats.rangedLevel = value);
    }

    private int reduceBgsDefence(ReducedMonsterStats stats, int damage, int defenceFloor) {
        if (damage <= 0) {
            return 0;
        }
        int startLevel = stats.defenceLevel;
        stats.defenceLevel = reduceDefenceToFloor(startLevel - damage, defenceFloor);
        if (stats.defenceLevel > 0) {
            return 0;
        }
        return damage - startLevel;
    }

    private int reduceBgsSkill(int startLevel, int damage, SkillSetter setter) {
        if (damage <= 0) {
            return 0;
        }
        int newLevel = startLevel - damage;
        setter.set(newLevel);
        if (newLevel > 0) {
            return 0;
        }
        return damage - startLevel;
    }

    private int reduceDefenceToFloor(int value, int floor) {
        return Math.max(floor, value);
    }

    private int getDefenceReductionFloor() {
        int monsterId = monster.getId();
        if (MonsterConstants.contains(MonsterConstants.VERZIK_IDS, monsterId) || MonsterConstants.contains(MonsterConstants.VARDORVIS_IDS, monsterId)) {
            return monster.getDefenceLevel();
        }
        if (MonsterConstants.contains(MonsterConstants.SOTETSEG_IDS, monsterId)) return 100;
        if (MonsterConstants.contains(MonsterConstants.NIGHTMARE_IDS, monsterId)) return 120;
        if (MonsterConstants.contains(MonsterConstants.AKKHA_IDS, monsterId)) return 70;
        if (MonsterConstants.contains(MonsterConstants.BABA_IDS, monsterId)) return 60;
        if (MonsterConstants.contains(MonsterConstants.KEPHRI_UNSHIELDED_IDS, monsterId) || MonsterConstants.contains(MonsterConstants.KEPHRI_SHIELDED_IDS, monsterId)) return 60;
        if (MonsterConstants.contains(MonsterConstants.ZEBAK_IDS, monsterId)) return 50;
        if (MonsterConstants.contains(MonsterConstants.P3_WARDEN_IDS, monsterId)) return 120;
        if (MonsterConstants.contains(MonsterConstants.TOA_OBELISK_IDS, monsterId)) return 60;
        if (MonsterConstants.contains(MonsterConstants.NEX_IDS, monsterId)) return 250;
        if (MonsterConstants.contains(MonsterConstants.ARAXXOR_IDS, monsterId)) return 90;
        if (MonsterConstants.contains(MonsterConstants.HUEYCOATL_IDS, monsterId)) return 120;
        if (MonsterConstants.contains(MonsterConstants.YAMA_IDS, monsterId)) return 145;
        return 0;
    }

    private interface SkillSetter {
        void set(int value);
    }

    private static class ReducedMonsterStats {
        private int attackLevel;
        private int strengthLevel;
        private int defenceLevel;
        private int magicLevel;
        private int rangedLevel;
        private int magicDefence;

        private ReducedMonsterStats(MonsterStats monster) {
            this.attackLevel = monster.getAttackLevel();
            this.strengthLevel = monster.getStrengthLevel();
            this.defenceLevel = monster.getDefenceLevel();
            this.magicLevel = monster.getMagicLevel();
            this.rangedLevel = monster.getRangedLevel();
            this.magicDefence = monster.getMagicDefence();
        }
    }

    private AttackType getDefenceStyleForAttack(AttackType attackType) {
        if (!usingSpecialAttack) {
            return attackType.isRanged() ? getRangedDefenceType() : attackType;
        }

        if (player.isWearingAny("Dragon claws", "Dragon dagger", "Dragon halberd", "Dragon longsword",
                "Dragon scimitar", "Crystal halberd", "Abyssal dagger", "Saradomin sword", "Arkan blade")
            || isWearingGodsword()) {
            return AttackType.SLASH;
        } else if (player.isWearingAny("Arclight", "Emberlight", "Dragon sword")) {
            return AttackType.STAB;
        } else if (player.isWearingAny("Voidwaker", "Saradomin's blessed sword")) {
            return AttackType.MAGIC;
        } else if (player.isWearing("Dragon mace")) {
            return AttackType.CRUSH;
        }

        return attackType.isRanged() ? getRangedDefenceType() : attackType;
    }

    private AttackType getRangedDefenceType() {
        String category = player.getWeaponCategory();
        switch (category == null ? "" : category) {
            case "Thrown":
                return AttackType.RANGED_LIGHT;
            case "Bow":
            case "Salamander":
                return AttackType.RANGED_STANDARD;
            case "Crossbow":
            case "Chinchompas":
                return AttackType.RANGED_HEAVY;
            default:
                throw new IllegalStateException("Not a ranged weapon category: " + category);
        }
    }

    public double calculateHitChance(int attackRoll, int defenceRoll) {
        if (isGuaranteedAccuracy()) {
            return 1.0;
        }

        if (isSunspearFinisher()) {
            return BaseCalc.getFixedAttackHitChance(attackRoll, defenceRoll);
        }

        if (isWearingFang() && player.getCombatStyle().getAttackType() == AttackType.STAB) {
            double normalAcc = BaseCalc.getNormalAccuracyRoll(attackRoll, defenceRoll);
            if (MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(monster.getId())) {
                return 1 - Math.pow(1 - normalAcc, 2);
            }
            return BaseCalc.getFangAccuracyRoll(attackRoll, defenceRoll);
        }

        return BaseCalc.getNormalAccuracyRoll(attackRoll, defenceRoll);
    }

    private boolean isGuaranteedAccuracy() {
        int monsterId = monster.getId();
        AttackType attackType = player.getCombatStyle().getAttackType();
        String phase = monster.getInputs().getPhase();

        if (MonsterConstants.GUARANTEED_ACCURACY_MONSTERS.contains(monsterId)) {
            return true;
        }

        if (MonsterConstants.isDoomOfMokhaiotl(monsterId) && !"Normal".equals(phase)) {
            return true;
        }

        if (MonsterConstants.isVerzikP1(monsterId) && player.isWearing("Dawnbringer")) {
            return true;
        }

        if (MonsterConstants.isP2Warden(monsterId)) {
            return true;
        }

        if (monsterId == 7223) {
            return true;
        }

        if (MonsterConstants.isTormentedDemon(monsterId) && !"Shielded".equals(phase)) {
            return true;
        }

        if (MonsterConstants.isTitanElemental(monsterId) && attackType.isMagic()) {
            return false;
        }

        if (MonsterConstants.isEclipseMoon(monsterId) && "Clone".equals(monster.getVersion()) && isUsingMeleeStyle()) {
            return true;
        }

        if (attackType.isMagic() && MonsterConstants.ALWAYS_MAX_HIT_MAGIC.contains(monsterId)) {
            return true;
        }
        if (attackType.isRanged() && MonsterConstants.ALWAYS_MAX_HIT_RANGED.contains(monsterId)) {
            return true;
        }
        if (isUsingMeleeStyle() && MonsterConstants.ALWAYS_MAX_HIT_MELEE.contains(monsterId)) {
            return true;
        }

        if (usingSpecialAttack && player.isWearingAny("Voidwaker", "Dawnbringer")) {
            return true;
        }

        if (usingSpecialAttack && (player.isWearing("Seercull") || isWearingMlb())) {
            return true;
        }

        return false;
    }

    public double getTitanElementalMagicAccuracy() {
        int magicAttack = player.getEquipmentStats().getMagicAttack();
        double accuracy = Math.min(1.0, Math.max(0, magicAttack) / 100.0 + 0.3);
        if (isWearingEliteMagicVoid() || isWearingMagicVoid()) {
            accuracy = Math.min(1.0, accuracy * 1.45);
        }
        return accuracy;
    }

    public int getMaxHit() {
        return getDistributionMax(getScalarMaxHit());
    }

    private int getScalarMaxHit() {
        if (!isManualCast() && isAmmoInvalid()) {
            return 0;
        }
        CombatStyle style = player.getCombatStyle();
        AttackType attackType = style.getAttackType();

        if (MonsterConstants.ONE_HIT_MONSTERS.contains(monster.getId())) {
            return monster.getHitpoints();
        }

        if (attackType.isMelee()) {
            return getMeleeMaxHit();
        } else if (attackType.isRanged()) {
            return getRangedMaxHit();
        } else if (attackType.isMagic()) {
            return getMagicMaxHit();
        }
        return 0;
    }

    private int getMeleeMaxHit() {
        fangMinimumHit = 0;
        CombatStyle style = player.getCombatStyle();

        int baseLevel = player.getBoostedStrength();
        int effectiveLevel = baseLevel;
        effectiveLevel = applyPrayerBonus(effectiveLevel, false);

        if (player.isWearing("Soulreaper axe") && !usingSpecialAttack) {
            int stacks = Math.max(0, Math.min(5, player.getSoulreaperStacks()));
            int bonus = applyFactor(baseLevel, stacks * 6, 100);
            effectiveLevel += bonus;
        }

        int stanceBonus = 8;
        if ("Aggressive".equals(style.getStance())) {
            stanceBonus += 3;
        } else if ("Controlled".equals(style.getStance())) {
            stanceBonus += 1;
        }
        effectiveLevel += stanceBonus;

        if (isWearingMeleeVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 11, 10);
        }

        EquipmentStats stats = player.getEquipmentStats();
        int gearBonus = stats.getMeleeStrength() + 64;
        int baseMax = (effectiveLevel * gearBonus + 320) / 640;
        int maxHit = baseMax;

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            int factor = player.isForinthrySurgeActive() ? 27 : 24;
            maxHit = applyFactor(maxHit, factor, 20);
        } else if (player.isWearingAny("Salve amulet (e)", "Salve amulet(ei)")
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            maxHit = applyFactor(maxHit, 6, 5);
        } else if (player.isWearingAny("Salve amulet", "Salve amulet(i)")
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            maxHit = applyFactor(maxHit, 7, 6);
        } else if (isWearingBlackMask() && player.isOnSlayerTask()) {
            maxHit = applyFactor(maxHit, 7, 6);
        }

        if (player.isWearingAny("Arclight", "Emberlight") && hasAttribute(MonsterAttribute.DEMON)) {
            maxHit = addFactor(maxHit, demonbaneFactor(70), 100);
        }

        if (player.isWearingAny("Bone claws", "Burning claws") && hasAttribute(MonsterAttribute.DEMON)) {
            maxHit = addFactor(maxHit, demonbaneFactor(5), 100);
        }

        if (isWearingTzhaarWeapon() && isWearingObsidian()) {
            int obsidianBonus = applyFactor(baseMax, 1, 10);
            maxHit += obsidianBonus;
        }

        if (isWearingTzhaarWeapon() && isWearingBerserkerNecklace()) {
            maxHit = applyFactor(maxHit, 6, 5);
        }

        if (player.isWearing("Dragon hunter lance") && hasAttribute(MonsterAttribute.DRAGON)) {
            maxHit = applyFactor(maxHit, 6, 5);
        }

        if (player.isWearing("Dragon hunter wand") && hasAttribute(MonsterAttribute.DRAGON)) {
            maxHit = applyFactor(maxHit, 7, 5);
        }

        if (isWearingKeris() && hasAttribute(MonsterAttribute.KALPHITE)) {
            if (player.isWearing("Keris partisan of amascut")) {
                maxHit = applyFactor(maxHit, 115, 100);
            } else {
                maxHit = applyFactor(maxHit, 133, 100);
            }
        }

        if (player.isWearing("Barronite mace") && hasAttribute(MonsterAttribute.GOLEM)) {
            maxHit = applyFactor(maxHit, 23, 20);
        }

        if (player.isWearing("Granite hammer") && hasAttribute(MonsterAttribute.GOLEM)) {
            maxHit = applyFactor(maxHit, 13, 10);
        }

        if (isRevWeaponApplicable()) {
            maxHit = applyFactor(maxHit, 3, 2);
        }

        if (player.isWearingAny("Silverlight", "Darklight", "Silverlight (dyed)") && hasAttribute(MonsterAttribute.DEMON)) {
            maxHit = addFactor(maxHit, demonbaneFactor(60), 100);
        }

        if (player.isWearing("Leaf-bladed battleaxe") && hasAttribute(MonsterAttribute.LEAFY)) {
            maxHit = applyFactor(maxHit, 47, 40);
        }

        if (player.isWearing("Colossal blade") && monster != null) {
            maxHit += Math.min(monster.getSize() * 2, 10);
        }

        if (isWearingRatBoneWeapon() && hasAttribute(MonsterAttribute.RAT)) {
            maxHit += 10;
        }

        if (style.getAttackType() == AttackType.CRUSH) {
            int inqPieces = countInquisitorPieces();
            if (inqPieces > 0) {
                if (player.isWearing("Inquisitor's mace")) {
                    inqPieces *= 5;
                } else if (inqPieces == 3) {
                    inqPieces = 5;
                }
                maxHit = applyFactor(maxHit, 200 + inqPieces, 200);
            }
        }

        if (isWearingFang()) {
            fangMinimumHit = applyFactor(maxHit, 3, 20);
            if (!usingSpecialAttack) {
                maxHit -= fangMinimumHit;
            }
        }

        if (usingSpecialAttack) {
            maxHit = applyMeleeSpecMaxHitBonus(maxHit);
        }

        return Math.max(0, maxHit);
    }

    private int applyMeleeSpecMaxHitBonus(int maxHit) {
        if (isWearingGodsword()) {
            maxHit = applyFactor(maxHit, 11, 10);
        }

        if (player.isWearingAny("Bandos godsword", "Saradomin sword")) {
            maxHit = applyFactor(maxHit, 11, 10);
        } else if (player.isWearingAny("Armadyl godsword", "Dragon sword", "Dragon longsword", "Saradomin's blessed sword")) {
            maxHit = applyFactor(maxHit, 5, 4);
        } else if (player.isWearingAny("Dragon mace", "Dragon warhammer", "Arkan blade")) {
            maxHit = applyFactor(maxHit, 3, 2);
        } else if (player.isWearing("Voidwaker")) {
            int minHit = applyFactor(maxHit, 1, 2);
            maxHit += minHit;
        } else if (player.isWearingAny("Dragon halberd", "Crystal halberd")) {
            maxHit = applyFactor(maxHit, 11, 10);
        } else if (player.isWearing("Dragon dagger")) {
            maxHit = applyFactor(maxHit, 23, 20);
        } else if (player.isWearing("Abyssal dagger")) {
            maxHit = applyFactor(maxHit, 17, 20);
        } else if (player.isWearing("Barrelchest anchor")) {
            maxHit = applyFactor(maxHit, 110, 100);
        } else if (player.isWearing("Soulreaper axe")) {
            int stacks = Math.max(0, Math.min(5, player.getSoulreaperStacks()));
            maxHit = applyFactor(maxHit, 100 + 6 * stacks, 100);
        }

        return maxHit;
    }

    private int getRangedMaxHit() {
        CombatStyle style = player.getCombatStyle();

        int effectiveLevel = player.getBoostedRanged();

        boolean scalesWithStr = player.isWearingAny("Eclipse atlatl", "Hunter's spear");
        if (scalesWithStr) {
            effectiveLevel = player.getBoostedStrength();
        }

        effectiveLevel = applyPrayerBonus(effectiveLevel, false);

        if ("Accurate".equals(style.getStance())) {
            effectiveLevel += 3;
        }
        effectiveLevel += 8;

        if (isWearingEliteRangedVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 9, 8);
        } else if (isWearingRangedVoid()) {
            effectiveLevel = applyFactor(effectiveLevel, 11, 10);
        }

        EquipmentStats stats = player.getEquipmentStats();
        int bonusStr = scalesWithStr ? stats.getMeleeStrength() : stats.getRangedStrength();
        int gearBonus = bonusStr + 64;
        int baseMax = (effectiveLevel * gearBonus + 320) / 640;
        int maxHit = baseMax;

        if (isWearingCrystalBow()) {
            int crystalPieces = countCrystalPieces();
            maxHit = applyFactor(maxHit, 40 + crystalPieces, 40);
        }

        boolean needRevWeaponBonus = isRevWeaponApplicable();
        boolean needDragonbane = player.isWearing("Dragon hunter crossbow") && hasAttribute(MonsterAttribute.DRAGON);
        boolean needDemonbane = player.isWearing("Scorching bow") && hasAttribute(MonsterAttribute.DEMON);

        if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            int factor = player.isForinthrySurgeActive() ? 27 : 24;
            maxHit = applyFactor(maxHit, factor, 20);
        } else if ((player.isWearing("Salve amulet(ei)") || (scalesWithStr && player.isWearing("Salve amulet (e)")))
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            maxHit = applyFactor(maxHit, 6, 5);
        } else if ((player.isWearing("Salve amulet(i)") || (scalesWithStr && player.isWearing("Salve amulet")))
            && hasAttribute(MonsterAttribute.UNDEAD)) {
            maxHit = applyFactor(maxHit, 7, 6);
        } else if (scalesWithStr && isWearingBlackMask() && player.isOnSlayerTask()) {
            maxHit = applyFactor(maxHit, 7, 6);
        } else if (isWearingImbuedBlackMask() && player.isOnSlayerTask()) {
            int numerator = 23;
            if (needRevWeaponBonus) {
                needRevWeaponBonus = false;
                numerator += 10;
            }
            if (needDragonbane) {
                needDragonbane = false;
                numerator += 5;
            }
            if (needDemonbane) {
                needDemonbane = false;
                numerator += 6;
            }
            maxHit = applyFactor(maxHit, numerator, 20);
        }

        if (player.isWearing("Twisted bow")) {
            maxHit = applyTwistedBowScaling(maxHit, false);
        }

        if (needRevWeaponBonus) {
            maxHit = applyFactor(maxHit, 3, 2);
        }
        if (needDragonbane) {
            maxHit = applyFactor(maxHit, 5, 4);
        }
        if (needDemonbane) {
            maxHit = addFactor(maxHit, demonbaneFactor(30), 100);
        }

        if (isWearingRatBoneWeapon() && hasAttribute(MonsterAttribute.RAT)) {
            maxHit += 10;
        }

        if (player.isWearing("Tonalztics of ralos")) {
            maxHit = applyFactor(maxHit, 3, 4);
        }

        if (usingSpecialAttack) {
            maxHit = applyRangedSpecMaxHitBonus(maxHit);
        }

        if (MonsterConstants.isP2Warden(monster.getId())) {
            maxHit = applyP2WardensMaxHitModifier(maxHit);
        }

        return Math.max(0, maxHit);
    }

    private int applyRangedSpecMaxHitBonus(int maxHit) {
        if (isWearingBlowpipe()) {
            return applyFactor(maxHit, 3, 2);
        } else if (player.isWearing("Webweaver bow")) {
            int maxReduction = applyFactor(maxHit, 6, 10);
            return maxHit - maxReduction;
        } else if (player.isWearingAny("Heavy ballista", "Light ballista")) {
            return applyFactor(maxHit, 5, 4);
        } else if (player.isWearing("Rosewood blowpipe")) {
            return applyFactor(maxHit, 11, 10);
        }
        return maxHit;
    }

    private int getMagicMaxHit() {
        int maxHit = getBaseMagicMaxHit();
        if (maxHit == 0) return 0;

        if (usingSpecialAttack && player.isWearing("Eye of ayak")) {
            maxHit = applyFactor(maxHit, 13, 10);
        }

        int baseMaxHit = maxHit;
        int magicDmgBonus = player.getEquipmentStats().getMagicDamage();

        if (isWearingSmokeStaff() && isStandardSpellSelected()) {
            magicDmgBonus += 100;
        }

        boolean blackMaskBonus = false;

        if (player.isWearing("Salve amulet(ei)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            magicDmgBonus += 200;
        } else if (player.isWearing("Salve amulet(i)") && hasAttribute(MonsterAttribute.UNDEAD)) {
            magicDmgBonus += 150;
        } else if (player.isWearing("Amulet of avarice") && monster.getName() != null && monster.getName().startsWith("Revenant")) {
            magicDmgBonus += player.isForinthrySurgeActive() ? 350 : 200;
        } else if (isWearingImbuedBlackMask() && player.isOnSlayerTask()) {
            blackMaskBonus = true;
        }

        for (Prayer prayer : player.getActivePrayers()) {
            if (prayer.isMagicPrayer()) {
                magicDmgBonus += prayer.getMagicDamageBonus();
            }
        }

        maxHit = addFactor(maxHit, magicDmgBonus, 1000);

        if (blackMaskBonus) {
            maxHit = applyFactor(maxHit, 23, 20);
        }

        if (hasAttribute(MonsterAttribute.DRAGON)) {
            if (player.isWearing("Dragon hunter lance")) {
                maxHit = applyFactor(maxHit, 6, 5);
            } else if (player.isWearing("Dragon hunter wand")) {
                maxHit = applyFactor(maxHit, 7, 5);
            } else if (player.isWearing("Dragon hunter crossbow")) {
                maxHit = applyFactor(maxHit, 5, 4);
            }
        }

        if (isRevWeaponApplicable()) {
            maxHit = applyFactor(maxHit, 3, 2);
        }

        if (usingSpecialAttack && isWearingAccursedSceptre()) {
            maxHit = applyFactor(maxHit, 3, 2);
        }

        if (isSpellElementMatchingWeakness()) {
            maxHit += applyFactor(baseMaxHit, monster.getWeaknessSeverity(), 100);
        }

        if (isMatchingChargedTome()) {
            maxHit = applyFactor(maxHit, 11, 10);
        }

        if (MonsterConstants.isP2Warden(monster.getId())) {
            maxHit = applyP2WardensMaxHitModifier(maxHit);
        }

        return Math.max(0, maxHit);
    }

    private int getBaseMagicMaxHit() {
        int magicLevel = player.getBoostedMagic();
        if (player.getSpellName() != null) {
            return getSelectedSpellMaxHit(magicLevel);
        }
        String weapon = player.getWeaponName();

        if (weapon == null) return 0;

        if (weapon.contains("Trident of the seas")) {
            return Math.max(1, magicLevel / 3 - 5);
        }
        if (weapon.equals("Thammaron's sceptre")) {
            return Math.max(1, magicLevel / 3 - 8);
        }
        if (weapon.contains("Accursed sceptre")) {
            return Math.max(1, magicLevel / 3 - 6);
        }
        if (weapon.contains("Trident of the swamp")) {
            return Math.max(1, magicLevel / 3 - 2);
        }
        if (weapon.contains("Sanguinesti staff") || weapon.contains("Holy sanguinesti staff")) {
            return Math.max(1, magicLevel / 3 - 1);
        }
        if (weapon.contains("Tumeken's shadow")) {
            return Math.max(1, magicLevel / 3 + 1);
        }
        if (weapon.equals("Eye of ayak")) {
            return Math.max(1, magicLevel / 3 - 6);
        }
        if (weapon.contains("Warped sceptre")) {
            return Math.max(1, (8 * magicLevel + 96) / 37);
        }
        if (weapon.equals("Bone staff")) {
            return Math.max(1, magicLevel / 3 - 5) + 10;
        }
        if (weapon.equals("Starter staff")) {
            return 8;
        }
        if (weapon.equals("Dawnbringer")) {
            return Math.max(1, magicLevel / 6 - 1);
        }
        if (weapon.contains("Crystal staff (basic)") || weapon.contains("Corrupted staff (basic)")) {
            return 23;
        }
        if (weapon.contains("Crystal staff (attuned)") || weapon.contains("Corrupted staff (attuned)")) {
            return 31;
        }
        if (weapon.contains("Crystal staff (perfected)") || weapon.contains("Corrupted staff (perfected)")) {
            return 39;
        }
        if (weapon.equals("Swamp lizard")) {
            return (magicLevel * (56 + 64) + 320) / 640;
        }
        if (weapon.equals("Orange salamander")) {
            return (magicLevel * (59 + 64) + 320) / 640;
        }
        if (weapon.equals("Red salamander")) {
            return (magicLevel * (77 + 64) + 320) / 640;
        }
        if (weapon.equals("Black salamander")) {
            return (magicLevel * (92 + 64) + 320) / 640;
        }
        if (weapon.equals("Tecu salamander")) {
            return (magicLevel * (104 + 64) + 320) / 640;
        }

        return 0;
    }

    private int getSelectedSpellMaxHit(int magicLevel) {
        String spellName = player.getSpellName();
        if (spellName == null) {
            return 0;
        }
        if (!spellName.equals("Saradomin Strike") && spellName.endsWith(" Strike")) {
            if (magicLevel >= 13) return 8;
            if (magicLevel >= 9) return 6;
            if (magicLevel >= 5) return 4;
            return 2;
        }
        if (spellName.endsWith(" Bolt")) {
            if (magicLevel >= 35) return 12;
            if (magicLevel >= 29) return 11;
            if (magicLevel >= 23) return 10;
            return 9;
        }
        if (!spellName.equals("Iban Blast") && spellName.endsWith(" Blast")) {
            if (magicLevel >= 59) return 16;
            if (magicLevel >= 53) return 15;
            if (magicLevel >= 47) return 14;
            return 13;
        }
        if (spellName.endsWith(" Wave")) {
            if (magicLevel >= 75) return 20;
            if (magicLevel >= 70) return 19;
            if (magicLevel >= 65) return 18;
            return 17;
        }
        if (spellName.endsWith(" Surge")) {
            if (magicLevel >= 95) return 24;
            if (magicLevel >= 90) return 23;
            if (magicLevel >= 85) return 22;
            return 21;
        }
        return player.getSpellMaxHit();
    }

    private boolean isStandardSpellSelected() {
        return "standard".equals(player.getSpellbook());
    }

    private boolean isSpellElementMatchingWeakness() {
        return player.getSpellElement() != null
            && monster.getWeaknessElement() != null
            && player.getSpellElement().equals(monster.getWeaknessElement().getJsonName());
    }

    private boolean isMatchingChargedTome() {
        if (!"Charged".equals(player.getShieldVersion())) {
            return false;
        }
        String element = player.getSpellElement();
        return element != null
            && (player.isWearing("Tome of fire") && element.equals("fire")
                || player.isWearing("Tome of water") && element.equals("water")
                || player.isWearing("Tome of earth") && element.equals("earth"));
    }

    private int applyP2WardensMaxHitModifier(int maxHit) {
        int defenceRoll = getNpcDefenceRoll();
        int attackRoll = getMaxAttackRoll();

        int reducedNpcDefence = defenceRoll / 3;
        int accuracyDelta = Math.max(attackRoll - reducedNpcDefence, 0);

        int modifier = (int) Math.max(15, Math.min(40, 15 + (40 - 15) * accuracyDelta / 42000.0));
        int maxPctRange = 20;

        return (maxHit * (modifier + maxPctRange)) / 100;
    }

    private int getDistributionMax(int scalarMaxHit) {
        if (scalarMaxHit <= 0) {
            return 0;
        }
        if (isImmune()) {
            return 0;
        }
        int maxHit = scalarMaxHit;
        if (isWearingVeracs()) {
            maxHit = Math.max(maxHit, scalarMaxHit + 1);
        }
        if (isWearingScythe() && monster.getSize() >= 2) {
            maxHit = transformAccurateDamage(scalarMaxHit) + transformAccurateDamage(scalarMaxHit / 2);
            if (monster.getSize() >= 3) {
                maxHit += transformAccurateDamage(scalarMaxHit / 4);
            }
            return maxHit;
        }
        if (isWearingDharok()) {
            maxHit = getDharokScaledDamage(maxHit);
        }
        if (isUsingMeleeStyle() && isWearingKeris() && hasAttribute(MonsterAttribute.KALPHITE)) {
            maxHit = Math.max(maxHit, scalarMaxHit * 3);
        }
        if (isVampyre()) {
            maxHit = applyVampyreDamageScaling(maxHit);
        }
        if (isCorporealBeast() && !isWearingCorpbaneWeapon()) {
            maxHit /= 2;
        }
        maxHit = applyStyleDamageReduction(maxHit);
        if (isNonRubyBoltEffectApplicable()) {
            maxHit = Math.max(maxHit, nonRubyBoltMax(scalarMaxHit));
        }
        if (isRubyBoltEffectApplicable()) {
            int rubyDamage = rubyBoltDamage();
            maxHit = isZaryteCrossbowSpec() ? rubyDamage : Math.max(maxHit, rubyDamage);
        }
        int simpleSpecHitCount = getSimpleSpecHitCount();
        if (simpleSpecHitCount > 1) {
            maxHit *= simpleSpecHitCount;
        }
        if (isAbyssalDaggerSpec()) {
            maxHit += finishAccurateDamage(applyAttackerDamageScaling(scalarMaxHit));
        }
        return maxHit;
    }

    private double getExpectedDamage(double hitChance, int scalarMaxHit) {
        if (scalarMaxHit <= 0) {
            return 0;
        }
        double expected = transformedExpectedHit(hitChance, getMinimumHit(scalarMaxHit), scalarMaxHit);
        if (isWearingVeracs()) {
            expected = 0.75 * expected + 0.25 * transformedExpectedHit(1.0, 1, scalarMaxHit + 1);
        }
        if (isWearingScythe() && monster.getSize() >= 2) {
            expected = transformedExpectedHit(hitChance, getMinimumHit(scalarMaxHit), scalarMaxHit);
            expected += transformedExpectedHit(hitChance, getMinimumHit(scalarMaxHit), scalarMaxHit / 2);
            if (monster.getSize() >= 3) {
                expected += transformedExpectedHit(hitChance, getMinimumHit(scalarMaxHit), scalarMaxHit / 4);
            }
        }
        if (isUsingMeleeStyle() && isWearingKeris() && hasAttribute(MonsterAttribute.KALPHITE)) {
            double standard = transformedExpectedHit(hitChance, getMinimumHit(scalarMaxHit), scalarMaxHit);
            double tripleDamageProc = transformedTripleDamageExpectedHit(hitChance, getMinimumHit(scalarMaxHit), scalarMaxHit);
            expected = standard * 50.0 / 51.0 + tripleDamageProc / 51.0;
        }
        if (isNonRubyBoltEffectApplicable()) {
            expected = nonRubyBoltExpectedDamage(expected, hitChance, scalarMaxHit);
        }
        if (isRubyBoltEffectApplicable()) {
            double rubyExpected = rubyBoltDamage();
            double rubyChance = 0.06 * (player.isKandarinDiary() ? 1.1 : 1.0);
            if (isZaryteCrossbowSpec()) {
                expected = (hitChance + (1.0 - hitChance) * rubyChance) * rubyExpected;
            } else {
                expected = rubyExpected * rubyChance + expected * (1.0 - rubyChance);
            }
        }
        int simpleSpecHitCount = getSimpleSpecHitCount();
        if (simpleSpecHitCount > 1) {
            expected *= simpleSpecHitCount;
        }
        if (isAbyssalDaggerSpec()) {
            expected += hitChance * transformedExpectedHit(1.0, getMinimumHit(scalarMaxHit), scalarMaxHit);
        }
        return expected;
    }

    private boolean isAbyssalDaggerSpec() {
        return usingSpecialAttack && player.isWearing("Abyssal dagger");
    }

    private int getSimpleSpecHitCount() {
        if (!usingSpecialAttack) {
            return 1;
        }
        if (player.isWearingAny("Dragon dagger", "Dragon knife", "Rosewood blowpipe") || isWearingMsb()) {
            return 2;
        }
        if (player.isWearing("Webweaver bow")) {
            return 4;
        }
        return 1;
    }

    private double transformedTripleDamageExpectedHit(double hitChance, int minHit, int maxHit) {
        if (maxHit < minHit || maxHit < 0) {
            return 0;
        }
        double expected = 0;
        double probability = hitChance / (maxHit - minHit + 1);
        for (int damage = minHit; damage <= maxHit; damage++) {
            expected += probability * transformAccurateDamage(damage * 3);
        }
        return expected;
    }

    private double transformedExpectedHit(double hitChance, int minHit, int maxHit) {
        if (maxHit < minHit || maxHit < 0) {
            return 0;
        }
        return getTransformedHitDistribution(hitChance, minHit, maxHit).getExpectedValue();
    }

    private AttackDistribution getBaseAttackDistribution(double hitChance, int scalarMaxHit) {
        if (scalarMaxHit <= 0) {
            return AttackDistribution.single(HitDistribution.deterministic(Hitsplat.inaccurate()));
        }
        return AttackDistribution.single(getTransformedHitDistribution(
            hitChance, getMinimumHit(scalarMaxHit), scalarMaxHit));
    }

    private AttackDistribution getAttackDistribution(double hitChance, int scalarMaxHit) {
        if (scalarMaxHit <= 0) return getBaseAttackDistribution(hitChance, scalarMaxHit);
        int minHit = getMinimumHit(scalarMaxHit);
        if (isSunspearSpec()) {
            int damage = transformAccurateDamage(scalarMaxHit * 7 / 10);
            return AttackDistribution.single(HitDistribution.single(
                hitChance, Collections.singletonList(Hitsplat.accurate(damage))));
        }
        HitDistribution standard = getTransformedHitDistribution(hitChance, minHit, scalarMaxHit);
        if (isWearingVeracs()) {
            List<WeightedHit> outcomes = new ArrayList<>();
            outcomes.addAll(standard.scaleProbability(0.75).getOutcomes());
            outcomes.addAll(getTransformedHitDistribution(1.0, 1, scalarMaxHit + 1)
                .scaleProbability(0.25).getOutcomes());
            return AttackDistribution.single(new HitDistribution(outcomes).flatten());
        }
        if (isWearingScythe() && monster.getSize() >= 2) {
            List<HitDistribution> splats = new ArrayList<>();
            splats.add(standard);
            splats.add(getTransformedHitDistribution(hitChance,
                minHit, Math.max(minHit, scalarMaxHit / 2)));
            if (monster.getSize() >= 3) {
                splats.add(getTransformedHitDistribution(hitChance,
                    minHit, Math.max(minHit, scalarMaxHit / 4)));
            }
            return new AttackDistribution(splats);
        }
        if (isUsingMeleeStyle() && isWearingKeris() && hasAttribute(MonsterAttribute.KALPHITE)) {
            HitDistribution raw = HitDistribution.linear(hitChance, minHit, scalarMaxHit);
            List<WeightedHit> outcomes = new ArrayList<>();
            outcomes.addAll(raw.scaleProbability(50.0 / 51.0).getOutcomes());
            outcomes.addAll(raw.scaleProbability(1.0 / 51.0).scaleDamage(3, 1).getOutcomes());
            return AttackDistribution.single(finishDistribution(new HitDistribution(outcomes)));
        }
        int hitCount = getSimpleSpecHitCount();
        if (hitCount > 1) {
            List<HitDistribution> splats = new ArrayList<>();
            for (int index = 0; index < hitCount; index++) splats.add(standard);
            return new AttackDistribution(splats);
        }
        if (isAbyssalDaggerSpec()) {
            HitDistribution second = getTransformedHitDistribution(1.0, minHit, scalarMaxHit);
            HitDistribution dagger = standard.transform(hitsplat ->
                HitDistribution.deterministic(hitsplat).zip(second), false);
            return AttackDistribution.single(dagger);
        }
        if (isNonRubyBoltEffectApplicable()) {
            return AttackDistribution.single(getNonRubyBoltDistribution(hitChance, minHit, scalarMaxHit));
        }
        if (isRubyBoltEffectApplicable()) {
            return AttackDistribution.single(getRubyBoltDistribution(standard));
        }
        return AttackDistribution.single(standard);
    }

    private HitDistribution getNonRubyBoltDistribution(double hitChance, int minHit, int scalarMaxHit) {
        HitDistribution preTarget = HitDistribution.linear(hitChance, minHit, scalarMaxHit)
            .transform(hitsplat -> HitDistribution.deterministic(new Hitsplat(
                applyAttackerDamageScaling(hitsplat.getDamage()), hitsplat.isAccurate())), false);
        BoltEffect effect = getNonRubyBoltType();
        double chance;
        int effectDamage;
        boolean accurateOnly;
        switch (effect) {
            case OPAL:
                chance = 0.05 * kandarinBoltFactor();
                effectDamage = opalBoltBonus();
                accurateOnly = false;
                break;
            case PEARL:
                chance = 0.06 * kandarinBoltFactor();
                effectDamage = pearlBoltBonus();
                accurateOnly = false;
                break;
            case DRAGONSTONE:
                chance = 0.06 * kandarinBoltFactor();
                effectDamage = dragonstoneBoltBonus();
                accurateOnly = true;
                break;
            case DIAMOND:
                return finishDistribution(effectReplacementDistribution(
                    preTarget, 0.10 * kandarinBoltFactor(), diamondBoltEffectMax(), false));
            case ONYX:
                return finishDistribution(effectReplacementDistribution(
                    preTarget, 0.11 * kandarinBoltFactor(), onyxBoltEffectMax(), true));
            default:
                return finishDistribution(preTarget);
        }
        HitDistribution effected = preTarget.transform(hitsplat -> {
            if (!hitsplat.isAccurate() && accurateOnly) return HitDistribution.deterministic(hitsplat);
            if (hitsplat.isAccurate() && isZaryteCrossbowSpec()) {
                return HitDistribution.deterministic(Hitsplat.accurate(hitsplat.getDamage() + effectDamage));
            }
            return new HitDistribution(java.util.Arrays.asList(
                new WeightedHit(chance, Collections.singletonList(
                    new Hitsplat(hitsplat.getDamage() + effectDamage, hitsplat.isAccurate()))),
                new WeightedHit(1.0 - chance, Collections.singletonList(hitsplat))));
        });
        return finishDistribution(effected);
    }

    private HitDistribution effectReplacementDistribution(
        HitDistribution base, double chance, int effectMax, boolean accurateOnly) {
        HitDistribution effect = HitDistribution.linear(1.0, 0, effectMax);
        return base.transform(hitsplat -> {
            if (!hitsplat.isAccurate() && accurateOnly) return HitDistribution.deterministic(hitsplat);
            if (hitsplat.isAccurate() && isZaryteCrossbowSpec()) return effect;
            List<WeightedHit> outcomes = new ArrayList<>(effect.scaleProbability(chance).getOutcomes());
            outcomes.add(new WeightedHit(1.0 - chance, Collections.singletonList(hitsplat)));
            return new HitDistribution(outcomes);
        });
    }

    private HitDistribution getRubyBoltDistribution(HitDistribution base) {
        double chance = 0.06 * kandarinBoltFactor();
        HitDistribution effect = HitDistribution.deterministic(Hitsplat.accurate(rubyBoltDamage()));
        return base.transform(hitsplat -> {
            if (hitsplat.isAccurate() && isZaryteCrossbowSpec()) return effect;
            List<WeightedHit> outcomes = new ArrayList<>(effect.scaleProbability(chance).getOutcomes());
            outcomes.add(new WeightedHit(1.0 - chance, Collections.singletonList(hitsplat)));
            return new HitDistribution(outcomes);
        });
    }

    private HitDistribution finishDistribution(HitDistribution distribution) {
        return distribution.transform(hitsplat -> HitDistribution.deterministic(new Hitsplat(
            hitsplat.isAccurate()
                ? finishAccurateDamage(hitsplat.getDamage())
                : finishInaccurateDamage(hitsplat.getDamage()),
            hitsplat.isAccurate())));
    }

    private int getDotMaxHit() {
        if (!usingSpecialAttack) return 0;
        if (player.isWearing("Scorching bow")) return hasAttribute(MonsterAttribute.DEMON) ? 5 : 1;
        if (player.isWearing("Arkan blade")) return 10;
        return 0;
    }

    private double getExpectedDotDamage(double hitChance) {
        if (!usingSpecialAttack) return 0;
        if (player.isWearing("Scorching bow")) return hasAttribute(MonsterAttribute.DEMON) ? 5 : 1;
        if (player.isWearing("Arkan blade")) return 10 * hitChance;
        return 0;
    }

    private HitDistribution getTransformedHitDistribution(double hitChance, int minHit, int maxHit) {
        return HitDistribution.linear(hitChance, minHit, maxHit).transform(hitsplat ->
            HitDistribution.deterministic(new Hitsplat(
                transformAccurateDamage(hitsplat.getDamage()), hitsplat.isAccurate())), false);
    }

    private int transformAccurateDamage(int damage) {
        if (isImmune()) {
            return 0;
        }
        return finishAccurateDamage(applyAttackerDamageScaling(damage));
    }

    private int applyAttackerDamageScaling(int damage) {
        int transformed = damage;
        if (isWearingDharok()) {
            transformed = getDharokScaledDamage(transformed);
        }
        if (isVampyre()) {
            transformed = applyVampyreDamageScaling(transformed);
        }
        return transformed;
    }

    private int finishAccurateDamage(int damage) {
        int transformed = damage;
        boolean reducedByTarget = false;
        if (isCorporealBeast() && !isWearingCorpbaneWeapon()) {
            transformed /= 2;
            reducedByTarget = true;
        }
        boolean styleReduction = isStyleDamageReductionApplicable();
        transformed = applyStyleDamageReduction(transformed);
        reducedByTarget = reducedByTarget || styleReduction;
        transformed = applyVampyreTargetDamageLimit(transformed);
        return Math.max(transformed, reducedByTarget ? 0 : 1);
    }

    private int finishInaccurateDamage(int damage) {
        int transformed = damage;
        if (isCorporealBeast() && !isWearingCorpbaneWeapon()) {
            transformed /= 2;
        }
        transformed = applyStyleDamageReduction(transformed);
        return applyVampyreTargetDamageLimit(transformed);
    }

    private int applyStyleDamageReduction(int damage) {
        if (isStyleDamageReductionApplicable()) {
            return damage / 3;
        }
        return damage;
    }

    private boolean isStyleDamageReductionApplicable() {
        AttackType attackType = player.getCombatStyle().getAttackType();
        int monsterId = monster.getId();
        if (attackType.isMagic()
            && (MonsterConstants.isOlmMeleeHand(monsterId) || MonsterConstants.isOlmHead(monsterId))) {
            return true;
        }
        if (attackType.isRanged()
            && (MonsterConstants.isOlmMageHand(monsterId) || MonsterConstants.isOlmMeleeHand(monsterId))) {
            return true;
        }
        return false;
    }

    private int getDharokScaledDamage(int damage) {
        int missingHp = player.getHitpointsLevel() - player.getCurrentHitpoints();
        return applyFactor(damage, 10000 + missingHp * player.getHitpointsLevel(), 10000);
    }

    private int getMinimumHit(int scalarMaxHit) {
        if (isWearingFang()) {
            return fangMinimumHit;
        }
        return 0;
    }

    private double averageAccurateHit(int minHit, int maxHit) {
        if (maxHit <= 0) {
            return 0;
        }
        if (minHit <= 0) {
            return getAverageHit(maxHit);
        }
        return (minHit + maxHit) / 2.0;
    }

    private int applyVampyreDamageScaling(int damage) {
        if (player.isWearingAny("Blisterwood flail", "Hallowed flail", "Blisterwood stake")) {
            return applyFactor(applyEfaritayDamageScaling(damage), 5, 4);
        }
        if (player.isWearing("Sunspear") && !usingSpecialAttack) {
            return applyFactor(applyEfaritayDamageScaling(damage), 3, 2);
        }
        if (player.isWearing("Blisterwood sickle")) {
            return applyFactor(applyEfaritayDamageScaling(damage), 23, 20);
        }
        if (player.isWearing("Ivandis flail")) {
            return applyFactor(applyEfaritayDamageScaling(damage), 6, 5);
        }
        if (player.isWearing("Rod of ivandis") && !hasAttribute(MonsterAttribute.VAMPYRE_3)) {
            return applyFactor(applyEfaritayDamageScaling(damage), 11, 10);
        }
        if (isWearingSilverWeapon() && hasAttribute(MonsterAttribute.VAMPYRE_1)) {
            return applyFactor(applyEfaritayDamageScaling(damage), 11, 10);
        }
        return damage;
    }

    private double applyVampyreDamageScaling(double damage) {
        if (player.isWearingAny("Blisterwood flail", "Hallowed flail", "Blisterwood stake")) {
            return applyEfaritayDamageScaling(damage) * 5.0 / 4.0;
        }
        if (player.isWearing("Sunspear") && !usingSpecialAttack) {
            return applyEfaritayDamageScaling(damage) * 3.0 / 2.0;
        }
        if (player.isWearing("Blisterwood sickle")) {
            return applyEfaritayDamageScaling(damage) * 23.0 / 20.0;
        }
        if (player.isWearing("Ivandis flail")) {
            return applyEfaritayDamageScaling(damage) * 6.0 / 5.0;
        }
        if (player.isWearing("Rod of ivandis") && !hasAttribute(MonsterAttribute.VAMPYRE_3)) {
            return applyEfaritayDamageScaling(damage) * 11.0 / 10.0;
        }
        if (isWearingSilverWeapon() && hasAttribute(MonsterAttribute.VAMPYRE_1)) {
            return applyEfaritayDamageScaling(damage) * 11.0 / 10.0;
        }
        return damage;
    }

    private int applyEfaritayDamageScaling(int damage) {
        return player.isWearing("Efaritay's aid") ? applyFactor(damage, 11, 10) : damage;
    }

    private double applyEfaritayDamageScaling(double damage) {
        return player.isWearing("Efaritay's aid") ? damage * 11.0 / 10.0 : damage;
    }

    private int applyVampyreTargetDamageLimit(int damage) {
        if (!hasAttribute(MonsterAttribute.VAMPYRE_2)) {
            return damage;
        }
        if (!wearingVampyrebane(MonsterAttribute.VAMPYRE_2) && player.isWearing("Efaritay's aid")) {
            return damage / 2;
        }
        if (isWearingSilverWeapon()) {
            return Math.min(damage, 10);
        }
        return damage;
    }

    private boolean isNonRubyBoltEffectApplicable() {
        return isCrossbowAttack() && !isRubyBoltEffectApplicable() && getNonRubyBoltType() != BoltEffect.NONE;
    }

    private double nonRubyBoltExpectedDamage(double baseExpected, double hitChance, int scalarMaxHit) {
        BoltEffect effect = getNonRubyBoltType();
        switch (effect) {
            case OPAL:
                return bonusBoltExpected(baseExpected, hitChance, scalarMaxHit, 0.05 * kandarinBoltFactor(), opalBoltBonus(), false);
            case PEARL:
                return bonusBoltExpected(baseExpected, hitChance, scalarMaxHit, 0.06 * kandarinBoltFactor(), pearlBoltBonus(), false);
            case DRAGONSTONE:
                return bonusBoltExpected(baseExpected, hitChance, scalarMaxHit, 0.06 * kandarinBoltFactor(), dragonstoneBoltBonus(), true);
            case DIAMOND:
                return effectDistributionBoltExpected(baseExpected, hitChance, 0.10 * kandarinBoltFactor(), diamondBoltEffectMax(), false);
            case ONYX:
                return effectDistributionBoltExpected(baseExpected, hitChance, 0.11 * kandarinBoltFactor(), onyxBoltEffectMax(), true);
            default:
                return baseExpected;
        }
    }

    private double bonusBoltExpected(double baseExpected, double hitChance, int scalarMaxHit, double chance, int bonusDamage, boolean accurateOnly) {
        double procExpected = 0;
        int minHit = getMinimumHit(scalarMaxHit);
        int rollCount = scalarMaxHit - minHit + 1;
        if (rollCount > 0) {
            double probability = hitChance / rollCount;
            for (int damage = minHit; damage <= scalarMaxHit; damage++) {
                int preTarget = applyAttackerDamageScaling(damage) + bonusDamage;
                procExpected += probability * finishAccurateDamage(preTarget);
            }
        }
        if (!accurateOnly) {
            procExpected += (1.0 - hitChance) * finishInaccurateDamage(bonusDamage);
        }
        if (isZaryteCrossbowSpec()) {
            double forcedAccurate = 0;
            if (rollCount > 0) {
                double probability = hitChance / rollCount;
                for (int damage = minHit; damage <= scalarMaxHit; damage++) {
                    forcedAccurate += probability * finishAccurateDamage(applyAttackerDamageScaling(damage) + bonusDamage);
                }
            }
            double inaccurateProc = accurateOnly ? 0 : (1.0 - hitChance) * chance * finishInaccurateDamage(bonusDamage);
            return forcedAccurate + inaccurateProc;
        }
        return chance * procExpected + (1.0 - chance) * baseExpected;
    }

    private double effectDistributionBoltExpected(double baseExpected, double hitChance, double chance, int effectMax, boolean accurateOnly) {
        double effectExpected = uniformEffectExpected(effectMax);
        if (isZaryteCrossbowSpec()) {
            if (accurateOnly) {
                return hitChance * effectExpected;
            }
            return (hitChance + (1.0 - hitChance) * chance) * effectExpected;
        }
        if (accurateOnly) {
            return hitChance * chance * effectExpected + (1.0 - chance) * baseExpected;
        }
        return chance * effectExpected + (1.0 - chance) * baseExpected;
    }

    private double uniformEffectExpected(int effectMax) {
        if (effectMax < 0) {
            return 0;
        }
        double expected = 0;
        double probability = 1.0 / (effectMax + 1);
        for (int damage = 0; damage <= effectMax; damage++) {
            expected += probability * finishAccurateDamage(damage);
        }
        return expected;
    }

    private int nonRubyBoltMax(int scalarMaxHit) {
        BoltEffect effect = getNonRubyBoltType();
        switch (effect) {
            case OPAL:
                return finishAccurateDamage(applyAttackerDamageScaling(scalarMaxHit) + opalBoltBonus());
            case PEARL:
                return finishAccurateDamage(applyAttackerDamageScaling(scalarMaxHit) + pearlBoltBonus());
            case DRAGONSTONE:
                return finishAccurateDamage(applyAttackerDamageScaling(scalarMaxHit) + dragonstoneBoltBonus());
            case DIAMOND:
                return finishAccurateDamage(diamondBoltEffectMax());
            case ONYX:
                return finishAccurateDamage(onyxBoltEffectMax());
            default:
                return 0;
        }
    }

    private BoltEffect getNonRubyBoltType() {
        if (!isCrossbowAttack()) {
            return BoltEffect.NONE;
        }
        if (player.isWearingAny("Opal bolts (e)", "Opal dragon bolts (e)")) {
            return BoltEffect.OPAL;
        }
        if (player.isWearingAny("Pearl bolts (e)", "Pearl dragon bolts (e)")) {
            return BoltEffect.PEARL;
        }
        if (player.isWearingAny("Diamond bolts (e)", "Diamond dragon bolts (e)")) {
            return BoltEffect.DIAMOND;
        }
        if (player.isWearingAny("Dragonstone bolts (e)", "Dragonstone dragon bolts (e)")
            && !hasAttribute(MonsterAttribute.FIERY) && !hasAttribute(MonsterAttribute.DRAGON)) {
            return BoltEffect.DRAGONSTONE;
        }
        if (player.isWearingAny("Onyx bolts (e)", "Onyx dragon bolts (e)") && !hasAttribute(MonsterAttribute.UNDEAD)) {
            return BoltEffect.ONYX;
        }
        return BoltEffect.NONE;
    }

    private int opalBoltBonus() {
        return player.getBoostedRanged() / (isWearingZaryteCrossbow() ? 9 : 10);
    }

    private int pearlBoltBonus() {
        int divisor = hasAttribute(MonsterAttribute.FIERY) ? 15 : 20;
        return player.getBoostedRanged() / (isWearingZaryteCrossbow() ? divisor - 2 : divisor);
    }

    private int dragonstoneBoltBonus() {
        return player.getBoostedRanged() * 2 / (isWearingZaryteCrossbow() ? 9 : 10);
    }

    private int diamondBoltEffectMax() {
        return applyFactor(getScalarMaxHit(), isWearingZaryteCrossbow() ? 126 : 115, 100);
    }

    private int onyxBoltEffectMax() {
        return applyFactor(getScalarMaxHit(), isWearingZaryteCrossbow() ? 132 : 120, 100);
    }

    private double kandarinBoltFactor() {
        return player.isKandarinDiary() ? 1.1 : 1.0;
    }

    private boolean isRubyBoltEffectApplicable() {
        return isCrossbowAttack()
            && player.isWearingAny("Ruby bolts (e)", "Ruby dragon bolts (e)")
            && player.getCurrentHitpoints() >= 10;
    }

    private int rubyBoltDamage() {
        int currentHp = monster.getInputs().getMonsterCurrentHp() > 0 ? monster.getInputs().getMonsterCurrentHp() : monster.getHitpoints();
        int cap = contains(MonsterConstants.INFINITE_HEALTH_MONSTERS, monster.getId())
            ? (isWearingZaryteCrossbow() ? 66 : 60)
            : (isWearingZaryteCrossbow() ? 110 : 100);
        return Math.min(cap, applyFactor(currentHp, isWearingZaryteCrossbow() ? 22 : 20, 100));
    }

    private boolean isCrossbowAttack() {
        if (!player.getCombatStyle().getAttackType().isRanged()) {
            return false;
        }
        String category = player.getWeaponCategory();
        return "Crossbow".equals(category);
    }

    private boolean isWearingZaryteCrossbow() {
        return player.isWearing("Zaryte crossbow");
    }

    private boolean isZaryteCrossbowSpec() {
        return usingSpecialAttack && isWearingZaryteCrossbow();
    }

    private enum BoltEffect {
        NONE,
        OPAL,
        PEARL,
        DIAMOND,
        DRAGONSTONE,
        ONYX
    }

    public int getAttackSpeed() {
        int speed = player.getWeaponSpeed();
        if (speed <= 0) {
            speed = 4;
        }

        CombatStyle style = player.getCombatStyle();
        if ("Rapid".equals(style.getStance())) {
            speed = Math.max(1, speed - 1);
        }

        if (usingSpecialAttack && player.isWearing("Eye of ayak")) {
            return 5;
        }

        return speed;
    }

    public boolean isImmune() {
        int monsterId = monster.getId();
        AttackType attackType = player.getCombatStyle().getAttackType();

        if (usingSpecialAttack && player.isWearing("Voidwaker")) {
            attackType = AttackType.MAGIC;
        }

        if (MonsterConstants.IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS.contains(monsterId) && attackType.isMagic()) {
            return true;
        }
        if (MonsterConstants.IMMUNE_TO_RANGED_DAMAGE_NPC_IDS.contains(monsterId) && attackType.isRanged()) {
            return true;
        }
        if (MonsterConstants.IMMUNE_TO_MELEE_DAMAGE_NPC_IDS.contains(monsterId) && isUsingMeleeStyle()) {
            if (MonsterConstants.isZulrah(monsterId) && isWearingPolearm()) {
                return false;
            }
            return true;
        }
        if (hasAttribute(MonsterAttribute.FLYING) && isUsingMeleeStyle()) {
            if (MonsterConstants.isVespula(monsterId)) {
                return true;
            }
            if (isWearingPolearm() || isWearingSalamander()) {
                return false;
            }
            return true;
        }
        if (MonsterConstants.IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS.contains(monsterId)
            && isUsingMeleeStyle() && !isWearingSalamander()) {
            return true;
        }
        if (hasAttribute(MonsterAttribute.VAMPYRE_3) && !wearingVampyrebane(MonsterAttribute.VAMPYRE_3)) {
            return true;
        }
        if (hasAttribute(MonsterAttribute.VAMPYRE_2) && !wearingVampyrebane(MonsterAttribute.VAMPYRE_2)
            && !player.isWearing("Efaritay's aid") && !isWearingSilverWeapon()) {
            return true;
        }
        if (MonsterConstants.isGuardian(monsterId) && (!isUsingMeleeStyle() || !isWearingPickaxe())) {
            return true;
        }
        if (hasAttribute(MonsterAttribute.LEAFY) && !isWearingLeafBladedWeapon()) {
            return true;
        }
        if (MonsterConstants.isDoomOfMokhaiotl(monsterId) && "Shielded".equals(monster.getInputs().getPhase()) && !isUsingDemonbane()) {
            return true;
        }
        if (!hasAttribute(MonsterAttribute.RAT) && isWearingRatBoneWeapon()) {
            return true;
        }
        if (MonsterConstants.isEclipseMoon(monsterId) && "Clone".equals(monster.getVersion()) && !isUsingMeleeStyle()) {
            return true;
        }

        return false;
    }

    private int applyPrayerBonus(int level, boolean isAttack) {
        Set<Prayer> prayers = player.getActivePrayers();
        int effectiveLevel = level;

        for (Prayer prayer : prayers) {
            if (isAttack) {
                if (isUsingMeleeStyle() && prayer.isMeleePrayer()) {
                    effectiveLevel = applyPrayerFactor(effectiveLevel, prayer.getAttackBonus());
                } else if (player.getCombatStyle().getAttackType().isRanged() && prayer.isRangedPrayer()) {
                    effectiveLevel = applyPrayerFactor(effectiveLevel, prayer.getRangedBonus());
                } else if (player.getCombatStyle().getAttackType().isMagic() && prayer.isMagicPrayer()) {
                    effectiveLevel = applyPrayerFactor(effectiveLevel, prayer.getMagicBonus());
                }
            } else {
                if (isUsingMeleeStyle() && prayer.isMeleePrayer()) {
                    if (prayer == Prayer.BURST_OF_STRENGTH && effectiveLevel <= 20) {
                        effectiveLevel += 1;
                    } else {
                        effectiveLevel = applyPrayerFactor(effectiveLevel, prayer.getStrengthBonus());
                    }
                } else if (player.getCombatStyle().getAttackType().isRanged() && prayer.isRangedPrayer()) {
                    if (prayer == Prayer.SHARP_EYE && effectiveLevel <= 20) {
                        effectiveLevel += 1;
                    } else {
                        effectiveLevel = applyPrayerFactor(effectiveLevel, prayer == Prayer.RIGOUR ? 23 : prayer.getRangedBonus());
                    }
                }
            }
        }

        return effectiveLevel;
    }

    private int applyPrayerFactor(int level, int bonusPercent) {
        return applyFactor(level, 100 + bonusPercent, 100);
    }

    private int getAttackBonusForStyle() {
        EquipmentStats stats = player.getEquipmentStats();
        AttackType type = player.getCombatStyle().getAttackType();
        return stats.getAttackBonusForType(type);
    }

    private int countInquisitorPieces() {
        int count = 0;
        if (player.isWearing("Inquisitor's great helm")) count++;
        if (player.isWearing("Inquisitor's hauberk")) count++;
        if (player.isWearing("Inquisitor's plateskirt")) count++;
        return count;
    }

    private int countCrystalPieces() {
        int count = 0;
        if (player.isWearing("Crystal helm")) count++;
        if (player.isWearing("Crystal legs")) count += 2;
        if (player.isWearing("Crystal body")) count += 3;
        return count;
    }

    private boolean isRevWeaponApplicable() {
        if (!player.isInWilderness() || !"Charged".equals(player.getWeaponVersion())) {
            return false;
        }

        AttackType type = player.getCombatStyle().getAttackType();
        if (type.isMagic()) {
            return player.isWearingAny("Accursed sceptre", "Accursed sceptre (a)",
                "Thammaron's sceptre", "Thammaron's sceptre (a)");
        } else if (type.isRanged()) {
            return player.isWearingAny("Craw's bow", "Webweaver bow");
        } else {
            return player.isWearingAny("Ursine chainmace", "Viggora's chainmace");
        }
    }

    private int applyTwistedBowScaling(int value, boolean isAccuracy) {
        if (monster == null) return value;

        int monsterMagic = Math.max(getReducedMonsterStats().magicLevel, monster.getOffensiveMagic());
        int cap = hasAttribute(MonsterAttribute.XERICIAN) ? 350 : 250;
        monsterMagic = Math.min(cap, monsterMagic);

        int factor;
        int base;

        if (isAccuracy) {
            factor = 10;
            base = 140;
        } else {
            factor = 14;
            base = 250;
        }

        int t2 = (3 * monsterMagic - factor) / 100;
        int t3 = (int) Math.pow((3 * monsterMagic / 10) - (10 * factor), 2) / 100;

        int bonus = base + t2 - t3;
        return (value * bonus) / 100;
    }

    private boolean isAmmoInvalid() {
        return player.getAmmoApplicability() == AmmoApplicability.INVALID;
    }

    private boolean isManualCast() {
        return "Manual Cast".equals(player.getCombatStyle().getStance());
    }

    private boolean contains(int[] values, int needle) {
        for (int value : values) {
            if (value == needle) {
                return true;
            }
        }
        return false;
    }

    private int demonbaneFactor(int weaponDemonbane) {
        return (weaponDemonbane * getDemonbaneVulnerability()) / 100;
    }

    private int getDemonbaneVulnerability() {
        if (monster.getId() == -1 && monster.getInputs().getDemonbaneVulnerability() > 0) {
            return monster.getInputs().getDemonbaneVulnerability();
        }
        if (monster.getId() == 12191 || "Duke Sucellus".equals(monster.getName())) {
            return 70;
        }
        if (MonsterConstants.isYama(monster.getId())) {
            return 120;
        }
        if (MonsterConstants.isYamaVoidFlare(monster.getId())) {
            return 200;
        }
        return 100;
    }

    protected boolean isWearingAccursedSceptre() {
        return player.isWearingAny("Accursed sceptre", "Accursed sceptre (a)");
    }

    protected boolean isWearingBlowpipe() {
        return player.isWearingAny("Toxic blowpipe", "Blazing blowpipe");
    }

    protected boolean isWearingMsb() {
        return player.isWearingAny("Magic shortbow", "Magic shortbow (i)");
    }

    protected boolean isWearingMlb() {
        return player.isWearingAny("Magic longbow", "Magic comp bow");
    }

    protected boolean isWearingSmokeStaff() {
        return player.isWearingAny("Smoke battlestaff", "Mystic smoke staff", "Twinflame staff");
    }

    protected boolean isWearingRatBoneWeapon() {
        return player.isWearingAny("Bone mace", "Bone shortbow", "Bone staff");
    }

    protected boolean isWearingPolearm() {
        String weapon = player.getWeaponName();
        return weapon != null && (weapon.contains("halberd") || weapon.contains("spear"));
    }

    protected boolean isWearingCorpbaneWeapon() {
        String weapon = player.getWeaponName();
        if (weapon == null) {
            return false;
        }

        boolean isStab = player.getCombatStyle().getAttackType() == AttackType.STAB;
        if (isWearingFang()) {
            return isStab;
        }
        if (weapon.endsWith("halberd")) {
            return isStab;
        }
        if (weapon.contains("spear") && !"Blue moon spear".equals(weapon)) {
            return isStab;
        }
        return player.getCombatStyle().getAttackType().isMagic();
    }

    protected boolean isWearingSalamander() {
        String weapon = player.getWeaponName();
        return weapon != null && weapon.toLowerCase().contains("salamander");
    }

    protected boolean isWearingPickaxe() {
        String weapon = player.getWeaponName();
        return weapon != null && weapon.toLowerCase().contains("pickaxe");
    }

    protected boolean isWearingLeafBladedWeapon() {
        if (isUsingMeleeStyle() && player.isWearingAny("Leaf-bladed battleaxe", "Leaf-bladed spear", "Leaf-bladed sword")) {
            return true;
        }
        if (player.isWearingAny("Broad arrows", "Broad bolts", "Amethyst broad bolts")
            && player.getCombatStyle().getAttackType().isRanged()) {
            return true;
        }
        return false;
    }

    protected boolean isWearingSilverWeapon() {
        if (player.isWearingAny("Silver bolts") && player.getCombatStyle().getAttackType().isRanged()) {
            return true;
        }
        return isUsingMeleeStyle() && player.isWearingAny(
            "Blessed axe", "Ivandis flail", "Blisterwood flail", "Silver sickle", "Silver sickle (b)",
            "Emerald sickle", "Emerald sickle (b)", "Enchanted emerald sickle (b)", "Ruby sickle (b)",
            "Enchanted ruby sickle (b)", "Blisterwood sickle", "Silverlight", "Darklight", "Arclight",
            "Rod of ivandis", "Wolfbane"
        );
    }

    protected boolean wearingVampyrebane(MonsterAttribute tier) {
        boolean t2 = tier == MonsterAttribute.VAMPYRE_2;
        return (t2 || isUsingMeleeStyle()) && player.isWearingAny(
            t2 ? "Rod of ivandis" : "",
            "Ivandis flail", "Blisterwood sickle", "Blisterwood flail", "Hallowed flail", "Sunspear"
        );
    }

    private boolean isSunspearSpec() {
        return usingSpecialAttack && player.isWearing("Sunspear");
    }

    private boolean isSunspearFinisher() {
        if (!isSunspearSpec()) {
            return false;
        }

        int currentHp = monster.getInputs().getMonsterCurrentHp() > 0
            ? monster.getInputs().getMonsterCurrentHp()
            : monster.getHitpoints();
        int finisherDamage = applyFactor(getMaxHit(), 7, 10);
        return currentHp <= finisherDamage;
    }

    protected boolean isVampyre() {
        return hasAttribute(MonsterAttribute.VAMPYRE_1)
            || hasAttribute(MonsterAttribute.VAMPYRE_2)
            || hasAttribute(MonsterAttribute.VAMPYRE_3);
    }

    protected boolean isCorporealBeast() {
        return monster != null && "Corporeal Beast".equals(monster.getName());
    }

    protected boolean isUsingDemonbane() {
        AttackType type = player.getCombatStyle().getAttackType();
        if (type.isRanged()) {
            return player.isWearing("Scorching bow");
        }
        return player.isWearingAny("Silverlight", "Darklight", "Arclight", "Emberlight", "Bone claws", "Burning claws");
    }

    protected boolean isUsingAbyssal() {
        return isUsingMeleeStyle() && player.isWearingAny("Abyssal bludgeon", "Abyssal dagger", "Abyssal whip", "Abyssal tentacle");
    }

    public boolean isUsingSpecialAttack() {
        return usingSpecialAttack;
    }

    public void setUsingSpecialAttack(boolean usingSpecialAttack) {
        this.usingSpecialAttack = usingSpecialAttack;
    }
}
