package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.PlayerData;
import com.zerowars.models.Zone;
import com.zerowars.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestor del sistema Heat/Wanted.
 * Cuanto más domina un jugador, mayor su nivel de amenaza:
 *   Nivel 1 → Amenaza (amarillo)
 *   Nivel 2 → Peligroso (dorado)
 *   Nivel 3 → BUSCADO (rojo, glow, anuncio global)
 *
 * El heat expira con el tiempo o al morir.
 */
public class HeatManager {

    private final ZeroWars plugin;

    private final Map<Integer, Integer> levelThresholds = new HashMap<>();
    private final Map<Integer, String>  levelNames       = new HashMap<>();
    private int maxLevel = 3;

    public HeatManager(ZeroWars plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        ConfigurationSection levels = plugin.getConfigManager().config()
                .getConfigurationSection("heat.levels");
        if (levels == null) return;
        for (String key : levels.getKeys(false)) {
            try {
                int lvl       = Integer.parseInt(key);
                int threshold = levels.getInt(key + ".threshold", lvl * 2);
                String name   = levels.getString(key + ".name", "Level " + lvl);
                levelThresholds.put(lvl, threshold);
                levelNames.put(lvl, name);
                if (lvl > maxLevel) maxLevel = lvl;
            } catch (NumberFormatException ignored) {}
        }
    }

    // ── Eventos ───────────────────────────────────────────────────────────────

    public void onZoneCaptured(Player player, Zone zone) {
        if (!plugin.getConfigManager().isHeatEnabled()) return;
        PlayerData data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        int previousLevel = data.getHeatLevel();
        int newLevel      = calculateHeatLevel(data.getHeatCaptures());

        if (newLevel > previousLevel) {
            data.setHeatLevel(newLevel);
            long heatDuration = plugin.getConfigManager().config()
                    .getLong("heat.heat-duration", 600);
            data.setHeatExpireTime(System.currentTimeMillis() + (heatDuration * 1000L));

            String levelName = levelNames.getOrDefault(newLevel, "Nivel " + newLevel);
            Bukkit.broadcast(MessageUtil.parse(
                    plugin.getConfigManager().getMessage("heat.level-up",
                            "%player%", player.getName(), "%level_name%", levelName)));

            if (newLevel >= maxLevel) {
                boolean announce = plugin.getConfigManager().config()
                        .getBoolean("heat.announce-max-level", true);
                if (announce) {
                    Bukkit.broadcast(MessageUtil.parse(
                            plugin.getConfigManager().getMessage("heat.max-level-broadcast",
                                    "%player%", player.getName(),
                                    "%bounty%", String.valueOf(calculateBounty(data)))));
                }
                player.setGlowing(true);
            }
        }
    }

    public void onPlayerDeath(Player player, Player killer) {
        if (!plugin.getConfigManager().isHeatEnabled()) return;
        PlayerData data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
        if (data == null) return;

        int heatLevel = data.getHeatLevel();
        if (heatLevel <= 0) return;

        if (killer != null) {
            double reward = calculateBounty(data)
                    * plugin.getConfigManager().getHeatKillRewardMultiplier();
            killer.sendMessage(MessageUtil.parse(
                    plugin.getConfigManager().getMessage("heat.kill-reward",
                            "%player%", player.getName(),
                            "%reward%", String.format("%.0f", reward))));
        }

        if (heatLevel >= maxLevel) player.setGlowing(false);
        data.resetHeat();
    }

    public void checkExpiry(Player player) {
        if (!plugin.getConfigManager().isHeatEnabled()) return;
        PlayerData data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
        if (data == null || data.getHeatLevel() <= 0) return;

        long heatDuration = plugin.getConfigManager().config()
                .getLong("heat.heat-duration", 600);
        if (heatDuration <= 0) return;

        if (System.currentTimeMillis() >= data.getHeatExpireTime()) {
            if (data.getHeatLevel() >= maxLevel) player.setGlowing(false);
            data.resetHeat();
        }
    }

    // ── Lógica interna ────────────────────────────────────────────────────────

    private int calculateHeatLevel(int captures) {
        int level = 0;
        for (Map.Entry<Integer, Integer> e : levelThresholds.entrySet()) {
            if (captures >= e.getValue() && e.getKey() > level) {
                level = e.getKey();
            }
        }
        return Math.min(level, maxLevel);
    }

    private double calculateBounty(PlayerData data) {
        return (data.getKills() * 50.0) + (data.getCaptures() * 100.0);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public int getHeatLevel(UUID uuid) {
        PlayerData data = plugin.getRankingManager().getCachedPlayerData(uuid);
        return data != null ? data.getHeatLevel() : 0;
    }

    public String getHeatName(int level) {
        return levelNames.getOrDefault(level, "");
    }

    public boolean isWanted(UUID uuid) {
        return getHeatLevel(uuid) >= maxLevel;
    }

    public Component getHeatTag(Player player) {
        int level = getHeatLevel(player.getUniqueId());
        if (level <= 0) return Component.empty();
        return MessageUtil.parse(levelNames.getOrDefault(level, ""));
    }
}
