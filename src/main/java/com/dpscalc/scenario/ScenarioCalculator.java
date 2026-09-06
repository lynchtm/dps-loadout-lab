package com.dpscalc.scenario;

import com.dpscalc.calc.*;
import com.dpscalc.data.*;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.state.*;

import java.util.*;

public final class ScenarioCalculator {
    private final EquipmentPreparationFacade equipment;

    public ScenarioCalculator(EquipmentPreparationFacade equipment) {
        this.equipment = equipment;
    }

    public static final class Result {
        public String name, error, breakdown;
        public DpsResult normal, special;
        public EquipmentStats equipmentStats;
        public com.dpscalc.equipment.AmmoApplicability ammoApplicability;
        public double[] histogram;
        public Double ttk, ttkVariance, rotationDps, damageTaken, prayerSeconds;
        public Double groupDps;
        public int encounterTargets = 1, targetsHit = 1;

        public double rankingDps() {
            return groupDps == null ? normal.getDps() : groupDps;
        }

        public final List<String> warnings = new ArrayList<>();
        public final List<String> limitations = new ArrayList<>();

        public Result named(String name) {
            Result copy = new Result();
            copy.name = name;
            copy.error = error;
            copy.breakdown = breakdown;
            copy.normal = normal;
            copy.special = special;
            copy.equipmentStats = equipmentStats;
            copy.ammoApplicability = ammoApplicability;
            copy.histogram = histogram;
            copy.ttk = ttk;
            copy.ttkVariance = ttkVariance;
            copy.rotationDps = rotationDps;
            copy.damageTaken = damageTaken;
            copy.prayerSeconds = prayerSeconds;
            copy.groupDps = groupDps;
            copy.encounterTargets = encounterTargets;
            copy.targetsHit = targetsHit;
            copy.warnings.addAll(warnings);
            copy.limitations.addAll(limitations);
            return copy;
        }
    }

    public Result calculate(Scenario.Loadout loadout, MonsterStats target) {
        return calculate(loadout, target, new Encounter(), false);
    }

    public Result calculate(Scenario.Loadout loadout, MonsterStats target, Encounter encounter) {
        return calculate(loadout, target, encounter, false);
    }

    public Result score(Scenario.Loadout loadout, MonsterStats target) {
        return calculate(loadout, target, new Encounter(), true);
    }

    public Result score(Scenario.Loadout loadout, MonsterStats target, Encounter encounter) {
        return calculate(loadout, target, encounter, true);
    }

    private Result calculate(
            Scenario.Loadout loadout, MonsterStats target, Encounter encounter, boolean scoreOnly) {
        Result out = new Result();
        out.name = loadout.name;
        try {
            encounter.validate();
            Scenario.validate(loadout);
            if (target != null) Scenario.validate(target);
            PlayerState p = Scenario.copy(loadout.player);
            PotionSelection.apply(p, loadout);
            MonsterStats m = target == null ? null : MonsterScaling.scale(target);
            if (!loadout.manualEquipmentStats) {
                com.dpscalc.equipment.EquipmentLoadout raw =
                        equipment.loadoutFromIds(p.getEquippedItemIds());
                java.util.Map<
                                com.dpscalc.equipment.EquipmentSlot,
                                com.dpscalc.equipment.EquipmentItem>
                        slots = new java.util.EnumMap<>(com.dpscalc.equipment.EquipmentSlot.class);
                slots.putAll(raw.asMap());
                if (loadout.blowpipeDartId > 0 && p.getWeaponId() > 0)
                    slots.put(
                            com.dpscalc.equipment.EquipmentSlot.WEAPON,
                            com.dpscalc.equipment.EquipmentItem.raw(
                                    p.getWeaponId(),
                                    Map.of(
                                            "blowpipeDartId",
                                            com.dpscalc.equipment.ItemVariable.ofNumber(
                                                    loadout.blowpipeDartId))));
                equipment.prepare(
                        p,
                        com.dpscalc.equipment.EquipmentLoadout.of(slots),
                        null,
                        EquipmentPreparationFacade.context(p, m == null ? 0 : m.getId()));
                com.dpscalc.equipment.EquipmentCatalogItem weapon =
                        equipment.getItemFacts(p.getWeaponId());
                if (weapon != null && weapon.isTwoHanded() && p.getEquippedItemIds()[5] > 0)
                    throw new IllegalArgumentException(
                            "Two-handed weapon cannot be combined with a shield");
                if (p.isWearingAny("Toxic blowpipe", "Rosewood blowpipe")
                        && loadout.blowpipeDartId <= 0)
                    throw new IllegalArgumentException(
                            "Select a blowpipe dart ID in Conditions; loaded darts are unavailable"
                                    + " from live state");
            } else
                out.warnings.add(
                        "Manual equipment totals: item names and charge/version values must match"
                                + " the equipment being modelled.");
            out.equipmentStats = p.getEquipmentStats();
            out.ammoApplicability = p.getAmmoApplicability();
            if (m == null) throw new IllegalArgumentException("Select or create a target");
            out.normal = new DpsCalculator(p, m).calculate();
            if (encounter.grouped) {
                out.encounterTargets = encounter.targets;
                out.targetsHit = encounter.targetsHit(p);
                out.groupDps = out.normal.getDps() * out.targetsHit;
            }
            out.limitations.addAll(CoverageWarnings.forState(p, m));
            if (scoreOnly) {
                if (p.getAmmoApplicability() == com.dpscalc.equipment.AmmoApplicability.INVALID)
                    throw new IllegalArgumentException("Incompatible ammunition");
                if (!Double.isFinite(out.normal.getDps()))
                    throw new IllegalArgumentException("Invalid DPS");
                return out;
            }
            out.warnings.addAll(out.limitations);
            if (encounter.grouped) out.warnings.add(encounter.assumption());
            if (!Double.isFinite(out.normal.getDps()))
                throw new IllegalArgumentException("Invalid attack interval or result");
            out.normal.setMonsterHp(
                    m.getInputs().getMonsterCurrentHp() > 0
                            ? m.getInputs().getMonsterCurrentHp()
                            : m.getHitpoints());
            out.histogram = DistributionAnalysis.histogram(out.normal.getAttackDistribution());
            if (out.normal.getExpectedDotDamage() == 0) {
                try {
                    double[] moments =
                            DistributionAnalysis.moments(out.histogram, out.normal.getMonsterHp());
                    double seconds = out.normal.getExpectedAttackSpeed() * 0.6;
                    out.ttk = moments[0] * seconds;
                    out.ttkVariance = moments[1] * seconds * seconds;
                } catch (IllegalArgumentException ex) {
                    out.warnings.add(ex.getMessage());
                }
            } else out.warnings.add("Kill-time distribution unavailable for delayed damage.");
            out.warnings.add(
                    "Kill times assume a fixed damage distribution and attack interval;"
                            + " HP-dependent procs, phase changes and variable attack delays need a"
                            + " dynamic model.");
            if (loadout.specialAttack) {
                out.special = new DpsCalculator(p, m, true).calculate();
                int maxEnergy =
                        loadout.specialEnergy
                                + (loadout.rotationTicks / (p.isWearing("Lightbearer") ? 25 : 50))
                                        * 10;
                if (loadout.specials * loadout.specialCost > maxEnergy)
                    out.warnings.add("Rotation requests more special energy than available.");
                else {
                    double ticks = loadout.specials * out.special.getExpectedAttackSpeed();
                    if (ticks > loadout.rotationTicks)
                        out.warnings.add("Special attacks exceed the rotation duration.");
                    else
                        out.rotationDps =
                                (loadout.specials * out.special.getExpectedDamage()
                                                + Math.floor(
                                                                (loadout.rotationTicks - ticks)
                                                                        / out.normal
                                                                                .getExpectedAttackSpeed())
                                                        * out.normal.getExpectedDamage())
                                        / (loadout.rotationTicks * 0.6);
                }
                out.warnings.add(
                        "Special model: verify weapon coverage and energy cost. Rotation uses fixed"
                                + " target stats and average damage; it does not apply sequential"
                                + " defence drains or enforce energy availability at each attack.");
                if (p.isWearingAny(
                        "Dragon claws",
                        "Burning claws",
                        "Dark bow",
                        "Voidwaker",
                        "Dragon halberd",
                        "Crystal halberd",
                        "Dawnbringer")) {
                    out.special = null;
                    out.rotationDps = null;
                    out.warnings.add(
                            "Special result unavailable: this weapon's complete correlated hits or"
                                    + " minimum-hit distribution is not implemented. Normal attacks"
                                    + " remain available.");
                }
            }
            if (!"Unavailable".equals(loadout.incomingStyle)) {
                out.damageTaken = incoming(p, loadout);
                out.warnings.add(
                        "Incoming damage uses the manually configured single attack; boss patterns,"
                                + " healing and prayer penetration are not inferred.");
            }
            if (loadout.prayerDrainPerMinute > 0)
                out.prayerSeconds =
                        p.getPrayerLevel()
                                * 60.0
                                * (1 + p.getEquipmentStats().getPrayerBonus() / 30.0)
                                / loadout.prayerDrainPerMinute;
            out.warnings.add(
                    "Reference engine/data: Wiki b6bc098d (2026-07-09). Current Wiki parity and"
                            + " seasonal mechanics are not established.");
            if (p.getCombatStyle().getAttackType().isMagic() && p.getSpellName() == null)
                out.warnings.add(
                        "Live spell detection unavailable: select a spell or confirm powered-staff"
                                + " behavior.");
            out.breakdown = explain(p, m, out);
            if (encounter.grouped)
                out.breakdown +=
                        "\n\n"
                                + encounter.assumption()
                                + "\nEstimated group DPS: "
                                + out.normal.getDps()
                                + " × "
                                + out.targetsHit
                                + " = "
                                + out.groupDps;
        } catch (RuntimeException ex) {
            out.error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        }
        return out;
    }

    public static void applyBoost(PlayerState p, String boost, int seconds, boolean divine) {
        int decay = divine ? 0 : seconds / 60;
        if (boost == null || boost.equals("None")) return;
        switch (boost) {
            case "Super combat":
                p.setAttackBoost(Math.max(0, 5 + p.getAttackLevel() * 15 / 100 - decay));
                p.setStrengthBoost(Math.max(0, 5 + p.getStrengthLevel() * 15 / 100 - decay));
                p.setDefenceBoost(Math.max(0, 5 + p.getDefenceLevel() * 15 / 100 - decay));
                break;
            case "Ranging":
                p.setRangedBoost(Math.max(0, 4 + p.getRangedLevel() / 10 - decay));
                break;
            case "Imbued heart":
                p.setMagicBoost(Math.max(0, 1 + p.getMagicLevel() / 10 - decay));
                break;
            case "Saturated heart":
                p.setMagicBoost(4 + p.getMagicLevel() / 10);
                break;
            case "Forgotten brew":
                p.setMagicBoost(Math.max(0, 3 + p.getMagicLevel() * 8 / 100 - decay));
                p.setAttackBoost(-Math.max(0, 2 + (p.getAttackLevel() + 9) / 10 - decay));
                p.setStrengthBoost(-Math.max(0, 2 + (p.getStrengthLevel() + 9) / 10 - decay));
                p.setDefenceBoost(-Math.max(0, 2 + (p.getDefenceLevel() + 9) / 10 - decay));
                break;
            case "Overload":
                p.setAttackBoost(5 + p.getAttackLevel() * 13 / 100);
                p.setStrengthBoost(5 + p.getStrengthLevel() * 13 / 100);
                p.setDefenceBoost(5 + p.getDefenceLevel() * 13 / 100);
                p.setRangedBoost(5 + p.getRangedLevel() * 13 / 100);
                p.setMagicBoost(5 + p.getMagicLevel() * 13 / 100);
                break;
            case "Overload (+)":
            case "Smelling salts":
                int flat = boost.equals("Overload (+)") ? 6 : 11;
                p.setAttackBoost(flat + p.getAttackLevel() * 16 / 100);
                p.setStrengthBoost(flat + p.getStrengthLevel() * 16 / 100);
                p.setDefenceBoost(flat + p.getDefenceLevel() * 16 / 100);
                p.setRangedBoost(flat + p.getRangedLevel() * 16 / 100);
                p.setMagicBoost(flat + p.getMagicLevel() * 16 / 100);
                break;
            case "Attack":
                p.setAttackBoost(Math.max(0, 3 + p.getAttackLevel() / 10 - decay));
                break;
            case "Strength":
                p.setStrengthBoost(Math.max(0, 3 + p.getStrengthLevel() / 10 - decay));
                break;
            case "Defence":
                p.setDefenceBoost(Math.max(0, 3 + p.getDefenceLevel() / 10 - decay));
                break;
            case "Magic":
                p.setMagicBoost(Math.max(0, 4 - decay));
                break;
            case "Super attack":
                p.setAttackBoost(Math.max(0, 5 + p.getAttackLevel() * 15 / 100 - decay));
                break;
            case "Super strength":
                p.setStrengthBoost(Math.max(0, 5 + p.getStrengthLevel() * 15 / 100 - decay));
                break;
            case "Super defence":
                p.setDefenceBoost(Math.max(0, 5 + p.getDefenceLevel() * 15 / 100 - decay));
                break;
            case "Super ranging":
                p.setRangedBoost(Math.max(0, 5 + p.getRangedLevel() * 15 / 100 - decay));
                break;
            case "Super magic":
                p.setMagicBoost(Math.max(0, 5 + p.getMagicLevel() * 15 / 100 - decay));
                break;
            default:
                throw new IllegalArgumentException("Unknown boost preset; use manual boosts");
        }
    }

    private static double incoming(PlayerState p, Scenario.Loadout l) {
        EquipmentStats e = p.getEquipmentStats();
        int bonus;
        switch (l.incomingStyle) {
            case "Stab":
                bonus = e.getStabDefence();
                break;
            case "Slash":
                bonus = e.getSlashDefence();
                break;
            case "Crush":
                bonus = e.getCrushDefence();
                break;
            case "Ranged":
                bonus = e.getRangedDefence();
                break;
            case "Magic":
                bonus = e.getMagicDefence();
                break;
            default:
                throw new IllegalArgumentException("Unknown incoming style");
        }
        int prayer =
                p.getActivePrayers().stream().mapToInt(Prayer::getDefenceBonus).max().orElse(0);
        int level = p.getBoostedDefence() * (100 + prayer) / 100;
        if (l.incomingStyle.equals("Magic")) level = p.getBoostedMagic() * 7 / 10 + level * 3 / 10;
        String stance = p.getCombatStyle().getStance();
        level +=
                "Defensive".equals(stance) || "Longrange".equals(stance)
                        ? 3
                        : "Controlled".equals(stance) ? 1 : 0;
        double accuracy =
                BaseCalc.getNormalAccuracyRoll(l.incomingAttackRoll, (level + 8) * (bonus + 64));
        double expected = 0;
        for (int hit = 0; hit <= l.incomingMaxHit; hit++) {
            double damage = hit;
            if (p.isWearing("Elysian spirit shield"))
                damage = 0.3 * hit + 0.7 * Math.max(0, hit - Math.max(1, hit / 4));
            expected += damage / (l.incomingMaxHit + 1);
        }
        return l.protectionPrayer ? 0 : expected * accuracy / (l.incomingSpeed * 0.6);
    }

    private static String explain(PlayerState p, MonsterStats m, Result r) {
        DpsResult d = r.normal;
        return "Levels (base + boost)\nAttack "
                + p.getAttackLevel()
                + " + "
                + p.getAttackBoost()
                + "\nStrength "
                + p.getStrengthLevel()
                + " + "
                + p.getStrengthBoost()
                + "\nRanged "
                + p.getRangedLevel()
                + " + "
                + p.getRangedBoost()
                + "\nMagic "
                + p.getMagicLevel()
                + " + "
                + p.getMagicBoost()
                + "\nStyle: "
                + p.getCombatStyle()
                + "\nPrayers: "
                + p.getActivePrayers()
                + "\nEquipment: "
                + Arrays.toString(p.getEquippedItemNames())
                + "\nTarget attributes: "
                + m.getAttributes()
                + "\nSlayer task: "
                + p.isOnSlayerTask()
                + "\nAttack roll: "
                + d.getAttackRoll()
                + "\nDefence roll: "
                + d.getDefenceRoll()
                + "\nHit chance after weapon mechanics: "
                + d.getAccuracy()
                + "\nDirect damage: sum(damage × probability) = "
                + d.getExpectedDirectDamage()
                + "\nDelayed damage: "
                + d.getExpectedDotDamage()
                + "\nExpected interval: "
                + d.getExpectedAttackSpeed()
                + " ticks\nDPS = (direct + delayed) / (ticks × 0.6) = "
                + d.getDps()
                + "\n"
                + "TTK uses E[0]=0; E[h]=(1+sum(p[d]×E[max(0,h-d)]))/(1-p[0]). Includes overkill"
                + " and misses.\n\n"
                + "Limits\n"
                + String.join("\n", r.warnings);
    }
}
