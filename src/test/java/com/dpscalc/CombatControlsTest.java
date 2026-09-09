package com.dpscalc;

import com.dpscalc.scenario.*;
import com.loadoutlab.equipment.EquipmentPreparationFacade;
import com.loadoutlab.model.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CombatControlsTest {
    @Test public void weaponChangesRestrictAndNormalizeStyles(){
        EquipmentPreparationFacade equipment=new EquipmentPreparationFacade();Scenario.Loadout draft=new Scenario.Loadout();
        draft.player.getEquippedItemIds()[3]=4151;WeaponStyles.normalize(draft,equipment);
        List<CombatStyle> whip=WeaponStyles.available(draft.player,equipment);assertEquals(3,whip.size());
        assertTrue(whip.stream().allMatch(s->s.getAttackType()==AttackType.SLASH));
        assertFalse(whip.stream().anyMatch(s->s.getStance().equals("Aggressive")));
        WeaponStyles.select(draft,whip.get(1));assertEquals("Controlled",draft.player.getCombatStyle().getStance());
        draft.player.getEquippedItemIds()[3]=20997;WeaponStyles.normalize(draft,equipment);
        assertEquals(3,WeaponStyles.available(draft.player,equipment).size());assertTrue(draft.player.getCombatStyle().getAttackType().isRanged());
        draft.player.getEquippedItemIds()[3]=11905;WeaponStyles.normalize(draft,equipment);assertTrue(draft.player.getCombatStyle().getAttackType().isMagic());
        assertFalse(WeaponStyles.available(draft.player,equipment).stream().anyMatch(s->s.getStance().equals("Aggressive")));
        draft.player.getEquippedItemIds()[3]=-1;WeaponStyles.normalize(draft,equipment);assertEquals("Punch",draft.player.getCombatStyle().getName());
    }
    @Test public void potionSelectionsCombineByStatWithoutStackingAndIgnoreOrder(){
        Scenario.Loadout loadout=new Scenario.Loadout();loadout.potions.addAll(Arrays.asList("Attack","Super attack","Super strength","Ranging"));
        PlayerState player=Scenario.defaults();PotionSelection.apply(player,loadout);
        assertEquals(19,player.getAttackBoost());assertEquals(19,player.getStrengthBoost());assertEquals(13,player.getRangedBoost());
        List<String> reverse=new ArrayList<>(loadout.potions);Collections.reverse(reverse);loadout.potions=new LinkedHashSet<>(reverse);
        PlayerState other=Scenario.defaults();PotionSelection.apply(other,loadout);assertEquals(Scenario.JSON.toJson(player),Scenario.JSON.toJson(other));
        PotionSelection.toggle(loadout,"Super attack");PotionSelection.apply(other,loadout);assertEquals(12,other.getAttackBoost());
        loadout.elapsedSeconds=120;PotionSelection.apply(other,loadout);assertEquals(10,other.getAttackBoost());
    }
    @Test public void legacyBoostMigratesWhenMultiSelectionIsEdited(){
        Scenario scenario=new Scenario();Scenario.Loadout l=scenario.loadouts.get(0);l.boost="Super combat";
        Scenario restored=Scenario.parse(Scenario.JSON.toJson(scenario));l=restored.loadouts.get(0);
        assertEquals(Collections.singleton("Super combat"),PotionSelection.selected(l));
        PotionSelection.toggle(l,"Ranging");assertEquals("None",l.boost);assertEquals(2,l.potions.size());
        Scenario.Loadout copy=l.copy();copy.potions.clear();assertEquals(2,l.potions.size());
        assertEquals(l.potions,Scenario.parse(Scenario.JSON.toJson(restored)).loadouts.get(0).potions);
    }
    @Test public void slayerDefaultDoesNotOverrideExplicitSavedChoice(){
        Scenario s=new Scenario();assertTrue(s.loadouts.get(0).player.isOnSlayerTask());
        s.loadouts.get(0).player.setOnSlayerTask(false);
        assertFalse(Scenario.parse(Scenario.JSON.toJson(s)).loadouts.get(0).player.isOnSlayerTask());
    }
}
