package com.dpscalc.scenario;

import java.util.Objects;

/** Immediate, deduplicated writes: no pending draft can be lost at a profile switch. */
final class WorkspacePersistence {
    private final ScenarioStorage storage;
    private String lastKey, lastJson;

    WorkspacePersistence(ScenarioStorage storage) {
        this.storage = storage;
    }

    void save(Scenario scenario, String profile, long configProfile) {
        if (configProfile != storage.profileId()
                || !Objects.equals(profile, storage.getRSProfileKey())) return;
        String key = configProfile + "/" + profile;
        String json = Scenario.JSON.toJson(scenario);
        if (key.equals(lastKey) && json.equals(lastJson)) return;
        storage.setConfiguration("wikiDpsScenarios", profile, "workspaceV1", json);
        lastKey = key;
        lastJson = json;
    }

    void reset() {
        lastKey = null;
        lastJson = null;
    }
}
