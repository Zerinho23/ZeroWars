package com.zerowars;

import com.zerowars.api.ZeroWarsAPI;
import com.zerowars.commands.EventsCommand;
import com.zerowars.commands.TopCommand;
import com.zerowars.commands.ZeroWarsCommand;
import com.zerowars.commands.ZoneCommand;
import com.zerowars.commands.ZonesCommand;
import com.zerowars.config.ConfigManager;
import com.zerowars.listeners.ConsumableListener;
import com.zerowars.listeners.PlayerListener;
import com.zerowars.listeners.ZoneCaptureListener;
import com.zerowars.managers.CaptureManager;
import com.zerowars.managers.ClanManager;
import com.zerowars.managers.ConsumableManager;
import com.zerowars.managers.CooldownManager;
import com.zerowars.managers.EventManager;
import com.zerowars.managers.HeatManager;
import com.zerowars.managers.RankingManager;
import com.zerowars.managers.ZoneManager;
import com.zerowars.placeholders.ZeroWarsPlaceholders;
import com.zerowars.storage.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * ZeroWars - Competitive PvP Zone Control Plugin
 * Autor: zerinho23 | Colaborador: The_Titan19
 *
 * Clase principal del plugin. Gestiona el ciclo de vida completo:
 * enable → carga managers → registra listeners/comandos → disable → limpieza.
 */
public final class ZeroWars extends JavaPlugin {

    // ── Instancia singleton ──────────────────────────────────────────────────
    private static ZeroWars instance;

    // ── Managers (inyección manual, sin framework) ───────────────────────────
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private ZoneManager zoneManager;
    private CaptureManager captureManager;
    private ConsumableManager consumableManager;
    private CooldownManager cooldownManager;
    private EventManager eventManager;
    private HeatManager heatManager;
    private RankingManager rankingManager;
    private ClanManager clanManager;

    // ── API pública ──────────────────────────────────────────────────────────
    private ZeroWarsAPI api;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║       ZeroWars  v" + getDescription().getVersion() + "          ║");
        getLogger().info("║  zerinho23 | The_Titan19         ║");
        getLogger().info("╚══════════════════════════════════╝");

        // 1. Configuración — primero siempre
        if (!initConfig()) {
            getLogger().severe("Error cargando configuración. Desactivando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 2. Base de datos — bloquea el hilo principal brevemente para inicializar
        if (!initDatabase()) {
            getLogger().severe("Error conectando a la base de datos. Desactivando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Managers — orden importa (dependencias entre managers)
        initManagers();

        // 4. Listeners
        registerListeners();

        // 5. Comandos
        registerCommands();

        // 6. PlaceholderAPI (opcional)
        registerPlaceholders();

        // 7. API pública
        this.api = new ZeroWarsAPI(this);

        long elapsed = System.currentTimeMillis() - start;
        getLogger().info("ZeroWars activado correctamente en " + elapsed + "ms.");
        getLogger().info("Zonas cargadas: " + zoneManager.getZoneCount());
    }

    @Override
    public void onDisable() {
        getLogger().info("Deteniendo ZeroWars...");

        // Cancelar tareas del event manager
        if (eventManager != null) eventManager.shutdown();

        // Cancelar capturas activas y guardar estado
        if (captureManager != null) captureManager.shutdown();

        // Guardar datos de jugadores y zonas
        if (zoneManager != null) zoneManager.saveAll();
        if (rankingManager != null) rankingManager.saveAll();
        if (clanManager != null) clanManager.saveAll();

        // Cerrar pool de base de datos
        if (databaseManager != null) databaseManager.close();

        getLogger().info("ZeroWars desactivado correctamente. ¡Hasta la próxima!");
        instance = null;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Inicialización interna
    // ────────────────────────────────────────────────────────────────────────

    private boolean initConfig() {
        try {
            this.configManager = new ConfigManager(this);
            configManager.loadAll();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Fallo al cargar configuración", e);
            return false;
        }
    }

    private boolean initDatabase() {
        try {
            this.databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Fallo al inicializar base de datos", e);
            return false;
        }
    }

    private void initManagers() {
        // CooldownManager — sin dependencias
        this.cooldownManager = new CooldownManager(this);

        // ConsumableManager — depende de cooldownManager
        this.consumableManager = new ConsumableManager(this);

        // ClanManager — sin dependencias externas
        this.clanManager = new ClanManager(this);

        // ZoneManager — depende de databaseManager
        this.zoneManager = new ZoneManager(this);
        this.zoneManager.loadZones();

        // CaptureManager — depende de zoneManager y cooldownManager
        this.captureManager = new CaptureManager(this);
        this.captureManager.startUpdateTask();

        // HeatManager — depende de zoneManager
        this.heatManager = new HeatManager(this);

        // RankingManager — depende de databaseManager
        this.rankingManager = new RankingManager(this);
        this.rankingManager.loadRankings();

        // EventManager — depende de todos los anteriores
        this.eventManager = new EventManager(this);
        this.eventManager.startScheduler();

        getLogger().info("Managers inicializados correctamente.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new ZoneCaptureListener(this), this);
        pm.registerEvents(new ConsumableListener(this), this);
        getLogger().info("Listeners registrados.");
    }

    private void registerCommands() {
        var zwCmd = getCommand("zerowars");
        if (zwCmd != null) {
            var handler = new ZeroWarsCommand(this);
            zwCmd.setExecutor(handler);
            zwCmd.setTabCompleter(handler);
        }

        var zoneCmd = getCommand("zone");
        if (zoneCmd != null) zoneCmd.setExecutor(new ZoneCommand(this));

        var zonesCmd = getCommand("zones");
        if (zonesCmd != null) zonesCmd.setExecutor(new ZonesCommand(this));

        var topCmd = getCommand("top");
        if (topCmd != null) topCmd.setExecutor(new TopCommand(this));

        var eventsCmd = getCommand("events");
        if (eventsCmd != null) eventsCmd.setExecutor(new EventsCommand(this));

        getLogger().info("Comandos registrados.");
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ZeroWarsPlaceholders(this).register();
            getLogger().info("PlaceholderAPI integrado correctamente.");
        } else {
            getLogger().warning("PlaceholderAPI no encontrado. Placeholders desactivados.");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Getters públicos
    // ────────────────────────────────────────────────────────────────────────

    public static ZeroWars getInstance() { return instance; }

    public ConfigManager getConfigManager()       { return configManager; }
    public DatabaseManager getDatabaseManager()   { return databaseManager; }
    public ZoneManager getZoneManager()           { return zoneManager; }
    public CaptureManager getCaptureManager()     { return captureManager; }
    public ConsumableManager getConsumableManager() { return consumableManager; }
    public CooldownManager getCooldownManager()   { return cooldownManager; }
    public EventManager getEventManager()         { return eventManager; }
    public HeatManager getHeatManager()           { return heatManager; }
    public RankingManager getRankingManager()     { return rankingManager; }
    public ClanManager getClanManager()           { return clanManager; }
    public ZeroWarsAPI getAPI()                   { return api; }
}
