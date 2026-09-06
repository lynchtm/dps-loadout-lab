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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageKindTest
{
	@Test
	public void slayerTaskPagesAreRecognised()
	{
		assertEquals(PageKind.SLAYER_TASK, PageKind.of("Slayer task/Abyssal demons"));
		assertEquals(PageKind.SLAYER_TASK, PageKind.of("Slayer task/Gargoyles"));
	}

	@Test
	public void guidePagesAreRecognised()
	{
		assertEquals(PageKind.GUIDE, PageKind.of("Guide:Theatre of Blood setups"));
		assertEquals(PageKind.GUIDE, PageKind.of("Ultimate Ironman Guide/Equipment"));
		assertEquals(PageKind.GUIDE, PageKind.of("Ultimate Ironman Guide/Slayer"));
	}

	@Test
	public void bossStrategyPagesHaveNoKind()
	{
		assertEquals(PageKind.NONE, PageKind.of("Zulrah/Strategies"));
		assertEquals(PageKind.NONE, PageKind.of("General Graardor/Strategies"));
	}

	/** Combat Achievement pages follow no convention — deliberately unlabelled. */
	@Test
	public void combatAchievementPagesStayUnlabelled()
	{
		assertEquals(PageKind.NONE, PageKind.of("Morytania Only"));
		assertEquals(PageKind.NONE, PageKind.of("Budget Cutter"));
		assertEquals(PageKind.NONE, PageKind.of("Fight Caves Speed-Runner"));
	}

	@Test
	public void nullTitleIsNone()
	{
		assertEquals(PageKind.NONE, PageKind.of(null));
	}
}
