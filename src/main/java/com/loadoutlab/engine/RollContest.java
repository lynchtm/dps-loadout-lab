/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. See src/independent/LICENSE.
 */
package com.loadoutlab.engine;

/** Independent uniform integer rolls with a strict greater-than success rule. */
public final class RollContest {
    private RollContest() {}

    public static double successProbability(int attackerMaximum, int defenderMaximum) {
        if (attackerMaximum < 0 || defenderMaximum < 0) {
            throw new IllegalArgumentException("Roll maxima must be nonnegative");
        }
        // Promote before adding: Integer.MAX_VALUE is a valid maximum.
        double a = attackerMaximum;
        double d = defenderMaximum;
        return a <= d ? a / (2.0 * (d + 1.0)) : 1.0 - (d + 2.0) / (2.0 * (a + 1.0));
    }
}
