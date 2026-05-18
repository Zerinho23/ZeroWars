package com.zerowars.config;

import com.zerowars.ZeroWars;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Gestor centralizado de todos los archivos de configuración.
 * Carga, recarga y expone cada yml de forma tipada.
 * Patrón: acceso por clave estática para evitar strings sueltos.
 */
public class ConfigManager {

    public static final String CONFIG      = "config";
    public static final String MESSAGES    = "messages";
    public static final String ZONES       = "zones";
    public static final String REWARDS     = "rewards";
    public static final String CONSUMABLES = "consumables";
    public static final String COOLDOWNS   = "cooldowns";
    public static final String EVENTS      = "events";

    private final ZeroWars plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> files = new HashMap<>();

    public ConfigManager(ZeroWars plugin) {
        this.plugin = plugin;
    }

    /**
     * Carga todos los archivos de configuración desde el dataFolder del plugin.
     * Si no existen, se copian desde los resources incluidos en el jar.
     */
    public void loadAll() {
        saveDefault(CONFIG,      "config.yml");
        saveDefault(MESSAGES,    "messages.yml");
        saveDefault(ZONES,       "zones.yml");
        saveDefault(REWARDS,     "rewards.yml");
        saveDefault(CONSUMABLES, "consumables.yml");
        saveDefault(COOLDOWNS,   "cooldowns.yml");
        saveDefault(EVENTS,      "events.yml");
        plugin.getLogger().info("Configuraciones cargadas: " + configs.size() + " archivos.");
    }

    /**
     * Recarga todos los archivos de configuración desde disco.
     * Llamado por /zw reload.
     */
    public void reloadAll() {
        configs.clear();
        loadAll();
    }

    /**
     * Guarda el archivo de config y lo carga en memoria.
     */
    private void saveDefault(String key, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(key, config);
        files.put(key, file);
    }

    /**
     * Guarda cambios de un config en disco.
     */
    public void save(String key) {
        FileConfiguration cfg = configs.get(key);
        File file = files.get(key);
        if (cfg == null || file == null) return;
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo guardar " + key, e);
        }
    }

    // ── Acceso tipado ────────────────────────────────────────────────────────

    public FileConfiguration get(String key) {
        return configs.getOrDefault(key, plugin.getConfig());
    }

    public FileConfiguration config()      { return get(CONFIG); }
    public FileConfiguration messages()    { return get(MESSAGES); }
    public FileConfiguration zones()       { return get(ZONES); }
    public FileConfiguration rewards()     { return get(REWARDS); }
    public FileConfiguration consumables() { return get(CONSUMABLES); }
    public FileConfiguration cooldowns()   { return get(COOLDOWNS); }
    public FileConfiguration events()      { return get(EVENTS); }

    // ── Helpers tipados para config.yml ──────────────────────────────────────

    public String getPrefix() {
        return config().getString("general.prefix", "<gradient:#ff4444:#ff8800>[ZeroWars]</gradient>");
    }

    public boolean isDebug() {
        return config().getBoolean("general.debug", false);
    }

    public int getAutosaveInterval() {
        return config().getInt("general.autosave-interval", 300);
    }

    public double getCaptureRadius() {
        return config().getDouble("general.capture-radius", 20.0);
    }

    public int getCaptureBaseTime() {
        return config().getInt("capture.base-time", 30);
    }

    public int getCaptureTimeReductionPerPlayer() {
        return config().getInt("capture.time-reduction-per-player", 5);
    }

    public int getCaptureMinTime() {
        return config().getInt("capture.min-capture-time", 10);
    }

    public boolean isPauseOnContest() {
        return config().getBoolean("capture.pause-on-contest", true);
    }

    public boolean isHeatEnabled() {
        return config().getBoolean("heat.enabled", true);
    }

    public double getHeatKillRewardMultiplier() {
        return config().getDouble("heat.kill-reward-multiplier", 2.0);
    }

    public int getTopSize() {
        return config().getInt("ranking.top-size", 10);
    }

    public float getSoundVolume() {
        return (float) config().getDouble("effects.volume", 1.0);
    }

    public float getSoundPitch() {
        return (float) config().getDouble("effects.pitch", 1.0);
    }

    // ── Helpers para messages.yml ────────────────────────────────────────────

    public String getMessage(String path) {
        String msg = messages().getString(path);
        if (msg == null) {
            if (isDebug()) plugin.getLogger().warning("Mensaje no encontrado: " + path);
            return "<red>[ZeroWars] Mensaje no encontrado: " + path;
        }
        return msg;
    }

    public String getMessage(String path, String... placeholders) {
        String msg = getMessage(path);
        for (int i = 0; i < placeholders.length - 1; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return msg;
    }
}
