package com.zerowars.listeners;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.utils.EffectUtil;
import com.zerowars.utils.MessageUtil;
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

import java.util.Optional;

/**
 * Listener de eventos de jugador:
 * - Join/Quit: carga y guarda datos del jugador
 * - Move: detecta entrada/salida de zonas
 * - Death: registra kills/deaths, aplica lógica de heat
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
        // Si estaba capturando, cancelar
        String capturingZone = plugin.getCaptureManager()
                .isPlayerCapturing(player.getUniqueId())
                ? plugin.getZoneManager().getPlayerZoneId(player.getUniqueId())
                : null;
        if (capturingZone != null) {
            plugin.getCaptureManager().cancelCapture(capturingZone, "capture.cancelled");
        }

        // Limpiar zona actual del jugador
        plugin.getZoneManager().setPlayerZone(player.getUniqueId(), null);

        // Limpiar cooldowns en memoria
        plugin.getCooldownManager().cleanup(player.getUniqueId());

        // Guardar datos
        plugin.getRankingManager().unloadPlayer(player.getUniqueId());
    }

    // ── Movimiento: detección de entrada/salida de zonas ────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        // Optimización: solo procesar si cambió bloque completo
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;

        Player player = event.getPlayer();
        String previousZoneId = plugin.getZoneManager().getPlayerZoneId(player.getUniqueId());
        Optional<Zone> currentZoneOpt = plugin.getZoneManager().getZoneAt(event.getTo());

        String currentZoneId = currentZoneOpt.map(Zone::getId).orElse(null);

        // Sin cambio de zona
        if (java.util.Objects.equals(previousZoneId, currentZoneId)) return;

        // Salió de una zona
        if (previousZoneId != null) {
            onZoneLeave(player, previousZoneId);
        }

        // Entró a una zona
        if (currentZoneId != null) {
            onZoneEnter(player, currentZoneOpt.get());
        }

        plugin.getZoneManager().setPlayerZone(player.getUniqueId(), currentZoneId);
    }

    private void onZoneEnter(Player player, Zone zone) {
        // Mensaje de entrada
        String msg = plugin.getConfigManager().getMessage("zone.enter",
                "%zone%", zone.getDisplayName(),
                "%owner%", zone.isOwned() ? zone.getOwnerName() : "Nadie");
        player.sendMessage(MessageUtil.parse(msg));

        // Sonido de entrada
        try {
            Sound sound = Sound.valueOf(plugin.getConfigManager().config()
                    .getString("effects.sounds.zone-enter", "ENTITY_PLAYER_LEVELUP"));
            player.playSound(player.getLocation(), sound,
                    plugin.getConfigManager().getSoundVolume(),
                    plugin.getConfigManager().getSoundPitch());
        } catch (IllegalArgumentException ignored) {}

        // Iniciar captura si no es el dueño y no hay captura activa ya
        if (!zone.isOwnedBy(player.getUniqueId())
                && !plugin.getCaptureManager().isCapturing(zone.getId())
                && player.hasPermission("zerowars.zone.capture")) {

            // Verificar cooldown de recaptura
            var data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
            if (data != null && data.isOnRecaptureCooldown(zone.getId())) {
                long remaining = data.getRecaptureCooldownSeconds(zone.getId());
                player.sendActionBar(MessageUtil.parse(
                        "<red>⏳ Cooldown de recaptura: <white>" + remaining + "s"));
                return;
            }

            // Pequeño delay antes de iniciar captura (config: start-delay)
            int startDelay = plugin.getConfigManager().config()
                    .getInt("zones.start-delay", 3) * 20;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Verificar que siga en la zona después del delay
                if (plugin.getZoneManager().isInZone(player.getUniqueId())
                        && zone.getId().equals(plugin.getZoneManager().getPlayerZoneId(player.getUniqueId()))) {
                    plugin.getCaptureManager().startCapture(player, zone);
                }
            }, startDelay);
        }
    }

    private void onZoneLeave(Player player, String zoneId) {
        // Mensaje de salida
        plugin.getZoneManager().getZone(zoneId).ifPresent(zone -> {
            player.sendMessage(MessageUtil.parse(
                    plugin.getConfigManager().getMessage("zone.leave",
                            "%zone%", zone.getDisplayName())));
        });

        // Cancelar BossBar de captura si era el atacante
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

        // Registrar estadísticas
        plugin.getRankingManager().registerKill(
                killer != null ? killer.getUniqueId() : null,
                victim.getUniqueId());

        // Heat: resetear al morir
        plugin.getHeatManager().onPlayerDeath(victim, killer);

        // Cancelar captura activa si estaba capturando
        String zoneId = plugin.getZoneManager().getPlayerZoneId(victim.getUniqueId());
        if (zoneId != null && plugin.getCaptureManager().isPlayerCapturing(victim.getUniqueId())) {
            plugin.getCaptureManager().cancelCapture(zoneId, "capture.cancelled");
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // Heat check al revivir para limpiar efectos visuales
        plugin.getHeatManager().checkExpiry(event.getPlayer());
    }
}
