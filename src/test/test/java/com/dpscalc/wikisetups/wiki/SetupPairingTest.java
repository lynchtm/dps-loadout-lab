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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Pages routinely head every inventory "==Inventory==", so their labels can't tell them apart — but
 * their order can. These pin when that order is trusted and, just as importantly, when it isn't.
 */
public class SetupPairingTest {
    private static String fixture(String name) throws IOException {
        try (InputStream in =
                SetupPairingTest.class.getResourceAsStream("/fixtures/" + name + ".wikitext")) {
            assertNotNull("missing fixture " + name, in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void araxxorIgnoresAraxyteWeaponAnnotationWhenPairingInventories() throws IOException {
        String text = fixture("araxxor-strategies");
        List<GearSetup> gear = new GearSetupParser().parse(text);
        List<InventorySetup> inventories = new InventoryParser().parse(text);
        assertEquals(3, gear.size());
        assertEquals(2, inventories.size());
        assertEquals(0, SetupPairing.pair(text, gear.size(), inventories.size()).length);
        GearSetups.Partition partition = GearSetups.partition(gear);
        assertEquals(2, partition.getLoadouts().size());
        assertEquals(1, partition.getSwitches().size());
        assertArrayEquals(
                new int[] {0, 1},
                SetupPairing.pairLoadouts(
                        text, gear.size(), inventories.size(), partition.getLoadoutIndices()));
        java.util.List<com.dpscalc.scenario.WikiLoadouts.Choice> choices =
                com.dpscalc.scenario.WikiLoadouts.parse(text);
        assertEquals("Elder maul", choices.get(0).inventory.getSlots().get(0));
        assertEquals("Divine rune pouch", choices.get(0).inventory.getSlots().get(27));
        assertEquals("Blood rune", choices.get(0).inventory.getRunes().get(0));
        assertEquals("Heavy ballista", choices.get(1).inventory.getSlots().get(1));
    }

    @Test
    public void filteringStillRejectsOrphanInventoriesAndParserCountMismatch() {
        String text =
                "{{Recommended equipment|weapon1=x}}{{Inventory|1=Shark}}{{Recommended"
                    + " equipment|head1=x}}{{Inventory|1=Tuna}}";
        assertEquals(0, SetupPairing.pairLoadouts(text, 2, 2, java.util.List.of(1)).length);
        assertEquals(0, SetupPairing.pairLoadouts(text, 3, 2, java.util.List.of(1, 2)).length);
    }

    @Test
    public void interleavedPagesPairEachSetupWithTheInventoryBelowIt() throws IOException {
        String wikitext = fixture("general-graardor-strategies");
        List<GearSetup> setups = new GearSetupParser().parse(wikitext);
        List<InventorySetup> inventories = new InventoryParser().parse(wikitext);
        assertEquals(8, setups.size());
        assertEquals(8, inventories.size());

        assertArrayEquals(
                new int[] {0, 1, 2, 3, 4, 5, 6, 7},
                SetupPairing.pair(wikitext, setups.size(), inventories.size()));
    }

    @Test
    public void interleavedOrderIsRecognised() {
        assertTrue(SetupPairing.isInterleaved("EIEIEI"));
        assertTrue(SetupPairing.isInterleaved("EI"));
    }

    @Test
    public void blockedOrderIsRecognised() {
        assertTrue(SetupPairing.isBlocked("EEEIII"));
        assertTrue(SetupPairing.isBlocked("EII"));
    }

    @Test
    public void raggedOrderIsNeither() {
        // Araxxor: E E I E I — no way to say which setup the first owns
        assertEquals(
                0,
                SetupPairing.pair(
                                "{{Recommended equipment|head1=a}}"
                                        + "{{Recommended equipment|head1=b}}{{Inventory|1=Shark}}"
                                        + "{{Recommended equipment|head1=c}}{{Inventory|1=Shark}}",
                                3,
                                2)
                        .length);
    }

    /** An unequal blocked page can't say which setup the spare belongs to. */
    @Test
    public void blockedOrderWithUnequalCountsIsNotPaired() {
        String wikitext =
                "{{Recommended equipment|head1=a}}{{Recommended equipment|head1=b}}"
                        + "{{Inventory|1=Shark}}";
        assertEquals(0, SetupPairing.pair(wikitext, 2, 1).length);
    }

    @Test
    public void blockedOrderWithEqualCountsPairsByIndex() {
        String wikitext =
                "{{Recommended equipment|head1=a}}{{Recommended equipment|head1=b}}"
                        + "{{Inventory|1=Shark}}{{Inventory|1=Tuna}}";
        assertArrayEquals(new int[] {0, 1}, SetupPairing.pair(wikitext, 2, 2));
    }

    /**
     * A template the parsers dropped (empty, malformed) shifts every index after it, so a count
     * that doesn't match the models disables pairing rather than mispairing.
     */
    @Test
    public void countMismatchWithTheParsedModelsDisablesPairing() {
        String wikitext = "{{Recommended equipment|head1=a}}{{Inventory|1=Shark}}";
        assertEquals(0, SetupPairing.pair(wikitext, 2, 1).length);
    }

    @Test
    public void emptyInputsPairNothing() {
        assertEquals(0, SetupPairing.pair(null, 1, 1).length);
        assertEquals(0, SetupPairing.pair("{{Inventory|1=Shark}}", 0, 1).length);
    }

    @Test
    public void crlfInputPairsIdentically() throws IOException {
        String lf = fixture("general-graardor-strategies");
        assertArrayEquals(
                SetupPairing.pair(lf, 8, 8), SetupPairing.pair(lf.replace("\n", "\r\n"), 8, 8));
    }
}
