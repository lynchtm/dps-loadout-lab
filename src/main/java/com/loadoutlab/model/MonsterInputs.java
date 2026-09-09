/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently authored; see PROVENANCE.md.
 */
package com.loadoutlab.model;

public class MonsterInputs {
    private int toaInvocationLevel = 0;
    private int toaPathLevel = 0;
    private int monsterCurrentHp = 0;
    private int partySumMiningLevel = 0;
    private int partyMaxCombatLevel = 126;
    private int partyMaxHpLevel = 99;
    private int partySize = 1;
    private int demonbaneVulnerability = 100;
    private String phase = null;
    private boolean fromCoxCm = false;
    private DefenceReductions defenceReductions = new DefenceReductions();

    public MonsterInputs() {}

    public MonsterInputs(MonsterInputs other) {
        toaInvocationLevel = other.toaInvocationLevel;
        toaPathLevel = other.toaPathLevel;
        monsterCurrentHp = other.monsterCurrentHp;
        partySumMiningLevel = other.partySumMiningLevel;
        partyMaxCombatLevel = other.partyMaxCombatLevel;
        partyMaxHpLevel = other.partyMaxHpLevel;
        partySize = other.partySize;
        demonbaneVulnerability = other.demonbaneVulnerability;
        phase = other.phase;
        fromCoxCm = other.fromCoxCm;
        defenceReductions = new DefenceReductions(other.defenceReductions);
    }

    public int getToaInvocationLevel() {
        return toaInvocationLevel;
    }

    public void setToaInvocationLevel(int value) {
        toaInvocationLevel = value;
    }

    public int getToaPathLevel() {
        return toaPathLevel;
    }

    public void setToaPathLevel(int value) {
        toaPathLevel = value;
    }

    public int getMonsterCurrentHp() {
        return monsterCurrentHp;
    }

    public void setMonsterCurrentHp(int value) {
        monsterCurrentHp = value;
    }

    public int getPartySumMiningLevel() {
        return partySumMiningLevel;
    }

    public void setPartySumMiningLevel(int value) {
        partySumMiningLevel = value;
    }

    public int getPartyMaxCombatLevel() {
        return partyMaxCombatLevel;
    }

    public void setPartyMaxCombatLevel(int value) {
        partyMaxCombatLevel = value;
    }

    public int getPartyMaxHpLevel() {
        return partyMaxHpLevel;
    }

    public void setPartyMaxHpLevel(int value) {
        partyMaxHpLevel = value;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int value) {
        partySize = value;
    }

    public int getDemonbaneVulnerability() {
        return demonbaneVulnerability;
    }

    public void setDemonbaneVulnerability(int value) {
        demonbaneVulnerability = value;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String value) {
        phase = value;
    }

    public boolean isFromCoxCm() {
        return fromCoxCm;
    }

    public void setFromCoxCm(boolean value) {
        fromCoxCm = value;
    }

    public DefenceReductions getDefenceReductions() {
        return defenceReductions;
    }

    public void setDefenceReductions(DefenceReductions value) {
        defenceReductions = value;
    }

    public static class DefenceReductions {
        private int dwh;

        public int getDwh() {
            return dwh;
        }

        public void setDwh(int value) {
            dwh = value;
        }

        private int bgs;

        public int getBgs() {
            return bgs;
        }

        public void setBgs(int value) {
            bgs = value;
        }

        private int arclight;

        public int getArclight() {
            return arclight;
        }

        public void setArclight(int value) {
            arclight = value;
        }

        private int emberlight;

        public int getEmberlight() {
            return emberlight;
        }

        public void setEmberlight(int value) {
            emberlight = value;
        }

        private int tonalztic;

        public int getTonalztic() {
            return tonalztic;
        }

        public void setTonalztic(int value) {
            tonalztic = value;
        }

        private int elderMaul;

        public int getElderMaul() {
            return elderMaul;
        }

        public void setElderMaul(int value) {
            elderMaul = value;
        }

        private int vulnerability;

        public int getVulnerability() {
            return vulnerability;
        }

        public void setVulnerability(int value) {
            vulnerability = value;
        }

        private int accursedSceptre;

        public int getAccursedSceptre() {
            return accursedSceptre;
        }

        public void setAccursedSceptre(int value) {
            accursedSceptre = value;
        }

        private int seercull;

        public int getSeercull() {
            return seercull;
        }

        public void setSeercull(int value) {
            seercull = value;
        }

        private int ayak;

        public int getAyak() {
            return ayak;
        }

        public void setAyak(int value) {
            ayak = value;
        }

        public DefenceReductions() {}

        public DefenceReductions(DefenceReductions other) {
            dwh = other.dwh;
            bgs = other.bgs;
            arclight = other.arclight;
            emberlight = other.emberlight;
            tonalztic = other.tonalztic;
            elderMaul = other.elderMaul;
            vulnerability = other.vulnerability;
            accursedSceptre = other.accursedSceptre;
            seercull = other.seercull;
            ayak = other.ayak;
        }
    }
}
