package com.dpscalc.scenario;

import com.dpscalc.data.*;
import com.dpscalc.state.PlayerState;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntFunction;

/** Supported autocast spells, with factual casting requirements attributed in NOTICE.md. */
final class OptimizerSpell {
    final String name,book,element;
    final int maxHit,level;
    private OptimizerSpell(JsonObject data,int level){
        name=data.get("name").getAsString();book=data.get("spellbook").getAsString();
        element=data.get("element").isJsonNull()?null:data.get("element").getAsString();
        maxHit=data.get("max_hit").getAsInt();this.level=level;
    }
    static List<OptimizerSpell> load(){
        List<OptimizerSpell> spells=new ArrayList<>();
        try(Reader levelsReader=reader("optimizer-spell-levels.json");Reader spellsReader=reader("spells.json")){
            JsonObject levels=new JsonParser().parse(levelsReader).getAsJsonObject();
            for(JsonElement entry:new JsonParser().parse(spellsReader).getAsJsonArray()){
                JsonObject data=entry.getAsJsonObject();String name=data.get("name").getAsString();
                if(levels.has(name)&&data.get("max_hit").getAsInt()>0)spells.add(new OptimizerSpell(data,levels.get(name).getAsInt()));
            }
        }catch(IOException ex){throw new IllegalStateException("Optimizer spell catalog unavailable",ex);}
        // Start with high damage spells when a large search reaches its time/score cap.
        spells.sort(Comparator.comparingInt((OptimizerSpell spell)->spell.maxHit).reversed().thenComparing(spell->spell.name));
        return Collections.unmodifiableList(spells);
    }
    private static Reader reader(String file){return new InputStreamReader(OptimizerSpell.class.getResourceAsStream("/com/dpscalc/"+file),StandardCharsets.UTF_8);}
    boolean eligible(int magic,MonsterStats target){
        if(level>magic)return false;
        if(name.endsWith("Demonbane"))return target.getAttributes().contains(MonsterAttribute.DEMON);
        return !name.equals("Crumble Undead")||target.getAttributes().contains(MonsterAttribute.UNDEAD);
    }
    boolean compatible(String weapon,int[] ids,IntFunction<String> names){
        if(book.equals("ancient"))return weapon.equals("Ancient staff")||weapon.contains("ancient sceptre")||weapon.equals("Ancient sceptre")
            ||weapon.equals("Master wand")||weapon.equals("Kodai wand")||weapon.equals("Blue moon spear")
            ||weapon.contains("nightmare staff")&&!weapon.startsWith("Harmonised")||weapon.equals("Nightmare staff")
            ||alternateSceptre(weapon)||weapon.startsWith("Ahrim's staff")&&names.apply(ids[0]).startsWith("Ahrim's hood")
                &&names.apply(ids[4]).startsWith("Ahrim's robetop")&&names.apply(ids[7]).startsWith("Ahrim's robeskirt")&&names.apply(ids[2]).startsWith("Amulet of the damned");
        if(book.equals("arceuus"))return weapon.startsWith("Skull sceptre")||weapon.startsWith("Slayer's staff")||weapon.startsWith("Ahrim's staff")
            ||weapon.equals("Blue moon spear")||weapon.equals("Staff of the dead")||weapon.equals("Toxic staff of the dead")
            ||weapon.equals("Purging staff")||weapon.equals("Master wand")||weapon.equals("Kodai wand");
        switch(name){
            case "Iban Blast":return weapon.startsWith("Iban's staff");
            case "Claws of Guthix":return weapon.equals("Staff of balance")||weapon.equals("Void knight mace");
            case "Saradomin Strike":return weapon.equals("Staff of light");
            case "Flames of Zamorak":return weapon.equals("Staff of the dead")||weapon.equals("Toxic staff of the dead")||alternateSceptre(weapon);
            case "Crumble Undead":return weapon.equals("Skull sceptre (i)")||weapon.startsWith("Slayer's staff")||weapon.equals("Void knight mace")
                ||weapon.equals("Staff of the dead")||weapon.equals("Toxic staff of the dead")||weapon.equals("Staff of light")||weapon.equals("Staff of balance");
            default:
                if(element==null||weapon.startsWith("Skull sceptre"))return false;
                return !(weapon.startsWith("Slayer's staff")||weapon.equals("Void knight mace"))||name.endsWith(" Wave")||name.endsWith(" Surge");
        }
    }
    private static boolean alternateSceptre(String weapon){return (weapon.startsWith("Thammaron's sceptre")||weapon.startsWith("Accursed sceptre"))&&weapon.endsWith("(a)");}
    void apply(PlayerState player){player.setSpellName(name);player.setSpellbook(book);player.setSpellElement(element);player.setSpellMaxHit(maxHit);}
    static void clear(PlayerState player){player.setSpellName(null);player.setSpellbook(null);player.setSpellElement(null);player.setSpellMaxHit(0);}
}
