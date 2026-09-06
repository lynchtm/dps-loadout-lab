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

import com.dpscalc.wikisetups.items.ItemNames;
import com.dpscalc.wikisetups.wiki.EquipSlot;
import com.dpscalc.wikisetups.wiki.GearSetup;
import com.dpscalc.wikisetups.wiki.RankedItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Example loadouts name one concrete item per slot, which alone would lose
 * ownership personalization. Most raid pages also carry ranked
 * recommendation tables — each example item is looked up in those ladders
 * and its slot gains the ladder as fallback alternatives. The example item
 * stays first (owned → placed, unowned → the goal), so tier intent is
 * preserved while unowned slots degrade to the best gear the player has.
 *
 * The anchor lookup also picks the right style implicitly: a melee example
 * weapon is only found in the melee setup's weapon ladder.
 */
public final class ExampleGearEnricher
{
	private ExampleGearEnricher()
	{
	}

	public static GearSetup enrich(GearSetup example, List<GearSetup> rankedSetups)
	{
		if (rankedSetups == null || rankedSetups.isEmpty())
		{
			return example;
		}
		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		for (Map.Entry<EquipSlot, List<RankedItem>> entry : example.getSlots().entrySet())
		{
			RankedItem anchor = entry.getValue().get(0);
			List<RankedItem> merged = new ArrayList<>();
			merged.add(anchor);
			List<RankedItem> ladder = findLadder(entry.getKey(), anchor, rankedSetups);
			if (ladder != null)
			{
				String anchorName = ItemNames.normalize(anchor.getName());
				for (RankedItem alternative : ladder)
				{
					if (!ItemNames.normalize(alternative.getName()).equals(anchorName))
					{
						merged.add(alternative);
					}
				}
			}
			slots.put(entry.getKey(), merged);
		}
		return new GearSetup(example.getStyleLabel(), slots);
	}

	/** The first ranked slot list containing the anchor item, or null. */
	private static List<RankedItem> findLadder(EquipSlot slot, RankedItem anchor, List<GearSetup> rankedSetups)
	{
		String anchorName = ItemNames.normalize(anchor.getName());
		for (GearSetup setup : rankedSetups)
		{
			List<RankedItem> list = setup.getSlots().get(slot);
			if (list == null)
			{
				continue;
			}
			for (RankedItem item : list)
			{
				if (ItemNames.normalize(item.getName()).equals(anchorName))
				{
					return list;
				}
			}
		}
		return null;
	}
}
