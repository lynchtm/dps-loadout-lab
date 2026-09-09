/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently authored; see PROVENANCE.md.
 */
package com.loadoutlab.model;

public class MonsterStats {
    private int id = -1;
    private String name = "Custom target";
    private String version = "";
    private String sourcePage = "";
    private int size = 1;
    private int speed = 4;
    private int hitpoints = 100;
    private int attackLevel = 1;
    private int strengthLevel = 1;
    private int defenceLevel = 1;
    private int rangedLevel = 1;
    private int magicLevel = 1;
    private int stabDefence = 0;
    private int slashDefence = 0;
    private int crushDefence = 0;
    private int magicDefence = 0;
    private int lightRangedDefence = 0;
    private int standardRangedDefence = 0;
    private int heavyRangedDefence = 0;
    private int flatArmour = 0;
    private int offensiveMagic = 0;
    private int weaknessSeverity = 0;
    private java.util.Set<MonsterAttribute> attributes =
            java.util.EnumSet.noneOf(MonsterAttribute.class);
    private WeaknessElement weaknessElement = null;
    private MonsterInputs inputs = new MonsterInputs();

    public MonsterStats() {}

    public MonsterStats(MonsterStats other) {
        id = other.id;
        name = other.name;
        version = other.version;
        sourcePage = other.sourcePage;
        size = other.size;
        speed = other.speed;
        hitpoints = other.hitpoints;
        attackLevel = other.attackLevel;
        strengthLevel = other.strengthLevel;
        defenceLevel = other.defenceLevel;
        rangedLevel = other.rangedLevel;
        magicLevel = other.magicLevel;
        stabDefence = other.stabDefence;
        slashDefence = other.slashDefence;
        crushDefence = other.crushDefence;
        magicDefence = other.magicDefence;
        lightRangedDefence = other.lightRangedDefence;
        standardRangedDefence = other.standardRangedDefence;
        heavyRangedDefence = other.heavyRangedDefence;
        flatArmour = other.flatArmour;
        offensiveMagic = other.offensiveMagic;
        weaknessSeverity = other.weaknessSeverity;
        attributes = new java.util.HashSet<>(other.attributes);
        weaknessElement = other.weaknessElement;
        inputs = new MonsterInputs(other.inputs);
    }

    public int getId() {
        return id;
    }

    public void setId(int value) {
        id = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String value) {
        version = value;
    }

    public String getSourcePage() {
        return sourcePage;
    }

    public void setSourcePage(String value) {
        sourcePage = value;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int value) {
        size = value;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int value) {
        speed = value;
    }

    public int getHitpoints() {
        return hitpoints;
    }

    public void setHitpoints(int value) {
        hitpoints = value;
    }

    public int getAttackLevel() {
        return attackLevel;
    }

    public void setAttackLevel(int value) {
        attackLevel = value;
    }

    public int getStrengthLevel() {
        return strengthLevel;
    }

    public void setStrengthLevel(int value) {
        strengthLevel = value;
    }

    public int getDefenceLevel() {
        return defenceLevel;
    }

    public void setDefenceLevel(int value) {
        defenceLevel = value;
    }

    public int getRangedLevel() {
        return rangedLevel;
    }

    public void setRangedLevel(int value) {
        rangedLevel = value;
    }

    public int getMagicLevel() {
        return magicLevel;
    }

    public void setMagicLevel(int value) {
        magicLevel = value;
    }

    public int getStabDefence() {
        return stabDefence;
    }

    public void setStabDefence(int value) {
        stabDefence = value;
    }

    public int getSlashDefence() {
        return slashDefence;
    }

    public void setSlashDefence(int value) {
        slashDefence = value;
    }

    public int getCrushDefence() {
        return crushDefence;
    }

    public void setCrushDefence(int value) {
        crushDefence = value;
    }

    public int getMagicDefence() {
        return magicDefence;
    }

    public void setMagicDefence(int value) {
        magicDefence = value;
    }

    public int getLightRangedDefence() {
        return lightRangedDefence;
    }

    public void setLightRangedDefence(int value) {
        lightRangedDefence = value;
    }

    public int getStandardRangedDefence() {
        return standardRangedDefence;
    }

    public void setStandardRangedDefence(int value) {
        standardRangedDefence = value;
    }

    public int getHeavyRangedDefence() {
        return heavyRangedDefence;
    }

    public void setHeavyRangedDefence(int value) {
        heavyRangedDefence = value;
    }

    public int getFlatArmour() {
        return flatArmour;
    }

    public void setFlatArmour(int value) {
        flatArmour = value;
    }

    public int getOffensiveMagic() {
        return offensiveMagic;
    }

    public void setOffensiveMagic(int value) {
        offensiveMagic = value;
    }

    public int getWeaknessSeverity() {
        return weaknessSeverity;
    }

    public void setWeaknessSeverity(int value) {
        weaknessSeverity = value;
    }

    public java.util.Set<MonsterAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(java.util.Set<MonsterAttribute> value) {
        attributes = value;
    }

    public WeaknessElement getWeaknessElement() {
        return weaknessElement;
    }

    public void setWeaknessElement(WeaknessElement value) {
        weaknessElement = value;
    }

    public MonsterInputs getInputs() {
        return inputs;
    }

    public void setInputs(MonsterInputs value) {
        inputs = value;
    }

    public MonsterStats copy() {
        return new MonsterStats(this);
    }

    public void addAttribute(MonsterAttribute attribute) {
        attributes.add(attribute);
    }

    public boolean hasAttribute(MonsterAttribute attribute) {
        return attributes.contains(attribute);
    }

    public boolean usesDefenceForMagicDefence() {
        return name.equalsIgnoreCase("Verzik Vitur") || name.equalsIgnoreCase("Ice demon");
    }

    public int getDefenceLevelForMagic() {
        return usesDefenceForMagicDefence() ? defenceLevel : magicLevel;
    }

    public int getDefenceForStyle(String key) {
        switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "stab":
                return stabDefence;
            case "slash":
                return slashDefence;
            case "crush":
                return crushDefence;
            case "magic":
                return magicDefence;
            case "ranged_light":
            case "light":
                return lightRangedDefence;
            case "ranged_heavy":
            case "heavy":
                return heavyRangedDefence;
            default:
                return standardRangedDefence;
        }
    }

    public boolean isToaMonster() {
        return java.util.Arrays.asList(
                        "Akkha",
                        "Ba-Ba",
                        "Kephri",
                        "Zebak",
                        "Elidinis' Warden",
                        "Tumeken's Warden",
                        "Warden core")
                .contains(name);
    }

    public boolean isToaPathMonster() {
        return isToaMonster() && !name.contains("Warden") && !name.contains("core");
    }

    public String[] getAvailablePhases() {
        return new String[0];
    }

    @Override
    public String toString() {
        return name + (version == null || version.isEmpty() ? "" : " (" + version + ")");
    }
}
