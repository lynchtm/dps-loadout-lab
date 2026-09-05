package com.dpscalc;

import com.dpscalc.equipment.EquipmentCalculator;
import com.dpscalc.equipment.EquipmentDomainCatalog;
import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.EquipmentSlot;
import com.dpscalc.equipment.EquipmentStatTotals;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.io.IOException;

import static com.dpscalc.EquipmentCalculatorTest.context;
import static com.dpscalc.EquipmentCalculatorTest.style;
import static com.dpscalc.EquipmentTestFixtures.factsById;
import static com.dpscalc.EquipmentTestFixtures.item;
import static com.dpscalc.EquipmentTestFixtures.loadout;
import static com.dpscalc.EquipmentTestFixtures.stat;
import static org.junit.Assert.assertEquals;

public class EquipmentDizanaTest {
    private final EquipmentCalculator calculator = new EquipmentCalculator(loadCatalog());

    public EquipmentDizanaTest() throws IOException {}

    @Test
    public void chargedQuiverAddsBonusForIncludedDragonArrow() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.CAPE, item("Dizana's quiver", "Charged"),
            EquipmentSlot.WEAPON, item("Twisted bow"),
            EquipmentSlot.AMMO, item("Dragon arrow")
        );

        EquipmentStatTotals totals = calculate(gear);

        assertEquals(visibleRangedAttack(gear) + 10, totals.getRangedAttack());
        assertEquals(visibleRangedStrength(gear) + 1, totals.getRangedStrength());
    }

    @Test
    public void blessedQuiverActsChargedForIncludedAmmo() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.CAPE, item("Blessed dizana's quiver"),
            EquipmentSlot.WEAPON, item("Twisted bow"),
            EquipmentSlot.AMMO, item("Dragon arrow")
        );

        EquipmentStatTotals totals = calculate(gear);

        assertEquals(visibleRangedAttack(gear) + 10, totals.getRangedAttack());
        assertEquals(visibleRangedStrength(gear) + 1, totals.getRangedStrength());
    }

    @Test
    public void dizanaMaxCapeActsChargedForIncludedAmmo() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.CAPE, item("Dizana's max cape"),
            EquipmentSlot.WEAPON, item("Twisted bow"),
            EquipmentSlot.AMMO, item("Dragon arrow")
        );

        EquipmentStatTotals totals = calculate(gear);

        assertEquals(visibleRangedAttack(gear) + 10, totals.getRangedAttack());
        assertEquals(visibleRangedStrength(gear) + 1, totals.getRangedStrength());
    }

    @Test
    public void unchargedQuiverDoesNotAddBonus() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.CAPE, item("Dizana's quiver", "Uncharged"),
            EquipmentSlot.WEAPON, item("Twisted bow"),
            EquipmentSlot.AMMO, item("Dragon arrow")
        );

        EquipmentStatTotals totals = calculate(gear);

        assertEquals(visibleRangedAttack(gear), totals.getRangedAttack());
        assertEquals(visibleRangedStrength(gear), totals.getRangedStrength());
    }

    @Test
    public void chargedQuiverDoesNotAddBonusForAllowedAmmo() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.CAPE, item("Dizana's quiver", "Charged"),
            EquipmentSlot.WEAPON, item("Bow of faerdhinen"),
            EquipmentSlot.AMMO, item("Dragon arrow")
        );

        EquipmentStatTotals totals = calculate(gear);

        assertEquals(visibleRangedAttackWithoutAmmo(gear), totals.getRangedAttack());
        assertEquals(visibleRangedStrengthWithoutAmmo(gear), totals.getRangedStrength());
    }

    @Test
    public void chargedQuiverDoesNotAddBonusForInvalidDart() {
        EquipmentLoadout gear = loadout(
            EquipmentSlot.CAPE, item("Dizana's quiver", "Charged"),
            EquipmentSlot.WEAPON, item("Twisted bow"),
            EquipmentSlot.AMMO, item("Dragon dart")
        );

        EquipmentStatTotals totals = calculate(gear);

        assertEquals(visibleRangedAttackWithoutAmmo(gear), totals.getRangedAttack());
        assertEquals(visibleRangedStrengthWithoutAmmo(gear), totals.getRangedStrength());
    }

    private EquipmentStatTotals calculate(EquipmentLoadout gear) {
        return calculator.calculate(gear, context(415, style("ranged", "Accurate"), "")).getStats();
    }

    private static int visibleRangedAttack(EquipmentLoadout gear) {
        return visible(gear, "offensive", "ranged", true);
    }

    private static int visibleRangedStrength(EquipmentLoadout gear) {
        return visible(gear, "bonuses", "ranged_str", true);
    }

    private static int visibleRangedAttackWithoutAmmo(EquipmentLoadout gear) {
        return visible(gear, "offensive", "ranged", false);
    }

    private static int visibleRangedStrengthWithoutAmmo(EquipmentLoadout gear) {
        return visible(gear, "bonuses", "ranged_str", false);
    }

    private static int visible(EquipmentLoadout gear, String block, String field, boolean includeAmmo) {
        int total = 0;
        for (java.util.Map.Entry<EquipmentSlot, com.dpscalc.equipment.EquipmentItem> entry : gear.asMap().entrySet()) {
            if (!includeAmmo && entry.getKey() == EquipmentSlot.AMMO) continue;
            JsonObject itemFacts = factsById(entry.getValue().getOriginalId());
            total += stat(itemFacts, block, field);
        }
        return total;
    }

    private static EquipmentDomainCatalog loadCatalog() throws IOException {
        return EquipmentDomainCatalogTest.loadCatalog();
    }
}
