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

import com.dpscalc.wikisetups.wiki.EquipSlot;
import com.dpscalc.wikisetups.wiki.GearSetup;
import java.util.List;
import java.util.Map;

/**
 * A gear setup resolved against what the player owns. Slots the player has
 * nothing for land in emptySlots (unless config asked for goal items).
 * unresolvedNames holds wiki names the item index couldn't map — surfaced in
 * the preview so failures are never silent.
 */
public class SetupResolution
{
	private final GearSetup setup;
	private final List<SlotPick> picks;
	private final Map<EquipSlot, List<String>> unresolvedNames;
	private final List<EquipSlot> emptySlots;

	public SetupResolution(GearSetup setup, List<SlotPick> picks, Map<EquipSlot, List<String>> unresolvedNames, List<EquipSlot> emptySlots) { this.setup = setup; this.picks = picks; this.unresolvedNames = unresolvedNames; this.emptySlots = emptySlots; }
	public GearSetup getSetup() { return setup; }
	public List<SlotPick> getPicks() { return picks; }
	public Map<EquipSlot, List<String>> getUnresolvedNames() { return unresolvedNames; }
	public List<EquipSlot> getEmptySlots() { return emptySlots; }
}
