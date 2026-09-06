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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExampleSetupParserTest
{
	private final ExampleSetupParser parser = new ExampleSetupParser();

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = ExampleSetupParserTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext"))
		{
			assertNotNull("missing fixture " + name, in);
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static ExampleSetup byLabelPrefix(List<ExampleSetup> setups, String prefix)
	{
		for (ExampleSetup setup : setups)
		{
			if (setup.getLabel().startsWith(prefix))
			{
				return setup;
			}
		}
		throw new AssertionError("no setup labeled '" + prefix + "…' among " + setups.size());
	}

	@Test
	public void toaExampleColumnsParseWithPairedInventory() throws IOException
	{
		List<ExampleSetup> setups = parser.parse(fixture("toa-strategies"));
		assertTrue("expected at least the 5 example columns, got " + setups.size(), setups.size() >= 5);

		ExampleSetup minimum = byLabelPrefix(setups, "Minimum");
		assertEquals("Helm of neitiznot", minimum.getGear().getSlots().get(EquipSlot.HEAD).get(0).getName());
		assertEquals("Dragon sword", minimum.getGear().getSlots().get(EquipSlot.WEAPON).get(0).getName());

		assertNotNull("minimum column should pair an inventory", minimum.getInventory());
		assertEquals("Toxic blowpipe", minimum.getInventory().getSlots().get(0));
		assertFalse("rune pouch should attach", minimum.getInventory().getRunes().isEmpty());
		assertEquals("Fire rune", minimum.getInventory().getRunes().get(0));
	}

	@Test
	public void toaColumnLabelsIncludeInvocationContinuationLines() throws IOException
	{
		ExampleSetup minimum = byLabelPrefix(parser.parse(fixture("toa-strategies")), "Minimum");
		assertTrue("multi-line header should merge: " + minimum.getLabel(),
			minimum.getLabel().toLowerCase().contains("invocation"));
	}

	@Test
	public void tobGuideYieldsModeTierRoleHierarchy() throws IOException
	{
		List<ExampleSetup> setups = parser.parse(fixture("tob-setups-guide"));
		assertTrue("expected many role columns, got " + setups.size(), setups.size() >= 12);

		Set<String> modes = new LinkedHashSet<>();
		Set<String> normalTiers = new LinkedHashSet<>();
		Set<String> labels = new LinkedHashSet<>();
		for (ExampleSetup setup : setups)
		{
			assertFalse("every setup should sit under a mode heading", setup.getPath().isEmpty());
			modes.add(setup.getPath().get(0));
			if (setup.getPath().get(0).toLowerCase().contains("normal") && setup.getPath().size() > 1)
			{
				normalTiers.add(setup.getPath().get(1));
			}
			labels.add(setup.getLabel());
		}

		assertEquals("normal + hard modes", 2, modes.size());
		assertTrue("normal tiers should include Learner: " + normalTiers, normalTiers.contains("Learner"));
		assertTrue("roles should include Magic: " + labels, labels.contains("Magic"));
		assertTrue("roles should include Melee", labels.contains("Melee"));
		assertTrue("roles should include Range", labels.contains("Range"));
	}

	@Test
	public void tobRoleColumnsCarryGearAndInventory()
		throws IOException
	{
		List<ExampleSetup> setups = parser.parse(fixture("tob-setups-guide"));
		for (ExampleSetup setup : setups)
		{
			assertFalse("gear empty in " + setup.getPath() + "/" + setup.getLabel(),
				setup.getGear().getSlots().isEmpty());
		}
	}

	@Test
	public void crlfInputParsesIdenticallyToLf() throws IOException
	{
		String lf = fixture("toa-strategies");
		List<ExampleSetup> fromLf = parser.parse(lf);
		List<ExampleSetup> fromCrlf = parser.parse(lf.replace("\n", "\r\n"));

		assertEquals(fromLf.size(), fromCrlf.size());
		for (int i = 0; i < fromLf.size(); i++)
		{
			assertEquals(fromLf.get(i).getLabel(), fromCrlf.get(i).getLabel());
			assertEquals(fromLf.get(i).getPath(), fromCrlf.get(i).getPath());
			assertEquals(fromLf.get(i).getGear().getSlots().size(),
				fromCrlf.get(i).getGear().getSlots().size());
		}
	}

	@Test
	public void standaloneVorkathExamplesIncludeTheirInventories() throws IOException
	{
		List<ExampleSetup> examples = parser.parse(fixture("vorkath-strategies"));
		assertEquals(3, examples.size());
		for (ExampleSetup example : examples) assertNotNull(example.getInventory());
	}
}
