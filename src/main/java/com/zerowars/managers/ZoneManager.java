package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.storage.dao.ZoneDAO;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor central de zonas PvP.
 * Cache en memoria con ConcurrentHashMap para acceso thread-safe.
 * La carga desde YML y la persistencia en SQLite son async-friendly.
 */
public class ZoneManager {

    private final ZeroWars plugin;
    private final ZoneDAO zoneDAO;

    // Cache principal: zoneId → Zone
    private final Map<String, Zone> zones = new ConcurrentHashMap<>();

    // Cache de jugadores en zona: playerUUID → zoneId (null = fuera de zona)
    private final Map<UUID, String> playerZoneCache = new ConcurrentHashMap<>();

    public ZoneManager(ZeroWars plugin) {
        this.plugin = plugin;
        this.zoneDAO = new ZoneDAO(plugin);
    }

    // ── Carga ────────────────────────────────────────────────────────────────

    /**
     * Carga todas las zonas definidas en zones.yml,
     * luego restaura su estado persistente desde SQLite.
     */
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

                // Carga estado persistente async para no bloquear el hilo principal
                final Zone fZone = zone;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> zoneDAO.loadState(fZone));
            } catch (Exception e) {
                plugin.getLogger().warning("Error parseando zona '" + key + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info("Zonas cargadas: " + zones.size());
    }

    private Zone parseZone(String id, ConfigurationSection s) {
        String rawName  = s.getString("name", id);
        String typeStr  = s.getString("type", "MINE");
        Zone.ZoneType type;
        try { type = Zone.ZoneType.valueOf(typeStr); }
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

        int level       = s.getInt("level", 1);
        int maxLevel    = s.getInt("max-level", 5);
        int captureTime = s.getInt("capture-time", 30);

        Zone zone = new Zone(id, rawName, type, world,
                minX, minY, minZ, maxX, maxY, maxZ,
                level, maxLevel, captureTime);

        zone.setEnabled(s.getBoolean("enabled", true));
        zone.setEventOnly(s.getBoolean("event-only", false));
        zone.setProtected(s.getBoolean("protected", true));
        zone.setRewardIds(s.getStringList("rewards"));

        return zone;
    }

    // ── Guardar ──────────────────────────────────────────────────────────────

    /** Guarda todas las zonas de forma asíncrona. */
    public void saveAll() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Zone zone : zones.values()) {
                zoneDAO.saveState(zone);
            }
        });
    }

    /** Guarda una zona específica de forma asíncrona. */
    public void saveAsync(Zone zone) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> zoneDAO.saveState(zone));
    }

    // ── Acceso y búsqueda ────────────────────────────────────────────────────

    public Optional<Zone> getZone(String id) {
        return Optional.ofNullable(zones.get(id));
    }

    public Collection<Zone> getAllZones() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public List<Zone> getEnabledZones() {
        return zones.values().stream()
                .filter(Zone::isEnabled)
                .filter(z -> !z.isEventOnly())
                .toList();
    }

    /**
     * Encuentra la zona activa en la que se encuentra una localización.
     * Optimizado: itera solo zonas habilitadas del mismo mundo.
     */
    public Optional<Zone> getZoneAt(Location location) {
        if (location == null || location.getWorld() == null) return Optional.empty();
        String worldName = location.getWorld().getName();
        return zones.values().stream()
                .filter(Zone::isEnabled)
                .filter(z -> z.getWorldName().equals(worldName))
                .filter(z -> z.contains(location))
                .findFirst();
    }

    // ── Cache de posición de jugadores ───────────────────────────────────────

    /** Actualiza la zona en la que está un jugador (null = fuera de zona). */
    public void setPlayerZone(UUID uuid, String zoneId) {
        if (zoneId == null) {
            playerZoneCache.remove(uuid);
        } else {
            playerZoneCache.put(uuid, zoneId);
        }
    }

    /** Obtiene el ID de la zona donde está el jugador (null = fuera). */
    public String getPlayerZoneId(UUID uuid) {
        return playerZoneCache.get(uuid);
    }

    /** ¿El jugador está en alguna zona? */
    public boolean isInZone(UUID uuid) {
        return playerZoneCache.containsKey(uuid);
    }

    /** Obtiene la zona donde está el jugador. */
    public Optional<Zone> getPlayerZone(UUID uuid) {
        String zoneId = playerZoneCache.get(uuid);
        return zoneId != null ? getZone(zoneId) : Optional.empty();
    }

    // ── Jugadores en zona ─────────────────────────────────────────────────────

    /** Lista de UUIDs de jugadores actualmente en la zona dada. */
    public List<UUID> getPlayersInZone(String zoneId) {
        return playerZoneCache.entrySet().stream()
                .filter(e -> zoneId.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .toList();
    }

    // ── CRUD de zonas ─────────────────────────────────────────────────────────

    public void registerZone(Zone zone) {
        zones.put(zone.getId(), zone);
    }

    public boolean removeZone(String id) {
        Zone removed = zones.remove(id);
        if (removed != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> zoneDAO.resetZone(id));
            return true;
        }
        return false;
    }

    public int getZoneCount() { return zones.size(); }

    public boolean zoneExists(String id) { return zones.containsKey(id); }
}
