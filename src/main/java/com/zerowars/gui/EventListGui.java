package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import com.zerowars.models.ZoneEvent;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Material;
  import org.bukkit.entity.Player;
  import org.bukkit.event.inventory.ClickType;
  import org.bukkit.inventory.ItemStack;

  import java.util.ArrayList;
  import java.util.List;

  /** GUI para iniciar/detener eventos PvP. */
  public class EventListGui extends BaseGui {

      private List<ZoneEvent> eventList;

      private static final int[] SLOTS = {
          10,11,12,13,14,15,16,
          19,20,21,22,23,24,25,
          28,29,30,31,32,33,34,
          37,38,39,40,41,42,43
      };

      public EventListGui(ZeroWars plugin, Player player) {
          super(plugin, player, 54, "<aqua>Eventos PvP");
      }

      @Override
      public void buildGui() {
          border(Material.CYAN_STAINED_GLASS_PANE);
          eventList = new ArrayList<>(plugin.getEventManager().getAllEvents());
          int active = plugin.getEventManager().getActiveCount();

          for (int i = 0; i < Math.min(eventList.size(), SLOTS.length); i++) {
              ZoneEvent ev  = eventList.get(i);
              boolean isAct = plugin.getEventManager().isEventActive(ev.getId());
              inventory.setItem(SLOTS[i], item(
                  isAct ? Material.BEACON : Material.GRAY_CONCRETE,
                  isAct ? "<green><bold>" + ev.getName() : "<gray>" + ev.getName(),
                  "<gray>ID: <white>"           + ev.getId(),
                  "<gray>Duracion: <white>"     + ev.getDurationMinutes() + " min",
                  "<gray>Multiplicador: <gold>x" + ev.getRewardMultiplier(),
                  " ",
                  isAct ? "<red>▶ Clic para DETENER" : "<green>▶ Clic para INICIAR"));
          }

          if (active > 0)
              inventory.setItem(46, item(Material.RED_WOOL,
                  "<red>Detener todos <gray>(" + active + " activos)",
                  "<gray>Detiene todos los eventos PvP."));

          inventory.setItem(49, item(Material.NETHER_STAR,
              "<white>Menu principal", "<gray>Volver al menu admin."));
      }

      @Override
      public void handleClick(int slot, ItemStack item, ClickType click) {
          for (int i = 0; i < SLOTS.length; i++) {
              if (SLOTS[i] == slot && i < eventList.size()) {
                  ZoneEvent ev = eventList.get(i);
                  if (plugin.getEventManager().isEventActive(ev.getId())) {
                      plugin.getEventManager().endEvent(ev.getId());
                      MessageUtil.send(player, "<red>Evento <white>" + ev.getId() + "<red> detenido.");
                  } else {
                      plugin.getEventManager().startEvent(ev.getId());
                      MessageUtil.send(player, "<green>Evento <white>" + ev.getId() + "<green> iniciado.");
                  }
                  new EventListGui(plugin, player).open();
                  return;
              }
          }
          if (slot == 46) {
              new ArrayList<>(plugin.getEventManager().getActiveEvents())
                  .forEach(e -> plugin.getEventManager().endEvent(e.getId()));
              MessageUtil.send(player, "<red>Todos los eventos detenidos.");
              new EventListGui(plugin, player).open();
          }
          if (slot == 49) plugin.getGuiManager().openMainMenu(player);
      }
  }