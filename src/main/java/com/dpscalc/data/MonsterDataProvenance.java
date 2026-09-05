package com.dpscalc.data;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class MonsterDataProvenance {
    static final String REFERENCE_SHA = "b6bc098dc0d742b2b763375d2e78e1b611a22070";
    static final String DATA_SHA256 = "909c316da2d4269e4de2a86506a588b2a9df93826e9e0016d121cfc5edc32fc1";
    static final String RAW_URL = "https://raw.githubusercontent.com/weirdgloop/osrs-dps-calc/"
        + REFERENCE_SHA + "/cdn/json/monsters.json";

    private MonsterDataProvenance() {
    }

    static boolean matches(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                hexadecimal.append(String.format("%02x", value & 0xff));
            }
            return DATA_SHA256.equals(hexadecimal.toString());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
