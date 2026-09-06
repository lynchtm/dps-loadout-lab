package com.dpscalc.data;

public final class MonsterScaling {
    private static final int WARDEN_CORE_BASE_HP = 4500;
    private static final int CM_SCALE_PERCENT = 50;

    private MonsterScaling() {}

    public static MonsterStats scale(MonsterStats source) {
        MonsterStats scaled = source.copy();
        applyToaHpScaling(scaled);
        applyCoxScaling(scaled);
        return scaled;
    }

    private static void applyCoxScaling(MonsterStats monster) {
        if (!monster.hasAttribute(MonsterAttribute.XERICIAN)) {
            return;
        }

        if (MonsterConstants.COX_USE_SINGLES_SCALING_IDS.contains(monster.getId())) {
            applySinglesCoxScaling(monster);
            return;
        }

        if (MonsterConstants.contains(MonsterConstants.OLM_IDS, monster.getId())) {
            applyOlmScaling(monster);
            return;
        }

        applyMultiCoxScaling(monster);
    }

    private static void applySinglesCoxScaling(MonsterStats monster) {
        MonsterInputs inputs = monster.getInputs();
        SkillMeta meta = skillMeta(monster);
        int hpScaler = clamp(inputs.getPartyMaxCombatLevel(), 60, 126);
        int statScaler = clamp(inputs.getPartyMaxHpLevel(), 55, 99);

        if (inputs.isFromCoxCm()) {
            statScaler = addPercent(statScaler, CM_SCALE_PERCENT);
            hpScaler = addPercent(hpScaler, CM_SCALE_PERCENT);
        }

        int hp = Math.max(meta.baseHp * hpScaler / 126, 5);
        int offensive = Math.max(meta.baseOffensive * statScaler / 99, 1);
        int defensive = Math.max(meta.baseDefensive * statScaler / 99, 1);
        applySkillChanges(monster, meta, hp, offensive, defensive);
    }

    private static void applyMultiCoxScaling(MonsterStats monster) {
        MonsterInputs inputs = monster.getInputs();
        SkillMeta meta = skillMeta(monster);
        int partySize = clamp(inputs.getPartySize(), 1, 100);
        int partySizeM1 = partySize - 1;
        int highestComLevel = clamp(inputs.getPartyMaxCombatLevel(), 60, 126);
        int highestHp = clamp(55 + 44 * inputs.getPartyMaxHpLevel() / 99, 55, 99);

        int offensive = meta.baseOffensive * highestHp / 99;
        int defensive = meta.baseDefensive * highestHp / 99;
        int hp = meta.baseHp * highestComLevel / 126;

        int offensiveScalePct = 100 + (int) Math.sqrt(partySizeM1) * 7 + partySizeM1;
        offensive = offensive * offensiveScalePct / 100;

        int defensiveScalePct = 100 + (int) Math.sqrt(partySizeM1) + partySizeM1 * 7 / 10;
        defensive = defensive * defensiveScalePct / 100;

        hp += hp * (partySize * 50 / 100);

        if (inputs.isFromCoxCm()) {
            offensive = addPercent(offensive, CM_SCALE_PERCENT);
            if (!MonsterConstants.isGlowingCrystal(monster.getId())) {
                hp = addPercent(hp, CM_SCALE_PERCENT);
            }
            if (MonsterConstants.isTekton(monster.getId())) {
                defensive = addPercent(defensive, partySize < 4 ? 20 : 35);
            } else if (!MonsterConstants.isGlowingCrystal(monster.getId())) {
                defensive = addPercent(defensive, CM_SCALE_PERCENT);
            }
        }

        applySkillChanges(monster, meta, clamp(hp, 50, 30000), clamp(offensive, 50, 5000), clamp(defensive, 50, 20000));
    }

    private static void applyOlmScaling(MonsterStats monster) {
        boolean meleeHand = MonsterConstants.isOlmMeleeHand(monster.getId());
        boolean mageHand = MonsterConstants.isOlmMageHand(monster.getId());
        int partySize = monster.getInputs().getPartySize();
        int partySizeScaleFactor = Math.min(partySize - 1, 50) - 3 * (Math.min(partySize, 50) / 8);

        applyMultiCoxScaling(monster);
        if (mageHand) {
            monster.setMagicLevel(monster.getMagicLevel() / 2);
        }
        int hp = meleeHand || mageHand
            ? 600 + 300 * partySizeScaleFactor
            : 800 + 400 * partySizeScaleFactor;
        monster.setHitpoints(hp);
    }

    private static SkillMeta skillMeta(MonsterStats monster) {
        boolean magicIsDefensive = MonsterConstants.COX_MAGIC_IS_DEFENSIVE_IDS.contains(monster.getId());
        boolean scaleAttack = monster.getAttackLevel() != 1;
        boolean scaleStrength = monster.getStrengthLevel() != 1;
        boolean scaleRanged = monster.getRangedLevel() != 1;
        boolean scaleMagicOffensive = !magicIsDefensive && monster.getMagicLevel() != 1;
        boolean scaleDefence = monster.getDefenceLevel() != 1;
        boolean scaleMagicDefensive = magicIsDefensive && monster.getMagicLevel() != 1;

        int baseOffensive = 1;
        if (scaleAttack) baseOffensive = Math.max(baseOffensive, monster.getAttackLevel());
        if (scaleStrength) baseOffensive = Math.max(baseOffensive, monster.getStrengthLevel());
        if (scaleRanged) baseOffensive = Math.max(baseOffensive, monster.getRangedLevel());
        if (scaleMagicOffensive) baseOffensive = Math.max(baseOffensive, monster.getMagicLevel());

        int baseDefensive = 1;
        if (scaleDefence) baseDefensive = Math.max(baseDefensive, monster.getDefenceLevel());
        if (scaleMagicDefensive) baseDefensive = Math.max(baseDefensive, monster.getMagicLevel());

        int baseHp = MonsterConstants.isGuardian(monster.getId())
            ? 151 + monster.getInputs().getPartySumMiningLevel() / monster.getInputs().getPartySize()
            : monster.getHitpoints();
        SkillMeta meta = new SkillMeta();
        meta.scaleAttack = scaleAttack;
        meta.scaleStrength = scaleStrength;
        meta.scaleRanged = scaleRanged;
        meta.scaleMagicOffensive = scaleMagicOffensive;
        meta.scaleDefence = scaleDefence;
        meta.scaleMagicDefensive = scaleMagicDefensive;
        meta.baseOffensive = baseOffensive;
        meta.baseDefensive = baseDefensive;
        meta.baseHp = baseHp;
        return meta;
    }

    private static void applySkillChanges(MonsterStats monster, SkillMeta meta, int hp, int offensive, int defensive) {
        monster.setHitpoints(hp);
        if (meta.scaleAttack) monster.setAttackLevel(offensive);
        if (meta.scaleStrength) monster.setStrengthLevel(offensive);
        if (meta.scaleRanged) monster.setRangedLevel(offensive);
        if (meta.scaleMagicOffensive) monster.setMagicLevel(offensive);
        if (meta.scaleDefence) monster.setDefenceLevel(defensive);
        if (meta.scaleMagicDefensive) monster.setMagicLevel(defensive);
    }

    private static void applyToaHpScaling(MonsterStats monster) {
        if (!monster.isToaMonster()) {
            return;
        }

        MonsterInputs inputs = monster.getInputs();
        boolean wardenCore = MonsterConstants.contains(MonsterConstants.TOA_WARDEN_CORE_EJECTED_IDS, monster.getId());
        int hitpoints = wardenCore ? WARDEN_CORE_BASE_HP : monster.getHitpoints();

        int invocationFactor = ((wardenCore ? 1 : 4) * inputs.getToaInvocationLevel()) / 10;
        hitpoints += hitpoints * invocationFactor / 100;

        int clampedPathLevel = clamp(inputs.getToaPathLevel(), 0, 6);
        if (monster.isToaPathMonster() && clampedPathLevel >= 1) {
            int pathLevelFactor = 3 + 5 * inputs.getToaPathLevel();
            hitpoints = hitpoints * (100 + pathLevelFactor) / 100;
        }

        int partySize = clamp(inputs.getPartySize(), 1, 8);
        if (partySize >= 2) {
            int partyFactor = 9 * (partySize >= 3 ? 2 : 1);
            if (partySize >= 4) {
                partyFactor += 6 * (partySize - 3);
            }
            hitpoints = hitpoints * (10 + partyFactor) / 10;
        }

        monster.setHitpoints(roundToToaStep(hitpoints));
    }

    private static int roundToToaStep(int hitpoints) {
        if (hitpoints <= 100) {
            return hitpoints;
        }
        int roundTo = hitpoints > 300 ? 10 : 5;
        return ((hitpoints + roundTo / 2) / roundTo) * roundTo;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int addPercent(int value, int percent) {
        return value + value * percent / 100;
    }

    private static final class SkillMeta {
        private boolean scaleAttack;
        private boolean scaleStrength;
        private boolean scaleRanged;
        private boolean scaleMagicOffensive;
        private boolean scaleDefence;
        private boolean scaleMagicDefensive;
        private int baseOffensive;
        private int baseDefensive;
        private int baseHp;
    }
}
