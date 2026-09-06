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

/**
 * Interchangeable-consumable classes, best first. When a recommended
 * consumable isn't owned, the best owned member of its class substitutes —
 * an unowned Shark slot becomes your Monkfish rather than a goal item.
 * Groups are keyed by every member (dose-stripped), so "Prayer potion"
 * finds the restore group regardless of how the wiki wrote the dose.
 */
public final class ConsumableGroups
{
	private static final Map<String, List<String>> BY_MEMBER = new HashMap<>();

	static
	{
		group(
			"Anglerfish", "Dark crab", "Manta ray", "Sea turtle", "Shark",
			"Monkfish", "Potato with cheese", "Bass", "Lobster", "Swordfish",
			"Tuna", "Salmon", "Trout", "Cake");
		group("Super restore(4)", "Sanfew serum(4)", "Prayer potion(4)");
	}

	private static void group(String... members)
	{
		List<String> list = Arrays.asList(members);
		for (String member : members)
		{
			BY_MEMBER.put(key(member), list);
		}
	}

	private static String key(String name)
	{
		return ItemNames.stripChargeDecorations(ItemNames.normalize(name));
	}

	private ConsumableGroups()
	{
	}

	/** The item's class, best first, or empty if it isn't a grouped consumable. */
	public static List<String> groupFor(String name)
	{
		List<String> group = BY_MEMBER.get(key(name));
		return group == null ? Collections.emptyList() : group;
	}
}
