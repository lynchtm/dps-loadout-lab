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
package com.dpscalc.wikisetups.items;

import java.util.List;
import java.util.Optional;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Names the wiki uses that no item carries. Each of these was found failing
 * to resolve on a real page, so the point is that the generic name reaches
 * the concrete items — not which one wins, which is ownership's job.
 */
public class ItemAliasesTest
{
	@Test
	public void blessedDhideAcceptsEveryGodsVersion()
	{
		List<String> body = ItemAliases.genericFor("Blessed d'hide body");
		assertTrue(body.contains("Guthix d'hide body"));
		assertTrue(body.contains("Armadyl d'hide body"));
		assertEquals("all six gods", 6, body.size());
	}

	/** Pages write the same slot half a dozen ways; all of them must land. */
	@Test
	public void everySpellingOfAGodDhideSlotResolvesToTheSameItems()
	{
		List<String> canonical = ItemAliases.genericFor("Blessed d'hide body");
		assertEquals(canonical, ItemAliases.genericFor("Blessed body"));
		assertEquals(canonical, ItemAliases.genericFor("Blessed dragonhide body"));

		List<String> bracers = ItemAliases.genericFor("Blessed bracers");
		assertEquals(bracers, ItemAliases.genericFor("Blessed vambraces"));
		assertFalse(bracers.isEmpty());
	}

	/** "{{plink|Blessed_bracers}}" — a URL-style link must still normalize. */
	@Test
	public void underscoresInLinksNormalizeToSpaces()
	{
		assertEquals(ItemNames.normalize("Blessed bracers"), ItemNames.normalize("Blessed_bracers"));
		assertFalse(ItemAliases.genericFor("Blessed_bracers").isEmpty());
	}

	@Test
	public void blessingsCoverTheSixGods()
	{
		assertEquals(6, ItemAliases.genericFor("Blessing").size());
		assertTrue(ItemAliases.genericFor("Blessing").contains("Holy blessing"));
	}

	/** Diary rewards are numbered in game; best tier first so it's preferred. */
	@Test
	public void diaryRewardsListTheirTiersHighestFirst()
	{
		assertEquals(java.util.Arrays.asList(
			"Ardougne cloak 4", "Ardougne cloak 3", "Ardougne cloak 2", "Ardougne cloak 1"),
			ItemAliases.genericFor("Ardougne cloak"));
		assertEquals("Explorer's ring 4", ItemAliases.genericFor("Explorer's ring").get(0));
		assertEquals("Rada's blessing 4", ItemAliases.genericFor("Rada's blessing").get(0));
	}

	@Test
	public void wintertodtWarmClothingHasConcreteCandidates()
	{
		List<String> warm = ItemAliases.genericFor("Warm clothing");
		assertTrue(warm.contains("Bruma torch"));
		assertTrue(warm.contains("Warm gloves"));
	}

	@Test
	public void unknownNamesStayUnknown()
	{
		assertTrue(ItemAliases.genericFor("Twisted bow").isEmpty());
	}

	/** Generic names only apply when the exact name failed, so nothing regresses. */
	@Test
	public void aRealItemNameStillResolvesExactly()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			11826, "Armadyl helmet", 10370, "Guthix d'hide body");
		ItemResolver resolver = new ItemResolver(index);

		Optional<ResolvedItem> exact = resolver.resolve("Armadyl helmet");
		assertTrue(exact.isPresent());
		assertEquals(11826, exact.get().getPrimaryId());
	}

	@Test
	public void aGenericNameResolvesThroughToAConcreteItem()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(10370, "Guthix d'hide body");
		ItemResolver resolver = new ItemResolver(index);

		Optional<ResolvedItem> resolved = resolver.resolve("Blessed d'hide body");
		assertTrue("generic name should reach the concrete item", resolved.isPresent());
		assertTrue(resolved.get().getAcceptableIds().contains(10370));
	}

	/** "God blessing" is another family name for the same six items. */
	@Test
	public void godBlessingResolvesToAConcreteBlessing()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(20220, "Holy blessing");
		ItemResolver resolver = new ItemResolver(index);
		assertTrue(resolver.resolve("God blessing").isPresent());
	}

	/** A bare trailing count and a leading "Open " both fall back to the item. */
	@Test
	public void quantityAndOpenStatePrefixFallBackToTheItem()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(995, "Coins", 13226, "Herb sack");
		ItemResolver resolver = new ItemResolver(index);

		assertEquals(995, resolver.resolve("Coins 10000").get().getPrimaryId());
		assertEquals(13226, resolver.resolve("Open herb sack").get().getPrimaryId());
	}

	/** The strips are last-resort: a genuinely numbered item resolves as itself. */
	@Test
	public void numberedItemsStillResolveExactlyFirst()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			13124, "Ardougne cloak 4", 13123, "Ardougne cloak 3");
		ItemResolver resolver = new ItemResolver(index);
		assertEquals(13124, resolver.resolve("Ardougne cloak 4").get().getPrimaryId());
	}

	/**
	 * Ornament kits and Echo recolours are cosmetic — owning the decorated
	 * version satisfies a recommendation for the base item.
	 */
	@Test
	public void echoRecolourSatisfiesTheBaseItem()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			26241, "Virtus mask", 28001, "Echo virtus mask");
		ItemResolver resolver = new ItemResolver(index);
		assertTrue("owned Echo virtus mask should satisfy a Virtus mask rec",
			resolver.resolve("Virtus mask").get().getAcceptableIds().contains(28001));
	}

	@Test
	public void ornamentKitSuffixSatisfiesTheBaseItem()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			6585, "Amulet of fury", 12436, "Amulet of fury (or)");
		ItemResolver resolver = new ItemResolver(index);
		assertTrue(resolver.resolve("Amulet of fury").get().getAcceptableIds().contains(12436));
	}

	/** Gilded, trimmed, heraldic and Deadman are all cosmetic same-tier copies. */
	@Test
	public void gildedTrimmedHeraldicAndDeadmanSatisfyTheBase()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			1163, "Rune full helm",
			2619, "Rune full helm (g)",
			2627, "Rune full helm (t)",
			10306, "Rune full helm (h1)",
			11838, "Bandos godsword",
			30000, "Bandos godsword (deadman)");
		ItemResolver resolver = new ItemResolver(index);

		java.util.Set<Integer> helm = resolver.resolve("Rune full helm").get().getAcceptableIds();
		assertTrue("gilded", helm.contains(2619));
		assertTrue("trimmed", helm.contains(2627));
		assertTrue("heraldic", helm.contains(10306));
		assertTrue("deadman", resolver.resolve("Bandos godsword").get().getAcceptableIds().contains(30000));
	}

	/** Dyes are listed explicitly because their prefixes are ordinary words. */
	@Test
	public void dyedVariantsSatisfyTheBaseItem()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(
			26233, "Ancient sceptre", 28262, "Shadow ancient sceptre",
			26382, "Torva full helm", 27622, "Sanguine torva full helm",
			22486, "Scythe of vitur", 28338, "Holy scythe of vitur");
		ItemResolver resolver = new ItemResolver(index);

		assertTrue(resolver.resolve("Ancient sceptre").get().getAcceptableIds().contains(28262));
		assertTrue(resolver.resolve("Torva full helm").get().getAcceptableIds().contains(27622));
		assertTrue(resolver.resolve("Scythe of vitur").get().getAcceptableIds().contains(28338));
	}

	/** The base item still resolves as itself — no regression from the extra lookups. */
	@Test
	public void baseItemsAreUnaffectedWhenNoOrnamentExists()
	{
		FakeItemNameIndex index = FakeItemNameIndex.of(26241, "Virtus mask");
		ItemResolver resolver = new ItemResolver(index);
		assertEquals(26241, resolver.resolve("Virtus mask").get().getPrimaryId());
	}
}
