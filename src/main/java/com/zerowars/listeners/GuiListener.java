package com.zerowars.listeners;

  import com.zerowars.gui.BaseGui;
  import org.bukkit.entity.Player;
  import org.bukkit.event.EventHandler;
  import org.bukkit.event.Listener;
  import org.bukkit.event.inventory.InventoryClickEvent;
  import org.bukkit.event.inventory.InventoryDragEvent;
  import org.bukkit.inventory.ItemStack;

  /** Intercepta clicks en GUIs de ZeroWars y los delega al GUI correspondiente. */
  public class GuiListener implements Listener {

      @EventHandler
      public void onInventoryClick(InventoryClickEvent e) {
          if (!(e.getInventory().getHolder() instanceof BaseGui gui)) return;
          e.setCancelled(true);
          if (!(e.getWhoClicked() instanceof Player)) return;
          ItemStack item = e.getCurrentItem();
          if (item == null) return;
          gui.handleClick(e.getSlot(), item, e.getClick());
      }

      @EventHandler
      public void onInventoryDrag(InventoryDragEvent e) {
          if (e.getInventory().getHolder() instanceof BaseGui) e.setCancelled(true);
      }
  }