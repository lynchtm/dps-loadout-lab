package com.dpscalc.scenario;

import com.dpscalc.equipment.*;
import com.dpscalc.state.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Weapon interface styles from the attributed Wiki reference, not the player's live weapon. */
public final class WeaponStyles {
    private static final Map<String,List<CombatStyle>> CATEGORIES=load();
    private static Map<String,List<CombatStyle>> load(){
        Map<String,List<CombatStyle>> result=new HashMap<>();
        try(Reader reader=new InputStreamReader(WeaponStyles.class.getResourceAsStream("/com/dpscalc/weapon-styles.json"),StandardCharsets.UTF_8)){
            for(Map.Entry<String,JsonElement> entry:new JsonParser().parse(reader).getAsJsonObject().entrySet()){
                List<CombatStyle> styles=new ArrayList<>();
                for(JsonElement element:entry.getValue().getAsJsonArray()){
                    JsonObject obj=element.getAsJsonObject();String stance=obj.get("stance").getAsString(),type=obj.get("type").getAsString();
                    AttackType attack=type.equals("ranged")?AttackType.RANGED_STANDARD:AttackType.valueOf(type.toUpperCase(Locale.ROOT));
                    boolean melee=attack==AttackType.STAB||attack==AttackType.SLASH||attack==AttackType.CRUSH;
                    int controlled=stance.equals("Controlled")?1:0;
                    styles.add(new CombatStyle(obj.get("name").getAsString(),attack,stance,
                        melee&&stance.equals("Accurate")?3:controlled,melee&&stance.equals("Aggressive")?3:controlled,
                        stance.equals("Defensive")||stance.equals("Defensive Autocast")?3:stance.equals("Longrange")?(type.equals("magic")?1:3):controlled,
                        type.equals("ranged")&&stance.equals("Accurate")?3:0,
                        type.equals("magic")?(stance.equals("Accurate")?3:stance.equals("Longrange")?1:0):0));
                }
                result.put(entry.getKey(),Collections.unmodifiableList(styles));
            }
        }catch(IOException ex){throw new IllegalStateException("Weapon style catalog unavailable",ex);}
        return Collections.unmodifiableMap(result);
    }
    public static List<CombatStyle> available(PlayerState player,EquipmentPreparationFacade equipment){
        if(player.getWeaponId()<=0)return CATEGORIES.get("Unarmed");
        try{EquipmentCatalogItem item=equipment.getItemFacts(player.getWeaponId());return item.getSpeed()<0?Collections.emptyList():CATEGORIES.getOrDefault(item.getCategory(),Collections.emptyList());}
        catch(IllegalArgumentException ex){return Collections.emptyList();}
    }
    public static void select(Scenario.Loadout loadout,CombatStyle style){
        loadout.player.setCombatStyle(style);
        for(String path:Scenario.flatten(Scenario.JSON.toJsonTree(loadout.player).getAsJsonObject()).keySet())if(path.startsWith("combatStyle."))loadout.overrides.add(path);
    }
    public static void normalize(Scenario.Loadout loadout,EquipmentPreparationFacade equipment){
        List<CombatStyle> options=available(loadout.player,equipment);if(options.isEmpty())return;
        CombatStyle current=loadout.player.getCombatStyle();
        for(CombatStyle option:options)if(option.getName().equals(current.getName())&&option.getAttackType()==current.getAttackType()&&option.getStance().equals(current.getStance())){loadout.player.setCombatStyle(option);return;}
        for(CombatStyle option:options)if(option.getAttackType()==current.getAttackType()&&option.getStance().equals(current.getStance())){loadout.player.setCombatStyle(option);return;}
        select(loadout,options.get(0));
    }
}
