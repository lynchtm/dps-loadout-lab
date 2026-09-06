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
package com.dpscalc.wikisetups.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GearSetupParserTest
{
	private final GearSetupParser parser = new GearSetupParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = GearSetupParserTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	public void efnWithNamedParamStillYieldsNote()
	{
		// exact head2 syntax from Amoxliatl/Strategies — the |name= param must
		// not displace the positional note text
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Melee"
			+ "|head2={{plink|Blood moon helm}}{{efn|Best used with the full [[Blood moon armour]] set and [[Dual macuahuitl]]|name=Blood moon}}}}");

		RankedItem helm = setups.get(0).getSlots().get(EquipSlot.HEAD).get(0);
		assertEquals("Blood moon helm", helm.getName());
		assertNotNull(helm.getNote());
		assertTrue(helm.getNote().contains("full"));
		assertTrue(helm.getNote().contains("set"));
	}

	@Test
	public void pageAnchorLinkUsesDisplayTextAsTheItemName()
	{
		// exact cape1 syntax from CoX and ToA: the link target is a page
		// section, the real item name is in txt=
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Magic"
			+ "|cape1 = {{plink|God capes#Imbuing|txt=Imbued god cape|pic=Imbued saradomin cape}}}}");

		RankedItem cape = setups.get(0).getSlots().get(EquipSlot.CAPE).get(0);
		assertEquals("Imbued god cape", cape.getName());
		assertTrue("the picture is kept as a further fallback",
			cape.getFallbackNames().contains("Imbued saradomin cape"));
	}

	@Test
	public void pageAnchorWithoutDisplayTextFallsBackToThePageName()
	{
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Magic"
			+ "|cape1 = {{plink|God capes#Imbuing}}}}");

		assertEquals("God capes", setups.get(0).getSlots().get(EquipSlot.CAPE).get(0).getName());
	}

	@Test
	public void ordinaryLinkKeepsItsPositionalNameAndOffersTheRest()
	{
		// a shortened display text must not displace a perfectly good item name
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Ranged"
			+ "|weapon1 = {{plink|Toxic blowpipe|txt=blowpipe|pic=Toxic blowpipe (empty)}}}}");

		RankedItem weapon = setups.get(0).getSlots().get(EquipSlot.WEAPON).get(0);
		assertEquals("Toxic blowpipe", weapon.getName());
		assertEquals(Arrays.asList("blowpipe", "Toxic blowpipe (empty)"), weapon.getFallbackNames());
	}

	@Test
	public void footnotesSurviveOnItemsThatCarryFallbackNames()
	{
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Magic"
			+ "|cape1 = {{plink|God capes#Imbuing|txt=Imbued god cape}}{{efn|Any god cape works}}}}");

		RankedItem cape = setups.get(0).getSlots().get(EquipSlot.CAPE).get(0);
		assertEquals("Imbued god cape", cape.getName());
		assertEquals("Any god cape works", cape.getNote());
		assertEquals(Arrays.asList("God capes"), cape.getFallbackNames());
	}

	@Test
	public void noFixtureYieldsAPageAnchorAsAnItemName() throws IOException
	{
		for (String page : new String[]{"cox-strategies", "toa-strategies", "zulrah-strategies",
			"vorkath-strategies", "alchemical-hydra-strategies", "gotr-strategies"})
		{
			for (GearSetup setup : parser.parse(fixture(page)))
			{
				for (List<RankedItem> slot : setup.getSlots().values())
				{
					for (RankedItem item : slot)
					{
						assertFalse(page + " produced a page link as an item: " + item.getName(),
							item.getName().contains("#"));
					}
				}
			}
		}
	}

	@Test
	public void coxCapeSlotResolvesToTheImbuedGodCape() throws IOException
	{
		// the live bug: cape1 is {{plink|God capes#Imbuing|txt=Imbued god cape|...}}
		boolean found = false;
		for (GearSetup setup : parser.parse(fixture("cox-strategies")))
		{
			List<RankedItem> cape = setup.getSlots().get(EquipSlot.CAPE);
			if (cape != null && cape.get(0).getName().equalsIgnoreCase("Imbued god cape"))
			{
				found = true;
				assertTrue(cape.get(0).getFallbackNames().contains("Imbued saradomin cape"));
			}
		}
		assertTrue("expected a cape slot naming the imbued god cape", found);
	}

	@Test
	public void trouverParchmentCompanionIsNotAnAlternative()
	{
		// exact body1 syntax from Calvar'ion/Strategies: the "+{{plinkp|...}}"
		// marks a companion item to lock the gear, not wearable equipment
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Melee"
			+ "|body1={{plink|Fighter torso}}{{efn|name=break}}+{{plinkp|Trouver parchment}}{{efn|ITEM WILL BE LOST|name=trouver}}"
			+ "|body2={{plink|Black d'hide body}}}}");

		List<RankedItem> body = setups.get(0).getSlots().get(EquipSlot.BODY);
		assertEquals(2, body.size());
		assertEquals("Fighter torso", body.get(0).getName());
		assertEquals("Black d'hide body", body.get(1).getName());
	}

	@Test
	public void zulrahParsesAllTabberSetups() throws IOException
	{
		List<GearSetup> setups = parser.parse(fixture("zulrah-strategies"));
		assertEquals(4, setups.size());
		assertEquals("Magic", setups.get(0).getStyleLabel());
		assertEquals("Ranged", setups.get(1).getStyleLabel());
		assertEquals("Magic (Diary kill)", setups.get(3).getStyleLabel());
	}

	/** Each setup keeps the tabber pane it sat under, for when style is weak. */
	@Test
	public void gemstoneCrabSetupsCarryTheirPaneLabels() throws IOException
	{
		List<GearSetup> setups = parser.parse(fixture("gemstone-crab-strategies"));
		// four melee setups all styled "Melee"/"Melee (Low level)" but panes differ
		List<String> panes = new java.util.ArrayList<>();
		for (GearSetup s : setups)
		{
			panes.add(s.getPaneLabel());
		}
		assertTrue("expected distinct pane labels, got " + panes,
			panes.contains("Dharok's") && panes.contains("Obsidian") && panes.contains("Blood moon"));
	}

	@Test
	public void paneLabelIsNullWithoutATabber()
	{
		List<GearSetup> setups = parser.parse(
			"{{Recommended equipment|style=Melee|head1={{plink|Torva full helm}}}}");
		assertNull(setups.get(0).getPaneLabel());
	}

	@Test
	public void zulrahMagicSlotContents() throws IOException
	{
		GearSetup magic = parser.parse(fixture("zulrah-strategies")).get(0);

		List<RankedItem> neck = magic.getSlots().get(EquipSlot.NECK);
		assertEquals("Occult necklace", neck.get(0).getName());
		assertEquals(1, neck.get(0).getRank());

		// head1 = Slayer helmet (i) {{efn|...}} / Ancestral hat
		List<RankedItem> head = magic.getSlots().get(EquipSlot.HEAD);
		assertEquals("Slayer helmet (i)", head.get(0).getName());
		assertEquals("If on a Zulrah slayer boss task", head.get(0).getNote());
		assertEquals("Ancestral hat", head.get(1).getName());
		assertEquals(1, head.get(1).getRank());
		assertNull(head.get(1).getNote());

		// ranks are ascending
		int prev = 0;
		for (RankedItem item : head)
		{
			assertTrue(item.getRank() >= prev);
			prev = item.getRank();
		}
	}

	@Test
	public void efnBackReferenceWithoutTextLeavesNoteNull() throws IOException
	{
		// magic weapon4: {{plink|Smoke battlestaff}}{{efn|name=fire}}
		GearSetup magic = parser.parse(fixture("zulrah-strategies")).get(0);
		RankedItem smokeStaff = magic.getSlots().get(EquipSlot.WEAPON).stream()
			.filter(i -> i.getName().equals("Smoke battlestaff"))
			.findFirst()
			.orElseThrow(AssertionError::new);
		assertNull(smokeStaff.getNote());
	}

	@Test
	public void noteMarkupIsStripped() throws IOException
	{
		// ranged head2: {{efn|With full [[crystal armour]] and [[bow of faerdhinen]]|name=crystal}}
		GearSetup ranged = parser.parse(fixture("zulrah-strategies")).get(1);
		RankedItem crystalHelm = ranged.getSlots().get(EquipSlot.HEAD).stream()
			.filter(i -> i.getName().equals("Crystal helm"))
			.findFirst()
			.orElseThrow(AssertionError::new);
		assertEquals("With full crystal armour and bow of faerdhinen", crystalHelm.getNote());
	}

	@Test
	public void plinkPicParameterIsNotTheItemName() throws IOException
	{
		// ranged head4: {{plink|Blessed coif|pic=Ancient coif}}
		GearSetup ranged = parser.parse(fixture("zulrah-strategies")).get(1);
		assertTrue(ranged.getSlots().get(EquipSlot.HEAD).stream()
			.anyMatch(i -> i.getName().equals("Blessed coif")));
	}

	@Test
	public void orBrSeparatorYieldsBothItems() throws IOException
	{
		// ranged head1 = {{plink|Slayer helmet (i)}} or <br> {{plink|Masori mask (f)}}
		GearSetup ranged = parser.parse(fixture("zulrah-strategies")).get(1);
		List<RankedItem> head = ranged.getSlots().get(EquipSlot.HEAD);
		assertEquals("Slayer helmet (i)", head.get(0).getName());
		assertEquals("Masori mask (f)", head.get(1).getName());
	}

	@Test
	public void vorkathParsesTwoSetups() throws IOException
	{
		assertEquals(2, parser.parse(fixture("vorkath-strategies")).size());
	}

	@Test
	public void coxParsesAllFifteenSetups() throws IOException
	{
		assertEquals(15, parser.parse(fixture("cox-strategies")).size());
	}

	@Test
	public void alchemicalHydraParsesTwoSetups() throws IOException
	{
		assertEquals(2, parser.parse(fixture("alchemical-hydra-strategies")).size());
	}

	@Test
	public void questPageWithoutTemplateParsesEmpty() throws IOException
	{
		assertTrue(parser.parse(fixture("dragon-slayer-ii")).isEmpty());
	}

	@Test
	public void emptyAndNullInput()
	{
		assertTrue(parser.parse(null).isEmpty());
		assertTrue(parser.parse("").isEmpty());
	}

	@Test
	public void unclosedTemplateDoesNotCrash()
	{
		assertTrue(parser.parse("{{Recommended equipment|head1 = {{plink|Foo}").isEmpty());
	}

	@Test
	public void missingStyleFallsBackToSetupIndex()
	{
		List<GearSetup> setups = parser.parse(
			"{{Recommended equipment|head1 = {{plink|Bronze med helm}}}}");
		assertEquals(1, setups.size());
		assertEquals("Setup 1", setups.get(0).getStyleLabel());
	}

	@Test
	public void unknownSlotAndNaAreIgnored()
	{
		List<GearSetup> setups = parser.parse(
			"{{Recommended equipment"
				+ "|style = Melee"
				+ "|foo1 = {{plink|Not a slot}}"
				+ "|head1 = {{plink|N/A}}"
				+ "|weapon1 = {{plink|Abyssal whip}}"
				+ "}}");
		assertEquals(1, setups.size());
		GearSetup s = setups.get(0);
		assertNull(s.getSlots().get(EquipSlot.HEAD));
		assertEquals(1, s.getSlots().size());
		assertEquals("Abyssal whip", s.getSlots().get(EquipSlot.WEAPON).get(0).getName());
	}

	@Test
	public void templateWithNoItemsIsDropped()
	{
		assertTrue(parser.parse("{{Recommended equipment|style = Magic}}").isEmpty());
	}

	@Test
	public void splitTopLevelRespectsNesting()
	{
		List<String> parts = GearSetupParser.splitTopLevel(
			"a|{{x|y}}|[[p|q]]|b", '|');
		assertEquals(4, parts.size());
		assertEquals("{{x|y}}", parts.get(1));
		assertEquals("[[p|q]]", parts.get(2));
	}

	/**
	 * A spaced plus means "and bring this too" — Fortis Colosseum lists a
	 * whole switch loadout that way. Treating it like the attached companion
	 * notation silently dropped the bow of faerdhinen, sanguinesti staff and
	 * venator bow out of the weapon slot, and the crystal set out of the rest.
	 */
	@Test
	public void spacedPlusListsExtraGearRatherThanACompanion()
	{
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Budget"
			+ "|weapon1={{plink|Osmumten's fang}} / {{plink|Noxious halberd}}"
			+ " + {{plink|Bow of faerdhinen}} + {{plink|Sanguinesti staff}}"
			+ "|legs1={{plink|Crystal legs}}}}");

		List<String> weapons = new java.util.ArrayList<>();
		for (RankedItem item : setups.get(0).getSlots().get(EquipSlot.WEAPON))
		{
			weapons.add(item.getName());
		}
		assertEquals(Arrays.asList("Osmumten's fang", "Noxious halberd",
			"Bow of faerdhinen", "Sanguinesti staff"), weapons);
	}

	/**
	 * Roughly half the pages qualify a recommendation with a bare
	 * parenthetical instead of an {{efn}}. Without capturing it, the slayer
	 * helmet was recommended unconditionally and the "On a slayer task"
	 * checkbox never appeared.
	 */
	@Test
	public void trailingParentheticalBecomesTheNote()
	{
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Melee"
			+ "|head1={{plink|Slayer helmet (i)}} (on task)"
			+ "|head2={{plink|Neitiznot faceguard}}}}");

		List<RankedItem> head = setups.get(0).getSlots().get(EquipSlot.HEAD);
		assertEquals("on task", head.get(0).getNote());
		assertNull("an unqualified item keeps no note", head.get(1).getNote());
	}

	@Test
	public void separatorsAndOpenParensAreNotNotes()
	{
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Melee"
			+ "|feet1={{plink|Echo boots}}/<br/>{{plink|Guardian boots}}"
			+ "|head1={{plink|Ahrim's hood}} (or:{{plinkp|Blue moon helm}})}}");

		for (RankedItem item : setups.get(0).getSlots().get(EquipSlot.FEET))
		{
			assertNull("a separator is not a note", item.getNote());
		}
		assertNull(setups.get(0).getSlots().get(EquipSlot.HEAD).get(0).getNote());
	}

	@Test
	public void refBodiesBecomeNotesToo()
	{
		List<GearSetup> setups = parser.parse("{{Recommended equipment|style=Melee"
			+ "|head1={{plink|Slayer helmet (i)}}<ref>Only when on slayer task.</ref>}}");

		assertEquals("Only when on slayer task.",
			setups.get(0).getSlots().get(EquipSlot.HEAD).get(0).getNote());
	}
}
