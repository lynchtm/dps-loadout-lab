package com.dpscalc.equipment;

import com.dpscalc.state.AttackType;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

@Singleton
public final class EquipmentPreparationFacade {
    public static final String REFERENCE_SHA = "b6bc098dc0d742b2b763375d2e78e1b611a22070";
    public static final String DOMAIN_DIGEST = "070a34ce7f6267be1ae8c84cfdd3757100f5a027898ba34937eaba16536ea951";
    private static final int SLOT_COUNT = 14;

    private final EquipmentDomainCatalog catalog;
    private final EquipmentCalculator calculator;
    public Map<Integer, EquipmentCatalogItem> getItems() { return catalog.getItems(); }
    public EquipmentCatalogItem getItemFacts(int id) { return id <= 0 ? null : catalog.requireItem(catalog.canonicalize(EquipmentItem.raw(id, Map.of()))); }

    @Inject
    public EquipmentPreparationFacade() {
        this(loadCatalog());
    }

    EquipmentPreparationFacade(EquipmentDomainCatalog catalog) {
        this.catalog = catalog;
        this.calculator = new EquipmentCalculator(catalog);
    }

    public EquipmentResult prepare(PlayerState state, EquipmentLoadout rawLoadout,
                                   String[] displayNames, EquipmentContext context) {
        EquipmentResult result = calculator.calculate(rawLoadout, context);
        apply(state, result, displayNames, context);
        return result;
    }

    public EquipmentResult prepare(PlayerState state, int[] itemIds, String[] displayNames,
                                   EquipmentContext context) {
        return prepare(state, loadoutFromIds(itemIds), displayNames, context);
    }

    public EquipmentLoadout loadoutFromIds(int[] itemIds) {
        if (itemIds == null || itemIds.length != SLOT_COUNT) {
            throw new IllegalArgumentException("itemIds must be a 14-element array");
        }
        Map<EquipmentSlot, EquipmentItem> items = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            int id = itemIds[stateIndex(slot)];
            if (id != -1 && id != 0) items.put(slot, EquipmentItem.raw(id, Map.of()));
        }
        return EquipmentLoadout.of(items);
    }

    public static EquipmentContext context(PlayerState state, int monsterId) {
        CombatStyle style = state.getCombatStyle();
        String stance = style == null ? "" : style.getStance();
        return EquipmentContext.of(monsterId, EquipmentCombatStyle.of(type(style), stance),
            state.getSpellbook() == null ? "" : state.getSpellbook());
    }

    private void apply(PlayerState state, EquipmentResult result, String[] displayNames,
                       EquipmentContext context) {
        int[] ids = new int[SLOT_COUNT];
        String[] names = new String[SLOT_COUNT];
        String[] versions = new String[SLOT_COUNT];
        String[] categories = new String[SLOT_COUNT];
        Arrays.fill(ids, -1);
        for (Map.Entry<EquipmentSlot, EquipmentItem> entry : result.getCanonicalLoadout().asMap().entrySet()) {
            int index = stateIndex(entry.getKey());
            EquipmentCatalogItem facts = catalog.getItem(entry.getValue());
            ids[index] = entry.getValue().getOriginalId();
            names[index] = displayName(displayNames, index, facts);
            versions[index] = facts.getVersion();
            categories[index] = facts.getCategory();
        }
        state.setEquippedItemIds(ids);
        state.setEquippedItemNames(names);
        state.setEquippedItemVersions(versions);
        state.setEquippedItemCategories(categories);
        state.setEquipmentStats(toMutableStats(result.getStats()));
        state.setAmmoApplicability(result.getAmmoApplicability());
        int speed = result.getAttackSpeed();
        if ("Rapid".equals(context.getCombatStyle().getStance())) speed += 1;
        state.setWeaponSpeed(speed);
    }

    private static String displayName(String[] displayNames, int index, EquipmentCatalogItem facts) {
        if (displayNames != null && displayNames.length == SLOT_COUNT && displayNames[index] != null) {
            return displayNames[index];
        }
        return facts.getName();
    }

    private static EquipmentStats toMutableStats(EquipmentStatTotals totals) {
        EquipmentStats stats = new EquipmentStats();
        stats.setStabAttack(totals.getStabAttack()); stats.setSlashAttack(totals.getSlashAttack());
        stats.setCrushAttack(totals.getCrushAttack()); stats.setMagicAttack(totals.getMagicAttack());
        stats.setRangedAttack(totals.getRangedAttack()); stats.setMeleeStrength(totals.getMeleeStrength());
        stats.setRangedStrength(totals.getRangedStrength()); stats.setMagicDamage(totals.getMagicDamage());
        stats.setPrayerBonus(totals.getPrayerBonus()); stats.setStabDefence(totals.getStabDefence());
        stats.setSlashDefence(totals.getSlashDefence()); stats.setCrushDefence(totals.getCrushDefence());
        stats.setMagicDefence(totals.getMagicDefence()); stats.setRangedDefence(totals.getRangedDefence());
        return stats;
    }

    private static int stateIndex(EquipmentSlot slot) {
        switch (slot) {
            case HEAD: return com.dpscalc.state.EquipmentSlot.HEAD.getIndex();
            case CAPE: return com.dpscalc.state.EquipmentSlot.CAPE.getIndex();
            case NECK: return com.dpscalc.state.EquipmentSlot.AMULET.getIndex();
            case WEAPON: return com.dpscalc.state.EquipmentSlot.WEAPON.getIndex();
            case BODY: return com.dpscalc.state.EquipmentSlot.BODY.getIndex();
            case SHIELD: return com.dpscalc.state.EquipmentSlot.SHIELD.getIndex();
            case LEGS: return com.dpscalc.state.EquipmentSlot.LEGS.getIndex();
            case HANDS: return com.dpscalc.state.EquipmentSlot.GLOVES.getIndex();
            case FEET: return com.dpscalc.state.EquipmentSlot.BOOTS.getIndex();
            case RING: return com.dpscalc.state.EquipmentSlot.RING.getIndex();
            case AMMO: return com.dpscalc.state.EquipmentSlot.AMMO.getIndex();
            default: throw new AssertionError(slot);
        }
    }

    private static String type(CombatStyle style) {
        if (style == null) return "melee";
        AttackType attackType = style.getAttackType();
        if (attackType == AttackType.STAB) return "stab";
        if (attackType == AttackType.SLASH) return "slash";
        if (attackType == AttackType.CRUSH) return "crush";
        if (attackType.isRanged()) return "ranged";
        if (attackType.isMagic()) return "magic";
        return "melee";
    }

    private static EquipmentDomainCatalog loadCatalog() {
        try (InputStream stream = EquipmentPreparationFacade.class.getResourceAsStream("/equipment-domain.json")) {
            return EquipmentDomainCatalog.load(stream, REFERENCE_SHA, DOMAIN_DIGEST);
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
