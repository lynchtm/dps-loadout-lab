package com.dpscalc.fixture;

import com.dpscalc.calc.distribution.HitDistribution;
import com.dpscalc.calc.distribution.Hitsplat;
import com.dpscalc.calc.distribution.WeightedHit;
import com.dpscalc.parity.ParityOutput;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class FixtureOutputParser {
    private FixtureOutputParser() {}

    static ParityOutput parse(JsonObject outputs) {
        ParityOutput.Builder result = ParityOutput.builder();
        JsonObject equipment = outputs.getAsJsonObject("equipment");
        addEquipment(result, equipment.getAsJsonObject("bonuses"), "bonuses",
            "str", "magic_str", "ranged_str", "prayer");
        addEquipment(result, equipment.getAsJsonObject("offensive"), "offensive",
            "stab", "slash", "crush", "magic", "ranged");
        addEquipment(result, equipment.getAsJsonObject("defensive"), "defensive",
            "stab", "slash", "crush", "magic", "ranged");
        result.putInteger("outputs.equipment.attackSpeed", equipment.get("attackSpeed").getAsInt());

        addIntegers(result, outputs, "maxHit", "maxAttackRoll", "npcDefRoll", "scalarMax",
            "directMax", "dotMax", "totalMax", "distributionMax", "baseAttackSpeed");
        addFloats(result, outputs, "accuracy", "expectedDirectDamage", "expectedDotDamage",
            "expectedDamage", "expectedAttackSpeed", "dpt", "dps");
        return result.distribution(distribution(outputs.getAsJsonArray("normalizedDistribution"))).build();
    }

    private static void addEquipment(ParityOutput.Builder result, JsonObject values,
                                     String group, String... fields) {
        for (String field : fields) {
            result.putInteger("outputs.equipment." + group + "." + field, values.get(field).getAsInt());
        }
    }

    private static void addIntegers(ParityOutput.Builder result, JsonObject outputs, String... fields) {
        for (String field : fields) result.putInteger("outputs." + field, outputs.get(field).getAsInt());
    }

    private static void addFloats(ParityOutput.Builder result, JsonObject outputs, String... fields) {
        for (String field : fields) result.putFloat("outputs." + field, outputs.get(field).getAsDouble());
    }

    private static HitDistribution distribution(JsonArray values) {
        List<WeightedHit> outcomes = new ArrayList<>(values.size());
        for (JsonElement value : values) {
            JsonObject outcome = value.getAsJsonObject();
            JsonArray hitsplats = outcome.getAsJsonArray("hitsplats");
            List<Hitsplat> splats = new ArrayList<>(hitsplats.size());
            for (JsonElement hitsplatValue : hitsplats) {
                JsonObject hitsplat = hitsplatValue.getAsJsonObject();
                splats.add(new Hitsplat(hitsplat.get("damage").getAsInt(), hitsplat.get("accurate").getAsBoolean()));
            }
            outcomes.add(new WeightedHit(outcome.get("probability").getAsDouble(), splats));
        }
        return new HitDistribution(outcomes);
    }
}
