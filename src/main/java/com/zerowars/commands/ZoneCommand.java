package com.zerowars.commands;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.utils.MessageUtil;
import com.zerowars.utils.RegionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /zone — Muestra información de la zona actual del jugador.
 */
public class ZoneCommand implements CommandExecutor {

    private final ZeroWars plugin;

    public ZoneCommand(ZeroWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.player-only"));
            return true;
        }

        var zoneOpt = plugin.getZoneManager().getPlayerZone(player.getUniqueId());
        if (zoneOpt.isEmpty()) {
            MessageUtil.send(player, plugin.getConfigManager().getMessage("zone.not-in-zone"));
            return true;
        }

        Zone zone = zoneOpt.get();
        String owner = zone.isOwned() ? zone.getOwnerName() : "<gray>Nadie";
        String captureStr = String.format("%.1f", zone.getCaptureProgress());
        String status = switch (zone.getState()) {
            case NEUTRAL   -> "<gray>Neutral";
            case OWNED     -> "<green>Dominada";
            case CAPTURING -> "<yellow>En captura";
            case CONTESTED -> "<red>En disputa";
        };
        String domTime = RegionUtil.formatTime(zone.getTotalDomineTime());
        String center  = RegionUtil.formatLocation(
                (zone.getMinX() + zone.getMaxX()) / 2,
                (zone.getMinY() + zone.getMaxY()) / 2,
                (zone.getMinZ() + zone.getMaxZ()) / 2);

        // Calcular evento activo y multiplicador
        double multiplier = plugin.getEventManager().getRewardMultiplier(zone.getId());
        String eventStr = multiplier > 1.0
                ? "<green>x" + String.format("%.1f", multiplier) + " ACTIVO"
                : "<gray>Ninguno";

        MessageUtil.sendLines(player,
                "<gradient:#ff4444:#ff8800>━━━━━━ " + zone.getDisplayName() + " ━━━━━━</gradient>",
                "<gray>Tipo: <white>" + zone.getType().name(),
                "<gray>Dueño: <white>" + owner,
                "<gray>Nivel: <white>" + zone.getLevel() + "<gray>/" + zone.getMaxLevel(),
                "<gray>Captura: <white>" + captureStr + "%",
                "<gray>Estado: " + status,
                "<gray>Tiempo dominado: <white>" + domTime,
                "<gray>Centro: <white>" + center,
                "<gray>Multiplicador evento: " + eventStr
        );

        // Si hay captura activa, mostrar info extra
        var capture = plugin.getCaptureManager().getCapture(zone.getId());
        if (capture != null) {
            MessageUtil.send(player, "<yellow>⚔ Atacante: <white>" + capture.getAttackerName()
                    + " <yellow>| Progreso: <white>" + String.format("%.1f", capture.getProgress()) + "%");
        }

        return true;
    }
}
