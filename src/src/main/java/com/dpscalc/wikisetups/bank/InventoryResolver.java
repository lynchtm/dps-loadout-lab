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

import com.dpscalc.wikisetups.WikiSetupOptions.MissingGearBehavior;
import com.dpscalc.wikisetups.items.ConsumableGroups;
import com.dpscalc.wikisetups.items.ItemNames;
import com.dpscalc.wikisetups.items.ItemResolver;
import com.dpscalc.wikisetups.items.PlaceholderItems;
import com.dpscalc.wikisetups.items.ResolvedItem;
import com.dpscalc.wikisetups.wiki.GearSetup;
import com.dpscalc.wikisetups.wiki.InventorySetup;
import com.dpscalc.wikisetups.wiki.RankedItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Resolves an inventory setup's 28 slots (and its rune pouch) against owned
 * items. Inventory has no ranked alternatives — a slot's item is either
 * owned (place the owned variant), missing (placed only under
 * SHOW_BEST_UNOWNED), or unrecognized (placeholder templates like
 * "Cheap food", reported not placed).
 */
@Singleton
public class InventoryResolver
{
	private final ItemResolver itemResolver;

	@Inject
	public InventoryResolver(ItemResolver itemResolver)
	{
		this.itemResolver = itemResolver;
	}

	public InventoryResolution resolve(InventorySetup setup, Set<Integer> ownedIds, MissingGearBehavior missingBehavior)
	{
		return resolve(setup, ownedIds, missingBehavior, Collections.emptyList());
	}

	/**
	 * {@code rankedSetups} are the page's gear ladders — an unowned switch
	 * item in the inventory (blowpipe, trident) falls back to the best owned
	 * alternative from the ladder that contains it, just like worn gear.
	 */
	public InventoryResolution resolve(InventorySetup setup, Set<Integer> ownedIds,
		MissingGearBehavior missingBehavior, List<GearSetup> rankedSetups)
	{
		return resolve(setup, ownedIds, missingBehavior, rankedSetups, PickStrategy.BEST_OWNED);
	}

	public InventoryResolution resolve(InventorySetup setup, Set<Integer> ownedIds,
		MissingGearBehavior missingBehavior, List<GearSetup> rankedSetups, PickStrategy strategy)
	{
		List<InventoryPick> picks = new ArrayList<>();
		List<InventoryPick> runePicks = new ArrayList<>();
		Set<String> unresolved = new LinkedHashSet<>();
		Set<String> missingNames = new LinkedHashSet<>();
		int[] missing = {0};

		List<String> slots = setup.getSlots();
		for (int i = 0; i < slots.size(); i++)
		{
			resolveOne(slots.get(i), i, ownedIds, missingBehavior, rankedSetups,
				picks, unresolved, missingNames, missing, strategy);
		}
		List<String> runes = setup.getRunes();
		for (int i = 0; i < runes.size(); i++)
		{
			resolveOne(runes.get(i), i, ownedIds, missingBehavior, Collections.emptyList(),
				runePicks, unresolved, missingNames, missing, strategy);
		}

		return new InventoryResolution(setup, picks, runePicks,
			new ArrayList<>(unresolved), new ArrayList<>(missingNames), missing[0]);
	}

	private void resolveOne(String name, int slot, Set<Integer> ownedIds, MissingGearBehavior missingBehavior,
		List<GearSetup> rankedSetups, List<InventoryPick> out, Set<String> unresolved,
		Set<String> missingNames, int[] missing, PickStrategy strategy)
	{
		if (name.isEmpty())
		{
			return;
		}
		Optional<ResolvedItem> resolved = itemResolver.resolve(name);
		if (!resolved.isPresent())
		{
			resolvePlaceholder(name, slot, ownedIds, missingBehavior, out, unresolved,
				missingNames, missing, strategy);
			return;
		}

		Integer ownedId = firstOwned(resolved.get(), ownedIds);
		if (ownedId != null)
		{
			out.add(new InventoryPick(slot, ownedId, true));
			return;
		}

		// showing the wiki's own list: place it unowned and count it, rather
		// than substituting something the player happens to have
		if (strategy == PickStrategy.TOP_RECOMMENDATION)
		{
			missing[0]++;
			missingNames.add(name);
			out.add(new InventoryPick(slot, resolved.get().getPrimaryId(), false));
			return;
		}

		Integer substitute = substituteFor(name, ownedIds, rankedSetups);
		if (substitute != null)
		{
			out.add(new InventoryPick(slot, substitute, true));
			return;
		}

		missing[0]++;
		missingNames.add(name);
		if (missingBehavior == MissingGearBehavior.SHOW_BEST_UNOWNED)
		{
			out.add(new InventoryPick(slot, resolved.get().getPrimaryId(), false));
		}
	}

	/**
	 * Owned stand-in for an unowned inventory item: first the gear ladder
	 * containing it (switch items, style-correct by construction), then its
	 * consumable class (best owned food for an unowned shark).
	 */
	private Integer substituteFor(String name, Set<Integer> ownedIds, List<GearSetup> rankedSetups)
	{
		String normalized = ItemNames.normalize(name);
		for (GearSetup setup : rankedSetups)
		{
			for (List<RankedItem> ladder : setup.getSlots().values())
			{
				boolean anchored = false;
				for (RankedItem item : ladder)
				{
					if (ItemNames.normalize(item.getName()).equals(normalized))
					{
						anchored = true;
						break;
					}
				}
				if (!anchored)
				{
					continue;
				}
				for (RankedItem item : ladder)
				{
					Optional<ResolvedItem> resolved = itemResolver.resolve(item.getName());
					if (resolved.isPresent())
					{
						Integer owned = firstOwned(resolved.get(), ownedIds);
						if (owned != null)
						{
							return owned;
						}
					}
				}
			}
		}
		for (String candidate : ConsumableGroups.groupFor(name))
		{
			Optional<ResolvedItem> resolved = itemResolver.resolve(candidate);
			if (resolved.isPresent())
			{
				Integer owned = firstOwned(resolved.get(), ownedIds);
				if (owned != null)
				{
					return owned;
				}
			}
		}
		return null;
	}

	/**
	 * Placeholder templates ({{Cheap food}}) become whichever concrete
	 * candidate the player owns; failing that, the first candidate as a goal
	 * item under SHOW_BEST_UNOWNED. Unknown placeholders stay unresolved.
	 */
	private void resolvePlaceholder(String name, int slot, Set<Integer> ownedIds,
		MissingGearBehavior missingBehavior, List<InventoryPick> out, Set<String> unresolved,
		Set<String> missingNames, int[] missing, PickStrategy strategy)
	{
		List<String> candidates = PlaceholderItems.candidatesFor(name);
		if (candidates.isEmpty())
		{
			unresolved.add(name);
			return;
		}

		ResolvedItem firstResolvable = null;
		for (String candidate : candidates)
		{
			Optional<ResolvedItem> resolved = itemResolver.resolve(candidate);
			if (!resolved.isPresent())
			{
				continue;
			}
			if (firstResolvable == null)
			{
				firstResolvable = resolved.get();
				// the wiki's own answer for a placeholder is its best
				// candidate, owned or not
				if (strategy == PickStrategy.TOP_RECOMMENDATION)
				{
					Integer owned = firstOwned(firstResolvable, ownedIds);
					if (owned == null)
					{
						missing[0]++;
						missingNames.add(name);
					}
					out.add(new InventoryPick(slot,
						owned != null ? owned : firstResolvable.getPrimaryId(), owned != null));
					return;
				}
			}
			Integer ownedId = firstOwned(resolved.get(), ownedIds);
			if (ownedId != null)
			{
				out.add(new InventoryPick(slot, ownedId, true));
				return;
			}
		}

		if (firstResolvable == null)
		{
			unresolved.add(name);
			return;
		}
		missing[0]++;
		missingNames.add(name);
		if (missingBehavior == MissingGearBehavior.SHOW_BEST_UNOWNED)
		{
			out.add(new InventoryPick(slot, firstResolvable.getPrimaryId(), false));
		}
	}

	private static Integer firstOwned(ResolvedItem resolved, Set<Integer> ownedIds)
	{
		for (int id : resolved.getAcceptableIds())
		{
			if (ownedIds.contains(id))
			{
				return id;
			}
		}
		return null;
	}
}
