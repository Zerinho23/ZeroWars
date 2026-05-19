package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Material;
  import org.bukkit.configuration.ConfigurationSection;
  import org.bukkit.entity.Player;
  import org.bukkit.event.inventory.ClickType;
  import org.bukkit.inventory.ItemStack;

  import java.util.ArrayList;
  import java.util.List;

  /**
   * GUI que lista todos los rewards configurados en rewards.yml.
   * Click en un reward → RewardDetailGui para ver y editar.
   */
  public class RewardListGui extends BaseGui {

      private final int page;
      private List<String> rewardIds;

      private static final int[] SLOTS = {
          10,11,12,13,14,15,16,
          19,20,21,22,23,24,25,
          28,29,30,31,32,33,34,
          37,38,39,40,41,42,43
      };

      public RewardListGui(ZeroWars plugin, Player player, int page) {
          super(plugin, player, 54, "<gold>Recompensas <dark_gray>— Pagina " + (page + 1));
          this.page = page;
      }

      @Override
      public void buildGui() {
          border(Material.ORANGE_STAINED_GLASS_PANE);

          ConfigurationSection sec = plugin.getConfigManager().rewards()
                  .getConfigurationSection("rewards");
          rewardIds = sec != null ? new ArrayList<>(sec.getKeys(false)) : new ArrayList<>();

          int start = page * SLOTS.length;
          int end   = Math.min(start + SLOTS.length, rewardIds.size());

          for (int i = start; i < end; i++) {
              String id  = rewardIds.get(i);
              ConfigurationSection rSec = sec.getConfigurationSection(id);
              double capMoney = rSec != null ? rSec.getDouble("on-capture.money", 0) : 0;
              double pasMoney = rSec != null ? rSec.getDouble("passive.money", 0)    : 0;
              int    interval = rSec != null ? rSec.getInt("passive.interval", 60)   : 60;
              int    capItems = rSec != null ? rSec.getList("on-capture.items", List.of()).size() : 0;
              double lvlMult  = rSec != null ? rSec.getDouble("per-level.money-multiplier", 1.0) : 1.0;

              inventory.setItem(SLOTS[i - start], item(Material.CHEST,
                  "<gold><bold>" + id,
                  "<gray>Al capturar:  <white>$" + (int) capMoney + "  <gray>+" + capItems + " items",
                  "<gray>Pasiva:       <white>$" + (int) pasMoney + " <gray>cada <white>" + interval + "s",
                  "<gray>Multiplicador nivel: <yellow>x" + lvlMult,
                  " ",
                  "<yellow>▶ Clic para ver y editar"));
          }

          if (rewardIds.isEmpty())
              inventory.setItem(22, item(Material.BARRIER,
                  "<red>No hay rewards configurados",
                  "<gray>Crea rewards en rewards.yml",
                  "<gray>y ejecuta /zw reload."));

          if (page > 0)
              inventory.setItem(45, item(Material.ARROW, "<yellow>◀ Anterior", "<gray>Pagina " + page));
          inventory.setItem(49, item(Material.NETHER_STAR, "<white>Menu principal"));
          if (end < rewardIds.size())
              inventory.setItem(53, item(Material.ARROW, "<yellow>Siguiente ▶", "<gray>Pagina " + (page + 2)));
      }

      @Override
      public void handleClick(int slot, ItemStack item, ClickType click) {
          int start = page * SLOTS.length;
          for (int i = 0; i < SLOTS.length; i++) {
              if (SLOTS[i] == slot && (start + i) < rewardIds.size()) {
                  plugin.getGuiManager().openRewardDetail(player, rewardIds.get(start + i));
                  return;
              }
          }
          if (slot == 45 && page > 0) new RewardListGui(plugin, player, page - 1).open();
          if (slot == 49) plugin.getGuiManager().openMainMenu(player);
          if (slot == 53 && (page * SLOTS.length + SLOTS.length) < rewardIds.size())
              new RewardListGui(plugin, player, page + 1).open();
      }
  }