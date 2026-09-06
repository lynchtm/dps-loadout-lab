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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Uses real item ids so the real ItemVariationMapping data (bundled with the
 * runelite client jar) drives variation expansion.
 */
public class ItemResolverTest
{
	// Barrows: karil's leathertop group = [4736, 4940..4944, 23632]
	private static final int KARILS_LEATHERTOP = 4736;
	private static final int KARILS_LEATHERTOP_100 = 4940;
	private static final int KARILS_LEATHERTOP_0 = 4944;
	private static final int KARILS_LEATHERTOP_LMS = 23632;

	// black mask group merges plain, charged, and imbued tiers
	private static final int BLACK_MASK = 8921;
	private static final int BLACK_MASK_10 = 8901;
	private static final int BLACK_MASK_I = 11784;
	private static final int BLACK_MASK_10_I = 11774;

	// slayer helmet group merges plain, imbued, and recolors
	private static final int SLAYER_HELMET = 11864;
	private static final int SLAYER_HELMET_I = 11865;
	private static final int BLACK_SLAYER_HELMET = 19639;
	private static final int BLACK_SLAYER_HELMET_I = 19641;

	// abyssal whip group includes recolors
	private static final int ABYSSAL_WHIP = 4151;
	private static final int VOLCANIC_ABYSSAL_WHIP = 12773;
	private static final int FROZEN_ABYSSAL_WHIP = 12774;

	private static ItemResolver resolver(Object... idNamePairs)
	{
		return new ItemResolver(FakeItemNameIndex.of(idNamePairs));
	}

	@Test
	public void degradationStatesAreAccepted()
	{
		ItemResolver r = resolver(
			KARILS_LEATHERTOP, "Karil's leathertop",
			KARILS_LEATHERTOP_100, "Karil's leathertop 100",
			KARILS_LEATHERTOP_0, "Karil's leathertop 0");
		ResolvedItem item = r.resolve("Karil's leathertop").orElseThrow(AssertionError::new);
		assertEquals(KARILS_LEATHERTOP, item.getPrimaryId());
		assertTrue(item.getAcceptableIds().contains(KARILS_LEATHERTOP_100));
		assertTrue(item.getAcceptableIds().contains(KARILS_LEATHERTOP_0));
	}

	@Test
	public void sameNameDuplicateIsAcceptedViaVariations()
	{
		ItemResolver r = resolver(
			KARILS_LEATHERTOP, "Karil's leathertop",
			KARILS_LEATHERTOP_LMS, "Karil's leathertop");
		ResolvedItem item = r.resolve("Karil's leathertop").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(KARILS_LEATHERTOP));
		assertTrue(item.getAcceptableIds().contains(KARILS_LEATHERTOP_LMS));
	}

	@Test
	public void lowerTierNeverSatisfiesImbuedRecommendation()
	{
		ItemResolver r = resolver(
			BLACK_MASK, "Black mask",
			BLACK_MASK_10, "Black mask (10)",
			BLACK_MASK_I, "Black mask (i)",
			BLACK_MASK_10_I, "Black mask (10) (i)");
		ResolvedItem item = r.resolve("Black mask (i)").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(BLACK_MASK_I));
		assertTrue("charged imbued satisfies imbued", item.getAcceptableIds().contains(BLACK_MASK_10_I));
		assertFalse("plain must not satisfy imbued", item.getAcceptableIds().contains(BLACK_MASK));
		assertFalse("charged plain must not satisfy imbued", item.getAcceptableIds().contains(BLACK_MASK_10));
	}

	@Test
	public void upgradeTierSatisfiesPlainRecommendation()
	{
		ItemResolver r = resolver(
			BLACK_MASK, "Black mask",
			BLACK_MASK_10, "Black mask (10)",
			BLACK_MASK_I, "Black mask (i)",
			BLACK_MASK_10_I, "Black mask (10) (i)");
		ResolvedItem item = r.resolve("Black mask").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(BLACK_MASK_10));
		assertTrue("imbued upgrade satisfies plain", item.getAcceptableIds().contains(BLACK_MASK_I));
		assertTrue(item.getAcceptableIds().contains(BLACK_MASK_10_I));
	}

	@Test
	public void recolorsSatisfyButPlainDoesNotSatisfyImbued()
	{
		ItemResolver r = resolver(
			SLAYER_HELMET, "Slayer helmet",
			SLAYER_HELMET_I, "Slayer helmet (i)",
			BLACK_SLAYER_HELMET, "Black slayer helmet",
			BLACK_SLAYER_HELMET_I, "Black slayer helmet (i)");
		ResolvedItem item = r.resolve("Slayer helmet (i)").orElseThrow(AssertionError::new);
		assertTrue("recolored imbued satisfies imbued", item.getAcceptableIds().contains(BLACK_SLAYER_HELMET_I));
		assertFalse("plain must not satisfy imbued", item.getAcceptableIds().contains(SLAYER_HELMET));
		assertFalse("recolored plain must not satisfy imbued", item.getAcceptableIds().contains(BLACK_SLAYER_HELMET));
	}

	@Test
	public void whipRecolorsAccepted()
	{
		ItemResolver r = resolver(
			ABYSSAL_WHIP, "Abyssal whip",
			VOLCANIC_ABYSSAL_WHIP, "Volcanic abyssal whip",
			FROZEN_ABYSSAL_WHIP, "Frozen abyssal whip");
		ResolvedItem item = r.resolve("Abyssal whip").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(VOLCANIC_ABYSSAL_WHIP));
		assertTrue(item.getAcceptableIds().contains(FROZEN_ABYSSAL_WHIP));
	}

	@Test
	public void nameMatchingIsCaseAndWhitespaceInsensitive()
	{
		ItemResolver r = resolver(ABYSSAL_WHIP, "Abyssal whip");
		assertTrue(r.resolve("  abyssal   WHIP ").isPresent());
	}

	@Test
	public void unknownNameResolvesEmpty()
	{
		ItemResolver r = resolver(ABYSSAL_WHIP, "Abyssal whip");
		assertFalse(r.resolve("Sword of a thousand truths").isPresent());
	}

	@Test
	public void wikiDisambiguatorSuffixIsRetriedWithoutParenthetical()
	{
		int houseTab = 8013;
		ItemResolver r = resolver(houseTab, "Teleport to house");
		ResolvedItem item = r.resolve("Teleport to house (tablet)").orElseThrow(AssertionError::new);
		assertEquals(houseTab, item.getPrimaryId());
	}

	@Test
	public void meaningfulParentheticalsAreNotStrippedWhenTheyResolve()
	{
		// "(i)" resolves directly — the fallback must not kick in and widen the match
		ItemResolver r = resolver(
			BLACK_MASK, "Black mask",
			BLACK_MASK_I, "Black mask (i)");
		assertEquals(BLACK_MASK_I, (int) r.resolve("Black mask (i)").orElseThrow(AssertionError::new).getPrimaryId());
	}

	@Test
	public void divineRunePouchSatisfiesRunePouchRecommendation()
	{
		ItemResolver r = resolver(
			12791, "Rune pouch",
			27281, "Divine rune pouch");
		ResolvedItem item = r.resolve("Rune pouch").orElseThrow(AssertionError::new);
		assertEquals(12791, item.getPrimaryId());
		assertTrue("divine upgrade satisfies rune pouch", item.getAcceptableIds().contains(27281));
	}

	@Test
	public void imbuedGodCapeGenericAcceptsAnyGodCape()
	{
		ItemResolver r = resolver(
			21791, "Imbued saradomin cape",
			21793, "Imbued guthix cape",
			21795, "Imbued zamorak cape");
		ResolvedItem item = r.resolve("Imbued god cape").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(21791));
		assertTrue(item.getAcceptableIds().contains(21793));
		assertTrue(item.getAcceptableIds().contains(21795));
	}

	@Test
	public void potionNameWithoutDoseResolvesToFourDose()
	{
		ItemResolver r = resolver(12625, "Stamina potion(4)");
		ResolvedItem item = r.resolve("Stamina potion").orElseThrow(AssertionError::new);
		assertEquals(12625, item.getPrimaryId());
	}

	@Test
	public void quantityMarkersAreStripped()
	{
		ItemResolver r = resolver(ABYSSAL_WHIP, "Abyssal whip", 385, "Shark");
		assertTrue(r.resolve("2 x Shark").isPresent());
		assertTrue(r.resolve("Shark x28").isPresent());
	}

	@Test
	public void pluralNameFallsBackToSingular()
	{
		ItemResolver r = resolver(385, "Shark");
		assertTrue(r.resolve("Sharks").isPresent());
	}

	@Test
	public void parenSpacingDifferencesAreNormalizedAway()
	{
		// the game says "Salve amulet(ei)", the wiki says "Salve amulet (ei)"
		ItemResolver r = resolver(12018, "Salve amulet(ei)");
		assertTrue(r.resolve("Salve amulet (ei)").isPresent());
	}

	@Test
	public void blazingBlowpipeSatisfiesToxicBlowpipe()
	{
		ItemResolver r = resolver(
			12926, "Toxic blowpipe",
			30374, "Blazing blowpipe");
		ResolvedItem item = r.resolve("Toxic blowpipe").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(30374));
	}

	@Test
	public void quiverSatisfiesAssemblerTransitivelyFromAccumulator()
	{
		ItemResolver r = resolver(
			10499, "Ava's accumulator",
			22109, "Ava's assembler",
			28955, "Blessed dizana's quiver");
		ResolvedItem item = r.resolve("Ava's accumulator").orElseThrow(AssertionError::new);
		assertTrue("assembler satisfies accumulator", item.getAcceptableIds().contains(22109));
		assertTrue("quiver satisfies accumulator via assembler", item.getAcceptableIds().contains(28955));
	}

	@Test
	public void unchargedTridentSatisfiesTridentOfTheSeas()
	{
		ItemResolver r = resolver(
			11905, "Trident of the seas",
			11908, "Uncharged trident");
		ResolvedItem item = r.resolve("Trident of the seas").orElseThrow(AssertionError::new);
		assertTrue(item.getAcceptableIds().contains(11908));
	}

	@Test
	public void slayerHelmetSatisfiesBlackMaskButNotImbued()
	{
		ItemResolver r = resolver(
			BLACK_MASK, "Black mask",
			BLACK_MASK_I, "Black mask (i)",
			SLAYER_HELMET, "Slayer helmet",
			SLAYER_HELMET_I, "Slayer helmet (i)");
		ResolvedItem plain = r.resolve("Black mask").orElseThrow(AssertionError::new);
		assertTrue(plain.getAcceptableIds().contains(SLAYER_HELMET));
		ResolvedItem imbued = r.resolve("Black mask (i)").orElseThrow(AssertionError::new);
		assertTrue(imbued.getAcceptableIds().contains(SLAYER_HELMET_I));
		assertFalse("plain slayer helm must not satisfy imbued mask",
			imbued.getAcceptableIds().contains(SLAYER_HELMET));
	}

	@Test
	public void ringOfSufferingRecoilImbuedSatisfiesAllLesserForms()
	{
		int plain = 19550;
		int imbued = 19710;
		int recoilImbued = 20655;
		ItemResolver r = resolver(
			plain, "Ring of suffering",
			imbued, "Ring of suffering (i)",
			recoilImbued, "Ring of suffering (ri)");

		assertTrue("(ri) satisfies plain",
			r.resolve("Ring of suffering").orElseThrow(AssertionError::new)
				.getAcceptableIds().contains(recoilImbued));
		assertTrue("(ri) satisfies (i)",
			r.resolve("Ring of suffering (i)").orElseThrow(AssertionError::new)
				.getAcceptableIds().contains(recoilImbued));
		assertFalse("plain must not satisfy (i)",
			r.resolve("Ring of suffering (i)").orElseThrow(AssertionError::new)
				.getAcceptableIds().contains(plain));
	}

	@Test
	public void divinePotionSatisfiesBasePotionButNotReverse()
	{
		int superCombat = 12695;
		int divineSuperCombat = 23685;
		ItemResolver r = resolver(
			superCombat, "Super combat potion(4)",
			divineSuperCombat, "Divine super combat potion(4)");

		assertTrue("divine satisfies base",
			r.resolve("Super combat potion(4)").orElseThrow(AssertionError::new)
				.getAcceptableIds().contains(divineSuperCombat));
		assertFalse("base must not satisfy divine",
			r.resolve("Divine super combat potion(4)").orElseThrow(AssertionError::new)
				.getAcceptableIds().contains(superCombat));
	}

	@Test
	public void anyImbuedGodCapeSatisfiesAnyOther()
	{
		int imbuedSara = 21791;
		int imbuedGuthix = 21793;
		int imbuedZamorak = 21795;
		ItemResolver r = resolver(
			imbuedSara, "Imbued saradomin cape",
			imbuedGuthix, "Imbued guthix cape",
			imbuedZamorak, "Imbued zamorak cape");

		// wiki example setups name the saradomin one; owning either other
		// imbued cape must count
		ResolvedItem sara = r.resolve("Imbued saradomin cape").orElseThrow(AssertionError::new);
		assertEquals("the named cape stays the primary/goal", imbuedSara, (int) sara.getPrimaryId());
		assertTrue(sara.getAcceptableIds().contains(imbuedGuthix));
		assertTrue(sara.getAcceptableIds().contains(imbuedZamorak));

		ResolvedItem zamorak = r.resolve("Imbued zamorak cape").orElseThrow(AssertionError::new);
		assertTrue("and the relation works in reverse", zamorak.getAcceptableIds().contains(imbuedSara));
	}

	@Test
	public void plainGodCapesAreInterchangeableButNeverSatisfyImbued()
	{
		int sara = 2412;
		int guthix = 2413;
		int imbuedSara = 21791;
		ItemResolver r = resolver(
			sara, "Saradomin cape",
			guthix, "Guthix cape",
			imbuedSara, "Imbued saradomin cape");

		ResolvedItem plain = r.resolve("Saradomin cape").orElseThrow(AssertionError::new);
		assertTrue("plain capes swap freely", plain.getAcceptableIds().contains(guthix));
		assertTrue("and an imbued upgrade still counts", plain.getAcceptableIds().contains(imbuedSara));

		ResolvedItem imbued = r.resolve("Imbued saradomin cape").orElseThrow(AssertionError::new);
		assertFalse("but a plain cape must not satisfy an imbued recommendation",
			imbued.getAcceptableIds().contains(sara));
		assertFalse(imbued.getAcceptableIds().contains(guthix));
	}

	@Test
	public void stripChargeDecorations()
	{
		assertEquals("black mask (i)", ItemNames.stripChargeDecorations("black mask (10) (i)"));
		assertEquals("karil's leathertop", ItemNames.stripChargeDecorations("karil's leathertop 100"));
		assertEquals("trident of the swamp (e)", ItemNames.stripChargeDecorations("trident of the swamp (e)"));
	}
}
