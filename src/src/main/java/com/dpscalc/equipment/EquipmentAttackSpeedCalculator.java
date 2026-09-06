package com.dpscalc.equipment;

import java.util.Set;

final class EquipmentAttackSpeedCalculator {
    private static final int DEFAULT_ATTACK_SPEED = 4;
    private static final Set<String> CAST_STANCES = Set.of("Autocast", "Defensive Autocast", "Manual Cast");
    private static final Set<String> SCURRIUS_WEAPONS = Set.of("Bone mace", "Bone shortbow", "Bone staff");

    private EquipmentAttackSpeedCalculator() {}

    static int calculate(EquipmentCatalogItem weapon, EquipmentContext context) {
        if (weapon != null && weapon.getSpeed() < 0) {
            throw new IllegalArgumentException(weapon.getName() + " cannot be used as a combat weapon");
        }
        int attackSpeed = weapon == null || weapon.getSpeed() == 0 ? DEFAULT_ATTACK_SPEED : weapon.getSpeed();
        String stance = context.getCombatStyle().getStance();
        if ("ranged".equals(context.getCombatStyle().getType()) && "Rapid".equals(stance)) {
            attackSpeed -= 1;
        } else if (CAST_STANCES.contains(stance)) {
            if (weapon != null && "Harmonised nightmare staff".equals(weapon.getName())
                && "standard".equals(context.getSpellbook()) && !"Manual Cast".equals(stance)) {
                attackSpeed = 4;
            } else if (weapon != null && "Twinflame staff".equals(weapon.getName())) {
                attackSpeed = 6;
            } else {
                attackSpeed = 5;
            }
        }
        if (context.getMonsterId() == 7223 && !"Manual Cast".equals(stance)
            && weapon != null && SCURRIUS_WEAPONS.contains(weapon.getName())) {
            attackSpeed = 1;
        }
        return Math.max(attackSpeed, 1);
    }
}
