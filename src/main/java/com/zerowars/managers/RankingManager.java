package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.PlayerData;
import com.zerowars.storage.dao.PlayerDAO;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de jugadores online y ranking.
 * Cache en memoria de todos los PlayerData de jugadores conectados.
 * Los datos se cargan al entrar y se guardan al salir de forma asíncrona.
 */
public class RankingManager {

    private final ZeroWars plugin;
    private final PlayerDAO playerDAO;

    // Cache de jugadores online
    private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();

    // Cache de ranking (actualizado periódicamente)
    private final Map<String, List<PlayerData>> rankingCache = new HashMap<>();

    public RankingManager(ZeroWars plugin) {
        this.plugin = plugin;
        this.playerDAO = new PlayerDAO(plugin);
    }

    // ── Carga inicial ─────────────────────────────────────────────────────────

    /**
     * Carga rankings iniciales. Llama a este método una vez al iniciar.
     */
    public void loadRankings() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            refreshRanking("kills");
            refreshRanking("captures");
            refreshRanking("total_domine_ms");
            plugin.getLogger().info("Rankings cargados correctamente.");
        });
    }

    // ── Gestión de jugadores ──────────────────────────────────────────────────

    /**
     * Carga los datos de un jugador al entrar al servidor.
     * Si no existe, crea un registro nuevo.
     */
    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = playerDAO.load(uuid);
            if (data == null) {
                playerDAO.createNew(uuid, player.getName());
                data = new PlayerData(uuid, player.getName());
            }
            data.setName(player.getName()); // actualizar nombre por si cambió
            PlayerData finalData = data;
            playerCache.put(uuid, finalData);
        });
    }

    /**
     * Guarda y elimina del cache al desconectarse.
     */
    public void unloadPlayer(UUID uuid) {
        PlayerData data = playerCache.remove(uuid);
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> playerDAO.save(data));
        }
    }

    /**
     * Guarda todos los jugadores online de forma asíncrona.
     */
    public void saveAll() {
        for (PlayerData data : playerCache.values()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> playerDAO.save(data));
        }
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    public PlayerData getCachedPlayerData(UUID uuid) {
        return playerCache.get(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return playerCache.containsKey(uuid);
    }

    // ── Ranking ───────────────────────────────────────────────────────────────

    /**
     * Actualiza el ranking para una categoría desde la BD.
     * Ejecutar siempre de forma asíncrona.
     */
    public void refreshRanking(String column) {
        int size = plugin.getConfigManager().getTopSize();
        List<PlayerData> top = playerDAO.getTop(column, size);
        synchronized (rankingCache) {
            rankingCache.put(column, top);
        }
    }

    /**
     * Obtiene el ranking cacheado para una categoría.
     */
    public List<PlayerData> getRanking(String category) {
        String column = switch (category.toLowerCase()) {
            case "kills"         -> "kills";
            case "captures"      -> "captures";
            case "time-dominated", "time" -> "total_domine_ms";
            default              -> "kills";
        };
        synchronized (rankingCache) {
            return rankingCache.getOrDefault(column, Collections.emptyList());
        }
    }

    // ── Estadísticas ──────────────────────────────────────────────────────────

    public void registerKill(UUID killerUUID, UUID victimUUID) {
        PlayerData killer = playerCache.get(killerUUID);
        PlayerData victim = playerCache.get(victimUUID);
        if (killer != null) killer.addKill();
        if (victim != null) victim.addDeath();
    }
}
