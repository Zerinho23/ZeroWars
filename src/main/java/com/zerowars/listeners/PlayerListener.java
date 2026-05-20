package com.zerowars.listeners;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener de eventos de jugador:
 * - Join/Quit: carga y guarda datos del jugador
 * - Move: detecta entrada/salida de zonas
 * - Death: registra kills/deaths, aplica lógica de heat
 * - Anti-logout: desconectarse dentro de una zona cuenta como derrota
 */
public class PlayerListener implements Listener {

    private final ZeroWars plugin;

    public PlayerListener(ZeroWars plugin) {
        this.plugin = plugin;
    }

    // ── Join / Quit ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getRankingManager().loadPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // ── Anti-logout: desconectarse dentro de una zona cuenta como derrota ──
        // Se ejecuta ANTES de limpiar el caché para que registerKill pueda
        // encontrar los datos del jugador y guardarlos con la muerte incluida.
        String zoneId = plugin.getZoneManager().getPlayerZoneId(uuid);
        if (zoneId != null) {
            // Registrar muerte por desconexión en el ranking
            plugin.getRankingManager().registerKill(null, uuid);

            // Aplicar penalización de heat (igual que una muerte normal sin killer)
            plugin.getHeatManager().onPlayerDeath(player, null);

            // Notificar a los jugadores que están dentro de esa zona
            String zoneName = plugin.getZoneManager().getZone(zoneId)
                    .map(Zone::getDisplayName).orElse(zoneId);
            String msg = plugin.getConfigManager().getMessage(
                    "zone.logout-in-zone",
                    "%player%", player.getName(),
                    "%zone%", zoneName);
            plugin.getZoneManager().getPlayersInZone(zoneId).forEach(inZoneUuid -> {
                if (inZoneUuid.equals(uuid)) return;
                Player other = Bukkit.getPlayer(inZoneUuid);
                if (other != null) other.sendMessage(MessageUtil.parse(msg));
            });

            plugin.getLogger().info("[AntiLogout] " + player.getName()
                    + " se desconectó en zona " + zoneId + " — muerte registrada.");
        }

        // Cancelar captura activa si la había
        if (plugin.getCaptureManager().isPlayerCapturing(uuid)) {
            if (zoneId != null) {
                plugin.getCaptureManager().cancelCapture(zoneId, "capture.cancelled");
            }
        }

        plugin.getZoneManager().setPlayerZone(uuid, null);
        plugin.getCooldownManager().cleanup(uuid);
        plugin.getRankingManager().unloadPlayer(uuid);
    }

    // ── Movimiento ───────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Solo procesar si el jugador cambió de bloque
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        String previousZoneId = plugin.getZoneManager().getPlayerZoneId(player.getUniqueId());
        Optional<Zone> currentZoneOpt = plugin.getZoneManager().getZoneAt(event.getTo());
        String currentZoneId = currentZoneOpt.map(Zone::getId).orElse(null);

        if (Objects.equals(previousZoneId, currentZoneId)) return;

        if (previousZoneId != null) onZoneLeave(player, previousZoneId);
        if (currentZoneId  != null) onZoneEnter(player, currentZoneOpt.get());

        plugin.getZoneManager().setPlayerZone(player.getUniqueId(), currentZoneId);
    }

    private void onZoneEnter(Player player, Zone zone) {
        player.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("zone.enter",
                        "%zone%", zone.getDisplayName(),
                        "%owner%", zone.isOwned() ? zone.getOwnerName() : "Nadie")));

        // Sonido de entrada
        try {
            String soundName = plugin.getConfigManager().config()
                    .getString("effects.sounds.zone-enter", "ENTITY_PLAYER_LEVELUP");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound,
                    plugin.getConfigManager().getSoundVolume(),
                    plugin.getConfigManager().getSoundPitch());
        } catch (IllegalArgumentException ignored) {}

        // Solo iniciar captura si el jugador no es el dueño y no hay captura activa
        if (zone.isOwnedBy(player.getUniqueId())
                || plugin.getCaptureManager().isCapturing(zone.getId())
                || !player.hasPermission("zerowars.zone.capture")) return;

        // Verificar cooldown de recaptura
        var data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
        if (data != null && data.isOnRecaptureCooldown(zone.getId())) {
            long remaining = data.getRecaptureCooldownSeconds(zone.getId());
            player.sendActionBar(MessageUtil.parse(
                    "<red>⏳ Cooldown de recaptura: <white>" + remaining + "s"));
            return;
        }

        int startDelay = plugin.getConfigManager().config()
                .getInt("zones.start-delay", 3) * 20;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Confirmar que sigue en la zona tras el delay
            if (plugin.getZoneManager().isInZone(player.getUniqueId())
                    && zone.getId().equals(
                            plugin.getZoneManager().getPlayerZoneId(player.getUniqueId()))) {
                plugin.getCaptureManager().startCapture(player, zone);
            }
        }, startDelay);
    }

    private void onZoneLeave(Player player, String zoneId) {
        plugin.getZoneManager().getZone(zoneId).ifPresent(zone ->
                player.sendMessage(MessageUtil.parse(
                        plugin.getConfigManager().getMessage("zone.leave",
                                "%zone%", zone.getDisplayName()))));

        if (plugin.getCaptureManager().isPlayerCapturing(player.getUniqueId())) {
            boolean cancelOnLeave = plugin.getConfigManager().config()
                    .getBoolean("zones.cancel-on-leave", true);
            if (cancelOnLeave) {
                plugin.getCaptureManager().cancelCapture(zoneId, "capture.cancelled");
            }
        }
    }

    // ── Muerte ────────────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        plugin.getRankingManager().registerKill(
                killer != null ? killer.getUniqueId() : null,
                victim.getUniqueId());

        plugin.getHeatManager().onPlayerDeath(victim, killer);

        String zoneId = plugin.getZoneManager().getPlayerZoneId(victim.getUniqueId());
        if (zoneId != null && plugin.getCaptureManager().isPlayerCapturing(victim.getUniqueId())) {
            plugin.getCaptureManager().cancelCapture(zoneId, "capture.cancelled");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getHeatManager().checkExpiry(event.getPlayer());
    }
}
