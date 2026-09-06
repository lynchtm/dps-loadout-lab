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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GearSetupsTest
{
	/** A setup filling {@code slotCount} slots, each with one placeholder item. */
	private static GearSetup withSlots(String label, int slotCount)
	{
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		EquipSlot[] all = EquipSlot.values();
		for (int i = 0; i < slotCount; i++)
		{
			slots.put(all[i], Collections.singletonList(new RankedItem("Item " + i, 1, null)));
		}
		return new GearSetup(label, slots);
	}

	@Test
	public void loadoutThresholdSitsInTheEmptyBand()
	{
		assertFalse(GearSetups.isLoadout(withSlots("switch", 1)));
		assertFalse(GearSetups.isLoadout(withSlots("switch", 2)));
		assertTrue(GearSetups.isLoadout(withSlots("partial", 5)));
		assertTrue(GearSetups.isLoadout(withSlots("full", 12)));
	}

	@Test
	public void partitionSeparatesLoadoutsFromSwitches()
	{
		List<GearSetup> setups = Arrays.asList(
			withSlots("Melee", 11), withSlots("Tekton", 1),
			withSlots("Ranged", 12), withSlots("Olm", 1));
		GearSetups.Partition p = GearSetups.partition(setups);

		assertEquals(Arrays.asList("Melee", "Ranged"), labels(p.getLoadouts()));
		assertEquals(Arrays.asList("Tekton", "Olm"), labels(p.getSwitches()));
	}

	/** A page that is entirely annotations is left whole rather than blanked. */
	@Test
	public void anAllAnnotationPageIsNotEmptied()
	{
		List<GearSetup> setups = Arrays.asList(
			withSlots("Setup 1", 1), withSlots("Setup 2", 1), withSlots("Setup 3", 2));
		GearSetups.Partition p = GearSetups.partition(setups);

		assertEquals(3, p.getLoadouts().size());
		assertTrue(p.getSwitches().isEmpty());
	}

	/**
	 * Conflict 2 from the plan: pairing is computed against the unfiltered
	 * list, so filtering must remap the indices onto the loadouts.
	 */
	@Test
	public void pairingRemapsOntoTheFilteredLoadouts()
	{
		// setups: [Melee(loadout), Tekton(switch), Ranged(loadout)]
		// original pairing said setup 0 -> inv 0, setup 2 -> inv 1
		List<GearSetup> setups = Arrays.asList(
			withSlots("Melee", 11), withSlots("Tekton", 1), withSlots("Ranged", 12));
		GearSetups.Partition p = GearSetups.partition(setups);

		int[] original = {0, -1, 1};
		// loadout 0 (Melee, orig 0) -> 0; loadout 1 (Ranged, orig 2) -> 1
		assertArrayEquals(new int[]{0, 1}, p.remapPairing(original));
	}

	@Test
	public void emptyPairingStaysEmptyAfterRemap()
	{
		GearSetups.Partition p = GearSetups.partition(
			Arrays.asList(withSlots("Melee", 11), withSlots("Tekton", 1)));
		assertEquals(0, p.remapPairing(SetupPairing.NONE).length);
	}

	private static List<String> labels(List<GearSetup> setups)
	{
		List<String> out = new ArrayList<>();
		for (GearSetup s : setups)
		{
			out.add(s.getStyleLabel());
		}
		return out;
	}
}
