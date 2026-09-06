package com.dpscalc.data;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Singleton
public class MonsterDataManager {
    private static final Logger log = LoggerFactory.getLogger(MonsterDataManager.class);

    private volatile MonsterCatalog catalog = MonsterCatalog.empty();
    @Inject private Gson gson = net.runelite.http.api.RuneLiteAPI.GSON;
    private volatile boolean loaded;

    public synchronized void loadMonsters() {
        if (!loaded) loadFromBundledResource();
    }

    private void loadFromBundledResource() {
        try (InputStream is = getClass().getResourceAsStream("/com/dpscalc/monsters.json")) {
            if (is == null) {
                log.error("Could not find bundled monsters.json resource");
                return;
            }

            JsonArray monsters = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
            parseAndStoreMonsters(monsters);
            loaded = true;
            log.info("Loaded {} monster IDs from bundled resource", catalog.uniqueIdCount());
        } catch (Exception e) {
            log.error("Failed to load bundled monsters.json", e);
        }
    }

    private void parseAndStoreMonsters(JsonArray monsters) {
        List<MonsterStats> newMonsters = new ArrayList<>();

        for (JsonElement element : monsters) {
            JsonObject obj = element.getAsJsonObject();
            MonsterStats stats = parseMonster(obj);
            if (stats != null && stats.getId() > 0) {
                newMonsters.add(stats);
            }
        }

        catalog = MonsterCatalog.from(newMonsters);
    }

    private MonsterStats parseMonster(JsonObject obj) {
        MonsterStats stats = new MonsterStats();

        stats.setId(getIntSafe(obj, "id"));
        stats.setName(getStringSafe(obj, "name"));
        stats.setVersion(getStringSafe(obj, "version"));
        stats.setSize(getIntSafe(obj, "size"));
        stats.setSpeed(getIntSafe(obj, "speed"));

        JsonObject skills = obj.getAsJsonObject("skills");
        if (skills != null) {
            stats.setAttackLevel(getIntSafe(skills, "atk"));
            stats.setStrengthLevel(getIntSafe(skills, "str"));
            stats.setDefenceLevel(getIntSafe(skills, "def"));
            stats.setHitpoints(getIntSafe(skills, "hp"));
            stats.setMagicLevel(getIntSafe(skills, "magic"));
            stats.setRangedLevel(getIntSafe(skills, "ranged"));
        }

        JsonObject defensive = obj.getAsJsonObject("defensive");
        if (defensive != null) {
            stats.setStabDefence(getIntSafe(defensive, "stab"));
            stats.setSlashDefence(getIntSafe(defensive, "slash"));
            stats.setCrushDefence(getIntSafe(defensive, "crush"));
            stats.setMagicDefence(getIntSafe(defensive, "magic"));
            stats.setLightRangedDefence(getIntSafe(defensive, "light"));
            stats.setStandardRangedDefence(getIntSafe(defensive, "standard"));
            stats.setHeavyRangedDefence(getIntSafe(defensive, "heavy"));
            stats.setFlatArmour(getIntSafe(defensive, "flat_armour"));
        }

        JsonArray attributes = obj.getAsJsonArray("attributes");
        if (attributes != null) {
            for (JsonElement attr : attributes) {
                MonsterAttribute monsterAttr = MonsterAttribute.fromJson(attr.getAsString());
                stats.addAttribute(monsterAttr);
            }
        }

        JsonElement weaknessEl = obj.get("weakness");
        if (weaknessEl != null && !weaknessEl.isJsonNull() && weaknessEl.isJsonObject()) {
            JsonObject weakness = weaknessEl.getAsJsonObject();
            stats.setWeaknessElement(WeaknessElement.fromJson(getStringSafe(weakness, "element")));
            stats.setWeaknessSeverity(getIntSafe(weakness, "severity"));
        }

        return stats;
    }

    private int getIntSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return 0;
        try {
            return el.getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    private String getStringSafe(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        try {
            return el.getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    public MonsterStats getMonster(int npcId) {
        return catalog.get(npcId, null);
    }

    public MonsterStats getMonster(int npcId, String version) {
        return catalog.get(npcId, version);
    }

    public List<MonsterStats> getMonsterVersions(int npcId) {
        return catalog.versions(npcId);
    }

    public boolean hasMonster(int npcId) {
        return catalog.contains(npcId);
    }

    public int getMonsterCount() {
        return catalog.uniqueIdCount();
    }

    public Collection<MonsterStats> getAllMonsters() {
        return catalog.all();
    }

    public void forceRefresh() {
        loadMonsters();
    }

    public boolean isLoaded() { return loaded; }

    public boolean isUpdatedFromRemote() { return false; }
}
