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
  import com.zerowars.listeners.ZoneCaptureListener;
  import com.zerowars.managers.CaptureManager;
  import com.zerowars.managers.ClanManager;
  import com.zerowars.managers.ConsumableManager;
  import com.zerowars.managers.CooldownManager;
  import com.zerowars.managers.EventManager;
  import com.zerowars.managers.HeatManager;
  import com.zerowars.managers.RankingManager;
  import com.zerowars.managers.RewardManager;
  import com.zerowars.managers.ZoneManager;
  import com.zerowars.placeholders.ZeroWarsPlaceholders;
  import com.zerowars.storage.DatabaseManager;
  import org.bukkit.plugin.java.JavaPlugin;

  import java.util.logging.Level;

  /**
   * ZeroWars - Competitive PvP Zone Control Plugin
   * Autor: zerinho23 | Colaborador: The_Titan19
   */
  public final class ZeroWars extends JavaPlugin {

      private static ZeroWars instance;

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
      private RewardManager rewardManager;
      private GuiManager guiManager;

      private ZeroWarsAPI api;

      @Override
      public void onEnable() {
          instance = this;
          long start = System.currentTimeMillis();
          getLogger().info("╔══════════════════════════════════╗");
          getLogger().info("║       ZeroWars  v" + getDescription().getVersion() + "          ║");
          getLogger().info("║  zerinho23 | The_Titan19         ║");
          getLogger().info("╚══════════════════════════════════╝");
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
          getLogger().info("ZeroWars v" + getDescription().getVersion() + " activado en "
                  + (System.currentTimeMillis() - start) + "ms | Zonas: " + zoneManager.getZoneCount()
                  + " | Vault: " + (rewardManager.isVaultEnabled() ? "SI" : "NO (dinero desactivado)")
                  + " | GUI: activo");
      }

      @Override
      public void onDisable() {
          getLogger().info("Deteniendo ZeroWars...");
          if (eventManager    != null) eventManager.shutdown();
          if (captureManager  != null) captureManager.shutdown();
          if (zoneManager     != null) zoneManager.saveAll();
          if (rankingManager  != null) rankingManager.saveAll();
          if (clanManager     != null) clanManager.saveAll();
          if (databaseManager != null) databaseManager.close();
          getLogger().info("ZeroWars desactivado. Hasta la proxima!");
          instance = null;
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
          getLogger().info("Managers inicializados correctamente.");
      }

      private void registerListeners() {
          var pm = getServer().getPluginManager();
          pm.registerEvents(new PlayerListener(this),       this);
          pm.registerEvents(new ZoneCaptureListener(this),  this);
          pm.registerEvents(new ConsumableListener(this),   this);
          pm.registerEvents(new GuiListener(),              this);
          getLogger().info("Listeners registrados.");
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
      public ZeroWarsAPI getAPI()                       { return api; }
  }