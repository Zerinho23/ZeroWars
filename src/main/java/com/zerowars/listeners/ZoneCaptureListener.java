package com.zerowars.listeners;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Optional;

/**
 * Listener específico de lógica de captura de zonas:
 * - PvP en zonas (habilitar/deshabilitar según estado)
 * - Protección de bloques en zonas protegidas
 * - Interacción entre jugadores durante captura
 */
public class ZoneCaptureListener implements Listener {

    private final ZeroWars plugin;

    public ZoneCaptureListener(ZeroWars plugin) {
        this.plugin = plugin;
    }

    // ── PvP en zonas ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // Verificar si alguno de los dos está en una zona
        Optional<Zone> attackerZone = plugin.getZoneManager().getPlayerZone(attacker.getUniqueId());
        Optional<Zone> victimZone   = plugin.getZoneManager().getPlayerZone(victim.getUniqueId());

        // Compañeros de clan no pueden atacarse en zonas
        if (attackerZone.isPresent() || victimZone.isPresent()) {
            if (plugin.getClanManager().areSameClan(attacker.getUniqueId(), victim.getUniqueId())) {
                event.setCancelled(true);
                attacker.sendActionBar(MessageUtil.parse(
                        "<red>✗ No puedes atacar a un compañero de clan."));
                return;
            }
        }

        // Si ambos están en la misma zona, el combate está permitido (zona PvP)
        // Si ninguno está en una zona, el comportamiento es el por defecto del servidor
    }

    // ── Protección de bloques ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zerowars.bypass")) return;

        Optional<Zone> zoneOpt = plugin.getZoneManager().getZoneAt(event.getBlock().getLocation());
        if (zoneOpt.isEmpty()) return;

        Zone zone = zoneOpt.get();
        if (!zone.isProtected()) return;

        // Solo el dueño (o su clan) puede romper bloques en su zona
        if (zone.isOwnedBy(player.getUniqueId())) return;
        if (zone.getOwnerClanId() != null) {
            var data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
            if (data != null && zone.getOwnerClanId().equals(data.getClanId())) return;
        }

        event.setCancelled(true);
        player.sendActionBar(MessageUtil.parse(
                "<red>✗ Esta zona está protegida. Captúrala primero."));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zerowars.bypass")) return;

        Optional<Zone> zoneOpt = plugin.getZoneManager().getZoneAt(event.getBlock().getLocation());
        if (zoneOpt.isEmpty()) return;

        Zone zone = zoneOpt.get();
        if (!zone.isProtected()) return;

        if (zone.isOwnedBy(player.getUniqueId())) return;
        if (zone.getOwnerClanId() != null) {
            var data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
            if (data != null && zone.getOwnerClanId().equals(data.getClanId())) return;
        }

        event.setCancelled(true);
        player.sendActionBar(MessageUtil.parse(
                "<red>✗ Esta zona está protegida. Captúrala primero."));
    }
}
