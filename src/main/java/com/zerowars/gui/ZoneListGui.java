package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import com.zerowars.models.Zone;
  import org.bukkit.Material;
  import org.bukkit.entity.Player;
  import org.bukkit.event.inventory.ClickType;
  import org.bukkit.inventory.ItemStack;

  import java.util.ArrayList;
  import java.util.List;

  /** GUI de lista de zonas con paginacion. */
  public class ZoneListGui extends BaseGui {

      private final int page;
      private List<Zone> zoneList;
      private static final int PAGE_SIZE = 28;

      // Slots interiores (excluyendo borde) filas 1-4
      private static final int[] SLOTS = {
          10,11,12,13,14,15,16,
          19,20,21,22,23,24,25,
          28,29,30,31,32,33,34,
          37,38,39,40,41,42,43
      };

      public ZoneListGui(ZeroWars plugin, Player player, int page) {
          super(plugin, player, 54,
              "<gray>Zonas PvP <dark_gray>— <white>Pagina " + (page + 1));
          this.page = page;
      }

      @Override
      public void buildGui() {
          border(Material.GRAY_STAINED_GLASS_PANE);
          zoneList = new ArrayList<>(plugin.getZoneManager().getAllZones());

          int start = page * PAGE_SIZE;
          int end   = Math.min(start + PAGE_SIZE, zoneList.size());

          for (int i = start; i < end; i++) {
              Zone z    = zoneList.get(i);
              Material wool = woolFor(z);
              String owner  = z.getOwnerName() != null ? "<gray>Dueno: <white>" + z.getOwnerName() : "<gray>Sin dueno";
              inventory.setItem(SLOTS[i - start], item(wool,
                  z.getDisplayName() + " <dark_gray>[" + z.getType() + "]",
                  stateLabel(z),
                  owner,
                  "<gray>Nivel: <gold>" + z.getLevel() + "<gray>/" + z.getMaxLevel(),
                  "<gray>Activa: " + (z.isEnabled() ? "<green>SI" : "<red>NO"),
                  " ",
                  "<yellow>▶ Clic para ver detalle"));
          }

          if (page > 0)
              inventory.setItem(45, item(Material.ARROW,
                  "<yellow>◀ Pagina anterior", "<gray>Pagina " + page));

          inventory.setItem(49, item(Material.NETHER_STAR,
              "<white>Menu principal", "<gray>Volver al menu admin."));

          if (end < zoneList.size())
              inventory.setItem(53, item(Material.ARROW,
                  "<yellow>Pagina siguiente ▶", "<gray>Pagina " + (page + 2)));
      }

      @Override
      public void handleClick(int slot, ItemStack item, ClickType click) {
          int start = page * PAGE_SIZE;
          for (int i = 0; i < SLOTS.length; i++) {
              if (SLOTS[i] == slot && (start + i) < zoneList.size()) {
                  plugin.getGuiManager().openZoneDetail(player, zoneList.get(start + i).getId());
                  return;
              }
          }
          if (slot == 45 && page > 0)
              new ZoneListGui(plugin, player, page - 1).open();
          if (slot == 49)
              plugin.getGuiManager().openMainMenu(player);
          if (slot == 53 && start + PAGE_SIZE < zoneList.size())
              new ZoneListGui(plugin, player, page + 1).open();
      }

      private Material woolFor(Zone z) {
          if (!z.isEnabled()) return Material.GRAY_WOOL;
          return switch (z.getState()) {
              case OWNED     -> Material.LIME_WOOL;
              case CAPTURING -> Material.YELLOW_WOOL;
              case CONTESTED -> Material.ORANGE_WOOL;
              case NEUTRAL   -> Material.WHITE_WOOL;
          };
      }

      private String stateLabel(Zone z) {
          if (!z.isEnabled()) return "<gray>Estado: DESACTIVADA";
          return "<gray>Estado: " + switch (z.getState()) {
              case OWNED     -> "<green>DOMINADA";
              case CAPTURING -> "<yellow>EN CAPTURA";
              case CONTESTED -> "<gold>EN DISPUTA";
              case NEUTRAL   -> "<white>NEUTRAL";
          };
      }
  }