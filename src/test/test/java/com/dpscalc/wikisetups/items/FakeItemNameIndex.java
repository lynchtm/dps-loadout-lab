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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Map-backed index for tests. Use real item ids so ItemVariationMapping works. */
public class FakeItemNameIndex implements ItemNameIndex
{
	private final Map<Integer, String> names = new HashMap<>();
	private final Map<String, List<Integer>> byName = new HashMap<>();

	/** Alternating id, name pairs. */
	public static FakeItemNameIndex of(Object... idNamePairs)
	{
		FakeItemNameIndex index = new FakeItemNameIndex();
		for (int i = 0; i < idNamePairs.length; i += 2)
		{
			index.add((Integer) idNamePairs[i], (String) idNamePairs[i + 1]);
		}
		return index;
	}

	public void add(int id, String name)
	{
		names.put(id, name);
		byName.computeIfAbsent(ItemNames.normalize(name), k -> new ArrayList<>()).add(id);
	}

	@Override
	public List<Integer> idsByName(String normalizedName)
	{
		return byName.getOrDefault(normalizedName, Collections.emptyList());
	}

	@Override
	public String nameById(int itemId)
	{
		return names.get(itemId);
	}

	@Override
	public boolean isReady()
	{
		return true;
	}
}
