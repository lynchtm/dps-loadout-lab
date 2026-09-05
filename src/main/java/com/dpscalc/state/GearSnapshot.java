package com.dpscalc.state;


import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot of a player's gear loadout at a specific point in time.
 * Used for comparing DPS across different equipment configurations.
 *
 * <p>Identity is based on timestamp - each snapshot is unique by creation time.
 * Arrays are defensively copied to ensure immutability.
 */

public class GearSnapshot {
    private final int[] equippedItemIds;
    private final String[] equippedItemNames;
    private final Set<Prayer> activePrayers;
    private final String combatStyleName;
    private final String combatStyleStance;
    private final String name;
    private final boolean assumeMaxBoosts;
    private final boolean useBestOffensivePrayer;
    private final long timestamp;

    /**
     * Creates a new gear snapshot with defensive copying of arrays.
     *
     * @param equippedItemIds 14-slot array of item IDs (matching EquipmentSlot indices)
     * @param equippedItemNames 14-slot array of item names
     * @param activePrayers Set of active prayers (null becomes empty set)
     * @param combatStyleName Combat style name (e.g., "Slash", "Stab")
     * @param combatStyleStance Combat style stance (e.g., "Accurate", "Aggressive")
     * @param name User-provided or auto-generated snapshot name
     * @param assumeMaxBoosts Whether to assume max stat boosts (+5/+6)
     * @param useBestOffensivePrayer Whether to auto-select best offensive prayer
     * @param timestamp Creation timestamp (milliseconds since epoch)
     * @throws IllegalArgumentException if arrays are not length 14 or name is null/empty
     */
    public GearSnapshot(
            int[] equippedItemIds,
            String[] equippedItemNames,
            Set<Prayer> activePrayers,
            String combatStyleName,
            String combatStyleStance,
            String name,
            boolean assumeMaxBoosts,
            boolean useBestOffensivePrayer,
            long timestamp
    ) {
        // Validate array lengths
        if (equippedItemIds == null || equippedItemIds.length != 14) {
            throw new IllegalArgumentException("equippedItemIds must be a 14-element array");
        }
        if (equippedItemNames == null || equippedItemNames.length != 14) {
            throw new IllegalArgumentException("equippedItemNames must be a 14-element array");
        }

        // Validate name
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be null or empty");
        }

        // Defensive copy arrays
        this.equippedItemIds = equippedItemIds.clone();
        this.equippedItemNames = equippedItemNames.clone();

        // Defensive copy prayer set (never null)
        this.activePrayers = activePrayers == null
                ? EnumSet.noneOf(Prayer.class)
                : EnumSet.copyOf(activePrayers);

        this.combatStyleName = combatStyleName;
        this.combatStyleStance = combatStyleStance;
        this.name = name;
        this.assumeMaxBoosts = assumeMaxBoosts;
        this.useBestOffensivePrayer = useBestOffensivePrayer;
        this.timestamp = timestamp;
    }

    /**
     * Equality based on timestamp only - each snapshot is unique by creation time.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GearSnapshot that = (GearSnapshot) o;
        return timestamp == that.timestamp;
    }

    /**
     * Hash code based on timestamp only.
     */
    @Override
    public int hashCode() {
        return Objects.hash(timestamp);
    }

    @Override
    public String toString() {
        return "GearSnapshot{" +
                "name='" + name + '\'' +
                ", combatStyle=" + combatStyleName + "/" + combatStyleStance +
                ", prayers=" + activePrayers.size() +
                ", timestamp=" + timestamp +
                '}';
    }

    public int[] getEquippedItemIds() { return equippedItemIds; }

    public String[] getEquippedItemNames() { return equippedItemNames; }

    public Set<Prayer> getActivePrayers() { return activePrayers; }

    public String getCombatStyleName() { return combatStyleName; }

    public String getCombatStyleStance() { return combatStyleStance; }

    public String getName() { return name; }

    public boolean isAssumeMaxBoosts() { return assumeMaxBoosts; }

    public boolean isUseBestOffensivePrayer() { return useBestOffensivePrayer; }

    public long getTimestamp() { return timestamp; }
}
