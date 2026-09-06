package com.dpscalc;

import static org.junit.Assert.*;

import com.dpscalc.data.MonsterStats;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.scenario.*;
import com.dpscalc.state.*;

import org.junit.Test;

public class DpsCalcPluginTest {
    @Test
    public void liveAndDraftUseIdenticalPreparationAndDistributions() {
        PlayerState player = Scenario.defaults();
        player.getEquippedItemIds()[3] = 4151;
        player.setCombatStyle(CombatStyle.MELEE_ACCURATE_SLASH);
        player.setStrengthBoost(12);
        Scenario.Loadout live = CalculationInputs.live(player, true, false, true);
        Scenario.Loadout draft = live.copy();
        draft.name = "Comparison";
        MonsterStats target = new MonsterStats();
        target.setName("Test target");
        target.setId(-1);
        target.setSize(1);
        target.setSpeed(4);
        target.setHitpoints(100);
        target.setDefenceLevel(120);
        ScenarioCalculator calculator = new ScenarioCalculator(new EquipmentPreparationFacade());
        ScenarioCalculator.Result a = calculator.calculate(live, target),
                b = calculator.calculate(draft, target);
        assertNull(a.error);
        assertEquals(a.normal.getDps(), b.normal.getDps(), 0);
        assertEquals(a.ttk, b.ttk);
        assertArrayEquals(a.histogram, b.histogram, 0);
        assertEquals(a.warnings, b.warnings);
        assertEquals(0, player.getEquipmentStats().getSlashAttack());
        assertTrue(live.player.isOnSlayerTask());
        assertNotSame(player, live.player);
    }

    @Test
    public void comparisonOverlayCopiesTargetAndClearsExplicitly() {
        DpsCalcPlugin plugin =
                new DpsCalcPlugin() {
                    public DpsCalcConfig getConfig() {
                        return new DpsCalcConfig() {
                            public OverlaySource overlaySource() {
                                return OverlaySource.SELECTED_COMPARISON;
                            }
                        };
                    }
                };
        ScenarioCalculator.Result result = new ScenarioCalculator.Result();
        result.name = "Melee";
        MonsterStats target = new MonsterStats();
        target.setName("Before");
        plugin.publishComparison(result, target);
        target.setName("After");
        assertEquals("Before", plugin.getOverlaySnapshot().target.getName());
        assertEquals("Comparison: Melee", plugin.getOverlaySnapshot().source);
        plugin.clearComparison();
        assertNull(plugin.getOverlaySnapshot());
        plugin.publishComparison(result, target);
        plugin.onProfileChanged(null);
        assertNull(plugin.getOverlaySnapshot());
        plugin.publishComparison(result, target);
        plugin.onRuneScapeProfileChanged(null);
        assertNull(plugin.getOverlaySnapshot());
    }

    @Test
    public void defaultsUseLiveSourceAndSlayerAssumption() {
        DpsCalcConfig config = new DpsCalcConfig() {};
        assertEquals(DpsCalcConfig.OverlaySource.LIVE_PLAYER, config.overlaySource());
        assertTrue(config.onSlayerTask());
    }
}
