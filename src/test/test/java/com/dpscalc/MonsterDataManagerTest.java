package com.dpscalc;

import com.dpscalc.data.MonsterDataManager;
import com.dpscalc.data.MonsterStats;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.lang.reflect.Method;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class MonsterDataManagerTest {
    @Test
    public void uniqueMonsterIdsRemainAvailable_whenInMemoryDataIsLoaded() throws Exception {
        // Given
        MonsterDataManager manager = managerWith(
            monsterJson(101, "Goblin", "", 1) + "," +
            monsterJson(202, "Guard", "Falador", 50)
        );

        // When
        MonsterStats goblin = manager.getMonster(101);
        Collection<MonsterStats> allMonsters = manager.getAllMonsters();

        // Then
        assertEquals("Goblin", goblin.getName());
        assertEquals(1, goblin.getDefenceLevel());
        assertEquals(2, manager.getMonsterCount());
        assertEquals(2, allMonsters.size());
    }

    @Test
    public void duplicateVersionsRemainInInputOrder_whenInMemoryDataIsLoaded() throws Exception {
        // Given
        MonsterDataManager manager = managerWith(maggotKingEntries());

        // When
        List<MonsterStats> versions = monsterVersions(manager, 15742);

        // Then
        assertEquals(3, versions.size());
        assertEquals("Far", versions.get(0).getVersion());
        assertEquals("Nearby", versions.get(1).getVersion());
        assertEquals("Roaring", versions.get(2).getVersion());
    }

    @Test
    public void firstInputEntryIsDefault_whenIdHasMultipleVersions() throws Exception {
        // Given
        MonsterDataManager manager = managerWith(maggotKingEntries());

        // When
        MonsterStats selected = manager.getMonster(15742);

        // Then
        assertEquals("Far", selected.getVersion());
        assertEquals(200, selected.getDefenceLevel());
    }

    @Test
    public void exactVersionLookupSelectsMatchingEntry_whenIdHasMultipleVersions() throws Exception {
        // Given
        MonsterDataManager manager = managerWith(maggotKingEntries());

        // When
        MonsterStats selected = monster(manager, 15742, "Roaring");

        // Then
        assertEquals("Roaring", selected.getVersion());
        assertEquals(100, selected.getDefenceLevel());
    }

    @Test
    public void unknownVersionFallsBackToFirstInputEntry() throws Exception {
        // Given
        MonsterDataManager manager = managerWith(
            monsterJson(15742, "Maggot King", "Nearby", 200) + "," +
            monsterJson(15742, "Maggot King", "Far", 200) + "," +
            monsterJson(15742, "Maggot King", "Roaring", 100)
        );

        // When
        MonsterStats selected = monster(manager, 15742, "Unknown");

        // Then
        assertEquals("Nearby", selected.getVersion());
    }

    @Test
    public void uniqueIdIgnoresRequestedVersion_whenOnlyOneEntryExists() throws Exception {
        // Given
        MonsterDataManager manager = managerWith(monsterJson(202, "Guard", "Falador", 50));

        // When
        MonsterStats defaultMonster = manager.getMonster(202);
        MonsterStats selected = monster(manager, 202, "Varrock");

        // Then
        assertSame(defaultMonster, selected);
        assertEquals("Falador", selected.getVersion());
    }

    @Test
    public void enumerationRetainsDuplicateIds_whenVersionsDiffer() throws Exception {
        // Given
        MonsterDataManager manager = managerWith(
            maggotKingEntries() + "," + monsterJson(202, "Guard", "Falador", 50)
        );

        // When
        Collection<MonsterStats> allMonsters = manager.getAllMonsters();

        // Then
        assertEquals(4, allMonsters.size());
    }

    @Test
    public void bundledPinnedResourceContainsMaggotKingVersions() throws Exception {
        // Given
        MonsterDataManager manager = new MonsterDataManager();
        JsonArray monsters;
        try (InputStream stream = MonsterDataManager.class.getResourceAsStream("/com/dpscalc/monsters.json")) {
            monsters = (new JsonParser()).parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonArray();
        }
        Method parse = MonsterDataManager.class.getDeclaredMethod("parseAndStoreMonsters", JsonArray.class);
        parse.setAccessible(true);

        // When
        parse.invoke(manager, monsters);
        setLoaded(manager);
        List<MonsterStats> versions = monsterVersions(manager, 15742);

        // Then
        assertEquals(3, versions.size());
        assertEquals("Far", versions.get(0).getVersion());
        assertEquals(200, versions.get(0).getDefenceLevel());
        assertEquals("Nearby", versions.get(1).getVersion());
        assertEquals(200, versions.get(1).getDefenceLevel());
        assertEquals("Roaring", versions.get(2).getVersion());
        assertEquals(100, versions.get(2).getDefenceLevel());
    }

    private static MonsterDataManager managerWith(String entries) throws Exception {
        MonsterDataManager manager = new MonsterDataManager();
        JsonArray monsters = (new JsonParser()).parse("[" + entries + "]").getAsJsonArray();
        Method parse = MonsterDataManager.class.getDeclaredMethod("parseAndStoreMonsters", JsonArray.class);
        parse.setAccessible(true);
        parse.invoke(manager, monsters);

        java.lang.reflect.Field loaded = MonsterDataManager.class.getDeclaredField("loaded");
        loaded.setAccessible(true);
        loaded.set(manager, true);
        return manager;
    }

    private static void setLoaded(MonsterDataManager manager) throws Exception {
        java.lang.reflect.Field loaded = MonsterDataManager.class.getDeclaredField("loaded");
        loaded.setAccessible(true);
        loaded.set(manager, true);
    }

    private static String monsterJson(int id, String name, String version, int defence) {
        return String.format(
            "{\"id\":%d,\"name\":\"%s\",\"version\":\"%s\",\"skills\":{\"def\":%d},\"defensive\":{}}",
            id,
            name,
            version,
            defence
        );
    }

    @SuppressWarnings("unchecked")
    private static List<MonsterStats> monsterVersions(MonsterDataManager manager, int id) throws Exception {
        Method lookup = MonsterDataManager.class.getMethod("getMonsterVersions", int.class);
        return (List<MonsterStats>) lookup.invoke(manager, id);
    }

    private static MonsterStats monster(MonsterDataManager manager, int id, String version) throws Exception {
        Method lookup = MonsterDataManager.class.getMethod("getMonster", int.class, String.class);
        return (MonsterStats) lookup.invoke(manager, id, version);
    }

    private static String maggotKingEntries() {
        return monsterJson(15742, "Maggot King", "Far", 200) + "," +
            monsterJson(15742, "Maggot King", "Nearby", 200) + "," +
            monsterJson(15742, "Maggot King", "Roaring", 100);
    }
}
