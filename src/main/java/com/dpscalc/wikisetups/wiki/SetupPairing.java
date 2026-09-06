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

/**
 * Works out which {{Inventory}} belongs to which {{Recommended equipment}} from their order in the
 * page.
 *
 * <p>Document position is a far stronger signal than the labels are: pages routinely head every
 * inventory "==Inventory==", so half of them carry no distinguishing label at all, while their
 * order is unambiguous. Two shapes cover the pages that do this —
 *
 * <pre>
 *   interleaved   E I E I E I    each setup is followed by its own inventory
 *   blocked       E E E I I I    all the setups, then all the inventories
 * </pre>
 *
 * — and both mean setup <i>i</i> owns inventory <i>i</i>. Anything else (ragged orders like E E I E
 * I) is genuinely ambiguous and gets no pairing, leaving label matching to do its best.
 *
 * <p>The template counts found here must match the number of setups the parsers actually kept,
 * otherwise the indices refer to different things — a template that parsed to nothing is dropped
 * from the model but still appears in the page. A mismatch disables pairing rather than guessing.
 */
public final class SetupPairing {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SetupPairing.class);

    /** No inventory paired to any setup. */
    public static final int[] NONE = new int[0];

    private SetupPairing() {}

    /** Match unique named methods when unrelated or optional templates break page-wide order. */
    public static int[] pairUniqueLabels(java.util.List<GearSetup> gear, java.util.List<InventorySetup> inventories) {
        int[] result = new int[gear.size()];
        Arrays.fill(result, -1);
        for (int g = 0; g < gear.size(); g++) {
            int candidate = -1;
            for (int i = 0; i < inventories.size(); i++) {
                String label = normalizedLabel(inventories.get(i).getLabel());
                if (label.isEmpty()) continue;
                int panes = 0;
                for (GearSetup other : gear) if (label.equals(normalizedLabel(other.getPaneLabel()))) panes++;
                boolean paneFirst = panes > 0;
                if (!label.equals(normalizedLabel(paneFirst ? gear.get(g).getPaneLabel() : gear.get(g).getStyleLabel()))) continue;
                int owners = 0;
                for (GearSetup other : gear) if (label.equals(normalizedLabel(paneFirst ? other.getPaneLabel() : other.getStyleLabel()))) owners++;
                if (owners != 1 || candidate >= 0) { candidate = -2; break; }
                candidate = i;
            }
            if (candidate >= 0) result[g] = candidate;
        }
        return result;
    }

    private static String normalizedLabel(String value) {
        String label = value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
        if (java.util.List.of("inventory", "inventories", "equipment", "gear", "setup", "setups", "notes").contains(label)) return "";
        return label;
    }

    /**
     * Index of the inventory belonging to each setup, or -1 where unknown. Empty when the page's
     * order carries no usable pairing.
     */
    public static int[] pair(String rawWikitext, int setupCount, int inventoryCount) {
        if (rawWikitext == null || setupCount == 0 || inventoryCount == 0) {
            return NONE;
        }

        String order = templateOrder(GearSetupParser.normalizeLineEndings(rawWikitext));
        int setups = count(order, 'E');
        int inventories = count(order, 'I');
        if (setups != setupCount || inventories != inventoryCount) {
            // a template we skipped (empty, malformed) would shift every index
            log.debug(
                    "pairing skipped: page has {}E/{}I, parsers kept {}/{}",
                    setups,
                    inventories,
                    setupCount,
                    inventoryCount);
            return NONE;
        }

        // Interleaved order pairs by adjacency, so a spare template at the end
        // is harmless. Blocked order pairs by index alone, and with unequal
        // counts there is no way to tell which setup the spare belongs to.
        boolean pairable =
                isInterleaved(order) || (isBlocked(order) && setupCount == inventoryCount);
        if (!pairable) {
            return NONE;
        }

        int[] pairing = new int[setupCount];
        Arrays.fill(pairing, -1);
        for (int i = 0; i < setupCount && i < inventoryCount; i++) {
            pairing[i] = i;
        }
        return pairing;
    }

    /** Pair only full loadouts when an unrelated single-item switch broke whole-page order. */
    public static int[] pairLoadouts(
            String rawWikitext,
            int setupCount,
            int inventoryCount,
            java.util.List<Integer> retained) {
        if (rawWikitext == null || retained.isEmpty() || inventoryCount == 0) return NONE;
        String order = templateOrder(GearSetupParser.normalizeLineEndings(rawWikitext));
        if (count(order, 'E') != setupCount || count(order, 'I') != inventoryCount) return NONE;
        java.util.Set<Integer> indices = new java.util.HashSet<>(retained);
        if (indices.size() != retained.size()) return NONE;
        int previous = -1;
        for (int index : retained) {
            if (index <= previous || index >= setupCount) return NONE;
            previous = index;
        }
        StringBuilder filtered = new StringBuilder();
        int original = 0;
        for (int i = 0; i < order.length(); i++) {
            char kind = order.charAt(i);
            if (kind == 'I' || indices.contains(original)) filtered.append(kind);
            if (kind == 'E') original++;
        }
        String loadouts = filtered.toString();
        if (!isInterleaved(loadouts) && !(isBlocked(loadouts) && retained.size() == inventoryCount))
            return NONE;
        int[] pairing = new int[retained.size()];
        Arrays.fill(pairing, -1);
        for (int i = 0; i < pairing.length && i < inventoryCount; i++) pairing[i] = i;
        return pairing;
    }

    /** The page's equipment and inventory templates as a string like "EIEIEI". */
    static String templateOrder(String wikitext) {
        StringBuilder order = new StringBuilder();
        int i = 0;
        while ((i = wikitext.indexOf("{{", i)) >= 0) {
            int end = GearSetupParser.findTemplateEnd(wikitext, i);
            if (end < 0) {
                i += 2;
                continue;
            }
            String name = GearSetupParser.templateName(wikitext.substring(i + 2, end - 2));
            if (name.equalsIgnoreCase("recommended equipment")) {
                order.append('E');
                i = end;
            } else if (name.equalsIgnoreCase("inventory")) {
                order.append('I');
                i = end;
            } else {
                // step inside: raid tables nest {{Equipment}} and {{Inventory}}
                // within wikitable rows, and those still count
                i += 2;
            }
        }
        return order.toString();
    }

    /** "EIEIEI" — every setup immediately followed by its inventory. */
    static boolean isInterleaved(String order) {
        if (order.length() % 2 != 0) {
            return false;
        }
        for (int i = 0; i < order.length(); i += 2) {
            if (order.charAt(i) != 'E' || order.charAt(i + 1) != 'I') {
                return false;
            }
        }
        return order.length() > 0;
    }

    /** "EEEIII" — every setup before every inventory. */
    static boolean isBlocked(String order) {
        int firstInventory = order.indexOf('I');
        return firstInventory > 0
                && order.indexOf('E', firstInventory) < 0
                && order.lastIndexOf('I') == order.length() - 1;
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }
}
