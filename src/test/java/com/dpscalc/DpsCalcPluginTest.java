package com.dpscalc;

import java.lang.reflect.Field;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.events.InteractingChanged;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Development test launcher for the DPS Calculator plugin.
 *
 * Run this class with assertions enabled (-ea) to test the plugin in RuneLite.
 *
 * Make sure to add the following VM options:
 * -ea
 */
public class DpsCalcPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(DpsCalcPlugin.class);
        RuneLite.main(args);
    }

    @Test
    public void selectedVersionResets_whenTargetNpcIdChanges() throws Exception {
        // Given
        DpsCalcPlugin plugin = new DpsCalcPlugin();
        Client client = mock(Client.class);
        Player localPlayer = mock(Player.class);
        NPC firstTarget = mock(NPC.class);
        NPC secondTarget = mock(NPC.class);
        when(client.getLocalPlayer()).thenReturn(localPlayer);
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        when(firstTarget.getId()).thenReturn(15742);
        when(secondTarget.getId()).thenReturn(202);
        setField(plugin, "client", client);
        setField(plugin, "selectedVersion", "Roaring");
        setField(plugin, "targetNpc", firstTarget);
        InteractingChanged event = mock(InteractingChanged.class);
        when(event.getSource()).thenReturn(localPlayer);
        when(event.getTarget()).thenReturn(secondTarget);

        // When
        plugin.onInteractingChanged(event);

        // Then
        assertEquals(secondTarget, plugin.getTargetNpc());
        assertNull(field(plugin, "selectedVersion"));
    }

    @Test
    public void selectMonsterVersionAcceptsOnlyCurrentNpcId() throws Exception {
        // Given
        DpsCalcPlugin plugin = new DpsCalcPlugin();
        NPC target = mock(NPC.class);
        ClientThread clientThread = mock(ClientThread.class);
        when(target.getId()).thenReturn(15742);
        setField(plugin, "targetNpc", target);
        setField(plugin, "clientThread", clientThread);

        // When
        boolean rejected = plugin.selectMonsterVersion(202, "Falador");
        boolean accepted = plugin.selectMonsterVersion(15742, "Roaring");

        // Then
        assertFalse(rejected);
        assertTrue(accepted);
        assertEquals("Roaring", field(plugin, "selectedVersion"));
        verify(clientThread).invokeLater(any(Runnable.class));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
