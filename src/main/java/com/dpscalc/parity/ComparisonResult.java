package com.dpscalc.parity;

public final class ComparisonResult {
    private static final ComparisonResult MATCH = new ComparisonResult(true, "");

    private final boolean match;
    private final String diagnostic;

    private ComparisonResult(boolean match, String diagnostic) {
        this.match = match;
        this.diagnostic = diagnostic;
    }

    public static ComparisonResult match() {
        return MATCH;
    }

    public static ComparisonResult mismatch(String diagnostic) {
        return new ComparisonResult(false, diagnostic);
    }

    public boolean isMatch() {
        return match;
    }

    public String getDiagnostic() {
        return diagnostic;
    }
}
