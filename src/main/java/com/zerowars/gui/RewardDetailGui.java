package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Material;
  import org.bukkit.configuration.ConfigurationSection;
  import org.bukkit.entity.Player;
  import org.bukkit.event.inventory.ClickType;
  import org.bukkit.inventory.ItemStack;

  import java.util.List;

  /**
   * GUI de detalle/edicion de un reward especifico.
   * Permite editar montos de dinero via chat input.
   */
  public class RewardDetailGui extends BaseGui {

      private final String rewardId;

      public RewardDetailGui(ZeroWars plugin, Player player, String rewardId) {
          super(plugin, player, 54, "<gold>Reward: <white>" + rewardId);
          this.rewardId = rewardId;
      }

      @Override
      public void buildGui() {
          border(Material.ORANGE_STAINED_GLASS_PANE);

          ConfigurationSection sec = plugin.getConfigManager().rewards()
                  .getConfigurationSection("rewards." + rewardId);
          if (sec == null) {
              inventory.setItem(22, item(Material.BARRIER, "<red>Reward no encontrado",
                  "<gray>ID: " + rewardId));
              inventory.setItem(49, item(Material.ARROW, "<yellow>Volver"));
              return;
          }

          // ── On-capture ────────────────────────────────────────────────────────
          double capMoney = sec.getDouble("on-capture.money", 0);
          int    capItems = sec.getList("on-capture.items", List.of()).size();
          int    capCmds  = sec.getStringList("on-capture.commands").size();

          inventory.setItem(11, item(Material.GOLDEN_SWORD,
              "<green><bold>Al Capturar",
              "<gray>Dinero: <gold>$" + (int) capMoney,
              "<gray>Items:    <white>" + capItems,
              "<gray>Comandos: <white>" + capCmds,
              " ",
              "<yellow>▶ Clic izq para cambiar el dinero"));

          // ── Pasiva ────────────────────────────────────────────────────────────
          double pasMoney  = sec.getDouble("passive.money", 0);
          int    pasItems  = sec.getList("passive.items", List.of()).size();
          int    interval  = sec.getInt("passive.interval", 60);

          inventory.setItem(13, item(Material.CLOCK,
              "<aqua><bold>Recompensa Pasiva",
              "<gray>Intervalo: <white>" + interval + "s",
              "<gray>Dinero:    <gold>$" + (int) pasMoney,
              "<gray>Items:     <white>" + pasItems,
              " ",
              "<yellow>▶ Clic izq → cambiar dinero",
              "<yellow>▶ Clic der → cambiar intervalo"));

          // ── Per-level ─────────────────────────────────────────────────────────
          double mult = sec.getDouble("per-level.money-multiplier", 1.0);
          inventory.setItem(15, item(Material.EXPERIENCE_BOTTLE,
              "<light_purple><bold>Multiplicador por Nivel",
              "<gray>Multiplicador actual: <yellow>x" + mult,
              "<gray>El dinero de captura y pasiva",
              "<gray>se multiplica por nivel.",
              " ",
              "<yellow>▶ Clic para cambiar"));

          // ── Items de captura (lectura) ────────────────────────────────────────
          var capItemList = sec.getConfigurationSection("on-capture.items");
          inventory.setItem(28, item(Material.CHEST,
              "<white>Items al capturar",
              "<gray>Se configuran en rewards.yml",
              capItems > 0 ? "<gray>Cantidad: <white>" + capItems + " tipos" : "<gray>(ninguno)"));

          // ── Items pasivos (lectura) ────────────────────────────────────────────
          inventory.setItem(30, item(Material.HOPPER,
              "<white>Items pasivos",
              "<gray>Se configuran en rewards.yml",
              pasItems > 0 ? "<gray>Cantidad: <white>" + pasItems + " tipos" : "<gray>(ninguno)"));

          // ── Comandos al capturar (lectura) ────────────────────────────────────
          inventory.setItem(32, item(Material.COMMAND_BLOCK,
              "<white>Comandos al capturar",
              "<gray>Se configuran en rewards.yml",
              capCmds > 0 ? "<gray>Comandos: <white>" + capCmds : "<gray>(ninguno)"));

          // ── Navegacion ────────────────────────────────────────────────────────
          inventory.setItem(49, item(Material.ARROW,
              "<yellow>◀ Volver a lista de rewards"));
      }

      @Override
      public void handleClick(int slot, ItemStack item, ClickType click) {
          switch (slot) {
              case 11 -> {
                  // Cambiar dinero al capturar
                  player.closeInventory();
                  plugin.getChatInputManager().awaitInput(player,
                      "<yellow>Escribe el nuevo monto de dinero <gray>al capturar <yellow>(ej: <white>500<yellow>):",
                      (p, input) -> {
                          try {
                              double amount = Double.parseDouble(input);
                              setRewardValue("on-capture.money", amount);
                              MessageUtil.send(p, "<green>✔ Dinero al capturar actualizado: <white>$" + (int) amount);
                          } catch (NumberFormatException e) {
                              MessageUtil.send(p, "<red>Numero invalido. Usa solo digitos (ej: 500).");
                          }
                          plugin.getGuiManager().openRewardDetail(p, rewardId);
                      });
              }
              case 13 -> {
                  if (click.isRightClick()) {
                      // Cambiar intervalo pasivo
                      player.closeInventory();
                      plugin.getChatInputManager().awaitInput(player,
                          "<yellow>Escribe el nuevo intervalo pasivo <gray>en segundos <yellow>(ej: <white>60<yellow>):",
                          (p, input) -> {
                              try {
                                  int secs = Integer.parseInt(input);
                                  setRewardValue("passive.interval", secs);
                                  MessageUtil.send(p, "<green>✔ Intervalo actualizado: <white>" + secs + "s");
                              } catch (NumberFormatException e) {
                                  MessageUtil.send(p, "<red>Numero invalido. Usa solo digitos (ej: 60).");
                              }
                              plugin.getGuiManager().openRewardDetail(p, rewardId);
                          });
                  } else {
                      // Cambiar dinero pasivo
                      player.closeInventory();
                      plugin.getChatInputManager().awaitInput(player,
                          "<yellow>Escribe el nuevo monto de dinero <gray>pasivo <yellow>(ej: <white>100<yellow>):",
                          (p, input) -> {
                              try {
                                  double amount = Double.parseDouble(input);
                                  setRewardValue("passive.money", amount);
                                  MessageUtil.send(p, "<green>✔ Dinero pasivo actualizado: <white>$" + (int) amount);
                              } catch (NumberFormatException e) {
                                  MessageUtil.send(p, "<red>Numero invalido. Usa solo digitos (ej: 100).");
                              }
                              plugin.getGuiManager().openRewardDetail(p, rewardId);
                          });
                  }
              }
              case 15 -> {
                  // Cambiar multiplicador
                  player.closeInventory();
                  plugin.getChatInputManager().awaitInput(player,
                      "<yellow>Escribe el multiplicador por nivel <yellow>(ej: <white>1.5<yellow>):",
                      (p, input) -> {
                          try {
                              double mult = Double.parseDouble(input);
                              setRewardValue("per-level.money-multiplier", mult);
                              MessageUtil.send(p, "<green>✔ Multiplicador actualizado: <white>x" + mult);
                          } catch (NumberFormatException e) {
                              MessageUtil.send(p, "<red>Numero invalido. Usa punto decimal (ej: 1.5).");
                          }
                          plugin.getGuiManager().openRewardDetail(p, rewardId);
                      });
              }
              case 49 -> plugin.getGuiManager().openRewardList(player);
          }
      }

      private void setRewardValue(String subPath, Object value) {
          String path = "rewards." + rewardId + "." + subPath;
          plugin.getConfigManager().rewards().set(path, value);
          plugin.getConfigManager().save(com.zerowars.config.ConfigManager.REWARDS);
      }
  }