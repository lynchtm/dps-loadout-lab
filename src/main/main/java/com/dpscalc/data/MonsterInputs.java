package com.dpscalc.data;

public class MonsterInputs {

    private int toaInvocationLevel;
    private int toaPathLevel;
    private int monsterCurrentHp;
    private String phase;
    private boolean fromCoxCm;
    private int partyMaxCombatLevel = 126;
    private int partySumMiningLevel = 99;
    private int partyMaxHpLevel = 99;
    private int partySize = 1;
    private int demonbaneVulnerability = 100;
    private DefenceReductions defenceReductions = new DefenceReductions();

    public MonsterInputs() {}

    public MonsterInputs(MonsterInputs other) {
        this.toaInvocationLevel = other.toaInvocationLevel;
        this.toaPathLevel = other.toaPathLevel;
        this.monsterCurrentHp = other.monsterCurrentHp;
        this.phase = other.phase;
        this.fromCoxCm = other.fromCoxCm;
        this.partyMaxCombatLevel = other.partyMaxCombatLevel;
        this.partySumMiningLevel = other.partySumMiningLevel;
        this.partyMaxHpLevel = other.partyMaxHpLevel;
        this.partySize = other.partySize;
        this.demonbaneVulnerability = other.demonbaneVulnerability;
        this.defenceReductions = new DefenceReductions(other.defenceReductions);
    }

    public int getToaInvocationLevel() {
        return toaInvocationLevel;
    }

    public void setToaInvocationLevel(int toaInvocationLevel) {
        this.toaInvocationLevel = toaInvocationLevel;
    }

    public int getToaPathLevel() {
        return toaPathLevel;
    }

    public void setToaPathLevel(int toaPathLevel) {
        this.toaPathLevel = toaPathLevel;
    }

    public int getMonsterCurrentHp() {
        return monsterCurrentHp;
    }

    public void setMonsterCurrentHp(int monsterCurrentHp) {
        this.monsterCurrentHp = monsterCurrentHp;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public boolean isFromCoxCm() {
        return fromCoxCm;
    }

    public void setFromCoxCm(boolean fromCoxCm) {
        this.fromCoxCm = fromCoxCm;
    }

    public int getPartyMaxCombatLevel() {
        return partyMaxCombatLevel;
    }

    public void setPartyMaxCombatLevel(int partyMaxCombatLevel) {
        this.partyMaxCombatLevel = partyMaxCombatLevel;
    }

    public int getPartySumMiningLevel() {
        return partySumMiningLevel;
    }

    public void setPartySumMiningLevel(int partySumMiningLevel) {
        this.partySumMiningLevel = partySumMiningLevel;
    }

    public int getPartyMaxHpLevel() {
        return partyMaxHpLevel;
    }

    public void setPartyMaxHpLevel(int partyMaxHpLevel) {
        this.partyMaxHpLevel = partyMaxHpLevel;
    }

    public int getPartySize() {
        return partySize;
    }

    public void setPartySize(int partySize) {
        this.partySize = Math.max(1, partySize);
    }

    public int getDemonbaneVulnerability() {
        return demonbaneVulnerability;
    }

    public void setDemonbaneVulnerability(int demonbaneVulnerability) {
        this.demonbaneVulnerability = demonbaneVulnerability;
    }

    public DefenceReductions getDefenceReductions() {
        return defenceReductions;
    }

    public void setDefenceReductions(DefenceReductions defenceReductions) {
        this.defenceReductions = defenceReductions;
    }

    public static class DefenceReductions {
        private int dwh;
        private int bgs;
        private int arclight;
        private int emberlight;
        private int tonalztic;
        private int elderMaul;
        private int vulnerability;
        private int accursedSceptre;
        private int seercull;
        private int ayak;

        public DefenceReductions() {}

        public DefenceReductions(DefenceReductions other) {
            this.dwh = other.dwh;
            this.bgs = other.bgs;
            this.arclight = other.arclight;
            this.emberlight = other.emberlight;
            this.tonalztic = other.tonalztic;
            this.elderMaul = other.elderMaul;
            this.vulnerability = other.vulnerability;
            this.accursedSceptre = other.accursedSceptre;
            this.seercull = other.seercull;
            this.ayak = other.ayak;
        }

        public int getDwh() { return dwh; }
        public void setDwh(int dwh) { this.dwh = dwh; }

        public int getBgs() { return bgs; }
        public void setBgs(int bgs) { this.bgs = bgs; }

        public int getArclight() { return arclight; }
        public void setArclight(int arclight) { this.arclight = arclight; }

        public int getEmberlight() { return emberlight; }
        public void setEmberlight(int emberlight) { this.emberlight = emberlight; }

        public int getTonalztic() { return tonalztic; }
        public void setTonalztic(int tonalztic) { this.tonalztic = tonalztic; }

        public int getElderMaul() { return elderMaul; }
        public void setElderMaul(int elderMaul) { this.elderMaul = elderMaul; }

        public int getVulnerability() { return vulnerability; }
        public void setVulnerability(int vulnerability) { this.vulnerability = vulnerability; }

        public int getAccursedSceptre() { return accursedSceptre; }
        public void setAccursedSceptre(int accursedSceptre) { this.accursedSceptre = accursedSceptre; }

        public int getSeercull() { return seercull; }
        public void setSeercull(int seercull) { this.seercull = seercull; }

        public int getAyak() { return ayak; }
        public void setAyak(int ayak) { this.ayak = ayak; }
    }
}
