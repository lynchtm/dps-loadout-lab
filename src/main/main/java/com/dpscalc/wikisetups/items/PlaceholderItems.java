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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The wiki fills some inventory slots with placeholder templates instead of
 * concrete items ({{Cheap food}}, {{Cheap prayer}}). Each maps to concrete
 * candidates, best-owned-first semantics applied by the resolver — so a
 * "Cheap food" slot becomes whichever of these the player actually banks.
 */
public final class PlaceholderItems
{
	private static final Map<String, List<String>> CANDIDATES = new HashMap<>();

	static
	{
		CANDIDATES.put("cheap food", Arrays.asList(
			"Shark", "Monkfish", "Lobster", "Swordfish", "Tuna", "Trout", "Cake"));
		CANDIDATES.put("high healing food", Arrays.asList(
			"Anglerfish", "Manta ray", "Dark crab", "Shark"));
		CANDIDATES.put("cheap prayer", Arrays.asList(
			"Prayer potion(4)", "Super restore(4)"));
		CANDIDATES.put("combo food", Arrays.asList(
			"Cooked karambwan", "Potato with cheese"));
	}

	private PlaceholderItems()
	{
	}

	/**
	 * Joins candidates a choice template listed for itself. Mirrors
	 * {@code InventoryParser.CHOICE_SEPARATOR} — kept here as a literal so the
	 * items package doesn't depend on the wiki package.
	 */
	private static final String CHOICE_SEPARATOR = " / ";

	/**
	 * Concrete candidates for a placeholder name, or empty if not a
	 * placeholder at all.
	 *
	 * Two sources: the table above, for templates that name a class of item
	 * without listing it ({{Cheap food}}), and the slot value itself, for
	 * templates that carry their own list ({{MinPrice|Manta ray|Tuna
	 * potato|…}}) and are joined by the parser.
	 */
	public static List<String> candidatesFor(String name)
	{
		List<String> candidates = CANDIDATES.get(ItemNames.normalize(name));
		if (candidates != null)
		{
			return candidates;
		}
		return name.contains(CHOICE_SEPARATOR)
			? Arrays.asList(name.split(Pattern.quote(CHOICE_SEPARATOR)))
			: Collections.emptyList();
	}
}
