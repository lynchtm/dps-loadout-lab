/* SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Tommy Lynch. Independently specified game rules: RULES.md.
 */
package com.loadoutlab.calculation;

import com.loadoutlab.engine.*;
import com.loadoutlab.equipment.AmmoApplicability;
import com.loadoutlab.model.*;

import java.util.*;
import java.util.function.ToIntFunction;

/** Pure calculation over an already prepared loadout and target snapshot. */
public final class DpsCalculator {
    private final PlayerState p;
    private final MonsterStats target;
    private final boolean special;
    private final List<String> details = new ArrayList<>();

    public DpsCalculator(PlayerState p, MonsterStats target) {
        this(p, target, false);
    }

    public DpsCalculator(PlayerState p, MonsterStats target, boolean special) {
        this.p = p;
        this.target = target;
        this.special = special;
    }

    private static int scale(int n, int numerator, int denominator) {
        return (int) Math.floor((double) n * numerator / denominator);
    }

    private static int roll(int level, int bonus) {
        return (int)
                Math.min(Integer.MAX_VALUE, Math.max(0L, (long) level * Math.max(0, bonus + 64)));
    }

    private static int damage(int level, int bonus) {
        return Math.max(0, (int) (((long) level * (bonus + 64) + 320) / 640));
    }

    private int prayer(ToIntFunction<Prayer> field) {
        return p.getActivePrayers().stream().mapToInt(field).max().orElse(0);
    }

    private String weapon() {
        return Objects.toString(p.getWeaponName(), "").toLowerCase(Locale.ROOT);
    }

    private boolean wears(String fragment) {
        return p.isWearingItemContaining(fragment);
    }

    private int pieces(String... fragments) {
        int n = 0;
        for (String part : fragments) if (wears(part)) n++;
        return n;
    }

    private boolean set(String family) {
        return Arrays.stream(p.getEquippedItemNames())
                        .filter(s -> s != null && s.startsWith(family + "'s"))
                        .count()
                >= 4;
    }

    private boolean voidSet(String helm) {
        return wears("Void knight gloves")
                && wears(helm)
                && (wears("Void knight top") || wears("Elite void top"))
                && (wears("Void knight robe") || wears("Elite void robe"));
    }

    private double neckBonus(AttackType type) {
        String neck =
                Objects.toString(p.getEquippedItemNames()[2], "")
                        .toLowerCase(Locale.ROOT)
                        .replace(" ", "");
        if (neck.contains("avarice")
                && target.getName().toLowerCase(Locale.ROOT).startsWith("revenant"))
            return p.isForinthrySurgeActive() ? .35 : .20;
        if (!target.hasAttribute(MonsterAttribute.UNDEAD)
                || target.getName().equals("Ba-Ba")
                || !neck.startsWith("salveamulet")) return 0;
        if (type.isMagic()
                && weapon().contains("dragon hunter wand")
                && target.hasAttribute(MonsterAttribute.DRAGON)) return 0;
        boolean imbued = neck.contains("(i)") || neck.contains("(ei)"),
                enchanted = neck.contains("(e)") || neck.contains("(ei)");
        if (!type.isMelee() && !imbued) return 0;
        return enchanted ? .20 : type.isMagic() ? .15 : 1.0 / 6;
    }

    private double slayerBonus(AttackType type) {
        String head = Objects.toString(p.getEquippedItemNames()[0], "").toLowerCase(Locale.ROOT);
        boolean mask = head.contains("slayer helmet") || head.contains("black mask");
        if (!p.isOnSlayerTask() || !mask || !type.isMelee() && !head.contains("(i)")) return 0;
        if (Objects.toString(target.getVersion(), "").equalsIgnoreCase("Awakened")) return 0;
        return type.isMelee() ? 1.0 / 6 : .15;
    }

    private double taskBonus(AttackType type) {
        return Math.max(neckBonus(type), slayerBonus(type));
    }

    public DpsResult calculate() {
        Objects.requireNonNull(p);
        Objects.requireNonNull(target);
        Objects.requireNonNull(p.getEquipmentStats());
        AttackType type = p.getCombatStyle().getAttackType();
        EquipmentStats e = p.getEquipmentStats();
        String w = weapon();
        if (type.isRanged() && p.getAmmoApplicability() == AmmoApplicability.INVALID)
            throw new IllegalArgumentException("Incompatible or missing ammunition");
        if (p.getWeaponSpeed() < 1) throw new IllegalArgumentException("Invalid attack interval");
        if (target.hasAttribute(MonsterAttribute.LEAFY)
                && type.isMelee()
                && !w.contains("leaf-bladed"))
            throw new IllegalArgumentException(
                    "This target requires a leaf-bladed weapon for melee");
        if (target.hasAttribute(MonsterAttribute.VAMPYRE_3)
                && !w.contains("blisterwood")
                && !w.contains("ivandis"))
            throw new IllegalArgumentException(
                    "This vampyre requires an Ivandis or blisterwood weapon");
        if (target.hasAttribute(MonsterAttribute.LEAFY)
                && type.isRanged()
                && !Objects.toString(p.getAmmoName(), "")
                        .toLowerCase(Locale.ROOT)
                        .contains("broad"))
            throw new IllegalArgumentException("This target requires broad ammunition");
        if (target.hasAttribute(MonsterAttribute.LEAFY)
                && type.isMagic()
                && !"Magic Dart".equals(p.getSpellName()))
            throw new IllegalArgumentException("This target requires Magic Dart");
        int attack, max;
        double interval = p.getWeaponSpeed(), dot = 0;
        double task = taskBonus(type);
        int stance =
                type.isMelee()
                        ? p.getCombatStyle().getAttackBonus()
                        : type.isMagic()
                                ? p.getCombatStyle().getMagicBonus()
                                : p.getCombatStyle().getRangedBonus();
        if (type.isMelee()) {
            int effectiveAttack =
                    scale(p.getBoostedAttack(), 100 + prayer(Prayer::getAttackBonus), 100)
                            + stance
                            + 8;
            int strengthPrayer = prayer(Prayer::getStrengthBonus);
            if (w.contains("soulreaper") && !special)
                strengthPrayer += 6 * Math.min(5, p.getSoulreaperStacks());
            int effectiveStrength =
                    scale(p.getBoostedStrength(), 100 + strengthPrayer, 100)
                            + p.getCombatStyle().getStrengthBonus()
                            + 8;
            if (voidSet("Void melee helm")) {
                effectiveAttack = scale(effectiveAttack, 11, 10);
                effectiveStrength = scale(effectiveStrength, 11, 10);
            }
            attack = roll(effectiveAttack, e.getAttackBonusForType(type));
            max = damage(effectiveStrength, e.getMeleeStrength());
            attack = (int) Math.floor(attack * (1 + task));
            max = (int) Math.floor(max * (1 + task));
            if (w.contains("dragon hunter lance") && target.hasAttribute(MonsterAttribute.DRAGON)) {
                attack = scale(attack, 6, 5);
                max = scale(max, 6, 5);
            }
            if ((w.contains("arclight") || w.contains("emberlight"))
                    && target.hasAttribute(MonsterAttribute.DEMON)) {
                double susceptibility =
                        target.getName().equals("Duke Sucellus")
                                ? .7
                                : target.getInputs().getDemonbaneVulnerability() / 100.0;
                attack = (int) Math.floor(attack * (1 + .7 * susceptibility));
                max = (int) Math.floor(max * (1 + .7 * susceptibility));
            }
            if ((w.equals("silverlight") || w.equals("darklight"))
                    && target.hasAttribute(MonsterAttribute.DEMON)) {
                attack = scale(attack, 16, 10);
                max = scale(max, 16, 10);
            }
            if (type == AttackType.CRUSH) {
                int bonus =
                        (wears("Inquisitor's great helm") ? 5 : 0)
                                + (wears("Inquisitor's hauberk") ? 10 : 0)
                                + (wears("Inquisitor's plateskirt") ? 10 : 0);
                attack = scale(attack, 1000 + bonus, 1000);
                max = scale(max, 1000 + bonus, 1000);
            }
            if (w.contains("leaf-bladed battleaxe") && target.hasAttribute(MonsterAttribute.LEAFY))
                max = scale(max, 47, 40);
            if (w.contains("keris") && target.hasAttribute(MonsterAttribute.KALPHITE)) {
                max = scale(max, 4, 3);
                if (w.contains("breaching")) attack = scale(attack, 4, 3);
            }
            if (w.contains("viggora") || w.contains("ursine")) {
                if (p.isInWilderness()) {
                    attack = scale(attack, 3, 2);
                    max = scale(max, 3, 2);
                }
            }
            if (w.contains("blisterwood flail")
                    && target.getAttributes().stream().anyMatch(MonsterAttribute::isVampyre)) {
                attack = scale(attack, 21, 20);
                max = scale(max, 5, 4);
            }
            if (w.contains("ivandis flail")
                    && target.getAttributes().stream().anyMatch(MonsterAttribute::isVampyre)) {
                attack = scale(attack, 21, 20);
                max = scale(max, 6, 5);
            }
            if (set("Dharok"))
                max =
                        (int)
                                Math.floor(
                                        max
                                                * (1
                                                        + Math.max(
                                                                        0,
                                                                        p.getHitpointsLevel()
                                                                                - p
                                                                                        .getCurrentHitpoints())
                                                                * p.getHitpointsLevel()
                                                                / 10000.0));
            if (pieces("Obsidian helmet", "Obsidian platebody", "Obsidian platelegs") == 3
                    && (w.contains("toktz") || w.contains("tzhaar"))) {
                attack = scale(attack, 11, 10);
                max = scale(max, 11, 10);
            }
            if (wears("Berserker necklace") && (w.contains("toktz") || w.contains("tzhaar")))
                max = scale(max, 6, 5);
            details.add(
                    "Melee effective attack "
                            + effectiveAttack
                            + ", effective strength "
                            + effectiveStrength);
        } else if (type.isRanged()) {
            int level =
                    scale(p.getBoostedRanged(), 100 + prayer(Prayer::getRangedBonus), 100)
                            + stance
                            + 8;
            int strengthPrayer =
                    p.getActivePrayers().contains(Prayer.RIGOUR)
                            ? 23
                            : prayer(Prayer::getRangedBonus);
            int strength = scale(p.getBoostedRanged(), 100 + strengthPrayer, 100) + stance + 8;
            if (voidSet("Void ranger helm")) {
                level = scale(level, 11, 10);
                strength =
                        scale(
                                strength,
                                wears("Elite void top") && wears("Elite void robe") ? 1125 : 1100,
                                1000);
            }
            attack = roll(level, e.getRangedAttack());
            max = damage(strength, e.getRangedStrength());
            attack = (int) Math.floor(attack * (1 + task));
            max = (int) Math.floor(max * (1 + task));
            if (w.contains("crystal bow") || w.contains("bow of faerdhinen")) {
                double bonus =
                        (wears("Crystal helm") ? .03 : 0)
                                + (wears("Crystal body") ? .075 : 0)
                                + (wears("Crystal legs") ? .045 : 0);
                attack = (int) Math.floor(attack * (1 + 2 * bonus));
                max = (int) Math.floor(max * (1 + bonus));
            }
            if (w.contains("dragon hunter crossbow")
                    && target.hasAttribute(MonsterAttribute.DRAGON)) {
                attack = scale(attack, 13, 10);
                max = scale(max, 5, 4);
            }
            if ((w.contains("craw's") || w.contains("webweaver")) && p.isInWilderness()) {
                attack = scale(attack, 3, 2);
                max = scale(max, 3, 2);
            }
            if (w.contains("scorching bow") && target.hasAttribute(MonsterAttribute.DEMON)) {
                attack = scale(attack, 13, 10);
                max = scale(max, 13, 10);
            }
            if (w.contains("twisted bow")) {
                int cap = target.hasAttribute(MonsterAttribute.XERICIAN) ? 350 : 250;
                double magic =
                        Math.min(cap, Math.max(target.getMagicLevel(), target.getOffensiveMagic()));
                double accuracy =
                        Math.max(
                                0,
                                Math.min(
                                        140,
                                        140
                                                + (3 * magic - 10) / 100
                                                - Math.pow(.3 * magic - 100, 2) / 100));
                double power =
                        Math.max(
                                0,
                                Math.min(
                                        250,
                                        250
                                                + (3 * magic - 14) / 100
                                                - Math.pow(.3 * magic - 140, 2) / 100));
                attack = (int) Math.floor(attack * accuracy / 100);
                max = (int) Math.floor(max * power / 100);
            }
            details.add(
                    "Ranged effective accuracy level " + level + ", strength level " + strength);
        } else {
            int effective = scale(p.getBoostedMagic(), 100 + prayer(Prayer::getMagicBonus), 100);
            if (voidSet("Void mage helm")) effective = scale(effective, 145, 100);
            effective += stance + 8;
            boolean powered = p.getSpellName() == null || p.getSpellName().isEmpty();
            int bonus = e.getMagicAttack();
            double magicDamage = e.getMagicDamage() / 1000.0;
            if (powered && w.contains("tumeken")) {
                int factor = target.isToaMonster() ? 4 : 3;
                bonus *= factor;
                magicDamage = Math.min(1, magicDamage * factor);
            }
            attack = roll(effective, bonus);
            attack = (int) Math.floor(attack * (1 + task));
            max = powered ? poweredMax(w) : elementalBase();
            if (!powered) {
                interval =
                        w.contains("harmonised nightmare")
                                        && "standard".equalsIgnoreCase(p.getSpellbook())
                                ? 4
                                : 5;
                String god =
                        "Claws of Guthix".equals(p.getSpellName())
                                ? "guthix"
                                : "Flames of Zamorak".equals(p.getSpellName())
                                        ? "zamorak"
                                        : "Saradomin Strike".equals(p.getSpellName())
                                                ? "saradomin"
                                                : "";
                String cape =
                        Objects.toString(p.getEquippedItemNames()[1], "").toLowerCase(Locale.ROOT);
                if (p.isChargeSpellActive()
                        && !god.isEmpty()
                        && cape.contains(god)
                        && cape.contains("cape")) max += 10;
                if (wears("Chaos gauntlets") && p.getSpellName().endsWith(" Bolt")) max += 3;
                if ("ancient".equalsIgnoreCase(p.getSpellbook()))
                    magicDamage +=
                            .03 * pieces("Virtus mask", "Virtus robe top", "Virtus robe bottom");
                if (w.contains("smoke") && "standard".equalsIgnoreCase(p.getSpellbook())) {
                    magicDamage += .1;
                    attack = scale(attack, 11, 10);
                }
            }
            magicDamage += prayer(Prayer::getMagicDamageBonus) / 100.0 + neckBonus(type);
            if (voidSet("Void mage helm") && wears("Elite void top") && wears("Elite void robe"))
                magicDamage += .05;
            int base = max;
            max = (int) Math.floor(max * (1 + magicDamage));
            if (!powered
                    && target.getWeaknessElement() != null
                    && target.getWeaknessElement()
                            .getJsonName()
                            .equalsIgnoreCase(p.getSpellElement())) {
                max += scale(base, target.getWeaknessSeverity(), 100);
                attack += scale(roll(effective, bonus), target.getWeaknessSeverity(), 100);
            }
            if (neckBonus(type) == 0) max = (int) Math.floor(max * (1 + slayerBonus(type)));
            if (w.contains("dragon hunter wand") && target.hasAttribute(MonsterAttribute.DRAGON)) {
                attack = scale(attack, 7, 4);
                max = scale(max, 7, 5);
            }
            if ((w.contains("thammaron") || w.contains("accursed")) && p.isInWilderness()) {
                attack = scale(attack, 3, 2);
                max = scale(max, 3, 2);
            }
            if (!powered && p.getSpellName().toLowerCase(Locale.ROOT).endsWith("demonbane")) {
                if (!target.hasAttribute(MonsterAttribute.DEMON))
                    throw new IllegalArgumentException("Demonbane spells require a demon target");
                double increase = p.isMarkOfDarknessActive() ? .4 : .2;
                if (w.contains("purging")) increase *= 2;
                attack = (int) Math.floor(attack * (1 + increase));
            }
            if (!powered
                    && "standard".equalsIgnoreCase(p.getSpellbook())
                    && p.getSpellElement() != null
                    && wears("Tome of " + p.getSpellElement())
                    && !Objects.toString(p.getEquippedItemNames()[5], "").contains("(empty)"))
                max = scale(max, 11, 10);
            details.add(
                    "Magic effective accuracy level "
                            + effective
                            + ", base spell hit "
                            + base
                            + ", gear/prayer damage "
                            + magicDamage);
        }
        AttackType defenceType = type;
        if (special) {
            if (w.contains("godsword")) {
                attack = scale(attack, 2, 1);
                max = scale(max, 11, 10);
                defenceType = AttackType.SLASH;
                if (w.contains("armadyl")) max = scale(max, 5, 4);
                else if (w.contains("bandos")) max = scale(max, 11, 10);
            } else if (w.contains("dragon dagger")) {
                attack = scale(attack, 23, 20);
                max = scale(max, 23, 20);
                defenceType = AttackType.SLASH;
            } else if (w.contains("dragon mace")) {
                attack = scale(attack, 5, 4);
                max = scale(max, 3, 2);
                defenceType = AttackType.CRUSH;
            } else if (w.contains("dragon warhammer")) {
                max = scale(max, 3, 2);
                defenceType = AttackType.CRUSH;
            } else if (w.contains("elder maul")) {
                attack = scale(attack, 5, 4);
                defenceType = AttackType.CRUSH;
            } else if (w.contains("abyssal whip") || w.contains("abyssal tentacle")) {
                attack = scale(attack, 5, 4);
                defenceType = AttackType.SLASH;
            } else if (w.contains("fang")) {
                attack = scale(attack, 3, 2);
            } else if (w.contains("soulreaper")) {
                attack = scale(attack, 100 + 12 * p.getSoulreaperStacks(), 100);
                max = scale(max, 100 + 6 * p.getSoulreaperStacks(), 100);
            } else if (w.contains("magic shortbow")) {
                attack = scale(attack, 10, 7);
            } else if (w.contains("dragon halberd") || w.contains("crystal halberd")) {
                attack = scale(attack, 11, 10);
                max = scale(max, 11, 10);
                defenceType = AttackType.SLASH;
            } else if (w.contains("dark bow")) {
                max =
                        scale(
                                max,
                                p.getAmmoName() != null
                                                && p.getAmmoName()
                                                        .toLowerCase(Locale.ROOT)
                                                        .contains("dragon")
                                        ? 15
                                        : 13,
                                10);
            } else if (w.contains("voidwaker")) {
                defenceType = AttackType.MAGIC;
            } else if (w.contains("dragon claws") || w.contains("burning claws"))
                throw new IllegalArgumentException(
                        "Correlated claw special attacks require an independently verified rule"
                                + " implementation");
            else if (w.contains("toxic blowpipe")) {
                attack = scale(attack, 2, 1);
                max = scale(max, 3, 2);
            } else if (w.contains("arclight")
                    || w.contains("emberlight")
                    || w.equals("darklight")) {
                defenceType = AttackType.STAB;
            } else if (w.contains("zaryte crossbow")) {
                attack = scale(attack, 2, 1);
            } else
                throw new IllegalArgumentException(
                        "Special attack not supported for " + p.getWeaponName());
        }
        if (type.isRanged()) {
            String category = Objects.toString(p.getWeaponCategory(), "").toLowerCase(Locale.ROOT);
            if (category.equals("crossbow") || category.equals("blaster"))
                defenceType = AttackType.RANGED_HEAVY;
            else if (category.equals("chinchompas")) defenceType = AttackType.RANGED_HEAVY;
            else if (category.equals("thrown") || w.contains("blowpipe"))
                defenceType = AttackType.RANGED_LIGHT;
            else defenceType = AttackType.RANGED_STANDARD;
        }
        int defence =
                roll(
                        (defenceType.isMagic()
                                        ? target.getDefenceLevelForMagic()
                                        : target.getDefenceLevel())
                                + 9,
                        target.getDefenceForStyle(defenceType.getKey()));
        double accuracy = RollContest.successProbability(attack, defence);
        if (w.contains("fang") && type == AttackType.STAB) {
            accuracy =
                    target.isToaMonster()
                            ? 1 - Math.pow(1 - accuracy, 2)
                            : doubleAttackAgainstOneDefence(attack, defence);
        }
        if (type.isMagic() && wears("Brimstone ring"))
            accuracy =
                    .75 * accuracy
                            + .25 * RollContest.successProbability(attack, scale(defence, 9, 10));
        int
                minimum =
                        w.contains("soulreaper") && special
                                ? scale(max, 6 * p.getSoulreaperStacks(), 100)
                                : 0,
                ceiling = max;
        if (w.contains("fang")) {
            minimum = scale(max, 15, 100);
            if (!special) ceiling = max - minimum;
        }
        if (type.isMagic()
                && p.isUsingSunfireRunes()
                && "fire".equalsIgnoreCase(p.getSpellElement())) minimum = scale(max, 1, 10);
        DamagePmf successful = successful(minimum, ceiling);
        if (type.isMagic()
                && p.isMarkOfDarknessActive()
                && Objects.toString(p.getSpellName(), "")
                        .toLowerCase(Locale.ROOT)
                        .endsWith("demonbane")) {
            int percent = w.contains("purging") ? 50 : 25;
            int susceptibility = target.getInputs().getDemonbaneVulnerability();
            successful =
                    successful.map(d -> d + scale(scale(d, percent, 100), susceptibility, 100));
        }
        if (type.isMagic()
                && w.contains("sanguinesti")
                && (p.getSpellName() == null || p.getSpellName().isEmpty()))
            successful = successful.map(d -> d + 8).mixture(.2, successful);
        DamagePmf distribution = successful.mixture(accuracy, new DamagePmf(1));
        if (w.contains("keris") && target.hasAttribute(MonsterAttribute.KALPHITE))
            distribution = distribution.map(d -> d * 3).mixture(1.0 / 51, distribution);
        if (set("Verac") && type.isMelee())
            distribution = successful(1, max + 1).mixture(.25, distribution);
        if (set("Karil") && wears("Amulet of the damned") && type.isRanged())
            distribution = distribution.map(d -> d + d / 2).mixture(.25, distribution);
        if (set("Ahrim") && wears("Amulet of the damned") && type.isMagic())
            distribution = distribution.map(d -> scale(d, 13, 10)).mixture(.25, distribution);
        if (type.isRanged()) distribution = enchantedBolts(distribution, accuracy, max, w);
        distribution = mitigate(distribution, type, w);
        if (special) {
            if (w.contains("dragon dagger") || w.contains("magic shortbow"))
                distribution = distribution.independentSum(distribution);
            if ((w.contains("dragon halberd") || w.contains("crystal halberd"))
                    && target.getSize() > 1)
                distribution =
                        distribution.independentSum(
                                mitigate(
                                        successful.mixture(
                                                RollContest.successProbability(
                                                        scale(attack, 3, 4), defence),
                                                new DamagePmf(1)),
                                        type,
                                        w));
            if (w.contains("dark bow")) {
                int min =
                        p.getAmmoName() != null
                                        && p.getAmmoName()
                                                .toLowerCase(Locale.ROOT)
                                                .contains("dragon")
                                ? 8
                                : 5;
                DamagePmf hit =
                        successful
                                .mixture(accuracy, new DamagePmf(1))
                                .map(d -> Math.min(48, Math.max(min, d)));
                hit = mitigate(hit, type, w);
                distribution = hit.independentSum(hit);
            }
            if (w.contains("voidwaker")) {
                accuracy = 1;
                distribution =
                        mitigate(
                                DamagePmf.singleHit(1, max / 2, max + max / 2),
                                AttackType.MAGIC,
                                w);
            }
        }
        if (w.contains("scythe") && type.isMelee()) {
            if (target.getSize() >= 2)
                distribution =
                        distribution.independentSum(
                                mitigate(
                                        successful(0, max / 2).mixture(accuracy, new DamagePmf(1)),
                                        type,
                                        w));
            if (target.getSize() >= 3)
                distribution =
                        distribution.independentSum(
                                mitigate(
                                        successful(0, max / 4).mixture(accuracy, new DamagePmf(1)),
                                        type,
                                        w));
        }
        if (!special && w.contains("dark bow"))
            distribution = distribution.independentSum(distribution);
        details.add(
                "Resolved maximum rolls: attack "
                        + attack
                        + ", defence "
                        + defence
                        + "; accuracy "
                        + accuracy);
        details.add(
                "Damage probabilities retain misses, successful-zero conversion, independent hits"
                        + " and applicable procs.");
        DpsResult result =
                new DpsResult(distribution, accuracy, interval, attack, defence, dot, details);
        result.setMonsterHp(
                target.getInputs().getMonsterCurrentHp() > 0
                        ? target.getInputs().getMonsterCurrentHp()
                        : target.getHitpoints());
        return result;
    }

    private DamagePmf mitigate(DamagePmf distribution, AttackType type, String w) {
        if (target.getName().equals("Corporeal Beast")
                && !type.isMagic()
                && !(type == AttackType.STAB
                        && (w.contains("fang")
                                || Objects.toString(p.getWeaponCategory(), "")
                                        .equalsIgnoreCase("spear")
                                || Objects.toString(p.getWeaponCategory(), "")
                                        .equalsIgnoreCase("polearm"))))
            distribution = distribution.map(d -> d / 2);
        if (target.getFlatArmour() > 0)
            distribution =
                    distribution.map(d -> d == 0 ? 0 : Math.max(1, d - target.getFlatArmour()));
        if (target.getName().equals("Zulrah")) distribution = zulrahCap(distribution);
        return distribution;
    }

    private int elementalBase() {
        String name = Objects.toString(p.getSpellName(), "");
        if (name.equals("Magic Dart"))
            return 10
                    + p.getBoostedMagic()
                            / (weapon().contains("slayer's staff (e)") && p.isOnSlayerTask()
                                    ? 5
                                    : 10);
        String[] tiers = {"Strike", "Bolt", "Blast", "Wave", "Surge"};
        int[][] levels = {
            {1, 5, 9, 13}, {17, 23, 29, 35}, {41, 47, 53, 59}, {62, 65, 70, 75}, {81, 85, 90, 95}
        };
        int[][] damage = {
            {2, 4, 6, 8}, {9, 10, 11, 12}, {13, 14, 15, 16}, {17, 18, 19, 20}, {21, 22, 23, 24}
        };
        for (int tier = 0; tier < tiers.length; tier++)
            if (name.endsWith(" " + tiers[tier]) && p.getSpellElement() != null) {
                int max = 0;
                for (int rank = 0; rank < 4; rank++)
                    if (p.getBoostedMagic() >= levels[tier][rank]) max = damage[tier][rank];
                if (max == 0)
                    throw new IllegalArgumentException(
                            "Magic level is too low for this spell tier");
                return max;
            }
        return p.getSpellMaxHit();
    }

    private int poweredMax(String w) {
        int level = p.getBoostedMagic();
        if (w.contains("tumeken")) return Math.max(1, level / 3 + 1);
        if (w.contains("sanguinesti")) return Math.max(6, level / 3);
        if (w.contains("trident of the swamp")) return Math.max(1, level / 3 - 2);
        if (w.contains("trident of the seas") || w.contains("trident of the swamp"))
            return Math.max(1, level / 3 - 5);
        if (w.contains("warped sceptre")) return Math.max(1, (8 * level + 96) / 37);
        if (w.contains("bone staff")) {
            if (!target.hasAttribute(MonsterAttribute.RAT))
                throw new IllegalArgumentException("Bone staff only attacks rats");
            return Math.max(1, level / 3 + 5);
        }
        if (w.contains("thammaron") || w.contains("accursed")) return Math.max(1, level / 3 - 6);
        throw new IllegalArgumentException(
                "Choose a spell; this weapon has no supported built-in spell");
    }

    private static DamagePmf successful(int min, int max) {
        if (max <= 0) return new DamagePmf(1);
        return DamagePmf.singleHit(1, Math.max(0, min), Math.max(min, max))
                .map(d -> Math.max(1, d));
    }

    static double doubleAttackAgainstOneDefence(int a, int d) {
        double m = Math.min(a, d) + 1.0;
        double squares = m * (m + 1) * (2 * m + 1) / 6;
        double total = (a + 1.0) * (a + 1.0);
        return Math.max(
                0, Math.min(1, 1 - (squares + Math.max(0, d - a) * total) / ((d + 1.0) * total)));
    }

    private DamagePmf enchantedBolts(DamagePmf normal, double accuracy, int max, String w) {
        String ammo = Objects.toString(p.getAmmoName(), "").toLowerCase(Locale.ROOT);
        if (!w.contains("crossbow") || !ammo.contains("(e)")) return normal;
        double chance =
                ammo.contains("ruby")
                        ? .06
                        : ammo.contains("diamond")
                                ? .10
                                : ammo.contains("onyx")
                                        ? .11
                                        : ammo.contains("dragonstone")
                                                ? .06
                                                : ammo.contains("opal") ? .05 : 0;
        if (chance == 0) return normal;
        if (p.isKandarinDiary()) chance *= 1.1;
        boolean zaryte = w.contains("zaryte");
        DamagePmf proc;
        if (ammo.contains("ruby")) {
            int hp =
                    target.getInputs().getMonsterCurrentHp() > 0
                            ? target.getInputs().getMonsterCurrentHp()
                            : target.getHitpoints();
            int hit = Math.min(zaryte ? 110 : 100, scale(hp, zaryte ? 22 : 20, 100));
            proc = DamagePmf.singleHit(1, hit, hit);
        } else if (ammo.contains("diamond"))
            proc = successful(0, scale(max, zaryte ? 126 : 115, 100));
        else if (ammo.contains("onyx")) {
            if (target.hasAttribute(MonsterAttribute.UNDEAD)) return normal;
            proc =
                    successful(0, scale(max, zaryte ? 132 : 120, 100))
                            .mixture(accuracy, new DamagePmf(1));
        } else if (ammo.contains("dragonstone")) {
            if (target.hasAttribute(MonsterAttribute.DRAGON)
                    || target.hasAttribute(MonsterAttribute.FIERY)) return normal;
            int extra = scale(p.getBoostedRanged(), zaryte ? 22 : 20, 100);
            proc = successful(0, max).map(d -> d + extra).mixture(accuracy, new DamagePmf(1));
        } else {
            int extra = p.getBoostedRanged() / (zaryte ? 9 : 10);
            proc = successful(0, max).map(d -> d + extra).mixture(accuracy, new DamagePmf(1));
        }
        if (special && zaryte) {
            // Ruby/diamond can proc before the accuracy contest. Otherwise Evoke
            // converts a successful contest into the effect; a failed contest is a miss.
            if (ammo.contains("ruby") || ammo.contains("diamond"))
                return proc.mixture(chance + (1 - chance) * accuracy, new DamagePmf(1));
            // These effects already include their one accuracy contest above.
            return proc;
        }
        return proc.mixture(chance, normal);
    }

    private static DamagePmf zulrahCap(DamagePmf input) {
        double[] bins = new double[51];
        for (int d = 0; d <= input.maximum(); d++) {
            double probability = input.probabilityAt(d);
            if (d <= 50) bins[d] += probability;
            else for (int hit = 45; hit <= 50; hit++) bins[hit] += probability / 6;
        }
        return new DamagePmf(bins);
    }
}
