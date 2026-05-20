package com.zerowars;

import com.zerowars.api.ZeroWarsAPI;
import com.zerowars.commands.EventsCommand;
import com.zerowars.commands.TopCommand;
import com.zerowars.commands.ZeroWarsCommand;
import com.zerowars.commands.ZoneCommand;
import com.zerowars.commands.ZonesCommand;
import com.zerowars.config.ConfigManager;
import com.zerowars.gui.GuiManager;
import com.zerowars.listeners.ConsumableListener;
import com.zerowars.listeners.GuiListener;
import com.zerowars.listeners.PlayerListener;
import com.zerowars.listeners.WandListener;
import com.zerowars.listeners.ZoneCaptureListener;
import com.zerowars.managers.CaptureManager;
import com.zerowars.managers.ChatInputManager;
import com.zerowars.managers.ClanManager;
import com.zerowars.managers.ConsumableManager;
import com.zerowars.managers.CooldownManager;
import com.zerowars.managers.EventManager;
import com.zerowars.managers.HeatManager;
import com.zerowars.managers.RankingManager;
import com.zerowars.managers.RewardManager;
import com.zerowars.managers.ZoneManager;
import com.zerowars.managers.ZoneWandManager;
import com.zerowars.placeholders.ZeroWarsPlaceholders;
import com.zerowars.storage.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class ZeroWars extends JavaPlugin {

    // ── ANSI colors para la consola ──────────────────────────────────────────
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String RED     = "\u001B[38;2;255;68;68m";
    private static final String ORANGE  = "\u001B[38;2;255;136;0m";
    private static final String YELLOW  = "\u001B[38;2;255;215;0m";
    private static final String GRAY    = "\u001B[38;2;160;160;160m";
    private static final String WHITE   = "\u001B[97m";

    private static ZeroWars instance;

    private ConfigManager     configManager;
    private DatabaseManager   databaseManager;
    private ZoneManager       zoneManager;
    private CaptureManager    captureManager;
    private ConsumableManager consumableManager;
    private CooldownManager   cooldownManager;
    private EventManager      eventManager;
    private HeatManager       heatManager;
    private RankingManager    rankingManager;
    private ClanManager       clanManager;
    private RewardManager     rewardManager;
    private GuiManager        guiManager;
    private ZoneWandManager   zoneWandManager;
    private ChatInputManager  chatInputManager;

    private ZeroWarsAPI api;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        printBanner();

        if (!initConfig()) {
            getLogger().severe("Error cargando configuracion. Desactivando plugin.");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        if (!initDatabase()) {
            getLogger().severe("Error conectando a la base de datos. Desactivando plugin.");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        initManagers();
        registerListeners();
        registerCommands();
        registerPlaceholders();
        this.api = new ZeroWarsAPI(this);

        long elapsed = System.currentTimeMillis() - start;
        getLogger().info(ORANGE + BOLD + "ZeroWars" + RESET + GRAY + " activado en " + YELLOW + elapsed + "ms"
                + GRAY + " | Zonas: " + WHITE + zoneManager.getZoneCount()
                + GRAY + " | Vault: " + (rewardManager.isVaultEnabled() ? "\u001B[32mSI" : "\u001B[31mNO")
                + RESET);
    }

    @Override
    public void onDisable() {
        getLogger().info(ORANGE + BOLD + "ZeroWars" + RESET + GRAY + " desactivando..." + RESET);
        if (eventManager    != null) eventManager.shutdown();
        if (captureManager  != null) captureManager.shutdown();
        if (zoneManager     != null) zoneManager.saveAll();
        if (rankingManager  != null) rankingManager.saveAll();
        if (clanManager     != null) clanManager.saveAll();
        if (databaseManager != null) databaseManager.close();
        getLogger().info(GRAY + "ZeroWars desactivado. Hasta la proxima!" + RESET);
        instance = null;
    }

    private void printBanner() {
        String v = getDescription().getVersion();
        getLogger().info(RED  + BOLD + "  ╔══════════════════════════════════════╗" + RESET);
        getLogger().info(RED  + BOLD + "  ║  " + RESET
                + ORANGE + BOLD + "⚔  ZeroWars  " + RESET
                + YELLOW + "v" + v
                + RED + BOLD + "                    ║" + RESET);
        getLogger().info(RED  + BOLD + "  ║  " + RESET
                + GRAY   + "Autor:        " + WHITE + "zerinho23"
                + RED + BOLD + "              ║" + RESET);
        getLogger().info(RED  + BOLD + "  ║  " + RESET
                + GRAY   + "Colaborador:  " + WHITE + "The_Titan19"
                + RED + BOLD + "            ║" + RESET);
        getLogger().info(RED  + BOLD + "  ║  " + RESET
                + GRAY   + "Paper:        " + WHITE + "1.20.4+"
                + RED + BOLD + "                ║" + RESET);
        getLogger().info(RED  + BOLD + "  ╚══════════════════════════════════════╝" + RESET);
    }

    private boolean initConfig() {
        try { this.configManager = new ConfigManager(this); configManager.loadAll(); return true; }
        catch (Exception e) { getLogger().log(Level.SEVERE, "Fallo al cargar configuracion", e); return false; }
    }

    private boolean initDatabase() {
        try { this.databaseManager = new DatabaseManager(this); databaseManager.initialize(); return true; }
        catch (Exception e) { getLogger().log(Level.SEVERE, "Fallo al inicializar base de datos", e); return false; }
    }

    private void initManagers() {
        this.cooldownManager   = new CooldownManager(this);
        this.consumableManager = new ConsumableManager(this);
        this.clanManager       = new ClanManager(this);
        this.zoneManager       = new ZoneManager(this);
        this.zoneManager.loadZones();
        this.captureManager    = new CaptureManager(this);
        this.captureManager.startUpdateTask();
        this.heatManager       = new HeatManager(this);
        this.rankingManager    = new RankingManager(this);
        this.rankingManager.loadRankings();
        this.eventManager      = new EventManager(this);
        this.eventManager.startScheduler();
        this.rewardManager     = new RewardManager(this);
        this.zoneManager.startPassiveRewardTick();
        this.guiManager        = new GuiManager(this);
        this.zoneWandManager   = new ZoneWandManager(this);
        this.chatInputManager  = new ChatInputManager(this);
        getLogger().info(GRAY + "Managers inicializados." + RESET);
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this),      this);
        pm.registerEvents(new ZoneCaptureListener(this), this);
        pm.registerEvents(new ConsumableListener(this),  this);
        pm.registerEvents(new GuiListener(),             this);
        pm.registerEvents(new WandListener(this),        this);
        pm.registerEvents(chatInputManager,              this);
        getLogger().info(GRAY + "Listeners registrados." + RESET);
    }

    private void registerCommands() {
        var zwCmd = getCommand("zerowars");
        if (zwCmd != null) {
            var handler = new ZeroWarsCommand(this);
            zwCmd.setExecutor(handler); zwCmd.setTabCompleter(handler);
        }
        var zoneCmd   = getCommand("zone");   if (zoneCmd   != null) zoneCmd.setExecutor(new ZoneCommand(this));
        var zonesCmd  = getCommand("zones");  if (zonesCmd  != null) zonesCmd.setExecutor(new ZonesCommand(this));
        var topCmd    = getCommand("top");    if (topCmd    != null) topCmd.setExecutor(new TopCommand(this));
        var eventsCmd = getCommand("events"); if (eventsCmd != null) eventsCmd.setExecutor(new EventsCommand(this));
        getLogger().info(GRAY + "Comandos registrados." + RESET);
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ZeroWarsPlaceholders(this).register();
            getLogger().info(GRAY + "PlaceholderAPI integrado." + RESET);
        } else {
            getLogger().warning("PlaceholderAPI no encontrado. Placeholders desactivados.");
        }
    }

    public static ZeroWars getInstance()              { return instance; }
    public ConfigManager getConfigManager()           { return configManager; }
    public DatabaseManager getDatabaseManager()       { return databaseManager; }
    public ZoneManager getZoneManager()               { return zoneManager; }
    public CaptureManager getCaptureManager()         { return captureManager; }
    public ConsumableManager getConsumableManager()   { return consumableManager; }
    public CooldownManager getCooldownManager()       { return cooldownManager; }
    public EventManager getEventManager()             { return eventManager; }
    public HeatManager getHeatManager()               { return heatManager; }
    public RankingManager getRankingManager()         { return rankingManager; }
    public ClanManager getClanManager()               { return clanManager; }
    public RewardManager getRewardManager()           { return rewardManager; }
    public GuiManager getGuiManager()                 { return guiManager; }
    public ZoneWandManager getZoneWandManager()       { return zoneWandManager; }
    public ChatInputManager getChatInputManager()     { return chatInputManager; }
    public ZeroWarsAPI getAPI()                       { return api; }
}
