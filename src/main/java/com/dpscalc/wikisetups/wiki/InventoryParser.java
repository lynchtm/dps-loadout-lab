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
import java.util.Collections;
import java.util.List;

import static com.dpscalc.wikisetups.wiki.GearSetupParser.findTemplateEnd;
import static com.dpscalc.wikisetups.wiki.GearSetupParser.indexOfTopLevel;
import static com.dpscalc.wikisetups.wiki.GearSetupParser.splitTopLevel;
import static com.dpscalc.wikisetups.wiki.GearSetupParser.stripMarkup;
import static com.dpscalc.wikisetups.wiki.GearSetupParser.templateName;

/**
 * Extracts {{Inventory}} templates and their trailing {{Rune pouch}}.
 *
 * Slots may be numbered ("|1=Twisted bow", Zulrah style) or positional
 * ("|Giant pouch |Large pouch |...", GotR style) — positional params count
 * empty cells too, preserving grid positions. Labels come from the nearest
 * preceding tabber pane label or section heading, since the template has no
 * style param of its own. A {{Rune pouch}} attaches to the inventory that
 * precedes it.
 */
public class InventoryParser
{
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(InventoryParser.class);
	private static final int SLOTS = 28;
	private static final int RUNE_SLOTS = 4;

	/**
	 * Joins the candidates of a choice template into one slot value. No item
	 * name contains it, so splitting back out is unambiguous.
	 */
	public static final String CHOICE_SEPARATOR = " / ";

	private static final class Pending
	{
		final String label;
		final List<String> slots;
		List<String> runes = Collections.emptyList();
        List<Integer> quantities=Collections.emptyList(), runeQuantities=Collections.emptyList();
        List<Boolean> noted=Collections.emptyList();

		Pending(String label, List<String> slots)
		{
			this.label = label;
			this.slots = slots;
		}
	}

	public List<InventorySetup> parse(String rawWikitext)
	{
		List<Pending> pending = new ArrayList<>();
		if (rawWikitext == null || rawWikitext.isEmpty())
		{
			return Collections.emptyList();
		}
		String wikitext = GearSetupParser.normalizeLineEndings(rawWikitext);

		int i = 0;
		while ((i = wikitext.indexOf("{{", i)) >= 0)
		{
			int end = findTemplateEnd(wikitext, i);
			if (end < 0)
			{
				i += 2;
				continue;
			}
			String body = wikitext.substring(i + 2, end - 2);
			String name = templateName(body);
			if (name.equalsIgnoreCase("inventory"))
			{
				List<String> slots = parseSlots(body, SLOTS);
				if (slots != null)
				{
					Pending entry=new Pending(labelFor(wikitext, i, pending.size() + 1), slots);
                    List<String> raw=rawSlots(body,SLOTS);entry.quantities=new ArrayList<>();entry.noted=new ArrayList<>();
                    for(String value:raw){entry.quantities.add(quantity(value));entry.noted.add(value.contains(";n"));}pending.add(entry);
				}
				i = end;
			}
			else if (name.equalsIgnoreCase("rune pouch"))
			{
				List<String> runes = parseSlots(body, RUNE_SLOTS);
				if (runes != null && !pending.isEmpty())
				{
					Pending last = pending.get(pending.size() - 1);
					if (last.runes.isEmpty())
					{
						last.runes = compact(runes);
                        last.runeQuantities=new ArrayList<>();List<String> raw=rawSlots(body,RUNE_SLOTS);
                        for(int r=0;r<runes.size();r++)if(!runes.get(r).isEmpty())last.runeQuantities.add(quantity(raw.get(r)));
					}
				}
				i = end;
			}
			else
			{
				i += 2;
			}
		}

		List<InventorySetup> setups = new ArrayList<>(pending.size());
		for (Pending p : pending)
		{
			setups.add(new InventorySetup(p.label,p.slots,p.runes,p.quantities,p.runeQuantities,p.noted));
		}
		return setups;
	}

	/**
	 * Numbered and positional params both fill slots; positional params count
	 * empty cells so grid positions survive. Returns null when nothing filled.
	 */
	private static List<String> parseSlots(String templateBody, int maxSlots)
	{
		List<String> slots = new ArrayList<>(maxSlots);
		for (int s = 0; s < maxSlots; s++)
		{
			slots.add("");
		}

		List<String> parts = splitTopLevel(templateBody, '|');
		int positional = 0;
		int filled = 0;
		for (int p = 1; p < parts.size(); p++)
		{
			String part = parts.get(p);
			int eq = indexOfTopLevel(part, '=');
			int slot;
			String value;
			if (eq < 0)
			{
				slot = ++positional;
				value = part;
			}
			else
			{
				String name = part.substring(0, eq).trim();
				try
				{
					slot = Integer.parseInt(name);
				}
				catch (NumberFormatException e)
				{
					continue; // align=, buttons=, etc.
				}
				value = part.substring(eq + 1);
			}
			if (slot < 1 || slot > maxSlots)
			{
				continue;
			}

			String item = cleanSlotValue(value);
			if (!item.isEmpty())
			{
				slots.set(slot - 1, item);
				filled++;
			}
		}
		return filled == 0 ? null : slots;
	}


    private static List<String> rawSlots(String body,int limit){
        List<String> raw=new ArrayList<>(Collections.nCopies(limit,""));List<String> parts=splitTopLevel(body,'|');int positional=0;
        for(int p=1;p<parts.size();p++){String value=parts.get(p);int eq=indexOfTopLevel(value,'=');int slot;
            if(eq<0)slot=++positional;else {try{slot=Integer.parseInt(value.substring(0,eq).trim());}catch(NumberFormatException e){continue;}value=value.substring(eq+1);}
            if(slot>0&&slot<=limit)raw.set(slot-1,value);
        }return raw;
    }
    static int quantity(String raw){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\\\\\s*([0-9][0-9,]*)").matcher(raw);
        if(!m.find())return 0;try{return Integer.parseInt(m.group(1).replace(",",""));}catch(NumberFormatException e){return 0;}
    }

    /** Drops empty cells, preserving order — rune lists have no grid meaning. */
	private static List<String> compact(List<String> slots)
	{
		List<String> out = new ArrayList<>();
		for (String s : slots)
		{
			if (!s.isEmpty())
			{
				out.add(s);
			}
		}
		return out;
	}

	/**
	 * Plain names pass through; a value that is only a placeholder template
	 * ({{Cheap food}}) yields that template's name so it surfaces as
	 * unrecognized downstream instead of vanishing silently.
	 */
	static String cleanSlotValue(String value)
	{
		String stripped = stripQuantity(stripMarkup(value));
		if (stripped.equalsIgnoreCase("placeholder"))
		{
			// a deliberately empty grid cell (Maniacal monkeys write "|16=Placeholder")
			return "";
		}
		if (!stripped.isEmpty())
		{
			return stripped;
		}
		int t = value.indexOf("{{");
		if (t >= 0)
		{
			int end = findTemplateEnd(value, t);
			if (end > 0)
			{
				String body = value.substring(t + 2, end - 2);
				String choices = choiceList(body);
				return choices.isEmpty() ? templateName(body) : choices;
			}
		}
		return "";
	}

	/**
	 * Some templates carry their own candidates rather than naming one item:
	 * {{MinPrice|format=item|link=n|Manta ray|Tuna potato|Anglerfish|Dark
	 * crab}} means "whichever of these is cheapest". Their unnamed parameters
	 * are the candidate list, emitted joined so the resolver can treat it the
	 * same way as a hand-listed placeholder.
	 *
	 * Empty unless there are at least two candidates — a single unnamed
	 * parameter is far more likely to be an argument than a choice.
	 */
	static String choiceList(String templateBody)
	{
		List<String> parts = splitTopLevel(templateBody, '|');
		List<String> choices = new ArrayList<>();
		for (int p = 1; p < parts.size(); p++)
		{
			if (indexOfTopLevel(parts.get(p), '=') >= 0)
			{
				continue; // format=, link=, align= …
			}
			String choice = stripMarkup(parts.get(p));
			if (!choice.isEmpty())
			{
				choices.add(choice);
			}
		}
		return choices.size() < 2 ? "" : String.join(CHOICE_SEPARATOR, choices);
	}

	/**
	 * Strips the two markers the template appends to a name: a stack size
	 * after a backslash ("Blood rune\16000", overwhelmingly in {{Rune pouch}})
	 * and a ";n" noted flag ("Iron ore;n" on restock inventories). Values
	 * carry both ("Shark\200;n"). No item name contains either character, so
	 * everything from the first one on is a marker, not part of the name.
	 */
	static String stripQuantity(String value)
	{
		int cut = value.length();
		int slash = value.indexOf('\\');
		if (slash >= 0)
		{
			cut = slash;
		}
		int semi = value.indexOf(';');
		if (semi >= 0 && semi < cut)
		{
			cut = semi;
		}
		return cut == value.length() ? value : value.substring(0, cut).trim();
	}

	/** The pane/heading that distinguishes this inventory, else "Inventory N". */
	static String labelFor(String wikitext, int templateStart, int index)
	{
		String label = SectionLabels.precedingLabel(wikitext, templateStart);
		return label != null ? label : "Inventory " + index;
	}
}
