package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Material;
  import org.bukkit.entity.Player;
  import org.bukkit.event.inventory.ClickType;
  import org.bukkit.inventory.ItemStack;

  /**
   * Menu principal de administracion — /zw menu
   *
   * Layout (54 slots):
   *  Fila 1 interior: Zonas | Eventos | Reload | Estado
   *  Fila 2 interior: Crear Zona (wand) | Recompensas | Vault | Debug
   *  Fila 5: Cerrar
   */
  public class MainMenuGui extends BaseGui {

      public MainMenuGui(ZeroWars plugin, Player player) {
          super(plugin, player, 54,
              "<gradient:#ff4444:#ff8800><bold>⚔ ZeroWars Admin</bold></gradient> <dark_gray>v"
              + plugin.getDescription().getVersion());
      }

      @Override
      public void buildGui() {
          border(Material.GRAY_STAINED_GLASS_PANE);

          int zones    = plugin.getZoneManager().getZoneCount();
          int captures = plugin.getCaptureManager().getActiveCaptureCount();
          int events   = plugin.getEventManager().getActiveCount();
          boolean vault = plugin.getRewardManager().isVaultEnabled();

          // ── Fila 1 ──────────────────────────────────────────────────────────

          // Zonas (lista)
          inventory.setItem(10, item(Material.EMERALD_BLOCK,
              "<green><bold>Gestion de Zonas",
              "<gray>Zonas cargadas: <white>" + zones,
              "<gray>Capturas activas: <white>" + captures,
              " ",
              "<yellow>▶ Clic para ver lista de zonas"));

          // Eventos
          inventory.setItem(12, item(Material.BEACON,
              "<aqua><bold>Gestion de Eventos",
              "<gray>Eventos activos: <white>" + events,
              " ",
              "<yellow>▶ Clic para gestionar eventos"));

          // Reload
          inventory.setItem(14, item(Material.CLOCK,
              "<yellow><bold>Recargar Configuracion",
              "<gray>Recarga zones.yml, rewards.yml,",
              "<gray>events.yml y consumables.yml",
              " ",
              "<yellow>▶ Clic para recargar"));

          // Estado plugin
          inventory.setItem(16, item(Material.BOOK,
              "<white><bold>Estado del Plugin",
              "<gray>Version: <white>"    + plugin.getDescription().getVersion(),
              "<gray>Zonas: <white>"      + zones + "  <gray>Capturas: <white>" + captures,
              "<gray>Vault: "             + (vault ? "<green>Conectado" : "<red>No disponible"),
              "<gray>Eventos activos: <white>" + events));

          // ── Fila 2 ──────────────────────────────────────────────────────────

          // Crear Zona con wand (NUEVO v1.4.0)
          inventory.setItem(19, item(Material.GOLDEN_AXE,
              "<gold><bold>⚒ Crear Nueva Zona",
              "<gray>Recibiras un hacha especial.",
              "<gray>Marca 2 puntos en el mapa y",
              "<gray>se abrira el menu de creacion.",
              " ",
              "<yellow>▶ Clic para obtener el wand"));

          // Recompensas (NUEVO v1.4.0)
          inventory.setItem(21, item(Material.GOLD_INGOT,
              "<gold><bold>Recompensas",
              "<gray>Ver y editar los rewards",
              "<gray>configurados en rewards.yml.",
              "<gray>Dinero, intervalo y multiplicador",
              "<gray>editables directamente desde aqui.",
              " ",
              "<yellow>▶ Clic para abrir"));

          // Vault status
          inventory.setItem(23, item(vault ? Material.EMERALD : Material.IRON_INGOT,
              vault ? "<gold><bold>Vault: Conectado" : "<gray><bold>Vault: No disponible",
              vault ? "<gray>Economia activa — dinero en rewards" : "<gray>Instala Vault + plugin de economia",
              vault ? "" : "<gray>para activar rewards de dinero."));

          // Debug toggle
          boolean debug = plugin.getConfigManager().isDebug();
          inventory.setItem(25, item(debug ? Material.REDSTONE_TORCH : Material.LEVER,
              debug ? "<green><bold>Debug: ACTIVADO" : "<gray><bold>Debug: DESACTIVADO",
              "<gray>Logs detallados de capturas,",
              "<gray>rewards y sistema de zonas.",
              " ",
              "<yellow>▶ Clic para cambiar"));

          // Cerrar
          inventory.setItem(49, item(Material.BARRIER,
              "<red><bold>Cerrar",
              "<gray>Cierra este menu."));
      }

      @Override
      public void handleClick(int slot, ItemStack item, ClickType click) {
          switch (slot) {
              case 10 -> plugin.getGuiManager().openZoneList(player);
              case 12 -> plugin.getGuiManager().openEventList(player);
              case 14 -> {
                  player.closeInventory();
                  plugin.getConfigManager().reloadAll();
                  plugin.getZoneManager().loadZones();
                  plugin.getConsumableManager().loadConsumables();
                  plugin.getEventManager().loadEvents();
                  MessageUtil.send(player, "<green>✔ Configuracion recargada correctamente.");
              }
              case 19 -> {
                  // Dar wand al jugador
                  player.closeInventory();
                  player.getInventory().addItem(plugin.getZoneWandManager().createWand());
                  MessageUtil.send(player,
                      "<gold>⚒ Wand entregado. <gray>Clic izq → Pos1 | Clic der → Pos2 en el mapa.");
              }
              case 21 -> plugin.getGuiManager().openRewardList(player);
              case 25 -> {
                  boolean cur = plugin.getConfigManager().isDebug();
                  plugin.getConfigManager().config().set("general.debug", !cur);
                  new MainMenuGui(plugin, player).open();
              }
              case 49 -> player.closeInventory();
          }
      }
  }