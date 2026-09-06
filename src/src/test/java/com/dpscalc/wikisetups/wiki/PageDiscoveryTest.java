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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PageDiscoveryTest
{
	@Test
	public void basePageProbesGuideThenStrategies()
	{
		assertEquals(Arrays.asList("Guide:Zulrah setups", "Zulrah/Strategies", "Zulrah"),
			PageDiscovery.candidatesFor("Zulrah"));
	}

	@Test
	public void explicitSubpageComesAfterGuideWithoutDuplicates()
	{
		assertEquals(Arrays.asList("Guide:Zulrah setups", "Zulrah/Strategies", "Zulrah"),
			PageDiscovery.candidatesFor("Zulrah/Strategies"));
	}

	@Test
	public void nonStrategySubpageIsKeptAheadOfStrategies()
	{
		assertEquals(
			Arrays.asList("Guide:Money making guide setups", "Money making guide/Killing Zulrah",
				"Money making guide/Strategies", "Money making guide"),
			PageDiscovery.candidatesFor("Money making guide/Killing Zulrah"));
	}

	@Test
	public void guidePagesDoNotProbeGuideAgain()
	{
		assertEquals(
			Arrays.asList("Guide:Theatre of Blood setups/Strategies", "Guide:Theatre of Blood setups"),
			PageDiscovery.candidatesFor("Guide:Theatre of Blood setups"));
	}
}
