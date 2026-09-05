package com.dpscalc.equipment;

public final class EquipmentDomainException extends IllegalArgumentException {
    public EquipmentDomainException(String path, String detail) {
        super(path + ": " + detail);
    }

    public EquipmentDomainException(String path, String detail, Throwable cause) {
        super(path + ": " + detail, cause);
    }
}
