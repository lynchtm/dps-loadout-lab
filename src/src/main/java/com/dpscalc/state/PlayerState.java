package com.dpscalc.state;

import com.dpscalc.equipment.EquipmentLoadout;
import com.dpscalc.equipment.AmmoApplicability;

import java.util.EnumSet;
import java.util.Set;

public class PlayerState {
    private int attackLevel;
    private int strengthLevel;
    private int defenceLevel;
    private int rangedLevel;
    private int magicLevel;
    private int prayerLevel;
    private int hitpointsLevel;

    private int currentHitpoints;

    private int attackBoost;
    private int strengthBoost;
    private int defenceBoost;
    private int rangedBoost;
    private int magicBoost;

    private EquipmentStats equipmentStats;

    private int[] equippedItemIds = new int[14];
    private String[] equippedItemNames = new String[14];
    private String[] equippedItemVersions = new String[14];
    private String[] equippedItemCategories = new String[14];
    private EquipmentLoadout rawEquipmentLoadout;
    private AmmoApplicability ammoApplicability = AmmoApplicability.ALLOWED;

    private CombatStyle combatStyle;

    private Set<Prayer> activePrayers = EnumSet.noneOf(Prayer.class);

    private int weaponSpeed;

    private boolean onSlayerTask;
    private boolean inWilderness;
    private boolean chargeSpellActive;
    private boolean kandarinDiary = true;
    private boolean forinthrySurgeActive;
    private int soulreaperStacks;
    private boolean markOfDarknessActive;
    private boolean usingSunfireRunes;
    private String spellName;
    private String spellbook;
    private String spellElement;
    private int spellMaxHit;

    public int getAttackLevel() { return attackLevel; }
    public void setAttackLevel(int attackLevel) { this.attackLevel = attackLevel; }

    public int getStrengthLevel() { return strengthLevel; }
    public void setStrengthLevel(int strengthLevel) { this.strengthLevel = strengthLevel; }

    public int getDefenceLevel() { return defenceLevel; }
    public void setDefenceLevel(int defenceLevel) { this.defenceLevel = defenceLevel; }

    public int getRangedLevel() { return rangedLevel; }
    public void setRangedLevel(int rangedLevel) { this.rangedLevel = rangedLevel; }

    public int getMagicLevel() { return magicLevel; }
    public void setMagicLevel(int magicLevel) { this.magicLevel = magicLevel; }

    public int getPrayerLevel() { return prayerLevel; }
    public void setPrayerLevel(int prayerLevel) { this.prayerLevel = prayerLevel; }

    public int getHitpointsLevel() { return hitpointsLevel; }
    public void setHitpointsLevel(int hitpointsLevel) { this.hitpointsLevel = hitpointsLevel; }

    public int getCurrentHitpoints() { return currentHitpoints; }
    public void setCurrentHitpoints(int currentHitpoints) { this.currentHitpoints = currentHitpoints; }

    public int getAttackBoost() { return attackBoost; }
    public void setAttackBoost(int attackBoost) { this.attackBoost = attackBoost; }

    public int getStrengthBoost() { return strengthBoost; }
    public void setStrengthBoost(int strengthBoost) { this.strengthBoost = strengthBoost; }

    public int getDefenceBoost() { return defenceBoost; }
    public void setDefenceBoost(int defenceBoost) { this.defenceBoost = defenceBoost; }

    public int getRangedBoost() { return rangedBoost; }
    public void setRangedBoost(int rangedBoost) { this.rangedBoost = rangedBoost; }

    public int getMagicBoost() { return magicBoost; }
    public void setMagicBoost(int magicBoost) { this.magicBoost = magicBoost; }

    public EquipmentStats getEquipmentStats() { return equipmentStats; }
    public void setEquipmentStats(EquipmentStats equipmentStats) { this.equipmentStats = equipmentStats; }

    public int[] getEquippedItemIds() { return equippedItemIds; }
    public void setEquippedItemIds(int[] equippedItemIds) { this.equippedItemIds = equippedItemIds; }

    public String[] getEquippedItemNames() { return equippedItemNames; }
    public void setEquippedItemNames(String[] equippedItemNames) { this.equippedItemNames = equippedItemNames; }

    public String[] getEquippedItemVersions() { return equippedItemVersions; }
    public void setEquippedItemVersions(String[] equippedItemVersions) { this.equippedItemVersions = equippedItemVersions; }

    public String[] getEquippedItemCategories() { return equippedItemCategories; }
    public void setEquippedItemCategories(String[] equippedItemCategories) { this.equippedItemCategories = equippedItemCategories; }

    public EquipmentLoadout getRawEquipmentLoadout() { return rawEquipmentLoadout; }
    public void setRawEquipmentLoadout(EquipmentLoadout rawEquipmentLoadout) { this.rawEquipmentLoadout = rawEquipmentLoadout; }
    public AmmoApplicability getAmmoApplicability() { return ammoApplicability; }
    public void setAmmoApplicability(AmmoApplicability ammoApplicability) { this.ammoApplicability = ammoApplicability; }

    public int getWeaponId() { return equippedItemIds[EquipmentSlot.WEAPON.getIndex()]; }
    public String getWeaponName() { return equippedItemNames[EquipmentSlot.WEAPON.getIndex()]; }
    public String getWeaponVersion() { return equippedItemVersions[EquipmentSlot.WEAPON.getIndex()]; }
    public String getWeaponCategory() { return equippedItemCategories[EquipmentSlot.WEAPON.getIndex()]; }
    public int getAmmoId() { return equippedItemIds[EquipmentSlot.AMMO.getIndex()]; }
    public String getAmmoName() { return equippedItemNames[EquipmentSlot.AMMO.getIndex()]; }
    public String getCapeVersion() { return equippedItemVersions[EquipmentSlot.CAPE.getIndex()]; }
    public String getShieldVersion() { return equippedItemVersions[EquipmentSlot.SHIELD.getIndex()]; }

    public CombatStyle getCombatStyle() { return combatStyle; }
    public void setCombatStyle(CombatStyle combatStyle) { this.combatStyle = combatStyle; }

    public Set<Prayer> getActivePrayers() { return activePrayers; }
    public void setActivePrayers(Set<Prayer> activePrayers) { this.activePrayers = activePrayers; }

    public int getWeaponSpeed() { return weaponSpeed; }
    public void setWeaponSpeed(int weaponSpeed) { this.weaponSpeed = weaponSpeed; }

    public boolean isOnSlayerTask() { return onSlayerTask; }
    public void setOnSlayerTask(boolean onSlayerTask) { this.onSlayerTask = onSlayerTask; }

    public boolean isInWilderness() { return inWilderness; }
    public void setInWilderness(boolean inWilderness) { this.inWilderness = inWilderness; }

    public boolean isChargeSpellActive() { return chargeSpellActive; }
    public void setChargeSpellActive(boolean chargeSpellActive) { this.chargeSpellActive = chargeSpellActive; }

    public boolean isKandarinDiary() { return kandarinDiary; }
    public void setKandarinDiary(boolean kandarinDiary) { this.kandarinDiary = kandarinDiary; }

    public boolean isForinthrySurgeActive() { return forinthrySurgeActive; }
    public void setForinthrySurgeActive(boolean forinthrySurgeActive) { this.forinthrySurgeActive = forinthrySurgeActive; }

    public int getSoulreaperStacks() { return soulreaperStacks; }
    public void setSoulreaperStacks(int soulreaperStacks) { this.soulreaperStacks = Math.max(0, Math.min(5, soulreaperStacks)); }

    public boolean isMarkOfDarknessActive() { return markOfDarknessActive; }
    public void setMarkOfDarknessActive(boolean markOfDarknessActive) { this.markOfDarknessActive = markOfDarknessActive; }

    public boolean isUsingSunfireRunes() { return usingSunfireRunes; }
    public void setUsingSunfireRunes(boolean usingSunfireRunes) { this.usingSunfireRunes = usingSunfireRunes; }

    public String getSpellName() { return spellName; }
    public void setSpellName(String spellName) { this.spellName = spellName; }

    public String getSpellbook() { return spellbook; }
    public void setSpellbook(String spellbook) { this.spellbook = spellbook; }

    public String getSpellElement() { return spellElement; }
    public void setSpellElement(String spellElement) { this.spellElement = spellElement; }

    public int getSpellMaxHit() { return spellMaxHit; }
    public void setSpellMaxHit(int spellMaxHit) { this.spellMaxHit = spellMaxHit; }

    public int getBoostedAttack() { return attackLevel + attackBoost; }
    public int getBoostedStrength() { return strengthLevel + strengthBoost; }
    public int getBoostedDefence() { return defenceLevel + defenceBoost; }
    public int getBoostedRanged() { return rangedLevel + rangedBoost; }
    public int getBoostedMagic() { return magicLevel + magicBoost; }

    public boolean isWearing(String itemName) {
        if (itemName == null) return false;
        for (String equipped : equippedItemNames) {
            if (itemName.equals(equipped)) return true;
        }
        return false;
    }

    public boolean isWearingAny(String... itemNames) {
        for (String itemName : itemNames) {
            if (isWearing(itemName)) return true;
        }
        return false;
    }

    public boolean isWearingAll(String... itemNames) {
        for (String itemName : itemNames) {
            if (!isWearing(itemName)) return false;
        }
        return true;
    }

    public boolean isWearingItemContaining(String substring) {
        if (substring == null) return false;
        for (String equipped : equippedItemNames) {
            if (equipped != null && equipped.contains(substring)) return true;
        }
        return false;
    }
}
