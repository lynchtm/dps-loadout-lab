package com.dpscalc.scenario;

import com.loadoutlab.equipment.EquipmentPreparationFacade;
import com.dpscalc.wikisetups.items.FakeItemNameIndex;
import com.dpscalc.wikisetups.wiki.*;
import com.loadoutlab.model.MonsterStats;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class SetupFeaturesTest {
    private final EquipmentPreparationFacade equipment=new EquipmentPreparationFacade();
    private OwnedEquipment owned(Map<Integer,Integer> items){return new OwnedEquipment("player",1,1,items,Map.of(),Map.of(),Map.of("attack",99,"strength",99,"defence",99,"ranged",99,"magic",99,"prayer",99));}
    @Test public void cosmeticRequirementsFailClosedAndKeepCanonicalLevels(){
        Map<Integer,Map<String,Integer>> requirements=Map.of(1127,Map.of("defence",40));
        assertEquals(requirements.get(1127),VariantRequirements.resolve(2615,"Rune platebody (g)",equipment,requirements));
        assertNull(VariantRequirements.resolve(2615,"Rune platebody (i)",equipment,requirements));assertNull(VariantRequirements.resolve(2615,null,equipment,requirements));
        assertFalse(VariantRequirements.cosmetic("Black mask (i)","Black mask"));assertFalse(VariantRequirements.cosmetic("Trident of the seas (uncharged)","Trident of the seas"));
        assertTrue(VariantRequirements.cosmetic("Echo virtus mask","Virtus mask"));
    }
    @Test public void optimizerAcceptsDecoratedArmorButNeverBypassesItsLevel(){
        MonsterStats target=new MonsterStats();target.setId(-1);target.setName("Test");target.setHitpoints(100);target.setSize(1);target.setSpeed(4);target.setDefenceLevel(50);target.setMagicLevel(1);
        for(int defence:new int[]{1,99}){
            OwnedEquipment bank=new OwnedEquipment("player",1,1,Map.of(2615,1,4151,1),Map.of(),Map.of(),Map.of("attack",99,"strength",99,"defence",defence),Map.of(2615,"Rune platebody (g)",4151,"Abyssal whip"));
            BankOptimizer.Result result=new BankOptimizer(equipment).search(new BankOptimizer.Request(new Scenario.Loadout(),target,bank,Set.of(),false),n->{});
            assertEquals(defence==99?2615:-1,result.alternatives.get(0).player.getEquippedItemIds()[4]);
            if(defence==99)assertEquals("Rune platebody (g)",result.alternatives.get(0).player.getEquippedItemNames()[4]);
        }
    }
    @Test public void quantitiesSurviveParserPouchCompactionAndNotedRestock(){
        InventorySetup inventory=new InventoryParser().parse("{{Inventory|1=Shark|2=Shark\\200;n|3=Rune pouch}}{{Rune pouch|1=Blood rune\\16,000|3=Death rune\\500}}").get(0);
        assertEquals(200,inventory.quantity(1,false));assertTrue(inventory.noted(1));assertEquals(0,inventory.quantity(0,false));
        assertEquals(List.of("Blood rune","Death rune"),inventory.getRunes());assertEquals(16000,inventory.quantity(0,true));assertEquals(500,inventory.quantity(1,true));
        InventorySetup huge=new InventoryParser().parse("{{Inventory|1=Coins\\99999999999999999999}}").get(0);assertEquals(0,huge.quantity(0,false));
    }
    @Test public void supplyAuditCountsRepeatedFoodAndWornCopiesAndDoesNotSpendOptionalSwitches(){
        Scenario.Loadout draft=new Scenario.Loadout();draft.player.getEquippedItemIds()[3]=4151;draft.carry.slots[0]=new CarryPlan.Entry(4151,"Abyssal whip",1);draft.carry.slots[1]=new CarryPlan.Entry(385,"Shark",1);draft.carry.slots[2]=new CarryPlan.Entry(385,"Shark",1);draft.carry.switches.add(new CarryPlan.Entry(4151,"Abyssal whip",1));
        List<String> notes=draft.carry.audit(draft,owned(Map.of(4151,1,385,1)),"player");assertTrue(notes.stream().anyMatch(s->s.contains("Abyssal whip: need 2, own 1")));assertTrue(notes.stream().anyMatch(s->s.contains("Shark: need 2, own 1")));
        assertFalse(notes.stream().anyMatch(s->s.contains("need 3")));assertTrue(draft.carry.audit(draft,owned(Map.of()),"other").get(0).contains("unknown"));
    }
    @Test public void pouchCapacityUnknownQuantitiesAndUnrecognizedItemsAreVisible(){
        Scenario.Loadout draft=new Scenario.Loadout();draft.carry.slots[0]=new CarryPlan.Entry(12791,"Rune pouch",1);draft.carry.slots[1]=new CarryPlan.Entry(0,"Cheap food",0);
        for(int i=0;i<4;i++)draft.carry.runes[i]=new CarryPlan.Entry(554+i,"Rune "+i,0);
        List<String> notes=draft.carry.audit(draft,owned(Map.of(12791,1,554,100,555,100,556,100,557,100)),"player");
        assertTrue(notes.stream().anyMatch(s->s.contains("only 3")));assertTrue(notes.stream().anyMatch(s->s.contains("quantity")));assertTrue(notes.stream().anyMatch(s->s.contains("Cheap food")));
        draft.carry.slots[0].name="Divine rune pouch";assertFalse(draft.carry.audit(draft,owned(Map.of()),"player").stream().anyMatch(s->s.contains("only 3")));
    }
    @Test public void copiedAndSavedPlansNeverAliasAndLegacyScenariosStillLoad(){
        Scenario scenario=new Scenario();Scenario.Loadout draft=scenario.loadouts.get(0);draft.carry.slots[0]=new CarryPlan.Entry(385,"Shark",1);draft.wikiSeeds.add(new int[14]);draft.wikiSource="https://oldschool.runescape.wiki/w/Test?oldid=123";draft.wikiRevision="123";
        LoadoutLibrary library=new LoadoutLibrary();library.saveNew("Example",draft);Scenario.Loadout copy=library.createDraft("Example");copy.carry.slots[0].quantity=2;copy.wikiSeeds.get(0)[3]=4151;assertEquals(1,library.createDraft("Example").carry.slots[0].quantity);assertEquals(0,draft.wikiSeeds.get(0)[3]);
        Scenario restored=Scenario.parse(Scenario.JSON.toJson(scenario));assertEquals("123",restored.loadouts.get(0).wikiRevision);assertEquals(385,restored.loadouts.get(0).carry.slots[0].id);
        com.google.gson.JsonObject legacy=Scenario.JSON.toJsonTree(scenario).getAsJsonObject();com.google.gson.JsonObject old=legacy.getAsJsonArray("loadouts").get(0).getAsJsonObject();for(String field:List.of("carry","wikiSeeds","wikiSource","wikiRevision","wikiNotes"))old.remove(field);assertEquals(28,Scenario.parse(legacy.toString()).loadouts.get(0).carry.slots.length);
    }
    @Test public void importRetainsQuantitiesSourceAndIndependentPlayerSettings(){
        String text="==Melee==\n{{Recommended equipment|style=Melee|weapon1={{plink|Abyssal whip}}|body1={{plink|Rune platebody}}|shield1={{plink|Dragon defender}}}}\n{{Inventory|1=Shark|2=Shark|3=Rune pouch}}{{Rune pouch|1=Blood rune\\16000}}";
        List<WikiLoadouts.Choice> choices=WikiLoadouts.parse(text);assertEquals(1,choices.size());
        FakeItemNameIndex index=FakeItemNameIndex.of(4151,"Abyssal whip",1127,"Rune platebody",12954,"Dragon defender",385,"Shark",12791,"Rune pouch",565,"Blood rune");
        Scenario.Loadout base=new Scenario.Loadout();base.player.setAttackLevel(81);WikiSetupService.Page page=new WikiSetupService.Page("Test/Strategies","123",text,1,false);
        Scenario.Loadout draft=WikiLoadouts.draft(choices.get(0),page,base,owned(Map.of(4151,1,1127,1,12954,1,385,1,12791,1,565,15000)),"player",true,index,id->id==565,equipment);
        assertEquals(4151,draft.player.getWeaponId());assertEquals(81,draft.player.getAttackLevel());assertEquals(-1,base.player.getWeaponId());assertEquals(1,draft.carry.slots[0].quantity);assertEquals(16000,draft.carry.runes[0].quantity);assertEquals("123",draft.wikiRevision);assertTrue(draft.wikiSource.contains("oldid=123"));assertEquals(4151,draft.wikiSeeds.get(0)[3]);
        Scenario.mergeLive(draft,Scenario.defaults());assertEquals(4151,draft.player.getWeaponId());
    }
    @Test public void allRealPageFixturesProduceBoundedValidChoices()throws Exception{
        for(String name:List.of("nechryael-slayer-task","zulrah-strategies","vorkath-strategies","tob-setups-guide","toa-strategies","theatre-of-blood-strategies","phantom-muspah-strategies","general-graardor-strategies","cox-strategies","alchemical-hydra-strategies")){
            String text;try(InputStream stream=getClass().getResourceAsStream("/fixtures/"+name+".wikitext")){assertNotNull(name,stream);text=new String(stream.readAllBytes(),StandardCharsets.UTF_8);}
            List<WikiLoadouts.Choice> choices=WikiLoadouts.parse(text);assertFalse(name,choices.isEmpty());assertTrue(name,choices.size()<=100);for(WikiLoadouts.Choice choice:choices){assertNotNull(choice.gear);if(choice.inventory!=null){assertTrue(choice.inventory.getSlots().size()<=28);assertTrue(choice.inventory.getRunes().size()<=4);}}
        }
    }
    @Test public void bankLayoutPreservesInventoryPositionsAndCopiesWithoutPretendingQuantities(){
        Scenario.Loadout draft=new Scenario.Loadout();draft.player.getEquippedItemIds()[3]=4151;draft.carry.slots[0]=new CarryPlan.Entry(4151,"Whip",1);draft.carry.slots[1]=new CarryPlan.Entry(385,"Shark",1);draft.carry.slots[27]=new CarryPlan.Entry(385,"Shark",1);draft.carry.runes[3]=new CarryPlan.Entry(565,"Blood rune",16000);draft.carry.switches.add(new CarryPlan.Entry(1215,"Dragon dagger",1));
        Map<Integer,Integer> p=BankLayoutPlan.from(draft).positions;assertEquals(Integer.valueOf(4151),p.get(16));assertEquals(Integer.valueOf(4151),p.get(4));assertEquals(Integer.valueOf(385),p.get(55));assertEquals(Integer.valueOf(565),p.get(43));assertEquals(Integer.valueOf(1215),p.get(64));assertEquals(6,p.size());
    }
    @Test public void bankWritesRefuseDisabledChangedProfileAndNameCollisionsWithoutMutation(){
        for(int mode=0;mode<3;mode++){FakeBank bank=new FakeBank();bank.enabled=mode!=0;bank.profile=mode!=1;bank.exists=mode==2;try{BankLayoutWriter.create(bank,"dps test",plan());fail();}catch(IllegalStateException expected){}assertTrue(bank.calls.isEmpty());}
    }
    @Test public void failedBankWriteRollsBackAndSuccessfulWriteTagsUniqueItems(){
        FakeBank bank=new FakeBank();bank.fail=true;try{BankLayoutWriter.create(bank,"dps test",plan());fail();}catch(IllegalStateException expected){}assertEquals("rollback",bank.calls.get(bank.calls.size()-1));
        bank=new FakeBank();BankLayoutWriter.create(bank,"dps test",plan());assertEquals(List.of("tag:385","layout","tab","save"),bank.calls);
    }
    @Test public void bankNamesAreNormalizedAndRejectControlCharacters(){assertEquals("dps tom's melee",BankLayoutPlan.tagName(" DPS Tom's melee "));for(String name:List.of("", "a,b","<html>","bad\nname","x".repeat(61)))try{BankLayoutPlan.tagName(name);fail(name);}catch(IllegalArgumentException expected){}}
    private BankLayoutPlan plan(){Scenario.Loadout draft=new Scenario.Loadout();draft.carry.slots[0]=new CarryPlan.Entry(385,"Shark",1);draft.carry.slots[1]=new CarryPlan.Entry(385,"Shark",1);return BankLayoutPlan.from(draft);}
    private static final class FakeBank implements BankLayoutWriter.Backend {
        boolean enabled=true,profile=true,exists,fail;List<String> calls=new ArrayList<>();public boolean enabled(){return enabled;}public boolean currentProfile(){return profile;}public boolean exists(String name){return exists;}
        public void tag(int id,String name){calls.add("tag:"+id);}public void layout(String name,BankLayoutPlan plan){calls.add("layout");if(fail)throw new IllegalStateException("storage failed");}public void tab(String name,int icon){calls.add("tab");}public void save(){calls.add("save");}public void removeNew(String name){calls.add("rollback");}
    }
}
