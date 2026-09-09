package com.dpscalc.scenario;

import com.google.gson.*;
import com.loadoutlab.equipment.*;
import com.loadoutlab.model.*;
import com.loadoutlab.model.MonsterStats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.function.IntConsumer;

/** Searches owned, compatible equipment. Large searches deliberately report best-found. */
public final class BankOptimizer {
    public static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 7, 9, 10, 12, 13};
    public static final String[] SLOT_NAMES = {
        "Head", "Cape", "Neck", "Weapon", "Body", "Shield", "Legs", "Hands", "Feet", "Ring", "Ammo"
    };
    private static final String[] CATALOG_SLOTS = {
        "head", "cape", "neck", "weapon", "body", "shield", "legs", "hands", "feet", "ring", "ammo"
    };
    private static final String[] SETS = {
        "Elite void",
        "Void",
        "Dharok",
        "Verac",
        "Guthan",
        "Torag",
        "Karil",
        "Ahrim",
        "Obsidian",
        "Inquisitor",
        "Justiciar",
        "Crystal",
        "Virtus",
        "Blood moon",
        "Blue moon",
        "Eclipse moon"
    };
    // These owned blessings have no additional equip-level requirement. Acquisition
    // requirements for diary rewards are not equip requirements (see NOTICE.md).
    private static final Set<Integer> BLESSINGS_WITHOUT_LEVEL_REQUIREMENTS =
            Set.of(20220, 20223, 20226, 20229, 20232, 20235, 22941, 22943, 22945, 22947);
    private final EquipmentPreparationFacade equipment;
    private final ScenarioCalculator calculator;
    private final List<OptimizerSpell> spellCatalog = OptimizerSpell.load();
    private final Map<Integer, Map<String, Integer>> requirements = new HashMap<>();

    public BankOptimizer(EquipmentPreparationFacade equipment) {
        this.equipment = equipment;
        calculator = new ScenarioCalculator(equipment);
        try (Reader reader =
                new InputStreamReader(
                        getClass()
                                .getResourceAsStream("/com/loadoutlab/equipment-requirements.json"),
                        StandardCharsets.UTF_8)) {
            for (Map.Entry<String, JsonElement> entry :
                    new JsonParser().parse(reader).getAsJsonObject().entrySet()) {
                Map<String, Integer> levels = new HashMap<>();
                entry.getValue()
                        .getAsJsonObject()
                        .entrySet()
                        .forEach(e -> levels.put(e.getKey(), e.getValue().getAsInt()));
                requirements.put(Integer.parseInt(entry.getKey()), levels);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Equipment requirements unavailable", ex);
        }
    }

    public static String family(Scenario.Loadout loadout) {
        AttackType type = loadout.player.getCombatStyle().getAttackType();
        return type.isMagic() ? "Magic" : type.isRanged() ? "Ranged" : "Melee";
    }

    public static final class Request {
        public final Scenario.Loadout base;
        public final Scenario.Loadout baseline;
        public final MonsterStats target;
        public final OwnedEquipment owned;
        public final Set<Integer> locked;
        public final boolean includeUnverified;
        public final boolean includeKnownLimitations;
        public final String combatType;
        public final Encounter encounter;

        public Request(
                Scenario.Loadout base,
                MonsterStats target,
                OwnedEquipment owned,
                Set<Integer> locked,
                boolean includeUnverified) {
            this(base, target, owned, locked, includeUnverified, family(base));
        }

        public Request(
                Scenario.Loadout base,
                MonsterStats target,
                OwnedEquipment owned,
                Set<Integer> locked,
                boolean includeUnverified,
                String combatType) {
            this(base, target, owned, locked, includeUnverified, combatType, base);
        }

        public Request(
                Scenario.Loadout base,
                MonsterStats target,
                OwnedEquipment owned,
                Set<Integer> locked,
                boolean includeUnverified,
                String combatType,
                Encounter encounter) {
            this(base, target, owned, locked, includeUnverified, combatType, base, encounter);
        }

        public Request(
                Scenario.Loadout base,
                MonsterStats target,
                OwnedEquipment owned,
                Set<Integer> locked,
                boolean includeUnverified,
                String combatType,
                Scenario.Loadout baseline) {
            this(
                    base,
                    target,
                    owned,
                    locked,
                    includeUnverified,
                    combatType,
                    baseline,
                    new Encounter());
        }

        public Request(
                Scenario.Loadout base,
                MonsterStats target,
                OwnedEquipment owned,
                Set<Integer> locked,
                boolean includeUnverified,
                String combatType,
                Scenario.Loadout baseline,
                Encounter encounter) {
            this(
                    base,
                    target,
                    owned,
                    locked,
                    includeUnverified,
                    combatType,
                    baseline,
                    encounter,
                    false);
        }

        public Request(
                Scenario.Loadout base,
                MonsterStats target,
                OwnedEquipment owned,
                Set<Integer> locked,
                boolean includeUnverified,
                String combatType,
                Scenario.Loadout baseline,
                Encounter encounter,
                boolean includeKnownLimitations) {
            this.includeKnownLimitations = includeKnownLimitations;
            encounter.validate();
            this.encounter = encounter.copy();
            if (!Set.of("Melee", "Ranged", "Magic").contains(combatType))
                throw new IllegalArgumentException("Choose Melee, Ranged or Magic");
            this.combatType = combatType;
            this.baseline = baseline.copy();
            this.base = base.copy();
            this.target = target == null ? null : target.copy();
            this.owned = owned;
            this.locked = Set.copyOf(locked);
            this.includeUnverified = includeUnverified;
        }
    }

    public static final class Result {
        public final List<Scenario.Loadout> alternatives = new ArrayList<>();
        public final List<ScenarioCalculator.Result> calculations = new ArrayList<>();
        public final List<String> exclusions = new ArrayList<>();
        public int evaluated;
        public boolean exhaustive;
        public Double baselineDps, baselineGroupDps;
    }

    public Result search(Request request, IntConsumer progress) {
        return search(request, progress, 20000, 15000);
    }

    public Result search(Request request, IntConsumer progress, int budget, long milliseconds) {
        if (request.target == null) throw new IllegalArgumentException("Select a target first");
        if (!request.owned.ready(request.owned.profile))
            throw new IllegalArgumentException("Open your bank to scan equipment first");
        if (request.base.manualEquipmentStats)
            throw new IllegalArgumentException(
                    "Use item-based equipment totals before generating gear");
        Scenario.validate(request.base);
        Scenario.validate(request.target);
        Search search = new Search(request, progress, budget, milliseconds);
        return search.run();
    }

    private final class Search {
        final Request request;
        final IntConsumer progress;
        final int budget;
        final long deadline;
        final Result result = new Result();
        final Map<Integer, List<Integer>> pools = new LinkedHashMap<>();
        final List<Ranked> best = new ArrayList<>();
        final Map<String, Double> scores = new HashMap<>();
        final Map<Integer, Ranked> weaponBest = new LinkedHashMap<>();
        final Scenario.Loadout trial;
        final String family;
        final List<OptimizerSpell> spells = new ArrayList<>();
        boolean limited;

        Search(Request request, IntConsumer progress, int budget, long milliseconds) {
            this.request = request;
            this.progress = progress;
            this.budget = budget;
            deadline = System.nanoTime() + milliseconds * 1_000_000;
            trial = request.base.copy();
            trial.manualEquipmentStats = false;
            family = request.combatType;
            if (family.equals("Magic")) {
                PlayerState boosted = Scenario.copy(request.base.player);
                PotionSelection.apply(boosted, request.base);
                for (OptimizerSpell spell : spellCatalog)
                    if (spell.eligible(boosted.getBoostedMagic(), request.target))
                        spells.add(spell);
            }
        }

        Result run() {
            buildPools();
            long combinations = 1;
            for (List<Integer> pool : pools.values())
                combinations = Math.min(1_000_000, combinations * pool.size());
            result.exhaustive =
                    combinations * 4 * Math.max(1, spells.size()) <= Math.min(budget, 12000);
            if (result.exhaustive) enumerate(0, newIds());
            else {
                // Evaluate every weapon's seeds before refinement, so an early branch
                // cannot consume the whole search budget before later weapons are seen.
                List<int[]> seeds = new ArrayList<>();
                for (int weapon : pools.get(3)) {
                    int[] start = newIds();
                    start[3] = weapon;
                    for (int slot : SLOTS) if (slot != 3) start[slot] = pools.get(slot).get(0);
                    if (twoHanded(weapon) && !request.locked.contains(5)) start[5] = -1;
                    seeds.add(start);
                    int[] current = start.clone();
                    for (int slot : SLOTS)
                        if (slot != 3
                                && pools.get(slot)
                                        .contains(request.base.player.getEquippedItemIds()[slot]))
                            current[slot] = request.base.player.getEquippedItemIds()[slot];
                    if (twoHanded(weapon) && !request.locked.contains(5)) current[5] = -1;
                    seeds.add(current);
                    for (String set : SETS) {
                        int[] bundle = start.clone();
                        boolean found = false;
                        for (int slot : SLOTS)
                            if (slot != 3 && !request.locked.contains(slot))
                                for (int id : pools.get(slot))
                                    if (id > 0 && facts(id).getName().startsWith(set)) {
                                        bundle[slot] = id;
                                        found = true;
                                        break;
                                    }
                        if (found) {
                            if (twoHanded(weapon) && !request.locked.contains(5)) bundle[5] = -1;
                            seeds.add(bundle);
                        }
                    }
                }
                // Wiki examples supplement the general search; they never restrict its item pool.
                for (int[] wiki : request.base.wikiSeeds) {
                    int[] seed = newIds();
                    for (int slot : SLOTS) {
                        int wanted =
                                request.locked.contains(slot)
                                        ? request.base.player.getEquippedItemIds()[slot]
                                        : wiki[slot];
                        seed[slot] =
                                pools.get(slot).contains(wanted) ? wanted : pools.get(slot).get(0);
                    }
                    if (twoHanded(seed[3]) && !request.locked.contains(5)) seed[5] = -1;
                    seeds.add(seed);
                }
                for (int[] seed : seeds) {
                    if (stop()) break;
                    evaluate(seed);
                }
                // Include ammo with each weapon before selecting refinement starts.
                for (int[] seed : seeds) {
                    if (stop()) break;
                    for (int ammo : pools.get(13)) {
                        seed = seed.clone();
                        seed[13] = ammo;
                        evaluate(seed);
                        if (stop()) break;
                    }
                }
                List<int[]> starts = new ArrayList<>();
                for (Ranked ranked : weaponBest.values()) starts.add(ranked.ids.clone());
                for (Ranked ranked : best) starts.add(ranked.ids.clone());
                for (int[] seed : starts) {
                    if (stop()) break;
                    improve(seed);
                }
            }
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            result.exhaustive &= !limited;
            result.evaluated = scores.size();
            if (best.isEmpty())
                throw new IllegalArgumentException(
                        "No usable setup found. Review equipment eligibility, ammunition, Magic"
                                + " level, slot locks and the known formula limitations option.");
            ScenarioCalculator.Result baseline =
                    calculator.score(request.baseline, request.target, request.encounter);
            if (baseline.error == null) {
                result.baselineDps = baseline.normal.getDps();
                result.baselineGroupDps = baseline.groupDps;
            }
            Set<String> identities = new HashSet<>();
            for (Ranked ranked : best) {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                Scenario.Loadout draft = makeDraft(ranked);
                String signature = Arrays.toString(ranked.ids);
                if (!identities.add(signature)) continue;
                ScenarioCalculator.Result full =
                        calculator.calculate(draft, request.target, request.encounter);
                if (full.error != null) continue;
                result.alternatives.add(draft);
                result.calculations.add(full);
                if (result.alternatives.size() == 3) break;
            }
            if (result.alternatives.isEmpty())
                throw new IllegalArgumentException(
                        "Finalist calculation unavailable; review calculation coverage.");
            if (family.equals("Magic"))
                result.exclusions.add(
                        "Magic compares supported autocast spells across spellbooks and powered"
                                + " attacks. Spellbook/quest unlocks and rune supplies require"
                                + " confirmation. Manual casting, Magic Dart and support-spell"
                                + " rotations are not searched.");
            result.exclusions.add(
                    request.includeKnownLimitations
                            ? "Known formula limitations were allowed; flagged suggestions need"
                                    + " review."
                            : "Setups with known formula limitations were excluded.");
            return result;
        }

        void buildPools() {
            for (int slot : SLOTS) pools.put(slot, new ArrayList<>());
            for (int id : request.owned.quantities.keySet()) {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                EquipmentCatalogItem item = facts(id);
                if (item == null) continue;
                int slot = slot(item.getSlot());
                if (slot < 0) continue;
                Map<String, Integer> required =
                        VariantRequirements.resolve(
                                id, request.owned.names.get(id), equipment, requirements);
                if (required == null && BLESSINGS_WITHOUT_LEVEL_REQUIREMENTS.contains(id))
                    required = Collections.emptyMap();
                String exclusion = null;
                if (required == null
                        && !request.owned.equipped.containsKey(id)
                        && !request.includeUnverified) exclusion = "requirements unverified";
                if (required != null)
                    for (Map.Entry<String, Integer> level : required.entrySet()) {
                        String skill =
                                level.getKey().equals("runecraft") ? "runecraft" : level.getKey();
                        Integer actual = request.owned.levels.get(skill);
                        if (actual == null || actual < level.getValue())
                            exclusion = "requires " + level.getValue() + " " + skill;
                    }
                String name =
                        (item.getName() + " " + Objects.toString(item.getVersion(), ""))
                                .toLowerCase(Locale.ROOT);
                if (name.contains("blowpipe") && request.base.blowpipeDartId <= 0)
                    exclusion = "set loaded dart ID in Settings";
                if (name.contains("uncharged")
                        || name.contains("(empty)")
                        || name.contains("inactive")) exclusion = "not charged / active";
                if (slot == 3 && item.getSpeed() <= 0) exclusion = "not a supported combat weapon";
                if (slot == 13 && !family.equals("Ranged") && isProjectile(item))
                    exclusion = "ammunition is not used for " + family.toLowerCase(Locale.ROOT);
                if (exclusion != null) {
                    result.exclusions.add(item.getName() + " (#" + id + "): " + exclusion);
                    continue;
                }
                if (slot == 3) {
                    trial.player.getEquippedItemIds()[3] = id;
                    if (styles().isEmpty()) continue;
                }
                pools.get(slot).add(id);
            }
            for (int slot : SLOTS) {
                List<Integer> pool = pools.get(slot);
                pool.sort(
                        Comparator.<Integer>comparingDouble(id -> heuristic(facts(id)))
                                .reversed()
                                .thenComparing(
                                        Comparator.<Integer>comparingInt(
                                                        id -> facts(id).getStats().getPrayerBonus())
                                                .reversed())
                                .thenComparingInt(id -> id));
                if (request.locked.contains(slot)) {
                    int id = request.base.player.getEquippedItemIds()[slot];
                    if (id > 0 && (!request.owned.owns(id) || !pool.contains(id)))
                        throw new IllegalArgumentException(
                                "Locked "
                                        + SLOT_NAMES[index(slot)]
                                        + " is missing or excluded. Review equipment or unlock the"
                                        + " slot.");
                    pool.clear();
                    pool.add(id > 0 ? id : -1);
                } else pool.add(-1);
            }
            if (request.base.blowpipeDartId > 0
                    && !request.owned.owns(request.base.blowpipeDartId)) {
                // Loaded darts are not represented by a separate container entry.
                result.exclusions.add(
                        "Loaded darts use the manually selected ID; confirm the weapon is loaded"
                                + " with these darts.");
            }
        }

        double heuristic(EquipmentCatalogItem item) {
            EquipmentStatTotals s = item.getStats();
            if (family.equals("Magic")) return s.getMagicAttack() + s.getMagicDamage() * 5;
            if (family.equals("Ranged")) return s.getRangedAttack() + s.getRangedStrength() * 3;
            return Math.max(s.getStabAttack(), Math.max(s.getSlashAttack(), s.getCrushAttack()))
                    + s.getMeleeStrength() * 3;
        }

        int[] newIds() {
            int[] ids = new int[14];
            Arrays.fill(ids, -1);
            return ids;
        }

        void enumerate(int at, int[] ids) {
            if (stop()) return;
            if (at == SLOTS.length) {
                evaluate(ids);
                return;
            }
            int slot = SLOTS[at];
            for (int id : pools.get(slot)) {
                ids[slot] = id;
                enumerate(at + 1, ids);
                if (stop()) return;
            }
        }

        void improve(int[] ids) {
            double score = evaluate(ids);
            for (int pass = 0; pass < 3 && !stop(); pass++) {
                boolean changed = false;
                for (int slot : SLOTS) {
                    if (slot == 3 || request.locked.contains(slot)) continue;
                    int winner = ids[slot];
                    for (int id : pools.get(slot)) {
                        ids[slot] = id;
                        double value = evaluate(ids);
                        if (value > score + 1e-12) {
                            score = value;
                            winner = id;
                            changed = true;
                        }
                        if (stop()) break;
                    }
                    ids[slot] = winner;
                    if (stop()) break;
                }
                if (!changed) break;
            }
        }

        double evaluate(int[] ids) {
            if (stop()) return Double.NEGATIVE_INFINITY;
            if (twoHanded(ids[3]) && ids[5] > 0) return Double.NEGATIVE_INFINITY;
            trial.player.setEquippedItemIds(ids.clone());
            trial.player.setEquippedItemNames(new String[14]);
            trial.player.setRawEquipmentLoadout(null);
            double score = Double.NEGATIVE_INFINITY;
            for (CombatStyle style : styles()) {
                List<OptimizerSpell> choices =
                        style.getStance().contains("Autocast")
                                ? spells
                                : Collections.singletonList(null);
                for (OptimizerSpell spell : choices) {
                    if (stop()) break;
                    if (spell != null && !spell.compatible(itemName(ids[3]), ids, this::itemName))
                        continue;
                    String key =
                            Arrays.toString(ids)
                                    + style.getName()
                                    + style.getAttackType()
                                    + "/"
                                    + (spell == null ? "powered" : spell.name);
                    if (scores.containsKey(key)) {
                        score = Math.max(score, scores.get(key));
                        continue;
                    }
                    trial.player.setCombatStyle(style);
                    if (spell == null) OptimizerSpell.clear(trial.player);
                    else spell.apply(trial.player);
                    ScenarioCalculator.Result r =
                            calculator.score(trial, request.target, request.encounter);
                    if (r.error == null
                            && !request.includeKnownLimitations
                            && !r.limitations.isEmpty())
                        r.error =
                                "Known formula limitation excluded; enable Include known formula"
                                        + " limitations to compare these results.";
                    if (r.error == null
                            && ids[13] > 0
                            && isProjectile(facts(ids[13]))
                            && r.ammoApplicability != AmmoApplicability.INCLUDED)
                        r.error = "Ammunition is not used by this weapon";
                    double value =
                            r.error == null && r.normal.getDps() > 0
                                    ? r.rankingDps()
                                    : Double.NEGATIVE_INFINITY;
                    scores.put(key, value);
                    if (Double.isFinite(value)) {
                        score = Math.max(score, value);
                        EquipmentStats stats = r.equipmentStats;
                        int protection =
                                stats.getPrayerBonus() * 5
                                        + stats.getStabDefence()
                                        + stats.getSlashDefence()
                                        + stats.getCrushDefence()
                                        + stats.getMagicDefence()
                                        + stats.getRangedDefence();
                        Ranked ranked =
                                new Ranked(ids.clone(), style, spell, value, protection, key);
                        // Keep spell variants from crowding other gear out of refinement.
                        Ranked existing = null;
                        for (Ranked candidate : best)
                            if (Arrays.equals(candidate.ids, ids)) {
                                existing = candidate;
                                break;
                            }
                        if (existing == null || RANKING.compare(ranked, existing) < 0) {
                            best.remove(existing);
                            best.add(ranked);
                            best.sort(RANKING);
                            if (best.size() > 24) best.remove(best.size() - 1);
                        }
                        Ranked previous = weaponBest.get(ids[3]);
                        if (previous == null || RANKING.compare(ranked, previous) < 0)
                            weaponBest.put(ids[3], ranked);
                    }
                    if (scores.size() % 100 == 0) progress.accept(scores.size());
                }
            }
            return score;
        }

        List<CombatStyle> styles() {
            List<CombatStyle> styles = new ArrayList<>();
            for (CombatStyle style : WeaponStyles.available(trial.player, equipment)) {
                AttackType type = style.getAttackType();
                String typeFamily = type.isMagic() ? "Magic" : type.isRanged() ? "Ranged" : "Melee";
                if (typeFamily.equals(family)
                        && (!style.getStance().contains("Autocast") || !spells.isEmpty()))
                    styles.add(style);
            }
            return styles;
        }

        Scenario.Loadout makeDraft(Ranked ranked) {
            Scenario.Loadout draft = request.base.copy();
            draft.manualEquipmentStats = false;
            draft.name =
                    "Bank " + family.toLowerCase(Locale.ROOT) + " · " + request.target.getName();
            draft.player.setRawEquipmentLoadout(null);
            draft.player.setEquippedItemIds(ranked.ids.clone());
            String[] names = new String[14];
            for (int slot : SLOTS)
                if (ranked.ids[slot] > 0)
                    names[slot] =
                            request.owned.names.getOrDefault(
                                    ranked.ids[slot], facts(ranked.ids[slot]).getName());
            draft.player.setEquippedItemNames(names);
            WeaponStyles.select(draft, ranked.style);
            if (ranked.spell == null) OptimizerSpell.clear(draft.player);
            else ranked.spell.apply(draft.player);
            if (family.equals("Magic"))
                draft.overrides.addAll(
                        Arrays.asList("spellName", "spellbook", "spellElement", "spellMaxHit"));
            for (String path :
                    Scenario.flatten(Scenario.JSON.toJsonTree(draft.player).getAsJsonObject())
                            .keySet())
                if (Scenario.isLive(path)) {
                    draft.overrides.add(path);
                    draft.sources.put(path, "Bank optimizer snapshot");
                }
            draft.description =
                    "Best found using bank scan "
                            + request.owned.bankScannedAt
                            + ". "
                            + request.encounter.summary()
                            + "; "
                            + (request.encounter.grouped ? "estimated group DPS" : "normal DPS")
                            + "; fixed target, prayers and boosts. Confirm charges and unlocks.";
            return draft;
        }

        String itemName(int id) {
            EquipmentCatalogItem item = facts(id);
            return item == null ? "" : item.getName();
        }

        boolean stop() {
            if (Thread.currentThread().isInterrupted()) throw new CancellationException();
            if (scores.size() >= budget || System.nanoTime() >= deadline) limited = true;
            return limited;
        }
    }

    private EquipmentCatalogItem facts(int id) {
        try {
            return equipment.getItemFacts(id);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isProjectile(EquipmentCatalogItem item) {
        if (item == null) return false;
        String name = item.getName().toLowerCase(Locale.ROOT);
        return item.getStats().getRangedStrength() > 0
                || name.contains("arrow")
                || name.contains("bolt") && !name.contains("pouch")
                || name.endsWith(" tar")
                || name.contains("javelin");
    }

    private boolean twoHanded(int id) {
        EquipmentCatalogItem item = facts(id);
        return item != null && item.isTwoHanded();
    }

    private static int slot(String name) {
        for (int i = 0; i < CATALOG_SLOTS.length; i++)
            if (CATALOG_SLOTS[i].equals(name)) return SLOTS[i];
        return -1;
    }

    private static int index(int slot) {
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == slot) return i;
        return -1;
    }

    private static final Comparator<Ranked> RANKING =
            Comparator.comparingDouble((Ranked rank) -> rank.dps)
                    .reversed()
                    .thenComparing(
                            Comparator.comparingInt((Ranked rank) -> rank.protection).reversed())
                    .thenComparing(rank -> rank.key);

    private static final class Ranked {
        final int[] ids;
        final CombatStyle style;
        final OptimizerSpell spell;
        final double dps;
        final int protection;
        final String key;

        Ranked(
                int[] ids,
                CombatStyle style,
                OptimizerSpell spell,
                double dps,
                int protection,
                String key) {
            this.ids = ids;
            this.style = style;
            this.spell = spell;
            this.dps = dps;
            this.protection = protection;
            this.key = key;
        }
    }
}
