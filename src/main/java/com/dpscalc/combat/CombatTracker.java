package com.dpscalc.combat;

import java.util.function.LongSupplier;

import javax.inject.Singleton;

/** All measured statistics use the same activity window, including misses and idle time. */
@Singleton
public class CombatTracker {
    private static final long TIMEOUT_MS = 10_000;
    private final LongSupplier clock;
    private long started, lastActivity;
    private int totalDamage, killCount;
    private boolean active;

    public CombatTracker() {
        this(() -> System.nanoTime() / 1_000_000);
    }

    public CombatTracker(LongSupplier clock) {
        this.clock = clock;
    }

    public synchronized void reset() {
        active = false;
        started = lastActivity = 0;
        totalDamage = killCount = 0;
    }

    public synchronized void beginCombat() {
        long now = clock.getAsLong();
        if (!active || now - lastActivity > TIMEOUT_MS) {
            started = now;
            totalDamage = killCount = 0;
            active = true;
        }
        lastActivity = now;
    }

    public synchronized void recordDamage(int damage) {
        if (damage < 0) return;
        beginCombat();
        totalDamage += damage;
    }

    public synchronized void recordKill() {
        beginCombat();
        killCount++;
    }

    public synchronized boolean isInCombat() {
        return active && clock.getAsLong() - lastActivity <= TIMEOUT_MS;
    }

    public synchronized long getCombatDurationMs() {
        return active
                ? Math.max(0, Math.min(clock.getAsLong(), lastActivity + TIMEOUT_MS) - started)
                : 0;
    }

    public synchronized double getActualDps() {
        long elapsed = getCombatDurationMs();
        return isInCombat() && elapsed > 0 ? totalDamage * 1000.0 / elapsed : 0;
    }

    public synchronized double getActualKillsPerHour() {
        long elapsed = getCombatDurationMs();
        return isInCombat() && elapsed > 0 ? killCount * 3600000.0 / elapsed : 0;
    }

    public synchronized int getTotalDamage() {
        return totalDamage;
    }

    public synchronized int getKillCount() {
        return killCount;
    }

    public String getFormattedActualDps() {
        double value = getActualDps();
        return value > 0 ? String.format("%.2f", value) : "N/A";
    }

    public String getFormattedActualKillsPerHour() {
        double value = getActualKillsPerHour();
        return value <= 0
                ? "N/A"
                : value >= 1000
                        ? String.format("%.1fk", value / 1000)
                        : String.format("%.0f", value);
    }

    public String getFormattedCombatDuration() {
        long seconds = getCombatDurationMs() / 1000;
        return seconds < 60 ? seconds + "s" : String.format("%dm %ds", seconds / 60, seconds % 60);
    }
}
