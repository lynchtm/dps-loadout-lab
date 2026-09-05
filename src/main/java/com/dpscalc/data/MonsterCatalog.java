package com.dpscalc.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class MonsterCatalog {
    private static final MonsterCatalog EMPTY = new MonsterCatalog(
        Collections.emptyList(),
        Collections.emptyMap()
    );

    private final List<MonsterStats> monsters;
    private final Map<Integer, List<MonsterStats>> versionsById;

    private MonsterCatalog(
        List<MonsterStats> monsters,
        Map<Integer, List<MonsterStats>> versionsById
    ) {
        this.monsters = monsters;
        this.versionsById = versionsById;
    }

    static MonsterCatalog empty() {
        return EMPTY;
    }

    static MonsterCatalog from(List<MonsterStats> monsters) {
        List<MonsterStats> ordered = Collections.unmodifiableList(new ArrayList<>(monsters));
        Map<Integer, List<MonsterStats>> indexed = new LinkedHashMap<>();
        for (MonsterStats monster : ordered) {
            indexed.computeIfAbsent(monster.getId(), ignored -> new ArrayList<>()).add(monster);
        }

        Map<Integer, List<MonsterStats>> immutableIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<MonsterStats>> entry : indexed.entrySet()) {
            immutableIndex.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return new MonsterCatalog(ordered, Collections.unmodifiableMap(immutableIndex));
    }

    MonsterStats get(int id, String version) {
        List<MonsterStats> versions = versions(id);
        if (versions.isEmpty()) {
            return null;
        }
        if (versions.size() > 1 && version != null && !version.isEmpty()) {
            for (MonsterStats monster : versions) {
                if (version.equals(monster.getVersion())) {
                    return monster;
                }
            }
        }
        return versions.get(0);
    }

    List<MonsterStats> versions(int id) {
        return versionsById.getOrDefault(id, Collections.emptyList());
    }

    List<MonsterStats> all() {
        return monsters;
    }

    boolean contains(int id) {
        return versionsById.containsKey(id);
    }

    int uniqueIdCount() {
        return versionsById.size();
    }
}
