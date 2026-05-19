package com.zerowars.listeners;

  import com.zerowars.ZeroWars;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Location;
  import org.bukkit.entity.Player;
  import org.bukkit.event.EventHandler;
  import org.bukkit.event.Listener;
  import org.bukkit.event.block.BlockBreakEvent;
  import org.bukkit.event.player.PlayerInteractEvent;
  import org.bukkit.inventory.EquipmentSlot;

  import java.util.UUID;

  /** Listener del hacha wand para seleccion de pos1/pos2 de zonas. */
  public class WandListener implements Listener {

      private final ZeroWars plugin;

      public WandListener(ZeroWars plugin) { this.plugin = plugin; }

      @EventHandler
      public void onInteract(PlayerInteractEvent e) {
          // Solo mano principal para evitar duplicado
          if (e.getHand() != EquipmentSlot.HAND) return;
          if (!plugin.getZoneWandManager().isWand(e.getItem())) return;

          Player player = e.getPlayer();
          UUID   uuid   = player.getUniqueId();

          switch (e.getAction()) {
              case LEFT_CLICK_BLOCK -> {
                  e.setCancelled(true);
                  Location loc = e.getClickedBlock().getLocation();
                  plugin.getZoneWandManager().setPos1(uuid, loc);
                  MessageUtil.sendActionBar(player,
                      "<yellow>✔ Pos1: <white>" + fmt(loc));
                  MessageUtil.send(player,
                      "<yellow>Pos1 <gray>marcada en <white>" + fmt(loc));
                  checkBoth(player);
              }
              case RIGHT_CLICK_BLOCK -> {
                  e.setCancelled(true);
                  Location loc = e.getClickedBlock().getLocation();
                  plugin.getZoneWandManager().setPos2(uuid, loc);
                  MessageUtil.sendActionBar(player,
                      "<yellow>✔ Pos2: <white>" + fmt(loc));
                  MessageUtil.send(player,
                      "<yellow>Pos2 <gray>marcada en <white>" + fmt(loc));
                  checkBoth(player);
              }
          }
      }

      /** Cancela la ruptura de bloque si el jugador lleva el wand. */
      @EventHandler
      public void onBlockBreak(BlockBreakEvent e) {
          if (plugin.getZoneWandManager().isWand(
                  e.getPlayer().getInventory().getItemInMainHand()))
              e.setCancelled(true);
      }

      // ── Helpers ──────────────────────────────────────────────────────────────

      private void checkBoth(Player player) {
          if (!plugin.getZoneWandManager().hasBothPositions(player.getUniqueId())) return;
          MessageUtil.send(player, "<green>¡Ambos puntos marcados! Abriendo configuracion...");
          // 2 ticks de delay para que el ActionBar se muestre antes de abrir el GUI
          plugin.getServer().getScheduler().runTaskLater(plugin,
              () -> plugin.getGuiManager().openZoneCreate(player), 2L);
      }

      private String fmt(Location loc) {
          return "X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ();
      }
  }