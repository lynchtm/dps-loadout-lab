package com.dpscalc.scenario;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.*;
import net.runelite.client.plugins.banktags.tabs.*;
import net.runelite.client.util.Text;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.*;
import javax.swing.SwingUtilities;

/**
 * Writes only new, explicitly requested tags on the client thread. Existing tags are never
 * replaced.
 */
@Singleton
public final class BankLayoutService {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(BankLayoutService.class);
    private final Client client;
    private final ClientThread thread;
    private final ConfigManager config;
    private final PluginManager plugins;

    @Inject
    public BankLayoutService(
            Client client, ClientThread thread, ConfigManager config, PluginManager plugins) {
        this.client = client;
        this.thread = thread;
        this.config = config;
        this.plugins = plugins;
    }

    public static final class Result {
        public final boolean success;
        public final String message;

        public Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public void create(
            String raw,
            BankLayoutPlan plan,
            String profile,
            long configProfile,
            Consumer<Result> done) {
        String name = BankLayoutPlan.tagName(raw);
        thread.invokeLater(
                () -> {
                    Result result;
                    try {
                        BankTagsPlugin bankTags =
                                plugins.getPlugins().stream()
                                        .filter(p -> p instanceof BankTagsPlugin)
                                        .map(p -> (BankTagsPlugin) p)
                                        .findFirst()
                                        .orElse(null);
                        if (bankTags == null
                                || !plugins.isPluginActive(bankTags)
                                || !plugins.isPluginEnabled(bankTags))
                            throw new IllegalStateException(
                                    "Enable RuneLite's Bank Tags plugin first.");
                        // Core plugins have their own child injectors. Reuse the active plugin's
                        // services; constructing these in our scope would create an unloaded bank
                        // UI.
                        TagManager tags = bankTags.getInjector().getInstance(TagManager.class);
                        TabManager tabs = bankTags.getInjector().getInstance(TabManager.class);
                        LayoutManager layouts =
                                bankTags.getInjector().getInstance(LayoutManager.class);
                        net.runelite.api.widgets.Widget bank =
                                client.getWidget(InterfaceID.Bankmain.ITEMS_CONTAINER);
                        BankLayoutWriter.requireLoaded(
                                bank != null && !bank.isHidden(),
                                config.getConfig(BankTagsConfig.class).tabs(),
                                Text.fromCSV(
                                        Objects.toString(
                                                config.getConfiguration(
                                                        BankTagsPlugin.CONFIG_GROUP,
                                                        BankTagsPlugin.TAG_TABS_CONFIG),
                                                "")),
                                tabs.getTabs().stream()
                                        .map(TagTab::getTag)
                                        .collect(Collectors.toList()));
                        BankLayoutWriter.create(
                                new BankLayoutWriter.Backend() {
                                    public boolean currentProfile() {
                                        return profile != null
                                                && profile.equals(config.getRSProfileKey())
                                                && configProfile == config.getProfile().getId();
                                    }

                                    public boolean enabled() {
                                        return plugins.isPluginEnabled(bankTags)
                                                && plugins.isPluginActive(bankTags);
                                    }

                                    public boolean exists(String tag) {
                                        return tabs.find(tag) != null
                                                || layouts.loadLayout(tag) != null
                                                || !tags.getItemsForTag(tag).isEmpty();
                                    }

                                    public void tag(int id, String tag) {
                                        tags.addTag(id, tag, false);
                                    }

                                    public void layout(String tag, BankLayoutPlan plan) {
                                        Layout layout = new Layout(tag);
                                        plan.positions.forEach(
                                                (pos, id) -> layout.setItemAtPos(id, pos));
                                        layouts.saveLayout(layout);
                                    }

                                    public void tab(String tag, int icon) {
                                        TagTab tab = new TagTab();
                                        tab.setTag(tag);
                                        tab.setIconItemId(icon);
                                        tabs.add(tab);
                                    }

                                    public void save() {
                                        tabs.save();
                                    }

                                    public void removeNew(String tag) {
                                        tags.removeTag(tag);
                                        layouts.removeLayout(tag);
                                        tabs.remove(tag);
                                        tabs.save();
                                    }
                                },
                                name,
                                plan);
                        try {
                            bankTags.openBankTag(name);
                            result = new Result(true, "Created and opened bank tag: " + name);
                        } catch (RuntimeException error) {
                            log.warn("Created bank layout but could not open its tab", error);
                            result =
                                    new Result(
                                            true,
                                            "Created bank tag: "
                                                    + name
                                                    + ". Reopen your bank to view it.");
                        }
                    } catch (RuntimeException error) {
                        log.debug("Bank layout creation did not complete", error);
                        result =
                                new Result(
                                        false,
                                        Objects.toString(
                                                error.getMessage(),
                                                "Bank layout creation failed. Please try again."));
                    }
                    Result completed = result;
                    SwingUtilities.invokeLater(() -> done.accept(completed));
                });
    }
}
