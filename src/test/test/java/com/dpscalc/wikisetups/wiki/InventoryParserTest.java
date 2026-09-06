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
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class InventoryParserTest
{
	private final InventoryParser parser = new InventoryParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = InventoryParserTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	public void zulrahInventoriesParseWithTabberLabels() throws IOException
	{
		List<InventorySetup> setups = parser.parse(fixture("zulrah-strategies"));
		assertTrue("expected multiple inventory setups", setups.size() >= 2);
		assertEquals("Max Magic & Ranged", setups.get(0).getLabel());
		assertEquals("Void Mage and Ranged", setups.get(1).getLabel());
	}

	/**
	 * A fresh clone on Windows checks fixtures out with CRLF; line-anchored
	 * patterns must not capture the stray carriage return.
	 */
	@Test
	public void crlfInputParsesIdenticallyToLf() throws IOException
	{
		String lf = fixture("zulrah-strategies");
		List<InventorySetup> fromLf = parser.parse(lf);
		List<InventorySetup> fromCrlf = parser.parse(lf.replace("\n", "\r\n"));

		assertEquals(fromLf.size(), fromCrlf.size());
		for (int i = 0; i < fromLf.size(); i++)
		{
			assertEquals(fromLf.get(i).getLabel(), fromCrlf.get(i).getLabel());
			assertEquals(fromLf.get(i).getSlots(), fromCrlf.get(i).getSlots());
			assertEquals(fromLf.get(i).getRunes(), fromCrlf.get(i).getRunes());
		}
	}

	@Test
	public void slotsPreserveWikiOrder() throws IOException
	{
		InventorySetup first = parser.parse(fixture("zulrah-strategies")).get(0);
		assertEquals(28, first.getSlots().size());
		assertEquals("Twisted bow", first.getSlots().get(0));
		assertEquals("Toxic blowpipe", first.getSlots().get(8));
		assertEquals("Divine rune pouch", first.getSlots().get(27));
	}

	@Test
	public void placeholderTemplatesBecomeTheirName() throws IOException
	{
		// slot 11 is {{Cheap food}}
		InventorySetup first = parser.parse(fixture("zulrah-strategies")).get(0);
		assertEquals("Cheap food", first.getSlots().get(10));
	}

	@Test
	public void pageWithoutInventoriesParsesEmpty() throws IOException
	{
		assertTrue(parser.parse(fixture("dragon-slayer-ii")).isEmpty());
	}

	@Test
	public void headingLabelUsedOutsideTabber()
	{
		List<InventorySetup> setups = parser.parse(
			"==Suggested inventory==\n{{Inventory|1=Shark|2=Shark}}");
		assertEquals(1, setups.size());
		assertEquals("Suggested inventory", setups.get(0).getLabel());
		assertEquals("Shark", setups.get(0).getSlots().get(0));
	}

	@Test
	public void emptyInventoryTemplateIsDropped()
	{
		assertTrue(parser.parse("{{Inventory|align=right}}").isEmpty());
	}

	@Test
	public void gotrPositionalSlotsAndSameLinePaneLabelsParse() throws IOException
	{
		List<InventorySetup> setups = parser.parse(fixture("gotr-strategies"));
		assertEquals(4, setups.size());
		assertEquals("Entry-level", setups.get(0).getLabel());
		assertEquals("Combination runes", setups.get(1).getLabel());

		InventorySetup entry = setups.get(0);
		assertEquals("Giant pouch", entry.getSlots().get(0));
		assertEquals("Rune pouch", entry.getSlots().get(3));
		assertEquals("Small pouch", entry.getSlots().get(4));
		assertEquals("", entry.getSlots().get(6));
	}

	@Test
	public void runePouchAttachesToPrecedingInventory() throws IOException
	{
		InventorySetup entry = parser.parse(fixture("gotr-strategies")).get(0);
		assertEquals(java.util.Arrays.asList("Air rune", "Cosmic rune", "Astral rune"), entry.getRunes());
	}

	@Test
	public void zulrahRunePouchAttaches() throws IOException
	{
		InventorySetup first = parser.parse(fixture("zulrah-strategies")).get(0);
		assertEquals(java.util.Arrays.asList("Fire rune", "Blood rune", "Cosmic rune"), first.getRunes());
	}

	/**
	 * {{Rune pouch}} writes stack sizes after a backslash. Keeping them made
	 * every rune on nine of the pages sampled fail to resolve.
	 */
	@Test
	public void runeStackSizesAreStrippedFromNames() throws IOException
	{
		int named = 0;
		for (InventorySetup setup : parser.parse(fixture("general-graardor-strategies")))
		{
			for (String rune : setup.getRunes())
			{
				assertFalse("stack size left on '" + rune + "'", rune.contains("\\"));
				named++;
			}
		}
		assertTrue("expected this page's rune pouches to parse, got none", named >= 2);
	}

	@Test
	public void quantitiesAreStrippedFromInventorySlotsToo()
	{
		assertEquals("Blisterwood stake", InventoryParser.stripQuantity("Blisterwood stake\\12"));
		assertEquals("Ruby dragon bolts (e)", InventoryParser.stripQuantity("Ruby dragon bolts (e)\\500"));
		assertEquals("Shark", InventoryParser.stripQuantity("Shark"));
	}

	/** ";n" is the noted marker; a value can carry both markers at once. */
	@Test
	public void notedMarkerIsStripped()
	{
		assertEquals("Iron ore", InventoryParser.stripQuantity("Iron ore;n"));
		assertEquals("Adamant bar", InventoryParser.stripQuantity("Adamant bar;n"));
		assertEquals("Shark", InventoryParser.stripQuantity("Shark\\200;n"));
		assertEquals("Antidote++", InventoryParser.stripQuantity("Antidote++\\50;n"));
	}

	/** "Placeholder" is a deliberately empty grid cell, not an item. */
	@Test
	public void placeholderLiteralBecomesAnEmptySlot()
	{
		assertEquals("", InventoryParser.cleanSlotValue("Placeholder"));
		assertEquals("", InventoryParser.cleanSlotValue("placeholder"));
	}

	/**
	 * These pages head every single inventory "==Inventory==", so the nearest
	 * heading tells the reader nothing. The pane label further back does.
	 */
	@Test
	public void genericHeadingsFallBackToTheTabberPaneLabel() throws IOException
	{
		List<InventorySetup> setups = parser.parse(fixture("general-graardor-strategies"));
		assertEquals(8, setups.size());

		List<String> labels = new java.util.ArrayList<>();
		for (InventorySetup setup : setups)
		{
			assertNotEquals("generic heading used as a label", "Inventory", setup.getLabel());
			labels.add(setup.getLabel());
		}
		assertEquals("labels should be distinguishable", labels.size(),
			new java.util.HashSet<>(labels).size());
		assertEquals("Melee", labels.get(0));
		assertEquals("Magic", labels.get(1));
		assertEquals("Ranged", labels.get(2));
	}

	/** A template that lists its own options becomes that list, not its name. */
	@Test
	public void choiceTemplatesBecomeTheirCandidateList()
	{
		assertEquals("Manta ray / Tuna potato / Anglerfish / Dark crab",
			InventoryParser.cleanSlotValue(
				"{{MinPrice|format=item|link=n|Manta ray|Tuna potato|Anglerfish|Dark crab}}"));
	}

	@Test
	public void placeholderTemplatesWithNoOptionsKeepTheirName()
	{
		assertEquals("Cheap food", InventoryParser.cleanSlotValue("{{Cheap food}}"));
	}

	@Test
	public void muspahChoiceSlotsResolveToOptionsNotTemplateNames() throws IOException
	{
		boolean sawChoice = false;
		for (InventorySetup setup : parser.parse(fixture("phantom-muspah-strategies")))
		{
			for (String slot : setup.getSlots())
			{
				assertNotEquals("raw template name left in a slot", "MinPrice", slot);
				sawChoice |= slot.contains(InventoryParser.CHOICE_SEPARATOR);
			}
		}
		assertTrue("expected at least one choice slot on this page", sawChoice);
	}
}
