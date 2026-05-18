package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.ZoneEvent;
import com.zerowars.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Gestor de eventos PvP automáticos y manuales.
 * Los eventos modifican el multiplicador de recompensas globalmente
 * o en zonas específicas durante un tiempo limitado.
 */
public class EventManager {

    private final ZeroWars plugin;

    // Eventos registrados (cargados desde events.yml)
    private final Map<String, ZoneEvent> events = new HashMap<>();

    // Eventos activos actualmente
    private final Map<String, ZoneEvent> activeEvents = new HashMap<>();

    // Tarea del scheduler automático
    private BukkitTask schedulerTask;
    // Tarea de verificación de expiración
    private BukkitTask expiryTask;

    public EventManager(ZeroWars plugin) {
        this.plugin = plugin;
        loadEvents();
    }

    // ── Carga ────────────────────────────────────────────────────────────────

    public void loadEvents() {
        events.clear();
        ConfigurationSection section =
                plugin.getConfigManager().events().getConfigurationSection("events");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            if (key.equals("scheduler")) continue;
            ConfigurationSection es = section.getConfigurationSection(key);
            if (es == null) continue;
            try {
                ZoneEvent event = parseEvent(key, es);
                events.put(key, event);
            } catch (Exception e) {
                plugin.getLogger().warning("Error parseando evento '" + key + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info("Eventos cargados: " + events.size());
    }

    private ZoneEvent parseEvent(String id, ConfigurationSection s) {
        ZoneEvent e = new ZoneEvent(id);
        e.setName(s.getString("name", id));
        e.setDescription(s.getString("description", ""));
        e.setDurationMinutes(s.getInt("duration", 10));
        e.setRewardMultiplier(s.getDouble("reward-multiplier", 1.0));
        e.setAffectedZoneIds(s.getStringList("affected-zones"));
        e.setActivateEventZone(s.getBoolean("activate-event-zone", false));
        e.setRainIntervalSeconds(s.getInt("rain-interval", 30));
        e.setStartBroadcast(s.getString("on-start.broadcast", ""));
        e.setEndBroadcast(s.getString("on-end.broadcast", ""));
        e.setStartSound(s.getString("on-start.sound", ""));
        e.setStartCommands(s.getStringList("on-start.commands"));
        e.setEndCommands(s.getStringList("on-end.commands"));

        // Rain items
        List<ZoneEvent.RainItem> rainItems = new ArrayList<>();
        if (s.isList("rain-items")) {
            for (Map<?, ?> rawMap : s.getMapList("rain-items")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) rawMap;
                String mat    = String.valueOf(map.getOrDefault("material", "IRON_INGOT"));
                int    amount = ((Number) map.getOrDefault("amount", 1)).intValue();
                double chance = ((Number) map.getOrDefault("chance", 0.5)).doubleValue();
                rainItems.add(new ZoneEvent.RainItem(mat, amount, chance));
            }
        }
        e.setRainItems(rainItems);
        return e;
    }

    // ── Scheduler automático ─────────────────────────────────────────────────

    public void startScheduler() {
        boolean enabled = plugin.getConfigManager().events()
                .getBoolean("events.scheduler.enabled", true);
        if (!enabled) return;

        long intervalMinutes = plugin.getConfigManager().events()
                .getLong("events.scheduler.interval", 60);
        long intervalTicks = intervalMinutes * 60 * 20;

        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::autoLaunchEvent,
                intervalTicks, intervalTicks);

        // Verificar expiración cada 20 ticks (1 segundo)
        expiryTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpiry, 20L, 20L);
    }

    private void autoLaunchEvent() {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        boolean random = plugin.getConfigManager().events()
                .getBoolean("events.scheduler.random", true);
        List<String> pool = plugin.getConfigManager().events()
                .getStringList("events.scheduler.auto-pool");
        if (pool.isEmpty()) return;

        String eventId;
        if (random) {
            eventId = pool.get(new Random().nextInt(pool.size()));
        } else {
            // Lanzar el primero que no esté activo
            eventId = pool.stream().filter(id -> !activeEvents.containsKey(id))
                    .findFirst().orElse(null);
        }
        if (eventId != null) startEvent(eventId);
    }

    private void checkExpiry() {
        List<String> toEnd = new ArrayList<>();
        for (ZoneEvent event : activeEvents.values()) {
            if (event.isExpired()) toEnd.add(event.getId());
        }
        for (String id : toEnd) endEvent(id);
    }

    // ── Control de eventos ────────────────────────────────────────────────────

    /**
     * Inicia un evento por ID. Devuelve false si ya está activo o no existe.
     */
    public boolean startEvent(String eventId) {
        ZoneEvent event = events.get(eventId);
        if (event == null || event.isActive()) return false;

        event.start();
        activeEvents.put(eventId, event);

        // Activar zonas de evento si corresponde
        if (event.isActivateEventZone()) {
            for (String zoneId : event.getAffectedZoneIds()) {
                plugin.getZoneManager().getZone(zoneId).ifPresent(z -> z.setEnabled(true));
            }
        }

        // Broadcast de inicio
        if (!event.getStartBroadcast().isEmpty()) {
            Bukkit.broadcast(MessageUtil.parse(
                    event.getStartBroadcast()
                            .replace("%event%", event.getName())
                            .replace("%duration%", String.valueOf(event.getDurationMinutes()))));
        }

        // Sonido global
        if (!event.getStartSound().isEmpty()) {
            try {
                Sound sound = Sound.valueOf(event.getStartSound());
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), sound, 1f, 1f));
            } catch (IllegalArgumentException ignored) {}
        }

        // Ejecutar comandos de inicio
        for (String cmd : event.getStartCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%event%", eventId));
        }

        plugin.getLogger().info("Evento iniciado: " + eventId);
        return true;
    }

    /**
     * Termina un evento por ID.
     */
    public boolean endEvent(String eventId) {
        ZoneEvent event = activeEvents.remove(eventId);
        if (event == null) return false;

        event.end();

        // Desactivar zonas de evento
        if (event.isActivateEventZone()) {
            for (String zoneId : event.getAffectedZoneIds()) {
                plugin.getZoneManager().getZone(zoneId).ifPresent(z -> z.setEnabled(false));
            }
        }

        // Broadcast de fin
        if (!event.getEndBroadcast().isEmpty()) {
            Bukkit.broadcast(MessageUtil.parse(
                    event.getEndBroadcast().replace("%event%", event.getName())));
        }

        // Comandos de fin
        for (String cmd : event.getEndCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%event%", eventId));
        }

        plugin.getLogger().info("Evento terminado: " + eventId);
        return true;
    }

    // ── Multiplicadores ───────────────────────────────────────────────────────

    /**
     * Calcula el multiplicador de recompensa total para una zona.
     * Suma todos los multiplicadores de eventos activos que afectan a esa zona.
     */
    public double getRewardMultiplier(String zoneId) {
        return activeEvents.values().stream()
                .filter(e -> e.affectsZone(zoneId))
                .mapToDouble(ZoneEvent::getRewardMultiplier)
                .max()
                .orElse(1.0);
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void shutdown() {
        if (schedulerTask != null) schedulerTask.cancel();
        if (expiryTask != null) expiryTask.cancel();
        new ArrayList<>(activeEvents.keySet()).forEach(this::endEvent);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Optional<ZoneEvent> getEvent(String id)    { return Optional.ofNullable(events.get(id)); }
    public Collection<ZoneEvent> getActiveEvents()    { return Collections.unmodifiableCollection(activeEvents.values()); }
    public Collection<ZoneEvent> getAllEvents()        { return Collections.unmodifiableCollection(events.values()); }
    public boolean isEventActive(String id)           { return activeEvents.containsKey(id); }
    public boolean eventExists(String id)             { return events.containsKey(id); }
    public int getActiveCount()                       { return activeEvents.size(); }
}
