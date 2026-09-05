package com.dpscalc;

import com.dpscalc.data.MonsterDataManager;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.PlayerStateManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import javax.swing.JComboBox;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DpsCalcPanelVersionTest {
    @Test
    public void livePanelShowsVersionSelector_whenTargetHasDuplicateVersions() throws Exception {
        // Given
        Client client = mock(Client.class);
        DpsCalcPlugin plugin = mock(DpsCalcPlugin.class);
        DpsCalcConfig config = mock(DpsCalcConfig.class);
        NPC target = mock(NPC.class);
        MonsterDataManager manager = managerWith(maggotKingEntries());
        MonsterStats selected = manager.getMonster(15742, "Roaring");
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(plugin.getTargetNpc()).thenReturn(target);
        when(plugin.getCurrentMonsterStats()).thenReturn(selected);
        when(plugin.getSelectedVersion()).thenReturn("Roaring");
        when(target.getId()).thenReturn(15742);

        DpsCalcPanel panel = new DpsCalcPanel(client, plugin, config, mock(PlayerStateManager.class), manager);

        // When
        invokeUpdateLoop(panel);
        JComboBox<?> versionBox = firstComboBox(panel);

        // Then
        assertNotNull(versionBox);
        assertEquals(3, versionBox.getItemCount());
        assertEquals("Roaring", versionBox.getSelectedItem());
    }

    private static MonsterDataManager managerWith(String entries) throws Exception {
        MonsterDataManager manager = new MonsterDataManager();
        JsonArray monsters = (new JsonParser()).parse("[" + entries + "]").getAsJsonArray();
        Method parse = MonsterDataManager.class.getDeclaredMethod("parseAndStoreMonsters", JsonArray.class);
        parse.setAccessible(true);
        parse.invoke(manager, monsters);

        java.lang.reflect.Field loaded = MonsterDataManager.class.getDeclaredField("loaded");
        loaded.setAccessible(true);
        loaded.set(manager, true);
        return manager;
    }

    private static String maggotKingEntries() {
        return monsterJson(15742, "Maggot King", "Far", 200) + "," +
            monsterJson(15742, "Maggot King", "Nearby", 200) + "," +
            monsterJson(15742, "Maggot King", "Roaring", 100);
    }

    private static String monsterJson(int id, String name, String version, int defence) {
        return String.format(
            "{\"id\":%d,\"name\":\"%s\",\"version\":\"%s\",\"skills\":{\"def\":%d},\"defensive\":{}}",
            id,
            name,
            version,
            defence
        );
    }

    private static void invokeUpdateLoop(DpsCalcPanel panel) throws Exception {
        Method updateLoop = DpsCalcPanel.class.getDeclaredMethod("updateLoop");
        updateLoop.setAccessible(true);
        updateLoop.invoke(panel);
    }

    private static JComboBox<?> firstComboBox(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComboBox) {
                return (JComboBox<?>) component;
            }
            if (component instanceof Container) {
                JComboBox<?> child = firstComboBox((Container) component);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }
}
