/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2026, Jiimbones
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * Adapted for DPS Loadout Lab. Original source e836b8d156f0f5f9b7e6375bb04c52f21835ac19.
 */
package com.dpscalc.wikisetups.bank;

import com.dpscalc.wikisetups.WikiSetupOptions.MissingGearBehavior;
import com.dpscalc.wikisetups.items.FakeItemNameIndex;
import com.dpscalc.wikisetups.items.ItemResolver;
import com.dpscalc.wikisetups.wiki.EquipSlot;
import com.dpscalc.wikisetups.wiki.GearSetup;
import com.dpscalc.wikisetups.wiki.RankedItem;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OwnershipResolverTest
{
	// real ids — karil's leathertop variation group
	private static final int KARILS_LEATHERTOP = 4736;
	private static final int KARILS_LEATHERTOP_100 = 4940;
	// real ids with no variation group entanglement needed
	private static final int OCCULT_NECKLACE = 12002;
	private static final int AMULET_OF_FURY = 6585;

	private final FakeItemNameIndex index = FakeItemNameIndex.of(
		KARILS_LEATHERTOP, "Karil's leathertop",
		KARILS_LEATHERTOP_100, "Karil's leathertop 100",
		OCCULT_NECKLACE, "Occult necklace",
		AMULET_OF_FURY, "Amulet of fury");

	private final OwnershipResolver resolver = new OwnershipResolver(new ItemResolver(index));

	private static GearSetup setup(EquipSlot slot, RankedItem... items)
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(slot, Arrays.asList(items));
		return new GearSetup("Test", slots);
	}

	private static Set<Integer> owned(Integer... ids)
	{
		return new HashSet<>(Arrays.asList(ids));
	}

	// set-bonus gating — arbitrary ids, resolution goes through the fake index
	private static final int BLOOD_MOON_HELM = 29028;
	private static final int BLOOD_MOON_CHEST = 29031;
	private static final int BLOOD_MOON_TASSETS = 29034;
	private static final int DUAL_MACUAHUITL = 20155;
	private static final int NEITIZNOT_FACEGUARD = 24271;
	private static final int VOID_MAGE_HELM = 11663;
	private static final int CRYSTAL_BODY = 23975;
	private static final int BOWFA = 25865;
	private static final String FULL_SET_NOTE = "Best used with the full Blood moon armour set and Dual macuahuitl";

	private OwnershipResolver setResolver()
	{
		return new OwnershipResolver(new ItemResolver(FakeItemNameIndex.of(
			BLOOD_MOON_HELM, "Blood moon helm",
			BLOOD_MOON_CHEST, "Blood moon chestplate",
			BLOOD_MOON_TASSETS, "Blood moon tassets",
			DUAL_MACUAHUITL, "Dual macuahuitl",
			NEITIZNOT_FACEGUARD, "Neitiznot faceguard",
			VOID_MAGE_HELM, "Void mage helm",
			CRYSTAL_BODY, "Crystal body",
			BOWFA, "Bow of faerdhinen")));
	}

	@Test
	public void noteFlaggedSetPieceSkippedWithoutRestOfSet()
	{
		GearSetup s = setup(EquipSlot.HEAD,
			new RankedItem("Blood moon helm", 1, FULL_SET_NOTE),
			new RankedItem("Neitiznot faceguard", 2, null));
		SetupResolution r = setResolver().resolve(s,
			owned(BLOOD_MOON_HELM, NEITIZNOT_FACEGUARD), MissingGearBehavior.OMIT, false);

		assertEquals(NEITIZNOT_FACEGUARD, r.getPicks().get(0).getItemId());
	}

	@Test
	public void noteFlaggedSetPiecePickedWhenSetComplete()
	{
		GearSetup s = setup(EquipSlot.HEAD,
			new RankedItem("Blood moon helm", 1, FULL_SET_NOTE),
			new RankedItem("Neitiznot faceguard", 2, null));
		SetupResolution r = setResolver().resolve(s,
			owned(BLOOD_MOON_HELM, BLOOD_MOON_CHEST, BLOOD_MOON_TASSETS, DUAL_MACUAHUITL, NEITIZNOT_FACEGUARD),
			MissingGearBehavior.OMIT, false);

		assertEquals(BLOOD_MOON_HELM, r.getPicks().get(0).getItemId());
	}

	@Test
	public void unflaggedRecommendationOfSetPieceIsNotGated()
	{
		// same item recommended standalone (no footnote) — rank 3 Dual macuahuitl case
		GearSetup s = setup(EquipSlot.WEAPON,
			new RankedItem("Dual macuahuitl", 1, null));
		SetupResolution r = setResolver().resolve(s,
			owned(DUAL_MACUAHUITL), MissingGearBehavior.OMIT, false);

		assertEquals(DUAL_MACUAHUITL, r.getPicks().get(0).getItemId());
	}

	@Test
	public void voidPieceGatedEvenWithoutNote()
	{
		GearSetup s = setup(EquipSlot.HEAD,
			new RankedItem("Void mage helm", 1, null),
			new RankedItem("Neitiznot faceguard", 2, null));
		SetupResolution r = setResolver().resolve(s,
			owned(VOID_MAGE_HELM, NEITIZNOT_FACEGUARD), MissingGearBehavior.OMIT, false);

		assertEquals(NEITIZNOT_FACEGUARD, r.getPicks().get(0).getItemId());
	}

	@Test
	public void crystalArmourRequiresCrystalBow()
	{
		GearSetup s = setup(EquipSlot.BODY,
			new RankedItem("Crystal body", 1, null));
		OwnershipResolver r = setResolver();

		assertTrue(r.resolve(s, owned(CRYSTAL_BODY), MissingGearBehavior.OMIT, false)
			.getPicks().isEmpty());
		assertEquals(CRYSTAL_BODY, r.resolve(s, owned(CRYSTAL_BODY, BOWFA), MissingGearBehavior.OMIT, false)
			.getPicks().get(0).getItemId());
	}

	@Test
	public void ownedSetPieceStillSkippedWhenSetNotWornInLayout()
	{
		// full blood moon set banked, but a better body wins the body slot —
		// the helm must not ride along on ownership alone
		int bandos = 11832;
		OwnershipResolver r = new OwnershipResolver(new ItemResolver(FakeItemNameIndex.of(
			BLOOD_MOON_HELM, "Blood moon helm",
			BLOOD_MOON_CHEST, "Blood moon chestplate",
			BLOOD_MOON_TASSETS, "Blood moon tassets",
			DUAL_MACUAHUITL, "Dual macuahuitl",
			NEITIZNOT_FACEGUARD, "Neitiznot faceguard",
			bandos, "Bandos chestplate")));

		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(EquipSlot.HEAD, Arrays.asList(
			new RankedItem("Blood moon helm", 1, FULL_SET_NOTE),
			new RankedItem("Neitiznot faceguard", 2, null)));
		slots.put(EquipSlot.BODY, Arrays.asList(
			new RankedItem("Bandos chestplate", 1, null),
			new RankedItem("Blood moon chestplate", 2, FULL_SET_NOTE)));
		GearSetup s = new GearSetup("Test", slots);

		SetupResolution resolution = r.resolve(s,
			owned(BLOOD_MOON_HELM, BLOOD_MOON_CHEST, BLOOD_MOON_TASSETS, DUAL_MACUAHUITL,
				NEITIZNOT_FACEGUARD, bandos),
			MissingGearBehavior.OMIT, false);

		assertEquals(2, resolution.getPicks().size());
		assertEquals(NEITIZNOT_FACEGUARD, resolution.getPicks().get(0).getItemId());
		assertEquals(bandos, resolution.getPicks().get(1).getItemId());
	}

	@Test
	public void setPieceKeptWhenWholeSetIsWorn()
	{
		OwnershipResolver r = setResolver();
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(EquipSlot.HEAD, Arrays.asList(
			new RankedItem("Blood moon helm", 1, FULL_SET_NOTE),
			new RankedItem("Neitiznot faceguard", 2, null)));
		slots.put(EquipSlot.BODY, Collections.singletonList(
			new RankedItem("Blood moon chestplate", 1, null)));
		slots.put(EquipSlot.LEGS, Collections.singletonList(
			new RankedItem("Blood moon tassets", 1, null)));
		slots.put(EquipSlot.WEAPON, Collections.singletonList(
			new RankedItem("Dual macuahuitl", 1, null)));
		GearSetup s = new GearSetup("Test", slots);

		SetupResolution resolution = r.resolve(s,
			owned(BLOOD_MOON_HELM, BLOOD_MOON_CHEST, BLOOD_MOON_TASSETS, DUAL_MACUAHUITL, NEITIZNOT_FACEGUARD),
			MissingGearBehavior.OMIT, false);

		assertEquals(BLOOD_MOON_HELM, resolution.getPicks().get(0).getItemId());
	}

	@Test
	public void fullyGatedSlotStillShowsTopPieceAsGoal()
	{
		GearSetup s = setup(EquipSlot.HEAD,
			new RankedItem("Blood moon helm", 1, FULL_SET_NOTE));
		SetupResolution r = setResolver().resolve(s,
			owned(), MissingGearBehavior.SHOW_BEST_UNOWNED, false);

		assertEquals(1, r.getPicks().size());
		assertEquals(BLOOD_MOON_HELM, r.getPicks().get(0).getItemId());
		assertFalse(r.getPicks().get(0).isOwned());
	}

	@Test
	public void slayerConditionalDetectionDrivesTheToggle()
	{
		GearSetup conditional = setup(EquipSlot.HEAD,
			new RankedItem("Slayer helmet (i)", 1, "If on a Zulrah slayer boss task"),
			new RankedItem("Ancestral hat", 2, null));
		assertTrue(OwnershipResolver.hasSlayerConditional(conditional));

		GearSetup plain = setup(EquipSlot.HEAD, new RankedItem("Ancestral hat", 1, null));
		assertFalse(OwnershipResolver.hasSlayerConditional(plain));

		// an informational footnote mentioning slayer isn't a task condition
		GearSetup informational = setup(EquipSlot.HEAD,
			new RankedItem("Slayer helmet (i)", 1, "Requires 55 Slayer to make"));
		assertFalse(OwnershipResolver.hasSlayerConditional(informational));
	}

	@Test
	public void fallbackNamesRescueAPageAnchorLink()
	{
		int imbuedSara = 21791;
		OwnershipResolver r = new OwnershipResolver(new ItemResolver(
			FakeItemNameIndex.of(imbuedSara, "Imbued saradomin cape")));

		// what CoX/ToA actually produce: an unresolvable page+anchor name
		// with the real item in the display text and picture
		GearSetup s = setup(EquipSlot.CAPE, new RankedItem("God capes#Imbuing", 1, null,
			Arrays.asList("Imbued god cape", "Imbued saradomin cape")));

		SetupResolution resolution = r.resolve(s, owned(imbuedSara), MissingGearBehavior.OMIT, false);

		assertEquals(1, resolution.getPicks().size());
		assertEquals(imbuedSara, resolution.getPicks().get(0).getItemId());
		assertTrue("nothing should be reported unrecognized", resolution.getUnresolvedNames().isEmpty());
	}

	// --- PickStrategy.TOP_RECOMMENDATION (the "show wiki BIS" mode) ---

	@Test
	public void topRecommendationBeatsAnOwnedLowerAlternative()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Occult necklace", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, owned(AMULET_OF_FURY), MissingGearBehavior.OMIT,
			false, PickStrategy.TOP_RECOMMENDATION);

		SlotPick pick = r.getPicks().get(0);
		assertEquals(OCCULT_NECKLACE, pick.getItemId());
		assertFalse("the wiki's pick is not owned here", pick.isOwned());
		assertTrue(pick.isTopRecommendation());
	}

	@Test
	public void topRecommendationPlacesTheOwnedVariantWhenOwned()
	{
		GearSetup s = setup(EquipSlot.BODY, new RankedItem("Karil's leathertop", 1, null));
		SetupResolution r = resolver.resolve(s, owned(KARILS_LEATHERTOP_100),
			MissingGearBehavior.OMIT, false, PickStrategy.TOP_RECOMMENDATION);

		SlotPick pick = r.getPicks().get(0);
		assertEquals("place the copy the player actually has", KARILS_LEATHERTOP_100, pick.getItemId());
		assertTrue(pick.isOwned());
	}

	@Test
	public void topRecommendationPlacesUnownedItemsRegardlessOfMissingConfig()
	{
		GearSetup s = setup(EquipSlot.NECK, new RankedItem("Occult necklace", 1, null));
		// OMIT would normally drop an unowned slot entirely
		SetupResolution r = resolver.resolve(s, owned(), MissingGearBehavior.OMIT,
			false, PickStrategy.TOP_RECOMMENDATION);

		assertEquals(1, r.getPicks().size());
		assertFalse(r.getPicks().get(0).isOwned());
	}

	@Test
	public void ownedSetPieceSurvivesInTopRecommendationMode()
	{
		// the mirror of ownedSetPieceStillSkippedWhenSetNotWornInLayout: set
		// gating is ownership-derived, so the BIS view must not apply it
		int bandos = 11832;
		OwnershipResolver r = new OwnershipResolver(new ItemResolver(FakeItemNameIndex.of(
			BLOOD_MOON_HELM, "Blood moon helm",
			BLOOD_MOON_CHEST, "Blood moon chestplate",
			BLOOD_MOON_TASSETS, "Blood moon tassets",
			DUAL_MACUAHUITL, "Dual macuahuitl",
			NEITIZNOT_FACEGUARD, "Neitiznot faceguard",
			bandos, "Bandos chestplate")));

		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(EquipSlot.HEAD, Arrays.asList(
			new RankedItem("Blood moon helm", 1, FULL_SET_NOTE),
			new RankedItem("Neitiznot faceguard", 2, null)));
		slots.put(EquipSlot.BODY, Arrays.asList(
			new RankedItem("Bandos chestplate", 1, null),
			new RankedItem("Blood moon chestplate", 2, FULL_SET_NOTE)));
		GearSetup s = new GearSetup("Test", slots);

		SetupResolution resolution = r.resolve(s,
			owned(BLOOD_MOON_HELM, BLOOD_MOON_CHEST, BLOOD_MOON_TASSETS, DUAL_MACUAHUITL,
				NEITIZNOT_FACEGUARD, bandos),
			MissingGearBehavior.OMIT, false, PickStrategy.TOP_RECOMMENDATION);

		assertEquals(BLOOD_MOON_HELM, resolution.getPicks().get(0).getItemId());
		assertTrue(resolution.getPicks().get(0).isOwned());
	}

	@Test
	public void slayerGatingStillAppliesInTopRecommendationMode()
	{
		int slayerHelm = 11864;
		OwnershipResolver r = new OwnershipResolver(new ItemResolver(FakeItemNameIndex.of(
			slayerHelm, "Slayer helmet",
			NEITIZNOT_FACEGUARD, "Neitiznot faceguard")));
		GearSetup s = setup(EquipSlot.HEAD,
			new RankedItem("Slayer helmet", 1, "If on a Zulrah slayer boss task"),
			new RankedItem("Neitiznot faceguard", 2, null));

		SetupResolution off = r.resolve(s, owned(), MissingGearBehavior.OMIT,
			false, PickStrategy.TOP_RECOMMENDATION);
		assertEquals("slayer-only item skipped when not on task",
			NEITIZNOT_FACEGUARD, off.getPicks().get(0).getItemId());

		SetupResolution on = r.resolve(s, owned(), MissingGearBehavior.OMIT,
			true, PickStrategy.TOP_RECOMMENDATION);
		assertEquals("and taken when the player says they are on task",
			slayerHelm, on.getPicks().get(0).getItemId());
	}

	@Test
	public void twoHandedWeaponDropsShieldPick()
	{
		List<SlotPick> picks = Arrays.asList(
			new SlotPick(EquipSlot.WEAPON, new RankedItem("Elder maul", 1, null), 100, true, true),
			new SlotPick(EquipSlot.SHIELD, new RankedItem("Dragon defender", 1, null), 200, true, true),
			new SlotPick(EquipSlot.HEAD, new RankedItem("Neitiznot", 1, null), 300, true, true));

		List<SlotPick> filtered = OwnershipResolver.dropShieldForTwoHanded(picks, id -> id == 100);

		assertEquals(2, filtered.size());
		assertFalse(filtered.stream().anyMatch(p -> p.getSlot() == EquipSlot.SHIELD));
	}

	@Test
	public void oneHandedWeaponKeepsShieldPick()
	{
		List<SlotPick> picks = Arrays.asList(
			new SlotPick(EquipSlot.WEAPON, new RankedItem("Abyssal whip", 1, null), 100, true, true),
			new SlotPick(EquipSlot.SHIELD, new RankedItem("Dragon defender", 1, null), 200, true, true));

		assertEquals(picks, OwnershipResolver.dropShieldForTwoHanded(picks, id -> false));
	}

	@Test
	public void ownedAlternativesReturnsUnpickedOwnedItems()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Occult necklace", 1, null),
			new RankedItem("Amulet of fury", 2, null));

		List<Integer> extras = resolver.ownedAlternatives(s,
			owned(OCCULT_NECKLACE, AMULET_OF_FURY), false,
			owned(OCCULT_NECKLACE)); // the pick is excluded

		assertEquals(Collections.singletonList(AMULET_OF_FURY), extras);
	}

	@Test
	public void topRecommendationPickedWhenOwned()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Occult necklace", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, owned(OCCULT_NECKLACE, AMULET_OF_FURY), MissingGearBehavior.OMIT, false);

		assertEquals(1, r.getPicks().size());
		SlotPick pick = r.getPicks().get(0);
		assertEquals(OCCULT_NECKLACE, pick.getItemId());
		assertTrue(pick.isOwned());
		assertTrue(pick.isTopRecommendation());
	}

	@Test
	public void fallsBackToLowerRankedAlternative()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Occult necklace", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, owned(AMULET_OF_FURY), MissingGearBehavior.OMIT, false);

		SlotPick pick = r.getPicks().get(0);
		assertEquals(AMULET_OF_FURY, pick.getItemId());
		assertTrue(pick.isOwned());
		assertFalse(pick.isTopRecommendation());
	}

	@Test
	public void ownedVariantIdIsPlacedNotTheBaseId()
	{
		GearSetup s = setup(EquipSlot.BODY, new RankedItem("Karil's leathertop", 1, null));
		SetupResolution r = resolver.resolve(s, owned(KARILS_LEATHERTOP_100), MissingGearBehavior.OMIT, false);

		assertEquals(KARILS_LEATHERTOP_100, r.getPicks().get(0).getItemId());
	}

	@Test
	public void exactMatchBeatsOwnedVariant()
	{
		GearSetup s = setup(EquipSlot.BODY, new RankedItem("Karil's leathertop", 1, null));
		SetupResolution r = resolver.resolve(s,
			owned(KARILS_LEATHERTOP, KARILS_LEATHERTOP_100), MissingGearBehavior.OMIT, false);

		assertEquals(KARILS_LEATHERTOP, r.getPicks().get(0).getItemId());
	}

	@Test
	public void missingSlotOmittedByDefault()
	{
		GearSetup s = setup(EquipSlot.NECK, new RankedItem("Occult necklace", 1, null));
		SetupResolution r = resolver.resolve(s, Collections.emptySet(), MissingGearBehavior.OMIT, false);

		assertTrue(r.getPicks().isEmpty());
		assertEquals(Collections.singletonList(EquipSlot.NECK), r.getEmptySlots());
	}

	@Test
	public void missingSlotShowsGoalItemWhenConfigured()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Occult necklace", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED, false);

		assertTrue(r.getEmptySlots().isEmpty());
		SlotPick pick = r.getPicks().get(0);
		assertEquals("goal item is the best recommendation", OCCULT_NECKLACE, pick.getItemId());
		assertFalse(pick.isOwned());
	}

	@Test
	public void unresolvedNameIsRecordedAndSkipped()
	{
		GearSetup s = setup(EquipSlot.NECK,
			new RankedItem("Necklace of made-up power", 1, null),
			new RankedItem("Amulet of fury", 2, null));
		SetupResolution r = resolver.resolve(s, owned(AMULET_OF_FURY), MissingGearBehavior.OMIT, false);

		assertEquals(Collections.singletonList("Necklace of made-up power"),
			r.getUnresolvedNames().get(EquipSlot.NECK));
		assertEquals(AMULET_OF_FURY, r.getPicks().get(0).getItemId());
	}

	@Test
	public void allNamesUnresolvedLeavesSlotEmptyEvenWithGoalConfig()
	{
		GearSetup s = setup(EquipSlot.NECK, new RankedItem("Necklace of made-up power", 1, null));
		SetupResolution r = resolver.resolve(s, Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED, false);

		assertTrue(r.getPicks().isEmpty());
		assertEquals(Collections.singletonList(EquipSlot.NECK), r.getEmptySlots());
	}

	private static final int SLAYER_HELMET_I = 11865;
	private static final int ANCESTRAL_HAT = 21018;

	private OwnershipResolver slayerResolver()
	{
		return new OwnershipResolver(new ItemResolver(FakeItemNameIndex.of(
			SLAYER_HELMET_I, "Slayer helmet (i)",
			ANCESTRAL_HAT, "Ancestral hat")));
	}

	private static GearSetup slayerSetup()
	{
		return setup(EquipSlot.HEAD,
			new RankedItem("Slayer helmet (i)", 1, "If on a Zulrah slayer boss task"),
			new RankedItem("Ancestral hat", 1, null));
	}

	@Test
	public void slayerConditionalSkippedWhenOffTask()
	{
		SetupResolution r = slayerResolver().resolve(slayerSetup(),
			owned(SLAYER_HELMET_I, ANCESTRAL_HAT), MissingGearBehavior.OMIT, false);

		assertEquals(ANCESTRAL_HAT, r.getPicks().get(0).getItemId());
	}

	@Test
	public void slayerConditionalPickedWhenOnTask()
	{
		SetupResolution r = slayerResolver().resolve(slayerSetup(),
			owned(SLAYER_HELMET_I, ANCESTRAL_HAT), MissingGearBehavior.OMIT, true);

		assertEquals(SLAYER_HELMET_I, r.getPicks().get(0).getItemId());
	}

	@Test
	public void slayerConditionalNeverBecomesGoalItemOffTask()
	{
		SetupResolution r = slayerResolver().resolve(slayerSetup(),
			Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED, false);

		assertEquals("goal falls to the unconditional alternative",
			ANCESTRAL_HAT, r.getPicks().get(0).getItemId());
	}

	@Test
	public void informationalNotesAreNotTreatedAsSlayerConditional()
	{
		assertFalse(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Serpentine helm", 1, "Negates the need to bring anti-venom potions")));
		assertTrue(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Slayer helmet (i)", 1, "If on a slayer task")));
	}

	/**
	 * Pages write the task condition as a bare "(on task)" as often as they
	 * write a full "Only while on a Slayer task" footnote. Requiring the word
	 * "slayer" missed the short form on Graardor, KQ, Cerberus and Zulrah.
	 */
	@Test
	public void bareOnTaskParentheticalGatesLikeAFootnote()
	{
		assertTrue(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Slayer helmet (i)", 1, "on task")));
		assertTrue(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Slayer helmet (i)", 1, "if on task")));
		assertTrue(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Slayer helmet (i)", 1, "task only")));
		assertTrue(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Slayer helmet (i)", 1, "Only while on a Slayer task.")));
	}

	@Test
	public void unrelatedNotesAreNotTaskConditions()
	{
		assertFalse(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Serpentine helm", 1, "Better than the Neitiznot faceguard")));
		assertFalse(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Crystal helm", 1, "With full crystal and a bow of Faerdhinen.")));
		assertFalse(OwnershipResolver.isSlayerTaskConditional(
			new RankedItem("Torva full helm", 1, null)));
	}

	/**
	 * The four false positives from the full-corpus audit. Broadening the
	 * match to catch "(on task)" also caught notes that merely mention a task
	 * — and silently hiding a correct item is worse than the bug it replaced.
	 */
	@Test
	public void offTaskNoteIsNeverAGate()
	{
		// Dual macuahuitl on the Custodian stalker page — explicitly off-task
		assertFalse(OwnershipResolver.isSlayerTaskConditional(new RankedItem("Dual macuahuitl", 1,
			"With set effect, off-task. On a slayer task, using a Slayer helm and a slash weapon")));
	}

	@Test
	public void noteNamingADifferentTaskItemDoesNotGateThisItem()
	{
		// Amulet of rancour on KBD — the condition is about the helmet
		assertFalse(OwnershipResolver.isSlayerTaskConditional(new RankedItem("Amulet of rancour", 1,
			"Downgrade to an amulet of glory if bringing a Slayer helmet (i) on task.")));
		// Salve amulet (e) on Vorkath — the note is about the slayer helm's DPS
		assertFalse(OwnershipResolver.isSlayerTaskConditional(new RankedItem("Salve amulet (e)", 1,
			"DPS is slightly higher in max melee gear with slayer helm (i) and amulet")));
	}

	/** But a slayer item whose own note names it still gates. */
	@Test
	public void slayerHelmetWithSelfNamingNoteStillGates()
	{
		assertTrue(OwnershipResolver.isSlayerTaskConditional(new RankedItem("Slayer helmet (i)", 1,
			"Only on a Slayer task.")));
		assertTrue(OwnershipResolver.isSlayerTaskConditional(new RankedItem("Black mask (i)", 1,
			"On task only")));
	}

	@Test
	public void taskConditionWithAnOrEscapeHatchIsNotAGate()
	{
		// Toxic blowpipe on Royal Titans — a condition with an alternative
		assertFalse(OwnershipResolver.isSlayerTaskConditional(new RankedItem("Toxic blowpipe", 1,
			"If on task, or have 8 ranged strength aside from BP & darts, even Amethyst darts")));
	}

	@Test
	public void bareOnTaskMakesTheCheckboxAppear()
	{
		GearSetup s = setup(EquipSlot.HEAD,
			new RankedItem("Slayer helmet (i)", 1, "on task"),
			new RankedItem("Neitiznot faceguard", 2, null));
		assertTrue(OwnershipResolver.hasSlayerConditional(s));
	}

	/** The tooltip surfaces the wiki's own wording, one line per task item. */
	@Test
	public void slayerConditionalNotesPairItemsWithTheirWording()
	{
		GearSetup s = setup(EquipSlot.HEAD,
			new RankedItem("Slayer helmet (i)", 1, "When on a greater demon Slayer task."),
			new RankedItem("Neitiznot faceguard", 2, null));

		assertEquals(Collections.singletonList("Slayer helmet (i) — When on a greater demon Slayer task."),
			OwnershipResolver.slayerConditionalNotes(s));
	}

	@Test
	public void slayerConditionalNotesAreEmptyWhenNothingIsGated()
	{
		GearSetup s = setup(EquipSlot.HEAD, new RankedItem("Torva full helm", 1, null));
		assertTrue(OwnershipResolver.slayerConditionalNotes(s).isEmpty());
	}

	/**
	 * Fortis Colosseum lists a melee weapon, a bow of faerdhinen and a staff
	 * in one slot as a switch loadout. Only one can win the slot, but the
	 * crystal armour is still correct — the bow is carried.
	 */
	@Test
	public void crystalArmourSurvivesWhenTheBowIsASwitchThatLostTheSlot()
	{
		int fang = 26219;
		OwnershipResolver r = new OwnershipResolver(new ItemResolver(FakeItemNameIndex.of(
			CRYSTAL_BODY, "Crystal body", BOWFA, "Bow of faerdhinen", fang, "Osmumten's fang")));

		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(EquipSlot.WEAPON, Arrays.asList(
			new RankedItem("Osmumten's fang", 1, null),
			new RankedItem("Bow of faerdhinen", 1, null)));
		slots.put(EquipSlot.BODY, Collections.singletonList(new RankedItem("Crystal body", 1, null)));

		SetupResolution res = r.resolve(new GearSetup("Budget", slots),
			owned(CRYSTAL_BODY, BOWFA, fang), MissingGearBehavior.OMIT, false);

		boolean crystalKept = false;
		for (SlotPick pick : res.getPicks())
		{
			crystalKept |= pick.getItemId() == CRYSTAL_BODY;
		}
		assertTrue("crystal body should survive a carried bow of faerdhinen", crystalKept);
	}

	/** Armour is not swapped mid-fight, so those groups stay strict. */
	@Test
	public void armourSetGateIsStillStrict()
	{
		GearSetup s = setup(EquipSlot.BODY, new RankedItem("Crystal body", 1, null));
		assertTrue("no bow owned: crystal must go",
			setResolver().resolve(s, owned(CRYSTAL_BODY), MissingGearBehavior.OMIT, false)
				.getPicks().isEmpty());
	}
}
