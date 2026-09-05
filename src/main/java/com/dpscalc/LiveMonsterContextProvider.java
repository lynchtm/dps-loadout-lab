package com.dpscalc;

import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterConstants;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterScaling;
import com.dpscalc.data.MonsterStats;
import java.util.function.IntUnaryOperator;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.gameval.VarbitID;

public final class LiveMonsterContextProvider {
    static final int TOA_RAID_LEVEL_VARBIT = VarbitID.TOA_CLIENT_RAID_LEVEL;
    static final int[] TOA_PARTY_VARBITS = {
        VarbitID.TOA_CLIENT_P0,
        VarbitID.TOA_CLIENT_P1,
        VarbitID.TOA_CLIENT_P2,
        VarbitID.TOA_CLIENT_P3,
        VarbitID.TOA_CLIENT_P4,
        VarbitID.TOA_CLIENT_P5,
        VarbitID.TOA_CLIENT_P6,
        VarbitID.TOA_CLIENT_P7,
    };
    static final int TOA_CRONDIS_LEVEL_VARBIT = VarbitID.TOA_CLIENT_CRONDIS_LEVEL;
    static final int TOA_SCABARAS_LEVEL_VARBIT = VarbitID.TOA_CLIENT_SCABARAS_LEVEL;
    static final int TOA_HET_LEVEL_VARBIT = VarbitID.TOA_CLIENT_HET_LEVEL;
    static final int TOA_APMEKEN_LEVEL_VARBIT = VarbitID.TOA_CLIENT_APMEKEN_LEVEL;

    static final int RAIDS_CLIENT_INDUNGEON_VARBIT = VarbitID.RAIDS_CLIENT_INDUNGEON;
    static final int RAIDS_CLIENT_PARTYSIZE_SCALED_VARBIT = VarbitID.RAIDS_CLIENT_PARTYSIZE_SCALED;
    static final int RAIDS_SCALING_VARBIT = VarbitID.RAIDS_SCALING;
    static final int RAIDS_CLIENT_PARTYSIZE_VARBIT = VarbitID.RAIDS_CLIENT_PARTYSIZE;

    private final IntUnaryOperator varbitReader;
    private final TargetHealth targetHealth;

    public LiveMonsterContextProvider(Client client, NPC targetNpc) {
        this(varbit -> client.getVarbitValue(varbit), TargetHealth.from(targetNpc));
    }

    LiveMonsterContextProvider(IntUnaryOperator varbitReader, TargetHealth targetHealth) {
        this.varbitReader = varbitReader;
        this.targetHealth = targetHealth;
    }

    public MonsterStats enrich(MonsterStats source) {
        MonsterStats enriched = source.copy();
        MonsterInputs inputs = enriched.getInputs();

        applyToaContext(enriched, inputs);
        applyCoxContext(enriched, inputs);
        enriched = MonsterScaling.scale(enriched);
        inputs = enriched.getInputs();
        applyCurrentHp(enriched, inputs);

        return enriched;
    }

    private void applyToaContext(MonsterStats monster, MonsterInputs inputs) {
        if (!monster.isToaMonster()) {
            return;
        }

        inputs.setToaInvocationLevel(varbit(TOA_RAID_LEVEL_VARBIT));
        inputs.setPartySize(toaPartySize());

        Integer pathLevelVarbit = toaPathLevelVarbit(monster.getId());
        if (pathLevelVarbit != null) {
            inputs.setToaPathLevel(varbit(pathLevelVarbit));
        }
    }

    private int toaPartySize() {
        int partySize = 0;
        for (int partyVarbit : TOA_PARTY_VARBITS) {
            partySize += Math.min(varbit(partyVarbit), 1);
        }
        return Math.max(1, partySize);
    }

    private static Integer toaPathLevelVarbit(int monsterId) {
        if (MonsterConstants.contains(MonsterConstants.ZEBAK_IDS, monsterId)) {
            return TOA_CRONDIS_LEVEL_VARBIT;
        }
        if (isKephriMonster(monsterId)) {
            return TOA_SCABARAS_LEVEL_VARBIT;
        }
        if (MonsterConstants.contains(MonsterConstants.AKKHA_IDS, monsterId)
            || MonsterConstants.contains(MonsterConstants.AKKHA_SHADOW_IDS, monsterId)) {
            return TOA_HET_LEVEL_VARBIT;
        }
        if (MonsterConstants.contains(MonsterConstants.BABA_IDS, monsterId)) {
            return TOA_APMEKEN_LEVEL_VARBIT;
        }
        return null;
    }

    private static boolean isKephriMonster(int monsterId) {
        return MonsterConstants.contains(MonsterConstants.KEPHRI_SHIELDED_IDS, monsterId)
            || MonsterConstants.contains(MonsterConstants.KEPHRI_UNSHIELDED_IDS, monsterId)
            || MonsterConstants.contains(MonsterConstants.KEPHRI_OVERLORD_IDS, monsterId);
    }

    private void applyCoxContext(MonsterStats monster, MonsterInputs inputs) {
        if (varbit(RAIDS_CLIENT_INDUNGEON_VARBIT) <= 0 && !monster.hasAttribute(MonsterAttribute.XERICIAN)) {
            return;
        }

        int partySize = firstPositiveVarbit(
            RAIDS_CLIENT_PARTYSIZE_SCALED_VARBIT,
            RAIDS_SCALING_VARBIT,
            RAIDS_CLIENT_PARTYSIZE_VARBIT
        );
        if (partySize > 0) {
            inputs.setPartySize(partySize);
        }
    }

    private int firstPositiveVarbit(int... varbits) {
        for (int varbit : varbits) {
            int value = varbit(varbit);
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private void applyCurrentHp(MonsterStats monster, MonsterInputs inputs) {
        if (!targetHealth.isAvailable()) {
            return;
        }

        int maxHp = Math.max(0, monster.getHitpoints());
        int scaledHp = (int) Math.ceil((double) maxHp * targetHealth.ratio / targetHealth.scale);
        inputs.setMonsterCurrentHp(clamp(scaledHp, 0, maxHp));
    }

    private int varbit(int varbit) {
        return varbitReader.applyAsInt(varbit);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static final class TargetHealth {
        private final int ratio;
        private final int scale;

        TargetHealth(int ratio, int scale) {
            this.ratio = ratio;
            this.scale = scale;
        }

        static TargetHealth from(NPC targetNpc) {
            return new TargetHealth(targetNpc.getHealthRatio(), targetNpc.getHealthScale());
        }

        static TargetHealth unavailable() {
            return new TargetHealth(-1, 0);
        }

        private boolean isAvailable() {
            return scale > 0 && ratio >= 0;
        }
    }
}
