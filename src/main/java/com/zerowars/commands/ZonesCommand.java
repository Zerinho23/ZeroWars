package com.zerowars.commands;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * /zones — Lista todas las zonas disponibles con su estado.
 */
public class ZonesCommand implements CommandExecutor {

    private final ZeroWars plugin;

    public ZonesCommand(ZeroWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        Collection<Zone> zones = plugin.getZoneManager().getEnabledZones();

        MessageUtil.send(sender,
                plugin.getConfigManager().getMessage("zone.list-header"));

        if (zones.isEmpty()) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("zone.list-empty"));
            return true;
        }

        for (Zone zone : zones) {
            String owner = zone.isOwned() ? zone.getOwnerName() : "Nadie";
            String statusColor = switch (zone.getState()) {
                case NEUTRAL   -> "<gray>";
                case OWNED     -> "<green>";
                case CAPTURING -> "<yellow>";
                case CONTESTED -> "<red>";
            };
            String line = plugin.getConfigManager().getMessage("zone.list-entry",
                    "%zone%", zone.getDisplayName(),
                    "%owner%", owner,
                    "%type%", zone.getType().name());
            MessageUtil.send(sender, statusColor + line);
        }

        MessageUtil.send(sender, "<dark_gray>Total: <gray>" + zones.size() + " zonas activas.");
        return true;
    }
}
