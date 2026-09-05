package com.dpscalc;

import com.dpscalc.fixture.FixtureDocument;
import com.dpscalc.fixture.FixtureDocumentException;
import com.dpscalc.fixture.FixtureDocumentLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FixtureDocumentTest {
    private static final String TARGET_SHA = "b6bc098dc0d742b2b763375d2e78e1b611a22070";
    private static final String RULE_DIGEST = "070a34ce7f6267be1ae8c84cfdd3757100f5a027898ba34937eaba16536ea951";

    @Test
    public void loadsValidRawDocumentAsImmutableDtos() {
        FixtureDocument document = FixtureDocumentLoader.loadResource("/fixtures.json", TARGET_SHA, RULE_DIGEST);

        assertEquals(150, document.getDeclaredCount());
        assertEquals(150, document.getFixtures().size());
        assertEquals(4151, document.getFixtures().get(0).getInputs().getPlayer().getEquipment().get("weapon").getId());
        assertEquals("Catacombs of Kourend", document.getFixtures().get(0).getInputs().getMonster().getVersion());
        assertThrows(UnsupportedOperationException.class, () -> document.getFixtures().clear());
    }

    @Test
    public void rejectsMissingResource() {
        FixtureDocumentException error = assertThrows(FixtureDocumentException.class,
            () -> FixtureDocumentLoader.loadResource("/missing-fixtures-v3.json", TARGET_SHA, RULE_DIGEST));

        assertTrue(error.getMessage().contains("resource"));
    }

    @Test
    public void missingResourceCannotProduceZeroTestSuccess() {
        FixtureDocumentException error = assertThrows(FixtureDocumentException.class,
            () -> FixtureReplayTest.loadFixtures("/missing-fixtures-v3.json"));

        assertTrue(error.getMessage().contains("resource"));
        assertTrue(error.getMessage().contains("missing-fixtures-v3.json"));
    }

    @Test
    public void rejectsMalformedJson() {
        FixtureDocumentException error = assertThrows(FixtureDocumentException.class,
            () -> FixtureDocumentLoader.loadString("{", TARGET_SHA, RULE_DIGEST));

        assertTrue(error.getMessage().contains("$"));
    }

    @Test
    public void rejectsWrongReferenceSha() {
        String wrongSha = validJson().replace(TARGET_SHA, "0000000000000000000000000000000000000000");

        FixtureDocumentException error = assertThrows(FixtureDocumentException.class,
            () -> FixtureDocumentLoader.loadString(wrongSha, TARGET_SHA, RULE_DIGEST));

        assertTrue(error.getMessage().contains("$.webCalcCommit"));
    }

    @Test
    public void rejectsMalformedDocuments() {
        for (InvalidDocument invalid : invalidDocuments()) {
            FixtureDocumentException error = assertThrows(invalid.name, FixtureDocumentException.class,
                () -> FixtureDocumentLoader.loadString(invalid.json, TARGET_SHA, RULE_DIGEST));
            assertTrue(invalid.name + " path", error.getMessage().contains(invalid.path));
        }
    }

    private static List<InvalidDocument> invalidDocuments() {
        List<InvalidDocument> documents = new ArrayList<>();
        documents.add(new InvalidDocument("empty fixtures", mutate(root -> {
            root.addProperty("totalScenarios", 0);
            root.addProperty("successfulScenarios", 0);
            root.add("fixtures", new JsonArray());
        }), "$.fixtures"));
        documents.add(new InvalidDocument("unknown field", mutate(root -> root.addProperty("unknown", true)), "$.unknown"));
        documents.add(new InvalidDocument("missing field", mutate(root -> root.remove("totalScenarios")), "$.totalScenarios"));
        documents.add(new InvalidDocument("mistyped field", mutate(root -> root.addProperty("totalScenarios", "1")), "$.totalScenarios"));
        documents.add(new InvalidDocument("duplicate IDs", mutate(root -> {
            JsonArray fixtures = root.getAsJsonArray("fixtures");
            fixtures.add(fixtures.get(0).deepCopy());
            root.addProperty("totalScenarios", fixtures.size());
            root.addProperty("successfulScenarios", fixtures.size());
        }), "$.fixtures[150].id"));
        documents.add(new InvalidDocument("wrong digest", mutate(root -> root.addProperty("equipmentDomainDigest", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb")), "$.equipmentDomainDigest"));
        documents.add(new InvalidDocument("wrong schema", mutate(root -> root.addProperty("schemaVersion", 2)), "$.schemaVersion"));
        documents.add(new InvalidDocument("count mismatch", mutate(root -> root.addProperty("totalScenarios", 2)), "$.totalScenarios"));
        documents.add(new InvalidDocument("successful count mismatch", mutate(root -> root.addProperty("successfulScenarios", 2)), "$.totalScenarios"));
        documents.add(new InvalidDocument("failed count mismatch", mutate(root -> root.addProperty("failedScenarios", 1)), "$.totalScenarios"));
        documents.add(new InvalidDocument("fractional discrete output", mutate(root -> root.getAsJsonArray("fixtures").get(0).getAsJsonObject().getAsJsonObject("outputs").addProperty("maxAttackRoll", 1.5)), "$.fixtures[0].outputs.maxAttackRoll"));
        documents.add(new InvalidDocument("zero-total distribution", mutate(root -> {
            JsonArray distribution = root.getAsJsonArray("fixtures").get(0).getAsJsonObject().getAsJsonObject("outputs").getAsJsonArray("normalizedDistribution");
            for (int index = 0; index < distribution.size(); index++) distribution.get(index).getAsJsonObject().addProperty("probability", 0);
        }), "$.fixtures[0].outputs.normalizedDistribution"));
        documents.add(new InvalidDocument("non-finite output", validJson().replaceFirst("\"dps\"\\s*:\\s*[^,}]+", "\"dps\": 1e400"), "$.fixtures[0].outputs.dps"));
        documents.add(new InvalidDocument("positive integer overflow", mutate(root -> root.getAsJsonArray("fixtures").get(0).getAsJsonObject().getAsJsonObject("inputs").getAsJsonObject("monster").addProperty("id", 2147483648L)), "$.fixtures[0].inputs.monster.id"));
        documents.add(new InvalidDocument("negative integer overflow", mutate(root -> root.getAsJsonArray("fixtures").get(0).getAsJsonObject().getAsJsonObject("inputs").getAsJsonObject("monster").addProperty("id", -2147483649L)), "$.fixtures[0].inputs.monster.id"));
        documents.add(new InvalidDocument("derived input", mutate(root -> root.getAsJsonArray("fixtures").get(0).getAsJsonObject().getAsJsonObject("inputs").getAsJsonObject("player").getAsJsonObject("equipment").addProperty("weaponSpeed", 4)), "$.fixtures[0].inputs.player.equipment.weaponSpeed"));
        return documents;
    }

    private static String mutate(Mutation mutation) {
        JsonObject root = (new JsonParser()).parse(validJson()).getAsJsonObject();
        mutation.apply(root);
        return root.toString();
    }

    private static String validJson() {
        try (InputStream stream = FixtureDocumentTest.class.getResourceAsStream("/fixtures.json")) {
            if (stream == null) {
                throw new AssertionError("fixtures.json missing");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }

    private interface Mutation {
        void apply(JsonObject root);
    }

    private static final class InvalidDocument {
        private final String name;
        private final String json;
        private final String path;

        private InvalidDocument(String name, String json, String path) {
            this.name = name;
            this.json = json;
            this.path = path;
        }
    }
}
