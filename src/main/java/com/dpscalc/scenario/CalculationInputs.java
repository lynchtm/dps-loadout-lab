package com.dpscalc.scenario;

import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.PlayerState;
import com.google.gson.JsonObject;

/** Defines which inputs affect calculations, independently of presentation and packing. */
public final class CalculationInputs {
    private CalculationInputs() {}

    public static Scenario.Loadout live(
            PlayerState player, boolean slayer, boolean charge, boolean special) {
        Scenario.Loadout loadout = new Scenario.Loadout();
        loadout.name = "Live player";
        loadout.player = Scenario.copy(player);
        loadout.player.setOnSlayerTask(slayer);
        loadout.player.setChargeSpellActive(charge);
        loadout.specialAttack = special;
        return loadout;
    }

    public static String loadoutKey(Scenario.Loadout loadout) {
        JsonObject inputs = Scenario.JSON.toJsonTree(loadout).getAsJsonObject();
        for (String field :
                new String[] {
                    "name",
                    "description",
                    "inventory",
                    "carry",
                    "overrides",
                    "sources",
                    "wikiSource",
                    "wikiRevision",
                    "wikiNotes",
                    "wikiSeeds"
                }) inputs.remove(field);
        return inputs.toString();
    }

    public static String key(Scenario.Loadout loadout, MonsterStats target, Encounter encounter) {
        return loadoutKey(loadout)
                + "/"
                + Scenario.JSON.toJson(target)
                + "/"
                + Scenario.JSON.toJson(encounter);
    }

    public static String workspaceKey(Scenario scenario) {
        StringBuilder key = new StringBuilder();
        for (Scenario.Loadout loadout : scenario.loadouts)
            key.append(loadoutKey(loadout)).append('\n');
        return key
                + "/"
                + Scenario.JSON.toJson(scenario.target)
                + "/"
                + Scenario.JSON.toJson(scenario.encounter);
    }
}
