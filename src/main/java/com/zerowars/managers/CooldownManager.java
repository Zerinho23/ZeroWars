package com.zerowars.managers;

import com.zerowars.ZeroWars;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de cooldowns globales en memoria.
 * Para cooldowns individuales de consumibles, ver PlayerData.
 * Este manager gestiona el cooldown GLOBAL entre cualquier consumible.
 *
 * Thread-safe mediante ConcurrentHashMap.
 */
public class CooldownManager {

    private final ZeroWars plugin;

    // Cooldown global: playerUUID → timestamp de expiración en ms
    private final Map<UUID, Long> globalCooldowns = new ConcurrentHashMap<>();

    // Cache de último cooldown activo para ActionBar
    private final Map<UUID, String> lastActiveConsumable = new ConcurrentHashMap<>();

    public CooldownManager(ZeroWars plugin) {
        this.plugin = plugin;
    }

    // ── Cooldown Global ──────────────────────────────────────────────────────

    /** ¿El jugador tiene cooldown global activo? */
    public boolean hasGlobalCooldown(UUID uuid) {
        Long expire = globalCooldowns.get(uuid);
        if (expire == null) return false;
        if (System.currentTimeMillis() >= expire) {
            globalCooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    /** Segundos restantes del cooldown global. */
    public long getGlobalCooldownSeconds(UUID uuid) {
        Long expire = globalCooldowns.get(uuid);
        if (expire == null) return 0;
        long remaining = expire - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 1000L) + 1 : 0;
    }

    /** Establece el cooldown global para un jugador. */
    public void setGlobalCooldown(UUID uuid, long seconds) {
        if (seconds <= 0) return;
        globalCooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    /** Limpia el cooldown global de un jugador. */
    public void clearGlobalCooldown(UUID uuid) {
        globalCooldowns.remove(uuid);
    }

    // ── Seguimiento del último consumible para ActionBar ─────────────────────

    public void setLastActiveConsumable(UUID uuid, String consumableId) {
        lastActiveConsumable.put(uuid, consumableId);
    }

    public String getLastActiveConsumable(UUID uuid) {
        return lastActiveConsumable.get(uuid);
    }

    public void clearLastActiveConsumable(UUID uuid) {
        lastActiveConsumable.remove(uuid);
    }

    // ── Limpieza ──────────────────────────────────────────────────────────────

    /** Elimina datos de un jugador al desconectarse. */
    public void cleanup(UUID uuid) {
        globalCooldowns.remove(uuid);
        lastActiveConsumable.remove(uuid);
    }

    /** Limpia cooldowns expirados de la cache. Llamar periódicamente. */
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        globalCooldowns.entrySet().removeIf(e -> e.getValue() <= now);
    }

    public int getActiveCount() { return globalCooldowns.size(); }
}
