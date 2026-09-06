package com.dpscalc.parity;

import com.dpscalc.calc.distribution.HitDistribution;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ParityOutput {
    private final Map<String, Integer> integers;
    private final Map<String, Double> floats;
    private final HitDistribution distribution;

    private ParityOutput(Builder builder) {
        this.integers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.integers));
        this.floats = Collections.unmodifiableMap(new LinkedHashMap<>(builder.floats));
        this.distribution = Objects.requireNonNull(builder.distribution, "distribution");
    }

    public Map<String, Integer> getIntegers() { return integers; }
    public Map<String, Double> getFloats() { return floats; }
    public HitDistribution getDistribution() { return distribution; }

    public Builder toBuilder() {
        return new Builder().putIntegers(integers).putFloats(floats).distribution(distribution);
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final Map<String, Integer> integers = new LinkedHashMap<>();
        private final Map<String, Double> floats = new LinkedHashMap<>();
        private HitDistribution distribution;

        public Builder putInteger(String path, int value) {
            integers.put(path, value);
            return this;
        }

        public Builder putFloat(String path, double value) {
            floats.put(path, value);
            return this;
        }

        public Builder putIntegers(Map<String, Integer> values) {
            integers.putAll(values);
            return this;
        }

        public Builder putFloats(Map<String, Double> values) {
            floats.putAll(values);
            return this;
        }

        public Builder distribution(HitDistribution value) {
            distribution = value;
            return this;
        }

        public ParityOutput build() { return new ParityOutput(this); }
    }
}
