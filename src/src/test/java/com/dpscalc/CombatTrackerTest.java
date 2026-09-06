package com.dpscalc;

import static org.junit.Assert.*;

import com.dpscalc.combat.CombatTracker;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

public class CombatTrackerTest {
    @Test
    public void missesAndIdleTimeShareDamageAndKillWindow() {
        AtomicLong time = new AtomicLong();
        CombatTracker tracker = new CombatTracker(time::get);
        tracker.beginCombat();
        time.set(2000);
        tracker.recordDamage(20);
        tracker.recordKill();
        assertEquals(10, tracker.getActualDps(), 0);
        assertEquals(1800, tracker.getActualKillsPerHour(), 0);
        time.set(4000);
        tracker.recordDamage(0);
        assertEquals(5, tracker.getActualDps(), 0);
        assertEquals(900, tracker.getActualKillsPerHour(), 0);
        time.set(6000);
        assertEquals(20 / 6.0, tracker.getActualDps(), 1e-12);
    }

    @Test
    public void timeoutResetsKillsAndDamageTogetherOnNextActivity() {
        AtomicLong time = new AtomicLong(100);
        CombatTracker tracker = new CombatTracker(time::get);
        tracker.recordDamage(50);
        tracker.recordKill();
        time.set(10101);
        assertFalse(tracker.isInCombat());
        assertEquals(0, tracker.getActualDps(), 0);
        assertEquals(10000, tracker.getCombatDurationMs());
        tracker.recordDamage(0);
        assertEquals(0, tracker.getKillCount());
        assertEquals(0, tracker.getTotalDamage());
        assertEquals(0, tracker.getCombatDurationMs());
        time.addAndGet(1000);
        tracker.recordDamage(10);
        assertEquals(10, tracker.getActualDps(), 0);
        tracker.reset();
        assertEquals(0, tracker.getCombatDurationMs());
        assertEquals(0, tracker.getKillCount());
        assertFalse(tracker.isInCombat());
    }
}
