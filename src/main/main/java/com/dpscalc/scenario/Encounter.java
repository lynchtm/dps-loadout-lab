package com.dpscalc.scenario;

import com.dpscalc.state.PlayerState;
import java.util.Set;

/** Shared comparison assumptions, separate from an individual monster's combat stats. */
public final class Encounter {
    private static final Set<String> AREA_SPELLS=Set.of("Smoke Burst","Shadow Burst","Blood Burst","Ice Burst",
        "Smoke Barrage","Shadow Barrage","Blood Barrage","Ice Barrage");
    public boolean grouped;
    public int targets=5;
    public void validate(){if(targets<2||targets>9)throw new IllegalArgumentException("Grouped encounters require 2–9 targets");}
    public Encounter copy(){Encounter copy=new Encounter();copy.grouped=grouped;copy.targets=targets;return copy;}
    public int targetsHit(PlayerState player){
        return grouped&&player.getCombatStyle().getAttackType().isMagic()&&"ancient".equals(player.getSpellbook())
            &&AREA_SPELLS.contains(player.getSpellName()==null?"":player.getSpellName())?targets:1;
    }
    public String summary(){return grouped?"Grouped · "+targets+" targets":"Single target";}
    public String assumption(){return "Estimated group DPS assumes "+targets+" identical monsters kept in burst/barrage range. Gathering and looting time are excluded. Other attacks count once; kill time and hit distribution remain per monster.";}
}
