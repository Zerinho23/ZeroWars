package com.zerowars.commands;

import com.zerowars.ZeroWars;
import com.zerowars.models.ZoneEvent;
import com.zerowars.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * /events — Muestra los eventos activos actualmente.
 */
public class EventsCommand implements CommandExecutor {

    private final ZeroWars plugin;

    public EventsCommand(ZeroWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        Collection<ZoneEvent> active = plugin.getEventManager().getActiveEvents();

        MessageUtil.send(sender, "<gradient:#ffaa00:#ff0000>━━━━ Eventos Activos ━━━━</gradient>");

        if (active.isEmpty()) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("event.no-active"));
            return true;
        }

        for (ZoneEvent event : active) {
            String entry = plugin.getConfigManager().getMessage("event.info",
                    "%event%", MessageUtil.stripFormatting(event.getName()),
                    "%time%", String.valueOf(event.getRemainingMinutes()));
            MessageUtil.send(sender, "<yellow>▸ " + entry);
            MessageUtil.send(sender, "  <gray>" + event.getDescription());
            MessageUtil.send(sender, "  <green>Multiplicador: x" + event.getRewardMultiplier());
        }

        return true;
    }
}
