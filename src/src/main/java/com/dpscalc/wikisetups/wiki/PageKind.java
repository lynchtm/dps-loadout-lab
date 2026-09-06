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

import javax.annotation.Nullable;

/**
 * What kind of page a search result is, told from its title alone.
 *
 * A third of the pages carrying {{Recommended equipment}} aren't boss gear —
 * 15 are Slayer task pages, and there are prose guides too. Both are served
 * well but look identical to a boss page in the results list. A one-word tag
 * beside the result makes the well-served categories discoverable and warns
 * before someone opens a 42-style equipment guide.
 *
 * Title-only on purpose: search runs on the network path and must stay cheap,
 * so Combat Achievement task pages (which follow no title convention —
 * "Budget Cutter", "Morytania Only") are deliberately left unlabelled rather
 * than pay a category lookup per result.
 */
public enum PageKind
{
	SLAYER_TASK("Slayer task"),
	GUIDE("Guide"),
	NONE(null);

	@Nullable
	private final String label;

	PageKind(@Nullable String label)
	{
		this.label = label;
	}

	@Nullable
	public String getLabel()
	{
		return label;
	}

	public static PageKind of(String title)
	{
		if (title == null)
		{
			return NONE;
		}
		if (title.startsWith("Slayer task/"))
		{
			return SLAYER_TASK;
		}
		if (title.startsWith("Guide:") || title.contains("Guide/"))
		{
			return GUIDE;
		}
		return NONE;
	}
}
