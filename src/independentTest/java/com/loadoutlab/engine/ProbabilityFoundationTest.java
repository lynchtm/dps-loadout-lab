/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. See src/independent/LICENSE.
 */
package com.loadoutlab.engine;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.CancellationException;

public class ProbabilityFoundationTest {
    private static final double EPS = 1e-10;

    @Test
    public void damageTransformMergesCollisionsAndPreservesMass() {
        DamagePmf source = new DamagePmf(.1, .2, .3, .4);
        assertArrayEquals(new double[] {.3, .7}, source.map(d -> d / 2).probabilities(), EPS);
        assertArrayEquals(new double[] {.1, .2, .3, .4}, source.probabilities(), 0);
        assertThrows(IllegalArgumentException.class, () -> source.map(d -> -1));
        assertThrows(IllegalArgumentException.class, () -> source.map(d -> 10001));
    }

    @Test
    public void rollFormulaMatchesExhaustiveOrderedPairs() {
        for (int a = 0; a <= 25; a++)
            for (int d = 0; d <= 25; d++) {
                int wins = 0;
                for (int x = 0; x <= a; x++) for (int y = 0; y <= d; y++) if (x > y) wins++;
                assertEquals(
                        wins / (double) ((a + 1) * (d + 1)),
                        RollContest.successProbability(a, d),
                        EPS);
            }
    }

    @Test
    public void rollMaximaDoNotOverflow() {
        assertEquals(
                0.5 - 1.0 / 4294967296.0,
                RollContest.successProbability(Integer.MAX_VALUE, Integer.MAX_VALUE),
                EPS);
        assertEquals(
                1 - 1.0 / 2147483648.0, RollContest.successProbability(Integer.MAX_VALUE, 0), EPS);
        assertEquals(0, RollContest.successProbability(0, Integer.MAX_VALUE), 0);
        assertThrows(IllegalArgumentException.class, () -> RollContest.successProbability(-1, 0));
    }

    @Test
    public void singleHitDistinguishesMissesAndSuccessfulZero() {
        assertArrayEquals(
                new double[] {0.5, 0.25, 0.25},
                DamagePmf.singleHit(0.5, 1, 2).probabilities(),
                EPS);
        assertArrayEquals(
                new double[] {0.5, 0.25, 0.25},
                DamagePmf.singleHit(0.75, 0, 2).probabilities(),
                EPS);
        assertArrayEquals(new double[] {1}, DamagePmf.singleHit(0, 1, 20).probabilities(), EPS);
        assertArrayEquals(new double[] {1}, DamagePmf.singleHit(1, 0, 0).probabilities(), EPS);
    }

    @Test
    public void probabilitiesAreValidatedAndImmutable() {
        double[] source = {0.5, 0.5, 0};
        DamagePmf p = new DamagePmf(source);
        source[0] = 0;
        p.probabilities()[0] = 0;
        assertEquals(0.5, p.probabilityAt(0), 0);
        assertEquals(1, p.maximum());
        assertEquals(0, p.probabilityAt(-1), 0);
        assertEquals(0, p.probabilityAt(100), 0);
        for (double bad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, -0.1, 1.1}) {
            assertThrows(IllegalArgumentException.class, () -> new DamagePmf(bad));
        }
        assertThrows(IllegalArgumentException.class, () -> new DamagePmf(0.2, 0.2));
        assertThrows(IllegalArgumentException.class, () -> new DamagePmf());
        assertThrows(IllegalArgumentException.class, () -> DamagePmf.singleHit(1, 2, 1));
    }

    @Test
    public void independentSumMatchesEnumeratedDice() {
        DamagePmf die = DamagePmf.singleHit(1, 0, 5);
        double[] expected = new double[11];
        for (int a = 0; a <= 5; a++) for (int b = 0; b <= 5; b++) expected[a + b] += 1.0 / 36;
        DamagePmf sum = die.independentSum(die);
        assertArrayEquals(expected, sum.probabilities(), EPS);
        assertEquals(5, sum.mean(), EPS);
        assertEquals(10, sum.maximum());
        assertEquals(2, sum.dps(2.5), EPS);
        assertThrows(IllegalArgumentException.class, () -> sum.dps(0));
        assertThrows(IllegalArgumentException.class, () -> sum.dps(Double.NaN));
    }

    @Test
    public void mixtureIsNotIndependentAddition() {
        DamagePmf zero = new DamagePmf(1);
        DamagePmf two = new DamagePmf(0, 0, 1);
        assertArrayEquals(
                new double[] {0.25, 0, 0.75}, zero.mixture(0.25, two).probabilities(), EPS);
        assertArrayEquals(two.probabilities(), zero.independentSum(two).probabilities(), EPS);
        assertThrows(IllegalArgumentException.class, () -> zero.mixture(2, two));
    }

    @Test
    public void deterministicDamageIncludesOverkillAndExplicitFirstHitDelay() {
        DamagePmf hit = DamagePmf.singleHit(1, 3, 3);
        StationaryKill.Moments moments = StationaryKill.moments(hit, 7);
        assertEquals(3, moments.expectedAttacks(), EPS);
        assertEquals(0, moments.attackVariance(), EPS);
        assertEquals(4, moments.expectedSeconds(2, 0), EPS);
        assertEquals(6, moments.expectedSeconds(2, 2), EPS);
        assertArrayEquals(new double[] {0, 0, 1, 0}, StationaryKill.horizon(hit, 7, 3), EPS);
    }

    @Test
    public void bernoulliKillMatchesNegativeBinomialMomentsAndPreservesTail() {
        DamagePmf hit = new DamagePmf(0.5, 0.5);
        for (int hp = 1; hp <= 10; hp++) {
            StationaryKill.Moments moments = StationaryKill.moments(hit, hp);
            assertEquals(2.0 * hp, moments.expectedAttacks(), EPS);
            assertEquals(2.0 * hp, moments.attackVariance(), EPS);
        }
        assertArrayEquals(
                new double[] {0.5, 0.25, 0.125, 0.125}, StationaryKill.horizon(hit, 1, 3), EPS);
        assertArrayEquals(new double[] {1}, StationaryKill.horizon(hit, 1, 0), EPS);
    }

    @Test
    public void horizonMatchesBruteForceAttackSequences() {
        DamagePmf hit = new DamagePmf(0.25, 0.25, 0.5);
        double[] expected = new double[5];
        enumerate(hit, 4, 0, 4, 1, expected);
        double[] actual = StationaryKill.horizon(hit, 4, 4);
        assertArrayEquals(expected, actual, EPS);
        assertEquals(1, Arrays.stream(actual).sum(), EPS);
        StationaryKill.Moments simple = StationaryKill.moments(new DamagePmf(0, 0.5, 0.5), 2);
        assertEquals(1.5, simple.expectedAttacks(), EPS);
        assertEquals(0.25, simple.attackVariance(), EPS);
    }

    private static void enumerate(
            DamagePmf p, int hp, int turn, int limit, double weight, double[] bins) {
        if (hp <= 0) {
            bins[turn - 1] += weight;
            return;
        }
        if (turn == limit) {
            bins[limit] += weight;
            return;
        }
        for (int damage = 0; damage <= p.maximum(); damage++) {
            enumerate(p, hp - damage, turn + 1, limit, weight * p.probabilityAt(damage), bins);
        }
    }

    @Test
    public void impossibleAndVeryRareDamageRemainDistinct() {
        StationaryKill.Moments impossible = StationaryKill.moments(new DamagePmf(1), 1);
        assertEquals(Double.POSITIVE_INFINITY, impossible.expectedAttacks(), 0);
        assertTrue(Double.isNaN(impossible.attackVariance()));
        assertArrayEquals(
                new double[] {0, 0, 1}, StationaryKill.horizon(new DamagePmf(1), 5, 2), EPS);
        assertEquals(
                1e15,
                StationaryKill.moments(new DamagePmf(1 - 1e-15, 1e-15), 1).expectedAttacks(),
                1);
    }

    @Test
    public void costlyRequestsFailBeforeAllocationAndCancellationIsPreserved() {
        DamagePmf hit = DamagePmf.singleHit(1, 0, 10_000);
        assertThrows(IllegalArgumentException.class, () -> hit.independentSum(hit));
        assertThrows(IllegalArgumentException.class, () -> StationaryKill.moments(hit, 100_000));
        assertThrows(
                IllegalArgumentException.class,
                () -> StationaryKill.horizon(hit, 100, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> StationaryKill.moments(hit, 0));
        Thread.currentThread().interrupt();
        try {
            assertThrows(CancellationException.class, () -> StationaryKill.moments(hit, 1));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
