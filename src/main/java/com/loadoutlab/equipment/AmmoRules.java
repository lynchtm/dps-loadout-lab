/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.equipment;

import java.util.Locale;

/** Compatibility of ordinary ammunition, using the documented weapon families. */
final class AmmoRules {
    private AmmoRules() {}

    static AmmoApplicability classify(EquipmentCatalogItem weapon, EquipmentCatalogItem ammo) {
        if (weapon == null) return AmmoApplicability.ALLOWED;
        String w = weapon.getName().toLowerCase(Locale.ROOT),
                a = ammo == null ? "" : ammo.getName().toLowerCase(Locale.ROOT);
        String category = weapon.getCategory().toLowerCase(Locale.ROOT);
        if (w.contains("crystal bow")
                || w.contains("bow of faerdhinen")
                || w.contains("craw's bow")
                || w.contains("webweaver bow")
                || w.contains("blowpipe")) return AmmoApplicability.ALLOWED;
        if (category.equals("crossbow")) {
            if (w.contains("dorgeshuun"))
                return a.equals("bone bolts")
                        ? AmmoApplicability.INCLUDED
                        : AmmoApplicability.INVALID;
            if (w.startsWith("hunters") || w.startsWith("hunter's"))
                return a.contains("kebbit bolts")
                        ? AmmoApplicability.INCLUDED
                        : AmmoApplicability.INVALID;
            if (!a.contains("bolt") || a.contains("kebbit") || a.equals("bone bolts"))
                return AmmoApplicability.INVALID;
            if (a.contains("dragon bolts")
                    || a.contains("dragonstone dragon")
                    || a.matches(".* dragon bolts.*")) {
                return w.contains("dragon crossbow")
                                || w.contains("dragon hunter crossbow")
                                || w.contains("armadyl")
                                || w.contains("zaryte")
                        ? AmmoApplicability.INCLUDED
                        : AmmoApplicability.INVALID;
            }
            int cap =
                    w.startsWith("bronze")
                            ? 1
                            : w.startsWith("blurite")
                                    ? 2
                                    : w.startsWith("iron")
                                            ? 3
                                            : w.startsWith("steel")
                                                    ? 4
                                                    : w.startsWith("mithril")
                                                            ? 5
                                                            : w.startsWith("adamant") ? 6 : 7;
            int tier =
                    a.contains("runite") || a.contains("onyx") || a.contains("dragonstone")
                            ? 7
                            : a.contains("adamant") || a.contains("ruby") || a.contains("diamond")
                                    ? 6
                                    : a.contains("mithril") || a.contains("emerald")
                                            ? 5
                                            : a.contains("steel") || a.contains("topaz")
                                                    ? 4
                                                    : a.contains("iron") || a.contains("pearl")
                                                            ? 3
                                                            : a.contains("blurite")
                                                                            || a.contains("jade")
                                                                    ? 2
                                                                    : 1;
            return tier <= cap ? AmmoApplicability.INCLUDED : AmmoApplicability.INVALID;
        }
        if (category.equals("bow")) {
            if (!a.contains("arrow")) return AmmoApplicability.INVALID;
            int tier =
                    a.contains("dragon") || a.contains("seeker")
                            ? 6
                            : a.contains("amethyst")
                                    ? 5
                                    : a.contains("rune")
                                            ? 4
                                            : a.contains("adamant")
                                                    ? 3
                                                    : a.contains("mithril")
                                                            ? 2
                                                            : a.contains("steel") ? 1 : 0;
            int cap =
                    w.contains("twisted")
                                    || w.contains("dark bow")
                                    || w.contains("3rd age")
                                    || w.contains("venator")
                                    || w.contains("scorching")
                            ? 6
                            : w.contains("magic")
                                    ? 5
                                    : w.contains("yew") || w.contains("seercull")
                                            ? 4
                                            : w.contains("maple")
                                                    ? 3
                                                    : w.contains("willow")
                                                            ? 2
                                                            : w.contains("oak") ? 1 : 0;
            return tier <= cap ? AmmoApplicability.INCLUDED : AmmoApplicability.INVALID;
        }
        return AmmoApplicability.ALLOWED;
    }
}
