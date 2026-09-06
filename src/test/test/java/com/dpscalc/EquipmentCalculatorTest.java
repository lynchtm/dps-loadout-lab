package com.dpscalc;

import com.dpscalc.equipment.EquipmentCalculator;
import com.dpscalc.equipment.EquipmentCombatStyle;
import com.dpscalc.equipment.EquipmentContext;
import com.dpscalc.equipment.EquipmentDomainCatalog;
import com.dpscalc.equipment.EquipmentDomainException;
import com.dpscalc.equipment.EquipmentItem;
import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.EquipmentResult;
import com.dpscalc.equipment.EquipmentSlot;
import com.dpscalc.equipment.EquipmentStatTotals;
import com.dpscalc.equipment.ItemVariable;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static com.dpscalc.EquipmentTestFixtures.facts;
import static com.dpscalc.EquipmentTestFixtures.item;
import static com.dpscalc.EquipmentTestFixtures.loadout;
import static com.dpscalc.EquipmentTestFixtures.stat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class EquipmentCalculatorTest {
    private final EquipmentDomainCatalog catalog = loadCatalog();
    private final EquipmentCalculator calculator = new EquipmentCalculator(catalog);

    public EquipmentCalculatorTest() throws IOException {}

    @Test
    public void sumsRawVisibleStats() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.WEAPON, item("Abyssal whip"),
            EquipmentSlot.FEET, item("Dragon boots")
        );

        EquipmentStatTotals totals = calculate(gear, context(415, style("slash", "Accurate"), "")).getStats();

        assertEquals(82, totals.getSlashAttack());
        assertEquals(86, totals.getMeleeStrength());
    }

    @Test
    public void suppressesOnlyUnusedAmmoRangedStats() {
        JsonObject ammoFacts = EquipmentTestFixtures.ammoWithRetainedStats();
        EquipmentItem ammo = EquipmentItem.raw(ammoFacts.get("id").getAsInt(), Map.of());
        EquipmentLoadout gear = loadout(EquipmentSlot.WEAPON, item("Bow of faerdhinen"), EquipmentSlot.AMMO, ammo);

        EquipmentStatTotals totals = calculate(gear, context(415, style("ranged", "Accurate"), "")).getStats();

        JsonObject weapon = facts("Bow of faerdhinen");
        assertEquals(stat(weapon, "offensive", "ranged"), totals.getRangedAttack());
        assertEquals(stat(weapon, "bonuses", "ranged_str"), totals.getRangedStrength());
        assertEquals(stat(ammoFacts, "bonuses", "prayer"), totals.getPrayerBonus());
        assertEquals(stat(ammoFacts, "defensive", "stab"), totals.getStabDefence());
        assertEquals(stat(ammoFacts, "defensive", "slash"), totals.getSlashDefence());
        assertEquals(stat(ammoFacts, "defensive", "crush"), totals.getCrushDefence());
        assertEquals(stat(ammoFacts, "defensive", "magic"), totals.getMagicDefence());
        assertEquals(stat(ammoFacts, "defensive", "ranged"), totals.getRangedDefence());
    }

    @Test
    public void addsBlowpipeDartStrengthFromItemVariables() {
        EquipmentItem dart = item("Dragon dart");
        EquipmentLoadout emptyPipe = loadout(EquipmentSlot.WEAPON, item("Toxic blowpipe"));
        EquipmentLoadout loadedPipe = loadout(EquipmentSlot.WEAPON, item("Toxic blowpipe", Map.of(
            "blowpipeDartId", ItemVariable.ofNumber(dart.getOriginalId())
        )));

        int emptyStrength = calculate(emptyPipe, context(415, style("ranged", "Rapid"), "")).getStats().getRangedStrength();
        int loadedStrength = calculate(loadedPipe, context(415, style("ranged", "Rapid"), "")).getStats().getRangedStrength();

        assertEquals(stat(facts("Dragon dart"), "bonuses", "ranged_str"), loadedStrength - emptyStrength);
    }

    @Test
    public void rejectsMalformedBlowpipeDartVariable() {
        EquipmentLoadout gear = loadout(EquipmentSlot.WEAPON, item("Toxic blowpipe", Map.of(
            "blowpipeDartId", ItemVariable.ofString("dragon")
        )));

        EquipmentDomainException error = assertThrows(EquipmentDomainException.class,
            () -> calculate(gear, context(415, style("ranged", "Rapid"), "")));

        assertTrue(error.getMessage().contains("itemVars.blowpipeDartId"));
    }

    @Test
    public void appliesShadowFactorsCapAndManualCastGate() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.WEAPON, item("Tumeken's shadow"),
            EquipmentSlot.HEAD, item("Ancestral hat"),
            EquipmentSlot.BODY, item("Ancestral robe top"),
            EquipmentSlot.LEGS, item("Ancestral robe bottom"),
            EquipmentSlot.NECK, item("Occult necklace"),
            EquipmentSlot.CAPE, item(21780),
            EquipmentSlot.HANDS, item("Tormented bracelet"),
            EquipmentSlot.RING, item(28313),
            EquipmentSlot.SHIELD, item(25985)
        );
        EquipmentStatTotals manual = calculate(gear, context(415, style("magic", "Manual Cast"), "standard")).getStats();

        EquipmentStatTotals outside = calculate(gear, context(415, style("magic", "Autocast"), "standard")).getStats();
        EquipmentStatTotals inside = calculate(gear, context(11789, style("magic", "Autocast"), "standard")).getStats();

        assertEquals(manual.getMagicAttack() * 3, outside.getMagicAttack());
        assertEquals(Math.min(1000, manual.getMagicDamage() * 3), outside.getMagicDamage());
        assertEquals(manual.getMagicAttack() * 4, inside.getMagicAttack());
        assertEquals(Math.min(1000, manual.getMagicDamage() * 4), inside.getMagicDamage());
        assertEquals(1000, inside.getMagicDamage());
    }

    @Test
    public void appliesKerisPenaltyOnlyOutsideTombs() {
        EquipmentLoadout gear = loadout(EquipmentSlot.WEAPON, item("Keris partisan of amascut"));
        EquipmentStatTotals inside = calculate(gear, context(11789, style("stab", "Accurate"), "")).getStats();

        EquipmentStatTotals outside = calculate(gear, context(415, style("stab", "Accurate"), "")).getStats();

        assertEquals(inside.getMeleeStrength() - 22, outside.getMeleeStrength());
        assertEquals(inside.getStabAttack() - 50, outside.getStabAttack());
    }

    @Test
    public void derivesDinhStrengthFromAggregateDefences() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.WEAPON, item("Dinh's bulwark"),
            EquipmentSlot.HEAD, item("Justiciar faceguard"),
            EquipmentSlot.BODY, item("Justiciar chestguard"),
            EquipmentSlot.LEGS, item("Justiciar legguards")
        );
        JsonObject weapon = facts("Dinh's bulwark");

        EquipmentStatTotals totals = calculate(gear, context(415, style("crush", "Accurate"), "")).getStats();

        int defenceSum = totals.getStabDefence() + totals.getSlashDefence() + totals.getCrushDefence() + totals.getRangedDefence();
        int expectedBonus = Math.max(0, (defenceSum - 800) / 12 - 38);
        assertEquals(stat(weapon, "bonuses", "str") + expectedBonus, totals.getMeleeStrength());
    }

    @Test
    public void derivesBlazingDinhStrengthFromAggregateDefences() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.WEAPON, item("Dinh's blazing bulwark"),
            EquipmentSlot.HEAD, item("Justiciar faceguard"),
            EquipmentSlot.BODY, item("Justiciar chestguard"),
            EquipmentSlot.LEGS, item("Justiciar legguards")
        );
        JsonObject weapon = facts("Dinh's blazing bulwark");

        EquipmentStatTotals totals = calculate(gear, context(415, style("crush", "Accurate"), "")).getStats();

        int defenceSum = totals.getStabDefence() + totals.getSlashDefence() + totals.getCrushDefence() + totals.getRangedDefence();
        assertEquals(stat(weapon, "bonuses", "str") + Math.max(0, (defenceSum - 800) / 12 - 38), totals.getMeleeStrength());
    }

    @Test
    public void addsVirtusAncientCastBonusPerPiece() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.HEAD, item("Virtus mask"),
            EquipmentSlot.BODY, item("Virtus robe top"),
            EquipmentSlot.LEGS, item("Virtus robe bottom")
        );
        EquipmentStatTotals standard = calculate(gear, context(415, style("magic", "Autocast"), "standard")).getStats();

        EquipmentStatTotals ancient = calculate(gear, context(415, style("magic", "Autocast"), "ancient")).getStats();

        assertEquals(90, ancient.getMagicDamage() - standard.getMagicDamage());
    }

    @Test
    public void addsExactEliteVoidMageVisibleBonus() {
        EquipmentLoadout incomplete = loadout(
            EquipmentSlot.HEAD, item("Void mage helm"),
            EquipmentSlot.BODY, item("Elite void top"),
            EquipmentSlot.LEGS, item("Elite void robe")
        );
        EquipmentLoadout complete = loadout(
            EquipmentSlot.HEAD, item("Void mage helm"),
            EquipmentSlot.BODY, item("Elite void top"),
            EquipmentSlot.LEGS, item("Elite void robe"),
            EquipmentSlot.HANDS, item("Void knight gloves")
        );
        EquipmentStatTotals before = calculate(incomplete, context(415, style("magic", "Accurate"), "")).getStats();

        EquipmentStatTotals after = calculate(complete, context(415, style("magic", "Accurate"), "")).getStats();

        assertEquals(50, after.getMagicDamage() - before.getMagicDamage());
    }

    private EquipmentResult calculate(EquipmentLoadout gear, EquipmentContext context) {
        return calculator.calculate(gear, context);
    }

    static EquipmentContext context(int monsterId, EquipmentCombatStyle style, String spellbook) {
        return EquipmentContext.of(monsterId, style, spellbook);
    }

    static EquipmentCombatStyle style(String type, String stance) {
        return EquipmentCombatStyle.of(type, stance);
    }

    private static EquipmentDomainCatalog loadCatalog() throws IOException {
        return EquipmentDomainCatalogTest.loadCatalog();
    }
}
