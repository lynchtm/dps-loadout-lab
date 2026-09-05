package com.dpscalc;

import com.dpscalc.data.*;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.scenario.*;
import com.dpscalc.state.CombatStyle;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class EncounterTest {
    private final EquipmentPreparationFacade equipment=new EquipmentPreparationFacade();
    private MonsterStats target(){MonsterStats m=new MonsterStats();m.setName("Greater Nechryael");m.setId(-1);m.setSize(2);m.setSpeed(4);m.setHitpoints(205);m.setMagicLevel(1);m.setDefenceLevel(100);return m;}
    private Encounter group(){Encounter encounter=new Encounter();encounter.grouped=true;return encounter;}
    @Test public void groupedEncounterChangesWinningWeaponAndRetainsSingleTargetResults(){
        Scenario.Loadout base=new Scenario.Loadout();base.player.getEquippedItemIds()[3]=11905;base.player.setCombatStyle(CombatStyle.MAGIC_ACCURATE);
        OwnedEquipment bank=new OwnedEquipment("player",1,1,Map.of(11905,1,4675,1),Map.of(),Map.of(),Map.of("attack",99,"magic",99));
        BankOptimizer optimizer=new BankOptimizer(equipment);Encounter grouped=group();
        BankOptimizer.Result single=optimizer.search(new BankOptimizer.Request(base,target(),bank,Set.of(),false,"Magic"),n->{});
        assertEquals(11905,single.alternatives.get(0).player.getWeaponId());assertNull(single.calculations.get(0).groupDps);
        BankOptimizer.Request request=new BankOptimizer.Request(base,target(),bank,Set.of(),false,"Magic",grouped);grouped.targets=9;
        BankOptimizer.Result multi=optimizer.search(request,n->{});Scenario.Loadout winner=multi.alternatives.get(0);ScenarioCalculator.Result result=multi.calculations.get(0);
        assertEquals(4675,winner.player.getWeaponId());assertEquals("Ice Barrage",winner.player.getSpellName());assertEquals(5,result.targetsHit);assertEquals(5,result.encounterTargets);
        assertEquals(result.normal.getDps()*5,result.groupDps,1e-12);assertTrue(result.groupDps>single.calculations.get(0).normal.getDps());
        ScenarioCalculator.Result one=new ScenarioCalculator(equipment).calculate(winner,target());
        assertEquals(one.ttk,result.ttk);assertArrayEquals(one.histogram,result.histogram,1e-12);assertEquals(one.normal.getMaxHit(),result.normal.getMaxHit());
        assertEquals(multi.baselineDps,multi.baselineGroupDps);assertEquals(11905,base.player.getWeaponId());
    }
    @Test public void groupMultiplierOnlyAppliesToSupportedAreaSpells(){
        Encounter encounter=group();Scenario.Loadout loadout=new Scenario.Loadout();loadout.player.setCombatStyle(CombatStyle.MAGIC_AUTOCAST);loadout.player.setSpellbook("ancient");
        for(String spell:new String[]{"Smoke Burst","Shadow Burst","Blood Burst","Ice Burst","Smoke Barrage","Shadow Barrage","Blood Barrage","Ice Barrage"}){loadout.player.setSpellName(spell);assertEquals(5,encounter.targetsHit(loadout.player));}
        loadout.player.setSpellName("Ice Blitz");assertEquals(1,encounter.targetsHit(loadout.player));
        loadout.player.setSpellName(null);assertEquals(1,encounter.targetsHit(loadout.player));
        loadout.player.setSpellName("Ice Barrage");loadout.player.setCombatStyle(CombatStyle.MELEE_ACCURATE_STAB);assertEquals(1,encounter.targetsHit(loadout.player));
        loadout.player.setCombatStyle(CombatStyle.MAGIC_AUTOCAST);encounter.grouped=false;assertEquals(1,encounter.targetsHit(loadout.player));
    }
    @Test public void encounterPersistsAndOlderScenariosDefaultToSingleTarget(){
        Scenario scenario=new Scenario();scenario.encounter=group();scenario.encounter.targets=7;
        Scenario restored=Scenario.parse(Scenario.JSON.toJson(scenario));assertTrue(restored.encounter.grouped);assertEquals(7,restored.encounter.targets);
        com.google.gson.JsonObject old=Scenario.JSON.toJsonTree(scenario).getAsJsonObject();old.remove("encounter");assertFalse(Scenario.parse(old.toString()).encounter.grouped);
        for(int count:new int[]{0,1,10}){scenario.encounter.targets=count;try{Scenario.parse(Scenario.JSON.toJson(scenario));fail();}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("2–9"));}}
    }
}
