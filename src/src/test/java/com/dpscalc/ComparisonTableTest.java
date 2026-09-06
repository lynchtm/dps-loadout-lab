package com.dpscalc;

import static org.junit.Assert.*;

import com.dpscalc.calc.DpsResult;
import com.dpscalc.scenario.*;

import org.junit.Test;

import java.util.*;

import javax.swing.*;

public class ComparisonTableTest {
    private ScenarioCalculator.Result result(String name, double dps, int max, double ttk) {
        ScenarioCalculator.Result r = new ScenarioCalculator.Result();
        r.name = name;
        r.normal = new DpsResult();
        r.normal.setDps(dps);
        r.normal.setMaxHit(max);
        r.normal.setAccuracy(.9);
        r.ttk = ttk;
        return r;
    }

    @Test
    public void highlightsBestValuesTiesAndLowerKillTimesWithoutRankingErrors() throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    ComparisonTable table = new ComparisonTable(i -> {});
                    ScenarioCalculator.Result invalid = result("Invalid", 1000, 1000, 0);
                    invalid.error = "Unsupported weapon";
                    table.setResults(
                            Arrays.asList(
                                    result("Fast", 8, 30, 15),
                                    result("Heavy", 6, 50, 20),
                                    result("Tied", 8, 40, 15),
                                    invalid),
                            0);
                    assertTrue(table.best(0, 0));
                    assertTrue(table.best(0, 2));
                    assertFalse(table.best(0, 1));
                    assertFalse(table.best(0, 3));
                    assertTrue(table.best(1, 1));
                    assertFalse(table.best(1, 0));
                    assertTrue(table.best(3, 0));
                    assertFalse(table.best(3, 1));
                    table.setColumnWidth(120);
                    table.setSize(620, 240);
                    layout(table);
                    java.awt.image.BufferedImage image =
                            new java.awt.image.BufferedImage(
                                    620, 240, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g = image.createGraphics();
                    table.printAll(g);
                    g.dispose();
                    java.io.File file = new java.io.File("build/ui/comparison-table.png");
                    file.getParentFile().mkdirs();
                    try {
                        javax.imageio.ImageIO.write(image, "png", file);
                    } catch (java.io.IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }

    private static void layout(java.awt.Container c) {
        c.doLayout();
        for (java.awt.Component child : c.getComponents())
            if (child instanceof java.awt.Container) layout((java.awt.Container) child);
    }

    @Test
    public void groupedTableHighlightsGroupDpsSeparatelyFromPerMonsterDps() throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    ComparisonTable table = new ComparisonTable(null);
                    ScenarioCalculator.Result staff = result("Powered staff", 8, 30, 15),
                            barrage = result("Ice Barrage", 5, 30, 24);
                    staff.groupDps = 8.0;
                    staff.encounterTargets = 5;
                    staff.targetsHit = 1;
                    barrage.groupDps = 25.0;
                    barrage.encounterTargets = 5;
                    barrage.targetsHit = 5;
                    table.setResults(Arrays.asList(staff, barrage), 1);
                    assertTrue(table.best(0, 0));
                    assertFalse(table.best(0, 1));
                    assertTrue(table.best(1, 1));
                    assertFalse(table.best(1, 0));
                    assertTrue(table.best(4, 0));
                    table.setSize(225, 260);
                    layout(table);
                    java.awt.image.BufferedImage image =
                            new java.awt.image.BufferedImage(
                                    225, 260, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    java.awt.Graphics2D g = image.createGraphics();
                    table.printAll(g);
                    g.dispose();
                    java.io.File file = new java.io.File("build/ui/group-comparison.png");
                    file.getParentFile().mkdirs();
                    try {
                        javax.imageio.ImageIO.write(image, "png", file);
                    } catch (java.io.IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }
}
