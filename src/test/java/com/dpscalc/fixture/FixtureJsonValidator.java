package com.dpscalc.fixture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class FixtureJsonValidator {
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "webCalcCommit", "equipmentDomainDigest", "totalScenarios", "successfulScenarios", "failedScenarios", "fixtures");
    private static final Set<String> FIXTURE_FIELDS = Set.of("id", "name", "category", "source", "inputs", "outputs");
    private static final Set<String> INPUT_FIELDS = Set.of("player", "monster", "options");
    private static final Set<String> PLAYER_FIELDS = Set.of("skills", "boosts", "prayers", "buffs", "style", "spell", "equipment");
    private static final Set<String> SKILL_FIELDS = Set.of("atk", "str", "def", "ranged", "magic", "hp");
    private static final Set<String> PLAYER_SKILL_FIELDS = Set.of("atk", "str", "def", "ranged", "magic", "hp", "prayer");
    private static final Set<String> EQUIPMENT_FIELDS = Set.of("head", "cape", "neck", "weapon", "body", "shield", "legs", "hands", "feet", "ring", "ammo");
    private static final Set<String> MONSTER_FIELDS = Set.of("id", "version", "name", "size", "speed", "skills", "offensive", "defensive", "attributes", "weakness", "inputs");
    private static final Set<String> MONSTER_INPUT_FIELDS = Set.of("isFromCoxCm", "toaInvocationLevel", "toaPathLevel", "partyMaxCombatLevel", "partySumMiningLevel", "partyMaxHpLevel", "partySize", "monsterCurrentHp", "defenceReductions");
    private static final Set<String> REDUCTION_FIELDS = Set.of("vulnerability", "accursed", "elderMaul", "dwh", "arclight", "emberlight", "bgs", "tonalztic", "seercull", "ayak");
    private static final Set<String> OUTPUT_FIELDS = Set.of("equipment", "maxHit", "maxAttackRoll", "npcDefRoll", "accuracy", "scalarMax", "directMax", "dotMax", "totalMax", "distributionMax", "expectedDirectDamage", "expectedDotDamage", "expectedDamage", "baseAttackSpeed", "expectedAttackSpeed", "normalizedDistribution", "dpt", "dps");
    private static final Set<String> INTEGER_OUTPUT_FIELDS = Set.of("maxHit", "maxAttackRoll", "npcDefRoll", "scalarMax", "directMax", "dotMax", "totalMax", "distributionMax", "baseAttackSpeed");
    private static final Set<String> FLOAT_OUTPUT_FIELDS = Set.of("accuracy", "expectedDirectDamage", "expectedDotDamage", "expectedDamage", "expectedAttackSpeed", "dpt", "dps");

    private FixtureJsonValidator() {}

    static void validate(JsonObject root, String expectedSha, String expectedDigest) {
        strict(root, "$", ROOT_FIELDS);
        if (integer(root, "schemaVersion", "$.schemaVersion") != 3) fail("$.schemaVersion", "expected 3");
        if (!string(root, "webCalcCommit", "$.webCalcCommit").equals(expectedSha)) fail("$.webCalcCommit", "stale reference SHA");
        if (!string(root, "equipmentDomainDigest", "$.equipmentDomainDigest").equals(expectedDigest)) fail("$.equipmentDomainDigest", "stale rule digest");
        int total = nonNegative(root, "totalScenarios", "$.totalScenarios");
        int successful = nonNegative(root, "successfulScenarios", "$.successfulScenarios");
        int failed = nonNegative(root, "failedScenarios", "$.failedScenarios");
        JsonArray fixtures = array(root, "fixtures", "$.fixtures");
        if (fixtures.size() == 0) fail("$.fixtures", "must not be empty");
        if (total != fixtures.size() || successful != fixtures.size() || failed != 0) fail("$.totalScenarios", "declared counts do not match fixtures");

        Set<String> ids = new HashSet<>();
        for (int index = 0; index < fixtures.size(); index++) {
            String path = "$.fixtures[" + index + "]";
            JsonObject fixture = object(fixtures.get(index), path);
            validateFixture(fixture, path);
            String id = string(fixture, "id", path + ".id");
            if (!ids.add(id)) fail(path + ".id", "duplicate " + id);
        }
    }

    private static void validateFixture(JsonObject fixture, String path) {
        strict(fixture, path, FIXTURE_FIELDS);
        nonEmpty(fixture, "id", path + ".id");
        nonEmpty(fixture, "name", path + ".name");
        nonEmpty(fixture, "category", path + ".category");
        String source = string(fixture, "source", path + ".source");
        if (!source.equals("scenario") && !source.equals("deterministic-random")) fail(path + ".source", "unsupported value");

        JsonObject inputs = child(fixture, "inputs", path + ".inputs");
        strict(inputs, path + ".inputs", INPUT_FIELDS);
        validatePlayer(child(inputs, "player", path + ".inputs.player"), path + ".inputs.player");
        validateMonster(child(inputs, "monster", path + ".inputs.monster"), path + ".inputs.monster");
        JsonObject options = child(inputs, "options", path + ".inputs.options");
        strict(options, path + ".inputs.options", Set.of("usingSpecialAttack"));
        bool(options, "usingSpecialAttack", path + ".inputs.options.usingSpecialAttack");

        JsonObject outputs = child(fixture, "outputs", path + ".outputs");
        strict(outputs, path + ".outputs", OUTPUT_FIELDS);
        for (String field : INTEGER_OUTPUT_FIELDS) nonNegative(outputs, field, path + ".outputs." + field);
        for (String field : FLOAT_OUTPUT_FIELDS) finite(outputs, field, path + ".outputs." + field);
        validateEquipmentOutput(child(outputs, "equipment", path + ".outputs.equipment"), path + ".outputs.equipment");
        validateDistribution(array(outputs, "normalizedDistribution", path + ".outputs.normalizedDistribution"), path + ".outputs.normalizedDistribution");
    }

    private static void validateDistribution(JsonArray distribution, String path) {
        if (distribution.size() == 0) fail(path, "must not be empty");
        double totalProbability = 0;
        for (int index = 0; index < distribution.size(); index++) {
            String outcomePath = path + "[" + index + "]";
            JsonObject outcome = object(distribution.get(index), outcomePath);
            strict(outcome, outcomePath, Set.of("probability", "hitsplats"));
            double probability = finite(outcome, "probability", outcomePath + ".probability");
            if (probability < 0) fail(outcomePath + ".probability", "must be non-negative");
            totalProbability += probability;
            JsonArray hitsplats = array(outcome, "hitsplats", outcomePath + ".hitsplats");
            if (hitsplats.size() == 0) fail(outcomePath + ".hitsplats", "must not be empty");
            for (int splatIndex = 0; splatIndex < hitsplats.size(); splatIndex++) {
                String splatPath = outcomePath + ".hitsplats[" + splatIndex + "]";
                JsonObject hitsplat = object(hitsplats.get(splatIndex), splatPath);
                strict(hitsplat, splatPath, Set.of("damage", "accurate"));
                nonNegative(hitsplat, "damage", splatPath + ".damage");
                bool(hitsplat, "accurate", splatPath + ".accurate");
            }
        }
        if (!Double.isFinite(totalProbability) || totalProbability <= 0) fail(path, "total probability must be positive and finite");
    }

    private static void validateEquipmentOutput(JsonObject equipment, String path) {
        strict(equipment, path, Set.of("bonuses", "offensive", "defensive", "attackSpeed"));
        validateIntegers(child(equipment, "bonuses", path + ".bonuses"), path + ".bonuses", Set.of("str", "magic_str", "ranged_str", "prayer"));
        validateIntegers(child(equipment, "offensive", path + ".offensive"), path + ".offensive", Set.of("slash", "stab", "crush", "ranged", "magic"));
        validateIntegers(child(equipment, "defensive", path + ".defensive"), path + ".defensive", Set.of("slash", "stab", "crush", "ranged", "magic"));
        if (integer(equipment, "attackSpeed", path + ".attackSpeed") <= 0) fail(path + ".attackSpeed", "must be positive");
    }

    private static void validatePlayer(JsonObject player, String path) {
        strict(player, path, PLAYER_FIELDS);
        validateIntegers(child(player, "skills", path + ".skills"), path + ".skills", PLAYER_SKILL_FIELDS);
        validateIntegers(child(player, "boosts", path + ".boosts"), path + ".boosts", PLAYER_SKILL_FIELDS);
        JsonArray prayers = array(player, "prayers", path + ".prayers");
        for (int index = 0; index < prayers.size(); index++) primitiveString(prayers.get(index), path + ".prayers[" + index + "]");
        validatePrimitiveMap(child(player, "buffs", path + ".buffs"), path + ".buffs");
        JsonObject style = child(player, "style", path + ".style");
        strict(style, path + ".style", Set.of("type", "stance"));
        nonEmpty(style, "type", path + ".style.type");
        nonEmpty(style, "stance", path + ".style.stance");
        JsonElement spell = required(player, "spell", path + ".spell");
        if (!spell.isJsonNull()) validateSpell(object(spell, path + ".spell"), path + ".spell");

        JsonObject equipment = child(player, "equipment", path + ".equipment");
        strict(equipment, path + ".equipment", EQUIPMENT_FIELDS);
        for (String slot : EQUIPMENT_FIELDS) {
            JsonElement item = required(equipment, slot, path + ".equipment." + slot);
            if (item.isJsonNull()) continue;
            JsonObject itemObject = object(item, path + ".equipment." + slot);
            strict(itemObject, path + ".equipment." + slot, Set.of("id", "itemVars"));
            integer(itemObject, "id", path + ".equipment." + slot + ".id");
            validatePrimitiveMap(child(itemObject, "itemVars", path + ".equipment." + slot + ".itemVars"), path + ".equipment." + slot + ".itemVars");
        }
    }

    private static void validateSpell(JsonObject spell, String path) {
        strict(spell, path, Set.of("name", "spellbook", "element", "max_hit"));
        nonEmpty(spell, "name", path + ".name");
        String spellbook = string(spell, "spellbook", path + ".spellbook");
        if (!Set.of("standard", "ancient", "lunar", "arceuus").contains(spellbook)) fail(path + ".spellbook", "unsupported value");
        JsonElement element = required(spell, "element", path + ".element");
        if (!element.isJsonNull() && !Set.of("air", "water", "earth", "fire").contains(primitiveString(element, path + ".element"))) {
            fail(path + ".element", "unsupported value");
        }
        nonNegative(spell, "max_hit", path + ".max_hit");
    }

    private static void validateMonster(JsonObject monster, String path) {
        strict(monster, path, MONSTER_FIELDS);
        integer(monster, "id", path + ".id");
        string(monster, "version", path + ".version");
        nonEmpty(monster, "name", path + ".name");
        integer(monster, "size", path + ".size");
        integer(monster, "speed", path + ".speed");
        validateIntegers(child(monster, "skills", path + ".skills"), path + ".skills", SKILL_FIELDS);
        validateFiniteMap(child(monster, "offensive", path + ".offensive"), path + ".offensive");
        validateFiniteMap(child(monster, "defensive", path + ".defensive"), path + ".defensive");
        JsonArray attributes = array(monster, "attributes", path + ".attributes");
        for (int index = 0; index < attributes.size(); index++) primitiveString(attributes.get(index), path + ".attributes[" + index + "]");
        JsonElement weakness = required(monster, "weakness", path + ".weakness");
        if (!weakness.isJsonNull()) {
            JsonObject object = object(weakness, path + ".weakness");
            strict(object, path + ".weakness", Set.of("element", "severity"));
            string(object, "element", path + ".weakness.element");
            integer(object, "severity", path + ".weakness.severity");
        }
        JsonObject inputs = child(monster, "inputs", path + ".inputs");
        strict(inputs, path + ".inputs", MONSTER_INPUT_FIELDS);
        bool(inputs, "isFromCoxCm", path + ".inputs.isFromCoxCm");
        for (String field : Set.of("toaInvocationLevel", "toaPathLevel", "partyMaxCombatLevel", "partySumMiningLevel", "partyMaxHpLevel", "partySize", "monsterCurrentHp")) {
            integer(inputs, field, path + ".inputs." + field);
        }
        JsonObject reductions = child(inputs, "defenceReductions", path + ".inputs.defenceReductions");
        strict(reductions, path + ".inputs.defenceReductions", REDUCTION_FIELDS);
        bool(reductions, "vulnerability", path + ".inputs.defenceReductions.vulnerability");
        bool(reductions, "accursed", path + ".inputs.defenceReductions.accursed");
        for (String field : Set.of("elderMaul", "dwh", "arclight", "emberlight", "bgs", "tonalztic", "seercull", "ayak")) {
            integer(reductions, field, path + ".inputs.defenceReductions." + field);
        }
    }

    private static void validateIntegers(JsonObject object, String path, Set<String> fields) {
        strict(object, path, fields);
        for (String field : fields) integer(object, field, path + "." + field);
    }

    private static void validateFiniteMap(JsonObject object, String path) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) finiteElement(entry.getValue(), path + "." + entry.getKey());
    }

    private static void validatePrimitiveMap(JsonObject object, String path) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement value = entry.getValue();
            if (!value.isJsonPrimitive()) fail(path + "." + entry.getKey(), "expected primitive");
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isNumber()) finiteElement(value, path + "." + entry.getKey());
        }
    }

    private static void strict(JsonObject object, String path, Set<String> fields) {
        for (String field : fields) required(object, field, path + "." + field);
        for (String field : object.keySet()) if (!fields.contains(field)) fail(path + "." + field, "unknown field");
    }

    private static JsonElement required(JsonObject object, String field, String path) {
        JsonElement value = object.get(field);
        if (value == null) fail(path, "missing field");
        return value;
    }

    private static JsonObject child(JsonObject object, String field, String path) { return object(required(object, field, path), path); }
    private static JsonObject object(JsonElement value, String path) { if (!value.isJsonObject()) fail(path, "expected object"); return value.getAsJsonObject(); }
    private static JsonArray array(JsonObject object, String field, String path) { JsonElement value = required(object, field, path); if (!value.isJsonArray()) fail(path, "expected array"); return value.getAsJsonArray(); }
    private static String string(JsonObject object, String field, String path) { return primitiveString(required(object, field, path), path); }
    private static String primitiveString(JsonElement value, String path) { if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) fail(path, "expected string"); return value.getAsString(); }
    private static void nonEmpty(JsonObject object, String field, String path) { if (string(object, field, path).isEmpty()) fail(path, "must not be empty"); }
    private static boolean bool(JsonObject object, String field, String path) { JsonElement value = required(object, field, path); if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) fail(path, "expected boolean"); return value.getAsBoolean(); }
    private static int nonNegative(JsonObject object, String field, String path) { int value = integer(object, field, path); if (value < 0) fail(path, "must be non-negative"); return value; }
    private static int integer(JsonObject object, String field, String path) { JsonElement value = required(object, field, path); if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) fail(path, "expected integer"); double number = value.getAsDouble(); if (!Double.isFinite(number) || number != Math.rint(number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) fail(path, "expected finite 32-bit integer"); return value.getAsInt(); }
    private static double finite(JsonObject object, String field, String path) { return finiteElement(required(object, field, path), path); }
    private static double finiteElement(JsonElement value, String path) { if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) fail(path, "expected number"); double number = value.getAsDouble(); if (!Double.isFinite(number)) fail(path, "expected finite number"); return number; }
    private static void fail(String path, String detail) { throw new FixtureDocumentException(path, detail); }
}
