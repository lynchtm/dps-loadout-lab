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
import com.dpscalc.wikisetups.items.ItemNames;
import com.dpscalc.wikisetups.items.ItemResolver;
import com.dpscalc.wikisetups.items.ItemSets;
import com.dpscalc.wikisetups.items.ResolvedItem;
import com.dpscalc.wikisetups.wiki.EquipSlot;
import com.dpscalc.wikisetups.wiki.GearSetup;
import com.dpscalc.wikisetups.wiki.RankedItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.BiPredicate;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Walks each slot's ranked alternatives (best first) and picks the first
 * recommendation the player can satisfy, recording the specific owned
 * variant id. Pure logic — callers supply the owned-id set (see
 * {@link BankStateCache#ownedItemIds()}).
 */
@Singleton
public class OwnershipResolver
{
	private final ItemResolver itemResolver;
	private final BiPredicate<EquipSlot, Integer> allowed;

	@Inject
	public OwnershipResolver(ItemResolver itemResolver)
	{
		this(itemResolver, (slot, id) -> true);
	}

	/**
	 * Creates a resolver constrained by the caller's equipment facts. The
	 * default constructor remains unrestricted for bank ownership and legacy
	 * callers; callers that know the destination slot can reject a generic
	 * family member whose concrete item belongs elsewhere.
	 */
	public OwnershipResolver(ItemResolver itemResolver, BiPredicate<EquipSlot, Integer> allowed)
	{
		this.itemResolver = itemResolver;
		this.allowed = allowed;
	}

	/** Adaptive resolution (BEST_OWNED). */
	public SetupResolution resolve(GearSetup setup, Set<Integer> ownedIds, MissingGearBehavior missingBehavior,
		boolean assumeSlayerTask)
	{
		return resolve(setup, ownedIds, missingBehavior, assumeSlayerTask, PickStrategy.BEST_OWNED);
	}

	public SetupResolution resolve(GearSetup setup, Set<Integer> ownedIds, MissingGearBehavior missingBehavior,
		boolean assumeSlayerTask, PickStrategy strategy)
	{
		Map<EquipSlot, List<String>> unresolved = new EnumMap<>(EquipSlot.class);
		Map<EquipSlot, Set<String>> excluded = new EnumMap<>(EquipSlot.class);
		Map<EquipSlot, SlotPick> pickBySlot = new EnumMap<>(EquipSlot.class);

		for (Map.Entry<EquipSlot, List<RankedItem>> entry : setup.getSlots().entrySet())
		{
			excluded.put(entry.getKey(), new HashSet<>());
			SlotPick pick = resolveSlot(entry.getKey(), entry.getValue(), ownedIds, missingBehavior,
				assumeSlayerTask, excluded.get(entry.getKey()), unresolved, strategy);
			if (pick != null)
			{
				pickBySlot.put(entry.getKey(), pick);
			}
		}

		// A set piece must actually be WORN in this layout, not just banked:
		// owning the full blood moon set doesn't justify the helm when better
		// body/legs win their slots. Excluding a piece can invalidate another
		// set piece, so iterate to a fixpoint (exclusions only grow).
		// Ownership-derived, so it has no meaning when showing the wiki's own
		// pick — an owned set piece must stay put there.
		boolean changed = strategy == PickStrategy.BEST_OWNED;
		while (changed)
		{
			changed = false;
			for (Map.Entry<EquipSlot, List<RankedItem>> entry : setup.getSlots().entrySet())
			{
				SlotPick pick = pickBySlot.get(entry.getKey());
				if (pick == null || !pick.isOwned())
				{
					continue;
				}
				RankedItem item = pick.getItem();
				ItemSets.Requirement requirement = ItemSets.requirementFor(item.getName());
				if (requirement == null
					|| (!requirement.isAlways() && !ItemSets.setConditionalNote(item.getNote()))
					|| setWornInLayout(requirement, pickBySlot, ownedIds))
				{
					continue;
				}
				excluded.get(entry.getKey()).add(item.getName());
				SlotPick replacement = resolveSlot(entry.getKey(), entry.getValue(), ownedIds,
					missingBehavior, assumeSlayerTask, excluded.get(entry.getKey()), null, strategy);
				if (replacement != null)
				{
					pickBySlot.put(entry.getKey(), replacement);
				}
				else
				{
					pickBySlot.remove(entry.getKey());
				}
				changed = true;
			}
		}

		List<SlotPick> picks = new ArrayList<>();
		List<EquipSlot> emptySlots = new ArrayList<>();
		for (Map.Entry<EquipSlot, List<RankedItem>> entry : setup.getSlots().entrySet())
		{
			SlotPick pick = pickBySlot.get(entry.getKey());
			if (pick != null)
			{
				picks.add(pick);
			}
			else
			{
				emptySlots.add(entry.getKey());
			}
		}
		return new SetupResolution(setup, picks, unresolved, emptySlots);
	}

	/**
	 * Resolves one slot: first satisfiable alternative wins; under
	 * SHOW_BEST_UNOWNED an unowned best becomes a goal pick; null when the
	 * slot stays empty. {@code unresolved} is only recorded when non-null so
	 * re-resolution during set stabilization doesn't duplicate entries.
	 */
	private SlotPick resolveSlot(EquipSlot slot, List<RankedItem> alternatives, Set<Integer> ownedIds,
		MissingGearBehavior missingBehavior, boolean assumeSlayerTask,
		Set<String> excludedNames, Map<EquipSlot, List<String>> unresolved, PickStrategy strategy)
	{
		if (strategy == PickStrategy.TOP_RECOMMENDATION)
		{
			return topRecommendation(slot, alternatives, ownedIds, assumeSlayerTask, unresolved);
		}

		RankedItem bestResolvable = null;
		ResolvedItem bestResolved = null;
		// tracked ignoring the set gate, so a slot whose every alternative
		// needs an unowned set still shows the top piece as a goal
		RankedItem bestAnyResolvable = null;
		ResolvedItem bestAnyResolved = null;

		for (int i = 0; i < alternatives.size(); i++)
		{
			RankedItem candidate = alternatives.get(i);
			if (excludedNames.contains(candidate.getName()))
			{
				continue;
			}
			if (!assumeSlayerTask && isSlayerTaskConditional(candidate))
			{
				continue;
			}
			Optional<ResolvedItem> resolved = resolveCandidate(slot, candidate);
			if (!resolved.isPresent())
			{
				if (unresolved != null)
				{
					unresolved.computeIfAbsent(slot, k -> new ArrayList<>()).add(candidate.getName());
				}
				continue;
			}
			if (bestAnyResolvable == null)
			{
				bestAnyResolvable = candidate;
				bestAnyResolved = resolved.get();
			}
			if (!setSatisfied(candidate, ownedIds))
			{
				continue;
			}
			if (bestResolvable == null)
			{
				bestResolvable = candidate;
				bestResolved = resolved.get();
			}

			Integer ownedId = firstOwned(resolved.get(), ownedIds);
			if (ownedId != null)
			{
				return new SlotPick(slot, candidate, ownedId, true, i == 0);
			}
		}

		if (bestResolvable == null)
		{
			bestResolvable = bestAnyResolvable;
			bestResolved = bestAnyResolved;
		}
		if (missingBehavior == MissingGearBehavior.SHOW_BEST_UNOWNED && bestResolvable != null)
		{
			return new SlotPick(slot, bestResolvable, bestResolved.getPrimaryId(), false, true);
		}
		return null;
	}

	/**
	 * Wiki links sometimes point at a page rather than an item ("God
	 * capes#Imbuing"); the display text and picture the template carried are
	 * tried in turn when the primary name resolves to nothing.
	 */
	private Optional<ResolvedItem> resolveCandidate(EquipSlot slot, RankedItem candidate)
	{
		Optional<ResolvedItem> resolved = itemResolver.resolve(candidate.getName(), id -> allowed.test(slot, id));
		if (resolved.isPresent())
		{
			return resolved;
		}
		for (String fallback : candidate.getFallbackNames())
		{
			resolved = itemResolver.resolve(fallback, id -> allowed.test(slot, id));
			if (resolved.isPresent())
			{
				return resolved;
			}
		}
		return Optional.empty();
	}

	/**
	 * The wiki's own first choice for the slot, ownership only deciding
	 * whether the pick is flagged owned and which variant id is placed —
	 * an owned recolour/upgrade still lands as the player's copy so the tab
	 * stays usable. Set gating is deliberately not consulted: it exists to
	 * adapt to what the player owns, which this strategy ignores.
	 */
	private SlotPick topRecommendation(EquipSlot slot, List<RankedItem> alternatives, Set<Integer> ownedIds,
		boolean assumeSlayerTask, Map<EquipSlot, List<String>> unresolved)
	{
		for (RankedItem candidate : alternatives)
		{
			if (!assumeSlayerTask && isSlayerTaskConditional(candidate))
			{
				continue;
			}
			Optional<ResolvedItem> resolved = resolveCandidate(slot, candidate);
			if (!resolved.isPresent())
			{
				if (unresolved != null)
				{
					unresolved.computeIfAbsent(slot, k -> new ArrayList<>()).add(candidate.getName());
				}
				continue;
			}
			Integer ownedId = firstOwned(resolved.get(), ownedIds);
			return new SlotPick(slot, candidate,
				ownedId != null ? ownedId : resolved.get().getPrimaryId(), ownedId != null, true);
		}
		return null;
	}

	/**
	 * Set pieces (void, blood moon, crystal armour…) are only recommended
	 * because of their set bonus — "always" pieces unconditionally, others
	 * only when the wiki footnote flags the recommendation as full-set. When
	 * the player can't complete the set (or lacks the paired weapon), the
	 * piece is skipped so the next best alternative wins instead.
	 */
	private boolean setSatisfied(RankedItem item, Set<Integer> ownedIds)
	{
		ItemSets.Requirement requirement = ItemSets.requirementFor(item.getName());
		if (requirement == null)
		{
			return true;
		}
		if (!requirement.isAlways() && !ItemSets.setConditionalNote(item.getNote()))
		{
			return true;
		}
		for (ItemSets.PieceGroup group : requirement.getOtherPieces())
		{
			if (!anyOwned(group.getNames(), ownedIds))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * A required group counts as worn when its piece IS the pick for its
	 * slot, or when the piece is owned and its slot isn't claimed by some
	 * other owned pick (the player would naturally fill it with the set
	 * piece — also covers setups that don't list that slot at all).
	 */
	private boolean setWornInLayout(ItemSets.Requirement requirement,
		Map<EquipSlot, SlotPick> pickBySlot, Set<Integer> ownedIds)
	{
		for (ItemSets.PieceGroup group : requirement.getOtherPieces())
		{
			// Owning the weapon is enough. Only one can be worn, but a loadout
			// routinely carries several and swaps between them — Fortis
			// Colosseum lists fang, bow of faerdhinen and sanguinesti staff in
			// one slot — so crystal armour is still right when the bow is a
			// switch that lost the slot to a melee weapon. Armour is not
			// swapped mid-fight, so those groups stay strict: owning blood
			// moon body doesn't justify the helm when Bandos wins the slot.
			if (group.getSlot() == EquipSlot.WEAPON)
			{
				if (!anyOwned(group.getNames(), ownedIds))
				{
					return false;
				}
				continue;
			}

			SlotPick slotPick = pickBySlot.get(group.getSlot());
			if (slotPick != null && slotPick.isOwned())
			{
				if (!anyOwned(group.getNames(), Collections.singleton(slotPick.getItemId())))
				{
					return false;
				}
			}
			else if (!anyOwned(group.getNames(), ownedIds))
			{
				return false;
			}
		}
		return true;
	}

	private boolean anyOwned(List<String> names, Set<Integer> ownedIds)
	{
		for (String name : names)
		{
			Optional<ResolvedItem> resolved = itemResolver.resolve(name);
			if (resolved.isPresent() && firstOwned(resolved.get(), ownedIds) != null)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * A two-handed weapon pick makes a shield pick contradictory — the wiki
	 * lists shields for the one-handed alternatives. Returns the picks with
	 * the shield removed, or the original list when nothing conflicts.
	 */
	public static List<SlotPick> dropShieldForTwoHanded(List<SlotPick> picks, IntPredicate isTwoHanded)
	{
		boolean twoHanded = false;
		for (SlotPick pick : picks)
		{
			if (pick.getSlot() == EquipSlot.WEAPON && isTwoHanded.test(pick.getItemId()))
			{
				twoHanded = true;
				break;
			}
		}
		if (!twoHanded)
		{
			return picks;
		}
		List<SlotPick> filtered = new ArrayList<>(picks.size());
		for (SlotPick pick : picks)
		{
			if (pick.getSlot() != EquipSlot.SHIELD)
			{
				filtered.add(pick);
			}
		}
		return filtered;
	}

	/**
	 * Owned items from the setup's ranked alternatives that were NOT picked —
	 * switches and spec weapons the player has banked. Wiki-table order,
	 * deduplicated, excluding {@code excludeIds} (the already-placed picks).
	 */
	public List<Integer> ownedAlternatives(GearSetup setup, Set<Integer> ownedIds,
		boolean assumeSlayerTask, Set<Integer> excludeIds)
	{
		List<Integer> extras = new ArrayList<>();
		for (Map.Entry<EquipSlot, List<RankedItem>> entry : setup.getSlots().entrySet())
		{
			for (RankedItem candidate : entry.getValue())
			{
				if (!assumeSlayerTask && isSlayerTaskConditional(candidate))
				{
					continue;
				}
				if (!setSatisfied(candidate, ownedIds))
				{
					continue;
				}
				Optional<ResolvedItem> resolved = resolveCandidate(entry.getKey(), candidate);
				if (!resolved.isPresent())
				{
					continue;
				}
				Integer ownedId = firstOwned(resolved.get(), ownedIds);
				if (ownedId != null && !excludeIds.contains(ownedId) && !extras.contains(ownedId))
				{
					extras.add(ownedId);
				}
			}
		}
		return extras;
	}

	/**
	 * Wiki footnotes flag items that are only best-in-slot while on a slayer
	 * task (e.g. "If on a Zulrah slayer boss task" on the slayer helmet).
	 * When the user isn't assuming a task, such items are skipped entirely —
	 * they must satisfy neither ownership nor the goal-item fallback.
	 */
	/**
	 * Whether any recommendation in the setup only applies while on a slayer
	 * task — drives whether the panel offers the "on a slayer task" choice at
	 * all, so it appears exactly on the pages where it changes something.
	 */
	public static boolean hasSlayerConditional(GearSetup setup)
	{
		for (List<RankedItem> alternatives : setup.getSlots().values())
		{
			for (RankedItem item : alternatives)
			{
				if (isSlayerTaskConditional(item))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * The task-conditional items and their notes, so the checkbox can show the
	 * wiki's own wording — conditions are monster-specific ("Only on Fossil
	 * Island wyvern task") and "on a slayer task" alone overstates them.
	 * Deduplicated by item name, insertion order preserved.
	 */
	public static List<String> slayerConditionalNotes(GearSetup setup)
	{
		Map<String, String> byItem = new java.util.LinkedHashMap<>();
		for (List<RankedItem> alternatives : setup.getSlots().values())
		{
			for (RankedItem item : alternatives)
			{
				if (isSlayerTaskConditional(item) && item.getNote() != null)
				{
					byItem.putIfAbsent(item.getName(), item.getNote());
				}
			}
		}
		List<String> notes = new ArrayList<>(byItem.size());
		for (Map.Entry<String, String> e : byItem.entrySet())
		{
			notes.add(e.getKey() + " — " + e.getValue());
		}
		return notes;
	}

	/**
	 * Phrasings that mean "only when you're on a task". The footnote form
	 * spells out "Slayer task", but the bare parenthetical the pages use just
	 * as often does not — "(on task)", "(task only)" — so requiring the word
	 * "slayer" missed roughly half of them.
	 */
	private static final String[] TASK_ONLY_PHRASES = {
		"on task", "on a task", "task only", "only on task", "while on task",
	};

	/**
	 * The slayer-effect items a note might name. When a note names one of
	 * these but the item it's attached to isn't it, the task condition belongs
	 * to that other item, not this one.
	 */
	private static final String[] TASK_ITEMS = {
		"slayer helmet", "slayer helm", "black mask", "salve amulet",
	};

	static boolean isSlayerTaskConditional(RankedItem item)
	{
		String note = item.getNote();
		if (note == null)
		{
			return false;
		}
		String n = note.toLowerCase(java.util.Locale.ROOT);
		if (!n.contains("task"))
		{
			return false;
		}

		// The note describes the OFF-task case — gating it would hide the item
		// exactly when it's wanted (Dual macuahuitl on Custodian stalker).
		if (n.contains("off-task") || n.contains("off task"))
		{
			return false;
		}
		// The condition names a DIFFERENT task item than this one, so it
		// belongs to that item ("Downgrade to glory if bringing a Slayer
		// helmet on task" attached to the Amulet of rancour).
		String self = ItemNames.normalize(item.getName());
		for (String taskItem : TASK_ITEMS)
		{
			if (n.contains(taskItem) && !self.contains(taskItem))
			{
				return false;
			}
		}
		// A task phrase with an escape hatch is a preference, not a gate
		// ("If on task, or have 8 ranged strength…" on the Toxic blowpipe).
		if (followedByOr(n))
		{
			return false;
		}

		if (n.contains("slayer"))
		{
			return true;
		}
		for (String phrase : TASK_ONLY_PHRASES)
		{
			if (n.contains(phrase))
			{
				return true;
			}
		}
		return false;
	}

	/** True when a task phrase is followed shortly by " or " — an alternative. */
	private static boolean followedByOr(String note)
	{
		for (String phrase : TASK_ONLY_PHRASES)
		{
			int at = note.indexOf(phrase);
			if (at >= 0 && note.indexOf(" or ", at) >= 0
				&& note.indexOf(" or ", at) - at < 40)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The specific owned variant to place in the layout. acceptableIds
	 * iterates insertion order with the primary (exact-name) id first, so an
	 * owned exact match beats an owned variant.
	 */
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
