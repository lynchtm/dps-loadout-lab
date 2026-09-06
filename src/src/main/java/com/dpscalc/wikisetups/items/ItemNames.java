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

import java.util.Locale;

/** Name normalization shared by the index and the resolver. */
public final class ItemNames
{
	private ItemNames()
	{
	}

	/**
	 * Lowercase, straight apostrophes, collapsed whitespace, and no space
	 * before parentheses — the game is inconsistent ("Salve amulet(i)" but
	 * "Ring of suffering (i)") and the wiki always writes the space.
	 *
	 * Underscores become spaces: links are sometimes written in URL form
	 * ("{{plink|Blessed_bracers}}"), and no item name contains one.
	 */
	public static String normalize(String name)
	{
		return name
			.replace('’', '\'')
			.replace('_', ' ')
			.toLowerCase(Locale.ROOT)
			.replaceAll("\\s+\\(", "(")
			.replaceAll("\\s+", " ")
			.trim();
	}

	/**
	 * Removes charge/degradation decorations so variants compare equal to
	 * their base: parenthesized numbers "(10)" and a trailing bare number
	 * ("Karil's leathertop 100"). Non-numeric parens like "(i)" are kept —
	 * they distinguish genuinely different items.
	 */
	public static String stripChargeDecorations(String normalizedName)
	{
		return normalizedName
			.replaceAll("\\(\\d+\\)", "")
			.replaceAll("\\s+\\d+$", "")
			.replaceAll("\\s+", " ")
			.trim();
	}
}
