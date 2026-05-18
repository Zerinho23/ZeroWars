package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.storage.dao.ZoneDAO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor central de zonas PvP.
 * Cache en memoria ConcurrentHashMap (thread-safe).
 *
 * Cambios v1.2.0:
 *   - startPassiveRewardTick(): scheduler que entrega recompensas pasivas
 *     cada segundo a los duenos de zonas segun interval de rewards.yml.
 *   - tryLevelUp(Zone, Player): sube de nivel la zona segun tiempo dominado.
 */
public class ZoneManager {

    private final ZeroWars plugin;
    private final ZoneDAO zoneDAO;
    private final Map<String, Zone> zones = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerZoneCache = new ConcurrentHashMap<>();

    public ZoneManager(ZeroWars plugin) {
        this.plugin = plugin;
        this.zoneDAO = new ZoneDAO(plugin);
    }

    // -- Carga ----------------------------------------------------------------

    public void loadZones() {
        zones.clear();
        ConfigurationSection section = plugin.getConfigManager().zones()
                .getConfigurationSection("zones");
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
                final Zone fZone = zone;
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
        try { type = Zone.ZoneType.valueOf(s.getString("type", "MINE")); }
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

    // -- Guardar --------------------------------------------------------------

    public void saveAll() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Zone zone : zones.values()) zoneDAO.saveState(zone);
        });
    }

    public void saveAsync(Zone zone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> zoneDAO.saveState(zone));
    }

    // -- Passive reward tick --------------------------------------------------

    /**
     * Inicia el tick de recompensas pasivas (sync, hilo principal, cada 20 ticks = 1s).
     * Para cada zona habilitada con dueno online, verifica si el intervalo configurado
     * ha transcurrido y llama a RewardManager.givePassiveReward().
     *
     * Debe invocarse DESPUES de que RewardManager este inicializado.
     */
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
                // Acumular tiempo dominado y verificar level-up
                zone.addDomineTime(intervalMs);
                tryLevelUp(zone, owner);
            }
        }, 20L, 20L);
    }

    // -- Zone level-up --------------------------------------------------------

    /**
     * Verifica y aplica la subida de nivel de una zona.
     * Cada nivel requiere (nivel_actual * domine-minutes-per-level) minutos de dominio.
     * Con 5 min/nivel: nivel 1->2 = 5 min total, 2->3 = 10 min, etc.
     */
    public void tryLevelUp(Zone zone, Player owner) {
        if (zone.getLevel() >= zone.getMaxLevel()) return;
        long msPerLevel = plugin.getConfigManager().config()
                .getLong("zones.level-up.domine-minutes-per-level", 5) * 60_000L;
        long required = (long) zone.getLevel() * msPerLevel;
        if (zone.getTotalDomineTime() < required) return;
        zone.setLevel(zone.getLevel() + 1);
        saveAsync(zone);
        plugin.getRewardManager().giveLevelUpReward(owner, zone, zone.getLevel());
        plugin.getLogger().info("[ZoneLevel] " + zone.getId() + " nivel " + zone.getLevel()
                + " (dueno: " + owner.getName() + ")");
    }

    // -- Acceso y busqueda ---------------------------------------------------

    public Optional<Zone> getZone(String id)         { return Optional.ofNullable(zones.get(id)); }
    public Collection<Zone> getAllZones()             { return Collections.unmodifiableCollection(zones.values()); }

    public List<Zone> getEnabledZones() {
        return zones.values().stream()
                .filter(Zone::isEnabled).filter(z -> !z.isEventOnly()).toList();
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

    // -- Cache de posicion ----------------------------------------------------

    public void setPlayerZone(UUID uuid, String zoneId) {
        if (zoneId == null) playerZoneCache.remove(uuid);
        else playerZoneCache.put(uuid, zoneId);
    }

    public String getPlayerZoneId(UUID uuid)         { return playerZoneCache.get(uuid); }
    public boolean isInZone(UUID uuid)               { return playerZoneCache.containsKey(uuid); }

    public Optional<Zone> getPlayerZone(UUID uuid) {
        String zoneId = playerZoneCache.get(uuid);
        return zoneId != null ? getZone(zoneId) : Optional.empty();
    }

    public List<UUID> getPlayersInZone(String zoneId) {
        return playerZoneCache.entrySet().stream()
                .filter(e -> zoneId.equals(e.getValue()))
                .map(Map.Entry::getKey).toList();
    }

    // -- CRUD -----------------------------------------------------------------

    public void registerZone(Zone zone)               { zones.put(zone.getId(), zone); }

    public boolean removeZone(String id) {
        Zone removed = zones.remove(id);
        if (removed != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> zoneDAO.resetZone(id));
            return true;
        }
        return false;
    }

    public int getZoneCount()              { return zones.size(); }
    public boolean zoneExists(String id)   { return zones.containsKey(id); }
}