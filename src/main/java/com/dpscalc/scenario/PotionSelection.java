package com.dpscalc.scenario;

import com.loadoutlab.model.PlayerState;

import java.util.*;

public final class PotionSelection {
    public static final List<String> OPTIONS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "Super combat",
                            "Ranging",
                            "Imbued heart",
                            "Saturated heart",
                            "Forgotten brew",
                            "Overload",
                            "Overload (+)",
                            "Smelling salts",
                            "Attack",
                            "Strength",
                            "Defence",
                            "Magic",
                            "Super attack",
                            "Super strength",
                            "Super defence",
                            "Super ranging",
                            "Super magic"));

    public static Set<String> selected(Scenario.Loadout loadout) {
        Set<String> values = new LinkedHashSet<>(loadout.potions);
        if (values.isEmpty() && loadout.boost != null && !loadout.boost.equals("None"))
            values.add(loadout.boost);
        return values;
    }

    public static void toggle(Scenario.Loadout loadout, String potion) {
        Set<String> values = selected(loadout);
        if (!values.remove(potion)) values.add(potion);
        loadout.potions = values;
        loadout.boost = "None";
    }

    public static void apply(PlayerState player, Scenario.Loadout loadout) {
        // Keep legacy single-preset scenarios numerically unchanged until edited.
        if (loadout.potions.isEmpty()) {
            ScenarioCalculator.applyBoost(
                    player, loadout.boost, loadout.elapsedSeconds, loadout.divine);
            return;
        }
        int attack = 0, strength = 0, defence = 0, ranged = 0, magic = 0;
        for (String potion : loadout.potions) {
            PlayerState candidate = Scenario.copy(player);
            candidate.setAttackBoost(0);
            candidate.setStrengthBoost(0);
            candidate.setDefenceBoost(0);
            candidate.setRangedBoost(0);
            candidate.setMagicBoost(0);
            ScenarioCalculator.applyBoost(
                    candidate, potion, loadout.elapsedSeconds, loadout.divine);
            attack = Math.max(attack, candidate.getAttackBoost());
            strength = Math.max(strength, candidate.getStrengthBoost());
            defence = Math.max(defence, candidate.getDefenceBoost());
            ranged = Math.max(ranged, candidate.getRangedBoost());
            magic = Math.max(magic, candidate.getMagicBoost());
        }
        player.setAttackBoost(attack);
        player.setStrengthBoost(strength);
        player.setDefenceBoost(defence);
        player.setRangedBoost(ranged);
        player.setMagicBoost(magic);
    }
}
