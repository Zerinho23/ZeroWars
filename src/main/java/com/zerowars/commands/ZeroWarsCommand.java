package com.zerowars.commands;

import com.zerowars.ZeroWars;
import com.zerowars.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Comando principal de administración: /zerowars (/zw)
 *
 * Subcomandos:
 *   reload          — Recarga toda la configuración
 *   createzone      — Crea una zona en la posición del jugador
 *   deletezone <id> — Elimina una zona
 *   giveconsumable <player> <id> [amount] — Da un consumible
 *   startevent <id> — Inicia un evento manualmente
 *   stopevent <id>  — Detiene un evento activo
 *   debug           — Toggle modo debug
 *   help            — Muestra ayuda
 */
public class ZeroWarsCommand implements CommandExecutor, TabCompleter {

    private final ZeroWars plugin;

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "reload", "createzone", "deletezone", "giveconsumable",
            "startevent", "stopevent", "debug", "help"
    );

    public ZeroWarsCommand(ZeroWars plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zerowars.admin")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload"         -> cmdReload(sender);
            case "createzone"     -> cmdCreateZone(sender, args);
            case "deletezone"     -> cmdDeleteZone(sender, args);
            case "giveconsumable" -> cmdGiveConsumable(sender, args);
            case "startevent"     -> cmdStartEvent(sender, args);
            case "stopevent"      -> cmdStopEvent(sender, args);
            case "debug"          -> cmdDebug(sender);
            case "help"           -> { sendHelp(sender); yield true; }
            default               -> {
                MessageUtil.send(sender, plugin.getConfigManager()
                        .getMessage("general.unknown-command"));
                yield true;
            }
        };
    }

    // ── Subcomandos ──────────────────────────────────────────────────────────

    private boolean cmdReload(CommandSender sender) {
        if (!sender.hasPermission("zerowars.admin.reload")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        plugin.getConfigManager().reloadAll();
        plugin.getZoneManager().loadZones();
        plugin.getConsumableManager().loadConsumables();
        plugin.getEventManager().loadEvents();
        MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.reload-success"));
        return true;
    }

    private boolean cmdCreateZone(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zerowars.admin.createzone")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.player-only"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<red>Uso: /zw createzone <id> [tipo]");
            return true;
        }
        String zoneId = args[1].toLowerCase();
        if (plugin.getZoneManager().zoneExists(zoneId)) {
            MessageUtil.send(sender, plugin.getConfigManager()
                    .getMessage("zone.already-exists", "%zone%", zoneId));
            return true;
        }
        // Crear zona básica en la posición del jugador con área 20x20
        var loc = player.getLocation();
        var zone = new com.zerowars.models.Zone(
                zoneId, zoneId,
                args.length >= 3 ? parseZoneType(args[2]) : com.zerowars.models.Zone.ZoneType.MINE,
                loc.getWorld().getName(),
                loc.getX() - 10, loc.getY() - 5, loc.getZ() - 10,
                loc.getX() + 10, loc.getY() + 10, loc.getZ() + 10,
                1, 5, 30
        );
        plugin.getZoneManager().registerZone(zone);
        plugin.getZoneManager().saveAsync(zone);

        MessageUtil.send(sender, plugin.getConfigManager()
                .getMessage("zone.created", "%zone%", zoneId));
        MessageUtil.send(sender, "<gray>Área: " + zone.getMinX() + " → " + zone.getMaxX()
                + " | Edita zones.yml para ajustar manualmente.");
        return true;
    }

    private com.zerowars.models.Zone.ZoneType parseZoneType(String str) {
        try { return com.zerowars.models.Zone.ZoneType.valueOf(str.toUpperCase()); }
        catch (IllegalArgumentException e) { return com.zerowars.models.Zone.ZoneType.MINE; }
    }

    private boolean cmdDeleteZone(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zerowars.admin.deletezone")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<red>Uso: /zw deletezone <id>");
            return true;
        }
        String id = args[1];
        if (plugin.getZoneManager().removeZone(id)) {
            MessageUtil.send(sender, plugin.getConfigManager()
                    .getMessage("zone.deleted", "%zone%", id));
        } else {
            MessageUtil.send(sender, plugin.getConfigManager()
                    .getMessage("zone.not-found", "%zone%", id));
        }
        return true;
    }

    private boolean cmdGiveConsumable(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zerowars.admin.giveconsumable")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        if (args.length < 3) {
            MessageUtil.send(sender, "<red>Uso: /zw giveconsumable <jugador> <id> [cantidad]");
            return true;
        }
        var target = plugin.getServer().getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(sender, plugin.getConfigManager()
                    .getMessage("general.player-not-found", "%player%", args[1]));
            return true;
        }
        String consumableId = args[2];
        int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;

        if (!plugin.getConsumableManager().consumableExists(consumableId)) {
            MessageUtil.send(sender, "<red>✗ Consumible <yellow>" + consumableId + "</yellow> no existe.");
            return true;
        }

        var item = plugin.getConsumableManager().buildItem(consumableId, amount);
        target.getInventory().addItem(item);

        MessageUtil.send(sender, plugin.getConfigManager().getMessage("consumable.given",
                "%consumable%", consumableId, "%amount%", String.valueOf(amount),
                "%player%", target.getName()));
        return true;
    }

    private boolean cmdStartEvent(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zerowars.admin.startevent")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<red>Uso: /zw startevent <id>");
            return true;
        }
        String id = args[1];
        if (!plugin.getEventManager().eventExists(id)) {
            MessageUtil.send(sender, plugin.getConfigManager()
                    .getMessage("event.not-found", "%event%", id));
            return true;
        }
        if (plugin.getEventManager().isEventActive(id)) {
            MessageUtil.send(sender, plugin.getConfigManager()
                    .getMessage("event.already-running", "%event%", id));
            return true;
        }
        plugin.getEventManager().startEvent(id);
        MessageUtil.send(sender, plugin.getConfigManager()
                .getMessage("event.started-manual", "%event%", id));
        return true;
    }

    private boolean cmdStopEvent(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zerowars.admin.stopevent")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, "<red>Uso: /zw stopevent <id>");
            return true;
        }
        boolean stopped = plugin.getEventManager().endEvent(args[1]);
        if (stopped) {
            MessageUtil.send(sender, plugin.getConfigManager()
                    .getMessage("event.stopped", "%event%", args[1]));
        } else {
            MessageUtil.send(sender, "<red>✗ Evento no activo o no existe.");
        }
        return true;
    }

    private boolean cmdDebug(CommandSender sender) {
        if (!sender.hasPermission("zerowars.admin.debug")) {
            MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
            return true;
        }
        boolean current = plugin.getConfigManager().isDebug();
        // Toggle (en producción esto se haría editando config)
        MessageUtil.send(sender, "<yellow>Debug: <white>" + !current
                + " <gray>(edita config.yml para persistir)");
        MessageUtil.send(sender, "<gray>Zonas cargadas: " + plugin.getZoneManager().getZoneCount());
        MessageUtil.send(sender, "<gray>Capturas activas: " + plugin.getCaptureManager().getActiveCaptureCount());
        MessageUtil.send(sender, "<gray>Eventos activos: " + plugin.getEventManager().getActiveCount());
        MessageUtil.send(sender, "<gray>Cooldowns activos: " + plugin.getCooldownManager().getActiveCount());
        return true;
    }

    // ── Help ─────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendLines(sender,
                "<gradient:#ff4444:#ff8800>━━━━ ZeroWars Admin ━━━━</gradient>",
                "<gray>/zw reload <white>— Recargar configuración",
                "<gray>/zw createzone <id> [tipo] <white>— Crear zona",
                "<gray>/zw deletezone <id> <white>— Eliminar zona",
                "<gray>/zw giveconsumable <player> <id> [cantidad]",
                "<gray>/zw startevent <id> <white>— Iniciar evento",
                "<gray>/zw stopevent <id> <white>— Detener evento",
                "<gray>/zw debug <white>— Info de debug",
                "<dark_gray>Tipos de zona: MINE, ROOM, RARE, EVENT, SPECIAL"
        );
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("zerowars.admin")) return List.of();

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "deletezone"     -> plugin.getZoneManager().getAllZones().stream()
                        .map(z -> z.getId()).filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
                case "giveconsumable" -> plugin.getServer().getOnlinePlayers().stream()
                        .map(Player::getName).filter(n -> n.startsWith(args[1]))
                        .collect(Collectors.toList());
                case "startevent"     -> plugin.getEventManager().getAllEvents().stream()
                        .map(e -> e.getId()).filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
                case "stopevent"      -> plugin.getEventManager().getActiveEvents().stream()
                        .map(e -> e.getId()).filter(id -> id.startsWith(args[1]))
                        .collect(Collectors.toList());
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("giveconsumable")) {
            return plugin.getConsumableManager().getAllConsumables().stream()
                    .map(c -> c.getId()).filter(id -> id.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { return def; }
    }
}
