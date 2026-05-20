package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.PlayerData;
import com.zerowars.storage.dao.PlayerDAO;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RankingManager {

    private final ZeroWars plugin;
    private final PlayerDAO playerDAO;

    private final Map<UUID, PlayerData> playerCache = new ConcurrentHashMap<>();
    private final Map<String, List<PlayerData>> rankingCache = new HashMap<>();

    public RankingManager(ZeroWars plugin) {
        this.plugin = plugin;
        this.playerDAO = new PlayerDAO(plugin);
    }

    public void loadRankings() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            refreshRanking("kills");
            refreshRanking("captures");
            refreshRanking("total_domine_ms");
            plugin.getLogger().info("Rankings cargados correctamente.");
        });
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = playerDAO.load(uuid);
            if (data == null) {
                playerDAO.createNew(uuid, name);
                data = new PlayerData(uuid, name);
            }
            data.setName(name);
            playerCache.put(uuid, data);
        });
    }

    public void unloadPlayer(UUID uuid) {
        PlayerData data = playerCache.remove(uuid);
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> playerDAO.save(data));
        }
    }

    public void saveAll() {
        for (PlayerData data : playerCache.values()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> playerDAO.save(data));
        }
    }

    public PlayerData getCachedPlayerData(UUID uuid) {
        return playerCache.get(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return playerCache.containsKey(uuid);
    }

    public void refreshRanking(String column) {
        int size = plugin.getConfigManager().getTopSize();
        List<PlayerData> top = playerDAO.getTop(column, size);
        synchronized (rankingCache) {
            rankingCache.put(column, top);
        }
    }

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

    /**
     * Registra un kill/death. killerUUID puede ser null (muerte por entorno).
     * FIX: ConcurrentHashMap no acepta claves null — verificar antes de llamar get().
     */
    public void registerKill(UUID killerUUID, UUID victimUUID) {
        if (killerUUID != null) {
            PlayerData killer = playerCache.get(killerUUID);
            if (killer != null) killer.addKill();
        }
        if (victimUUID != null) {
            PlayerData victim = playerCache.get(victimUUID);
            if (victim != null) victim.addDeath();
        }
    }
}
