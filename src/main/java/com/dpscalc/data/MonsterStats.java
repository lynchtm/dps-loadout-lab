package com.dpscalc.data;

import java.util.HashSet;
import java.util.Set;

public class MonsterStats {
    private int id;
    private String name;
    private String version;
    private int size;
    private int speed;

    private int attackLevel;
    private int strengthLevel;
    private int defenceLevel;
    private int hitpoints;
    private int magicLevel;
    private int rangedLevel;

    private int stabDefence;
    private int slashDefence;
    private int crushDefence;
    private int magicDefence;
    private int lightRangedDefence;
    private int standardRangedDefence;
    private int heavyRangedDefence;
    private int flatArmour;

    private int offensiveMagic;

    private Set<MonsterAttribute> attributes = new HashSet<>();

    private WeaknessElement weaknessElement;
    private int weaknessSeverity;

    private MonsterInputs inputs = new MonsterInputs();

    public MonsterStats() {}

    public MonsterStats(MonsterStats other) {
        this.id = other.id;
        this.name = other.name;
        this.version = other.version;
        this.size = other.size;
        this.speed = other.speed;
        this.attackLevel = other.attackLevel;
        this.strengthLevel = other.strengthLevel;
        this.defenceLevel = other.defenceLevel;
        this.hitpoints = other.hitpoints;
        this.magicLevel = other.magicLevel;
        this.rangedLevel = other.rangedLevel;
        this.stabDefence = other.stabDefence;
        this.slashDefence = other.slashDefence;
        this.crushDefence = other.crushDefence;
        this.magicDefence = other.magicDefence;
        this.lightRangedDefence = other.lightRangedDefence;
        this.standardRangedDefence = other.standardRangedDefence;
        this.heavyRangedDefence = other.heavyRangedDefence;
        this.flatArmour = other.flatArmour;
        this.offensiveMagic = other.offensiveMagic;
        this.attributes = new HashSet<>(other.attributes);
        this.weaknessElement = other.weaknessElement;
        this.weaknessSeverity = other.weaknessSeverity;
        this.inputs = new MonsterInputs(other.inputs);
    }

    public MonsterStats copy() {
        return new MonsterStats(this);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public int getSpeed() { return speed; }
    public void setSpeed(int speed) { this.speed = speed; }

    public int getAttackLevel() { return attackLevel; }
    public void setAttackLevel(int attackLevel) { this.attackLevel = attackLevel; }

    public int getStrengthLevel() { return strengthLevel; }
    public void setStrengthLevel(int strengthLevel) { this.strengthLevel = strengthLevel; }

    public int getDefenceLevel() { return defenceLevel; }
    public void setDefenceLevel(int defenceLevel) { this.defenceLevel = defenceLevel; }

    public int getHitpoints() { return hitpoints; }
    public void setHitpoints(int hitpoints) { this.hitpoints = hitpoints; }

    public int getMagicLevel() { return magicLevel; }
    public void setMagicLevel(int magicLevel) { this.magicLevel = magicLevel; }

    public int getRangedLevel() { return rangedLevel; }
    public void setRangedLevel(int rangedLevel) { this.rangedLevel = rangedLevel; }

    public int getStabDefence() { return stabDefence; }
    public void setStabDefence(int stabDefence) { this.stabDefence = stabDefence; }

    public int getSlashDefence() { return slashDefence; }
    public void setSlashDefence(int slashDefence) { this.slashDefence = slashDefence; }

    public int getCrushDefence() { return crushDefence; }
    public void setCrushDefence(int crushDefence) { this.crushDefence = crushDefence; }

    public int getMagicDefence() { return magicDefence; }
    public void setMagicDefence(int magicDefence) { this.magicDefence = magicDefence; }

    public int getLightRangedDefence() { return lightRangedDefence; }
    public void setLightRangedDefence(int lightRangedDefence) { this.lightRangedDefence = lightRangedDefence; }

    public int getStandardRangedDefence() { return standardRangedDefence; }
    public void setStandardRangedDefence(int standardRangedDefence) { this.standardRangedDefence = standardRangedDefence; }

    public int getHeavyRangedDefence() { return heavyRangedDefence; }
    public void setHeavyRangedDefence(int heavyRangedDefence) { this.heavyRangedDefence = heavyRangedDefence; }

    public int getFlatArmour() { return flatArmour; }
    public void setFlatArmour(int flatArmour) { this.flatArmour = flatArmour; }

    public Set<MonsterAttribute> getAttributes() { return attributes; }
    public void setAttributes(Set<MonsterAttribute> attributes) { this.attributes = attributes; }

    public void addAttribute(MonsterAttribute attr) {
        if (attr != null) {
            this.attributes.add(attr);
        }
    }

    public boolean hasAttribute(MonsterAttribute attr) {
        return attributes.contains(attr);
    }

    public WeaknessElement getWeaknessElement() { return weaknessElement; }
    public void setWeaknessElement(WeaknessElement weaknessElement) { this.weaknessElement = weaknessElement; }

    public int getWeaknessSeverity() { return weaknessSeverity; }
    public void setWeaknessSeverity(int weaknessSeverity) { this.weaknessSeverity = weaknessSeverity; }

    public int getDefenceForStyle(String attackType) {
        if (attackType == null) return crushDefence;
        switch (attackType.toLowerCase()) {
            case "stab": return stabDefence;
            case "slash": return slashDefence;
            case "crush": return crushDefence;
            case "magic": return magicDefence;
            case "ranged": return standardRangedDefence;
            case "light": return lightRangedDefence;
            case "standard": return standardRangedDefence;
            case "heavy": return heavyRangedDefence;
            default: return crushDefence;
        }
    }

    public int getDefenceLevelForMagic() {
        return magicLevel;
    }

    public int getOffensiveMagic() {
        return offensiveMagic;
    }

    public void setOffensiveMagic(int offensiveMagic) {
        this.offensiveMagic = offensiveMagic;
    }

    public MonsterInputs getInputs() {
        return inputs;
    }

    public void setInputs(MonsterInputs inputs) {
        this.inputs = inputs != null ? inputs : new MonsterInputs();
    }

    public boolean usesDefenceForMagicDefence() {
        return MonsterConstants.USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS.contains(id);
    }

    public boolean isToaMonster() {
        return MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(id);
    }

    public boolean isToaPathMonster() {
        return MonsterConstants.TOMBS_OF_AMASCUT_PATH_MONSTER_IDS.contains(id);
    }

    public String[] getAvailablePhases() {
        return MonsterConstants.getPhasesForMonster(id);
    }

    @Override
    public String toString() {
        return name + (version != null && !version.isEmpty() ? " (" + version + ")" : "") + " [ID: " + id + "]";
    }
}
