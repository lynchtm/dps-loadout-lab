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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.ItemVariationMapping;

/**
 * Resolves a wiki item name to the set of item ids that satisfy the
 * recommendation.
 *
 * Variation groups (ItemVariationMapping) merge cosmetic recolors, charge
 * states AND upgrade tiers — e.g. plain and imbued black masks share one
 * group. Expansion is therefore filtered by name so a lesser tier never
 * satisfies a higher-tier recommendation:
 *  - equal after stripping charge decorations (charges/degradation), or
 *  - candidate starts with the wiki name (suffixed upgrades, e.g. "(e)"), or
 *  - candidate ends with the wiki name (recolor prefixes, e.g. "Black slayer helmet (i)").
 */
@Singleton
public class ItemResolver
{
	private final ItemNameIndex index;

	@Inject
	public ItemResolver(ItemNameIndex index)
	{
		this.index = index;
	}

	private static final Pattern TRAILING_PAREN = Pattern.compile("\\s*\\([^)]*\\)$");
	private static final Pattern LEADING_QUANTITY = Pattern.compile("^\\d+\\s*[x×]\\s*", Pattern.CASE_INSENSITIVE);
	private static final Pattern TRAILING_QUANTITY = Pattern.compile("\\s*[x×]\\s*\\d+$", Pattern.CASE_INSENSITIVE);
	private static final Pattern TRAILING_NUMBER = Pattern.compile("\\s+\\d+$");

	/**
	 * Purely cosmetic decorations that share a base item's stats and tier. Each
	 * suffix/prefix is unambiguous — it can only denote the decoration, never a
	 * different item — so owning the decorated copy satisfies the base rec:
	 * ornament kit "(or)", gilded "(g)", trimmed "(t)", heraldic "(h1)"-"(h5)",
	 * Deadman "(deadman)", and the Echo recolours ("Echo virtus mask").
	 */
	private static final String[] COSMETIC_SUFFIXES = {
		" (or)", " (g)", " (t)", " (h1)", " (h2)", " (h3)", " (h4)", " (h5)", " (deadman)",
	};
	private static final String[] COSMETIC_PREFIXES = {
		"Echo ",
	};

	/**
	 * Empty when the name is unknown — callers should check
	 * {@code index.isReady()} to distinguish "unknown" from "index not built yet".
	 *
	 * Wiki names diverge from in-game names in recurring ways, each tried in
	 * order until one resolves: quantity markers ("2 x Shark"), potion names
	 * without a dose ("Stamina potion" — the item is "Stamina potion(4)"),
	 * page disambiguators ("Teleport to house (tablet)"), plurals ("Sharks"),
	 * and generic names with no item of their own ("Imbued god cape").
	 */
	public Optional<ResolvedItem> resolve(String wikiName)
	{
		return resolve(wikiName, id -> true);
	}

	/**
	 * Resolves a name while retaining only item IDs accepted by the caller.
	 * This is used by equipment-slot resolution: a generic wiki family can
	 * resolve to a real item, but that item may belong to another slot.
	 */
	public Optional<ResolvedItem> resolve(String wikiName, IntPredicate allowed)
	{
		if (wikiName == null || allowed == null)
		{
			return Optional.empty();
		}
		for (String candidate : candidateNames(wikiName))
		{
			Optional<ResolvedItem> resolved = resolveExact(candidate, allowed);
			if (!resolved.isPresent())
			{
				resolved = resolveGeneric(candidate, allowed);
			}
			if (resolved.isPresent())
			{
				return resolved;
			}
		}
		return Optional.empty();
	}

	private static List<String> candidateNames(String wikiName)
	{
		Set<String> names = new LinkedHashSet<>();
		String raw = wikiName.trim();
		names.add(raw);
		String base = LEADING_QUANTITY.matcher(raw).replaceFirst("");
		base = TRAILING_QUANTITY.matcher(base).replaceFirst("").trim();
		names.add(base);
		if (!base.endsWith(")"))
		{
			names.add(base + "(4)");
		}
		String noParen = TRAILING_PAREN.matcher(base).replaceFirst("");
		if (!noParen.isEmpty())
		{
			names.add(noParen);
		}
		// a bare trailing count with no "x" ("Coins 10000" -> "Coins"). Tried
		// after the exact name, so genuinely numbered items ("Ardougne cloak
		// 4") still resolve as themselves first.
		String noNumber = TRAILING_NUMBER.matcher(base).replaceFirst("");
		if (!noNumber.isEmpty() && !noNumber.equals(base))
		{
			names.add(noNumber);
		}
		// storage sacks are named by their closed form; the wiki writes the
		// open state ("Open herb sack" -> "Herb sack")
		if (base.regionMatches(true, 0, "Open ", 0, 5))
		{
			names.add(base.substring(5));
		}
		if (base.length() > 3 && base.endsWith("s"))
		{
			String singular = base.substring(0, base.length() - 1);
			names.add(singular);
			names.add(singular + "(4)");
		}
		names.remove("");
		return new ArrayList<>(names);
	}

	/** Generic wiki names ("Imbued god cape") accept any of their concrete items. */
	private Optional<ResolvedItem> resolveGeneric(String name, IntPredicate allowed)
	{
		List<String> concrete = ItemAliases.genericFor(name);
		if (concrete.isEmpty())
		{
			return Optional.empty();
		}
		Integer primary = null;
		Set<Integer> acceptable = new LinkedHashSet<>();
		for (String c : concrete)
		{
			Optional<ResolvedItem> resolved = resolveExact(c, allowed);
			if (resolved.isPresent())
			{
				if (primary == null)
				{
					primary = resolved.get().getPrimaryId();
				}
				acceptable.addAll(resolved.get().getAcceptableIds());
			}
		}
		return primary == null
			? Optional.empty()
			: Optional.of(new ResolvedItem(name, primary, acceptable));
	}

	private Optional<ResolvedItem> resolveExact(String wikiName, IntPredicate allowed)
	{
		String wiki = ItemNames.normalize(wikiName);
		List<Integer> primary = new ArrayList<>();
		for (int id : index.idsByName(wiki))
		{
			if (allowed.test(id)) primary.add(id);
		}
		if (primary.isEmpty())
		{
			return Optional.empty();
		}

		Set<Integer> acceptable = new LinkedHashSet<>(primary);
		String wikiStripped = ItemNames.stripChargeDecorations(wiki);

		for (int id : primary)
		{
			for (int variant : ItemVariationMapping.getVariations(ItemVariationMapping.map(id)))
			{
				String name = index.nameById(variant);
				if (name == null)
				{
					continue;
				}
				String candidate = ItemNames.normalize(name);
				if (allowed.test(variant)
					&& (ItemNames.stripChargeDecorations(candidate).equals(wikiStripped)
					|| candidate.startsWith(wiki)
					|| candidate.endsWith(wiki)))
				{
					acceptable.add(variant);
				}
			}
		}

		// trouver-locked "(l)" variants aren't always in the variation group
		for (int id : index.idsByName(ItemNames.normalize(wiki + " (l)")))
			if (allowed.test(id)) acceptable.add(id);

		// cosmetic variants are the same item at the same tier, so an owned
		// decorated copy satisfies a recommendation for the base. They aren't
		// reliably grouped by ItemVariationMapping, so accept them by name.
		// Every marker here is unambiguous — it can only mean the decoration —
		// so none can cross into a different item. Overloaded dye prefixes
		// ("Blood", "Ice") are handled per-item in ItemAliases instead.
		for (String suffix : COSMETIC_SUFFIXES)
		{
			for (int id : index.idsByName(ItemNames.normalize(wiki + suffix)))
				if (allowed.test(id)) acceptable.add(id);
		}
		for (String prefix : COSMETIC_PREFIXES)
		{
			for (int id : index.idsByName(ItemNames.normalize(prefix + wiki)))
				if (allowed.test(id)) acceptable.add(id);
		}

		// distinct upgrade items in other variation groups (Divine rune pouch
		// for "Rune pouch") also satisfy the recommendation
		for (String upgrade : ItemAliases.upgradesFor(wiki))
		{
			resolveExact(upgrade, allowed).ifPresent(r -> acceptable.addAll(r.getAcceptableIds()));
		}

		// same-tier interchangeables (the three imbued god capes). Looked up
		// by name rather than resolved, since the relation is bidirectional
		// and resolving would recurse forever
		for (String equivalent : ItemAliases.equivalentsFor(wiki))
		{
			for (int id : index.idsByName(ItemNames.normalize(equivalent)))
				if (allowed.test(id)) acceptable.add(id);
		}

		// a divine potion satisfies its base potion (never the reverse)
		if (wiki.contains("potion") && !wiki.startsWith("divine "))
		{
			resolveExact("divine " + wiki, allowed).ifPresent(r -> acceptable.addAll(r.getAcceptableIds()));
		}

		return Optional.of(new ResolvedItem(wikiName, primary.get(0), acceptable));
	}
}
