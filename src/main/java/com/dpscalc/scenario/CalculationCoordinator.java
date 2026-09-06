package com.dpscalc.scenario;

import com.dpscalc.data.MonsterStats;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded calculation cache. Only calculation inputs determine reuse. */
public final class CalculationCoordinator {
    private final ScenarioCalculator calculator;
    private long generation;
    private final Map<String, ScenarioCalculator.Result> cache =
            new LinkedHashMap<String, ScenarioCalculator.Result>(32, .75f, true) {
                protected boolean removeEldestEntry(
                        Map.Entry<String, ScenarioCalculator.Result> entry) {
                    return size() > 64;
                }
            };

    public CalculationCoordinator(ScenarioCalculator calculator) {
        this.calculator = calculator;
    }

    public ScenarioCalculator.Result calculate(
            Scenario.Loadout loadout, MonsterStats target, Encounter encounter) {
        String key = CalculationInputs.key(loadout, target, encounter);
        ScenarioCalculator.Result result;
        long epoch;
        synchronized (this) {
            result = cache.get(key);
            epoch = generation;
        }
        if (result == null) {
            result = calculator.calculate(loadout, target, encounter);
            synchronized (this) {
                if (epoch == generation && !Thread.currentThread().isInterrupted())
                    cache.put(key, result);
            }
        }
        return result.named(loadout.name);
    }

    public synchronized void clear() {
        generation++;
        cache.clear();
    }
}
