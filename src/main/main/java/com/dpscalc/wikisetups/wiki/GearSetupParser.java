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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts {{Recommended equipment}} templates from raw wikitext.
 *
 * Deliberately tolerant: a malformed slot value is logged and skipped, never
 * fatal for the page. Only {{plink}}-family targets count as items — bare
 * wiki links in slot values are prose (see docs/wiki-format.md).
 */
public class GearSetupParser
{
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GearSetupParser.class);
	private static final String TEMPLATE_NAME = "recommended equipment";
	private static final Pattern SLOT_PARAM = Pattern.compile("([a-z]+)([0-9]+)");
	private static final Pattern HTML_COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
	/** A balanced parenthetical qualifying the item that precedes it: "(on task)". */
	private static final Pattern PARENTHETICAL = Pattern.compile("\\(([^()]+)\\)");
	private static final Pattern REF_BODY =
		Pattern.compile("<ref[^>]*>(.*?)</ref>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
	private static final Pattern PIPED_LINK = Pattern.compile("\\[\\[[^\\]|]*\\|([^\\]]*)\\]\\]");
	private static final Pattern PLAIN_LINK = Pattern.compile("\\[\\[([^\\]|]*)\\]\\]");
	private static final Pattern NESTED_TEMPLATE = Pattern.compile("\\{\\{[^{}]*\\}\\}");

	public List<GearSetup> parse(String rawWikitext)
	{
		List<GearSetup> setups = new ArrayList<>();
		if (rawWikitext == null || rawWikitext.isEmpty())
		{
			return setups;
		}
		String wikitext = normalizeLineEndings(rawWikitext);

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
			if (templateName(body).equalsIgnoreCase(TEMPLATE_NAME))
			{
				GearSetup setup = parseSetup(body, setups.size() + 1,
					SectionLabels.precedingLabel(wikitext, i));
				if (setup != null)
				{
					setups.add(setup);
				}
				i = end;
			}
			else
			{
				// step past the opening braces only, so templates nested
				// inside non-matching wrappers (e.g. tabber helpers) are found
				i += 2;
			}
		}
		return setups;
	}

	private GearSetup parseSetup(String templateBody, int index, String paneLabel)
	{
		List<String> parts = splitTopLevel(templateBody, '|');
		String style = null;
		Map<EquipSlot, TreeMap<Integer, List<RankedItem>>> ranked = new EnumMap<>(EquipSlot.class);

		for (int p = 1; p < parts.size(); p++)
		{
			String part = parts.get(p);
			int eq = indexOfTopLevel(part, '=');
			if (eq < 0)
			{
				continue;
			}
			String name = part.substring(0, eq).trim().toLowerCase(Locale.ROOT);
			String value = part.substring(eq + 1);

			try
			{
				if (name.equals("style"))
				{
					String s = stripMarkup(value);
					if (!s.isEmpty())
					{
						style = s;
					}
					continue;
				}

				Matcher m = SLOT_PARAM.matcher(name);
				if (!m.matches())
				{
					continue;
				}
				EquipSlot slot = EquipSlot.fromWikiParam(m.group(1));
				if (slot == null)
				{
					continue;
				}
				int rank = Integer.parseInt(m.group(2));

				List<RankedItem> items = parseItems(value, rank);
				if (!items.isEmpty())
				{
					ranked.computeIfAbsent(slot, k -> new TreeMap<>()).put(rank, items);
				}
			}
			catch (RuntimeException e)
			{
				log.warn("Skipping malformed slot param '{}'", name, e);
			}
		}

		Map<EquipSlot, List<RankedItem>> slots = new EnumMap<>(EquipSlot.class);
		int total = 0;
		for (Map.Entry<EquipSlot, TreeMap<Integer, List<RankedItem>>> e : ranked.entrySet())
		{
			List<RankedItem> flat = new ArrayList<>();
			for (List<RankedItem> tier : e.getValue().values())
			{
				flat.addAll(tier);
			}
			slots.put(e.getKey(), flat);
			total += flat.size();
		}

		if (total == 0)
		{
			return null;
		}
		return new GearSetup(style != null ? style : "Setup " + index, slots, paneLabel);
	}

	private List<RankedItem> parseItems(String value, int rank)
	{
		List<RankedItem> items = new ArrayList<>();
		String v = HTML_COMMENT.matcher(value).replaceAll("");

		int i = 0;
		while (i < v.length())
		{
			if (!v.startsWith("{{", i))
			{
				i++;
				continue;
			}
			int end = findTemplateEnd(v, i);
			if (end < 0)
			{
				break;
			}
			String inner = v.substring(i + 2, end - 2);
			String name = templateName(inner).toLowerCase(Locale.ROOT);

			if (name.equals("plink") || name.equals("plinkp") || name.equals("plinkt"))
			{
				String item = firstPositional(inner);
				if (item != null)
				{
					item = stripMarkup(item);
					// a "#" means the target is a page section, never an item —
					// the real name is in txt=, or before the anchor
					List<String> fallbacks = new ArrayList<>();
					addIfPresent(fallbacks, namedParam(inner, "txt"));
					addIfPresent(fallbacks, namedParam(inner, "pic"));
					int anchor = item.indexOf('#');
					if (anchor >= 0)
					{
						String beforeAnchor = item.substring(0, anchor).trim();
						item = fallbacks.isEmpty() ? beforeAnchor : fallbacks.remove(0);
						addIfPresent(fallbacks, beforeAnchor);
					}
					// "+{{plinkp|Trouver parchment}}" marks a companion item
					// (lock the gear at Perdu), not a wearable alternative
					if (!item.isEmpty() && !item.equalsIgnoreCase("n/a") && !item.equalsIgnoreCase("none")
						&& !item.equalsIgnoreCase("trouver parchment") && !isCompanion(v, i))
					{
						items.add(new RankedItem(item, rank, annotationAfter(v, end), fallbacks));
					}
				}
			}
			else if (name.equals("efn") && !items.isEmpty())
			{
				String note = firstPositional(inner);
				if (note != null)
				{
					note = stripMarkup(note);
					if (!note.isEmpty())
					{
						RankedItem last = items.get(items.size() - 1);
						String merged = last.getNote() == null ? note : last.getNote() + "; " + note;
						items.set(items.size() - 1,
							new RankedItem(last.getName(), rank, merged, last.getFallbackNames()));
					}
				}
			}
			i = end;
		}
		return items;
	}

	/**
	 * A companion item rather than a wearable alternative: written attached to
	 * the plus, "{{plink|Fighter torso}}+{{plinkp|Trouver parchment}}".
	 *
	 * The plus must touch the template. Pages also use a <em>spaced</em> plus
	 * to mean "and bring this too" — Fortis Colosseum lists a whole switch
	 * loadout that way ("Osmumten's fang + Bow of faerdhinen + Sanguinesti
	 * staff") — and treating those as companions silently dropped real gear
	 * from the slot.
	 */
	private static boolean isCompanion(String s, int templateStart)
	{
		return templateStart > 0 && s.charAt(templateStart - 1) == '+';
	}

	/**
	 * Plain text between this template and the next, as a note.
	 *
	 * Roughly half the pages qualify a recommendation with a bare
	 * parenthetical — "{{plink|Slayer helmet (i)}} (on task)" — rather than an
	 * {{efn}} footnote. Without this those conditions are invisible, and the
	 * item gets recommended unconditionally.
	 *
	 * Only balanced parentheses and &lt;ref&gt; bodies count, so separators
	 * ("/", "&lt;br&gt;") and the "(or:{{plinkp|…}}" alternative-list opener
	 * yield nothing.
	 */
	static String annotationAfter(String value, int templateEnd)
	{
		int next = value.indexOf("{{", templateEnd);
		String trailing = value.substring(templateEnd, next < 0 ? value.length() : next);
		if (trailing.isEmpty())
		{
			return null;
		}

		StringBuilder note = new StringBuilder();
		Matcher ref = REF_BODY.matcher(trailing);
		while (ref.find())
		{
			append(note, stripMarkup(ref.group(1)));
		}
		Matcher paren = PARENTHETICAL.matcher(REF_BODY.matcher(trailing).replaceAll(""));
		while (paren.find())
		{
			append(note, stripMarkup(paren.group(1)));
		}
		return note.length() == 0 ? null : note.toString();
	}

	private static void append(StringBuilder note, String text)
	{
		if (text != null && !text.trim().isEmpty())
		{
			if (note.length() > 0)
			{
				note.append("; ");
			}
			note.append(text.trim());
		}
	}

	/**
	 * From the index of an opening "{{", returns the index just past its
	 * matching "}}", or -1 if unbalanced.
	 */
	static int findTemplateEnd(String s, int start)
	{
		int depth = 0;
		int i = start;
		while (i < s.length() - 1)
		{
			if (s.charAt(i) == '{' && s.charAt(i + 1) == '{')
			{
				depth++;
				i += 2;
			}
			else if (s.charAt(i) == '}' && s.charAt(i + 1) == '}')
			{
				depth--;
				i += 2;
				if (depth == 0)
				{
					return i;
				}
			}
			else
			{
				i++;
			}
		}
		return -1;
	}

	/** Template name: body text up to the first pipe (or whole body). */
	static String templateName(String templateBody)
	{
		int pipe = templateBody.indexOf('|');
		String name = pipe < 0 ? templateBody : templateBody.substring(0, pipe);
		return name.trim();
	}

	/** First non-empty positional (un-named) parameter of a template body, or null. */
	private static void addIfPresent(List<String> out, String value)
	{
		if (value != null && !value.isEmpty() && !out.contains(value))
		{
			out.add(value);
		}
	}

	/** Value of a named template param ("txt", "pic"), markup stripped, or null. */
	static String namedParam(String templateBody, String param)
	{
		for (String part : splitTopLevel(templateBody, '|'))
		{
			int eq = indexOfTopLevel(part, '=');
			if (eq > 0 && part.substring(0, eq).trim().equalsIgnoreCase(param))
			{
				String value = stripMarkup(part.substring(eq + 1));
				return value.isEmpty() ? null : value;
			}
		}
		return null;
	}

	static String firstPositional(String templateBody)
	{
		List<String> parts = splitTopLevel(templateBody, '|');
		for (int i = 1; i < parts.size(); i++)
		{
			String part = parts.get(i);
			if (indexOfTopLevel(part, '=') < 0 && !part.trim().isEmpty())
			{
				return part.trim();
			}
		}
		return null;
	}

	/** Splits on sep, ignoring occurrences nested inside {{...}} or [[...]]. */
	static List<String> splitTopLevel(String s, char sep)
	{
		List<String> parts = new ArrayList<>();
		int braceDepth = 0;
		int bracketDepth = 0;
		StringBuilder cur = new StringBuilder();
		int i = 0;
		while (i < s.length())
		{
			if (s.startsWith("{{", i))
			{
				braceDepth++;
				cur.append("{{");
				i += 2;
			}
			else if (s.startsWith("}}", i))
			{
				braceDepth = Math.max(0, braceDepth - 1);
				cur.append("}}");
				i += 2;
			}
			else if (s.startsWith("[[", i))
			{
				bracketDepth++;
				cur.append("[[");
				i += 2;
			}
			else if (s.startsWith("]]", i))
			{
				bracketDepth = Math.max(0, bracketDepth - 1);
				cur.append("]]");
				i += 2;
			}
			else if (s.charAt(i) == sep && braceDepth == 0 && bracketDepth == 0)
			{
				parts.add(cur.toString());
				cur.setLength(0);
				i++;
			}
			else
			{
				cur.append(s.charAt(i));
				i++;
			}
		}
		parts.add(cur.toString());
		return parts;
	}

	/** Index of the first top-level occurrence of c, or -1. */
	static int indexOfTopLevel(String s, char c)
	{
		int braceDepth = 0;
		int bracketDepth = 0;
		int i = 0;
		while (i < s.length())
		{
			if (s.startsWith("{{", i))
			{
				braceDepth++;
				i += 2;
			}
			else if (s.startsWith("}}", i))
			{
				braceDepth = Math.max(0, braceDepth - 1);
				i += 2;
			}
			else if (s.startsWith("[[", i))
			{
				bracketDepth++;
				i += 2;
			}
			else if (s.startsWith("]]", i))
			{
				bracketDepth = Math.max(0, bracketDepth - 1);
				i += 2;
			}
			else
			{
				if (s.charAt(i) == c && braceDepth == 0 && bracketDepth == 0)
				{
					return i;
				}
				i++;
			}
		}
		return -1;
	}

	/**
	 * Reduces wiki markup to display text: [[a|b]] -> b, [[a]] -> a, nested
	 * templates dropped, br tags to spaces, whitespace collapsed.
	 */
	/**
	 * Line-based patterns (tabber labels, headings, table markup) must not
	 * see stray carriage returns. The wiki API serves LF, but a fixture
	 * checked out with CRLF — or any future caller — must parse identically.
	 */
	static String normalizeLineEndings(String s)
	{
		return s.indexOf('\r') < 0 ? s : s.replace("\r\n", "\n").replace('\r', '\n');
	}

	static String stripMarkup(String s)
	{
		String out = s;
		String prev;
		do
		{
			prev = out;
			out = NESTED_TEMPLATE.matcher(out).replaceAll("");
		}
		while (!out.equals(prev));
		out = PIPED_LINK.matcher(out).replaceAll("$1");
		out = PLAIN_LINK.matcher(out).replaceAll("$1");
		out = out.replaceAll("(?i)<br\\s*/?>", " ");
		out = out.replaceAll("''+", "");
		out = out.replaceAll("\\s+", " ");
		return out.trim();
	}
}
