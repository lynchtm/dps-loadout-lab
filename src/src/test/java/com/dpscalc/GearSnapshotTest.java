package com.dpscalc;

import com.dpscalc.state.GearSnapshot;
import com.dpscalc.state.Prayer;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.EnumSet;

import static org.junit.Assert.*;

/**
 * Comprehensive test suite for GearSnapshot data model.
 * Tests construction, validation, equality, immutability, and builder pattern.
 */
@RunWith(Enclosed.class)
public class GearSnapshotTest {

    // ========================================================================
    // CONSTRUCTION TESTS
    // ========================================================================

    public static class Construction {

        @Test
        public void testValidConstruction() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            Arrays.fill(itemIds, -1);
            Arrays.fill(itemNames, "Empty");
            long timestamp = System.currentTimeMillis();

            // Act
            GearSnapshot snapshot = new GearSnapshot(
                    itemIds,
                    itemNames,
                    EnumSet.of(Prayer.PIETY),
                    "Slash",
                    "Accurate",
                    "Test Snapshot",
                    true,
                    false,
                    timestamp
            );

            // Assert
            assertNotNull(snapshot);
            assertEquals("Test Snapshot", snapshot.getName());
            assertEquals("Slash", snapshot.getCombatStyleName());
            assertEquals("Accurate", snapshot.getCombatStyleStance());
            assertEquals(timestamp, snapshot.getTimestamp());
            assertTrue(snapshot.isAssumeMaxBoosts());
            assertFalse(snapshot.isUseBestOffensivePrayer());
            assertEquals(1, snapshot.getActivePrayers().size());
            assertTrue(snapshot.getActivePrayers().contains(Prayer.PIETY));
        }

        @Test
        public void testNullNameThrowsException() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];

            // Act & Assert
            try {
                new GearSnapshot(itemIds, itemNames, null, "Slash", "Accurate", null, false, false, 0L);
                fail("Expected IllegalArgumentException for null name");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("name must not be null or empty"));
            }
        }

        @Test
        public void testEmptyNameThrowsException() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];

            // Act & Assert
            try {
                new GearSnapshot(itemIds, itemNames, null, "Slash", "Accurate", "   ", false, false, 0L);
                fail("Expected IllegalArgumentException for empty name");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("name must not be null or empty"));
            }
        }

        @Test
        public void testWrongItemIdsLengthThrowsException() {
            // Arrange
            int[] itemIds = new int[13]; // Wrong length
            String[] itemNames = new String[14];

            // Act & Assert
            try {
                new GearSnapshot(itemIds, itemNames, null, "Slash", "Accurate", "Test", false, false, 0L);
                fail("Expected IllegalArgumentException for wrong itemIds length");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("equippedItemIds must be a 14-element array"));
            }
        }

        @Test
        public void testWrongItemNamesLengthThrowsException() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[15]; // Wrong length

            // Act & Assert
            try {
                new GearSnapshot(itemIds, itemNames, null, "Slash", "Accurate", "Test", false, false, 0L);
                fail("Expected IllegalArgumentException for wrong itemNames length");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("equippedItemNames must be a 14-element array"));
            }
        }

        @Test
        public void testNullPrayersBecomesEmptySet() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];

            // Act
            GearSnapshot snapshot = new GearSnapshot(
                    itemIds, itemNames, null, "Slash", "Accurate", "Test", false, false, 0L
            );

            // Assert
            assertNotNull(snapshot.getActivePrayers());
            assertTrue(snapshot.getActivePrayers().isEmpty());
        }
    }

    // ========================================================================
    // EQUALS AND HASHCODE TESTS
    // ========================================================================

    public static class EqualsAndHashCode {

        @Test
        public void testSameTimestampEquals() {
            // Arrange
            int[] itemIds1 = new int[14];
            String[] itemNames1 = new String[14];
            int[] itemIds2 = new int[14];
            String[] itemNames2 = new String[14];
            long timestamp = 1234567890L;

            // Act
            GearSnapshot snapshot1 = new GearSnapshot(
                    itemIds1, itemNames1, null, "Slash", "Accurate", "Snapshot 1", false, false, timestamp
            );
            GearSnapshot snapshot2 = new GearSnapshot(
                    itemIds2, itemNames2, EnumSet.of(Prayer.PIETY), "Stab", "Aggressive", "Snapshot 2", true, true, timestamp
            );

            // Assert - same timestamp means equal, regardless of other fields
            assertEquals(snapshot1, snapshot2);
            assertEquals(snapshot1.hashCode(), snapshot2.hashCode());
        }

        @Test
        public void testDifferentTimestampNotEquals() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            Arrays.fill(itemIds, 12345);
            Arrays.fill(itemNames, "Same Item");

            // Act
            GearSnapshot snapshot1 = new GearSnapshot(
                    itemIds.clone(), itemNames.clone(), EnumSet.of(Prayer.PIETY), "Slash", "Accurate", "Same Name", true, false, 1000L
            );
            GearSnapshot snapshot2 = new GearSnapshot(
                    itemIds.clone(), itemNames.clone(), EnumSet.of(Prayer.PIETY), "Slash", "Accurate", "Same Name", true, false, 2000L
            );

            // Assert - different timestamps means not equal, even if all other fields match
            assertNotEquals(snapshot1, snapshot2);
            assertNotEquals(snapshot1.hashCode(), snapshot2.hashCode());
        }

        @Test
        public void testSelfEquals() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            GearSnapshot snapshot = new GearSnapshot(
                    itemIds, itemNames, null, "Slash", "Accurate", "Test", false, false, 0L
            );

            // Act & Assert
            assertEquals(snapshot, snapshot);
        }

        @Test
        public void testNullNotEquals() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            GearSnapshot snapshot = new GearSnapshot(
                    itemIds, itemNames, null, "Slash", "Accurate", "Test", false, false, 0L
            );

            // Act & Assert
            assertNotEquals(snapshot, null);
        }
    }

    // ========================================================================
    // IMMUTABILITY TESTS
    // ========================================================================

    public static class Immutability {

        @Test
        public void testDefensiveCopyItemIds() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            itemIds[0] = 12345;
            itemIds[1] = 67890;

            // Act
            GearSnapshot snapshot = new GearSnapshot(
                    itemIds, itemNames, null, "Slash", "Accurate", "Test", false, false, 0L
            );

            // Modify input array after construction
            itemIds[0] = 99999;
            itemIds[1] = 11111;

            // Assert - snapshot should be unaffected
            assertEquals(12345, snapshot.getEquippedItemIds()[0]);
            assertEquals(67890, snapshot.getEquippedItemIds()[1]);
        }

        @Test
        public void testDefensiveCopyItemNames() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            itemNames[0] = "Abyssal whip";
            itemNames[1] = "Dragon defender";

            // Act
            GearSnapshot snapshot = new GearSnapshot(
                    itemIds, itemNames, null, "Slash", "Accurate", "Test", false, false, 0L
            );

            // Modify input array after construction
            itemNames[0] = "Modified";
            itemNames[1] = "Changed";

            // Assert - snapshot should be unaffected
            assertEquals("Abyssal whip", snapshot.getEquippedItemNames()[0]);
            assertEquals("Dragon defender", snapshot.getEquippedItemNames()[1]);
        }

        @Test
        public void testDefensiveCopyPrayers() {
            // Arrange
            int[] itemIds = new int[14];
            String[] itemNames = new String[14];
            EnumSet<Prayer> prayers = EnumSet.of(Prayer.PIETY);

            // Act
            GearSnapshot snapshot = new GearSnapshot(
                    itemIds, itemNames, prayers, "Slash", "Accurate", "Test", false, false, 0L
            );

            // Modify input set after construction
            prayers.add(Prayer.RIGOUR);
            prayers.add(Prayer.AUGURY);

            // Assert - snapshot should be unaffected
            assertEquals(1, snapshot.getActivePrayers().size());
            assertTrue(snapshot.getActivePrayers().contains(Prayer.PIETY));
            assertFalse(snapshot.getActivePrayers().contains(Prayer.RIGOUR));
        }
    }

    // ========================================================================
    // BUILDER PATTERN TESTS
    // ========================================================================

    public static class BuilderPattern {

        @Test
        public void testWithEquipmentSetsSlot() {
            // Act
            GearSnapshot snapshot = new GearSnapshotBuilder()
                    .withEquipment(0, 12345, "Abyssal whip")
                    .withEquipment(3, 67890, "Dragon defender")
                    .withName("Test")
                    .build();

            // Assert
            assertEquals(12345, snapshot.getEquippedItemIds()[0]);
            assertEquals("Abyssal whip", snapshot.getEquippedItemNames()[0]);
            assertEquals(67890, snapshot.getEquippedItemIds()[3]);
            assertEquals("Dragon defender", snapshot.getEquippedItemNames()[3]);
        }

        @Test
        public void testWithPrayersAccumulates() {
            // Act
            GearSnapshot snapshot = new GearSnapshotBuilder()
                    .withPrayers(Prayer.PIETY, Prayer.RIGOUR)
                    .withName("Test")
                    .build();

            // Assert
            assertEquals(2, snapshot.getActivePrayers().size());
            assertTrue(snapshot.getActivePrayers().contains(Prayer.PIETY));
            assertTrue(snapshot.getActivePrayers().contains(Prayer.RIGOUR));
        }

        @Test
        public void testWithPrayerAddsIndividual() {
            // Act
            GearSnapshot snapshot = new GearSnapshotBuilder()
                    .withPrayer(Prayer.PIETY)
                    .withPrayer(Prayer.RIGOUR)
                    .withName("Test")
                    .build();

            // Assert
            assertEquals(2, snapshot.getActivePrayers().size());
            assertTrue(snapshot.getActivePrayers().contains(Prayer.PIETY));
            assertTrue(snapshot.getActivePrayers().contains(Prayer.RIGOUR));
        }

        @Test
        public void testCopyFromPreservesAllFields() {
            // Arrange
            long timestamp = 1234567890L;
            GearSnapshot original = new GearSnapshotBuilder()
                    .withEquipment(0, 12345, "Abyssal whip")
                    .withEquipment(1, 67890, "Dragon defender")
                    .withPrayers(Prayer.PIETY, Prayer.RIGOUR)
                    .withCombatStyleName("Slash")
                    .withCombatStyleStance("Accurate")
                    .withName("Original")
                    .withAssumeMaxBoosts(true)
                    .withUseBestPrayer(true)
                    .withTimestamp(timestamp)
                    .build();

            // Act
            GearSnapshot copy = new GearSnapshotBuilder()
                    .copyFrom(original)
                    .build();

            // Assert - all fields preserved including timestamp
            assertEquals(original.getEquippedItemIds()[0], copy.getEquippedItemIds()[0]);
            assertEquals(original.getEquippedItemIds()[1], copy.getEquippedItemIds()[1]);
            assertEquals(original.getEquippedItemNames()[0], copy.getEquippedItemNames()[0]);
            assertEquals(original.getEquippedItemNames()[1], copy.getEquippedItemNames()[1]);
            assertEquals(original.getActivePrayers(), copy.getActivePrayers());
            assertEquals(original.getCombatStyleName(), copy.getCombatStyleName());
            assertEquals(original.getCombatStyleStance(), copy.getCombatStyleStance());
            assertEquals(original.getName(), copy.getName());
            assertEquals(original.isAssumeMaxBoosts(), copy.isAssumeMaxBoosts());
            assertEquals(original.isUseBestOffensivePrayer(), copy.isUseBestOffensivePrayer());
            assertEquals(original.getTimestamp(), copy.getTimestamp());

            // Verify equality based on timestamp
            assertEquals(original, copy);
        }

        @Test
        public void testCopyFromAllowsModification() {
            // Arrange
            GearSnapshot original = new GearSnapshotBuilder()
                    .withPrayers(Prayer.PIETY)
                    .withName("Original")
                    .build();

            // Act - copy and modify
            GearSnapshot modified = new GearSnapshotBuilder()
                    .copyFrom(original)
                    .withPrayers(Prayer.RIGOUR, Prayer.AUGURY)
                    .withName("Modified")
                    .build();

            // Assert - modified snapshot has new values
            assertEquals(2, modified.getActivePrayers().size());
            assertTrue(modified.getActivePrayers().contains(Prayer.RIGOUR));
            assertTrue(modified.getActivePrayers().contains(Prayer.AUGURY));
            assertFalse(modified.getActivePrayers().contains(Prayer.PIETY));
            assertEquals("Modified", modified.getName());

            // Original unchanged
            assertEquals(1, original.getActivePrayers().size());
            assertTrue(original.getActivePrayers().contains(Prayer.PIETY));
            assertEquals("Original", original.getName());
        }

        @Test
        public void testBuildCreatesValidSnapshot() {
            // Act
            GearSnapshot snapshot = new GearSnapshotBuilder()
                    .withName("Valid Snapshot")
                    .build();

            // Assert
            assertNotNull(snapshot);
            assertEquals("Valid Snapshot", snapshot.getName());
            assertEquals(14, snapshot.getEquippedItemIds().length);
            assertEquals(14, snapshot.getEquippedItemNames().length);
            assertNotNull(snapshot.getActivePrayers());
        }

        @Test
        public void testBuilderMethodChaining() {
            // Act - test that all methods return 'this' for chaining
            GearSnapshot snapshot = new GearSnapshotBuilder()
                    .withEquipment(0, 12345, "Item")
                    .withPrayer(Prayer.PIETY)
                    .withCombatStyleName("Slash")
                    .withCombatStyleStance("Accurate")
                    .withName("Chained")
                    .withAssumeMaxBoosts(true)
                    .withUseBestPrayer(false)
                    .withTimestamp(1000L)
                    .build();

            // Assert
            assertNotNull(snapshot);
            assertEquals("Chained", snapshot.getName());
        }

        @Test
        public void testBuilderDefaults() {
            // Act
            GearSnapshot snapshot = new GearSnapshotBuilder()
                    .withName("Defaults Test")
                    .build();

            // Assert - verify default values
            assertEquals("Defaults Test", snapshot.getName());
            assertEquals("Unknown", snapshot.getCombatStyleName());
            assertEquals("Unknown", snapshot.getCombatStyleStance());
            assertFalse(snapshot.isAssumeMaxBoosts());
            assertFalse(snapshot.isUseBestOffensivePrayer());
            assertTrue(snapshot.getActivePrayers().isEmpty());

            // All equipment slots should be -1 (empty)
            for (int id : snapshot.getEquippedItemIds()) {
                assertEquals(-1, id);
            }
        }
    }
}
