package com.dpscalc;

import com.dpscalc.equipment.EquipmentCalculator;
import com.dpscalc.equipment.EquipmentCombatStyle;
import com.dpscalc.equipment.EquipmentContext;
import com.dpscalc.equipment.EquipmentDomainCatalog;
import com.dpscalc.equipment.EquipmentItem;
import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.equipment.EquipmentResult;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.PlayerStateManager;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemEquipmentStats;
import net.runelite.http.api.item.ItemStats;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerStateManagerTest {
    private Client client;
    private ItemManager itemManager;
    private PlayerStateManager manager;

    @Before
    public void setUp() throws Exception {
        client = mock(Client.class);
        itemManager = mock(ItemManager.class);
        manager = new PlayerStateManager();
        setField("client", client);
        setField("itemManager", itemManager);
        setField("equipmentPreparation", new EquipmentPreparationFacade());
        when(client.getRealSkillLevel(any(Skill.class))).thenReturn(99);
        when(client.getBoostedSkillLevel(any(Skill.class))).thenReturn(99);
    }

    @Test
    public void liveReadPreservesRuneLiteEquipmentArraysAndVisibleTotals() {
        // Given
        ItemContainer equipment = mock(ItemContainer.class);
        Item weapon = item(25867);
        Item ammo = item(11212);
        Item[] items = emptyItems();
        items[EquipmentSlot.WEAPON.getIndex()] = weapon;
        items[EquipmentSlot.AMMO.getIndex()] = ammo;
        when(equipment.getItems()).thenReturn(items);
        when(client.getItemContainer(InventoryID.EQUIPMENT)).thenReturn(equipment);
        runeLiteItem(25867, "Bow of faerdhinen", 128, 106, 4);
        runeLiteItem(11212, "Dragon arrow", 0, 60, 0);

        // When
        PlayerState state = manager.getPlayerState();

        // Then
        assertEquals(128, state.getEquipmentStats().getRangedAttack());
        assertEquals(166, state.getEquipmentStats().getRangedStrength());
        assertEquals(4, state.getWeaponSpeed());
        assertEquals(25867, state.getWeaponId());
        assertEquals("Bow of faerdhinen", state.getWeaponName());
    }

    @Test
    public void snapshotIdCalculationPreservesVisibleRuneLiteTotals() {
        // Given
        int[] itemIds = emptyIds();
        itemIds[EquipmentSlot.WEAPON.getIndex()] = 4151;
        runeLiteItem(4151, "Abyssal whip", 0, 82, 4);

        // When
        PlayerState state = new PlayerState();
        state.setEquippedItemIds(itemIds);
        state.setEquippedItemNames(new String[14]);
        state.setCombatStyle(com.dpscalc.state.CombatStyle.MELEE_ACCURATE_SLASH);
        manager.prepareEquipment(state, -1);

        // Then
        assertEquals(82, state.getEquipmentStats().getMeleeStrength());
        assertEquals(4, state.getWeaponSpeed());
    }

    @Test
    public void bowfaAmmoCannotBypassSharedPreparation() throws Exception {
        // Given
        int[] itemIds = emptyIds();
        itemIds[EquipmentSlot.CAPE.getIndex()] = 28955;
        itemIds[EquipmentSlot.WEAPON.getIndex()] = 25867;
        itemIds[EquipmentSlot.AMMO.getIndex()] = 11212;
        runeLiteItem(28955, "Dizana's quiver", 18, 3, 0);
        runeLiteItem(25867, "Bow of faerdhinen", 128, 106, 4);
        runeLiteItem(11212, "Dragon arrow", 0, 60, 0);
        EquipmentResult shared = calculator().calculate(loadout(itemIds), EquipmentContext.of(
            415, EquipmentCombatStyle.of("ranged", "Rapid"), ""));

        // When
        EquipmentStats bypassed = visibleRuneLiteTotals(itemIds);
        PlayerState state = new PlayerState();
        state.setEquippedItemIds(itemIds);
        state.setEquippedItemNames(new String[14]);
        state.setCombatStyle(com.dpscalc.state.CombatStyle.RANGED_RAPID);
        manager.prepareEquipment(state, 415);

        // Then
        assertNotEquals(shared.getStats().getRangedStrength(), bypassed.getRangedStrength());
        assertEquals(shared.getStats().getRangedAttack(), state.getEquipmentStats().getRangedAttack());
        assertEquals(shared.getStats().getRangedStrength(), state.getEquipmentStats().getRangedStrength());
    }

    private Item item(int id) {
        Item item = mock(Item.class);
        when(item.getId()).thenReturn(id);
        return item;
    }

    private void runeLiteItem(int id, String name, int rangedAttack, int strength, int speed) {
        ItemComposition composition = mock(ItemComposition.class);
        ItemStats stats = mock(ItemStats.class);
        ItemEquipmentStats equipment = mock(ItemEquipmentStats.class);
        when(composition.getName()).thenReturn(name);
        when(equipment.getArange()).thenReturn(rangedAttack);
        when(equipment.getRstr()).thenReturn(strength);
        when(equipment.getStr()).thenReturn(strength);
        when(equipment.getAspeed()).thenReturn(speed);
        when(stats.getEquipment()).thenReturn(equipment);
        when(itemManager.getItemComposition(id)).thenReturn(composition);
        when(itemManager.getItemStats(eq(id), eq(false))).thenReturn(stats);
    }

    private static Item[] emptyItems() {
        Item[] items = new Item[14];
        for (int index = 0; index < items.length; index++) items[index] = mock(Item.class);
        for (Item item : items) when(item.getId()).thenReturn(-1);
        return items;
    }

    private static int[] emptyIds() {
        int[] ids = new int[14];
        java.util.Arrays.fill(ids, -1);
        return ids;
    }

    private void setField(String name, Object value) throws Exception {
        Field field = PlayerStateManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(manager, value);
    }

    private EquipmentStats visibleRuneLiteTotals(int[] itemIds) {
        EquipmentStats totals = new EquipmentStats();
        for (int id : itemIds) {
            if (id == -1) continue;
            ItemEquipmentStats equipment = itemManager.getItemStats(id, false).getEquipment();
            totals.addRangedAttack(equipment.getArange());
            totals.addRangedStrength(equipment.getRstr());
        }
        return totals;
    }

    private static EquipmentCalculator calculator() throws Exception {
        InputStream stream = PlayerStateManagerTest.class.getResourceAsStream("/equipment-domain.json");
        EquipmentDomainCatalog catalog = EquipmentDomainCatalog.load(stream,
            "b6bc098dc0d742b2b763375d2e78e1b611a22070",
            "070a34ce7f6267be1ae8c84cfdd3757100f5a027898ba34937eaba16536ea951");
        return new EquipmentCalculator(catalog);
    }

    private static EquipmentLoadout loadout(int[] itemIds) {
        Map<com.dpscalc.equipment.EquipmentSlot, EquipmentItem> items =
            new EnumMap<>(com.dpscalc.equipment.EquipmentSlot.class);
        put(items, com.dpscalc.equipment.EquipmentSlot.CAPE, itemIds, EquipmentSlot.CAPE);
        put(items, com.dpscalc.equipment.EquipmentSlot.WEAPON, itemIds, EquipmentSlot.WEAPON);
        put(items, com.dpscalc.equipment.EquipmentSlot.AMMO, itemIds, EquipmentSlot.AMMO);
        return EquipmentLoadout.of(items);
    }

    private static void put(Map<com.dpscalc.equipment.EquipmentSlot, EquipmentItem> items,
                            com.dpscalc.equipment.EquipmentSlot domainSlot, int[] ids, EquipmentSlot stateSlot) {
        int id = ids[stateSlot.getIndex()];
        if (id != -1) items.put(domainSlot, EquipmentItem.raw(id, Map.of()));
    }
}
