package com.dpscalc;

import com.dpscalc.scenario.*;
import com.loadoutlab.model.*;
import com.loadoutlab.model.*;
import com.loadoutlab.data.*;
import com.loadoutlab.calculation.*;
import com.loadoutlab.engine.DamagePmf;
import com.loadoutlab.equipment.EquipmentPreparationFacade;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ScenarioTest {
    @Test public void scenarioRoundTripPreservesOverridesAndPresets(){
        Scenario s=new Scenario();s.loadouts.get(0).overrides.add("attackLevel");s.loadouts.get(0).player.setAttackLevel(75);
        s.targets.put("custom",target());s.target=target();
        Scenario restored=Scenario.parse(Scenario.JSON.toJson(s));assertEquals(75,restored.loadouts.get(0).player.getAttackLevel());assertTrue(restored.loadouts.get(0).overrides.contains("attackLevel"));assertEquals(100,restored.targets.get("custom").getHitpoints());
    }
    @Test public void copiedLoadoutsDoNotShareMutableState(){
        Scenario.Loadout a=new Scenario.Loadout(),b=a.copy();b.player.getEquippedItemIds()[3]=123;b.player.getActivePrayers().add(Prayer.PIETY);b.overrides.add("attackLevel");
        assertEquals(-1,a.player.getWeaponId());assertTrue(a.player.getActivePrayers().isEmpty());assertTrue(a.overrides.isEmpty());
    }
    @Test public void liveMergePreservesOnlyExplicitOverrides(){
        Scenario.Loadout l=new Scenario.Loadout();l.player.setAttackLevel(50);l.overrides.add("attackLevel");l.player.setOnSlayerTask(true);
        PlayerState live=Scenario.defaults();live.setAttackLevel(90);live.setStrengthLevel(80);Scenario.mergeLive(l,live);
        assertEquals(50,l.player.getAttackLevel());assertEquals(80,l.player.getStrengthLevel());assertTrue(l.player.isOnSlayerTask());assertEquals("Live client",l.sources.get("strengthLevel"));
        l.overrides.clear();Scenario.mergeLive(l,live);assertEquals(90,l.player.getAttackLevel());
    }
    @Test public void simpleKillMomentsIncludeMissesAndOverkill(){
        double[] moments=DistributionAnalysis.moments(new double[]{.5,0,.5},3);assertEquals(4,moments[0],1e-10);assertEquals(4,moments[1],1e-10);
        assertEquals(1,DistributionAnalysis.moments(new double[]{0,0,1},1)[0],0);
    }
    @Test public void compactMultiHitHistogramPreservesMassAndExpectation(){
        DamagePmf h=DamagePmf.singleHit(.75,0,20);DamagePmf attack=h.independentSum(h).independentSum(h);
        double[] histogram=DistributionAnalysis.histogram(attack);assertEquals(1,Arrays.stream(histogram).sum(),1e-10);
        double mean=0;for(int i=0;i<histogram.length;i++)mean+=i*histogram[i];assertEquals(attack.mean(),mean,1e-9);
    }
    @Test public void killGraphReportsSurvivingTail(){double[] p=DistributionAnalysis.killDistribution(new double[]{.5,.5},1,3);assertArrayEquals(new double[]{.5,.25,.125,.125},p,1e-10);}
    @Test(expected=IllegalArgumentException.class) public void unkillableTargetHasNoMisleadingZeroTime(){DistributionAnalysis.moments(new double[]{1,0},100);}
    @Test(expected=IllegalArgumentException.class) public void exactAnalysisHasWorkBound(){DistributionAnalysis.moments(new double[1000],100000);}
    @Test(expected=IllegalArgumentException.class) public void unsupportedSchemaIsRejected(){Scenario.parse("{\"schemaVersion\":2}");}
    @Test(expected=IllegalArgumentException.class) public void deepInputIsRejectedBeforeParsing(){Scenario.parse("[".repeat(40)+"]".repeat(40));}
    @Test(expected=IllegalArgumentException.class) public void nullPlayerRejected(){Scenario.parse("{\"schemaVersion\":1,\"loadouts\":[{\"player\":null}]}");}
    @Test(expected=IllegalArgumentException.class) public void wrongEquipmentLengthRejected(){Scenario s=new Scenario();s.loadouts.get(0).player.setEquippedItemIds(new int[1]);Scenario.parse(Scenario.JSON.toJson(s));}
    @Test(expected=IllegalArgumentException.class) public void incompatiblePrayersRejected(){Scenario.Loadout l=new Scenario.Loadout();l.player.setActivePrayers(EnumSet.of(Prayer.PIETY,Prayer.CHIVALRY));Scenario.validate(l);}
    @Test public void boostPresetsMatchWikiIntegerRounding(){
        PlayerState p=Scenario.defaults();ScenarioCalculator.applyBoost(p,"Super combat",0,false);assertEquals(19,p.getAttackBoost());
        ScenarioCalculator.applyBoost(p,"Saturated heart",120,false);assertEquals(13,p.getMagicBoost());
        ScenarioCalculator.applyBoost(p,"Forgotten brew",0,false);assertEquals(10,p.getMagicBoost());assertEquals(-12,p.getAttackBoost());
        ScenarioCalculator.applyBoost(p,"Overload",0,false);assertEquals(17,p.getAttackBoost());
        ScenarioCalculator.applyBoost(p,"Overload (+)",0,false);assertEquals(21,p.getMagicBoost());
        ScenarioCalculator.applyBoost(p,"Smelling salts",0,false);assertEquals(26,p.getRangedBoost());
    }
    @Test public void decayAndDivineDoNotMutateBaseLevels(){PlayerState p=Scenario.defaults();ScenarioCalculator.applyBoost(p,"Super combat",120,false);assertEquals(17,p.getAttackBoost());assertEquals(99,p.getAttackLevel());ScenarioCalculator.applyBoost(p,"Super combat",120,true);assertEquals(19,p.getAttackBoost());}
    @Test public void resultAverageUsesDistribution(){DamagePmf d=DamagePmf.singleHit(.75,0,10);DpsResult r=new DpsResult(d,.75,4,100,100,0,List.of());assertEquals(d.mean(),r.getExpectedDamage(),0);assertEquals(d.mean()/4,r.getExpectedDamage()/r.getExpectedAttackSpeed(),0);}

    @Test public void realScenarioCalculationIsFiniteAndDoesNotMutateInput(){Scenario.Loadout l=new Scenario.Loadout();ScenarioCalculator.Result r=new ScenarioCalculator(new EquipmentPreparationFacade()).calculate(l,target());assertNull(r.error);assertTrue(r.normal.getDps()>0);assertNotNull(r.ttk);assertEquals(0,l.player.getEquipmentStats().getMeleeStrength());}
    @Test public void missingItemFailsWithExplanation(){Scenario.Loadout l=new Scenario.Loadout();l.player.getEquippedItemIds()[3]=999999;ScenarioCalculator.Result r=new ScenarioCalculator(new EquipmentPreparationFacade()).calculate(l,target());assertNotNull(r.error);}
    private static MonsterStats target(){MonsterStats m=new MonsterStats();m.setId(-1);m.setName("Target");m.setSize(1);m.setSpeed(4);m.setHitpoints(100);m.setDefenceLevel(1);m.setMagicLevel(1);return m;}
}
