/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently acquired RuneLite/Wiki facts.
 */
package com.loadoutlab.equipment;

import com.google.gson.*;
import com.loadoutlab.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Snapshot-based equipment preparation, safe to call from a calculation worker. */
public final class EquipmentPreparationFacade {
    private final Map<Integer, EquipmentCatalogItem> items;
    public static final String REFERENCE_SHA = "independent-facts-2026-09-09",
            DOMAIN_DIGEST = "see facts-provenance.json";

    public EquipmentPreparationFacade() {
        Map<Integer, EquipmentCatalogItem> loaded = new TreeMap<>();
        try (Reader r =
                new InputStreamReader(
                        Objects.requireNonNull(
                                getClass()
                                        .getResourceAsStream(
                                                "/com/loadoutlab/equipment-facts.json")),
                        StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(r).getAsJsonObject();
            Gson gson = net.runelite.http.api.RuneLiteAPI.GSON;
            for (Map.Entry<String, JsonElement> row : root.entrySet())
                loaded.put(
                        Integer.parseInt(row.getKey()),
                        gson.fromJson(row.getValue(), EquipmentCatalogItem.class));
        } catch (IOException ex) {
            throw new IllegalStateException("Equipment facts could not be loaded", ex);
        }
        items = Collections.unmodifiableMap(loaded);
    }

    public Map<Integer, EquipmentCatalogItem> getItems() {
        return items;
    }

    public int canonicalId(int id) {
        int base = net.runelite.client.game.ItemVariationMapping.map(id);
        return items.containsKey(base) ? base : id;
    }

    public EquipmentCatalogItem getItemFacts(int id) {
        return items.containsKey(id) ? items.get(id) : items.get(canonicalId(id));
    }

    public static EquipmentContext context(PlayerState player, int targetId) {
        return new EquipmentContext(targetId);
    }

    public EquipmentLoadout loadoutFromIds(int[] ids) {
        if (ids == null || ids.length != 14)
            throw new IllegalArgumentException("Equipment requires fourteen slots");
        Map<EquipmentSlot, EquipmentItem> slots = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values())
            if (ids[slot.index] > 0)
                slots.put(slot, EquipmentItem.raw(ids[slot.index], Collections.emptyMap()));
        return EquipmentLoadout.of(slots);
    }

    public EquipmentResult prepare(
            PlayerState player, int[] ids, String[] names, EquipmentContext context) {
        return prepare(player, loadoutFromIds(ids), names, context);
    }

    public EquipmentResult prepare(
            PlayerState player,
            EquipmentLoadout loadout,
            String[] liveNames,
            EquipmentContext context) {
        EquipmentStatTotals totals = new EquipmentStatTotals();
        int[] ids = new int[14];
        Arrays.fill(ids, -1);
        String[] names = new String[14], versions = new String[14], categories = new String[14];
        EquipmentCatalogItem weapon = facts(loadout.get(EquipmentSlot.WEAPON)),
                ammo = facts(loadout.get(EquipmentSlot.AMMO));
        AmmoApplicability applicability = AmmoRules.classify(weapon, ammo);
        if (weapon != null && weapon.isTwoHanded() && loadout.get(EquipmentSlot.SHIELD) != null)
            throw new IllegalArgumentException("Remove the shield when using a two-handed weapon");
        for (Map.Entry<EquipmentSlot, EquipmentItem> selected : loadout.asMap().entrySet()) {
            EquipmentSlot slot = selected.getKey();
            EquipmentCatalogItem item = facts(selected.getValue());
            if (item == null)
                throw new IllegalArgumentException(
                        "Equipment stats unavailable for item "
                                + selected.getValue().getOriginalId());
            if (!slot.key().equals(item.getSlot()))
                throw new IllegalArgumentException(item.getName() + " does not fit " + slot.key());
            ids[slot.index] = selected.getValue().getOriginalId();
            names[slot.index] = item.getName();
            versions[slot.index] = item.getVersion();
            categories[slot.index] = item.getCategory();
            add(
                    totals,
                    item.getStats(),
                    slot != EquipmentSlot.AMMO || applicability == AmmoApplicability.INCLUDED);
        }
        int speed = weapon == null ? 4 : weapon.getSpeed();
        if (speed <= 0)
            throw new IllegalArgumentException("This item cannot be used as a combat weapon");
        if (player.getCombatStyle().getAttackType().isRanged()
                && ("Rapid".equals(player.getCombatStyle().getStance())
                        || "Medium fuse".equals(player.getCombatStyle().getStance())))
            speed = Math.max(1, speed - 1);
        if (weapon != null && weapon.getName().toLowerCase(Locale.ROOT).contains("blowpipe")) {
            ItemVariable dart =
                    loadout.get(EquipmentSlot.WEAPON).getItemVariables().get("blowpipeDartId");
            if (dart == null)
                throw new IllegalArgumentException("Choose the darts loaded in the blowpipe");
            EquipmentCatalogItem dartStats = getItemFacts(dart.intValue());
            if (dartStats == null || !dartStats.getName().toLowerCase(Locale.ROOT).contains("dart"))
                throw new IllegalArgumentException("Invalid blowpipe darts");
            totals.addRangedStrength(dartStats.getStats().getRangedStrength());
        }
        player.setEquippedItemIds(ids);
        player.setEquippedItemNames(names);
        player.setEquippedItemVersions(versions);
        player.setEquippedItemCategories(categories);
        player.setEquipmentStats(totals);
        player.setWeaponSpeed(speed);
        player.setAmmoApplicability(applicability);
        player.setRawEquipmentLoadout(loadout);
        return new EquipmentResult(loadout, totals, speed, applicability);
    }

    private EquipmentCatalogItem facts(EquipmentItem item) {
        return item == null ? null : getItemFacts(item.getOriginalId());
    }

    private static void add(EquipmentStats a, EquipmentStats b, boolean rangedStrength) {
        a.addStabAttack(b.getStabAttack());
        a.addSlashAttack(b.getSlashAttack());
        a.addCrushAttack(b.getCrushAttack());
        a.addMagicAttack(b.getMagicAttack());
        a.addRangedAttack(b.getRangedAttack());
        a.addMeleeStrength(b.getMeleeStrength());
        if (rangedStrength) a.addRangedStrength(b.getRangedStrength());
        a.addMagicDamage(b.getMagicDamage());
        a.addPrayerBonus(b.getPrayerBonus());
        a.addStabDefence(b.getStabDefence());
        a.addSlashDefence(b.getSlashDefence());
        a.addCrushDefence(b.getCrushDefence());
        a.addMagicDefence(b.getMagicDefence());
        a.addRangedDefence(b.getRangedDefence());
    }
}
