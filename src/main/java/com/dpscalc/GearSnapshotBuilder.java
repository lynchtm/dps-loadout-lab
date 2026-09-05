package com.dpscalc;

import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.GearSnapshot;
import com.dpscalc.state.Prayer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Fluent builder for creating GearSnapshot instances.
 *
 * <p>Key features:
 * <ul>
 *   <li>copyFrom() method for immutable snapshot updates (critical for editing snapshots)</li>
 *   <li>Fluent API - all methods return 'this' for chaining</li>
 *   <li>Sensible defaults - empty equipment, no prayers, "Unknown" style</li>
 *   <li>Validation in build() - ensures arrays are length 14, name not null/empty</li>
 * </ul>
 */
public class GearSnapshotBuilder {
    private int[] itemIds = new int[14];
    private String[] itemNames = new String[14];
    private Set<Prayer> prayers = EnumSet.noneOf(Prayer.class);
    private String combatStyleName = "Unknown";
    private String combatStyleStance = "Unknown";
    private String name;
    private boolean assumeMaxBoosts = false;
    private boolean useBestPrayer = false;
    private long timestamp = System.currentTimeMillis();

    /**
     * Initializes builder with default values:
     * - Empty equipment (itemIds filled with -1, itemNames filled with null)
     * - No prayers
     * - "Unknown" combat style
     * - Auto-generated name based on timestamp
     * - Current timestamp
     */
    public GearSnapshotBuilder() {
        // Initialize equipment arrays with empty slots
        Arrays.fill(itemIds, -1);
        Arrays.fill(itemNames, null);

        // Auto-generate name from timestamp
        this.name = "Snapshot " + timestamp;
    }

    /**
     * Copies all fields from an existing GearSnapshot.
     * CRITICAL for immutable snapshot updates - preserves all state including timestamp.
     *
     * @param source The snapshot to copy from
     * @return this builder for chaining
     */
    public GearSnapshotBuilder copyFrom(GearSnapshot source) {
        this.itemIds = source.getEquippedItemIds().clone();
        this.itemNames = source.getEquippedItemNames().clone();
        this.prayers.clear();
        this.prayers.addAll(source.getActivePrayers());
        this.combatStyleName = source.getCombatStyleName();
        this.combatStyleStance = source.getCombatStyleStance();
        this.name = source.getName();
        this.assumeMaxBoosts = source.isAssumeMaxBoosts();
        this.useBestPrayer = source.isUseBestOffensivePrayer();
        this.timestamp = source.getTimestamp();  // Preserve original timestamp for identity
        return this;
    }

    /**
     * Sets a single equipment slot.
     *
     * @param slot Equipment slot index (0-13, matching EquipmentSlot enum)
     * @param itemId Item ID (-1 for empty slot)
     * @param itemName Item name (null for empty slot)
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withEquipment(int slot, int itemId, String itemName) {
        if (slot < 0 || slot >= 14) {
            throw new IllegalArgumentException("Equipment slot must be 0-13, got: " + slot);
        }
        this.itemIds[slot] = itemId;
        this.itemNames[slot] = itemName;
        return this;
    }

    /**
     * Sets all equipment slots at once (bulk setter).
     * Arrays are defensively copied.
     *
     * @param ids 14-element array of item IDs
     * @param names 14-element array of item names
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withEquipment(int[] ids, String[] names) {
        if (ids == null || ids.length != 14) {
            throw new IllegalArgumentException("Item IDs must be a 14-element array");
        }
        if (names == null || names.length != 14) {
            throw new IllegalArgumentException("Item names must be a 14-element array");
        }
        this.itemIds = ids.clone();
        this.itemNames = names.clone();
        return this;
    }

    /**
     * Adds a single prayer to the active prayer set.
     *
     * @param prayer Prayer to add
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withPrayer(Prayer prayer) {
        this.prayers.add(prayer);
        return this;
    }

    /**
     * Replaces the entire prayer set with the given prayers (varargs).
     *
     * @param prayers Prayers to activate (replaces existing set)
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withPrayers(Prayer... prayers) {
        this.prayers.clear();
        this.prayers.addAll(Arrays.asList(prayers));
        return this;
    }

    /**
     * Sets the combat style name (e.g., "Slash", "Stab", "Accurate").
     *
     * @param name Combat style name
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withCombatStyleName(String name) {
        this.combatStyleName = name;
        return this;
    }

    /**
     * Sets the combat style stance (e.g., "Accurate", "Aggressive", "Defensive").
     *
     * @param stance Combat style stance
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withCombatStyleStance(String stance) {
        this.combatStyleStance = stance;
        return this;
    }

    /**
     * Convenience method to set both combat style name and stance from a CombatStyle object.
     *
     * @param style CombatStyle to extract name and stance from
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withCombatStyle(CombatStyle style) {
        this.combatStyleName = style.getName();
        this.combatStyleStance = style.getStance();
        return this;
    }

    /**
     * Sets the snapshot name (user-provided or auto-generated).
     *
     * @param name Snapshot name
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets whether to assume max stat boosts (+5/+6).
     *
     * @param assume True to assume max boosts
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withAssumeMaxBoosts(boolean assume) {
        this.assumeMaxBoosts = assume;
        return this;
    }

    /**
     * Sets whether to auto-select best offensive prayer.
     *
     * @param useBest True to use best prayer
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withUseBestPrayer(boolean useBest) {
        this.useBestPrayer = useBest;
        return this;
    }

    /**
     * Sets the snapshot timestamp (milliseconds since epoch).
     * Useful when copying snapshots to preserve original timestamp.
     *
     * @param timestamp Timestamp in milliseconds
     * @return this builder for chaining
     */
    public GearSnapshotBuilder withTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    /**
     * Builds the immutable GearSnapshot.
     * Validates that arrays are length 14 and name is not null/empty.
     *
     * @return New GearSnapshot instance
     */
    public GearSnapshot build() {
        // Validation happens in GearSnapshot constructor
        // Convert mutable prayer set to EnumSet (defensive copy)
        Set<Prayer> prayersCopy = prayers.isEmpty()
                ? EnumSet.noneOf(Prayer.class)
                : EnumSet.copyOf(prayers);

        return new GearSnapshot(
                itemIds,
                itemNames,
                prayersCopy,
                combatStyleName,
                combatStyleStance,
                name,
                assumeMaxBoosts,
                useBestPrayer,
                timestamp
        );
    }
}
