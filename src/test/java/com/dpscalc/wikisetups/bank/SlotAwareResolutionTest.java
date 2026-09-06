package com.dpscalc.wikisetups.bank;

import com.dpscalc.wikisetups.WikiSetupOptions.MissingGearBehavior;
import com.dpscalc.wikisetups.items.FakeItemNameIndex;
import com.dpscalc.wikisetups.items.ItemResolver;
import com.dpscalc.wikisetups.items.ResolvedItem;
import com.dpscalc.wikisetups.wiki.EquipSlot;
import com.dpscalc.wikisetups.wiki.GearSetup;
import com.dpscalc.wikisetups.wiki.RankedItem;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Slot-aware checks for generic Wiki item families. */
public class SlotAwareResolutionTest
{
	@Test
	public void filteredGenericFamilySkipsDisallowedExactAndUsesAllowedTier()
	{
		ItemResolver resolver = new ItemResolver(FakeItemNameIndex.of(
			1001, "Rada's blessing",
			1002, "Rada's blessing 4"));

		ResolvedItem result = resolver.resolve("Rada's blessing", id -> id == 1002)
			.orElseThrow(AssertionError::new);

		assertEquals(1002, (int) result.getPrimaryId());
	}

	@Test
	public void warmClothingUsesItemAllowedForDestinationSlot()
	{
		ItemResolver resolver = new ItemResolver(FakeItemNameIndex.of(
			20720, "Bruma torch",
			1003, "Woolly scarf"));

		ResolvedItem result = resolver.resolve("Warm clothing", id -> id == 1003)
			.orElseThrow(AssertionError::new);

		assertEquals(1003, (int) result.getPrimaryId());
	}

	@Test
	public void ownershipResolverCannotPickGenericItemFromWrongSlot()
	{
		OwnershipResolver resolver = new OwnershipResolver(
			new ItemResolver(FakeItemNameIndex.of(
				20720, "Bruma torch",
				1003, "Woolly scarf")),
			(slot, id) -> slot == EquipSlot.NECK && id == 1003);
		GearSetup setup = new GearSetup(
			"Neck",
			Collections.singletonMap(
				EquipSlot.NECK,
				Collections.singletonList(new RankedItem("Warm clothing", 1, null))));

		SetupResolution result = resolver.resolve(
			setup, Collections.emptySet(), MissingGearBehavior.SHOW_BEST_UNOWNED, false);

		assertEquals(1, result.getPicks().size());
		assertEquals(1003, result.getPicks().get(0).getItemId());
	}
}
