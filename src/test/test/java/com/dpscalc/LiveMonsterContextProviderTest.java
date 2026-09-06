package com.dpscalc;

import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterConstants;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import java.util.function.IntUnaryOperator;
import org.junit.Test;

import static org.junit.Assert.*;

public class LiveMonsterContextProviderTest {

    @Test
    public void enrichCopiesSourceAndAddsToaContext_whenTargetIsPathMonster() {
        // Given
        MonsterStats source = monster(MonsterConstants.ZEBAK_IDS[0], 500);
        source.getInputs().setToaInvocationLevel(12);
        source.getInputs().setToaPathLevel(1);
        source.getInputs().setMonsterCurrentHp(444);
        source.getInputs().setPartySize(7);

        LiveMonsterContextProvider provider = new LiveMonsterContextProvider(
            varbits(
                LiveMonsterContextProvider.TOA_RAID_LEVEL_VARBIT, 350,
                LiveMonsterContextProvider.TOA_PARTY_VARBITS[0], 1,
                LiveMonsterContextProvider.TOA_PARTY_VARBITS[1], 2,
                LiveMonsterContextProvider.TOA_PARTY_VARBITS[2], 0,
                LiveMonsterContextProvider.TOA_PARTY_VARBITS[3], 1,
                LiveMonsterContextProvider.TOA_CRONDIS_LEVEL_VARBIT, 4
            ),
            new LiveMonsterContextProvider.TargetHealth(33, 100)
        );

        // When
        MonsterStats enriched = provider.enrich(source);

        // Then
        assertNotSame(source, enriched);
        assertNotSame(source.getInputs(), enriched.getInputs());
        assertEquals(12, source.getInputs().getToaInvocationLevel());
        assertEquals(1, source.getInputs().getToaPathLevel());
        assertEquals(444, source.getInputs().getMonsterCurrentHp());
        assertEquals(7, source.getInputs().getPartySize());
        assertEquals(350, enriched.getInputs().getToaInvocationLevel());
        assertEquals(4, enriched.getInputs().getToaPathLevel());
        assertEquals(3, enriched.getInputs().getPartySize());
        assertEquals(4130, enriched.getHitpoints());
        assertEquals(1363, enriched.getInputs().getMonsterCurrentHp());
    }

    @Test
    public void enrichUsesCoxScaledPartySize_whenScaledVarbitIsPositive() {
        // Given
        MonsterStats source = monster(123, 300);
        source.addAttribute(MonsterAttribute.XERICIAN);
        LiveMonsterContextProvider provider = new LiveMonsterContextProvider(
            varbits(
                LiveMonsterContextProvider.RAIDS_CLIENT_INDUNGEON_VARBIT, 0,
                LiveMonsterContextProvider.RAIDS_CLIENT_PARTYSIZE_SCALED_VARBIT, 5,
                LiveMonsterContextProvider.RAIDS_SCALING_VARBIT, 4,
                LiveMonsterContextProvider.RAIDS_CLIENT_PARTYSIZE_VARBIT, 3
            ),
            LiveMonsterContextProvider.TargetHealth.unavailable()
        );

        // When
        MonsterStats enriched = provider.enrich(source);

        // Then
        assertEquals(1, source.getInputs().getPartySize());
        assertEquals(5, enriched.getInputs().getPartySize());
        assertEquals(0, enriched.getInputs().getMonsterCurrentHp());
    }

    @Test
    public void enrichFallsBackToCoxScalingPartySize_whenScaledVarbitIsMissing() {
        // Given
        MonsterStats source = monster(MonsterConstants.OLM_HEAD_IDS[0], 300);
        LiveMonsterContextProvider provider = new LiveMonsterContextProvider(
            varbits(
                LiveMonsterContextProvider.RAIDS_CLIENT_INDUNGEON_VARBIT, 1,
                LiveMonsterContextProvider.RAIDS_CLIENT_PARTYSIZE_SCALED_VARBIT, 0,
                LiveMonsterContextProvider.RAIDS_SCALING_VARBIT, 4,
                LiveMonsterContextProvider.RAIDS_CLIENT_PARTYSIZE_VARBIT, 3
            ),
            LiveMonsterContextProvider.TargetHealth.unavailable()
        );

        // When
        MonsterStats enriched = provider.enrich(source);

        // Then
        assertEquals(4, enriched.getInputs().getPartySize());
    }

    @Test
    public void enrichFallsBackToCoxClientPartySize_whenScaledAndScalingAreMissing() {
        // Given
        MonsterStats source = monster(MonsterConstants.OLM_HEAD_IDS[0], 300);
        LiveMonsterContextProvider provider = new LiveMonsterContextProvider(
            varbits(
                LiveMonsterContextProvider.RAIDS_CLIENT_INDUNGEON_VARBIT, 1,
                LiveMonsterContextProvider.RAIDS_CLIENT_PARTYSIZE_SCALED_VARBIT, 0,
                LiveMonsterContextProvider.RAIDS_SCALING_VARBIT, 0,
                LiveMonsterContextProvider.RAIDS_CLIENT_PARTYSIZE_VARBIT, 3
            ),
            LiveMonsterContextProvider.TargetHealth.unavailable()
        );

        // When
        MonsterStats enriched = provider.enrich(source);

        // Then
        assertEquals(3, enriched.getInputs().getPartySize());
    }

    @Test
    public void enrichAppliesCoxScalingBeforeCurrentHp_whenTargetHealthIsAvailable() {
        // Given
        MonsterStats source = monster(MonsterConstants.TEKTON_IDS[0], 300);
        source.addAttribute(MonsterAttribute.XERICIAN);
        source.setAttackLevel(390);
        source.setDefenceLevel(205);
        source.setStrengthLevel(390);
        source.setMagicLevel(205);
        source.setRangedLevel(1);
        LiveMonsterContextProvider provider = new LiveMonsterContextProvider(
            varbits(
                LiveMonsterContextProvider.RAIDS_CLIENT_INDUNGEON_VARBIT, 1,
                LiveMonsterContextProvider.RAIDS_CLIENT_PARTYSIZE_SCALED_VARBIT, 2
            ),
            new LiveMonsterContextProvider.TargetHealth(50, 100)
        );

        // When
        MonsterStats enriched = provider.enrich(source);

        // Then
        assertEquals(300, source.getHitpoints());
        assertEquals(600, enriched.getHitpoints());
        assertEquals(300, enriched.getInputs().getMonsterCurrentHp());
    }

    @Test
    public void enrichCeilsAndClampsCurrentHp_whenTargetHealthIsAvailable() {
        // Given
        MonsterStats source = monster(123, 10);
        LiveMonsterContextProvider provider = new LiveMonsterContextProvider(
            varbits(),
            new LiveMonsterContextProvider.TargetHealth(1, 3)
        );
        LiveMonsterContextProvider overhealedProvider = new LiveMonsterContextProvider(
            varbits(),
            new LiveMonsterContextProvider.TargetHealth(40, 10)
        );

        // When
        MonsterStats enriched = provider.enrich(source);
        MonsterStats overhealed = overhealedProvider.enrich(source);

        // Then
        assertEquals(0, source.getInputs().getMonsterCurrentHp());
        assertEquals(4, enriched.getInputs().getMonsterCurrentHp());
        assertEquals(10, overhealed.getInputs().getMonsterCurrentHp());
    }

    @Test
    public void enrichLeavesCurrentHpUnchanged_whenTargetHealthIsUnavailable() {
        // Given
        MonsterStats source = monster(123, 10);
        source.getInputs().setMonsterCurrentHp(8);
        LiveMonsterContextProvider provider = new LiveMonsterContextProvider(
            varbits(),
            new LiveMonsterContextProvider.TargetHealth(-1, 30)
        );

        // When
        MonsterStats enriched = provider.enrich(source);

        // Then
        assertEquals(8, enriched.getInputs().getMonsterCurrentHp());
    }

    @Test
    public void enrichPreservesSelectedVersion_whenApplyingLiveContext() {
        // Given
        MonsterStats source = monster(MonsterConstants.ZEBAK_IDS[0], 500);
        source.setVersion("Challenge Mode");
        LiveMonsterContextProvider provider = new LiveMonsterContextProvider(
            varbits(
                LiveMonsterContextProvider.TOA_RAID_LEVEL_VARBIT, 300,
                LiveMonsterContextProvider.TOA_PARTY_VARBITS[0], 1,
                LiveMonsterContextProvider.TOA_CRONDIS_LEVEL_VARBIT, 2
            ),
            new LiveMonsterContextProvider.TargetHealth(50, 100)
        );

        // When
        MonsterStats enriched = provider.enrich(source);

        // Then
        assertEquals("Challenge Mode", enriched.getVersion());
        assertEquals(300, enriched.getInputs().getToaInvocationLevel());
        assertEquals(2, enriched.getInputs().getToaPathLevel());
        assertEquals(1, enriched.getInputs().getPartySize());
        assertEquals(620, enriched.getInputs().getMonsterCurrentHp());
        assertEquals(0, source.getInputs().getToaInvocationLevel());
        assertEquals(0, source.getInputs().getMonsterCurrentHp());
    }

    private static MonsterStats monster(int id, int hitpoints) {
        MonsterStats stats = new MonsterStats();
        stats.setId(id);
        stats.setHitpoints(hitpoints);
        stats.setInputs(new MonsterInputs());
        return stats;
    }

    private static IntUnaryOperator varbits(int... idAndValuePairs) {
        return id -> {
            for (int index = 0; index < idAndValuePairs.length; index += 2) {
                if (idAndValuePairs[index] == id) {
                    return idAndValuePairs[index + 1];
                }
            }
            return 0;
        };
    }
}
