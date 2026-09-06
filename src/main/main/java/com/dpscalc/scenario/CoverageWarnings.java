package com.dpscalc.scenario;

import com.dpscalc.state.PlayerState;
import com.dpscalc.data.MonsterStats;
import java.util.*;

/** Known upstream drift is attached to affected scenarios rather than hidden in release notes. */
public final class CoverageWarnings {
    private CoverageWarnings(){}
    public static List<String> forState(PlayerState player,MonsterStats monster){
        List<String> warnings=new ArrayList<>();
        if(player.isWearingItemContaining("Inquisitor"))warnings.add("Inquisitor armour: the current Wiki changed per-piece weighting and mace interactions; this pinned engine uses the older rules.");
        if(player.isWearingAny("Sanguinesti staff","Holy sanguinesti staff"))warnings.add("Sanguinesti: current Wiki base damage and additional damage proc differ from this pinned engine.");
        if(player.isWearing("Dawnbringer"))warnings.add("Dawnbringer: current Wiki damage rules differ; special distribution is not implemented completely.");
        if(player.isWearing("Soulreaper axe"))warnings.add("Soulreaper: current Wiki special accuracy and minimum-hit rules differ from this pinned engine.");
        if(player.isWearingAny("Silverlight","Darklight"))warnings.add("Silverlight/Darklight: current Wiki added demonbane accuracy not present in this reference.");
        if(player.isWearing("Twisted bow"))warnings.add("Twisted bow: current Wiki clamps the scaling multiplier; this pinned engine predates that change.");
        if(player.isWearingAny("Rosewood blowpipe","Tonalztics of ralos"))warnings.add("Weapon special rules changed since the pinned reference.");
        String name=Objects.toString(monster.getName(),"").toLowerCase(Locale.ROOT);
        if(name.contains("maggot")||name.contains("mad angel")||name.contains("ara xyte")||name.contains("araxyte")||Objects.toString(monster.getVersion(),"").contains("Glyphic"))
            warnings.add("This NPC has newer phase or guaranteed-hit mechanics not represented by the pinned engine.");
        return warnings;
    }
}
