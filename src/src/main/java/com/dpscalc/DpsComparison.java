package com.dpscalc;


public class DpsComparison {
    private final String targetName;
    private final String weaponName;
    private final double dps;
    private final int maxHit;
    private final double accuracy;
    private final int attackSpeed;
    private final long timestamp;

    public String getTargetName() { return targetName; }

    public String getWeaponName() { return weaponName; }

    public double getDps() { return dps; }

    public int getMaxHit() { return maxHit; }

    public double getAccuracy() { return accuracy; }

    public int getAttackSpeed() { return attackSpeed; }

    public long getTimestamp() { return timestamp; }

    public DpsComparison(String targetName, String weaponName, double dps, int maxHit, double accuracy, int attackSpeed, long timestamp) { this.targetName = targetName; this.weaponName = weaponName; this.dps = dps; this.maxHit = maxHit; this.accuracy = accuracy; this.attackSpeed = attackSpeed; this.timestamp = timestamp; }
}
