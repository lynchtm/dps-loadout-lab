package com.dpscalc.calc.distribution;

import java.util.Objects;

public final class Hitsplat implements Comparable<Hitsplat> {
    private static final Hitsplat INACCURATE = new Hitsplat(0, false);

    private final int damage;
    private final boolean accurate;

    public Hitsplat(int damage, boolean accurate) {
        if (damage < 0) {
            throw new IllegalArgumentException("damage must be non-negative");
        }
        this.damage = damage;
        this.accurate = accurate;
    }

    public static Hitsplat accurate(int damage) {
        return new Hitsplat(damage, true);
    }

    public static Hitsplat inaccurate() {
        return INACCURATE;
    }

    public int getDamage() {
        return damage;
    }

    public boolean isAccurate() {
        return accurate;
    }

    @Override
    public int compareTo(Hitsplat other) {
        int accuracyOrder = Boolean.compare(other.accurate, accurate);
        return accuracyOrder != 0 ? accuracyOrder : Integer.compare(damage, other.damage);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof Hitsplat)) return false;
        Hitsplat hitsplat = (Hitsplat) object;
        return damage == hitsplat.damage && accurate == hitsplat.accurate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(damage, accurate);
    }

    @Override
    public String toString() {
        return "Hitsplat{" + "damage=" + damage + ", accurate=" + accurate + '}';
    }
}
