package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterScaling;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.fixture.FixtureDocument.FixtureCase;
import com.dpscalc.parity.ParityOutput;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.google.gson.JsonObject;

final class ParityEvaluator {
    ParityOutput evaluate(FixtureCase fixture) {
        JsonObject inputs = fixture.getFixtureJson().getAsJsonObject("inputs");
        MonsterStats rawMonster = FixtureStateFactory.buildMonster(inputs.getAsJsonObject("monster"));
        PlayerState player = FixtureStateFactory.buildPlayer(inputs.getAsJsonObject("player"), rawMonster.getId());
        MonsterStats monster = MonsterScaling.scale(rawMonster);
        DpsResult result = new DpsCalculator(player, monster, fixture.getInputs().isUsingSpecialAttack()).calculate();
        return snapshot(player.getEquipmentStats(), result);
    }

    private static ParityOutput snapshot(EquipmentStats equipment, DpsResult result) {
        ParityOutput.Builder output = ParityOutput.builder();
        addEquipment(output, equipment, result.getBaseAttackSpeed());
        output.putInteger("outputs.maxHit", result.getMaxHit())
            .putInteger("outputs.maxAttackRoll", result.getAttackRoll())
            .putInteger("outputs.npcDefRoll", result.getDefenceRoll())
            .putInteger("outputs.scalarMax", result.getScalarMaxHit())
            .putInteger("outputs.directMax", result.getDirectMaxHit())
            .putInteger("outputs.dotMax", result.getDotMaxHit())
            .putInteger("outputs.totalMax", result.getTotalMaxHit())
            .putInteger("outputs.distributionMax", result.getDistributionMaxHit())
            .putInteger("outputs.baseAttackSpeed", result.getBaseAttackSpeed())
            .putFloat("outputs.accuracy", result.getAccuracy())
            .putFloat("outputs.expectedDirectDamage", result.getExpectedDirectDamage())
            .putFloat("outputs.expectedDotDamage", result.getExpectedDotDamage())
            .putFloat("outputs.expectedDamage", result.getExpectedDamage())
            .putFloat("outputs.expectedAttackSpeed", result.getExpectedAttackSpeed())
            .putFloat("outputs.dpt", result.getDistributionDamagePerTick())
            .putFloat("outputs.dps", result.getDps())
            .distribution(result.getAttackDistribution().getJointDistribution());
        return output.build();
    }

    private static void addEquipment(ParityOutput.Builder output, EquipmentStats equipment, int attackSpeed) {
        output.putInteger("outputs.equipment.bonuses.str", equipment.getMeleeStrength())
            .putInteger("outputs.equipment.bonuses.magic_str", equipment.getMagicDamage())
            .putInteger("outputs.equipment.bonuses.ranged_str", equipment.getRangedStrength())
            .putInteger("outputs.equipment.bonuses.prayer", equipment.getPrayerBonus())
            .putInteger("outputs.equipment.offensive.stab", equipment.getStabAttack())
            .putInteger("outputs.equipment.offensive.slash", equipment.getSlashAttack())
            .putInteger("outputs.equipment.offensive.crush", equipment.getCrushAttack())
            .putInteger("outputs.equipment.offensive.magic", equipment.getMagicAttack())
            .putInteger("outputs.equipment.offensive.ranged", equipment.getRangedAttack())
            .putInteger("outputs.equipment.defensive.stab", equipment.getStabDefence())
            .putInteger("outputs.equipment.defensive.slash", equipment.getSlashDefence())
            .putInteger("outputs.equipment.defensive.crush", equipment.getCrushDefence())
            .putInteger("outputs.equipment.defensive.magic", equipment.getMagicDefence())
            .putInteger("outputs.equipment.defensive.ranged", equipment.getRangedDefence())
            .putInteger("outputs.equipment.attackSpeed", attackSpeed);
    }
}
