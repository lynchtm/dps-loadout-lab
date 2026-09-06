package com.dpscalc;

import com.dpscalc.state.PlayerState;
import com.dpscalc.equipment.EquipmentDomainException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FixtureStateFactoryTest {
    @Test
    public void derivesRawEquipmentWithoutUsingExpectedTotals() throws Exception {
        JsonObject fixture = rawFixture();
        JsonObject player = fixture.getAsJsonObject("inputs").getAsJsonObject("player");
        int monsterId = fixture.getAsJsonObject("inputs").getAsJsonObject("monster").get("id").getAsInt();
        JsonObject outputs = fixture.getAsJsonObject("outputs");
        outputs.add("equipment", (new JsonParser()).parse("{"
            + "\"bonuses\":{\"str\":999,\"magic_str\":999,\"ranged_str\":999,\"prayer\":999},"
            + "\"offensive\":{\"slash\":999,\"stab\":999,\"crush\":999,\"ranged\":999,\"magic\":999},"
            + "\"defensive\":{\"slash\":999,\"stab\":999,\"crush\":999,\"ranged\":999,\"magic\":999},"
            + "\"attackSpeed\":999}").getAsJsonObject());

        PlayerState poisoned = FixtureStateFactory.buildPlayer(player, monsterId);
        outputs.remove("equipment");
        PlayerState removed = FixtureStateFactory.buildPlayer(player, monsterId);

        assertEquals(82, poisoned.getEquipmentStats().getMeleeStrength());
        assertEquals(82, poisoned.getEquipmentStats().getSlashAttack());
        assertEquals(4, poisoned.getWeaponSpeed());
        assertEquals(4151, poisoned.getWeaponId());
        assertEquals(poisoned.getEquipmentStats().toString(), removed.getEquipmentStats().toString());
        assertEquals(poisoned.getWeaponSpeed(), removed.getWeaponSpeed());
    }

    @Test
    public void rejectsMalformedRawBlowpipeItemVariables() throws Exception {
        JsonObject fixture = rawFixture();
        JsonObject player = fixture.getAsJsonObject("inputs").getAsJsonObject("player");
        JsonObject weapon = player.getAsJsonObject("equipment").getAsJsonObject("weapon");
        weapon.addProperty("id", 12926);
        weapon.getAsJsonObject("itemVars").addProperty("blowpipeDartId", "dragon");

        EquipmentDomainException error = assertThrows(EquipmentDomainException.class,
            () -> FixtureStateFactory.buildPlayer(player, 415));

        assertTrue(error.getMessage().contains("itemVars.blowpipeDartId"));
    }

    @Test
    public void nullAndStandardSpellContextsDoNotApplyVirtusBonus() throws Exception {
        JsonObject player = rawFixture().getAsJsonObject("inputs").getAsJsonObject("player");
        equipVirtus(player);

        PlayerState withoutSpell = FixtureStateFactory.buildPlayer(player, 415);
        player.add("spell", (new JsonParser()).parse("{\"name\":\"Fire Surge\",\"spellbook\":\"standard\",\"element\":\"fire\",\"max_hit\":24}"));
        PlayerState standardSpell = FixtureStateFactory.buildPlayer(player, 415);

        assertEquals(60, withoutSpell.getEquipmentStats().getMagicDamage());
        assertEquals(60, standardSpell.getEquipmentStats().getMagicDamage());
    }

    @Test
    public void generatedAncientVirtusFixtureDerivesSpellbookBonus() throws Exception {
        JsonObject fixture = fixtureById("virtus-ancient-cast");
        JsonObject inputs = fixture.getAsJsonObject("inputs");

        PlayerState state = FixtureStateFactory.buildPlayer(
            inputs.getAsJsonObject("player"), inputs.getAsJsonObject("monster").get("id").getAsInt());

        assertEquals(150, state.getEquipmentStats().getMagicDamage());
        assertEquals(150, fixture.getAsJsonObject("outputs").getAsJsonObject("equipment")
            .getAsJsonObject("bonuses").get("magic_str").getAsInt());
    }

    private static void equipVirtus(JsonObject player) {
        JsonObject equipment = player.getAsJsonObject("equipment");
        equipment.add("head", (new JsonParser()).parse("{\"id\":26241,\"itemVars\":{}}"));
        equipment.add("body", (new JsonParser()).parse("{\"id\":26243,\"itemVars\":{}}"));
        equipment.add("legs", (new JsonParser()).parse("{\"id\":26245,\"itemVars\":{}}"));
        player.getAsJsonObject("style").addProperty("type", "magic");
        player.getAsJsonObject("style").addProperty("stance", "Autocast");
    }

    private static JsonObject fixtureById(String id) throws Exception {
        try (InputStream stream = FixtureStateFactoryTest.class.getResourceAsStream("/fixtures.json")) {
            assertNotNull("fixtures.json", stream);
            JsonArray fixtures = (new JsonParser()).parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("fixtures");
            for (JsonElement fixture : fixtures) {
                if (id.equals(fixture.getAsJsonObject().get("id").getAsString())) return fixture.getAsJsonObject();
            }
            throw new AssertionError("fixture missing: " + id);
        }
    }

    private static JsonObject rawFixture() throws Exception {
        try (InputStream stream = FixtureStateFactoryTest.class.getResourceAsStream("/fixtures-v3.json")) {
            assertNotNull("fixtures-v3.json", stream);
            JsonObject root = (new JsonParser()).parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            return root.getAsJsonArray("fixtures").get(0).getAsJsonObject();
        }
    }
}
