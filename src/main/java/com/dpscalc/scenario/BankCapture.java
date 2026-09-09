package com.dpscalc.scenario;

import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;

import java.util.*;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Updated only on the client thread; readers receive immutable snapshots. */
@Singleton
public final class BankCapture {
    @Inject private Client client;
    @Inject private ConfigManager config;
    private volatile OwnedEquipment snapshot = OwnedEquipment.empty(0);
    private Item[] bankItems;
    private boolean pending, bankChanged;
    private String profile;

    public OwnedEquipment snapshot() {
        return snapshot;
    }

    public synchronized void playerChanged() {
        pending = true;
    }

    public synchronized void clear() {
        bankItems = null;
        pending = false;
        bankChanged = false;
        profile = null;
        snapshot = OwnedEquipment.empty(snapshot.revision + 1);
    }

    private void checkProfile() {
        String next = config.getRSProfileKey();
        if (!Objects.equals(profile, next)) {
            clear();
            profile = next;
        }
    }

    public synchronized void changed(ItemContainerChanged event) {
        checkProfile();
        int id = event.getContainerId();
        if (id == InventoryID.BANK.getId()) {
            bankItems = event.getItemContainer().getItems().clone();
            bankChanged = true;
            pending = true;
        }
        if (id == InventoryID.INVENTORY.getId() || id == InventoryID.EQUIPMENT.getId())
            pending = true;
    }

    public synchronized void tick() {
        checkProfile();
        if (!pending || profile == null) return;
        pending = false;
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY),
                gear = client.getItemContainer(InventoryID.EQUIPMENT);
        if (inv == null || gear == null) {
            pending = true;
            return;
        }
        Map<String, Integer> levels = new TreeMap<>();
        for (Skill skill : Skill.values())
            if (skill != Skill.OVERALL)
                levels.put(skill.name().toLowerCase(Locale.ROOT), client.getRealSkillLevel(skill));
        if (client.getLocalPlayer() != null)
            levels.put("combat", client.getLocalPlayer().getCombatLevel());
        Map<Integer, Integer> bank = bankChanged ? normalize(bankItems) : snapshot.bank,
                inventory = normalize(inv.getItems()),
                equipped = normalize(gear.getItems());
        if (bankChanged
                || !bank.equals(snapshot.bank)
                || !inventory.equals(snapshot.inventory)
                || !equipped.equals(snapshot.equipped)
                || !levels.equals(snapshot.levels)) {
            Map<Integer, String> names = new TreeMap<>();
            for (Map<Integer, Integer> container : Arrays.asList(bank, inventory, equipped))
                for (int id : container.keySet()) {
                    ItemComposition definition = client.getItemDefinition(id);
                    if (definition != null && definition.getName() != null)
                        names.put(id, definition.getName());
                }
            snapshot =
                    new OwnedEquipment(
                            profile,
                            snapshot.revision + 1,
                            bankChanged ? System.currentTimeMillis() : snapshot.bankScannedAt,
                            bank,
                            inventory,
                            equipped,
                            levels,
                            names);
        }
        bankChanged = false;
    }

    private Map<Integer, Integer> normalize(Item[] items) {
        Map<Integer, Integer> result = new TreeMap<>();
        if (items == null) return result;
        for (Item item : items) {
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0) continue;
            ItemComposition definition = client.getItemDefinition(item.getId());
            addItem(
                    result,
                    item.getId(),
                    item.getQuantity(),
                    definition.getPlaceholderTemplateId() != -1,
                    definition.getNote() != -1,
                    definition.getLinkedNoteId());
        }
        return result;
    }

    static void addItem(
            Map<Integer, Integer> result,
            int original,
            int quantity,
            boolean placeholder,
            boolean noted,
            int linked) {
        if (original <= 0 || quantity <= 0 || placeholder) return;
        int id = noted ? linked : original;
        if (id > 0)
            result.merge(id, quantity, (a, b) -> (int) Math.min(Integer.MAX_VALUE, (long) a + b));
    }
}
