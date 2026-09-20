// SPDX-License-Identifier: BSD-2-Clause
package com.loadoutlab.observed;

import java.util.*;
import static net.runelite.api.gameval.NpcID.*;

/** Explicit encounter membership. NPC names are display text, never grouping keys. */
public final class EncounterDefinition {
    private static final Map<Integer, EncounterDefinition> BY_ID = new HashMap<>();

    static {
        register("Vorkath", ids(VORKATH), ids(VORKATH_SPAWN), ids(VORKATH), ids(),
                ids(VORKATH_SLEEPING, VORKATH_SLEEPING_NOOP));
        register("Zulrah", ids(SNAKEBOSS_BOSS_RANGED, SNAKEBOSS_BOSS_MELEE, SNAKEBOSS_BOSS_MAGIC),
                ids(SNAKEBOSS_MINION_MELEE, SNAKEBOSS_MINION_MAGIC, SNAKEBOSS_MINION_DYING),
                ids(SNAKEBOSS_BOSS_RANGED, SNAKEBOSS_BOSS_MELEE, SNAKEBOSS_BOSS_MAGIC), ids(), ids());
        register("Alchemical Hydra", ids(HYDRABOSS, HYDRABOSS_P1_TRANSITION,
                HYDRABOSS_P2_TRANSITION, HYDRABOSS_P3_TRANSITION, HYDRABOSS_2,
                HYDRABOSS_3, HYDRABOSS_4, HYDRABOSS_FINALDEATH), ids(),
                ids(HYDRABOSS_4), ids(HYDRABOSS_FINALDEATH), ids());
        register("Kalphite Queen", ids(KALPHITE_QUEEN, KALPHITE_FLYINGQUEEN), ids(),
                ids(KALPHITE_FLYINGQUEEN), ids(), ids());
        register("Vet'ion", ids(VETION, VETION_2, VETION_TRANS, VETION_TRANS_2),
                ids(VETION_HELLHOUND_JNR, VETION_HELLHOUND_SNR), ids(VETION_2), ids(), ids());
        register("Calvar'ion", ids(VETION_SINGLE, VETION_2_SINGLE, VETION_TRANS_SINGLE,
                VETION_TRANS_2_SINGLE), ids(VETION_HELLHOUND_JNR_SINGLES, VETION_HELLHOUND_SNR_SINGLES),
                ids(VETION_2_SINGLE), ids(), ids());
        register("Grotesque Guardians", ids(GARGBOSS_DUSK_SPAWN, GARGBOSS_DAWN_SPAWN,
                GARGBOSS_DUSK_PHASE1_DEFENSIVE, GARGBOSS_DAWN_PHASE1,
                GARGBOSS_DAWN_PHASE1_TRANSITION, GARGBOSS_DUSK_PHASE1_TRANSITION,
                GARGBOSS_DUSK_PHASE1_FLYTRANSITION, GARGBOSS_DUSK_PHASE2_ATTACKING,
                GARGBOSS_DUSK_PHASE3_DEFENSIVE, GARGBOSS_DAWN_PHASE3, GARGBOSS_DAWN_DEATH,
                GARGBOSS_DUSK_PHASE3_TRANSITION, GARGBOSS_DUSK_PHASE4_SPAWN,
                GARGBOSS_DUSK_PHASE4, GARGBOSS_DUSK_DEATH), ids(),
                ids(GARGBOSS_DUSK_PHASE4), ids(GARGBOSS_DUSK_DEATH), ids());
        register("Hespori", ids(HESPORI), ids(HESPORI_HEALER_ACTIVE, HESPORI_HEALER_INACTIVE),
                ids(HESPORI), ids(), ids());
        register("Abyssal Sire", ids(ABYSSALSIRE_SIRE_STASIS_SLEEPING, ABYSSALSIRE_SIRE_STASIS_AWAKE,
                ABYSSALSIRE_SIRE_STASIS_STUNNED, ABYSSALSIRE_SIRE_PUPPET, ABYSSALSIRE_SIRE_WANDERING,
                ABYSSALSIRE_SIRE_PANICKING, ABYSSALSIRE_SIRE_APOCALYPSE),
                ids(ABYSSALSIRE_LUNG, ABYSSALSIRE_LUNG_DYING, ABYSSALSIRE_SPAWN,
                        ABYSSALSIRE_SPAWN_DYING, ABYSSALSIRE_SCION, ABYSSALSIRE_SCION_DYING),
                ids(ABYSSALSIRE_SIRE_APOCALYPSE), ids(), ids());
        register("Nex", ids(NEX, NEX_SPAWNING, NEX_SOULSPLIT, NEX_DEFLECT, NEX_DYING),
                ids(NEX_SMOKEMAGE, NEX_SHADOWMAGE, NEX_BLOODMAGE, NEX_ICEMAGE,
                        NEX_PRISON_BLOOD_REAVER_BOSS), ids(NEX, NEX_SOULSPLIT, NEX_DEFLECT),
                ids(NEX_DYING), ids());
        register("Phantom Muspah", ids(MUSPAH, MUSPAH_MELEE, MUSPAH_SOULSPLIT, MUSPAH_FINAL,
                MUSPAH_TELEPORT), ids(), ids(MUSPAH_FINAL), ids(), ids());
        register("Sarachnis", ids(SARACHNIS), ids(SARACHNIS_MELEE_SPAWN, SARACHNIS_MAGE_SPAWN),
                ids(SARACHNIS), ids(), ids());
        register("General Graardor", ids(GODWARS_BANDOS_AVATAR),
                ids(GODWARS_SERGEANT_GOBLIN1, GODWARS_SERGEANT_GOBLIN2, GODWARS_SERGEANT_GOBLIN3),
                ids(GODWARS_BANDOS_AVATAR), ids(), ids());
        register("Commander Zilyana", ids(GODWARS_SARADOMIN_AVATAR),
                ids(GODWARS_SARADOMIN_UNICORN, GODWARS_SARADOMIN_LION, GODWARS_SARADOMIN_CENTAUR),
                ids(GODWARS_SARADOMIN_AVATAR), ids(), ids());
        register("Kree'arra", ids(GODWARS_ARMADYL_AVATAR),
                ids(GODWARS_ARMADYL_BODYGUARD_SKREE, GODWARS_ARMADYL_BODYGUARD_GEERIN,
                        GODWARS_ARMADYL_BODYGUARD_KILISA), ids(GODWARS_ARMADYL_AVATAR), ids(), ids());
        register("K'ril Tsutsaroth", ids(GODWARS_ZAMORAK_AVATAR),
                ids(GODWARS_ANCIENT_GREATER_DEMON, GODWARS_ANCIENT_LESSER_DEMON, GODWARS_ANCIENT_BLACK_DEMON),
                ids(GODWARS_ZAMORAK_AVATAR), ids(), ids());
    }

    public final String name;
    private final Set<Integer> bosses, adds, deathIds, deadForms, resetForms;

    private EncounterDefinition(String name, Set<Integer> bosses, Set<Integer> adds,
            Set<Integer> deathIds, Set<Integer> deadForms, Set<Integer> resetForms) {
        this.name = name;
        this.bosses = bosses;
        this.adds = adds;
        this.deathIds = deathIds;
        this.deadForms = deadForms;
        this.resetForms = resetForms;
    }

    private static void register(String name, Set<Integer> bosses, Set<Integer> adds,
            Set<Integer> deathIds, Set<Integer> deadForms, Set<Integer> resetForms) {
        EncounterDefinition definition = new EncounterDefinition(name, bosses, adds, deathIds, deadForms, resetForms);
        for (Set<Integer> group : Arrays.asList(bosses, adds, resetForms))
            for (int id : group)
                if (BY_ID.put(id, definition) != null)
                    throw new IllegalStateException("Duplicate encounter NPC: " + id);
    }

    private static Set<Integer> ids(int... values) {
        Set<Integer> result = new HashSet<>();
        for (int id : values) result.add(id);
        return Collections.unmodifiableSet(result);
    }

    public static EncounterDefinition forNpc(int id) { return BY_ID.get(id); }
    public boolean isBoss(int id) { return bosses.contains(id); }
    public boolean isAdd(int id) { return adds.contains(id); }
    public boolean endsOnDeath(int id) { return deathIds.contains(id); }
    public boolean isDeadForm(int id) { return deadForms.contains(id); }
    public boolean isResetForm(int id) { return resetForms.contains(id); }
}
