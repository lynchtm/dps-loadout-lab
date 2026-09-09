/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.calculation;

import com.loadoutlab.engine.DamagePmf;

import java.util.*;

public final class DpsResult {
    private final DamagePmf distribution;
    private final double accuracy, speed, attackRoll, defenceRoll, dot;
    private final List<String> details;
    private int monsterHp;

    public DpsResult(
            DamagePmf distribution,
            double accuracy,
            double speed,
            double attackRoll,
            double defenceRoll,
            double dot,
            List<String> details) {
        this.distribution = distribution;
        this.accuracy = accuracy;
        this.speed = speed;
        this.attackRoll = attackRoll;
        this.defenceRoll = defenceRoll;
        this.dot = dot;
        this.details = Collections.unmodifiableList(new ArrayList<>(details));
    }

    public DamagePmf getAttackDistribution() {
        return distribution;
    }

    public int getMaxHit() {
        return distribution.maximum();
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getAttackRoll() {
        return attackRoll;
    }

    public double getDefenceRoll() {
        return defenceRoll;
    }

    public double getExpectedDamage() {
        return distribution.mean() + dot;
    }

    public double getExpectedDirectDamage() {
        return distribution.mean();
    }

    public double getExpectedDotDamage() {
        return dot;
    }

    public double getExpectedAttackSpeed() {
        return speed;
    }

    public double getDps() {
        return getExpectedDamage() / (speed * .6);
    }

    public int getMonsterHp() {
        return monsterHp;
    }

    public void setMonsterHp(int hp) {
        monsterHp = hp;
    }

    public List<String> getDetails() {
        return details;
    }
}
