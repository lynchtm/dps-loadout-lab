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

import com.dpscalc.wikisetups.wiki.EquipSlot;
import com.dpscalc.wikisetups.wiki.GearSetup;
import com.dpscalc.wikisetups.wiki.RankedItem;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class ExampleGearEnricherTest
{
	private static GearSetup single(EquipSlot slot, String item)
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		slots.put(slot, Collections.singletonList(new RankedItem(item, 1, null)));
		return new GearSetup("Example", slots);
	}

	private static GearSetup ranked(EquipSlot slot, String... items)
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		java.util.ArrayList<RankedItem> list = new java.util.ArrayList<>();
		for (int i = 0; i < items.length; i++)
		{
			list.add(new RankedItem(items[i], i + 1, null));
		}
		slots.put(slot, list);
		return new GearSetup("Ranked", slots);
	}

	@Test
	public void anchorStaysFirstAndLadderFollows()
	{
		GearSetup example = single(EquipSlot.WEAPON, "Rune crossbow");
		GearSetup ladder = ranked(EquipSlot.WEAPON, "Twisted bow", "Bow of faerdhinen", "Rune crossbow", "Magic shortbow");

		GearSetup enriched = ExampleGearEnricher.enrich(example, Collections.singletonList(ladder));
		List<RankedItem> weapon = enriched.getSlots().get(EquipSlot.WEAPON);

		assertEquals("Rune crossbow", weapon.get(0).getName());
		assertEquals(Arrays.asList("Twisted bow", "Bow of faerdhinen", "Magic shortbow"),
			Arrays.asList(weapon.get(1).getName(), weapon.get(2).getName(), weapon.get(3).getName()));
	}

	@Test
	public void anchorAbsentFromAllLaddersStaysAlone()
	{
		GearSetup example = single(EquipSlot.WEAPON, "Dragon sword");
		GearSetup ladder = ranked(EquipSlot.WEAPON, "Twisted bow", "Rune crossbow");

		GearSetup enriched = ExampleGearEnricher.enrich(example, Collections.singletonList(ladder));

		assertEquals(1, enriched.getSlots().get(EquipSlot.WEAPON).size());
	}

	@Test
	public void ladderMatchIsSlotScoped()
	{
		// the anchor appears in a HEAD ladder — the WEAPON slot must not use it
		GearSetup example = single(EquipSlot.WEAPON, "Slayer helmet");
		GearSetup wrongSlot = ranked(EquipSlot.HEAD, "Slayer helmet", "Neitiznot faceguard");

		GearSetup enriched = ExampleGearEnricher.enrich(example, Collections.singletonList(wrongSlot));

		assertEquals(1, enriched.getSlots().get(EquipSlot.WEAPON).size());
	}

	@Test
	public void noRankedSetupsReturnsExampleUnchanged()
	{
		GearSetup example = single(EquipSlot.WEAPON, "Rune crossbow");
		assertSame(example, ExampleGearEnricher.enrich(example, Collections.emptyList()));
	}
}
