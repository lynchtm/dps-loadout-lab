package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static com.dpscalc.TestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration tests for GearSnapshot-to-PlayerState conversion and DPS calculation.
 * Tests the full pipeline: GearSnapshot → PlayerState → DPS calculation.
 *
 * <p>Test scenarios:
 * <ul>
 *   <li>Full gear conversion - Equipment arrays match</li>
 *   <li>Best prayer auto-selection - Piety/Rigour/Augury added based on combat style</li>
 *   <li>Max boosts application - Boosts are +5 for relevant stats</li>
 *   <li>Empty equipment handling - PlayerState handles gracefully</li>
 *   <li>Unknown combat style - Defaults gracefully</li>
 *   <li>DPS calculation integration - Snapshot → PlayerState → DPS > 0</li>
 * </ul>
 */
@RunWith(Enclosed.class)
public class GearSnapshotCalculationTest {

    /**
     * Tests for snapshot-to-PlayerState conversion.
     */
    public static class SnapshotConversion {

        private DpsCalcPluginTestHelper pluginHelper;

        @Before
        public void setUp() {
            pluginHelper = new DpsCalcPluginTestHelper();
        }

        @Test
        public void testSnapshotWithFullGearConvertsToPlayerState() {
            // Arrange: Create snapshot with full gear
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            itemIds[EquipmentSlot.WEAPON.getIndex()] = 4151;  // Abyssal whip
            itemNames[EquipmentSlot.WEAPON.getIndex()] = "Abyssal whip";
            itemIds[EquipmentSlot.HEAD.getIndex()] = 11834;  // Bandos helmet
            itemNames[EquipmentSlot.HEAD.getIndex()] = "Bandos helmet";
            itemIds[EquipmentSlot.BODY.getIndex()] = 11832;  // Bandos chestplate
            itemNames[EquipmentSlot.BODY.getIndex()] = "Bandos chestplate";

            // Fill remaining slots with -1 (empty)
            for (int i = 0; i < 14; i++) {
                if (itemIds[i] == 0) {
                    itemIds[i] = -1;
                }
            }

            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withEquipment(itemIds, itemNames)
                .withCombatStyleName("Slash")
                .withCombatStyleStance("Accurate")
                .withName("Max melee test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Equipment arrays match
            assertNotNull("PlayerState should not be null", playerState);
            assertArrayEquals("Item IDs should match", itemIds, playerState.getEquippedItemIds());
            assertArrayEquals("Item names should match", itemNames, playerState.getEquippedItemNames());
            assertEquals("Weapon ID should match", 4151, playerState.getWeaponId());
            assertEquals("Weapon name should match", "Abyssal whip", playerState.getWeaponName());
        }

        @Test
        public void testEmptyEquipmentHandling() {
            // Arrange: Create snapshot with all empty slots
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            for (int i = 0; i < 14; i++) {
                itemIds[i] = -1;
                itemNames[i] = null;
            }

            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withEquipment(itemIds, itemNames)
                .withCombatStyleName("Unarmed")
                .withCombatStyleStance("Punch")
                .withName("Empty gear test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: PlayerState handles empty equipment gracefully
            assertNotNull("PlayerState should not be null", playerState);
            assertEquals("Weapon ID should be -1 for empty", -1, playerState.getWeaponId());
            assertNull("Weapon name should be null for empty", playerState.getWeaponName());
            assertNotNull("Equipment stats should not be null", playerState.getEquipmentStats());
        }

        @Test
        public void testUnknownCombatStyleDefaultsGracefully() {
            // Arrange: Create snapshot with unknown combat style
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("InvalidStyle")
                .withCombatStyleStance("InvalidStance")
                .withName("Unknown style test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Defaults to a valid combat style (UNARMED_PUNCH)
            assertNotNull("PlayerState should not be null", playerState);
            assertNotNull("Combat style should not be null", playerState.getCombatStyle());
            assertEquals("Should default to UNARMED_PUNCH", CombatStyle.UNARMED_PUNCH, playerState.getCombatStyle());
        }
    }

    /**
     * Tests for "Use Best Offensive Prayer" feature.
     */
    public static class BestPrayerSelection {

        private DpsCalcPluginTestHelper pluginHelper;

        @Before
        public void setUp() {
            pluginHelper = new DpsCalcPluginTestHelper();
        }

        @Test
        public void testUseBestPrayerAddsPietyForMelee() {
            // Arrange: Create snapshot with melee style and useBestPrayer enabled
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Slash")
                .withCombatStyleStance("Aggressive")
                .withUseBestPrayer(true)
                .withName("Melee best prayer test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Piety should be added
            assertNotNull("PlayerState should not be null", playerState);
            assertTrue("Piety should be active", playerState.getActivePrayers().contains(Prayer.PIETY));
        }

        @Test
        public void testUseBestPrayerAddsRigourForRanged() {
            // Arrange: Create snapshot with ranged style and useBestPrayer enabled
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Accurate")
                .withCombatStyleStance("Ranged")
                .withUseBestPrayer(true)
                .withName("Ranged best prayer test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Rigour should be added
            assertNotNull("PlayerState should not be null", playerState);
            assertTrue("Rigour should be active", playerState.getActivePrayers().contains(Prayer.RIGOUR));
        }

        @Test
        public void testUseBestPrayerAddsAuguryForMagic() {
            // Arrange: Create snapshot with magic style and useBestPrayer enabled
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Magic")
                .withCombatStyleStance("Autocast")
                .withUseBestPrayer(true)
                .withName("Magic best prayer test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Augury should be added
            assertNotNull("PlayerState should not be null", playerState);
            assertTrue("Augury should be active", playerState.getActivePrayers().contains(Prayer.AUGURY));
        }

        @Test
        public void testUseBestPrayerDisabledDoesNotAddPrayer() {
            // Arrange: Create snapshot with useBestPrayer disabled
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Slash")
                .withCombatStyleStance("Aggressive")
                .withUseBestPrayer(false)
                .withName("No best prayer test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: No prayers should be added
            assertNotNull("PlayerState should not be null", playerState);
            assertFalse("Piety should not be active", playerState.getActivePrayers().contains(Prayer.PIETY));
            assertTrue("Prayer set should be empty", playerState.getActivePrayers().isEmpty());
        }
    }

    /**
     * Tests for "Assume Max Boosts" feature.
     */
    public static class MaxBoostsApplication {

        private DpsCalcPluginTestHelper pluginHelper;

        @Before
        public void setUp() {
            pluginHelper = new DpsCalcPluginTestHelper();
        }

        @Test
        public void testAssumeMaxBoostsAppliesBoostsForMelee() {
            // Arrange: Create snapshot with melee style and assumeMaxBoosts enabled
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Slash")
                .withCombatStyleStance("Aggressive")
                .withAssumeMaxBoosts(true)
                .withName("Melee max boosts test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Attack and strength boosts should be +5
            assertNotNull("PlayerState should not be null", playerState);
            assertEquals("Attack boost should be +5", 5, playerState.getAttackBoost());
            assertEquals("Strength boost should be +5", 5, playerState.getStrengthBoost());
        }

        @Test
        public void testAssumeMaxBoostsAppliesBoostsForRanged() {
            // Arrange: Create snapshot with ranged style and assumeMaxBoosts enabled
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Accurate")
                .withCombatStyleStance("Ranged")
                .withAssumeMaxBoosts(true)
                .withName("Ranged max boosts test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Ranged boost should be +5
            assertNotNull("PlayerState should not be null", playerState);
            assertEquals("Ranged boost should be +5", 5, playerState.getRangedBoost());
        }

        @Test
        public void testAssumeMaxBoostsAppliesBoostsForMagic() {
            // Arrange: Create snapshot with magic style and assumeMaxBoosts enabled
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Magic")
                .withCombatStyleStance("Autocast")
                .withAssumeMaxBoosts(true)
                .withName("Magic max boosts test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Magic boost should be +5
            assertNotNull("PlayerState should not be null", playerState);
            assertEquals("Magic boost should be +5", 5, playerState.getMagicBoost());
        }

        @Test
        public void testAssumeMaxBoostsDisabledUsesCurrentBoosts() {
            // Arrange: Create snapshot with assumeMaxBoosts disabled
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Slash")
                .withCombatStyleStance("Aggressive")
                .withAssumeMaxBoosts(false)
                .withName("No max boosts test")
                .build();

            // Act: Convert to PlayerState
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);

            // Assert: Boosts should match cached player state (0 in test helper)
            assertNotNull("PlayerState should not be null", playerState);
            assertEquals("Attack boost should be 0", 0, playerState.getAttackBoost());
            assertEquals("Strength boost should be 0", 0, playerState.getStrengthBoost());
        }
    }

    /**
     * Integration tests for full DPS calculation pipeline.
     */
    public static class DpsCalculationIntegration {

        private DpsCalcPluginTestHelper pluginHelper;

        @Before
        public void setUp() {
            pluginHelper = new DpsCalcPluginTestHelper();
        }

        @Test
        public void testDpsCalculationWorksWithSnapshot() {
            // Arrange: Create snapshot with known gear
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            itemIds[EquipmentSlot.WEAPON.getIndex()] = 4151;  // Abyssal whip
            itemNames[EquipmentSlot.WEAPON.getIndex()] = "Abyssal whip";

            // Fill remaining slots with -1 (empty)
            for (int i = 0; i < 14; i++) {
                if (itemIds[i] == 0) {
                    itemIds[i] = -1;
                }
            }

            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withEquipment(itemIds, itemNames)
                .withCombatStyleName("Slash")
                .withCombatStyleStance("Accurate")
                .withAssumeMaxBoosts(true)
                .withUseBestPrayer(true)
                .withName("DPS calculation test")
                .build();

            // Act: Convert to PlayerState and calculate DPS
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);
            MonsterStats monster = getTestMonster();
            DpsCalculator calculator = new DpsCalculator(playerState, monster);
            DpsResult result = calculator.calculate();

            // Assert: DPS should be > 0
            assertNotNull("DPS result should not be null", result);
            assertTrue("DPS should be greater than 0", result.getDps() > 0);
            assertTrue("Max hit should be greater than 0", result.getMaxHit() > 0);
            assertTrue("Accuracy should be between 0 and 1", result.getAccuracy() > 0 && result.getAccuracy() <= 1);
        }

        @Test
        public void testDpsCalculationWithMaxBoostsAndPrayer() {
            // Arrange: Create snapshot with max boosts and best prayer
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Slash")
                .withCombatStyleStance("Aggressive")
                .withAssumeMaxBoosts(true)
                .withUseBestPrayer(true)
                .withName("Max DPS test")
                .build();

            // Act: Convert to PlayerState and calculate DPS
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);
            MonsterStats monster = getTestMonster();
            DpsCalculator calculator = new DpsCalculator(playerState, monster);
            DpsResult result = calculator.calculate();

            // Assert: DPS should be > 0 (even with no equipment, boosts and prayer help)
            assertNotNull("DPS result should not be null", result);
            assertTrue("DPS should be greater than 0", result.getDps() > 0);
            assertTrue("Max hit should be greater than 0", result.getMaxHit() > 0);
        }

        @Test
        public void testDpsCalculationWithEmptyGear() {
            // Arrange: Create snapshot with empty gear
            GearSnapshot snapshot = new GearSnapshotBuilder()
                .withCombatStyleName("Unarmed")
                .withCombatStyleStance("Punch")
                .withName("Empty gear DPS test")
                .build();

            // Act: Convert to PlayerState and calculate DPS
            PlayerState playerState = pluginHelper.snapshotToPlayerState(snapshot);
            MonsterStats monster = getTestMonster();
            DpsCalculator calculator = new DpsCalculator(playerState, monster);
            DpsResult result = calculator.calculate();

            // Assert: DPS should still be calculable (unarmed combat)
            assertNotNull("DPS result should not be null", result);
            assertTrue("DPS should be >= 0", result.getDps() >= 0);
            assertTrue("Max hit should be >= 0", result.getMaxHit() >= 0);
        }
    }

    /**
     * Test helper that simulates DpsCalcPlugin behavior without RuneLite dependencies.
     * Provides a minimal implementation of snapshotToPlayerState() for testing.
     */
    private static class DpsCalcPluginTestHelper {

        private PlayerState cachedPlayerState;

        public DpsCalcPluginTestHelper() {
            // Initialize cached player state with default values
            cachedPlayerState = player()
                .attackLevel(99)
                .strengthLevel(99)
                .defenceLevel(99)
                .rangedLevel(99)
                .magicLevel(99)
                .hitpointsLevel(99)
                .currentHitpoints(99)
                .build();
        }

        /**
         * Simplified version of DpsCalcPlugin.snapshotToPlayerState() for testing.
         * Does not use ItemManager (assumes equipment stats are calculated elsewhere).
         */
        public PlayerState snapshotToPlayerState(GearSnapshot snapshot) {
            if (cachedPlayerState == null || snapshot == null) {
                return null;
            }

            PlayerState state = new PlayerState();

            // Copy equipment arrays
            state.setEquippedItemIds(snapshot.getEquippedItemIds().clone());
            state.setEquippedItemNames(snapshot.getEquippedItemNames().clone());

            // For testing, use empty equipment stats (real implementation would calculate from ItemManager)
            state.setEquipmentStats(new EquipmentStats());

            // Weapon speed: default to 4 (unarmed)
            state.setWeaponSpeed(4);

            // Copy prayers
            java.util.Set<Prayer> prayers = java.util.EnumSet.copyOf(snapshot.getActivePrayers());

            // Determine attack type from combat style name
            AttackType attackType = determineAttackType(snapshot.getCombatStyleName());

            // Apply "Use Best Offensive Prayer" if enabled
            if (snapshot.isUseBestOffensivePrayer()) {
                Prayer bestPrayer = getBestOffensivePrayer(attackType);
                if (bestPrayer != null) {
                    prayers.add(bestPrayer);
                }
            }
            state.setActivePrayers(prayers);

            // Reconstruct combat style
            CombatStyle combatStyle = reconstructCombatStyle(snapshot.getCombatStyleName(), snapshot.getCombatStyleStance());
            state.setCombatStyle(combatStyle);

            // Copy player levels from cached state
            state.setAttackLevel(cachedPlayerState.getAttackLevel());
            state.setStrengthLevel(cachedPlayerState.getStrengthLevel());
            state.setDefenceLevel(cachedPlayerState.getDefenceLevel());
            state.setRangedLevel(cachedPlayerState.getRangedLevel());
            state.setMagicLevel(cachedPlayerState.getMagicLevel());
            state.setHitpointsLevel(cachedPlayerState.getHitpointsLevel());
            state.setCurrentHitpoints(cachedPlayerState.getCurrentHitpoints());

            // Apply max boosts or copy current boosts
            if (snapshot.isAssumeMaxBoosts()) {
                applyMaxBoosts(state, attackType);
            } else {
                state.setAttackBoost(cachedPlayerState.getAttackBoost());
                state.setStrengthBoost(cachedPlayerState.getStrengthBoost());
                state.setDefenceBoost(cachedPlayerState.getDefenceBoost());
                state.setRangedBoost(cachedPlayerState.getRangedBoost());
                state.setMagicBoost(cachedPlayerState.getMagicBoost());
            }

            // Copy config flags (default to false for testing)
            state.setOnSlayerTask(false);
            state.setChargeSpellActive(false);
            state.setInWilderness(false);

            return state;
        }

        private AttackType determineAttackType(String styleName) {
            if (styleName == null) {
                return AttackType.CRUSH;
            }

            String upper = styleName.toUpperCase();

            if (upper.contains("STAB")) {
                return AttackType.STAB;
            }
            if (upper.contains("SLASH")) {
                return AttackType.SLASH;
            }
            if (upper.contains("CRUSH")) {
                return AttackType.CRUSH;
            }

            if (upper.contains("RANGE") || upper.contains("ACCURATE") ||
                upper.contains("RAPID") || upper.contains("LONGRANGE")) {
                return AttackType.RANGED_STANDARD;
            }

            if (upper.contains("MAGIC") || upper.contains("AUTOCAST")) {
                return AttackType.MAGIC;
            }

            return AttackType.CRUSH;
        }

        private Prayer getBestOffensivePrayer(AttackType attackType) {
            if (attackType == null) {
                return null;
            }

            if (attackType.isMelee()) {
                return Prayer.PIETY;
            }
            if (attackType.isRanged()) {
                return Prayer.RIGOUR;
            }
            if (attackType.isMagic()) {
                return Prayer.AUGURY;
            }

            return null;
        }

        private void applyMaxBoosts(PlayerState state, AttackType attackType) {
            if (attackType == null) {
                return;
            }

            if (attackType.isMelee()) {
                state.setAttackBoost(5);
                state.setStrengthBoost(5);
            } else if (attackType.isRanged()) {
                state.setRangedBoost(5);
            } else if (attackType.isMagic()) {
                state.setMagicBoost(5);
            }
        }

        private CombatStyle reconstructCombatStyle(String name, String stance) {
            if (name == null || stance == null) {
                return CombatStyle.UNARMED_PUNCH;
            }

            CombatStyle matchByBoth = null;
            CombatStyle matchByName = null;

            try {
                for (java.lang.reflect.Field field : CombatStyle.class.getDeclaredFields()) {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) &&
                        java.lang.reflect.Modifier.isPublic(field.getModifiers()) &&
                        field.getType() == CombatStyle.class) {

                        CombatStyle style = (CombatStyle) field.get(null);

                        if (name.equals(style.getName()) && stance.equals(style.getStance())) {
                            matchByBoth = style;
                            break;
                        }

                        if (matchByName == null && name.equals(style.getName())) {
                            matchByName = style;
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                // Ignore reflection errors in tests
            }

            if (matchByBoth != null) {
                return matchByBoth;
            }
            if (matchByName != null) {
                return matchByName;
            }

            return CombatStyle.UNARMED_PUNCH;
        }
    }
}
