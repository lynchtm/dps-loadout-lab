package com.dpscalc;

import static org.junit.Assert.*;

import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.scenario.*;
import com.dpscalc.state.*;

import net.runelite.api.*;

import org.junit.Test;

import java.util.*;

public class PlayerStateManagerTest {
    static final class Source implements PlayerStateManager.Source {
        int[] ids = {-1, -1, -1, 4151};
        Map<Integer, Integer> vars = new HashMap<>();

        public int realLevel(Skill skill) {
            return 90;
        }

        public int boostedLevel(Skill skill) {
            return skill == Skill.HITPOINTS ? 75 : 100;
        }

        public int varp(int id) {
            return vars.getOrDefault(id, 0);
        }

        public int varbit(int id) {
            return vars.getOrDefault(id, 0);
        }

        public boolean prayerActive(net.runelite.api.Prayer prayer) {
            return prayer == net.runelite.api.Prayer.PIETY;
        }

        public int[] equipmentIds() {
            return ids;
        }

        public String itemName(int id) {
            return "Abyssal whip";
        }
    }

    @Test
    public void capturePreservesLiveLevelsPrayersAndCopiesEquipment() {
        Source source = new Source();
        PlayerStateManager manager =
                new PlayerStateManager(source, new EquipmentPreparationFacade());
        PlayerState first = manager.getPlayerState();
        assertEquals(90, first.getAttackLevel());
        assertEquals(10, first.getAttackBoost());
        assertEquals(75, first.getCurrentHitpoints());
        assertEquals(
                Collections.singleton(com.dpscalc.state.Prayer.PIETY), first.getActivePrayers());
        assertEquals(4151, first.getWeaponId());
        assertEquals(-1, first.getEquippedItemIds()[13]);
        source.ids[3] = -1;
        PlayerState second = manager.getPlayerState();
        assertEquals(-1, second.getWeaponId());
        assertEquals(4151, first.getWeaponId());
    }

    @Test
    public void captureReadsSpellbookAndWeaponStyleWithoutInventingASpell() {
        Source source = new Source();
        source.vars.put(Varbits.EQUIPPED_WEAPON_TYPE, 3);
        source.vars.put(VarPlayer.ATTACK_STYLE, 1);
        source.vars.put(net.runelite.api.gameval.VarbitID.SPELLBOOK, 1);
        PlayerState state =
                new PlayerStateManager(source, new EquipmentPreparationFacade()).getPlayerState();
        assertEquals(CombatStyle.RANGED_RAPID, state.getCombatStyle());
        assertEquals("ancient", state.getSpellbook());
        assertNull(state.getSpellName());
    }
}
