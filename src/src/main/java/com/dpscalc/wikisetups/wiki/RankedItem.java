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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/**
 * One recommended item for a slot. Items with the same rank are same-tier
 * alternatives, listed in the wiki's order of appearance (best first).
 *
 * {@code fallbackNames} carries the other names a {{plink}} offered — its
 * display text and its picture — for when the linked target is a wiki page
 * rather than an item ("God capes#Imbuing" with txt "Imbued god cape").
 * They are only consulted if the primary name doesn't resolve, so a normal
 * link with a shortened display text is unaffected.
 */
public class RankedItem
{
	private final String name;
	private final int rank;
	@Nullable
	private final String note;
	private final List<String> fallbackNames;

	public RankedItem(String name, int rank, @Nullable String note)
	{
		this(name, rank, note, Collections.emptyList());
	}

	public RankedItem(String name, int rank, @Nullable String note, List<String> fallbackNames)
	{
		this.name = name;
		this.rank = rank;
		this.note = note;
		this.fallbackNames = fallbackNames;
	}

	public String getName() { return name; }
	public int getRank() { return rank; }
	public String getNote() { return note; }
	public List<String> getFallbackNames() { return fallbackNames; }
}
