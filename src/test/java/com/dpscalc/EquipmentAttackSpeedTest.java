package com.dpscalc;

import com.dpscalc.equipment.EquipmentCalculator;
import com.dpscalc.equipment.EquipmentDomainCatalog;
import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.EquipmentSlot;
import org.junit.Test;

import java.io.IOException;

import static com.dpscalc.EquipmentCalculatorTest.context;
import static com.dpscalc.EquipmentCalculatorTest.style;
import static com.dpscalc.EquipmentTestFixtures.item;
import static com.dpscalc.EquipmentTestFixtures.loadout;
import static org.junit.Assert.assertEquals;

public class EquipmentAttackSpeedTest {
    private final EquipmentCalculator calculator = new EquipmentCalculator(loadCatalog());

    public EquipmentAttackSpeedTest() throws IOException {}

    @Test
    public void appliesRapidAndDefaultSpeedFloor() {
        EquipmentLoadout bow = loadout(EquipmentSlot.WEAPON, item("Twisted bow"));
        EquipmentLoadout empty = EquipmentLoadout.of(java.util.Map.of());

        int rapid = calculator.calculate(bow, context(415, style("ranged", "Rapid"), "")).getAttackSpeed();
        int defaultSpeed = calculator.calculate(empty, context(415, style("crush", "Accurate"), "")).getAttackSpeed();

        assertEquals(5, rapid);
        assertEquals(4, defaultSpeed);
    }

    @Test
    public void appliesCastStaffOverridesAndManualCastGate() {
        EquipmentLoadout harmonised = loadout(EquipmentSlot.WEAPON, item("Harmonised nightmare staff"));
        EquipmentLoadout twinflame = loadout(EquipmentSlot.WEAPON, item("Twinflame staff"));

        int harmonisedAuto = calculator.calculate(harmonised, context(415, style("magic", "Autocast"), "standard")).getAttackSpeed();
        int harmonisedManual = calculator.calculate(harmonised, context(415, style("magic", "Manual Cast"), "standard")).getAttackSpeed();
        int twinflameAuto = calculator.calculate(twinflame, context(415, style("magic", "Autocast"), "standard")).getAttackSpeed();

        assertEquals(4, harmonisedAuto);
        assertEquals(5, harmonisedManual);
        assertEquals(6, twinflameAuto);
    }

    @Test
    public void appliesScurriusBoneWeaponSpeedOutsideManualCast() {
        EquipmentLoadout mace = loadout(EquipmentSlot.WEAPON, item("Bone mace"));
        EquipmentLoadout bow = loadout(EquipmentSlot.WEAPON, item("Bone shortbow"));
        EquipmentLoadout staff = loadout(EquipmentSlot.WEAPON, item("Bone staff"));

        int maceSpeed = calculator.calculate(mace, context(7223, style("crush", "Accurate"), "")).getAttackSpeed();
        int bowSpeed = calculator.calculate(bow, context(7223, style("ranged", "Rapid"), "")).getAttackSpeed();
        int staffManual = calculator.calculate(staff, context(7223, style("magic", "Manual Cast"), "standard")).getAttackSpeed();

        assertEquals(1, maceSpeed);
        assertEquals(1, bowSpeed);
        assertEquals(5, staffManual);
    }

    private static EquipmentDomainCatalog loadCatalog() throws IOException {
        return EquipmentDomainCatalogTest.loadCatalog();
    }
}
