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
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Gear setups usually live on a "/Strategies" subpage of the page users
 * actually search for; some raids keep curated loadouts on a
 * "Guide:&lt;name&gt; setups" page instead (Theatre of Blood). Candidate
 * titles are tried in order until one parses to a non-empty setup list —
 * a missing Guide page fails fast and falls through.
 */
public final class PageDiscovery
{
	private PageDiscovery()
	{
	}

	public static List<String> candidatesFor(String title)
	{
		String base = title;
		int slash = title.indexOf('/');
		if (slash > 0)
		{
			base = title.substring(0, slash);
		}

		LinkedHashSet<String> candidates = new LinkedHashSet<>();
		// a curated Guide page outranks even an explicit subpage — search
		// returns "/Strategies" results, but ToB's real setups live on
		// "Guide:Theatre of Blood setups"; missing guides fail fast
		if (!base.startsWith("Guide:"))
		{
			candidates.add("Guide:" + base + " setups");
		}
		if (slash > 0)
		{
			candidates.add(title);
		}
		candidates.add(base + "/Strategies");
		candidates.add(base);
		return new ArrayList<>(candidates);
	}
}
