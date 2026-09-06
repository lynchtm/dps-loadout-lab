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
import java.util.List;

/**
 * Tells a real loadout apart from an inline "for this room, bring this one
 * item" annotation.
 *
 * Across the whole corpus, gear styles are bimodal by slot count: real
 * loadouts fill 8-12 slots, annotations fill 1-2, and <em>nothing</em> fills
 * 3 or 4. That empty band is the wiki's own editing convention, not a tuned
 * threshold — so any cutoff inside it separates the two cleanly. Raids drive
 * this: Chambers of Xeric alone contributes 12 single-item "styles" (Tekton,
 * the Olm claws, …) that would otherwise flood the dropdown as if they were
 * alternatives to your entire setup.
 */
public final class GearSetups
{
	/** Below this a setup is an annotation, not a loadout (see the class note). */
	public static final int MIN_LOADOUT_SLOTS = 3;

	private GearSetups()
	{
	}

	public static boolean isLoadout(GearSetup setup)
	{
		return setup.getSlots().size() >= MIN_LOADOUT_SLOTS;
	}

	/**
	 * Splits a page's setups into the loadouts worth a dropdown entry and the
	 * thin annotation setups.
	 *
	 * A page that is <em>entirely</em> annotations (the Ultimate Ironman
	 * equipment guide uses the template 42 times as an illustration) is left
	 * whole — filtering everything away would blank the panel, which is worse
	 * than a long list. Such a page has no loadout to hang the annotations off
	 * of anyway.
	 */
	public static Partition partition(List<GearSetup> setups)
	{
		List<GearSetup> loadouts = new ArrayList<>();
		List<GearSetup> switches = new ArrayList<>();
		List<Integer> loadoutIndices = new ArrayList<>();
		for (int i = 0; i < setups.size(); i++)
		{
			if (isLoadout(setups.get(i)))
			{
				loadouts.add(setups.get(i));
				loadoutIndices.add(i);
			}
			else
			{
				switches.add(setups.get(i));
			}
		}
		if (loadouts.isEmpty())
		{
			loadoutIndices.clear();
			for (int i = 0; i < setups.size(); i++)
			{
				loadoutIndices.add(i);
			}
			return new Partition(new ArrayList<>(setups), new ArrayList<>(), loadoutIndices);
		}
		return new Partition(loadouts, switches, loadoutIndices);
	}

	/**
	 * {@code loadoutIndices.get(j)} is the position of loadout {@code j} in the
	 * original setup list — used to remap a pairing array computed against the
	 * unfiltered list onto the filtered one.
	 */
		public static class Partition
	{
		List<GearSetup> loadouts;
		List<GearSetup> switches;
		List<Integer> loadoutIndices;
		public Partition(List<GearSetup> loadouts, List<GearSetup> switches, List<Integer> loadoutIndices) { this.loadouts=loadouts; this.switches=switches; this.loadoutIndices=loadoutIndices; }
		public List<GearSetup> getLoadouts() { return loadouts; }
		public List<GearSetup> getSwitches() { return switches; }
		public List<Integer> getLoadoutIndices() { return loadoutIndices; }

		/**
		 * Remaps an inventory-pairing array (indexed by original setup) so it is
		 * indexed by loadout instead. Empty in, empty out — a page with no
		 * positional pairing stays unpaired.
		 */
		public int[] remapPairing(int[] pairing)
		{
			if (pairing.length == 0)
			{
				return pairing;
			}
			int[] remapped = new int[loadouts.size()];
			for (int j = 0; j < loadouts.size(); j++)
			{
				int original = loadoutIndices.get(j);
				remapped[j] = original < pairing.length ? pairing[original] : -1;
			}
			return remapped;
		}
	}

}
