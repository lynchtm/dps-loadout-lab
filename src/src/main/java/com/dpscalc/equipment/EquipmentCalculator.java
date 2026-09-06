package com.dpscalc.equipment;

import com.dpscalc.data.MonsterConstants;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EquipmentCalculator {
    private static final Set<String> BLOWPIPES = Set.of("Toxic blowpipe", "Rosewood blowpipe");
    private static final Set<String> VIRTUS_PIECES = Set.of("Virtus mask", "Virtus robe top", "Virtus robe bottom");

    private final EquipmentDomainCatalog catalog;

    public EquipmentCalculator(EquipmentDomainCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public EquipmentResult calculate(EquipmentLoadout loadout, EquipmentContext context) {
        EquipmentLoadout canonical = catalog.canonicalize(Objects.requireNonNull(loadout, "loadout"));
        Objects.requireNonNull(context, "context");
        EquipmentItem weaponItem = canonical.get(EquipmentSlot.WEAPON);
        EquipmentItem ammoItem = canonical.get(EquipmentSlot.AMMO);
        EquipmentCatalogItem weapon = catalog.getItem(weaponItem);
        EquipmentCatalogItem cape = catalog.getItem(canonical.get(EquipmentSlot.CAPE));
        AmmoApplicability applicability = catalog.evaluate(weaponItem, ammoItem).getApplicability();

        int[] totals = visibleTotals(canonical, applicability).values();
        applyBlowpipeDart(totals, weaponItem, weapon);
        applyShadow(totals, weapon, context);
        applyKerisPenalty(totals, weapon, context);
        applyDinh(totals, weapon);
        applyVirtus(totals, canonical, context);
        applyEliteVoid(totals, canonical);
        applyDizana(totals, cape, applicability);
        int attackSpeed = EquipmentAttackSpeedCalculator.calculate(weapon, context);
        return new EquipmentResult(canonical, new EquipmentStatTotals(totals), attackSpeed, applicability);
    }

    private EquipmentStatTotals visibleTotals(EquipmentLoadout loadout, AmmoApplicability applicability) {
        EquipmentStatTotals totals = EquipmentStatTotals.zero();
        for (Map.Entry<EquipmentSlot, EquipmentItem> entry : loadout.asMap().entrySet()) {
            EquipmentStatTotals piece = catalog.requireItem(entry.getValue()).getStats();
            if (entry.getKey() == EquipmentSlot.AMMO && applicability != AmmoApplicability.INCLUDED) {
                piece = piece.withoutAmmoRangedStats();
            }
            totals = totals.plus(piece);
        }
        return totals;
    }

    private void applyBlowpipeDart(int[] totals, EquipmentItem weaponItem, EquipmentCatalogItem weapon) {
        if (weapon == null || !BLOWPIPES.contains(weapon.getName())) return;
        ItemVariable dartVariable = weaponItem.getItemVariables().get("blowpipeDartId");
        if (dartVariable == null) return;
        if (dartVariable.getKind() != ItemVariable.Kind.NUMBER) {
            throw new EquipmentDomainException("itemVars.blowpipeDartId", "expected integer item ID");
        }
        double value = dartVariable.getNumberValue();
        if (value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new EquipmentDomainException("itemVars.blowpipeDartId", "expected integer item ID");
        }
        EquipmentCatalogItem dart = catalog.requireItem(EquipmentItem.raw((int) value, Map.of()));
        totals[EquipmentStatTotals.RANGED_STRENGTH] += dart.getStats().getRangedStrength();
    }

    private static void applyShadow(int[] totals, EquipmentCatalogItem weapon, EquipmentContext context) {
        if (weapon == null || !"Tumeken's shadow".equals(weapon.getName())
            || "Manual Cast".equals(context.getCombatStyle().getStance())) return;
        int factor = MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(context.getMonsterId()) ? 4 : 3;
        totals[EquipmentStatTotals.MAGIC_DAMAGE] = Math.min(1000, totals[EquipmentStatTotals.MAGIC_DAMAGE] * factor);
        totals[EquipmentStatTotals.MAGIC_ATTACK] *= factor;
    }

    private static void applyKerisPenalty(int[] totals, EquipmentCatalogItem weapon, EquipmentContext context) {
        if (weapon != null && "Keris partisan of amascut".equals(weapon.getName())
            && !MonsterConstants.TOMBS_OF_AMASCUT_MONSTER_IDS.contains(context.getMonsterId())) {
            totals[EquipmentStatTotals.MELEE_STRENGTH] -= 22;
            totals[EquipmentStatTotals.STAB_ATTACK] -= 50;
        }
    }

    private static void applyDinh(int[] totals, EquipmentCatalogItem weapon) {
        if (weapon == null || !("Dinh's bulwark".equals(weapon.getName()) || "Dinh's blazing bulwark".equals(weapon.getName()))) return;
        int defenceSum = totals[EquipmentStatTotals.STAB_DEFENCE] + totals[EquipmentStatTotals.SLASH_DEFENCE]
            + totals[EquipmentStatTotals.CRUSH_DEFENCE] + totals[EquipmentStatTotals.RANGED_DEFENCE];
        totals[EquipmentStatTotals.MELEE_STRENGTH] += Math.max(0, (defenceSum - 800) / 12 - 38);
    }

    private void applyVirtus(int[] totals, EquipmentLoadout loadout, EquipmentContext context) {
        if (!"ancient".equals(context.getSpellbook()) || !isCastStance(context.getCombatStyle().getStance())) return;
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.BODY, EquipmentSlot.LEGS}) {
            EquipmentCatalogItem item = catalog.getItem(loadout.get(slot));
            if (item != null && VIRTUS_PIECES.contains(item.getName())) pieces++;
        }
        totals[EquipmentStatTotals.MAGIC_DAMAGE] += 30 * pieces;
    }

    private void applyEliteVoid(int[] totals, EquipmentLoadout loadout) {
        if (hasName(loadout, EquipmentSlot.HEAD, "Void mage helm")
            && hasName(loadout, EquipmentSlot.BODY, "Elite void top")
            && hasName(loadout, EquipmentSlot.LEGS, "Elite void robe")
            && hasName(loadout, EquipmentSlot.HANDS, "Void knight gloves")) {
            totals[EquipmentStatTotals.MAGIC_DAMAGE] += 50;
        }
    }

    private static void applyDizana(int[] totals, EquipmentCatalogItem cape, AmmoApplicability applicability) {
        if (cape == null || applicability != AmmoApplicability.INCLUDED) return;
        boolean charged = "Dizana's max cape".equals(cape.getName()) || "Blessed dizana's quiver".equals(cape.getName())
            || ("Dizana's quiver".equals(cape.getName()) && "Charged".equals(cape.getVersion()));
        if (charged) {
            totals[EquipmentStatTotals.RANGED_ATTACK] += 10;
            totals[EquipmentStatTotals.RANGED_STRENGTH] += 1;
        }
    }

    private boolean hasName(EquipmentLoadout loadout, EquipmentSlot slot, String name) {
        EquipmentCatalogItem item = catalog.getItem(loadout.get(slot));
        return item != null && name.equals(item.getName());
    }

    private static boolean isCastStance(String stance) {
        return "Autocast".equals(stance) || "Defensive Autocast".equals(stance) || "Manual Cast".equals(stance);
    }
}
