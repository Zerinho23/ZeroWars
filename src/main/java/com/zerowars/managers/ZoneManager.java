package com.zerowars.managers;

  import com.zerowars.ZeroWars;
  import com.zerowars.config.ConfigManager;
  import com.zerowars.models.Zone;
  import com.zerowars.storage.dao.ZoneDAO;
  import org.bukkit.Bukkit;
  import org.bukkit.Location;
  import org.bukkit.configuration.ConfigurationSection;
  import org.bukkit.configuration.file.FileConfiguration;
  import org.bukkit.entity.Player;

  import java.util.*;
  import java.util.concurrent.ConcurrentHashMap;

  /**
   * Gestor central de zonas PvP.
   *
   * v1.3.0: saveZoneToYml() — persiste zona en zones.yml al crearla o modificarla.
   * v1.2.0: startPassiveRewardTick() + tryLevelUp().
   */
  public class ZoneManager {

      private final ZeroWars plugin;
      private final ZoneDAO zoneDAO;
      private final Map<String, Zone> zones = new ConcurrentHashMap<>();
      private final Map<UUID, String> playerZoneCache = new ConcurrentHashMap<>();

      public ZoneManager(ZeroWars plugin) {
          this.plugin  = plugin;
          this.zoneDAO = new ZoneDAO(plugin);
      }

      // ── Carga ────────────────────────────────────────────────────────────────

      public void loadZones() {
          zones.clear();
          ConfigurationSection section =
                  plugin.getConfigManager().zones().getConfigurationSection("zones");
          if (section == null) {
              plugin.getLogger().warning("No hay zonas definidas en zones.yml");
              return;
          }
          for (String key : section.getKeys(false)) {
              ConfigurationSection zs = section.getConfigurationSection(key);
              if (zs == null) continue;
              try {
                  Zone zone = parseZone(key, zs);
                  zones.put(key, zone);
                  Zone fZone = zone;
                  Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> zoneDAO.loadState(fZone));
              } catch (Exception e) {
                  plugin.getLogger().warning("Error parseando zona '" + key + "': " + e.getMessage());
              }
          }
          plugin.getLogger().info("Zonas cargadas: " + zones.size());
      }

      private Zone parseZone(String id, ConfigurationSection s) {
          String rawName = s.getString("name", id);
          Zone.ZoneType type;
          try   { type = Zone.ZoneType.valueOf(s.getString("type", "MINE")); }
          catch (IllegalArgumentException e) { type = Zone.ZoneType.SPECIAL; }
          String world = s.getString("world", "world");
          ConfigurationSection c1 = s.getConfigurationSection("corner1");
          ConfigurationSection c2 = s.getConfigurationSection("corner2");
          double minX = c1 != null ? c1.getDouble("x") : 0;
          double minY = c1 != null ? c1.getDouble("y") : 0;
          double minZ = c1 != null ? c1.getDouble("z") : 0;
          double maxX = c2 != null ? c2.getDouble("x") : 0;
          double maxY = c2 != null ? c2.getDouble("y") : 0;
          double maxZ = c2 != null ? c2.getDouble("z") : 0;
          Zone zone = new Zone(id, rawName, type, world,
                  minX, minY, minZ, maxX, maxY, maxZ,
                  s.getInt("level", 1), s.getInt("max-level", 5), s.getInt("capture-time", 30));
          zone.setEnabled(s.getBoolean("enabled", true));
          zone.setEventOnly(s.getBoolean("event-only", false));
          zone.setProtected(s.getBoolean("protected", true));
          zone.setRewardIds(s.getStringList("rewards"));
          return zone;
      }

      // ── Guardar en SQLite ────────────────────────────────────────────────────

      public void saveAll() {
          Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
              for (Zone zone : zones.values()) zoneDAO.saveState(zone);
          });
      }

      public void saveAsync(Zone zone) {
          Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> zoneDAO.saveState(zone));
      }

      // ── Guardar en zones.yml (v1.3.0) ────────────────────────────────────────

      /**
       * Escribe la zona en zones.yml para que persista entre reinicios.
       * Llamar despues de registerZone() al crear una zona, o al modificar campos
       * que deben persistir (enabled, level, rewards).
       */
      public void saveZoneToYml(Zone zone) {
          FileConfiguration cfg  = plugin.getConfigManager().zones();
          String path = "zones." + zone.getId();

          cfg.set(path + ".name",          zone.getDisplayName());
          cfg.set(path + ".type",          zone.getType().name());
          cfg.set(path + ".world",         zone.getWorldName());
          cfg.set(path + ".corner1.x",    zone.getMinX());
          cfg.set(path + ".corner1.y",    zone.getMinY());
          cfg.set(path + ".corner1.z",    zone.getMinZ());
          cfg.set(path + ".corner2.x",    zone.getMaxX());
          cfg.set(path + ".corner2.y",    zone.getMaxY());
          cfg.set(path + ".corner2.z",    zone.getMaxZ());
          cfg.set(path + ".level",         zone.getLevel());
          cfg.set(path + ".max-level",     zone.getMaxLevel());
          cfg.set(path + ".capture-time",  zone.getCaptureTime());
          cfg.set(path + ".enabled",       zone.isEnabled());
          cfg.set(path + ".event-only",    zone.isEventOnly());
          cfg.set(path + ".protected",     zone.isProtected());
          cfg.set(path + ".rewards",       zone.getRewardIds());

          plugin.getConfigManager().save(ConfigManager.ZONES);

          if (plugin.getConfigManager().isDebug())
              plugin.getLogger().info("[ZoneYML] Zona '" + zone.getId() + "' guardada en zones.yml");
      }

      /** Elimina la entrada de una zona de zones.yml. */
      public void removeZoneFromYml(String id) {
          plugin.getConfigManager().zones().set("zones." + id, null);
          plugin.getConfigManager().save(ConfigManager.ZONES);
      }

      // ── Passive reward tick (v1.2.0) ─────────────────────────────────────────

      public void startPassiveRewardTick() {
          Bukkit.getScheduler().runTaskTimer(plugin, () -> {
              long now = System.currentTimeMillis();
              for (Zone zone : zones.values()) {
                  if (!zone.isEnabled() || zone.getOwnerUUID() == null) continue;
                  if (zone.getRewardIds().isEmpty()) continue;
                  long intervalMs = plugin.getRewardManager().getPassiveIntervalMs(zone);
                  if (now - zone.getLastRewardTime() < intervalMs) continue;
                  Player owner = Bukkit.getPlayer(zone.getOwnerUUID());
                  if (owner == null || !owner.isOnline()) continue;
                  zone.setLastRewardTime(now);
                  plugin.getRewardManager().givePassiveReward(owner, zone);
                  zone.addDomineTime(intervalMs);
                  tryLevelUp(zone, owner);
              }
          }, 20L, 20L);
      }

      // ── Zone level-up (v1.2.0) ───────────────────────────────────────────────

      public void tryLevelUp(Zone zone, Player owner) {
          if (zone.getLevel() >= zone.getMaxLevel()) return;
          long msPerLevel = plugin.getConfigManager().config()
                  .getLong("zones.level-up.domine-minutes-per-level", 5) * 60_000L;
          long required   = (long) zone.getLevel() * msPerLevel;
          if (zone.getTotalDomineTime() < required) return;
          zone.setLevel(zone.getLevel() + 1);
          saveAsync(zone);
          plugin.getRewardManager().giveLevelUpReward(owner, zone, zone.getLevel());
      }

      // ── Acceso ───────────────────────────────────────────────────────────────

      public Optional<Zone> getZone(String id)         { return Optional.ofNullable(zones.get(id)); }
      public Collection<Zone> getAllZones()             { return Collections.unmodifiableCollection(zones.values()); }

      public List<Zone> getEnabledZones() {
          return zones.values().stream().filter(Zone::isEnabled).filter(z -> !z.isEventOnly()).toList();
      }

      public Optional<Zone> getZoneAt(Location location) {
          if (location == null || location.getWorld() == null) return Optional.empty();
          String worldName = location.getWorld().getName();
          return zones.values().stream()
                  .filter(Zone::isEnabled)
                  .filter(z -> z.getWorldName().equals(worldName))
                  .filter(z -> z.contains(location))
                  .findFirst();
      }

      // ── Player zone cache ────────────────────────────────────────────────────

      public void setPlayerZone(UUID uuid, String zoneId) {
          if (zoneId == null) playerZoneCache.remove(uuid);
          else playerZoneCache.put(uuid, zoneId);
      }
      public String getPlayerZoneId(UUID uuid)    { return playerZoneCache.get(uuid); }
      public boolean isInZone(UUID uuid)          { return playerZoneCache.containsKey(uuid); }
      public Optional<Zone> getPlayerZone(UUID uuid) {
          String id = playerZoneCache.get(uuid);
          return id != null ? getZone(id) : Optional.empty();
      }
      public List<UUID> getPlayersInZone(String zoneId) {
          return playerZoneCache.entrySet().stream()
                  .filter(e -> zoneId.equals(e.getValue()))
                  .map(Map.Entry::getKey).toList();
      }

      // ── CRUD ─────────────────────────────────────────────────────────────────

      public void registerZone(Zone zone)     { zones.put(zone.getId(), zone); }

      public boolean removeZone(String id) {
          Zone removed = zones.remove(id);
          if (removed != null) {
              Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> zoneDAO.resetZone(id));
              removeZoneFromYml(id);
              return true;
          }
          return false;
      }

      public int getZoneCount()              { return zones.size(); }
      public boolean zoneExists(String id)   { return zones.containsKey(id); }
  }