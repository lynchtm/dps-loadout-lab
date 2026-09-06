package com.dpscalc.scenario;

import com.dpscalc.equipment.*;
import com.dpscalc.wikisetups.WikiSetupOptions.MissingGearBehavior;
import com.dpscalc.wikisetups.bank.*;
import com.dpscalc.wikisetups.items.*;
import com.dpscalc.wikisetups.wiki.*;

import java.util.*;
import java.util.function.IntPredicate;

/**
 * Adapts Wiki recommendations to independent calculator drafts; rankings never replace DPS scoring.
 */
public final class WikiLoadouts {
    public static final class Choice {
        public final String label;
        public final GearSetup gear;
        public final InventorySetup inventory;
        public final List<GearSetup> switches;

        public Choice(
                String label, GearSetup gear, InventorySetup inventory, List<GearSetup> switches) {
            this.label = label;
            this.gear = gear;
            this.inventory = inventory;
            this.switches = switches;
        }

        public String toString() {
            return label.replace('<', '‹').replace('>', '›');
        }
    }

    public static List<Choice> parse(String text) {
        if (text.length() > WikiSetupService.MAX_BYTES)
            throw new IllegalArgumentException("Wiki page too large");
        int depth = 0;
        for (int p = 0; p + 1 < text.length(); p++) {
            if (text.startsWith("{{", p)) {
                if (++depth > 64)
                    throw new IllegalArgumentException("Wiki template nesting exceeds 64 levels");
                p++;
            } else if (text.startsWith("}}", p)) {
                depth = Math.max(0, depth - 1);
                p++;
            }
        }
        List<Choice> choices = new ArrayList<>();
        List<GearSetup> gear = new GearSetupParser().parse(text);
        List<InventorySetup> inventories = new InventoryParser().parse(text);
        List<ExampleSetup> examples = new ExampleSetupParser().parse(text);
        GearSetups.Partition partition = GearSetups.partition(gear);
        int[] pairing =
                examples.isEmpty()
                        ? partition.remapPairing(
                                SetupPairing.pair(text, gear.size(), inventories.size()))
                        : SetupPairing.NONE;
        if (pairing.length == 0 && examples.isEmpty())
            pairing =
                    SetupPairing.pairLoadouts(
                            text, gear.size(), inventories.size(), partition.getLoadoutIndices());
        int[] named = SetupPairing.pairUniqueLabels(partition.getLoadouts(), inventories);
        if (pairing.length == 0) pairing = named;
        else for (int i = 0; i < pairing.length; i++) if (pairing[i] < 0) pairing[i] = named[i];
        for (int i = 0; i < partition.getLoadouts().size() && choices.size() < 100; i++) {
            GearSetup setup = partition.getLoadouts().get(i);
            String label =
                    Objects.toString(setup.getPaneLabel(), "")
                            + " · "
                            + Objects.toString(setup.getStyleLabel(), "Setup " + (i + 1));
            choices.add(
                    new Choice(
                            label,
                            setup,
                            pairing.length > i && pairing[i] >= 0
                                    ? inventories.get(pairing[i])
                                    : null,
                            partition.getSwitches()));
        }
        for (ExampleSetup example : examples) {
            if (choices.size() >= 100) break;
            choices.add(
                    new Choice(
                            (example.getPath().isEmpty()
                                            ? ""
                                            : String.join(" / ", example.getPath()) + " · ")
                                    + example.getLabel(),
                            example.getGear(),
                            example.getInventory(),
                            partition.getSwitches()));
        }
        return choices;
    }

    public static Scenario.Loadout draft(
            Choice choice,
            WikiSetupService.Page page,
            Scenario.Loadout base,
            OwnedEquipment owned,
            String profile,
            boolean preferOwned,
            ItemNameIndex index,
            IntPredicate stackable,
            EquipmentPreparationFacade equipment) {
        if (!index.isReady())
            throw new IllegalArgumentException("Item names are still loading. Try again shortly.");
        if (preferOwned && !owned.ready(profile))
            throw new IllegalArgumentException(
                    "Open this character's bank before choosing owned alternatives.");
        Set<Integer> ownedIds =
                owned.ready(profile) ? owned.quantities.keySet() : Collections.emptySet();
        ItemResolver resolver = new ItemResolver(index);
        OwnershipResolver ownership =
                new OwnershipResolver(
                        resolver,
                        (requested, id) -> {
                            try {
                                EquipmentCatalogItem facts = equipment.getItemFacts(id);
                                return facts != null
                                        && facts.getSlot()
                                                .equals(
                                                        requested == EquipSlot.SPECIAL
                                                                ? "weapon"
                                                                : slotName(slot(requested)));
                            } catch (IllegalArgumentException unavailable) {
                                return false;
                            }
                        });
        PickStrategy strategy =
                preferOwned ? PickStrategy.BEST_OWNED : PickStrategy.TOP_RECOMMENDATION;
        SetupResolution resolved =
                ownership.resolve(
                        choice.gear,
                        ownedIds,
                        MissingGearBehavior.SHOW_BEST_UNOWNED,
                        base.player.isOnSlayerTask(),
                        strategy);
        Scenario.Loadout draft = base.copy();
        draft.name = shortText(page.title + " · " + choice.label, 180);
        draft.manualEquipmentStats = false;
        draft.player.setRawEquipmentLoadout(null);
        draft.carry = new CarryPlan();
        draft.wikiSource = page.url();
        draft.wikiRevision = page.revision;
        draft.wikiNotes = new ArrayList<>();
        draft.wikiSeeds = new ArrayList<>();
        int[] ids = new int[14];
        Arrays.fill(ids, -1);
        String[] names = new String[14];
        for (SlotPick pick : resolved.getPicks()) {
            int slot = slot(pick.getSlot());
            int id = pick.getItemId();
            String name = Objects.toString(index.nameById(id), pick.getItem().getName());
            if (slot < 0) {
                draft.carry.switches.add(new CarryPlan.Entry(id, name, 1));
                continue;
            }
            try {
                EquipmentCatalogItem facts = equipment.getItemFacts(id);
                if (!facts.getSlot().equals(slotName(slot)))
                    throw new IllegalArgumentException("slot mismatch");
                ids[slot] = id;
                names[slot] = name;
            } catch (IllegalArgumentException error) {
                draft.wikiNotes.add("No compatible DPS data for " + name + "; slot left empty.");
                continue;
            }
            draft.wikiNotes.add(
                    BankOptimizer.SLOT_NAMES[indexOfSlot(slot)]
                            + ": "
                            + name
                            + " — "
                            + (owned.ready(profile)
                                    ? pick.isOwned()
                                            ? (pick.isTopRecommendation()
                                                    ? "owned"
                                                    : "owned alternative")
                                            : "missing"
                                    : "ownership unknown")
                            + ". "
                            + Objects.toString(pick.getItem().getNote(), ""));
        }
        resolved.getUnresolvedNames()
                .forEach(
                        (slot, unknown) ->
                                draft.wikiNotes.add(
                                        "Unrecognized or unsupported "
                                                + slot
                                                + ": "
                                                + String.join(", ", unknown)));
        if (ids[3] > 0 && equipment.getItemFacts(ids[3]).isTwoHanded() && ids[5] > 0) {
            draft.wikiNotes.add("Off-hand removed for a two-handed weapon.");
            ids[5] = -1;
            names[5] = null;
        }
        draft.player.setEquippedItemIds(ids);
        draft.player.setEquippedItemNames(names);
        WeaponStyles.normalize(draft, equipment);
        String label = choice.label.toLowerCase(Locale.ROOT);
        String family =
                label.contains("magic") || label.contains("burst") || label.contains("barrage")
                        ? "Magic"
                        : label.contains("ranged")
                                ? "Ranged"
                                : label.contains("melee") ? "Melee" : "";
        for (com.dpscalc.state.CombatStyle style :
                WeaponStyles.available(draft.player, equipment)) {
            String type =
                    style.getAttackType().isMagic()
                            ? "Magic"
                            : style.getAttackType().isRanged() ? "Ranged" : "Melee";
            if (type.equals(family)) {
                WeaponStyles.select(draft, style);
                break;
            }
        }
        if (choice.inventory != null) {
            InventoryResolution inventory =
                    new InventoryResolver(resolver)
                            .resolve(
                                    choice.inventory,
                                    ownedIds,
                                    MissingGearBehavior.SHOW_BEST_UNOWNED,
                                    Collections.singletonList(choice.gear),
                                    strategy);
            fill(
                    draft.carry.slots,
                    choice.inventory.getSlots(),
                    inventory.getPicks(),
                    choice.inventory,
                    false,
                    index,
                    stackable);
            fill(
                    draft.carry.runes,
                    choice.inventory.getRunes(),
                    inventory.getRunePicks(),
                    choice.inventory,
                    true,
                    index,
                    stackable);
        } else
            draft.wikiNotes.add(
                    "No unambiguous inventory pairing found; add supplies in Equipment.");
        for (GearSetup extra : choice.switches) {
            SetupResolution switches =
                    ownership.resolve(
                            extra,
                            ownedIds,
                            MissingGearBehavior.SHOW_BEST_UNOWNED,
                            base.player.isOnSlayerTask(),
                            strategy);
            for (SlotPick pick : switches.getPicks()) {
                if (draft.carry.switches.size() >= 28) break;
                CarryPlan.Entry entry =
                        new CarryPlan.Entry(
                                pick.getItemId(),
                                Objects.toString(
                                        index.nameById(pick.getItemId()), pick.getItem().getName()),
                                1);
                entry.note =
                        shortText(
                                Objects.toString(extra.getStyleLabel(), "Situational switch")
                                        + ": "
                                        + Objects.toString(pick.getItem().getNote(), ""),
                                2000);
                draft.carry.switches.add(entry);
            }
        }
        draft.wikiSeeds.add(ids.clone());
        for (String path :
                Scenario.flatten(Scenario.JSON.toJsonTree(draft.player).getAsJsonObject()).keySet())
            if (path.startsWith("equippedItem")) {
                draft.overrides.add(path);
                draft.sources.put(path, "Wiki template draft");
            }
        draft.description =
                "Wiki recommendation from revision "
                        + page.revision
                        + ". Calculated against the selected target. Check Combat, Prayer and"
                        + " Settings for this method.";
        draft.wikiNotes.add(
                "Wiki ranks are starting points. The bank optimizer scores full combinations"
                        + " against your selected target.");
        draft.wikiNotes.replaceAll(n -> shortText(n, 4000));
        if (draft.wikiNotes.size() > 200)
            draft.wikiNotes = new ArrayList<>(draft.wikiNotes.subList(0, 200));
        Scenario.validate(draft);
        return draft;
    }

    private static void fill(
            CarryPlan.Entry[] into,
            List<String> source,
            List<InventoryPick> picks,
            InventorySetup setup,
            boolean rune,
            ItemNameIndex index,
            IntPredicate stackable) {
        for (int i = 0; i < source.size() && i < into.length; i++)
            if (!source.get(i).isEmpty())
                into[i] =
                        new CarryPlan.Entry(
                                0, shortText(source.get(i), 200), setup.quantity(i, rune));
        for (InventoryPick pick : picks) {
            int slot = pick.getSlot();
            if (slot >= into.length) continue;
            int quantity = setup.quantity(slot, rune);
            if (quantity == 0 && !stackable.test(pick.getItemId())) quantity = 1;
            CarryPlan.Entry entry =
                    new CarryPlan.Entry(
                            pick.getItemId(),
                            Objects.toString(index.nameById(pick.getItemId()), source.get(slot)),
                            quantity);
            entry.noted = !rune && setup.noted(slot);
            if (entry.noted)
                entry.note =
                        "Wiki requests a noted stack. Check the packing arrangement; bank tags"
                                + " store item IDs only.";
            into[slot] = entry;
        }
    }

    static int slot(EquipSlot slot) {
        switch (slot) {
            case HEAD:
                return 0;
            case CAPE:
                return 1;
            case NECK:
                return 2;
            case WEAPON:
                return 3;
            case BODY:
                return 4;
            case SHIELD:
                return 5;
            case LEGS:
                return 7;
            case HANDS:
                return 9;
            case FEET:
                return 10;
            case RING:
                return 12;
            case AMMO:
                return 13;
            default:
                return -1;
        }
    }

    static String slotName(int slot) {
        return new String[] {
                    "head", "cape", "neck", "weapon", "body", "shield", "", "legs", "", "hands",
                    "feet", "", "ring", "ammo"
                }
                [slot];
    }

    private static int indexOfSlot(int slot) {
        for (int i = 0; i < BankOptimizer.SLOTS.length; i++)
            if (BankOptimizer.SLOTS[i] == slot) return i;
        return 0;
    }

    private static String shortText(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}
