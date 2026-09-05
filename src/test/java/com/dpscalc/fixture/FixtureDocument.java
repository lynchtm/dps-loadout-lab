package com.dpscalc.fixture;

import com.dpscalc.parity.ParityOutput;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FixtureDocument {
    private final int schemaVersion;
    private final String webCalcCommit;
    private final String equipmentDomainDigest;
    private final int declaredCount;
    private final List<FixtureCase> fixtures;

    FixtureDocument(int schemaVersion, String webCalcCommit, String equipmentDomainDigest,
                    int declaredCount, List<FixtureCase> fixtures) {
        this.schemaVersion = schemaVersion;
        this.webCalcCommit = webCalcCommit;
        this.equipmentDomainDigest = equipmentDomainDigest;
        this.declaredCount = declaredCount;
        this.fixtures = Collections.unmodifiableList(new ArrayList<>(fixtures));
    }

    public int getSchemaVersion() { return schemaVersion; }
    public String getWebCalcCommit() { return webCalcCommit; }
    public String getEquipmentDomainDigest() { return equipmentDomainDigest; }
    public int getDeclaredCount() { return declaredCount; }
    public List<FixtureCase> getFixtures() { return fixtures; }

    public static final class FixtureCase {
        private final String id;
        private final String name;
        private final String category;
        private final String source;
        private final FixtureInputs inputs;
        private final ParityOutput outputs;
        private final JsonObject fixtureJson;

        FixtureCase(String id, String name, String category, String source, FixtureInputs inputs,
                    ParityOutput outputs, JsonObject fixtureJson) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.source = source;
            this.inputs = inputs;
            this.outputs = outputs;
            this.fixtureJson = fixtureJson.deepCopy();
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getCategory() { return category; }
        public String getSource() { return source; }
        public FixtureInputs getInputs() { return inputs; }
        public ParityOutput getOutputs() { return outputs; }
        public JsonObject getFixtureJson() { return fixtureJson.deepCopy(); }
    }

    public static final class FixtureInputs {
        private final RawPlayer player;
        private final RawMonster monster;
        private final boolean usingSpecialAttack;

        FixtureInputs(RawPlayer player, RawMonster monster, boolean usingSpecialAttack) {
            this.player = player;
            this.monster = monster;
            this.usingSpecialAttack = usingSpecialAttack;
        }

        public RawPlayer getPlayer() { return player; }
        public RawMonster getMonster() { return monster; }
        public boolean isUsingSpecialAttack() { return usingSpecialAttack; }
    }

    public static final class RawPlayer {
        private final Map<String, Integer> skills;
        private final Map<String, Integer> boosts;
        private final List<String> prayers;
        private final Map<String, PrimitiveValue> buffs;
        private final String styleType;
        private final String styleStance;
        private final RawSpell spell;
        private final Map<String, RawEquipmentItem> equipment;

        RawPlayer(Map<String, Integer> skills, Map<String, Integer> boosts, List<String> prayers,
                  Map<String, PrimitiveValue> buffs, String styleType, String styleStance, RawSpell spell,
                  Map<String, RawEquipmentItem> equipment) {
            this.skills = immutableMap(skills);
            this.boosts = immutableMap(boosts);
            this.prayers = Collections.unmodifiableList(new ArrayList<>(prayers));
            this.buffs = immutableMap(buffs);
            this.styleType = styleType;
            this.styleStance = styleStance;
            this.spell = spell;
            this.equipment = immutableMap(equipment);
        }

        public Map<String, Integer> getSkills() { return skills; }
        public Map<String, Integer> getBoosts() { return boosts; }
        public List<String> getPrayers() { return prayers; }
        public Map<String, PrimitiveValue> getBuffs() { return buffs; }
        public String getStyleType() { return styleType; }
        public String getStyleStance() { return styleStance; }
        public RawSpell getSpell() { return spell; }
        public Map<String, RawEquipmentItem> getEquipment() { return equipment; }
    }

    public static final class RawSpell {
        private final String name;
        private final String spellbook;
        private final String element;
        private final int maxHit;

        RawSpell(String name, String spellbook, String element, int maxHit) {
            this.name = name;
            this.spellbook = spellbook;
            this.element = element;
            this.maxHit = maxHit;
        }

        public String getName() { return name; }
        public String getSpellbook() { return spellbook; }
        public String getElement() { return element; }
        public int getMaxHit() { return maxHit; }
    }

    public static final class RawEquipmentItem {
        private final int id;
        private final Map<String, PrimitiveValue> itemVars;

        RawEquipmentItem(int id, Map<String, PrimitiveValue> itemVars) {
            this.id = id;
            this.itemVars = immutableMap(itemVars);
        }

        public int getId() { return id; }
        public Map<String, PrimitiveValue> getItemVars() { return itemVars; }
    }

    public static final class RawMonster {
        private final int id;
        private final String version;
        private final String name;
        private final int size;
        private final int speed;
        private final Map<String, Integer> skills;
        private final Map<String, Double> offensive;
        private final Map<String, Double> defensive;
        private final List<String> attributes;
        private final Map<String, PrimitiveValue> weakness;
        private final Map<String, PrimitiveValue> inputs;

        RawMonster(int id, String version, String name, int size, int speed, Map<String, Integer> skills,
                   Map<String, Double> offensive, Map<String, Double> defensive,
                   List<String> attributes, Map<String, PrimitiveValue> weakness,
                   Map<String, PrimitiveValue> inputs) {
            this.id = id;
            this.version = version;
            this.name = name;
            this.size = size;
            this.speed = speed;
            this.skills = immutableMap(skills);
            this.offensive = immutableMap(offensive);
            this.defensive = immutableMap(defensive);
            this.attributes = Collections.unmodifiableList(new ArrayList<>(attributes));
            this.weakness = weakness == null ? null : immutableMap(weakness);
            this.inputs = immutableMap(inputs);
        }

        public int getId() { return id; }
        public String getVersion() { return version; }
        public String getName() { return name; }
        public int getSize() { return size; }
        public int getSpeed() { return speed; }
        public Map<String, Integer> getSkills() { return skills; }
        public Map<String, Double> getOffensive() { return offensive; }
        public Map<String, Double> getDefensive() { return defensive; }
        public List<String> getAttributes() { return attributes; }
        public Map<String, PrimitiveValue> getWeakness() { return weakness; }
        public Map<String, PrimitiveValue> getInputs() { return inputs; }
    }

    public static final class PrimitiveValue {
        public enum Kind { BOOLEAN, NUMBER, STRING, OBJECT }

        private final Kind kind;
        private final Boolean booleanValue;
        private final Double numberValue;
        private final String stringValue;
        private final Map<String, PrimitiveValue> objectValue;

        private PrimitiveValue(Kind kind, Boolean booleanValue, Double numberValue, String stringValue,
                               Map<String, PrimitiveValue> objectValue) {
            this.kind = kind;
            this.booleanValue = booleanValue;
            this.numberValue = numberValue;
            this.stringValue = stringValue;
            this.objectValue = objectValue == null ? null : immutableMap(objectValue);
        }

        static PrimitiveValue ofBoolean(boolean value) { return new PrimitiveValue(Kind.BOOLEAN, value, null, null, null); }
        static PrimitiveValue ofNumber(double value) { return new PrimitiveValue(Kind.NUMBER, null, value, null, null); }
        static PrimitiveValue ofString(String value) { return new PrimitiveValue(Kind.STRING, null, null, value, null); }
        static PrimitiveValue ofObject(Map<String, PrimitiveValue> value) { return new PrimitiveValue(Kind.OBJECT, null, null, null, value); }

        public Kind getKind() { return kind; }
        public Boolean getBooleanValue() { return booleanValue; }
        public Double getNumberValue() { return numberValue; }
        public String getStringValue() { return stringValue; }
        public Map<String, PrimitiveValue> getObjectValue() { return objectValue; }
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> source) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
