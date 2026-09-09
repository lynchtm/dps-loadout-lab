/* SPDX-License-Identifier: BSD-2-Clause; Copyright (c) 2026, Tommy Lynch. */
package com.loadoutlab.data;

import com.google.gson.*;
import com.loadoutlab.model.MonsterStats;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.inject.Singleton;

/** Independent Wiki fact snapshot; mutable drafts never escape into the catalog. */
@Singleton
public final class MonsterDataManager {
    private final List<MonsterStats> variants;

    public MonsterDataManager() {
        try (Reader r =
                new InputStreamReader(
                        Objects.requireNonNull(
                                getClass()
                                        .getResourceAsStream("/com/loadoutlab/target-facts.json")),
                        StandardCharsets.UTF_8)) {
            variants =
                    Collections.unmodifiableList(
                            Arrays.asList(
                                    net.runelite.http.api.RuneLiteAPI.GSON.fromJson(
                                            r, MonsterStats[].class)));
        } catch (IOException ex) {
            throw new IllegalStateException("Target facts could not be loaded", ex);
        }
    }

    public List<MonsterStats> getAllMonsters() {
        List<MonsterStats> copies = new ArrayList<>();
        for (MonsterStats m : variants) copies.add(m.copy());
        return copies;
    }

    public MonsterStats getMonster(int id) {
        for (MonsterStats m : variants) if (m.getId() == id) return m.copy();
        return null;
    }

    public MonsterStats getMonster(int id, String version) {
        for (MonsterStats m : variants)
            if (m.getId() == id && Objects.equals(m.getVersion(), version)) return m.copy();
        return getMonster(id);
    }

    public MonsterStats getMonster(String name) {
        for (MonsterStats m : variants) if (m.getName().equalsIgnoreCase(name)) return m.copy();
        return null;
    }
}
