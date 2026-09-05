package com.dpscalc.combat;

import javax.inject.Singleton;

@Singleton
public class CombatTracker {

    private int totalDamage;
    private long combatStartTime;
    private long lastDamageTime;
    private int killCount;
    private boolean inCombat;
    private double cachedDps;
    private long cachedDurationMs;

    private static final long COMBAT_TIMEOUT_MS = 10000;

    public void reset() {
        totalDamage = 0;
        combatStartTime = 0;
        lastDamageTime = 0;
        killCount = 0;
        inCombat = false;
        cachedDps = 0;
        cachedDurationMs = 0;
    }

    public void recordDamage(int damage) {
        if (damage <= 0) {
            return;
        }

        long now = System.currentTimeMillis();

        if (!inCombat || (now - lastDamageTime) > COMBAT_TIMEOUT_MS) {
            combatStartTime = now;
            totalDamage = 0;
            inCombat = true;
        }

        totalDamage += damage;
        lastDamageTime = now;

        cachedDurationMs = now - combatStartTime;
        if (cachedDurationMs > 0) {
            cachedDps = (totalDamage * 1000.0) / cachedDurationMs;
        }
    }

    public void recordKill() {
        killCount++;
    }

    public double getActualDps() {
        if (!inCombat || totalDamage == 0) {
            return 0;
        }

        long now = System.currentTimeMillis();
        if ((now - lastDamageTime) > COMBAT_TIMEOUT_MS) {
            inCombat = false;
            return 0;
        }

        return cachedDps;
    }

    public String getFormattedActualDps() {
        double dps = getActualDps();
        if (dps <= 0) {
            return "N/A";
        }
        return String.format("%.2f", dps);
    }

    public int getTotalDamage() {
        return totalDamage;
    }

    public long getCombatDurationMs() {
        if (!inCombat || combatStartTime == 0) {
            return 0;
        }
        return cachedDurationMs;
    }

    public String getFormattedCombatDuration() {
        long durationMs = getCombatDurationMs();
        if (durationMs <= 0) {
            return "0s";
        }

        long seconds = durationMs / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%dm %ds", minutes, seconds);
    }

    public int getKillCount() {
        return killCount;
    }

    public boolean isInCombat() {
        if (!inCombat) {
            return false;
        }

        long now = System.currentTimeMillis();
        if ((now - lastDamageTime) > COMBAT_TIMEOUT_MS) {
            inCombat = false;
            return false;
        }
        return true;
    }

    public double getActualKillsPerHour() {
        if (cachedDurationMs <= 0 || killCount == 0) {
            return 0;
        }

        return (killCount * 3600000.0) / cachedDurationMs;
    }

    public String getFormattedActualKillsPerHour() {
        double kph = getActualKillsPerHour();
        if (kph <= 0) {
            return "N/A";
        }
        if (kph >= 1000) {
            return String.format("%.1fk", kph / 1000);
        }
        return String.format("%.0f", kph);
    }
}
