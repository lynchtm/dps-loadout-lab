package com.dpscalc.fixture;

import com.dpscalc.fixture.FixtureDocument.FixtureCase;
import com.dpscalc.fixture.FixtureDocument.FixtureInputs;
import com.dpscalc.fixture.FixtureDocument.PrimitiveValue;
import com.dpscalc.fixture.FixtureDocument.RawEquipmentItem;
import com.dpscalc.fixture.FixtureDocument.RawMonster;
import com.dpscalc.fixture.FixtureDocument.RawPlayer;
import com.dpscalc.fixture.FixtureDocument.RawSpell;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FixtureDocumentLoader {
    private FixtureDocumentLoader() {}

    public static FixtureDocument loadResource(String resourcePath, String expectedSha, String expectedDigest) {
        try (InputStream stream = FixtureDocumentLoader.class.getResourceAsStream(resourcePath)) {
            if (stream == null) throw new FixtureDocumentException("resource", "not found: " + resourcePath);
            return loadString(new String(stream.readAllBytes(), StandardCharsets.UTF_8), expectedSha, expectedDigest);
        } catch (IOException error) {
            throw new FixtureDocumentException("resource", "could not read: " + resourcePath, error);
        }
    }

    public static FixtureDocument loadString(String json, String expectedSha, String expectedDigest) {
        final JsonElement parsed;
        try {
            parsed = (new JsonParser()).parse(json);
        } catch (JsonParseException error) {
            throw new FixtureDocumentException("$", "malformed JSON", error);
        }
        if (!parsed.isJsonObject()) throw new FixtureDocumentException("$", "expected object");
        JsonObject root = parsed.getAsJsonObject();
        FixtureJsonValidator.validate(root, expectedSha, expectedDigest);
        return buildDocument(root);
    }

    private static FixtureDocument buildDocument(JsonObject root) {
        List<FixtureCase> fixtures = new ArrayList<>();
        for (JsonElement element : root.getAsJsonArray("fixtures")) {
            JsonObject fixture = element.getAsJsonObject();
            JsonObject inputs = fixture.getAsJsonObject("inputs");
            JsonObject options = inputs.getAsJsonObject("options");
            fixtures.add(new FixtureCase(
                fixture.get("id").getAsString(),
                fixture.get("name").getAsString(),
                fixture.get("category").getAsString(),
                fixture.get("source").getAsString(),
                new FixtureInputs(buildPlayer(inputs.getAsJsonObject("player")), buildMonster(inputs.getAsJsonObject("monster")), options.get("usingSpecialAttack").getAsBoolean()),
                FixtureOutputParser.parse(fixture.getAsJsonObject("outputs")),
                fixture
            ));
        }
        return new FixtureDocument(root.get("schemaVersion").getAsInt(), root.get("webCalcCommit").getAsString(),
            root.get("equipmentDomainDigest").getAsString(), root.get("totalScenarios").getAsInt(), fixtures);
    }

    private static RawPlayer buildPlayer(JsonObject player) {
        JsonObject style = player.getAsJsonObject("style");
        JsonElement spell = player.get("spell");
        return new RawPlayer(
            intMap(player.getAsJsonObject("skills")),
            intMap(player.getAsJsonObject("boosts")),
            stringList(player.getAsJsonArray("prayers")),
            valueMap(player.getAsJsonObject("buffs")),
            style.get("type").getAsString(),
            style.get("stance").getAsString(),
            spell.isJsonNull() ? null : buildSpell(spell.getAsJsonObject()),
            equipmentMap(player.getAsJsonObject("equipment"))
        );
    }

    private static RawSpell buildSpell(JsonObject spell) {
        JsonElement element = spell.get("element");
        return new RawSpell(
            spell.get("name").getAsString(),
            spell.get("spellbook").getAsString(),
            element.isJsonNull() ? null : element.getAsString(),
            spell.get("max_hit").getAsInt()
        );
    }

    private static RawMonster buildMonster(JsonObject monster) {
        JsonElement weakness = monster.get("weakness");
        return new RawMonster(
            monster.get("id").getAsInt(),
            monster.get("version").getAsString(),
            monster.get("name").getAsString(),
            monster.get("size").getAsInt(),
            monster.get("speed").getAsInt(),
            intMap(monster.getAsJsonObject("skills")),
            doubleMap(monster.getAsJsonObject("offensive")),
            doubleMap(monster.getAsJsonObject("defensive")),
            stringList(monster.getAsJsonArray("attributes")),
            weakness.isJsonNull() ? null : valueMap(weakness.getAsJsonObject()),
            valueMap(monster.getAsJsonObject("inputs"))
        );
    }

    private static Map<String, RawEquipmentItem> equipmentMap(JsonObject equipment) {
        Map<String, RawEquipmentItem> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : equipment.entrySet()) {
            if (entry.getValue().isJsonNull()) {
                values.put(entry.getKey(), null);
            } else {
                JsonObject item = entry.getValue().getAsJsonObject();
                values.put(entry.getKey(), new RawEquipmentItem(item.get("id").getAsInt(), valueMap(item.getAsJsonObject("itemVars"))));
            }
        }
        return values;
    }

    private static Map<String, Integer> intMap(JsonObject object) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) values.put(entry.getKey(), entry.getValue().getAsInt());
        return values;
    }

    private static Map<String, Double> doubleMap(JsonObject object) {
        Map<String, Double> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) values.put(entry.getKey(), entry.getValue().getAsDouble());
        return values;
    }

    private static List<String> stringList(JsonArray array) {
        List<String> values = new ArrayList<>();
        for (JsonElement element : array) values.add(element.getAsString());
        return values;
    }

    private static Map<String, PrimitiveValue> valueMap(JsonObject object) {
        Map<String, PrimitiveValue> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) values.put(entry.getKey(), value(entry.getValue()));
        return values;
    }

    private static PrimitiveValue value(JsonElement element) {
        if (element.isJsonObject()) return PrimitiveValue.ofObject(valueMap(element.getAsJsonObject()));
        if (element.getAsJsonPrimitive().isBoolean()) return PrimitiveValue.ofBoolean(element.getAsBoolean());
        if (element.getAsJsonPrimitive().isNumber()) return PrimitiveValue.ofNumber(element.getAsDouble());
        return PrimitiveValue.ofString(element.getAsString());
    }
}
