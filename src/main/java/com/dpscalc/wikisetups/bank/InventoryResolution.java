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
package com.dpscalc.wikisetups.bank;

import com.dpscalc.wikisetups.wiki.InventorySetup;
import java.util.List;

/**
 * missingCount covers inventory slots and runes together (one per slot);
 * missingNames is the deduplicated wiki names behind that count. runePicks
 * use slot = index within the rune list (0-3), not a grid position.
 */
public class InventoryResolution
{
	private final InventorySetup setup;
	private final List<InventoryPick> picks;
	private final List<InventoryPick> runePicks;
	private final List<String> unresolvedNames;
	private final List<String> missingNames;
	private final int missingCount;

	public InventoryResolution(InventorySetup setup, List<InventoryPick> picks, List<InventoryPick> runePicks, List<String> unresolvedNames, List<String> missingNames, int missingCount) { this.setup = setup; this.picks = picks; this.runePicks = runePicks; this.unresolvedNames = unresolvedNames; this.missingNames = missingNames; this.missingCount = missingCount; }
	public InventorySetup getSetup() { return setup; }
	public List<InventoryPick> getPicks() { return picks; }
	public List<InventoryPick> getRunePicks() { return runePicks; }
	public List<String> getUnresolvedNames() { return unresolvedNames; }
	public List<String> getMissingNames() { return missingNames; }
	public int getMissingCount() { return missingCount; }
}
