package com.dpscalc.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Monster IDs and phase constants ported from the web calculator.
 * Contains all special monster IDs for accuracy, damage, and defence calculations.
 */
public final class MonsterConstants {

    private MonsterConstants() {}

    // ==================== BLOWPIPE IDS ====================
    public static final int[] BLOWPIPE_IDS = {
        12926, // regular
        28688, // blazing
        31575, // camphor
        31579, // ironwood
        31583, // rosewood
    };

    // ==================== TOMBS OF AMASCUT ====================
    public static final int[] AKKHA_IDS = {
        11789, 11790, 11791, 11792, 11793, 11794, 11795, 11796,
    };

    public static final int[] AKKHA_SHADOW_IDS = {
        11797, 11798, 11799,
    };

    public static final int[] BABA_IDS = {
        11778, 11779, 11780,
    };

    public static final int[] KEPHRI_SHIELDED_IDS = {
        11719,
    };

    public static final int[] KEPHRI_UNSHIELDED_IDS = {
        11721,
    };

    public static final int[] KEPHRI_OVERLORD_IDS = {
        11724, 11725, 11726,
    };

    public static final int[] ZEBAK_IDS = {
        11730, 11732, 11733,
    };

    public static final int[] TOA_OBELISK_IDS = {
        11751, 11750, 11752,
    };

    public static final int[] P2_WARDEN_IDS = {
        11753, 11754, // elidinis
        11756, 11757, // tumeken
    };

    public static final int[] P3_WARDEN_IDS = {
        11761, 11763, // elidinis
        11762, 11764, // tumeken
    };

    public static final int[] TOA_WARDEN_CORE_EJECTED_IDS = {
        11755, // elidinis
        11758, // tumeken
    };

    // Path monsters affected by path level
    public static final Set<Integer> TOMBS_OF_AMASCUT_PATH_MONSTER_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : AKKHA_IDS) ids.add(id);
        for (int id : AKKHA_SHADOW_IDS) ids.add(id);
        for (int id : BABA_IDS) ids.add(id);
        for (int id : KEPHRI_SHIELDED_IDS) ids.add(id);
        for (int id : KEPHRI_UNSHIELDED_IDS) ids.add(id);
        for (int id : KEPHRI_OVERLORD_IDS) ids.add(id);
        for (int id : ZEBAK_IDS) ids.add(id);
        TOMBS_OF_AMASCUT_PATH_MONSTER_IDS = Collections.unmodifiableSet(ids);
    }

    // All ToA monsters
    public static final Set<Integer> TOMBS_OF_AMASCUT_MONSTER_IDS;
    static {
        Set<Integer> ids = new HashSet<>(TOMBS_OF_AMASCUT_PATH_MONSTER_IDS);
        for (int id : TOA_OBELISK_IDS) ids.add(id);
        for (int id : P2_WARDEN_IDS) ids.add(id);
        for (int id : TOA_WARDEN_CORE_EJECTED_IDS) ids.add(id);
        for (int id : P3_WARDEN_IDS) ids.add(id);
        TOMBS_OF_AMASCUT_MONSTER_IDS = Collections.unmodifiableSet(ids);
    }

    // ==================== THEATRE OF BLOOD ====================
    public static final int[] VERZIK_P1_IDS = {
        10830, 10831, 10832, // em
        8369, 8370, 8371, // norm
        10847, 10848, 10849, // hmt
    };

    public static final int[] VERZIK_IDS;
    static {
        int[] p1 = VERZIK_P1_IDS;
        int[] other = {
            10833, 10834, 10835, // verzik entry mode
            8372, 8373, 8374, // verzik normal mode
            10850, 10851, 10852, // verzik hard mode
        };
        VERZIK_IDS = new int[p1.length + other.length];
        System.arraycopy(p1, 0, VERZIK_IDS, 0, p1.length);
        System.arraycopy(other, 0, VERZIK_IDS, p1.length, other.length);
    }

    public static final int[] SOTETSEG_IDS = {
        8387, 8388, // normal
        10867, 10868, // hard
    };

    public static final Set<Integer> TOB_MONSTER_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : VERZIK_P1_IDS) ids.add(id);
        // normal
        for (int id : new int[]{8360, 8361, 8362, 8363, 8364, 8365}) ids.add(id); // maiden
        for (int id : new int[]{8366, 8367}) ids.add(id); // maiden crab + blood spawn
        ids.add(8359); // bloat
        for (int id : new int[]{8342, 8343, 8344, 8345, 8346, 8347, 8348, 8349, 8350, 8351, 8352, 8353}) ids.add(id); // nylos
        for (int id : new int[]{8355, 8356, 8357}) ids.add(id); // nylo boss
        for (int id : SOTETSEG_IDS) ids.add(id);
        for (int id : new int[]{8339, 8340}) ids.add(id); // xarpus
        for (int id : new int[]{8372, 8373, 8374}) ids.add(id); // verzik
        for (int id : new int[]{8376, 8381, 8382, 8383, 8384, 8385}) ids.add(id); // verzik web + nylos
        // hmt
        for (int id : new int[]{10822, 10823, 10824, 10825, 10826, 10827}) ids.add(id); // maiden
        for (int id : new int[]{10828, 10829}) ids.add(id); // maiden crab + blood spawn
        ids.add(10813); // bloat
        for (int id : new int[]{10791, 10792, 10793, 10794, 10795, 10796, 10797, 10798, 10799, 10800, 10801, 10802}) ids.add(id); // nylos
        for (int id : new int[]{10804, 10805, 10806}) ids.add(id); // nylo demi-boss
        for (int id : new int[]{10808, 10809, 10810}) ids.add(id); // nylo boss
        for (int id : new int[]{10770, 10771, 10772}) ids.add(id); // xarpus
        for (int id : new int[]{10850, 10851, 10852}) ids.add(id); // verzik
        for (int id : new int[]{10854, 10858, 10859, 10860, 10861, 10862}) ids.add(id); // verzik web + nylos
        TOB_MONSTER_IDS = Collections.unmodifiableSet(ids);
    }

    public static final Set<Integer> TOB_EM_MONSTER_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : new int[]{10814, 10815, 10816, 10817, 10818, 10819}) ids.add(id); // maiden
        for (int id : new int[]{10820, 10821}) ids.add(id); // maiden crab + blood spawn
        ids.add(10812); // bloat
        for (int id : new int[]{10774, 10775, 10776, 10777, 10778, 10779, 10780, 10781, 10782, 10783, 10784, 10785}) ids.add(id); // nylos
        for (int id : new int[]{10787, 10788, 10789}) ids.add(id); // nylo boss
        for (int id : new int[]{10864, 10865}) ids.add(id); // sote
        for (int id : new int[]{10767, 10768}) ids.add(id); // xarpus
        for (int id : new int[]{10833, 10834, 10835}) ids.add(id); // verzik
        for (int id : new int[]{10837, 10841, 10842, 10843, 10844, 10845}) ids.add(id); // verzik web + nylos
        TOB_EM_MONSTER_IDS = Collections.unmodifiableSet(ids);
    }

    // ==================== GAUNTLET ====================
    public static final int[] GAUNTLET_MONSTER_IDS = {
        9021, // Crystalline Hunllef
        9026, // Crystalline Rat
        9027, // Crystalline Spider
        9028, // Crystalline Bat
        9029, // Crystalline Unicorn
        9030, // Crystalline Scorpion
        9031, // Crystalline Wolf
        9032, // Crystalline Bear
        9033, // Crystalline Dragon
        9034, // Crystalline Dark Beast
    };

    public static final int[] CORRUPTED_GAUNTLET_MONSTER_IDS = {
        9035, // Corrupted Hunllef
        9040, // Corrupted Rat
        9041, // Corrupted Spider
        9042, // Corrupted Bat
        9043, // Corrupted Unicorn
        9044, // Corrupted Scorpion
        9045, // Corrupted Wolf
        9046, // Corrupted Bear
        9047, // Corrupted Dragon
        9048, // Corrupted Dark Beast
    };

    // ==================== CHAMBERS OF XERIC ====================
    public static final int[] TEKTON_IDS = {
        7540, 7543, // reg
        7544, 7545, // cm
    };

    public static final int[] GUARDIAN_IDS = {
        7569, 7571, // reg
        7570, 7572, // cm
    };

    public static final int[] OLM_HEAD_IDS = {
        7551, // reg
        7554, // cm
    };

    public static final int[] OLM_MELEE_HAND_IDS = {
        7552, // reg
        7555, // cm
    };

    public static final int[] OLM_MAGE_HAND_IDS = {
        7550, // reg
        7553, // cm
    };

    public static final int[] OLM_IDS;
    static {
        OLM_IDS = new int[OLM_HEAD_IDS.length + OLM_MELEE_HAND_IDS.length + OLM_MAGE_HAND_IDS.length];
        int idx = 0;
        for (int id : OLM_HEAD_IDS) OLM_IDS[idx++] = id;
        for (int id : OLM_MELEE_HAND_IDS) OLM_IDS[idx++] = id;
        for (int id : OLM_MAGE_HAND_IDS) OLM_IDS[idx++] = id;
    }

    public static final int[] SCAVENGER_BEAST_IDS = {
        7548, 7549,
    };

    public static final int[] ABYSSAL_PORTAL_IDS = {
        7533,
    };

    public static final int[] GLOWING_CRYSTAL_IDS = {
        7568,
    };

    public static final int[] ICE_DEMON_IDS = {
        7584, // reg
        7585, // cm
    };

    public static final int[] VESPINE_SOLDIER_IDS = {
        7538, 7539,
    };

    public static final int[] DEATHLY_RANGER_IDS = {
        7559,
    };

    public static final int[] VESPULA_IDS = {
        7530, 7531, 7532,
    };

    // CoX monsters that use defence for magic defence
    public static final Set<Integer> COX_MAGIC_IS_DEFENSIVE_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : DEATHLY_RANGER_IDS) ids.add(id);
        for (int id : TEKTON_IDS) ids.add(id);
        for (int id : ABYSSAL_PORTAL_IDS) ids.add(id);
        for (int id : VESPULA_IDS) ids.add(id);
        for (int id : VESPINE_SOLDIER_IDS) ids.add(id);
        for (int id : OLM_MELEE_HAND_IDS) ids.add(id);
        for (int id : OLM_MAGE_HAND_IDS) ids.add(id);
        COX_MAGIC_IS_DEFENSIVE_IDS = Collections.unmodifiableSet(ids);
    }

    public static final Set<Integer> COX_USE_SINGLES_SCALING_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : SCAVENGER_BEAST_IDS) ids.add(id);
        for (int id : VESPINE_SOLDIER_IDS) ids.add(id);
        COX_USE_SINGLES_SCALING_IDS = Collections.unmodifiableSet(ids);
    }

    // ==================== OTHER BOSSES ====================
    public static final int[] FRAGMENT_OF_SEREN_IDS = {
        8917, 8918, 8919, 8920,
    };

    public static final int[] NIGHTMARE_IDS = {
        378, 9425, 9426, 9427, 9428, 9429, 9430, 9431, 9432, 9433, 9460, // nightmare
        377, 9423, 9416, 9417, 9418, 9419, 9420, 9421, 9422, 9424, 11153, 11154, 11155, // phosani's
    };

    public static final int[] NIGHTMARE_TOTEM_IDS = {
        9434, 9437, 9440, 9443,
        9435, 9438, 9441, 9444,
    };

    public static final int[] NEX_IDS = {
        11278, 11279, 11280, 11281, 11282,
    };

    public static final int[] ZULRAH_IDS = {
        2042, 2043, 2044,
    };

    public static final int[] VARDORVIS_IDS = {
        12223, 12224, 12228, 12425, 12426, 13656,
    };

    public static final int[] TITAN_BOSS_IDS = {
        12596, // Fire elemental (Royal Titans)
        14147, // Ice elemental (Royal Titans)
    };

    public static final int[] TITAN_ELEMENTAL_IDS = {
        14150, // Fire elemental (Royal Titans)
        14151, // Ice elemental (Royal Titans)
    };

    public static final int[] UNDERWATER_MONSTERS = {
        7796, // lobstrosity
    };

    public static final int[] YAMA_VOID_FLARE_IDS = {
        14179,
    };

    public static final int[] YAMA_IDS = {
        14176,
    };

    public static final int[] DOOM_OF_MOKHAIOTL_IDS = {
        14707,
    };

    public static final int[] ECLIPSE_MOON_IDS = {
        13012,
    };

    public static final int[] HUEYCOATL_HEAD_IDS = {
        14009, 14010, 14013,
    };

    public static final int[] HUEYCOATL_BODY_IDS = {
        14017,
    };

    public static final int[] HUEYCOATL_TAIL_IDS = {
        14014,
    };

    public static final int[] HUEYCOATL_IDS;
    static {
        HUEYCOATL_IDS = new int[HUEYCOATL_HEAD_IDS.length + HUEYCOATL_BODY_IDS.length + HUEYCOATL_TAIL_IDS.length];
        int idx = 0;
        for (int id : HUEYCOATL_HEAD_IDS) HUEYCOATL_IDS[idx++] = id;
        for (int id : HUEYCOATL_BODY_IDS) HUEYCOATL_IDS[idx++] = id;
        for (int id : HUEYCOATL_TAIL_IDS) HUEYCOATL_IDS[idx++] = id;
    }

    public static final Set<Integer> HUEYCOATL_PHASE_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : HUEYCOATL_HEAD_IDS) ids.add(id);
        for (int id : HUEYCOATL_TAIL_IDS) ids.add(id);
        HUEYCOATL_PHASE_IDS = Collections.unmodifiableSet(ids);
    }

    public static final int[] ABYSSAL_SIRE_TRANSITION_IDS = {
        5886, 5889, 5891,
    };

    public static final int[] ARAXXOR_IDS = {
        13668,
    };

    public static final int[] TD_IDS = {
        13599, 13600, 13601, 13602, 13603, 13604, 13605, 13606,
    };

    public static final int[] DUSK_IDS = {
        7851, 7854, 7855, 7882, 7883, 7886, // dusk first form
        7887, 7888, 7889, // dusk second form
    };

    public static final int[] WARRIORS_GUILD_CYCLOPES = {
        2463, 2465, 2467, // L56
        2464, 2466, 2468, // L76
        2137, 2138, 2139, 2140, 2141, 2142, // L106
    };

    public static final int[] BA_ATTACKER_MONSTERS = {
        // fighters
        1667, 5739, 5740, 5741, 5742, 5743, 5744, 5745, 5746, 5747,
        // rangers
        1668, 5757, 5758, 5759, 5760, 5761, 5762, 5763, 5764, 5765,
    };

    public static final int[] INFINITE_HEALTH_MONSTERS = {
        14779, // gemstone crab
    };

    // ==================== SPECIAL MONSTER SETS ====================

    /**
     * NPCs that calculate their magical defence using the defence stat.
     */
    public static final Set<Integer> USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : ICE_DEMON_IDS) ids.add(id);
        for (int id : VERZIK_IDS) ids.add(id);
        for (int id : FRAGMENT_OF_SEREN_IDS) ids.add(id);
        ids.add(11709); // baboon brawler
        ids.add(11712); // baboon brawler
        ids.add(9118); // rabbit (prifddinas)
        USES_DEFENCE_LEVEL_FOR_MAGIC_DEFENCE_NPC_IDS = Collections.unmodifiableSet(ids);
    }

    /**
     * Monsters immune to melee damage.
     */
    public static final Set<Integer> IMMUNE_TO_MELEE_DAMAGE_NPC_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        ids.add(494); // kraken
        for (int id : ABYSSAL_PORTAL_IDS) ids.add(id);
        ids.add(7706); // zuk
        ids.add(7708); // Jal-MejJak
        ids.add(12214); // leviathan
        ids.add(12215);
        ids.add(12219);
        for (int id : ZULRAH_IDS) ids.add(id);
        IMMUNE_TO_MELEE_DAMAGE_NPC_IDS = Collections.unmodifiableSet(ids);
    }

    public static final Set<Integer> IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : new int[]{3169, 3170, 3171, 3172, 3173, 3174, 3175, 3176, 3177, 3178, 3179, 3180, 3181, 3182, 3183}) {
            ids.add(id); // aviansie
        }
        ids.add(7037); // reanimated aviansie
        IMMUNE_TO_NON_SALAMANDER_MELEE_DAMAGE_NPC_IDS = Collections.unmodifiableSet(ids);
    }

    /**
     * Monsters immune to ranged damage.
     */
    public static final Set<Integer> IMMUNE_TO_RANGED_DAMAGE_NPC_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : TEKTON_IDS) ids.add(id);
        for (int id : DUSK_IDS) ids.add(id);
        for (int id : GLOWING_CRYSTAL_IDS) ids.add(id);
        for (int id : WARRIORS_GUILD_CYCLOPES) ids.add(id);
        IMMUNE_TO_RANGED_DAMAGE_NPC_IDS = Collections.unmodifiableSet(ids);
    }

    public static final Set<Integer> IMMUNE_TO_BURN_DAMAGE_NPC_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : TEKTON_IDS) ids.add(id);
        for (int id : DUSK_IDS) ids.add(id);
        for (int id : GLOWING_CRYSTAL_IDS) ids.add(id);
        for (int id : WARRIORS_GUILD_CYCLOPES) ids.add(id);
        IMMUNE_TO_BURN_DAMAGE_NPC_IDS = Collections.unmodifiableSet(ids);
    }

    /**
     * Monsters immune to magic damage.
     */
    public static final Set<Integer> IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        for (int id : DUSK_IDS) ids.add(id);
        for (int id : WARRIORS_GUILD_CYCLOPES) ids.add(id);
        IMMUNE_TO_MAGIC_DAMAGE_NPC_IDS = Collections.unmodifiableSet(ids);
    }

    public static final Set<Integer> PARTY_SIZE_REQUIRED_MONSTER_IDS;
    static {
        Set<Integer> ids = new HashSet<>();
        ids.addAll(TOMBS_OF_AMASCUT_MONSTER_IDS);
        ids.addAll(TOB_MONSTER_IDS);
        ids.addAll(TOB_EM_MONSTER_IDS);
        PARTY_SIZE_REQUIRED_MONSTER_IDS = Collections.unmodifiableSet(ids);
    }

    /**
     * NPCs that will always die in one hit from a player attack.
     */
    public static final Set<Integer> ONE_HIT_MONSTERS;
    static {
        Set<Integer> ids = new HashSet<>();
        ids.add(7223); // Giant rat (Scurrius)
        ids.add(8584); // Flower
        ids.add(11193); // Flower (A Night at the Theatre)
        ONE_HIT_MONSTERS = Collections.unmodifiableSet(ids);
    }

    /**
     * NPCs that the player always max hits with using the correct combat style.
     */
    public static final Set<Integer> ALWAYS_MAX_HIT_MELEE;
    static {
        Set<Integer> ids = new HashSet<>();
        ids.add(11710); // baboon thrower
        ids.add(11713); // baboon thrower
        ids.add(12814); // frem warband archer
        for (int id : TOA_WARDEN_CORE_EJECTED_IDS) ids.add(id);
        for (int id : YAMA_VOID_FLARE_IDS) ids.add(id);
        ALWAYS_MAX_HIT_MELEE = Collections.unmodifiableSet(ids);
    }

    public static final Set<Integer> ALWAYS_MAX_HIT_RANGED;
    static {
        Set<Integer> ids = new HashSet<>();
        ids.add(11711); // baboon mage
        ids.add(11714); // baboon mage
        ids.add(12815); // frem warband seer
        ids.add(11717); // cursed baboon
        ids.add(11715); // baboon shaman
        for (int id : YAMA_VOID_FLARE_IDS) ids.add(id);
        ALWAYS_MAX_HIT_RANGED = Collections.unmodifiableSet(ids);
    }

    public static final Set<Integer> ALWAYS_MAX_HIT_MAGIC;
    static {
        Set<Integer> ids = new HashSet<>();
        ids.add(11709); // baboon brawler
        ids.add(11712); // baboon brawler
        ids.add(12816); // frem warband berserker
        ids.add(14151); // Royal titans elemental
        ids.add(14150); // Royal titans elemental
        for (int id : YAMA_VOID_FLARE_IDS) ids.add(id);
        ALWAYS_MAX_HIT_MAGIC = Collections.unmodifiableSet(ids);
    }

    /**
     * NPCs that the player has 100% accuracy against.
     */
    public static final Set<Integer> GUARANTEED_ACCURACY_MONSTERS;
    static {
        Set<Integer> ids = new HashSet<>();
        ids.add(5916); // Spawn (abyssal sire)
        GUARANTEED_ACCURACY_MONSTERS = Collections.unmodifiableSet(ids);
    }

    /**
     * NPCs that will always hit the player with their attacks.
     */
    public static final Set<Integer> ALWAYS_ACCURATE_MONSTERS;
    static {
        Set<Integer> ids = new HashSet<>();
        ids.add(931); // Thrower Troll
        ids.add(4135); // Thrower troll (Trollheim)
        ids.add(7691); // Jal-Nib
        ids.add(12335); // Wighted Leech
        ALWAYS_ACCURATE_MONSTERS = Collections.unmodifiableSet(ids);
    }

    // ==================== MONSTER PHASES ====================

    public static final String[] TD_PHASES = {"Shielded", "Shielded (Defenceless)", "Unshielded"};
    public static final String[] ARAXXOR_PHASES = {"Standard", "Enraged"};
    public static final String[] HUEYCOATL_PHASES = {"Without Pillar", "With Pillar"};
    public static final String[] ROYAL_TITANS_PHASES = {"In Melee Range", "Out of Melee Range"};
    public static final String[] DOOM_OF_MOKHAIOTL_PHASES = {"Normal", "Shielded", "Burrowing"};
    public static final String[] ABYSSAL_SIRE_PHASES = {"Standard", "Transition"};
    public static final String[] YAMA_PHASES = {"Tank using magic", "Tank not using magic"};

    /**
     * Map of monster IDs to their available phases.
     */
    public static final Map<Integer, String[]> MONSTER_PHASES_BY_ID;
    static {
        Map<Integer, String[]> map = new HashMap<>();
        for (int id : TD_IDS) map.put(id, TD_PHASES);
        for (int id : ARAXXOR_IDS) map.put(id, ARAXXOR_PHASES);
        for (int id : HUEYCOATL_HEAD_IDS) map.put(id, HUEYCOATL_PHASES);
        for (int id : HUEYCOATL_TAIL_IDS) map.put(id, HUEYCOATL_PHASES);
        for (int id : TITAN_BOSS_IDS) map.put(id, ROYAL_TITANS_PHASES);
        for (int id : DOOM_OF_MOKHAIOTL_IDS) map.put(id, DOOM_OF_MOKHAIOTL_PHASES);
        for (int id : ABYSSAL_SIRE_TRANSITION_IDS) map.put(id, ABYSSAL_SIRE_PHASES);
        for (int id : YAMA_IDS) map.put(id, YAMA_PHASES);
        MONSTER_PHASES_BY_ID = Collections.unmodifiableMap(map);
    }

    // ==================== CONSTANTS ====================

    public static final int DEFAULT_ATTACK_SPEED = 4;
    public static final double SECONDS_PER_TICK = 0.6;
    public static final int ACCURACY_PRECISION = 2;
    public static final int DPS_PRECISION = 3;
    public static final int EXPECTED_HIT_PRECISION = 1;
    public static final int TTK_DIST_MAX_ITER_ROUNDS = 1000;
    public static final double TTK_DIST_EPSILON = 0.0001;

    // ==================== UTILITY METHODS ====================

    public static boolean contains(int[] array, int value) {
        for (int id : array) {
            if (id == value) return true;
        }
        return false;
    }

    public static boolean isP2Warden(int monsterId) {
        return contains(P2_WARDEN_IDS, monsterId);
    }

    public static boolean isVerzikP1(int monsterId) {
        return contains(VERZIK_P1_IDS, monsterId);
    }

    public static boolean isTekton(int monsterId) {
        return contains(TEKTON_IDS, monsterId);
    }

    public static boolean isGlowingCrystal(int monsterId) {
        return contains(GLOWING_CRYSTAL_IDS, monsterId);
    }

    public static boolean isOlmHead(int monsterId) {
        return contains(OLM_HEAD_IDS, monsterId);
    }

    public static boolean isOlmMeleeHand(int monsterId) {
        return contains(OLM_MELEE_HAND_IDS, monsterId);
    }

    public static boolean isOlmMageHand(int monsterId) {
        return contains(OLM_MAGE_HAND_IDS, monsterId);
    }

    public static boolean isIceDemon(int monsterId) {
        return contains(ICE_DEMON_IDS, monsterId);
    }

    public static boolean isNightmareTotem(int monsterId) {
        return contains(NIGHTMARE_TOTEM_IDS, monsterId);
    }

    public static boolean isGuardian(int monsterId) {
        return contains(GUARDIAN_IDS, monsterId);
    }

    public static boolean isTitanBoss(int monsterId) {
        return contains(TITAN_BOSS_IDS, monsterId);
    }

    public static boolean isTitanElemental(int monsterId) {
        return contains(TITAN_ELEMENTAL_IDS, monsterId);
    }

    public static boolean isDoomOfMokhaiotl(int monsterId) {
        return contains(DOOM_OF_MOKHAIOTL_IDS, monsterId);
    }

    public static boolean isEclipseMoon(int monsterId) {
        return contains(ECLIPSE_MOON_IDS, monsterId);
    }

    public static boolean isYamaVoidFlare(int monsterId) {
        return contains(YAMA_VOID_FLARE_IDS, monsterId);
    }

    public static boolean isYama(int monsterId) {
        return contains(YAMA_IDS, monsterId);
    }

    public static boolean isHueycoatlTail(int monsterId) {
        return contains(HUEYCOATL_TAIL_IDS, monsterId);
    }

    public static boolean isAbyssalSireTransition(int monsterId) {
        return contains(ABYSSAL_SIRE_TRANSITION_IDS, monsterId);
    }

    public static boolean isTormentedDemon(int monsterId) {
        return contains(TD_IDS, monsterId);
    }

    public static boolean isVespula(int monsterId) {
        return contains(VESPULA_IDS, monsterId);
    }

    public static boolean isZulrah(int monsterId) {
        return contains(ZULRAH_IDS, monsterId);
    }

    public static boolean isKephriOverlord(int monsterId) {
        return contains(KEPHRI_OVERLORD_IDS, monsterId);
    }

    public static boolean isBaAttackerMonster(int monsterId) {
        return contains(BA_ATTACKER_MONSTERS, monsterId);
    }

    public static String[] getPhasesForMonster(int monsterId) {
        return MONSTER_PHASES_BY_ID.get(monsterId);
    }
}
