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
   *  borde gris + botones centrados en filas 1-4
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

          // Zonas
          inventory.setItem(10, item(Material.EMERALD_BLOCK,
              "<green><bold>Gestion de Zonas",
              "<gray>Zonas cargadas: <white>" + zones,
              "<gray>Capturas activas: <white>" + captures,
              " ",
              "<yellow>▶ Clic para abrir lista de zonas"));

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
              "<gray>Version: <white>" + plugin.getDescription().getVersion(),
              "<gray>Zonas: <white>" + zones + "  <gray>Capturas: <white>" + captures,
              "<gray>Vault: " + (vault ? "<green>Conectado" : "<red>No disponible"),
              "<gray>Eventos activos: <white>" + events));

          // Vault status
          inventory.setItem(28, item(vault ? Material.GOLD_INGOT : Material.IRON_INGOT,
              vault ? "<gold><bold>Vault: Conectado" : "<gray><bold>Vault: No disponible",
              vault ? "<gray>Economia activa — dinero en rewards" : "<gray>Sin Vault: los rewards de dinero",
              vault ? "" : "<gray>estan desactivados. Instala Vault."));

          // Debug toggle
          boolean debug = plugin.getConfigManager().isDebug();
          inventory.setItem(30, item(debug ? Material.REDSTONE_TORCH : Material.LEVER,
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
              case 30 -> {
                  boolean cur = plugin.getConfigManager().isDebug();
                  plugin.getConfigManager().config().set("general.debug", !cur);
                  new MainMenuGui(plugin, player).open();
              }
              case 49 -> player.closeInventory();
          }
      }
  }