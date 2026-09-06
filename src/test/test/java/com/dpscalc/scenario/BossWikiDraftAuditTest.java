package com.dpscalc.scenario;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BossWikiDraftAuditTest {
    @Test
    public void everyPinnedBossChoiceConvertsAndPreservesItsInventory() throws Exception {
        Path corpus =
                Paths.get(getClass().getResource("/boss-wiki/manifest.json").toURI()).getParent();
        BossWikiAudit.main(
                new String[] {
                    corpus.toString(),
                    Paths.get("build/boss-wiki-audit.json").toAbsolutePath().toString()
                });
    }
}
