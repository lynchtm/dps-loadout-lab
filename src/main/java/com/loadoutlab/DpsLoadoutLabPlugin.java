// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Tommy Lynch.
package com.loadoutlab;

import com.dpscalc.scenario.*;
import com.google.inject.Provides;
import com.loadoutlab.data.MonsterDataManager;
import com.loadoutlab.equipment.*;
import com.loadoutlab.model.MonsterStats;
import com.loadoutlab.model.PlayerState;

import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.*;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.*;
import java.util.concurrent.*;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

/** Read-only client capture; all catalog loading and calculation runs off the client thread. */
@PluginDescriptor(
        name = "DPS Loadout Lab",
        description = "Compare and prepare combat loadouts for a selected target",
        tags = {"dps", "bank", "loadout", "pvm"})
public class DpsLoadoutLabPlugin extends Plugin {
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(DpsLoadoutLabPlugin.class);
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private ClientToolbar toolbar;
    @Inject private ConfigManager configManager;
    @Inject private DpsLoadoutLabConfig config;
    @Inject private ItemManager itemManager;
    @Inject private SpriteManager spriteManager;
    @Inject private BankCapture bankCapture;
    @Inject private SetupItemIndex setupItemIndex;
    @Inject private WikiSetupService wikiSetupService;
    @Inject private BankLayoutService bankLayoutService;
    @Inject private OverlayManager overlays;
    private ExecutorService worker;
    private volatile EquipmentPreparationFacade equipment;
    private volatile MonsterDataManager monsters;
    private volatile PlayerState player;
    private volatile MonsterStats target;
    private volatile ScenarioCalculator.Result liveResult, comparison;
    private volatile String comparisonTarget;
    private volatile int[] inventory = new int[0];
    private volatile boolean running;
    private volatile long generation;
    private volatile long lifecycle;
    private volatile ScenarioPanel panel;
    private NavigationButton navigation;
    private LabOverlay overlay;
    private NPC targetNpc;
    private int lastTargetTick, firstDamageTick = -1, totalDamage;
    private Future<?> calculation;

    @Provides
    DpsLoadoutLabConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(DpsLoadoutLabConfig.class);
    }

    @Override
    protected void startUp() {
        running = true;
        long token = ++lifecycle;
        generation++;
        totalDamage = 0;
        firstDamageTick = -1;
        UiIcons.useClientSprites(spriteManager);
        worker =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread t = new Thread(r, "dps-loadout-lab");
                            t.setDaemon(true);
                            return t;
                        });
        overlay = new LabOverlay(this, config);
        overlays.add(overlay);
        setupItemIndex.start();
        worker.submit(
                () -> {
                    try {
                        EquipmentPreparationFacade facts = new EquipmentPreparationFacade();
                        MonsterDataManager catalog = new MonsterDataManager();
                        if (!running || lifecycle != token) return;
                        equipment = facts;
                        monsters = catalog;
                        SwingUtilities.invokeLater(
                                () -> {
                                    if (!running || lifecycle != token) return;
                                    panel = new ScenarioPanel(this, catalog, configManager, facts);
                                    navigation =
                                            NavigationButton.builder()
                                                    .tooltip("DPS Loadout Lab")
                                                    .icon(UiIcons.image("logo", 24))
                                                    .priority(8)
                                                    .panel(panel)
                                                    .build();
                                    if (config.showPanel()) toolbar.addNavigation(navigation);
                                    LOG.info(
                                            "DPS Loadout Lab independent engine and sidebar ready");
                                });
                        clientThread.invokeLater(
                                () -> {
                                    if (running && lifecycle == token) {
                                        bankCapture.playerChanged();
                                        capture();
                                    }
                                });
                    } catch (RuntimeException failure) {
                        LOG.error("Unable to initialize DPS Loadout Lab catalogs", failure);
                    }
                });
    }

    @Override
    protected void shutDown() {
        running = false;
        lifecycle++;
        generation++;
        UiIcons.useClientSprites(null);
        if (worker != null) worker.shutdownNow();
        setupItemIndex.stop();
        bankCapture.clear();
        if (overlay != null) overlays.remove(overlay);
        ScenarioPanel old = panel;
        NavigationButton nav = navigation;
        panel = null;
        navigation = null;
        SwingUtilities.invokeLater(
                () -> {
                    if (nav != null) toolbar.removeNavigation(nav);
                    if (old != null) old.dispose();
                });
        player = null;
        target = null;
        targetNpc = null;
        inventory = new int[0];
        liveResult = null;
        comparison = null;
        equipment = null;
        monsters = null;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals("dpscalc")) return;
        SwingUtilities.invokeLater(
                () -> {
                    if (navigation != null) {
                        toolbar.removeNavigation(navigation);
                        if (config.showPanel()) toolbar.addNavigation(navigation);
                    }
                });
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        bankCapture.changed(event);
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        bankCapture.playerChanged();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN
                || event.getGameState() == GameState.HOPPING) {
            generation++;
            player = null;
            target = null;
            targetNpc = null;
            liveResult = null;
            comparison = null;
            inventory = new int[0];
            bankCapture.clear();
            totalDamage = 0;
            firstDamageTick = -1;
            SwingUtilities.invokeLater(
                    () -> {
                        if (panel != null) panel.refreshClientState();
                    });
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (!running) return;
        bankCapture.tick();
        if (equipment == null || monsters == null) return;
        if (config.automaticRefresh() || player == null) capture();
        Player local = client.getLocalPlayer();
        Actor interacting = local == null ? null : local.getInteracting();
        if (interacting instanceof NPC) {
            NPC npc = (NPC) interacting;
            if (targetNpc != npc) selectTarget(npc, false);
            lastTargetTick = client.getTickCount();
        } else if (targetNpc != null
                && !config.alwaysShowOverlay()
                && client.getTickCount() - lastTargetTick
                        > Math.max(1, config.targetTimeout()) / 0.6) {
            targetNpc = null;
            target = null;
            liveResult = null;
            generation++;
        }
        calculateLive();
        SwingUtilities.invokeLater(
                () -> {
                    if (panel != null) panel.refreshClientState();
                });
    }

    private void capture() {
        if (client.getGameState() != GameState.LOGGED_IN || equipment == null) return;
        PlayerState next = new PlayerState();
        next.setAttackLevel(client.getRealSkillLevel(Skill.ATTACK));
        next.setStrengthLevel(client.getRealSkillLevel(Skill.STRENGTH));
        next.setDefenceLevel(client.getRealSkillLevel(Skill.DEFENCE));
        next.setRangedLevel(client.getRealSkillLevel(Skill.RANGED));
        next.setMagicLevel(client.getRealSkillLevel(Skill.MAGIC));
        next.setHitpointsLevel(client.getRealSkillLevel(Skill.HITPOINTS));
        next.setPrayerLevel(client.getRealSkillLevel(Skill.PRAYER));
        next.setCurrentHitpoints(client.getBoostedSkillLevel(Skill.HITPOINTS));
        next.setAttackBoost(client.getBoostedSkillLevel(Skill.ATTACK) - next.getAttackLevel());
        next.setStrengthBoost(
                client.getBoostedSkillLevel(Skill.STRENGTH) - next.getStrengthLevel());
        next.setDefenceBoost(client.getBoostedSkillLevel(Skill.DEFENCE) - next.getDefenceLevel());
        next.setRangedBoost(client.getBoostedSkillLevel(Skill.RANGED) - next.getRangedLevel());
        next.setMagicBoost(client.getBoostedSkillLevel(Skill.MAGIC) - next.getMagicLevel());
        next.setOnSlayerTask(config.onSlayerTask());
        next.setChargeSpellActive(config.chargeSpell());
        next.setInWilderness(client.getVarbitValue(Varbits.IN_WILDERNESS) == 1);
        for (com.loadoutlab.model.Prayer prayer : com.loadoutlab.model.Prayer.values())
            if (client.isPrayerActive(prayer.getRunelitePrayer()))
                next.getActivePrayers().add(prayer);
        int[] ids = containerIds(InventoryID.EQUIPMENT, 14);
        next.setEquippedItemIds(ids);
        List<com.loadoutlab.model.CombatStyle> styles = WeaponStyles.available(next, equipment);
        if (!styles.isEmpty())
            next.setCombatStyle(
                    styles.get(
                            Math.min(
                                    Math.max(0, client.getVarpValue(VarPlayer.ATTACK_STYLE)),
                                    styles.size() - 1)));
        int book = client.getVarbitValue(net.runelite.api.gameval.VarbitID.SPELLBOOK);
        next.setSpellbook(
                book == 0
                        ? "standard"
                        : book == 1
                                ? "ancient"
                                : book == 2 ? "lunar" : book == 3 ? "arceuus" : "unknown");
        if (client.getVarbitValue(net.runelite.api.gameval.VarbitID.AUTOCAST_SET) != 0) {
            String stance =
                    client.getVarbitValue(net.runelite.api.gameval.VarbitID.AUTOCAST_DEFMODE) == 0
                            ? "Autocast"
                            : "Defensive Autocast";
            for (com.loadoutlab.model.CombatStyle style : styles)
                if (style.getStance().equals(stance)) {
                    next.setCombatStyle(style);
                    break;
                }
        }
        // Capture names even if preparation needs an explicit dart/spell choice in the editor.
        String[] names = new String[14], categories = new String[14], versions = new String[14];
        for (int i = 0; i < 14; i++)
            if (ids[i] > 0) {
                EquipmentCatalogItem fact = equipment.getItemFacts(ids[i]);
                names[i] = client.getItemDefinition(ids[i]).getName();
                if (fact != null) {
                    categories[i] = fact.getCategory();
                    versions[i] = fact.getVersion();
                }
            }
        next.setEquippedItemNames(names);
        next.setEquippedItemCategories(categories);
        next.setEquippedItemVersions(versions);
        try {
            equipment.prepare(
                    next,
                    ids,
                    names,
                    EquipmentPreparationFacade.context(next, target == null ? 0 : target.getId()));
        } catch (IllegalArgumentException ignored) {
            /* UI asks for missing variables. */
        }
        player = next;
        inventory = containerIds(InventoryID.INVENTORY, 28);
    }

    private int[] containerIds(InventoryID container, int length) {
        int[] ids = new int[length];
        Arrays.fill(ids, -1);
        ItemContainer items = client.getItemContainer(container);
        if (items != null) {
            Item[] values = items.getItems();
            for (int i = 0; i < Math.min(length, values.length); i++) ids[i] = values[i].getId();
        }
        return ids;
    }

    private void calculateLive() {
        if (player == null
                || target == null
                || worker == null
                || calculation != null && !calculation.isDone()) return;
        Scenario.Loadout draft = new Scenario.Loadout();
        draft.player = Scenario.copy(player);
        draft.name = "Live player";
        MonsterStats snapshot = target.copy();
        EquipmentPreparationFacade facts = equipment;
        long token = generation;
        calculation =
                worker.submit(
                        () -> {
                            ScenarioCalculator.Result result =
                                    new ScenarioCalculator(facts).calculate(draft, snapshot);
                            if (running && generation == token) liveResult = result;
                        });
    }

    private void selectTarget(NPC npc, boolean open) {
        if (monsters == null) return;
        MonsterStats match = monsters.getMonster(npc.getId());
        if (match == null) match = monsters.getMonster(npc.getName());
        if (targetNpc != npc) {
            totalDamage = 0;
            firstDamageTick = -1;
        }
        targetNpc = npc;
        target = match;
        lastTargetTick = client.getTickCount();
        generation++;
        MonsterStats selected = match;
        if (open && selected != null)
            SwingUtilities.invokeLater(
                    () -> {
                        if (panel != null) {
                            panel.acceptTarget(selected);
                            if (navigation != null) toolbar.openPanel(navigation);
                        }
                    });
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (!config.npcMenu()
                || !"Examine".equals(event.getOption())
                || event.getType() != MenuAction.EXAMINE_NPC.getId()) return;
        MenuEntry source = event.getMenuEntry();
        NPC npc = source.getNpc();
        if (npc == null) return;
        client.createMenuEntry(-1)
                .setOption("Calculate DPS")
                .setTarget(event.getTarget())
                .setType(MenuAction.RUNELITE)
                .onClick(entry -> selectTarget(npc, true));
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        if (event.getActor() != targetNpc || !event.getHitsplat().isMine()) return;
        if (firstDamageTick < 0) firstDamageTick = client.getTickCount();
        totalDamage += Math.max(0, event.getHitsplat().getAmount());
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public SpriteManager getSpriteManager() {
        return spriteManager;
    }

    public SetupItemIndex getSetupItemIndex() {
        return setupItemIndex;
    }

    public WikiSetupService getWikiSetupService() {
        return wikiSetupService;
    }

    public BankLayoutService getBankLayoutService() {
        return bankLayoutService;
    }

    public OwnedEquipment getOwnedEquipment() {
        return bankCapture == null ? OwnedEquipment.empty(0) : bankCapture.snapshot();
    }

    public int[] getInventoryIds() {
        return inventory.clone();
    }

    public int[] getBankIds() {
        return getOwnedEquipment().bank.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    public PlayerState getCachedPlayerState() {
        PlayerState state = player;
        return state == null ? null : Scenario.copy(state);
    }

    public MonsterStats getCurrentMonsterStats() {
        MonsterStats state = target;
        return state == null ? null : state.copy();
    }

    public void publishComparison(ScenarioCalculator.Result result, MonsterStats monster) {
        comparison = result;
        comparisonTarget = monster == null ? "Comparison" : monster.getName();
    }

    public void clearComparison() {
        comparison = null;
        comparisonTarget = null;
    }

    ScenarioCalculator.Result displayed() {
        return config.overlaySource() == DpsLoadoutLabConfig.OverlaySource.SELECTED_COMPARISON
                ? comparison
                : liveResult;
    }

    String targetLabel() {
        return config.overlaySource() == DpsLoadoutLabConfig.OverlaySource.SELECTED_COMPARISON
                ? comparisonTarget
                : target == null ? null : target.getName();
    }

    boolean playerPresent() {
        return player != null;
    }

    double actualDps() {
        return firstDamageTick < 0
                ? 0
                : totalDamage / (Math.max(1, client.getTickCount() - firstDamageTick + 1) * 0.6);
    }
}
