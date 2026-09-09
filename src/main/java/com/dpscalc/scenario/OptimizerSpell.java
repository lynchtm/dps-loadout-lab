package com.dpscalc.scenario;

import com.google.gson.*;
import com.loadoutlab.data.*;
import com.loadoutlab.model.*;
import com.loadoutlab.model.PlayerState;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntFunction;

/** Supported autocast spells, with factual casting requirements attributed in NOTICE.md. */
final class OptimizerSpell {
    final String name, book, element;
    final int maxHit, level;

    private OptimizerSpell(JsonObject data, int level) {
        name = data.get("name").getAsString();
        book = data.get("spellbook").getAsString();
        element = data.get("element").isJsonNull() ? null : data.get("element").getAsString();
        maxHit = data.get("max_hit").getAsInt();
        this.level = level;
    }

    static List<OptimizerSpell> load() {
        List<OptimizerSpell> spells = new ArrayList<>();
        try (Reader spellsReader = reader("spells.json")) {
            for (JsonElement entry : new JsonParser().parse(spellsReader).getAsJsonArray()) {
                JsonObject data = entry.getAsJsonObject();
                String name = data.get("name").getAsString();
                if (data.get("max_hit").getAsInt() > 0)
                    spells.add(new OptimizerSpell(data, data.get("level").getAsInt()));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Optimizer spell catalog unavailable", ex);
        }
        // Start with high damage spells when a large search reaches its time/score cap.
        spells.sort(
                Comparator.comparingInt((OptimizerSpell spell) -> spell.maxHit)
                        .reversed()
                        .thenComparing(spell -> spell.name));
        return Collections.unmodifiableList(spells);
    }

    private static Reader reader(String file) {
        return new InputStreamReader(
                OptimizerSpell.class.getResourceAsStream("/com/loadoutlab/" + file),
                StandardCharsets.UTF_8);
    }

    boolean eligible(int magic, MonsterStats target) {
        if (level > magic) return false;
        if (name.endsWith("Demonbane"))
            return target.getAttributes().contains(MonsterAttribute.DEMON);
        return !name.equals("Crumble Undead")
                || target.getAttributes().contains(MonsterAttribute.UNDEAD);
    }

    boolean compatible(String weapon, int[] ids, IntFunction<String> names) {
        weapon = weapon.toLowerCase(Locale.ROOT);
        IntFunction<String> originalNames = names;
        names = id -> Objects.toString(originalNames.apply(id), "").toLowerCase(Locale.ROOT);
        if (book.equals("ancient"))
            return weapon.equals("ancient staff")
                    || weapon.contains("ancient sceptre")
                    || weapon.equals("ancient sceptre")
                    || weapon.equals("master wand")
                    || weapon.equals("kodai wand")
                    || weapon.equals("dragon hunter wand")
                    || weapon.equals("blue moon spear")
                    || weapon.contains("nightmare staff") && !weapon.startsWith("harmonised")
                    || weapon.equals("nightmare staff")
                    || alternateSceptre(weapon)
                    || weapon.startsWith("ahrim's staff")
                            && names.apply(ids[0]).startsWith("ahrim's hood")
                            && names.apply(ids[4]).startsWith("ahrim's robetop")
                            && names.apply(ids[7]).startsWith("ahrim's robeskirt")
                            && names.apply(ids[2]).startsWith("amulet of the damned");
        if (book.equals("arceuus"))
            return weapon.startsWith("skull sceptre")
                    || weapon.startsWith("slayer's staff")
                    || weapon.startsWith("ahrim's staff")
                    || weapon.equals("blue moon spear")
                    || weapon.equals("staff of the dead")
                    || weapon.equals("toxic staff of the dead")
                    || weapon.equals("purging staff")
                    || weapon.equals("master wand")
                    || weapon.equals("kodai wand");
        switch (name.toLowerCase(Locale.ROOT)) {
            case "magic dart":
                return weapon.startsWith("slayer's staff")
                        || weapon.equals("staff of the dead")
                        || weapon.equals("toxic staff of the dead")
                        || weapon.equals("staff of light")
                        || weapon.equals("staff of balance");
            case "iban blast":
                return weapon.startsWith("iban's staff");
            case "claws of guthix":
                return weapon.equals("staff of balance") || weapon.equals("void knight mace");
            case "saradomin strike":
                return weapon.equals("staff of light");
            case "flames of zamorak":
                return weapon.equals("staff of the dead")
                        || weapon.equals("toxic staff of the dead")
                        || alternateSceptre(weapon);
            case "crumble undead":
                return weapon.equals("skull sceptre (i)")
                        || weapon.startsWith("slayer's staff")
                        || weapon.equals("void knight mace")
                        || weapon.equals("staff of the dead")
                        || weapon.equals("toxic staff of the dead")
                        || weapon.equals("staff of light")
                        || weapon.equals("staff of balance");
            default:
                if (element == null || weapon.startsWith("skull sceptre")) return false;
                return !(weapon.startsWith("slayer's staff") || weapon.equals("void knight mace"))
                        || name.toLowerCase(Locale.ROOT).endsWith(" wave")
                        || name.toLowerCase(Locale.ROOT).endsWith(" surge");
        }
    }

    private static boolean alternateSceptre(String weapon) {
        return (weapon.startsWith("thammaron's sceptre") || weapon.startsWith("accursed sceptre"))
                && weapon.endsWith("(a)");
    }

    void apply(PlayerState player) {
        player.setSpellName(name);
        player.setSpellbook(book);
        player.setSpellElement(element);
        player.setSpellMaxHit(maxHit);
    }

    static void clear(PlayerState player) {
        player.setSpellName(null);
        player.setSpellbook(null);
        player.setSpellElement(null);
        player.setSpellMaxHit(0);
    }
}
