package com.dpscalc.scenario;

import static org.junit.Assert.*;

import com.dpscalc.wikisetups.wiki.*;

import org.junit.Test;

import java.util.*;

public class BossImportRegressionTest {
    private static String gear(String style) {
        return "{{Recommended equipment|style="
                + style
                + "|head1={{plink|Rune full helm}}|body1={{plink|Rune"
                + " platebody}}|weapon1={{plink|Abyssal whip}}}}";
    }

    @Test
    public void inventoryBeforeGearAndMissingMethodsDoNotBreakOtherNamedMatches() {
        String text =
                "<tabber>Entry level=\n{{Inventory|Shark}}\n"
                        + gear("Entry level")
                        + "\n|-|Magic=\n"
                        + gear("Magic")
                        + "\n|-|Ranged=\n"
                        + gear("Ranged")
                        + "\n{{Inventory|Monkfish}}\n</tabber>";
        List<WikiLoadouts.Choice> choices = WikiLoadouts.parse(text);
        assertEquals("Shark", choices.get(0).inventory.getSlots().get(0));
        assertNull(choices.get(1).inventory);
        assertEquals("Monkfish", choices.get(2).inventory.getSlots().get(0));
    }

    @Test
    public void repeatedLabelsRemainAmbiguous() {
        List<GearSetup> gear = new GearSetupParser().parse(gear("Melee") + gear("Melee"));
        List<InventorySetup> inventories =
                new InventoryParser().parse("<tabber>Melee=\n{{Inventory|Shark}}");
        assertArrayEquals(new int[] {-1, -1}, SetupPairing.pairUniqueLabels(gear, inventories));
        assertArrayEquals(
                new int[] {-1},
                SetupPairing.pairUniqueLabels(
                        gear.subList(0, 1), List.of(inventories.get(0), inventories.get(0))));
    }

    @Test
    public void paneNamesTakePriorityOverRepeatedCombatStyles() {
        String text =
                "<tabber>Melee=\n"
                        + gear("Melee")
                        + "{{Inventory|Shark}}\n|-|Melee (POH)=\n"
                        + gear("Melee")
                        + "{{Inventory|Monkfish}}";
        assertArrayEquals(
                new int[] {0, 1},
                SetupPairing.pairUniqueLabels(
                        new GearSetupParser().parse(text), new InventoryParser().parse(text)));
    }

    @Test
    public void deathCostNotesKeepInventoryVariantsWithTheirExample() {
        String text =
                "==Example==\n"
                    + "{{Equipment|weapon=Ursine chainmace}}\n"
                    + "====Items protected:====\n"
                    + "* Weapon\n"
                    + "====Items lost:====\n"
                    + "* Food\n"
                    + "==Inventory==\n"
                    + "{{Inventory|Shark}}\n"
                    + "{{Inventory|Monkfish}}";
        List<ExampleSetup> examples = new ExampleSetupParser().parse(text);
        assertEquals(2, examples.size());
        assertEquals("Shark", examples.get(0).getInventory().getSlots().get(0));
        assertEquals("Monkfish", examples.get(1).getInventory().getSlots().get(0));
    }

    @Test
    public void standaloneExamplePreservesInventoryAndPouchWithoutBorrowingNextTab() {
        String text =
                "<tabber>Budget=\n"
                    + "{{Equipment|weapon=Abyssal whip|head=Rune full helm}}\n"
                    + "{{Inventory|Shark|Divine rune pouch}}{{Rune pouch|Blood rune\\500|Death"
                    + " rune\\200}}\n"
                    + "|-|Expensive=\n"
                    + "{{Equipment|weapon=Scythe of vitur}}\n"
                    + "|-|Other=\n"
                    + "{{Inventory|Monkfish}}\n"
                    + "</tabber>";
        List<ExampleSetup> examples = new ExampleSetupParser().parse(text);
        assertEquals(2, examples.size());
        assertTrue(examples.get(0).getLabel().contains("Budget"));
        assertEquals("Shark", examples.get(0).getInventory().getSlots().get(0));
        assertEquals(500, examples.get(0).getInventory().quantity(0, true));
        assertNull(examples.get(1).getInventory());
    }

    @Test
    public void headerlessSingleExampleTableIsImportedOnce() {
        String text =
                "<tabber>Twisted bow=\n"
                    + "{|\n"
                    + "|{{Equipment|weapon=Twisted bow}}\n"
                    + "|{{Inventory|Shark|Rune pouch}}\n"
                    + "|-\n"
                    + "|colspan=2|{{Rune pouch|Blood rune}}\n"
                    + "|}\n"
                    + "</tabber>";
        List<ExampleSetup> examples = new ExampleSetupParser().parse(text);
        assertEquals(1, examples.size());
        assertEquals(
                "Twisted bow",
                examples.get(0).getGear().getSlots().get(EquipSlot.WEAPON).get(0).getName());
        assertEquals("Shark", examples.get(0).getInventory().getSlots().get(0));
        assertEquals(List.of("Blood rune"), examples.get(0).getInventory().getRunes());
    }

    @Test
    public void decorativeGridDoesNotDuplicateRankedGearAndCannotBorrowNextSection() {
        assertTrue(
                new ExampleSetupParser()
                        .parse(
                                "{{Equipment|weapon=Abyssal whip}}"
                                        + gear("Melee")
                                        + "{{Inventory|Shark}}")
                        .isEmpty());
        List<ExampleSetup> examples =
                new ExampleSetupParser()
                        .parse(
                                "{{Equipment|weapon=Abyssal whip}}\n"
                                    + "===Different method===\n"
                                    + "{{Inventory|Shark}}");
        assertEquals(1, examples.size());
        assertNull(examples.get(0).getInventory());
    }

    @Test
    public void exampleInventoriesCannotBeZippedToUnrelatedRankedMethods() {
        String text =
                "<tabber>Melee=\n"
                        + gear("Melee")
                        + "\n|-|Ranged=\n"
                        + gear("Ranged")
                        + "\n"
                        + "</tabber>\n"
                        + "<tabber>Budget=\n"
                        + "{{Equipment|weapon=Abyssal whip}}{{Inventory|Shark}}\n"
                        + "|-|Expensive=\n"
                        + "{{Equipment|weapon=Twisted bow}}{{Inventory|Monkfish}}</tabber>";
        List<WikiLoadouts.Choice> choices = WikiLoadouts.parse(text);
        assertEquals(4, choices.size());
        assertNull(choices.get(0).inventory);
        assertNull(choices.get(1).inventory);
        assertEquals("Shark", choices.get(2).inventory.getSlots().get(0));
        assertEquals("Monkfish", choices.get(3).inventory.getSlots().get(0));
    }
}
