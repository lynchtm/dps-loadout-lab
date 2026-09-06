package com.dpscalc.equipment;

import java.util.Objects;

public final class ItemVariable {
    public enum Kind { BOOLEAN, NUMBER, STRING }

    private final Kind kind;
    private final Boolean booleanValue;
    private final Double numberValue;
    private final String stringValue;

    private ItemVariable(Kind kind, Boolean booleanValue, Double numberValue, String stringValue) {
        this.kind = kind;
        this.booleanValue = booleanValue;
        this.numberValue = numberValue;
        this.stringValue = stringValue;
    }

    public static ItemVariable ofBoolean(boolean value) {
        return new ItemVariable(Kind.BOOLEAN, value, null, null);
    }

    public static ItemVariable ofNumber(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Item variable number must be finite");
        return new ItemVariable(Kind.NUMBER, null, value, null);
    }

    public static ItemVariable ofString(String value) {
        return new ItemVariable(Kind.STRING, null, null, Objects.requireNonNull(value, "value"));
    }

    public Kind getKind() { return kind; }
    public Boolean getBooleanValue() { return booleanValue; }
    public Double getNumberValue() { return numberValue; }
    public String getStringValue() { return stringValue; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ItemVariable)) return false;
        ItemVariable that = (ItemVariable) other;
        return kind == that.kind
            && Objects.equals(booleanValue, that.booleanValue)
            && Objects.equals(numberValue, that.numberValue)
            && Objects.equals(stringValue, that.stringValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, booleanValue, numberValue, stringValue);
    }
}
