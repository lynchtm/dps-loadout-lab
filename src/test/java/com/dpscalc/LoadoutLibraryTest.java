package com.dpscalc;

import com.dpscalc.scenario.*;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.state.PlayerState;
import org.junit.Test;
import static org.junit.Assert.*;

public class LoadoutLibraryTest {
    @Test public void draftsAndTemplatesAreIndependentAcrossSaveUseReplaceAndDelete() {
        LoadoutLibrary library=new LoadoutLibrary();Scenario.Loadout original=new Scenario.Loadout();
        original.player.setAttackLevel(75);library.saveNew("Melee",original);
        original.player.setAttackLevel(10);
        Scenario.Loadout first=library.createDraft("Melee");assertEquals(75,first.player.getAttackLevel());
        first.player.setAttackLevel(80);first.player.getEquippedItemIds()[3]=4151;
        Scenario.Loadout second=library.createDraft("Melee");assertEquals(75,second.player.getAttackLevel());assertEquals(-1,second.player.getEquippedItemIds()[3]);
        library.replace("Melee",first);first.player.setAttackLevel(90);
        assertEquals(80,LoadoutLibrary.parse(library.toJson()).createDraft("Melee").player.getAttackLevel());
        library.remove("Melee");assertTrue(library.names().isEmpty());assertEquals(75,second.player.getAttackLevel());
        assertTrue(LoadoutLibrary.parse(library.toJson()).names().isEmpty());
    }
    @Test public void savedTemplateGearAndStatsSurviveLiveRefresh() {
        LoadoutLibrary library=new LoadoutLibrary();Scenario.Loadout original=new Scenario.Loadout();
        original.player.setStrengthLevel(70);library.saveNew("Snapshot",original);
        Scenario.Loadout draft=library.createDraft("Snapshot");PlayerState live=Scenario.defaults();
        live.setStrengthLevel(99);live.getEquippedItemIds()[3]=4151;Scenario.mergeLive(draft,live);
        assertEquals(70,draft.player.getStrengthLevel());assertEquals(-1,draft.player.getEquippedItemIds()[3]);
    }
    @Test public void startersUseSelectedLevelsAndOnlyKnownEquipment() {
        EquipmentPreparationFacade equipment=new EquipmentPreparationFacade();PlayerState levels=Scenario.defaults();levels.setAttackLevel(80);
        for(int i=0;i<3;i++){
            Scenario.Loadout starter=LoadoutLibrary.starter(i,levels,equipment);Scenario.validate(starter);
            assertEquals(80,starter.player.getAttackLevel());assertTrue(starter.player.getWeaponId()>0);
            for(int id:starter.player.getEquippedItemIds())if(id>0)assertNotNull(equipment.getItemFacts(id));
        }
        assertEquals(-1,levels.getEquippedItemIds()[3]);
    }
    @Test public void duplicateSaveRequiresExplicitReplacement() {
        LoadoutLibrary library=new LoadoutLibrary();library.saveNew("Original",new Scenario.Loadout());
        try{library.saveNew(" Original ",new Scenario.Loadout());fail("Duplicate name should be rejected");}catch(IllegalArgumentException expected){}
        assertEquals(1,library.names().size());
    }
}
