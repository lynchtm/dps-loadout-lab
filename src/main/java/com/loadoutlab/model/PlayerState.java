/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently authored; see PROVENANCE.md.
 */
package com.loadoutlab.model;

public class PlayerState {
    private int attackLevel = 99;
    private int strengthLevel = 99;
    private int defenceLevel = 99;
    private int rangedLevel = 99;
    private int magicLevel = 99;
    private int prayerLevel = 99;
    private int hitpointsLevel = 99;
    private int attackBoost = 0;
    private int strengthBoost = 0;
    private int defenceBoost = 0;
    private int rangedBoost = 0;
    private int magicBoost = 0;
    private int currentHitpoints = 99;
    private EquipmentStats equipmentStats = new EquipmentStats();
    private int[] equippedItemIds = new int[14];
    private String[] equippedItemNames = new String[14];
    private String[] equippedItemVersions = new String[14];
    private String[] equippedItemCategories = new String[14];
    private CombatStyle combatStyle = CombatStyle.UNARMED_PUNCH;
    private java.util.Set<Prayer> activePrayers = java.util.EnumSet.noneOf(Prayer.class);
    private int weaponSpeed = 4;
    private int soulreaperStacks = 0;
    private int spellMaxHit = 0;
    private boolean onSlayerTask = false;
    private boolean inWilderness = false;
    private boolean chargeSpellActive = false;
    private boolean kandarinDiary = false;
    private boolean forinthrySurgeActive = false;
    private boolean markOfDarknessActive = false;
    private boolean usingSunfireRunes = false;
    private String spellName = null;
    private String spellElement = null;
    private String spellbook = "Standard";
    private com.loadoutlab.equipment.EquipmentLoadout rawEquipmentLoadout = null;
    private com.loadoutlab.equipment.AmmoApplicability ammoApplicability =
            com.loadoutlab.equipment.AmmoApplicability.ALLOWED;

    public PlayerState() {
        java.util.Arrays.fill(equippedItemIds, -1);
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

    public int getPrayerLevel() {
        return prayerLevel;
    }

    public void setPrayerLevel(int value) {
        prayerLevel = value;
    }

    public int getHitpointsLevel() {
        return hitpointsLevel;
    }

    public void setHitpointsLevel(int value) {
        hitpointsLevel = value;
    }

    public int getAttackBoost() {
        return attackBoost;
    }

    public void setAttackBoost(int value) {
        attackBoost = value;
    }

    public int getStrengthBoost() {
        return strengthBoost;
    }

    public void setStrengthBoost(int value) {
        strengthBoost = value;
    }

    public int getDefenceBoost() {
        return defenceBoost;
    }

    public void setDefenceBoost(int value) {
        defenceBoost = value;
    }

    public int getRangedBoost() {
        return rangedBoost;
    }

    public void setRangedBoost(int value) {
        rangedBoost = value;
    }

    public int getMagicBoost() {
        return magicBoost;
    }

    public void setMagicBoost(int value) {
        magicBoost = value;
    }

    public int getCurrentHitpoints() {
        return currentHitpoints;
    }

    public void setCurrentHitpoints(int value) {
        currentHitpoints = value;
    }

    public EquipmentStats getEquipmentStats() {
        return equipmentStats;
    }

    public void setEquipmentStats(EquipmentStats value) {
        equipmentStats = value;
    }

    public int[] getEquippedItemIds() {
        return equippedItemIds;
    }

    public void setEquippedItemIds(int[] value) {
        equippedItemIds = value;
    }

    public String[] getEquippedItemNames() {
        return equippedItemNames;
    }

    public void setEquippedItemNames(String[] value) {
        equippedItemNames = value;
    }

    public String[] getEquippedItemVersions() {
        return equippedItemVersions;
    }

    public void setEquippedItemVersions(String[] value) {
        equippedItemVersions = value;
    }

    public String[] getEquippedItemCategories() {
        return equippedItemCategories;
    }

    public void setEquippedItemCategories(String[] value) {
        equippedItemCategories = value;
    }

    public CombatStyle getCombatStyle() {
        return combatStyle;
    }

    public void setCombatStyle(CombatStyle value) {
        combatStyle = value;
    }

    public java.util.Set<Prayer> getActivePrayers() {
        return activePrayers;
    }

    public void setActivePrayers(java.util.Set<Prayer> value) {
        activePrayers = value;
    }

    public int getWeaponSpeed() {
        return weaponSpeed;
    }

    public void setWeaponSpeed(int value) {
        weaponSpeed = value;
    }

    public int getSoulreaperStacks() {
        return soulreaperStacks;
    }

    public void setSoulreaperStacks(int value) {
        soulreaperStacks = value;
    }

    public int getSpellMaxHit() {
        return spellMaxHit;
    }

    public void setSpellMaxHit(int value) {
        spellMaxHit = value;
    }

    public boolean isOnSlayerTask() {
        return onSlayerTask;
    }

    public void setOnSlayerTask(boolean value) {
        onSlayerTask = value;
    }

    public boolean isInWilderness() {
        return inWilderness;
    }

    public void setInWilderness(boolean value) {
        inWilderness = value;
    }

    public boolean isChargeSpellActive() {
        return chargeSpellActive;
    }

    public void setChargeSpellActive(boolean value) {
        chargeSpellActive = value;
    }

    public boolean isKandarinDiary() {
        return kandarinDiary;
    }

    public void setKandarinDiary(boolean value) {
        kandarinDiary = value;
    }

    public boolean isForinthrySurgeActive() {
        return forinthrySurgeActive;
    }

    public void setForinthrySurgeActive(boolean value) {
        forinthrySurgeActive = value;
    }

    public boolean isMarkOfDarknessActive() {
        return markOfDarknessActive;
    }

    public void setMarkOfDarknessActive(boolean value) {
        markOfDarknessActive = value;
    }

    public boolean isUsingSunfireRunes() {
        return usingSunfireRunes;
    }

    public void setUsingSunfireRunes(boolean value) {
        usingSunfireRunes = value;
    }

    public String getSpellName() {
        return spellName;
    }

    public void setSpellName(String value) {
        spellName = value;
    }

    public String getSpellElement() {
        return spellElement;
    }

    public void setSpellElement(String value) {
        spellElement = value;
    }

    public String getSpellbook() {
        return spellbook;
    }

    public void setSpellbook(String value) {
        spellbook = value;
    }

    public com.loadoutlab.equipment.EquipmentLoadout getRawEquipmentLoadout() {
        return rawEquipmentLoadout;
    }

    public void setRawEquipmentLoadout(com.loadoutlab.equipment.EquipmentLoadout value) {
        rawEquipmentLoadout = value;
    }

    public com.loadoutlab.equipment.AmmoApplicability getAmmoApplicability() {
        return ammoApplicability;
    }

    public void setAmmoApplicability(com.loadoutlab.equipment.AmmoApplicability value) {
        ammoApplicability = value;
    }

    public int getBoostedAttack() {
        return Math.max(0, attackLevel + attackBoost);
    }

    public int getBoostedStrength() {
        return Math.max(0, strengthLevel + strengthBoost);
    }

    public int getBoostedDefence() {
        return Math.max(0, defenceLevel + defenceBoost);
    }

    public int getBoostedRanged() {
        return Math.max(0, rangedLevel + rangedBoost);
    }

    public int getBoostedMagic() {
        return Math.max(0, magicLevel + magicBoost);
    }

    public int getWeaponId() {
        return equippedItemIds[3];
    }

    public String getWeaponName() {
        return equippedItemNames[3];
    }

    public String getWeaponVersion() {
        return equippedItemVersions[3];
    }

    public String getWeaponCategory() {
        return equippedItemCategories[3];
    }

    public int getAmmoId() {
        return equippedItemIds[13];
    }

    public String getAmmoName() {
        return equippedItemNames[13];
    }

    public String getCapeVersion() {
        return equippedItemVersions[1];
    }

    public String getShieldVersion() {
        return equippedItemVersions[5];
    }

    public boolean isWearing(String name) {
        for (String item : equippedItemNames)
            if (name != null && name.equalsIgnoreCase(item)) return true;
        return false;
    }

    public boolean isWearingAny(String... names) {
        for (String name : names) if (isWearing(name)) return true;
        return false;
    }

    public boolean isWearingAll(String... names) {
        for (String name : names) if (!isWearing(name)) return false;
        return true;
    }

    public boolean isWearingItemContaining(String text) {
        if (text == null) return false;
        for (String item : equippedItemNames)
            if (item != null
                    && item.toLowerCase(java.util.Locale.ROOT)
                            .contains(text.toLowerCase(java.util.Locale.ROOT))) return true;
        return false;
    }
}
