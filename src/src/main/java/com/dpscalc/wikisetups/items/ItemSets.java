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

import com.dpscalc.wikisetups.wiki.EquipSlot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Items whose recommendation is only valid as part of a set (or alongside a
 * specific weapon). Two gating modes:
 *
 * always — the piece is pointless without its set (void; crystal armour
 * without a crystal bow), so the requirement applies to every
 * recommendation of it.
 *
 * note-flagged — the piece is reasonable standalone gear (barrows, moon
 * sets, obsidian, justiciar) and is gated only when the wiki footnote on
 * that particular recommendation says "full/complete ... set". The same
 * item can appear both gated (rank 2, with note) and ungated (rank 3,
 * standalone) on one page — Amoxliatl's Dual macuahuitl does exactly this.
 *
 * Each required group carries the equipment slot it occupies so the
 * resolver can distinguish "worn in this layout" from merely "banked".
 */
public final class ItemSets
{
	/** One required slot of a set: any of these names, worn in {@code slot}. */
	public static final class PieceGroup
	{
		private final EquipSlot slot;
		private final List<String> names;

		private PieceGroup(EquipSlot slot, List<String> names)
		{
			this.slot = slot;
			this.names = names;
		}

		public EquipSlot getSlot()
		{
			return slot;
		}

		public List<String> getNames()
		{
			return names;
		}
	}

	public static final class Requirement
	{
		private final boolean always;
		private final List<PieceGroup> otherPieces;

		private Requirement(boolean always, List<PieceGroup> otherPieces)
		{
			this.always = always;
			this.otherPieces = otherPieces;
		}

		public boolean isAlways()
		{
			return always;
		}

		/** All groups must be satisfied for the piece to be worth wearing. */
		public List<PieceGroup> getOtherPieces()
		{
			return otherPieces;
		}
	}

	private static final Map<String, Requirement> BY_PIECE = new HashMap<>();

	static
	{
		armourSet(true,
			group(EquipSlot.HEAD, "Void melee helm", "Void ranger helm", "Void mage helm"),
			group(EquipSlot.BODY, "Void knight top", "Elite void top"),
			group(EquipSlot.LEGS, "Void knight robe", "Elite void robe"),
			group(EquipSlot.HANDS, "Void knight gloves"));

		// crystal armour only shines behind a crystal bow — per-piece, no set needed
		PieceGroup crystalBows = group(EquipSlot.WEAPON,
			"Bow of faerdhinen", "Bow of faerdhinen (c)", "Bow of faerdhinen (inactive)", "Crystal bow");
		for (String piece : new String[]{"Crystal helm", "Crystal body", "Crystal legs"})
		{
			BY_PIECE.put(ItemNames.normalize(piece),
				new Requirement(true, Collections.singletonList(crystalBows)));
		}

		armourSet(false,
			group(EquipSlot.HEAD, "Justiciar faceguard"),
			group(EquipSlot.BODY, "Justiciar chestguard"),
			group(EquipSlot.LEGS, "Justiciar legguards"));
		armourSet(false,
			group(EquipSlot.HEAD, "Obsidian helmet"),
			group(EquipSlot.BODY, "Obsidian platebody"),
			group(EquipSlot.LEGS, "Obsidian platelegs"));

		barrows("Ahrim's hood", "Ahrim's robetop", "Ahrim's robeskirt", "Ahrim's staff");
		barrows("Dharok's helm", "Dharok's platebody", "Dharok's platelegs", "Dharok's greataxe");
		barrows("Guthan's helm", "Guthan's platebody", "Guthan's chainskirt", "Guthan's warspear");
		barrows("Karil's coif", "Karil's leathertop", "Karil's leatherskirt", "Karil's crossbow");
		barrows("Torag's helm", "Torag's platebody", "Torag's platelegs", "Torag's hammers");
		barrows("Verac's helm", "Verac's brassard", "Verac's plateskirt", "Verac's flail");

		moonSet("Blood moon helm", "Blood moon chestplate", "Blood moon tassets", "Dual macuahuitl");
		moonSet("Blue moon helm", "Blue moon chestplate", "Blue moon tassets", "Blue moon spear");
		moonSet("Eclipse moon helm", "Eclipse moon chestplate", "Eclipse moon tassets", "Eclipse atlatl");
	}

	private static PieceGroup group(EquipSlot slot, String... names)
	{
		return new PieceGroup(slot, Arrays.asList(names));
	}

	private static void barrows(String helm, String body, String legs, String weapon)
	{
		armourSet(false,
			group(EquipSlot.HEAD, helm),
			group(EquipSlot.BODY, body),
			group(EquipSlot.LEGS, legs),
			group(EquipSlot.WEAPON, weapon));
	}

	private static void moonSet(String helm, String chest, String legs, String weapon)
	{
		armourSet(false,
			group(EquipSlot.HEAD, helm),
			group(EquipSlot.BODY, chest),
			group(EquipSlot.LEGS, legs),
			group(EquipSlot.WEAPON, weapon));
	}

	/** Registers each member of each group with a requirement of all OTHER groups. */
	private static void armourSet(boolean always, PieceGroup... pieceGroups)
	{
		for (int i = 0; i < pieceGroups.length; i++)
		{
			List<PieceGroup> others = new ArrayList<>();
			for (int j = 0; j < pieceGroups.length; j++)
			{
				if (j != i)
				{
					others.add(pieceGroups[j]);
				}
			}
			Requirement requirement = new Requirement(always, others);
			for (String piece : pieceGroups[i].getNames())
			{
				BY_PIECE.put(ItemNames.normalize(piece), requirement);
			}
		}
	}

	private ItemSets()
	{
	}

	/** The set requirement covering this item, or null if it isn't a set piece. */
	public static Requirement requirementFor(String name)
	{
		return BY_PIECE.get(ItemNames.normalize(name));
	}

	/** True when a wiki footnote marks the recommendation as full-set-only. */
	public static boolean setConditionalNote(String note)
	{
		if (note == null)
		{
			return false;
		}
		String n = note.toLowerCase(Locale.ROOT);
		return n.contains("set") && (n.contains("full") || n.contains("complete"));
	}
}
