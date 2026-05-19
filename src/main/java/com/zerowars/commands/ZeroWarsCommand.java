package com.zerowars.commands;

  import com.zerowars.ZeroWars;
  import com.zerowars.models.Zone;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.command.Command;
  import org.bukkit.command.CommandExecutor;
  import org.bukkit.command.CommandSender;
  import org.bukkit.command.TabCompleter;
  import org.bukkit.entity.Player;
  import org.jetbrains.annotations.NotNull;

  import java.util.ArrayList;
  import java.util.Arrays;
  import java.util.List;
  import java.util.Optional;
  import java.util.stream.Collectors;

  /**
   * Comando principal de administracion: /zerowars (/zw)
   *
   * Subcomandos:
   *   menu                               — Abre el menu GUI interactivo (v1.3.0)
   *   reload                             — Recarga toda la configuracion
   *   createzone <id> [tipo]             — Crea zona y la guarda en zones.yml (v1.3.0)
   *   deletezone <id>                    — Elimina una zona (memoria + zones.yml)
   *   setreward <zona> <reward> [remove] — Asigna/quita reward a una zona
   *   giveconsumable <player> <id> [n]   — Da un consumible
   *   startevent <id>                    — Inicia un evento
   *   stopevent <id>                     — Detiene un evento
   *   debug                              — Toggle modo debug
   *   help                               — Muestra ayuda
   */
  public class ZeroWarsCommand implements CommandExecutor, TabCompleter {

      private final ZeroWars plugin;

      private static final List<String> SUBCOMMANDS = Arrays.asList(
              "menu", "reload", "createzone", "deletezone", "setreward",
              "giveconsumable", "startevent", "stopevent", "debug", "help"
      );

      public ZeroWarsCommand(ZeroWars plugin) { this.plugin = plugin; }

      @Override
      public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd,
                               @NotNull String label, @NotNull String[] args) {
          if (!sender.hasPermission("zerowars.admin")) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission"));
              return true;
          }
          if (args.length == 0) { sendHelp(sender); return true; }
          return switch (args[0].toLowerCase()) {
              case "menu"           -> cmdMenu(sender);
              case "reload"         -> cmdReload(sender);
              case "createzone"     -> cmdCreateZone(sender, args);
              case "deletezone"     -> cmdDeleteZone(sender, args);
              case "setreward"      -> cmdSetReward(sender, args);
              case "giveconsumable" -> cmdGiveConsumable(sender, args);
              case "startevent"     -> cmdStartEvent(sender, args);
              case "stopevent"      -> cmdStopEvent(sender, args);
              case "debug"          -> cmdDebug(sender);
              case "help"           -> { sendHelp(sender); yield true; }
              default               -> {
                  MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.unknown-command"));
                  yield true;
              }
          };
      }

      // ── Subcomandos ──────────────────────────────────────────────────────────

      private boolean cmdMenu(CommandSender sender) {
          if (!(sender instanceof Player player)) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.player-only"));
              return true;
          }
          plugin.getGuiManager().openMainMenu(player);
          return true;
      }

      private boolean cmdReload(CommandSender sender) {
          if (!sender.hasPermission("zerowars.admin.reload")) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission")); return true;
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
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission")); return true;
          }
          if (!(sender instanceof Player player)) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.player-only")); return true;
          }
          if (args.length < 2) {
              MessageUtil.send(sender, "<red>Uso: /zw createzone <id> [tipo]"); return true;
          }
          String zoneId = args[1].toLowerCase();
          if (plugin.getZoneManager().zoneExists(zoneId)) {
              MessageUtil.send(sender,
                  plugin.getConfigManager().getMessage("zone.already-exists", "%zone%", zoneId));
              return true;
          }
          var loc  = player.getLocation();
          var type = args.length >= 3 ? parseZoneType(args[2]) : Zone.ZoneType.MINE;
          var zone = new Zone(zoneId, zoneId, type, loc.getWorld().getName(),
                  loc.getX() - 10, loc.getY() - 5, loc.getZ() - 10,
                  loc.getX() + 10, loc.getY() + 10, loc.getZ() + 10,
                  1, 5, 30);
          plugin.getZoneManager().registerZone(zone);
          // v1.3.0: guardar en zones.yml (persistencia real) + SQLite (estado runtime)
          plugin.getZoneManager().saveZoneToYml(zone);
          plugin.getZoneManager().saveAsync(zone);
          MessageUtil.send(sender,
              plugin.getConfigManager().getMessage("zone.created", "%zone%", zoneId));
          MessageUtil.send(sender, "<gray>Area 20x15x20 creada en tu posicion y guardada en <white>zones.yml<gray>.");
          MessageUtil.send(sender, "<gray>Edita zones.yml para ajustar los corners exactos, luego <white>/zw reload<gray>.");
          return true;
      }

      private boolean cmdDeleteZone(CommandSender sender, String[] args) {
          if (!sender.hasPermission("zerowars.admin.deletezone")) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission")); return true;
          }
          if (args.length < 2) {
              MessageUtil.send(sender, "<red>Uso: /zw deletezone <id>"); return true;
          }
          String id = args[1];
          if (plugin.getZoneManager().removeZone(id)) {
              MessageUtil.send(sender,
                  plugin.getConfigManager().getMessage("zone.deleted", "%zone%", id));
          } else {
              MessageUtil.send(sender,
                  plugin.getConfigManager().getMessage("zone.not-found", "%zone%", id));
          }
          return true;
      }

      private boolean cmdSetReward(CommandSender sender, String[] args) {
          if (!sender.hasPermission("zerowars.admin.setreward")) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission")); return true;
          }
          if (args.length < 3) {
              MessageUtil.send(sender, "<red>Uso: /zw setreward <zona> <rewardId> [remove]"); return true;
          }
          String zoneId   = args[1];
          String rewardId = args[2];
          boolean remove  = args.length >= 4 && args[3].equalsIgnoreCase("remove");
          Zone zone = plugin.getZoneManager().getZone(zoneId).orElse(null);
          if (zone == null) {
              MessageUtil.send(sender,
                  plugin.getConfigManager().getMessage("zone.not-found", "%zone%", zoneId)); return true;
          }
          List<String> rewards = new ArrayList<>(zone.getRewardIds());
          if (remove) {
              if (rewards.remove(rewardId)) {
                  zone.setRewardIds(rewards);
                  MessageUtil.send(sender, "<green>Reward <white>" + rewardId
                      + "<green> eliminado de " + zone.getDisplayName() + "<green>.");
              } else {
                  MessageUtil.send(sender, "<yellow>La zona no tenia ese reward.");
              }
          } else {
              if (!rewards.contains(rewardId)) {
                  rewards.add(rewardId);
                  zone.setRewardIds(rewards);
                  MessageUtil.send(sender, "<green>Reward <white>" + rewardId
                      + "<green> asignado a " + zone.getDisplayName() + "<green>.");
              } else {
                  MessageUtil.send(sender, "<yellow>La zona ya tiene ese reward asignado.");
              }
          }
          return true;
      }

      private boolean cmdGiveConsumable(CommandSender sender, String[] args) {
          if (!sender.hasPermission("zerowars.admin.giveconsumable")) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission")); return true;
          }
          if (args.length < 3) {
              MessageUtil.send(sender, "<red>Uso: /zw giveconsumable <jugador> <id> [cantidad]"); return true;
          }
          var target = plugin.getServer().getPlayer(args[1]);
          if (target == null) {
              MessageUtil.send(sender,
                  plugin.getConfigManager().getMessage("general.player-not-found", "%player%", args[1])); return true;
          }
          String consumableId = args[2];
          int amount = args.length >= 4 ? Math.max(1, Math.min(64, parseInt(args[3], 1))) : 1;
          // Verificar que el consumible existe
          Optional<?> consumable = plugin.getConsumableManager().getConsumable(consumableId);
          if (consumable.isEmpty()) {
              MessageUtil.send(sender, "<red>Consumible <white>" + consumableId
                  + "<red> no existe en consumables.yml."); return true;
          }
          // buildItem(String consumableId, int amount) — firma correcta
          for (int i = 0; i < amount; i++)
              target.getInventory().addItem(plugin.getConsumableManager().buildItem(consumableId, 1));
          MessageUtil.send(sender, "<green>Entregado x" + amount + " <white>"
              + consumableId + "<green> a " + target.getName() + ".");
          return true;
      }

      private boolean cmdStartEvent(CommandSender sender, String[] args) {
          if (!sender.hasPermission("zerowars.admin.startevent")) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission")); return true;
          }
          if (args.length < 2) {
              MessageUtil.send(sender, "<red>Uso: /zw startevent <id>"); return true;
          }
          String id = args[1];
          if (plugin.getEventManager().startEvent(id)) {
              MessageUtil.send(sender, "<green>Evento <white>" + id + "<green> iniciado.");
          } else {
              MessageUtil.send(sender, "<red>El evento <white>" + id
                  + "<red> no existe o ya esta activo.");
          }
          return true;
      }

      private boolean cmdStopEvent(CommandSender sender, String[] args) {
          if (!sender.hasPermission("zerowars.admin.stopevent")) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission")); return true;
          }
          if (args.length < 2) {
              MessageUtil.send(sender, "<red>Uso: /zw stopevent <id>"); return true;
          }
          String id = args[1];
          if (plugin.getEventManager().endEvent(id)) {
              MessageUtil.send(sender, "<green>Evento <white>" + id + "<green> detenido.");
          } else {
              MessageUtil.send(sender, "<red>El evento <white>" + id + "<red> no esta activo.");
          }
          return true;
      }

      private boolean cmdDebug(CommandSender sender) {
          if (!sender.hasPermission("zerowars.admin.debug")) {
              MessageUtil.send(sender, plugin.getConfigManager().getMessage("general.no-permission")); return true;
          }
          boolean cur = plugin.getConfigManager().isDebug();
          plugin.getConfigManager().config().set("general.debug", !cur);
          MessageUtil.send(sender, "<yellow>Modo debug: " + (!cur ? "<green>ACTIVADO" : "<red>DESACTIVADO"));
          if (!cur) {
              MessageUtil.send(sender, "<gray>Zonas: " + plugin.getZoneManager().getZoneCount()
                  + " | Capturas: " + plugin.getCaptureManager().getActiveCaptureCount()
                  + " | Eventos: "  + plugin.getEventManager().getActiveCount()
                  + " | Vault: "    + (plugin.getRewardManager().isVaultEnabled() ? "SI" : "NO"));
          }
          return true;
      }

      private void sendHelp(CommandSender sender) {
          MessageUtil.send(sender, "<gradient:#ff4444:#ff8800>─── ZeroWars Admin v"
              + plugin.getDescription().getVersion() + " ───");
          MessageUtil.send(sender, "<yellow>/zw menu <gray>- Abre menu GUI interactivo");
          MessageUtil.send(sender, "<yellow>/zw reload <gray>- Recarga configuracion");
          MessageUtil.send(sender, "<yellow>/zw createzone <id> [tipo] <gray>- Crea zona (se guarda en zones.yml)");
          MessageUtil.send(sender, "<yellow>/zw deletezone <id> <gray>- Elimina zona");
          MessageUtil.send(sender, "<yellow>/zw setreward <zona> <reward> [remove] <gray>- Asigna/quita reward");
          MessageUtil.send(sender, "<yellow>/zw giveconsumable <player> <id> [cant] <gray>- Da consumible");
          MessageUtil.send(sender, "<yellow>/zw startevent <id> <gray>- Inicia evento");
          MessageUtil.send(sender, "<yellow>/zw stopevent <id> <gray>- Detiene evento");
          MessageUtil.send(sender, "<yellow>/zw debug <gray>- Toggle logs debug");
      }

      @Override
      public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd,
                                        @NotNull String label, @NotNull String[] args) {
          if (!sender.hasPermission("zerowars.admin")) return List.of();
          if (args.length == 1) return filter(SUBCOMMANDS, args[0]);
          if (args.length == 2) return switch (args[0].toLowerCase()) {
              case "deletezone", "setreward" ->
                  filter(plugin.getZoneManager().getAllZones().stream()
                      .map(Zone::getId).collect(Collectors.toList()), args[1]);
              case "giveconsumable" ->
                  filter(plugin.getServer().getOnlinePlayers().stream()
                      .map(Player::getName).collect(Collectors.toList()), args[1]);
              case "startevent", "stopevent" ->
                  filter(plugin.getEventManager().getAllEvents().stream()
                      .map(e -> e.getId()).collect(Collectors.toList()), args[1]);
              case "createzone" -> List.of("MINE", "ROOM", "RARE", "EVENT", "SPECIAL");
              default -> List.of();
          };
          if (args.length == 3 && args[0].equalsIgnoreCase("setreward")) {
              var sec = plugin.getConfigManager().rewards().getConfigurationSection("rewards");
              return sec != null ? filter(new ArrayList<>(sec.getKeys(false)), args[2]) : List.of();
          }
          if (args.length == 4 && args[0].equalsIgnoreCase("setreward"))
              return filter(List.of("remove"), args[3]);
          return List.of();
      }

      private List<String> filter(List<String> list, String prefix) {
          return list.stream()
              .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
              .collect(Collectors.toList());
      }
      private Zone.ZoneType parseZoneType(String s) {
          try { return Zone.ZoneType.valueOf(s.toUpperCase()); }
          catch (IllegalArgumentException e) { return Zone.ZoneType.MINE; }
      }
      private int parseInt(String s, int def) {
          try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
      }
  }