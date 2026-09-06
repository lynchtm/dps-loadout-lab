package com.dpscalc.calc.distribution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class WeightedHit implements Comparable<WeightedHit> {
    private final double probability;
    private final List<Hitsplat> hitsplats;

    public WeightedHit(double probability, List<Hitsplat> hitsplats) {
        if (!Double.isFinite(probability) || probability < 0) {
            throw new IllegalArgumentException("probability must be finite and non-negative");
        }
        if (hitsplats == null || hitsplats.isEmpty() || hasNull(hitsplats)) {
            throw new IllegalArgumentException("hitsplats must be non-empty and contain no nulls");
        }
        this.probability = probability;
        this.hitsplats = Collections.unmodifiableList(new ArrayList<>(hitsplats));
    }

    private static boolean hasNull(List<Hitsplat> hitsplats) {
        for (Hitsplat hitsplat : hitsplats) if (hitsplat == null) return true;
        return false;
    }

    public double getProbability() {
        return probability;
    }

    public List<Hitsplat> getHitsplats() {
        return hitsplats;
    }

    public int getSum() {
        int sum = 0;
        for (Hitsplat hitsplat : hitsplats) {
            sum += hitsplat.getDamage();
        }
        return sum;
    }

    public double getExpectedValue() {
        return probability * getSum();
    }

    public boolean anyAccurate() {
        for (Hitsplat hitsplat : hitsplats) {
            if (hitsplat.isAccurate()) return true;
        }
        return false;
    }

    public WeightedHit scale(double factor) {
        if (!Double.isFinite(factor) || factor < 0) {
            throw new IllegalArgumentException("factor must be finite and non-negative");
        }
        return new WeightedHit(probability * factor, hitsplats);
    }

    public WeightedHit zip(WeightedHit other) {
        Objects.requireNonNull(other, "other");
        List<Hitsplat> combined = new ArrayList<>(hitsplats.size() + other.hitsplats.size());
        combined.addAll(hitsplats);
        combined.addAll(other.hitsplats);
        return new WeightedHit(probability * other.probability, combined);
    }

    @Override
    public int compareTo(WeightedHit other) {
        int commonSize = Math.min(hitsplats.size(), other.hitsplats.size());
        for (int index = 0; index < commonSize; index++) {
            int comparison = hitsplats.get(index).compareTo(other.hitsplats.get(index));
            if (comparison != 0) return comparison;
        }
        int sizeComparison = Integer.compare(hitsplats.size(), other.hitsplats.size());
        return sizeComparison != 0 ? sizeComparison : Double.compare(probability, other.probability);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof WeightedHit)) return false;
        WeightedHit that = (WeightedHit) object;
        return Double.compare(probability, that.probability) == 0 && hitsplats.equals(that.hitsplats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(probability, hitsplats);
    }

    @Override
    public String toString() {
        return "WeightedHit{" + "probability=" + probability + ", hitsplats=" + hitsplats + '}';
    }
}
