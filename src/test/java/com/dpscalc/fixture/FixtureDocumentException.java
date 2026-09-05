package com.dpscalc.fixture;

public final class FixtureDocumentException extends IllegalArgumentException {
    public FixtureDocumentException(String path, String detail) {
        super(path + ": " + detail);
    }

    public FixtureDocumentException(String path, String detail, Throwable cause) {
        super(path + ": " + detail, cause);
    }
}
