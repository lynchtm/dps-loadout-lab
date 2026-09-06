package com.dpscalc;

import com.dpscalc.combat.CombatTracker;
import com.dpscalc.data.MonsterDataManager;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.equipment.EquipmentPreparationFacade;
import com.dpscalc.scenario.*;
import com.dpscalc.scenario.ScenarioPanel;
import com.dpscalc.state.PlayerState;
import com.dpscalc.state.PlayerStateManager;
import com.google.inject.Provides;

import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.swing.SwingUtilities;

@PluginDescriptor(
        name = "DPS Loadout Lab",
        description = "Calculates your theoretical DPS against monsters",
        tags = {"dps", "damage", "calculator", "combat", "pvm"})
public class DpsCalcPlugin extends Plugin {

    @Inject private Client client;

    @Inject private ClientThread clientThread;

    @Inject private DpsCalcConfig config;

    @Inject private OverlayManager overlayManager;

    @Inject private PlayerStateManager playerStateManager;

    @Inject private MonsterDataManager monsterDataManager;

    @Inject private DpsCalcOverlay overlay;

    @Inject private ClientToolbar clientToolbar;

    @Inject private ItemManager itemManager;

    private NavigationButton navButton;

    @Inject private CombatTracker combatTracker;

    private NPC targetNpc;

    private volatile MonsterStats currentMonsterStats;

    private volatile String selectedVersion;

    private volatile PlayerState cachedPlayerState;

    public ItemManager getItemManager() {
        return itemManager;
    }

    @Inject private com.dpscalc.scenario.SetupItemIndex setupItemIndex;
    @Inject private com.dpscalc.scenario.WikiSetupService wikiSetupService;
    @Inject private com.dpscalc.scenario.BankLayoutService bankLayoutService;

    public com.dpscalc.scenario.SetupItemIndex getSetupItemIndex() {
        return setupItemIndex;
    }

    public com.dpscalc.scenario.WikiSetupService getWikiSetupService() {
        return wikiSetupService;
    }

    public com.dpscalc.scenario.BankLayoutService getBankLayoutService() {
        return bankLayoutService;
    }

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> targetClearTask;
    private volatile ScenarioPanel scenarioPanel;
    @Inject private EquipmentPreparationFacade equipment;
    private volatile OverlaySnapshot liveOverlay, comparisonOverlay;
    private volatile String requestedLiveKey;
    private CalculationCoordinator liveCalculations;

    public static final class OverlaySnapshot {
        public final ScenarioCalculator.Result result;
        public final MonsterStats target;
        public final String source;

        public OverlaySnapshot(
                ScenarioCalculator.Result result, MonsterStats target, String source) {
            this.result = result;
            this.target = target == null ? null : target.copy();
            this.source = source;
        }
    }

    public void publishComparison(ScenarioCalculator.Result result, MonsterStats target) {
        comparisonOverlay = new OverlaySnapshot(result, target, "Comparison: " + result.name);
    }

    public void clearComparison() {
        comparisonOverlay = null;
    }

    public OverlaySnapshot getOverlaySnapshot() {
        return getConfig().overlaySource() == DpsCalcConfig.OverlaySource.SELECTED_COMPARISON
                ? comparisonOverlay
                : liveOverlay;
    }

    private volatile boolean running;
    private volatile long lifecycle;
    private volatile long calculationGeneration;
    private boolean dirty = true;
    private java.util.concurrent.Future<?> analysisTask;
    @Inject private com.dpscalc.scenario.BankCapture bankCapture;

    public com.dpscalc.scenario.OwnedEquipment getOwnedEquipment() {
        return bankCapture == null
                ? com.dpscalc.scenario.OwnedEquipment.empty(0)
                : bankCapture.snapshot();
    }

    private volatile int[] inventoryIds = new int[0];
    private volatile int[] bankIds = new int[0];

    public int[] getInventoryIds() {
        return inventoryIds.clone();
    }

    public int[] getBankIds() {
        return bankIds.clone();
    }

    @Override
    protected void startUp() {
        log.info("DPS Calculator plugin started");
        running = true;
        requestedLiveKey = null;
        liveCalculations = new CalculationCoordinator(new ScenarioCalculator(equipment));
        setupItemIndex.start();
        final long startupLifecycle = ++lifecycle;
        executor =
                Executors.newSingleThreadScheduledExecutor(
                        r -> {
                            Thread t = new Thread(r, "wiki-dps-live");
                            t.setDaemon(true);
                            return t;
                        });
        executor.execute(monsterDataManager::loadMonsters);
        overlayManager.add(overlay);

        SwingUtilities.invokeLater(
                () -> {
                    if (running && lifecycle == startupLifecycle) updatePanelVisibility();
                });
    }

    private void updatePanelVisibility() {
        if (!running) return;
        if (config.showPanel() && navButton == null) {
            BufferedImage icon;
            try {
                icon = ImageUtil.loadImageResource(getClass(), "icon.png");
            } catch (Exception e) {
                log.warn("Failed to load icon.png, using default", e);
                icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                java.awt.Graphics2D g = icon.createGraphics();
                g.setColor(new java.awt.Color(255, 200, 100));
                g.fillRect(0, 0, 16, 16);
                g.setColor(java.awt.Color.WHITE);
                g.drawString("D", 4, 12);
                g.dispose();
            }
            navButton =
                    NavigationButton.builder()
                            .tooltip("DPS Loadout Lab")
                            .icon(icon)
                            .priority(5)
                            .panel(scenarioPanel = injector.getInstance(ScenarioPanel.class))
                            .build();
            clientToolbar.addNavigation(navButton);
        } else if (!config.showPanel() && navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
            if (scenarioPanel != null) {
                scenarioPanel.dispose();
                scenarioPanel = null;
            }
            clearComparison();
        }
    }

    @Override
    protected void shutDown() {
        log.info("DPS Calculator plugin stopped");
        running = false;
        setupItemIndex.stop();
        bankCapture.clear();
        lifecycle++;
        calculationGeneration++;
        ScenarioPanel stoppedPanel = scenarioPanel;
        scenarioPanel = null;
        SwingUtilities.invokeLater(
                () -> {
                    if (stoppedPanel != null) stoppedPanel.dispose();
                });
        overlayManager.remove(overlay);

        if (navButton != null) {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }

        cancelTargetClearTask();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        targetNpc = null;
        requestedLiveKey = null;
        liveOverlay = null;
        currentMonsterStats = null;
        selectedVersion = null;
        cachedPlayerState = null;
        liveOverlay = null;
        comparisonOverlay = null;
        combatTracker.reset();
    }

    @Provides
    DpsCalcConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(DpsCalcConfig.class);
    }

    private void signalPanel() {
        SwingUtilities.invokeLater(
                () -> {
                    if (running && scenarioPanel != null) scenarioPanel.refreshClientState();
                });
    }

    @Subscribe
    public void onProfileChanged(ProfileChanged event) {
        dirty = true;
        resetLiveCalculation();
        clearComparison();
        SwingUtilities.invokeLater(this::updatePanelVisibility);
        signalPanel();
    }

    @Subscribe
    public void onRuneScapeProfileChanged(RuneScapeProfileChanged event) {
        dirty = true;
        resetLiveCalculation();
        clearComparison();
        signalPanel();
    }

    private void resetLiveCalculation() {
        calculationGeneration++;
        requestedLiveKey = null;
        liveOverlay = null;
        if (liveCalculations != null) liveCalculations.clear();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if ("dpscalc".equals(event.getGroup())) {
            dirty = true;
            SwingUtilities.invokeLater(this::updatePanelVisibility);
        }
        if ("dpscalc".equals(event.getGroup()) || "runelite".equals(event.getGroup()))
            signalPanel();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() != GameState.LOGGED_IN) {
            calculationGeneration++;
            liveOverlay = null;
            comparisonOverlay = null;
            if (event.getGameState() != GameState.LOADING) combatTracker.reset();
            cachedPlayerState = null;
            inventoryIds = new int[0];
            bankIds = new int[0];
            if (event.getGameState() != GameState.LOADING) bankCapture.clear();
            cancelTargetClearTask();
            targetNpc = null;
            requestedLiveKey = null;
            currentMonsterStats = null;
            selectedVersion = null;
        } else if (event.getGameState() == GameState.LOGGED_IN) {
            cachedPlayerState = playerStateManager.getPlayerState();
        }
        signalPanel();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        bankCapture.tick();
        cachedPlayerState = playerStateManager.getPlayerState();
        if (config.automaticRefresh() || dirty) {
            dirty = false;
            recalculateDps();
        }
        signalPanel();
    }

    @Subscribe
    public void onInteractingChanged(InteractingChanged event) {
        if (event.getTarget() == client.getLocalPlayer()
                && event.getSource() instanceof NPC
                && targetNpc == null) {
            targetNpc = (NPC) event.getSource();
            dirty = true;
        }
        if (event.getSource() != client.getLocalPlayer()) {
            return;
        }

        Actor target = event.getTarget();
        if (target instanceof NPC) {
            cancelTargetClearTask();
            NPC newTarget = (NPC) target;
            if (targetNpc == null || targetNpc.getId() != newTarget.getId()) {
                selectedVersion = null;
            }
            targetNpc = newTarget;
            combatTracker.beginCombat();
            dirty = true;
        } else if (target == null && targetNpc != null && !targetNpc.isDead()) {
            scheduleTargetClear();
        }
    }

    private void cancelTargetClearTask() {
        if (targetClearTask != null) {
            targetClearTask.cancel(false);
            targetClearTask = null;
        }
    }

    private void scheduleTargetClear() {
        cancelTargetClearTask();
        int timeout = config.targetTimeout();
        if (timeout > 0 && executor != null) {
            targetClearTask =
                    executor.schedule(
                            () -> clientThread.invokeLater(this::clearTarget),
                            timeout,
                            TimeUnit.SECONDS);
        }
    }

    private void clearTarget() {
        calculationGeneration++;
        targetNpc = null;
        requestedLiveKey = null;
        liveOverlay = null;
        currentMonsterStats = null;
        selectedVersion = null;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        bankCapture.changed(event);
        if (event.getContainerId() == net.runelite.api.InventoryID.INVENTORY.getId())
            inventoryIds =
                    java.util.Arrays.stream(event.getItemContainer().getItems())
                            .mapToInt(net.runelite.api.Item::getId)
                            .toArray();
        if (event.getContainerId() == net.runelite.api.InventoryID.BANK.getId())
            bankIds =
                    java.util.Arrays.stream(event.getItemContainer().getItems())
                            .mapToInt(net.runelite.api.Item::getId)
                            .toArray();
        signalPanel();
        if (event.getContainerId() == 94) {
            dirty = true;
        }
    }

    @Subscribe
    public void onStatChanged(net.runelite.api.events.StatChanged event) {
        bankCapture.playerChanged();
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        dirty = true;
    }

    private void recalculateDps() {
        if (client.getGameState() != GameState.LOGGED_IN) return;
        PlayerState playerState = playerStateManager.getPlayerState();
        cachedPlayerState = playerState;
        if (targetNpc == null) {
            clearTarget();
            return;
        }
        MonsterStats base = monsterDataManager.getMonster(targetNpc.getId(), selectedVersion);
        if (base == null) {
            calculationGeneration++;
            requestedLiveKey = null;
            liveOverlay = null;
            currentMonsterStats = null;
            return;
        }
        currentMonsterStats = new LiveMonsterContextProvider(client, targetNpc).enrich(base);
        Scenario.Loadout input =
                CalculationInputs.live(
                        playerState,
                        config.onSlayerTask(),
                        config.chargeSpell(),
                        config.showSpecialAttack());
        MonsterStats target = currentMonsterStats.copy();
        if (executor == null || executor.isShutdown()) return;
        String key = CalculationInputs.key(input, target, new Encounter());
        if (key.equals(requestedLiveKey)) return;
        requestedLiveKey = key;
        long generation = ++calculationGeneration;
        liveOverlay = null;
        if (analysisTask != null) analysisTask.cancel(true);
        CalculationCoordinator calculator = liveCalculations;
        analysisTask =
                executor.submit(
                        () -> {
                            try {
                                ScenarioCalculator.Result result =
                                        calculator.calculate(input, target, new Encounter());
                                if (running && generation == calculationGeneration)
                                    liveOverlay =
                                            new OverlaySnapshot(result, target, "Live player");
                            } catch (RuntimeException error) {
                                if (generation == calculationGeneration) {
                                    liveOverlay = null;
                                    requestedLiveKey = null;
                                }
                                log.debug("Live DPS unavailable: {}", error.getMessage());
                            }
                        });
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event) {
        Actor target = event.getActor();

        if (!(target instanceof NPC)) {
            return;
        }

        if (target != targetNpc) {
            return;
        }

        Hitsplat hitsplat = event.getHitsplat();
        if (hitsplat.isMine()) {
            combatTracker.recordDamage(hitsplat.getAmount());
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();

        if (npc != targetNpc) {
            return;
        }

        if (npc.isDead()) {
            combatTracker.recordKill();
        }
        clearTarget();
    }

    public void resetCombatTracker() {
        combatTracker.reset();
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (!config.npcMenu() || !"Attack".equals(event.getOption())) return;
        NPC npc = event.getMenuEntry().getNpc();
        if (npc == null) return;
        client.createMenuEntry(-1)
                .setOption("Calculate DPS")
                .setTarget(event.getTarget())
                .setType(MenuAction.RUNELITE)
                .onClick(
                        entry -> {
                            MonsterStats target = monsterDataManager.getMonster(npc.getId());
                            SwingUtilities.invokeLater(
                                    () -> {
                                        if (scenarioPanel != null)
                                            scenarioPanel.acceptTarget(target);
                                    });
                        });
    }

    public boolean selectMonsterVersion(int npcId, String version) {
        NPC target = targetNpc;
        if (target == null || target.getId() != npcId) {
            return false;
        }
        selectedVersion = version;
        clientThread.invokeLater(this::recalculateDps);
        return true;
    }

    public java.util.List<MonsterStats> getTargetVersions() {
        NPC target = targetNpc;
        if (target == null) {
            return java.util.Collections.emptyList();
        }
        return monsterDataManager.getMonsterVersions(target.getId());
    }

    public DpsCalcConfig getConfig() {
        return config;
    }

    public CombatTracker getCombatTracker() {
        return combatTracker;
    }

    public NPC getTargetNpc() {
        return targetNpc;
    }

    public MonsterStats getCurrentMonsterStats() {
        return currentMonsterStats;
    }

    public String getSelectedVersion() {
        return selectedVersion;
    }

    public PlayerState getCachedPlayerState() {
        return cachedPlayerState;
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(DpsCalcPlugin.class);
}
