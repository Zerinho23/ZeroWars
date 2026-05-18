package com.zerowars.commands;

import com.zerowars.ZeroWars;
import com.zerowars.models.PlayerData;
import com.zerowars.utils.MessageUtil;
import com.zerowars.utils.RegionUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /top [players|clans|zones] — Muestra el ranking del servidor.
 */
public class TopCommand implements CommandExecutor, TabCompleter {

    private final ZeroWars plugin;

    public TopCommand(ZeroWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String category = args.length >= 1 ? args[0].toLowerCase() : "kills";

        List<PlayerData> ranking = plugin.getRankingManager().getRanking(category);

        String categoryName = switch (category) {
            case "captures"      -> "Capturas";
            case "time", "time-dominated" -> "Tiempo Dominado";
            default              -> "Kills";
        };

        MessageUtil.send(sender, plugin.getConfigManager()
                .getMessage("top.header", "%category%", categoryName));

        if (ranking.isEmpty()) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("top.no-data"));
            return true;
        }

        for (int i = 0; i < ranking.size(); i++) {
            PlayerData data = ranking.get(i);
            String value = switch (category) {
                case "captures"      -> String.valueOf(data.getCaptures());
                case "time", "time-dominated" -> RegionUtil.formatTime(data.getTotalDomineTimeMs());
                default              -> String.valueOf(data.getKills());
            };
            String entry = plugin.getConfigManager().getMessage("top.entry",
                    "%pos%", String.valueOf(i + 1),
                    "%player%", data.getName(),
                    "%value%", value);
            // Decorar los 3 primeros
            String prefix = switch (i) {
                case 0 -> "<gold>🥇 ";
                case 1 -> "<gray>🥈 ";
                case 2 -> "<dark_red>🥉 ";
                default -> "";
            };
            MessageUtil.send(sender, prefix + entry);
        }

        MessageUtil.send(sender, plugin.getConfigManager().getMessage("top.categories"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("kills", "captures", "time").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}
