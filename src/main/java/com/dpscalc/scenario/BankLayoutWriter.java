package com.dpscalc.scenario;

import java.util.*;

/** Transaction boundary, tested without a game client. Only new names may be written. */
final class BankLayoutWriter {
    static void requireLoaded(
            boolean bankOpen, boolean tabsEnabled, List<String> saved, List<String> loaded) {
        if (!bankOpen) throw new IllegalStateException("Open your bank, then click Create again.");
        if (!tabsEnabled)
            throw new IllegalStateException(
                    "Enable Use Tag Tabs in RuneLite's Bank Tags settings, then reopen your bank.");
        if (!saved.equals(loaded))
            throw new IllegalStateException(
                    "Bank Tags has not loaded all your saved tabs. Close and reopen your bank, then"
                            + " try again.");
    }

    interface Backend {
        boolean currentProfile();

        boolean enabled();

        boolean exists(String name);

        void tag(int id, String name);

        void layout(String name, BankLayoutPlan plan);

        void tab(String name, int icon);

        void save();

        void removeNew(String name);
    }

    static void create(Backend backend, String name, BankLayoutPlan plan) {
        if (!backend.currentProfile())
            throw new IllegalStateException(
                    "Character or RuneLite profile changed. Reopen the layout preview.");
        if (!backend.enabled())
            throw new IllegalStateException("Enable RuneLite's Bank Tags plugin first.");
        if (backend.exists(name))
            throw new IllegalStateException(
                    "That bank tag already exists. Choose a new name to keep its layout and items"
                            + " safe.");
        try {
            for (int id : new LinkedHashSet<>(plan.positions.values())) backend.tag(id, name);
            backend.layout(name, plan);
            backend.tab(name, plan.positions.values().iterator().next());
            backend.save();
        } catch (RuntimeException error) {
            try {
                backend.removeNew(name);
            } catch (RuntimeException rollback) {
                error.addSuppressed(rollback);
            }
            throw error;
        }
    }
}
