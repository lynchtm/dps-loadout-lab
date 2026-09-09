// SPDX-License-Identifier: BSD-2-Clause
package com.dpscalc.scenario;

import com.loadoutlab.model.*;

import java.util.*;

/** Explicit limits travel with results and exclude affected optimizer candidates by default. */
public final class CoverageWarnings {
    private CoverageWarnings() {}

    public static List<String> forState(PlayerState p, MonsterStats m) {
        List<String> warnings = new ArrayList<>();
        String w = Objects.toString(p.getWeaponName(), "").toLowerCase(Locale.ROOT),
                name = Objects.toString(m.getName(), "").toLowerCase(Locale.ROOT);
        if (m.hasAttribute(MonsterAttribute.XERICIAN))
            warnings.add(
                    "Chambers of Xeric: party/Challenge Mode scaling and encounter-specific damage"
                        + " rules are not fully modelled. Enter observed target stats; use results"
                        + " as estimates.");
        if (m.isToaMonster())
            warnings.add(
                    "Tombs of Amascut: base invocation scaling is applied; phase-specific damage,"
                            + " core damage and raid mechanics need verification.");
        if (name.contains("araxyte") || name.contains("maggot") || name.contains("mad angel"))
            warnings.add(
                    "This target has special guaranteed-hit or damage rules that are not yet"
                            + " modelled.");
        if (name.contains("verzik")
                || name.contains("vardorvis")
                || name.contains("vorkath")
                || name.contains("kalphite queen")
                || name.contains("nightmare")
                || name.contains("tormented demon"))
            warnings.add(
                    "Encounter-specific phases, protection and changing defence are not simulated."
                            + " Results use the selected static target stats.");
        if (w.contains("dual macuahuitl")
                || w.contains("torag")
                || w.contains("sulphur blades")
                || w.contains("tonalztics")
                || w.contains("eclipse atlatl")
                || w.contains("dawnbringer")
                || w.contains("salamander")
                || w.contains("dinh"))
            warnings.add(
                    "This weapon's special normal-attack mechanics are not yet independently"
                            + " verified; displayed DPS is incomplete.");
        if (p.isWearingItemContaining("Blue moon")
                || p.isWearingItemContaining("Blood moon")
                || p.isWearingItemContaining("Eclipse moon"))
            warnings.add("Moon armour: set proc timing and delayed damage are not modelled.");
        if (w.contains("ahrim") || w.contains("karil"))
            warnings.add(
                    "Barrows set proc distributions are estimates pending independent multi-hit"
                            + " validation.");
        return warnings;
    }
}
