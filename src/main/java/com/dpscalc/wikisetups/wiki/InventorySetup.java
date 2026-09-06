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

import java.util.List;

/**
 * One parsed {{Inventory}} template: a label (tabber pane / heading it sat
 * under), up to 28 slot item names in wiki slot order (index = slot - 1,
 * empty slots are empty strings), and the runes of the {{Rune pouch}}
 * template that followed it, if any. Values that were placeholder templates
 * ({{Cheap food}}) become the template's name and will simply fail item
 * resolution, surfacing as "unrecognized" in the preview.
 */
public class InventorySetup
{
	private final String label;
	private final List<String> slots;
	private final List<String> runes;
    private List<Integer> quantities, runeQuantities;
    private List<Boolean> noted;
    public InventorySetup(String label,List<String> slots,List<String> runes,List<Integer> quantities,List<Integer> runeQuantities,List<Boolean> noted){this(label,slots,runes);this.quantities=quantities;this.runeQuantities=runeQuantities;this.noted=noted;}
    public int quantity(int slot,boolean rune){List<Integer> q=rune?runeQuantities:quantities;return q==null||slot>=q.size()?0:q.get(slot);}
    public boolean noted(int slot){return noted!=null&&slot<noted.size()&&noted.get(slot);}

	public InventorySetup(String label, List<String> slots, List<String> runes) { this.label = label; this.slots = slots; this.runes = runes; }
	public String getLabel() { return label; }
	public List<String> getSlots() { return slots; }
	public List<String> getRunes() { return runes; }
}
