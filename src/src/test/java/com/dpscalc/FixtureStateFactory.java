package com.dpscalc;

import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.data.WeaknessElement;
import com.dpscalc.state.AttackType;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.Prayer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

final class FixtureStateFactory {
    private FixtureStateFactory() {}

    static PlayerState buildPlayer(JsonObject playerJson) {
        return buildPlayer(playerJson, -1);
    }

    static PlayerState buildPlayer(JsonObject playerJson, int monsterId) {
        PlayerState state = new PlayerState();
        JsonObject skills = playerJson.getAsJsonObject("skills");
        state.setAttackLevel(getInt(skills, "atk"));
        state.setStrengthLevel(getInt(skills, "str"));
        state.setDefenceLevel(getInt(skills, "def"));
        state.setRangedLevel(getInt(skills, "ranged"));
        state.setMagicLevel(getInt(skills, "magic"));
        state.setHitpointsLevel(getInt(skills, "hp"));
        state.setPrayerLevel(99);
        state.setCurrentHitpoints(getInt(skills, "hp"));

        JsonObject buffs = playerJson.getAsJsonObject("buffs");
        if (has(buffs, "attackBoost")) state.setAttackBoost(getInt(buffs, "attackBoost"));
        if (has(buffs, "strengthBoost")) state.setStrengthBoost(getInt(buffs, "strengthBoost"));
        if (has(buffs, "defenceBoost")) state.setDefenceBoost(getInt(buffs, "defenceBoost"));
        if (has(buffs, "rangedBoost")) state.setRangedBoost(getInt(buffs, "rangedBoost"));
        if (has(buffs, "magicBoost")) state.setMagicBoost(getInt(buffs, "magicBoost"));
        if (has(buffs, "currentHp")) state.setCurrentHitpoints(getInt(buffs, "currentHp"));
        if (has(buffs, "onSlayerTask")) state.setOnSlayerTask(buffs.get("onSlayerTask").getAsBoolean());
        if (has(buffs, "inWilderness")) state.setInWilderness(buffs.get("inWilderness").getAsBoolean());
        if (has(buffs, "kandarinDiary")) state.setKandarinDiary(buffs.get("kandarinDiary").getAsBoolean());
        if (has(buffs, "chargeSpell")) state.setChargeSpellActive(buffs.get("chargeSpell").getAsBoolean());
        if (has(buffs, "usingSunfireRunes")) state.setUsingSunfireRunes(buffs.get("usingSunfireRunes").getAsBoolean());
        if (has(buffs, "forinthrySurge")) state.setForinthrySurgeActive(buffs.get("forinthrySurge").getAsBoolean());
        if (has(buffs, "markOfDarknessSpell")) state.setMarkOfDarknessActive(buffs.get("markOfDarknessSpell").getAsBoolean());
        if (has(buffs, "soulreaperStacks")) state.setSoulreaperStacks(getInt(buffs, "soulreaperStacks"));

        CombatStyle style = buildCombatStyle(playerJson.getAsJsonObject("style"));
        applySpell(state, playerJson.get("spell"));
        FixtureEquipmentAdapter.apply(state, playerJson, monsterId);
        state.setCombatStyle(style);
        state.setActivePrayers(buildPrayers(playerJson.getAsJsonArray("prayers")));
        return state;
    }

    private static void applySpell(PlayerState state, JsonElement spellElement) {
        if (spellElement == null || spellElement.isJsonNull()) {
            return;
        }
        if (spellElement.isJsonPrimitive()) {
            state.setSpellName(spellElement.getAsString());
            return;
        }
        JsonObject spell = spellElement.getAsJsonObject();
        state.setSpellName(getString(spell, "name")); state.setSpellbook(getString(spell, "spellbook"));
        state.setSpellElement(getString(spell, "element")); state.setSpellMaxHit(getInt(spell, "max_hit"));
    }

    static MonsterStats buildMonster(JsonObject monsterJson) {
        MonsterStats monster = new MonsterStats();
        monster.setId(getInt(monsterJson, "id"));
        monster.setName(monsterJson.get("name").getAsString());
        monster.setVersion(getString(monsterJson, "version"));
        monster.setSize(getInt(monsterJson, "size"));
        monster.setSpeed(getInt(monsterJson, "speed"));

        JsonObject skills = monsterJson.getAsJsonObject("skills");
        monster.setAttackLevel(getInt(skills, "atk"));
        monster.setStrengthLevel(getInt(skills, "str"));
        monster.setDefenceLevel(getInt(skills, "def"));
        monster.setHitpoints(getInt(skills, "hp"));
        monster.setMagicLevel(getInt(skills, "magic"));
        monster.setRangedLevel(getInt(skills, "ranged"));

        JsonObject offensive = monsterJson.getAsJsonObject("offensive");
        monster.setOffensiveMagic(getInt(offensive, "magic"));

        JsonObject defensive = monsterJson.getAsJsonObject("defensive");
        monster.setFlatArmour(getInt(defensive, "flat_armour"));
        monster.setStabDefence(getInt(defensive, "stab"));
        monster.setSlashDefence(getInt(defensive, "slash"));
        monster.setCrushDefence(getInt(defensive, "crush"));
        monster.setMagicDefence(getInt(defensive, "magic"));
        monster.setLightRangedDefence(getInt(defensive, "light"));
        monster.setStandardRangedDefence(getInt(defensive, "standard"));
        monster.setHeavyRangedDefence(getInt(defensive, "heavy"));
        monster.setAttributes(buildAttributes(monsterJson.getAsJsonArray("attributes")));
        monster.setInputs(buildMonsterInputs(monsterJson.getAsJsonObject("inputs")));

        if (monsterJson.has("weakness") && monsterJson.get("weakness").isJsonObject()) {
            JsonObject weakness = monsterJson.getAsJsonObject("weakness");
            monster.setWeaknessElement(WeaknessElement.fromJson(getString(weakness, "element")));
            monster.setWeaknessSeverity(getInt(weakness, "severity"));
        }
        return monster;
    }

    private static Set<Prayer> buildPrayers(JsonArray prayersJson) {
        Set<Prayer> prayers = EnumSet.noneOf(Prayer.class);
        for (JsonElement prayerJson : prayersJson) prayers.add(Prayer.valueOf(toEnumName(prayerJson.getAsString())));
        return prayers;
    }

    private static Set<MonsterAttribute> buildAttributes(JsonArray attributesJson) {
        Set<MonsterAttribute> attributes = EnumSet.noneOf(MonsterAttribute.class);
        for (JsonElement attributeJson : attributesJson) {
            MonsterAttribute attribute = MonsterAttribute.fromJson(attributeJson.getAsString());
            if (attribute != null) attributes.add(attribute);
        }
        return attributes;
    }

    private static MonsterInputs buildMonsterInputs(JsonObject inputsJson) {
        MonsterInputs inputs = new MonsterInputs();
        inputs.setToaInvocationLevel(getInt(inputsJson, "toaInvocationLevel"));
        inputs.setToaPathLevel(getInt(inputsJson, "toaPathLevel"));
        inputs.setMonsterCurrentHp(getInt(inputsJson, "monsterCurrentHp"));
        if (has(inputsJson, "isFromCoxCm")) inputs.setFromCoxCm(inputsJson.get("isFromCoxCm").getAsBoolean());
        if (has(inputsJson, "partyMaxCombatLevel")) inputs.setPartyMaxCombatLevel(getInt(inputsJson, "partyMaxCombatLevel"));
        if (has(inputsJson, "partySumMiningLevel")) inputs.setPartySumMiningLevel(getInt(inputsJson, "partySumMiningLevel"));
        if (has(inputsJson, "partyMaxHpLevel")) inputs.setPartyMaxHpLevel(getInt(inputsJson, "partyMaxHpLevel"));
        inputs.setPartySize(getInt(inputsJson, "partySize"));
        if (has(inputsJson, "demonbaneVulnerability")) inputs.setDemonbaneVulnerability(getInt(inputsJson, "demonbaneVulnerability"));
        if (has(inputsJson, "phase")) inputs.setPhase(getString(inputsJson, "phase"));
        JsonObject reductionsJson = inputsJson.getAsJsonObject("defenceReductions");
        MonsterInputs.DefenceReductions reductions = new MonsterInputs.DefenceReductions();
        reductions.setDwh(getInt(reductionsJson, "dwh"));
        reductions.setBgs(getInt(reductionsJson, "bgs"));
        reductions.setArclight(getInt(reductionsJson, "arclight"));
        reductions.setEmberlight(getInt(reductionsJson, "emberlight"));
        reductions.setTonalztic(getInt(reductionsJson, "tonalztic"));
        reductions.setElderMaul(getInt(reductionsJson, "elderMaul"));
        reductions.setVulnerability(getBooleanAsInt(reductionsJson, "vulnerability"));
        reductions.setAccursedSceptre(getBooleanAsInt(reductionsJson, "accursed"));
        reductions.setSeercull(getInt(reductionsJson, "seercull"));
        reductions.setAyak(getInt(reductionsJson, "ayak"));
        inputs.setDefenceReductions(reductions);
        return inputs;
    }

    private static CombatStyle buildCombatStyle(JsonObject styleJson) {
        String type = styleJson.get("type").getAsString();
        String stance = styleJson.get("stance").getAsString();
        switch (type) {
            case "stab":
                switch (stance) {
                    case "Aggressive": return CombatStyle.MELEE_AGGRESSIVE_STAB;
                    case "Controlled": return CombatStyle.MELEE_CONTROLLED_STAB;
                    case "Defensive": return CombatStyle.MELEE_DEFENSIVE_STAB;
                    default: return CombatStyle.MELEE_ACCURATE_STAB;
                }
            case "slash":
                switch (stance) {
                    case "Aggressive": return CombatStyle.MELEE_AGGRESSIVE_SLASH;
                    case "Controlled": return CombatStyle.MELEE_CONTROLLED_SLASH;
                    case "Defensive": return CombatStyle.MELEE_DEFENSIVE_SLASH;
                    default: return CombatStyle.MELEE_ACCURATE_SLASH;
                }
            case "crush":
                switch (stance) {
                    case "Aggressive": return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
                    case "Defensive": return CombatStyle.MELEE_DEFENSIVE_CRUSH;
                    default: return CombatStyle.MELEE_ACCURATE_CRUSH;
                }
            case "ranged":
                switch (stance) {
                    case "Rapid": return CombatStyle.RANGED_RAPID;
                    case "Longrange": return CombatStyle.RANGED_LONGRANGE;
                    default: return CombatStyle.RANGED_ACCURATE;
                }
            case "magic":
                switch (stance) {
                    case "Manual Cast": return new CombatStyle("Manual Cast", AttackType.MAGIC, "Manual Cast", 0, 0, 0, 0, 0);
                    case "Longrange": return CombatStyle.MAGIC_LONGRANGE;
                    case "Autocast": return CombatStyle.MAGIC_AUTOCAST;
                    case "Defensive Autocast": return CombatStyle.MAGIC_DEFENSIVE_AUTOCAST;
                    default: return CombatStyle.MAGIC_ACCURATE;
                }
            default:
                throw new IllegalArgumentException("Unsupported combat style: " + type + "/" + stance);
        }
    }

    private static boolean has(JsonObject json, String key) { return json != null && json.has(key) && !json.get(key).isJsonNull(); }

    private static int getInt(JsonObject json, String key) { return has(json, key) ? json.get(key).getAsInt() : 0; }

    private static int getBooleanAsInt(JsonObject json, String key) { return has(json, key) && json.get(key).getAsBoolean() ? 1 : 0; }

    private static String getString(JsonObject json, String key) { return has(json, key) ? json.get(key).getAsString() : ""; }

    private static String toEnumName(String name) { return name.toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_'); }
}
