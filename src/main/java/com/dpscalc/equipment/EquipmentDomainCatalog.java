package com.dpscalc.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EquipmentDomainCatalog {
    public Map<Integer, EquipmentCatalogItem> getItems() { return equipmentById; }
    private final int equipmentCount;
    private final int aliasGroupCount;
    private final Map<Integer, Integer> canonicalById;
    private final Map<Integer, Set<Integer>> includedAmmoByWeapon;
    private final Map<Integer, EquipmentCatalogItem> equipmentById;

    private EquipmentDomainCatalog(int equipmentCount, int aliasGroupCount, CatalogRules rules) {
        this.equipmentCount = equipmentCount;
        this.aliasGroupCount = aliasGroupCount;
        this.canonicalById = Collections.unmodifiableMap(new HashMap<>(rules.canonicalById));
        Map<Integer, Set<Integer>> ammoCopy = new HashMap<>();
        rules.includedAmmoByWeapon.forEach((id, ammo) -> ammoCopy.put(id, Collections.unmodifiableSet(new HashSet<>(ammo))));
        this.includedAmmoByWeapon = Collections.unmodifiableMap(ammoCopy);
        this.equipmentById = Collections.unmodifiableMap(new HashMap<>(rules.equipmentById));
    }

    public static EquipmentDomainCatalog load(InputStream stream, String expectedSha,
                                               String expectedDigest) throws IOException {
        if (stream == null) throw new EquipmentDomainException("resource", "equipment domain missing");
        final JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonElement parsed = (new JsonParser()).parse(reader);
            if (!parsed.isJsonObject()) throw new EquipmentDomainException("$", "expected object");
            root = parsed.getAsJsonObject();
        } catch (JsonParseException error) {
            throw new EquipmentDomainException("$", "malformed JSON", error);
        }
        requireMetadata(root, expectedSha, expectedDigest);
        return fromJson(root);
    }

    private static void requireMetadata(JsonObject root, String expectedSha, String expectedDigest) {
        if (!root.has("schemaVersion") || root.get("schemaVersion").getAsInt() != 1) {
            throw new EquipmentDomainException("$.schemaVersion", "expected 1");
        }
        if (!root.has("sourceSha") || !expectedSha.equals(root.get("sourceSha").getAsString())) {
            throw new EquipmentDomainException("$.sourceSha", "stale reference SHA");
        }
        if (!root.has("contentDigest") || !expectedDigest.equals(root.get("contentDigest").getAsString())) {
            throw new EquipmentDomainException("$.contentDigest", "stale equipment digest");
        }
    }

    private static EquipmentDomainCatalog fromJson(JsonObject root) {
        JsonArray equipment = requiredArray(root, "equipment");
        JsonArray aliases = requiredArray(root, "aliases");
        JsonArray weaponAmmo = requiredArray(root, "weaponAmmo");
        if (equipment.size() == 0) throw new EquipmentDomainException("$.equipment", "must not be empty");

        Map<Integer, EquipmentCatalogItem> equipmentById = new HashMap<>();
        for (int index = 0; index < equipment.size(); index++) {
            JsonObject item = equipment.get(index).getAsJsonObject();
            int id = item.get("id").getAsInt();
            EquipmentItemIdentity identity = new EquipmentItemIdentity(item);
            if (equipmentById.put(id, new EquipmentCatalogItem(identity, item.get("speed").getAsInt(), stats(item))) != null) {
                throw new EquipmentDomainException("$.equipment[" + index + "].id", "duplicate " + id);
            }
        }

        Map<Integer, Integer> canonicalById = new HashMap<>();
        Set<Integer> aliasGroups = new HashSet<>();
        for (int index = 0; index < aliases.size(); index++) {
            JsonObject alias = aliases.get(index).getAsJsonObject();
            int canonicalId = alias.get("canonicalId").getAsInt();
            if (!aliasGroups.add(canonicalId)) throw new EquipmentDomainException("$.aliases[" + index + "].canonicalId", "duplicate " + canonicalId);
            registerAlias(canonicalById, canonicalId, canonicalId);
            for (JsonElement variant : alias.getAsJsonArray("variantIds")) {
                registerAlias(canonicalById, variant.getAsInt(), canonicalId);
            }
        }

        Map<Integer, Set<Integer>> ammoByWeapon = new HashMap<>();
        for (int index = 0; index < weaponAmmo.size(); index++) {
            JsonObject rule = weaponAmmo.get(index).getAsJsonObject();
            int weaponId = rule.get("weaponId").getAsInt();
            Set<Integer> ammoIds = new HashSet<>();
            for (JsonElement ammo : rule.getAsJsonArray("includedAmmoIds")) ammoIds.add(ammo.getAsInt());
            if (ammoByWeapon.put(weaponId, ammoIds) != null) {
                throw new EquipmentDomainException("$.weaponAmmo[" + index + "].weaponId", "duplicate " + weaponId);
            }
        }
        return new EquipmentDomainCatalog(equipment.size(), aliases.size(), new CatalogRules(canonicalById, ammoByWeapon, equipmentById));
    }

    private static void registerAlias(Map<Integer, Integer> aliases, int originalId, int canonicalId) {
        aliases.putIfAbsent(originalId, canonicalId);
    }

    private static JsonArray requiredArray(JsonObject root, String field) {
        JsonElement value = root.get(field);
        if (value == null || !value.isJsonArray()) throw new EquipmentDomainException("$." + field, "expected array");
        return value.getAsJsonArray();
    }

    public EquipmentItem canonicalize(EquipmentItem item) {
        if (item == null) return null;
        int canonicalId = canonicalById.getOrDefault(item.getOriginalId(), item.getOriginalId());
        return item.canonicalized(canonicalId);
    }

    public EquipmentLoadout canonicalize(EquipmentLoadout loadout) {
        Map<EquipmentSlot, EquipmentItem> canonical = new HashMap<>();
        loadout.asMap().forEach((slot, item) -> canonical.put(slot, canonicalize(item)));
        return EquipmentLoadout.of(canonical);
    }

    public AmmoApplicabilityResult evaluate(EquipmentItem weapon, EquipmentItem ammo) {
        EquipmentItem canonicalWeapon = canonicalize(weapon);
        EquipmentItem canonicalAmmo = canonicalize(ammo);
        Set<Integer> validAmmo = canonicalWeapon == null ? null : includedAmmoByWeapon.get(canonicalWeapon.getCanonicalId());
        AmmoApplicability applicability;
        if (validAmmo == null || validAmmo.isEmpty()) applicability = AmmoApplicability.ALLOWED;
        else if (canonicalAmmo != null && validAmmo.contains(canonicalAmmo.getCanonicalId())) applicability = AmmoApplicability.INCLUDED;
        else applicability = AmmoApplicability.INVALID;
        return new AmmoApplicabilityResult(applicability, canonicalWeapon, canonicalAmmo);
    }

    public EquipmentCatalogItem getItem(EquipmentItem item) {
        if (item == null) return null;
        return equipmentById.get(canonicalize(item).getCanonicalId());
    }

    EquipmentCatalogItem requireItem(EquipmentItem item) {
        EquipmentCatalogItem found = getItem(item);
        if (found == null) {
            int id = item == null ? -1 : item.getOriginalId();
            throw new EquipmentDomainException("$.equipment[" + id + "]", "item not found");
        }
        return found;
    }

    private static EquipmentStatTotals stats(JsonObject item) {
        int[] values = new int[EquipmentStatTotals.COUNT];
        values[EquipmentStatTotals.STAB_ATTACK] = stat(item, "offensive", "stab");
        values[EquipmentStatTotals.SLASH_ATTACK] = stat(item, "offensive", "slash");
        values[EquipmentStatTotals.CRUSH_ATTACK] = stat(item, "offensive", "crush");
        values[EquipmentStatTotals.MAGIC_ATTACK] = stat(item, "offensive", "magic");
        values[EquipmentStatTotals.RANGED_ATTACK] = stat(item, "offensive", "ranged");
        values[EquipmentStatTotals.MELEE_STRENGTH] = stat(item, "bonuses", "str");
        values[EquipmentStatTotals.RANGED_STRENGTH] = stat(item, "bonuses", "ranged_str");
        values[EquipmentStatTotals.MAGIC_DAMAGE] = stat(item, "bonuses", "magic_str");
        values[EquipmentStatTotals.PRAYER] = stat(item, "bonuses", "prayer");
        values[EquipmentStatTotals.STAB_DEFENCE] = stat(item, "defensive", "stab");
        values[EquipmentStatTotals.SLASH_DEFENCE] = stat(item, "defensive", "slash");
        values[EquipmentStatTotals.CRUSH_DEFENCE] = stat(item, "defensive", "crush");
        values[EquipmentStatTotals.MAGIC_DEFENCE] = stat(item, "defensive", "magic");
        values[EquipmentStatTotals.RANGED_DEFENCE] = stat(item, "defensive", "ranged");
        return new EquipmentStatTotals(values);
    }

    private static int stat(JsonObject item, String block, String field) {
        JsonElement value = item.getAsJsonObject(block).get(field);
        return value == null || value.isJsonNull() ? 0 : value.getAsInt();
    }

    public int getEquipmentCount() { return equipmentCount; }
    public int getAliasGroupCount() { return aliasGroupCount; }
    public int getWeaponRuleCount() { return includedAmmoByWeapon.size(); }

    private static final class CatalogRules {
        private final Map<Integer, Integer> canonicalById;
        private final Map<Integer, Set<Integer>> includedAmmoByWeapon;
        private final Map<Integer, EquipmentCatalogItem> equipmentById;

        private CatalogRules(Map<Integer, Integer> canonicalById,
                             Map<Integer, Set<Integer>> includedAmmoByWeapon,
                             Map<Integer, EquipmentCatalogItem> equipmentById) {
            this.canonicalById = canonicalById;
            this.includedAmmoByWeapon = includedAmmoByWeapon;
            this.equipmentById = equipmentById;
        }
    }
}
