package com.zerowars.api;

import com.zerowars.ZeroWars;
import com.zerowars.models.Consumable;
import com.zerowars.models.PlayerData;
import com.zerowars.models.Zone;
import com.zerowars.models.ZoneCapture;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * API pública de ZeroWars para integración con otros plugins.
 *
 * Uso desde otro plugin:
 *   ZeroWarsAPI api = ZeroWars.getInstance().getAPI();
 *   Optional<Zone> zone = api.getZoneAt(player.getLocation());
 *
 * Todos los métodos son thread-safe para lectura.
 * Las modificaciones de estado deben hacerse en el hilo principal de Bukkit.
 */
public class ZeroWarsAPI {

    private final ZeroWars plugin;

    public ZeroWarsAPI(ZeroWars plugin) {
        this.plugin = plugin;
    }

    // ── Zonas ────────────────────────────────────────────────────────────────

    /**
     * Obtiene la zona en la que está el jugador.
     */
    public Optional<Zone> getPlayerZone(Player player) {
        return plugin.getZoneManager().getPlayerZone(player.getUniqueId());
    }

    /**
     * Obtiene la zona en una localización específica.
     */
    public Optional<Zone> getZoneAt(org.bukkit.Location location) {
        return plugin.getZoneManager().getZoneAt(location);
    }

    /**
     * Obtiene una zona por ID.
     */
    public Optional<Zone> getZone(String zoneId) {
        return plugin.getZoneManager().getZone(zoneId);
    }

    /**
     * Lista todas las zonas disponibles.
     */
    public Collection<Zone> getAllZones() {
        return plugin.getZoneManager().getAllZones();
    }

    /**
     * ¿Está el jugador en alguna zona?
     */
    public boolean isInZone(Player player) {
        return plugin.getZoneManager().isInZone(player.getUniqueId());
    }

    /**
     * ¿Está la zona siendo capturada actualmente?
     */
    public boolean isZoneBeingCaptured(String zoneId) {
        return plugin.getCaptureManager().isCapturing(zoneId);
    }

    /**
     * Obtiene la captura activa de una zona.
     */
    public Optional<ZoneCapture> getActiveCapture(String zoneId) {
        return Optional.ofNullable(plugin.getCaptureManager().getCapture(zoneId));
    }

    // ── Jugadores ─────────────────────────────────────────────────────────────

    /**
     * Obtiene los datos del jugador cacheados en memoria.
     * Null si el jugador no está online o sus datos aún se están cargando.
     */
    public PlayerData getPlayerData(UUID uuid) {
        return plugin.getRankingManager().getCachedPlayerData(uuid);
    }

    /**
     * ¿El jugador tiene heat activo?
     */
    public boolean isPlayerWanted(Player player) {
        return plugin.getHeatManager().isWanted(player.getUniqueId());
    }

    /**
     * Nivel de heat del jugador (0 = sin heat, 3 = máximo).
     */
    public int getPlayerHeatLevel(Player player) {
        return plugin.getHeatManager().getHeatLevel(player.getUniqueId());
    }

    // ── Consumibles ───────────────────────────────────────────────────────────

    /**
     * Da un consumible al jugador y lo construye como ItemStack.
     */
    public org.bukkit.inventory.ItemStack buildConsumableItem(String consumableId, int amount) {
        return plugin.getConsumableManager().buildItem(consumableId, amount);
    }

    /**
     * ¿Está el jugador en cooldown de un consumible?
     */
    public boolean isOnConsumableCooldown(Player player, String consumableId) {
        PlayerData data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
        return data != null && data.isOnConsumableCooldown(consumableId);
    }

    /**
     * Obtiene un consumible registrado por ID.
     */
    public Optional<Consumable> getConsumable(String id) {
        return plugin.getConsumableManager().getConsumable(id);
    }

    // ── Eventos ───────────────────────────────────────────────────────────────

    /**
     * Inicia un evento por ID. Devuelve false si ya está activo.
     */
    public boolean startEvent(String eventId) {
        return plugin.getEventManager().startEvent(eventId);
    }

    /**
     * Termina un evento activo.
     */
    public boolean endEvent(String eventId) {
        return plugin.getEventManager().endEvent(eventId);
    }

    /**
     * ¿Hay algún evento activo?
     */
    public boolean hasActiveEvents() {
        return plugin.getEventManager().getActiveCount() > 0;
    }

    /**
     * Multiplicador de recompensas activo para una zona.
     */
    public double getRewardMultiplier(String zoneId) {
        return plugin.getEventManager().getRewardMultiplier(zoneId);
    }

    // ── Clanes ────────────────────────────────────────────────────────────────

    /**
     * ID del clan del jugador (null si no tiene).
     */
    public String getPlayerClanId(UUID uuid) {
        return plugin.getClanManager().getClanOfPlayer(uuid);
    }

    /**
     * ¿Dos jugadores son del mismo clan?
     */
    public boolean areSameClan(UUID a, UUID b) {
        return plugin.getClanManager().areSameClan(a, b);
    }
}
