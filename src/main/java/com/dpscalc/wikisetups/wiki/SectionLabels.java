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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.dpscalc.wikisetups.wiki.GearSetupParser.stripMarkup;

/**
 * The label a template inherits from the tabber pane or section heading it
 * sits under. Templates carry no name of their own, and the wiki's own
 * `|style=` parameter is missing or duplicated often enough that this is the
 * better distinguisher on many pages ("Melee"/"Melee"/"Melee" as styles, but
 * "Lower Level"/"Dharok's"/"Obsidian" as panes).
 *
 * Shared by {@link InventoryParser} and {@link GearSetupParser} so there is
 * one pane-label pattern, not two that can drift apart.
 */
final class SectionLabels
{
	/**
	 * Tabber pane openers: "<tabber>Label=", "|-|Label=", or the label on a
	 * later line — blank lines between the separator and the label are common
	 * and must not break the match.
	 */
	private static final Pattern PANE_LABEL =
		Pattern.compile("(?m)^(?:<tabber>|\\|-\\|)[ \\t]*(?:\\n[ \\t]*)*([^=\\n|{}]+?)[ \\t]*=");
	private static final Pattern HEADING = Pattern.compile("(?m)^={2,5}\\s*(.+?)\\s*={2,5}\\s*$");

	/**
	 * Headings that describe what a template <em>is</em> rather than which
	 * setup it belongs to. Pages routinely put "==Inventory==" or
	 * "==Equipment==" immediately above every template, which would otherwise
	 * give them all the same label.
	 */
	private static final Set<String> GENERIC = new HashSet<>(Arrays.asList(
		"inventory", "inventories", "equipment", "gear", "setup", "setups",
		"loadout", "loadouts", "items", "recommended equipment", "notes",
		"items protected:", "items lost:", "equipment upgrades and downgrades"));

	private SectionLabels()
	{
	}

	/**
	 * The nearest preceding pane or heading label that actually distinguishes
	 * this template, or {@code null} if there is none.
	 *
	 * Generic headings are stepped over in favour of the enclosing pane label
	 * further back, and only fall through to the nearest generic one when
	 * nothing better exists — so a page that heads every template
	 * "==Inventory==" still finds its "Melee" / "Magic" pane labels.
	 */
	static String precedingLabel(String wikitext, int templateStart)
	{
		String before = wikitext.substring(0, templateStart);
		NavigableMap<Integer, String> candidates = new TreeMap<>();

		Matcher pane = PANE_LABEL.matcher(before);
		while (pane.find())
		{
			candidates.put(pane.start(), pane.group(1).trim());
		}
		Matcher heading = HEADING.matcher(before);
		while (heading.find())
		{
			candidates.put(heading.start(), stripMarkup(heading.group(1)));
		}

		String nearest = null;
		for (String label : candidates.descendingMap().values())
		{
			if (label.isEmpty())
			{
				continue;
			}
			if (nearest == null)
			{
				nearest = label;
			}
			if (!GENERIC.contains(label.toLowerCase(Locale.ROOT)))
			{
				return label;
			}
		}
		return nearest;
	}
}
