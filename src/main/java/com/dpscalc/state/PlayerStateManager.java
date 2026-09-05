package com.dpscalc.state;

import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.equipment.EquipmentResult;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.VarPlayer;
import net.runelite.api.Varbits;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.Set;

@Singleton
public class PlayerStateManager {
    private static final Logger log = LoggerFactory.getLogger(PlayerStateManager.class);

    private static final int WILDERNESS_VARBIT = 5963;

    @Inject
    private Client client;

    @Inject
    private ItemManager itemManager;

    @Inject
    private EquipmentPreparationFacade equipmentPreparation;

    public PlayerState getPlayerState() {
        PlayerState state = new PlayerState();

        readSkillLevels(state);
        readEquipment(state);
        readPrayers(state);
        readCombatStyle(state);
        readBuffs(state);

        return state;
    }

    private void readSkillLevels(PlayerState state) {
        state.setAttackLevel(client.getRealSkillLevel(Skill.ATTACK));
        state.setStrengthLevel(client.getRealSkillLevel(Skill.STRENGTH));
        state.setDefenceLevel(client.getRealSkillLevel(Skill.DEFENCE));
        state.setRangedLevel(client.getRealSkillLevel(Skill.RANGED));
        state.setMagicLevel(client.getRealSkillLevel(Skill.MAGIC));
        state.setPrayerLevel(client.getRealSkillLevel(Skill.PRAYER));
        state.setHitpointsLevel(client.getRealSkillLevel(Skill.HITPOINTS));

        state.setCurrentHitpoints(client.getBoostedSkillLevel(Skill.HITPOINTS));

        state.setAttackBoost(client.getBoostedSkillLevel(Skill.ATTACK) - state.getAttackLevel());
        state.setStrengthBoost(client.getBoostedSkillLevel(Skill.STRENGTH) - state.getStrengthLevel());
        state.setDefenceBoost(client.getBoostedSkillLevel(Skill.DEFENCE) - state.getDefenceLevel());
        state.setRangedBoost(client.getBoostedSkillLevel(Skill.RANGED) - state.getRangedLevel());
        state.setMagicBoost(client.getBoostedSkillLevel(Skill.MAGIC) - state.getMagicLevel());
    }

    private void readEquipment(PlayerState state) {
        EquipmentStats stats = new EquipmentStats();
        int[] itemIds = new int[14];
        String[] itemNames = new String[14];
        Arrays.fill(itemIds, -1);

        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null) {
            state.setEquipmentStats(stats);
            state.setEquippedItemIds(itemIds);
            state.setEquippedItemNames(itemNames);
            state.setRawEquipmentLoadout(equipmentPreparation.loadoutFromIds(itemIds));
            return;
        }

        Item[] items = equipment.getItems();
        for (int slot = 0; slot < items.length && slot < 14; slot++) {
            Item item = items[slot];
            if (item == null || item.getId() == -1) {
                itemIds[slot] = -1;
                itemNames[slot] = null;
                continue;
            }

            int itemId = item.getId();
            itemIds[slot] = itemId;

            ItemComposition itemComp = itemManager.getItemComposition(itemId);
            if (itemComp != null) {
                itemNames[slot] = itemComp.getName();
            }

            ItemStats itemStats = itemManager.getItemStats(itemId);
            if (itemStats != null) {
                ItemEquipmentStats eq = itemStats.getEquipment();
                if (eq != null) {
                    stats.addStabAttack(eq.getAstab());
                    stats.addSlashAttack(eq.getAslash());
                    stats.addCrushAttack(eq.getAcrush());
                    stats.addMagicAttack(eq.getAmagic());
                    stats.addRangedAttack(eq.getArange());

                    stats.addMeleeStrength(eq.getStr());
                    stats.addRangedStrength(eq.getRstr());
                    // Preserve the old RuneLite adapter's integer fallback. Calculation
                    // preparation replaces these bonuses with the pinned equipment model.
                    stats.addMagicDamage((int) eq.getMdmg());

                    stats.addStabDefence(eq.getDstab());
                    stats.addSlashDefence(eq.getDslash());
                    stats.addCrushDefence(eq.getDcrush());
                    stats.addMagicDefence(eq.getDmagic());
                    stats.addRangedDefence(eq.getDrange());

                    stats.addPrayerBonus(eq.getPrayer());

                    if (slot == EquipmentSlot.WEAPON.getIndex()) {
                        state.setWeaponSpeed(eq.getAspeed());
                    }
                }
            }
        }

        state.setEquipmentStats(stats);
        state.setEquippedItemIds(itemIds);
        state.setEquippedItemNames(itemNames);
        state.setRawEquipmentLoadout(equipmentPreparation.loadoutFromIds(itemIds));
    }

    public EquipmentResult prepareEquipment(PlayerState state, int monsterId) {
        com.dpscalc.equipment.EquipmentLoadout loadout = state.getRawEquipmentLoadout();
        if (loadout == null) loadout = equipmentPreparation.loadoutFromIds(state.getEquippedItemIds());
        return equipmentPreparation.prepare(state, loadout, state.getEquippedItemNames(),
            EquipmentPreparationFacade.context(state, monsterId));
    }

    private void readPrayers(PlayerState state) {
        Set<Prayer> active = EnumSet.noneOf(Prayer.class);

        for (Prayer prayer : Prayer.values()) {
            net.runelite.api.Prayer rlPrayer = prayer.getRunelitePrayer();
            if (rlPrayer != null && client.isPrayerActive(rlPrayer)) {
                active.add(prayer);
            }
        }

        state.setActivePrayers(active);
    }

    private void readCombatStyle(PlayerState state) {
        int attackStyleIndex = client.getVarpValue(VarPlayer.ATTACK_STYLE);
        int weaponType = client.getVarbitValue(Varbits.EQUIPPED_WEAPON_TYPE);
        int castingMode = client.getVarbitValue(Varbits.DEFENSIVE_CASTING_MODE);

        CombatStyle style = determineCombatStyle(weaponType, attackStyleIndex, castingMode);
        state.setCombatStyle(style);
    }

    private void readBuffs(PlayerState state) {
        int book=client.getVarbitValue(net.runelite.api.gameval.VarbitID.SPELLBOOK);
        String[] books={"standard","ancient","lunar","arceuus"};
        state.setSpellbook(book>=0&&book<books.length?books[book]:null);
        state.setKandarinDiary(client.getVarbitValue(net.runelite.api.gameval.VarbitID.KANDARIN_DIARY_HARD_COMPLETE)>0);
        int wildernessLevel = client.getVarbitValue(WILDERNESS_VARBIT);
        state.setInWilderness(wildernessLevel > 0);

        // Soulreaper axe soul stacks (0-5). The reference models this as the
        // player.buffs.soulreaperStacks input; DpsCalculator already grants the
        // +6% per-stack strength/accuracy exactly like PlayerVsNPCCalc. The setter
        // clamps to 0-5, so the raw varp value is safe to pass through.
        state.setSoulreaperStacks(client.getVarpValue(net.runelite.api.gameval.VarPlayerID.SOULREAPER_STACKS));
    }

    private CombatStyle determineCombatStyle(int weaponType, int attackStyle, int castingMode) {
        switch (weaponType) {
            case 0: // Unarmed
                return getUnarmedStyle(attackStyle);
            case 1: // Axe
                return getAxeStyle(attackStyle);
            case 2: // Blunt
                return getBluntStyle(attackStyle);
            case 3: // Bow
                return getBowStyle(attackStyle);
            case 4: // Claw
                return getClawStyle(attackStyle);
            case 5: // Crossbow
                return getCrossbowStyle(attackStyle);
            case 6: // Salamander
                return getSalamanderStyle(attackStyle);
            case 7: // Chinchompa
                return getChinchompaStyle(attackStyle);
            case 8: // Gun (Dwarven multicannon)
                return CombatStyle.RANGED_ACCURATE;
            case 9: // Slash sword
                return getSlashSwordStyle(attackStyle);
            case 10: // 2h sword
                return getTwoHandedSwordStyle(attackStyle);
            case 11: // Pickaxe
                return getPickaxeStyle(attackStyle);
            case 12: // Polearm
                return getPolearmStyle(attackStyle);
            case 13: // Polestaff
                return getPolestaffStyle(attackStyle);
            case 14: // Scythe
                return getScytheStyle(attackStyle);
            case 15: // Spear
                return getSpearStyle(attackStyle);
            case 16: // Spiked
                return getSpikedStyle(attackStyle);
            case 17: // Stab sword
                return getStabSwordStyle(attackStyle);
            case 18: // Staff
                return getStaffStyle(attackStyle, castingMode);
            case 19: // Thrown
                return getThrownStyle(attackStyle);
            case 20: // Whip
                return getWhipStyle(attackStyle);
            case 21: // Blade staff (Ivandis flail)
                return getBladeStaffStyle(attackStyle);
            case 22: // Two-handed staff (trident, etc.)
                return getPoweredStaffStyle(attackStyle);
            case 23: // Partisan
                return getPartisanStyle(attackStyle);
            case 24: // Banner
                return getBannerStyle(attackStyle);
            case 25: // Bladed staff
                return getBladedStaffStyle(attackStyle);
            case 26: // Bludgeon
                return getBludgeonStyle(attackStyle);
            case 27: // Bulwark
                return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 28: // Dual sai
                return getSaiStyle(attackStyle);
            default:
                return CombatStyle.UNARMED_PUNCH;
        }
    }

    private CombatStyle getUnarmedStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.UNARMED_PUNCH;
            case 1: return new CombatStyle("Kick", AttackType.CRUSH, "Aggressive", 0, 3, 0, 0, 0);
            case 2: return new CombatStyle("Block", AttackType.CRUSH, "Defensive", 0, 0, 3, 0, 0);
            default: return CombatStyle.UNARMED_PUNCH;
        }
    }

    private CombatStyle getAxeStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_SLASH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 2: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 3: return CombatStyle.MELEE_DEFENSIVE_SLASH;
            default: return CombatStyle.MELEE_ACCURATE_SLASH;
        }
    }

    private CombatStyle getBluntStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_CRUSH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 2: return CombatStyle.MELEE_DEFENSIVE_CRUSH;
            default: return CombatStyle.MELEE_ACCURATE_CRUSH;
        }
    }

    private CombatStyle getBowStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.RANGED_ACCURATE;
            case 1: return CombatStyle.RANGED_RAPID;
            case 2: return CombatStyle.RANGED_LONGRANGE;
            default: return CombatStyle.RANGED_ACCURATE;
        }
    }

    private CombatStyle getClawStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_SLASH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 2: return CombatStyle.MELEE_CONTROLLED_STAB;
            case 3: return CombatStyle.MELEE_DEFENSIVE_SLASH;
            default: return CombatStyle.MELEE_ACCURATE_SLASH;
        }
    }

    private CombatStyle getCrossbowStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.RANGED_ACCURATE;
            case 1: return CombatStyle.RANGED_RAPID;
            case 2: return CombatStyle.RANGED_LONGRANGE;
            default: return CombatStyle.RANGED_ACCURATE;
        }
    }

    private CombatStyle getSalamanderStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 1: return CombatStyle.RANGED_RAPID;
            case 2: return CombatStyle.MAGIC_AUTOCAST;
            default: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
        }
    }

    private CombatStyle getChinchompaStyle(int style) {
        switch (style) {
            case 0: return new CombatStyle("Short fuse", AttackType.RANGED_STANDARD, "Accurate", 0, 0, 0, 3, 0);
            case 1: return new CombatStyle("Medium fuse", AttackType.RANGED_STANDARD, "Rapid", 0, 0, 0, 0, 0);
            case 2: return new CombatStyle("Long fuse", AttackType.RANGED_STANDARD, "Longrange", 0, 0, 3, 0, 0);
            default: return CombatStyle.RANGED_ACCURATE;
        }
    }

    private CombatStyle getSlashSwordStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_SLASH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 2: return CombatStyle.MELEE_CONTROLLED_STAB;
            case 3: return CombatStyle.MELEE_DEFENSIVE_SLASH;
            default: return CombatStyle.MELEE_ACCURATE_SLASH;
        }
    }

    private CombatStyle getTwoHandedSwordStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_SLASH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 2: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 3: return CombatStyle.MELEE_DEFENSIVE_SLASH;
            default: return CombatStyle.MELEE_ACCURATE_SLASH;
        }
    }

    private CombatStyle getPickaxeStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_STAB;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_STAB;
            case 2: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 3: return CombatStyle.MELEE_DEFENSIVE_STAB;
            default: return CombatStyle.MELEE_ACCURATE_STAB;
        }
    }

    private CombatStyle getPolearmStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_CONTROLLED_STAB;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 2: return CombatStyle.MELEE_DEFENSIVE_STAB;
            default: return CombatStyle.MELEE_CONTROLLED_STAB;
        }
    }

    private CombatStyle getPolestaffStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_CRUSH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 2: return CombatStyle.MELEE_DEFENSIVE_CRUSH;
            default: return CombatStyle.MELEE_ACCURATE_CRUSH;
        }
    }

    private CombatStyle getScytheStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_SLASH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 2: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 3: return CombatStyle.MELEE_DEFENSIVE_SLASH;
            default: return CombatStyle.MELEE_ACCURATE_SLASH;
        }
    }

    private CombatStyle getSpearStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_CONTROLLED_STAB;
            case 1: return CombatStyle.MELEE_CONTROLLED_SLASH;
            case 2: return new CombatStyle("Crush", AttackType.CRUSH, "Controlled", 1, 1, 1, 0, 0);
            case 3: return CombatStyle.MELEE_DEFENSIVE_STAB;
            default: return CombatStyle.MELEE_CONTROLLED_STAB;
        }
    }

    private CombatStyle getSpikedStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_CRUSH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 2: return CombatStyle.MELEE_DEFENSIVE_CRUSH;
            default: return CombatStyle.MELEE_ACCURATE_CRUSH;
        }
    }

    private CombatStyle getStabSwordStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_STAB;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_STAB;
            case 2: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 3: return CombatStyle.MELEE_DEFENSIVE_STAB;
            default: return CombatStyle.MELEE_ACCURATE_STAB;
        }
    }

    private CombatStyle getStaffStyle(int style, int castingMode) {
        if (castingMode == 1) {
            return CombatStyle.MAGIC_DEFENSIVE_AUTOCAST;
        }
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_CRUSH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 2: return CombatStyle.MELEE_DEFENSIVE_CRUSH;
            case 3: return CombatStyle.MAGIC_AUTOCAST;
            default: return CombatStyle.MELEE_ACCURATE_CRUSH;
        }
    }

    private CombatStyle getThrownStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.RANGED_ACCURATE;
            case 1: return CombatStyle.RANGED_RAPID;
            case 2: return CombatStyle.RANGED_LONGRANGE;
            default: return CombatStyle.RANGED_ACCURATE;
        }
    }

    private CombatStyle getWhipStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_SLASH;
            case 1: return CombatStyle.MELEE_CONTROLLED_SLASH;
            case 2: return CombatStyle.MELEE_DEFENSIVE_SLASH;
            default: return CombatStyle.MELEE_ACCURATE_SLASH;
        }
    }

    private CombatStyle getBladeStaffStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_CRUSH;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 2: return CombatStyle.MELEE_DEFENSIVE_CRUSH;
            default: return CombatStyle.MELEE_ACCURATE_CRUSH;
        }
    }

    private CombatStyle getPoweredStaffStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MAGIC_ACCURATE;
            case 1: return CombatStyle.MAGIC_LONGRANGE;
            default: return CombatStyle.MAGIC_ACCURATE;
        }
    }

    private CombatStyle getPartisanStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_STAB;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_STAB;
            case 2: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 3: return CombatStyle.MELEE_DEFENSIVE_STAB;
            default: return CombatStyle.MELEE_ACCURATE_STAB;
        }
    }

    private CombatStyle getBannerStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_STAB;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 2: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            case 3: return CombatStyle.MELEE_DEFENSIVE_STAB;
            default: return CombatStyle.MELEE_ACCURATE_STAB;
        }
    }

    private CombatStyle getBladedStaffStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_STAB;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_SLASH;
            case 2: return CombatStyle.MELEE_DEFENSIVE_CRUSH;
            default: return CombatStyle.MELEE_ACCURATE_STAB;
        }
    }

    private CombatStyle getBludgeonStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
            default: return CombatStyle.MELEE_AGGRESSIVE_CRUSH;
        }
    }

    private CombatStyle getSaiStyle(int style) {
        switch (style) {
            case 0: return CombatStyle.MELEE_ACCURATE_STAB;
            case 1: return CombatStyle.MELEE_AGGRESSIVE_STAB;
            case 2: return CombatStyle.MELEE_DEFENSIVE_STAB;
            default: return CombatStyle.MELEE_ACCURATE_STAB;
        }
    }

}
