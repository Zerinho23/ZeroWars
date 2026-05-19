package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import com.zerowars.models.Zone;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Material;
  import org.bukkit.entity.Player;
  import org.bukkit.event.inventory.ClickType;
  import org.bukkit.inventory.ItemStack;

  import java.util.stream.Collectors;

  /** GUI de detalle y edicion de una zona individual. */
  public class ZoneDetailGui extends BaseGui {

      private final String zoneId;

      public ZoneDetailGui(ZeroWars plugin, Player player, String zoneId) {
          super(plugin, player, 54, "<gray>Zona: <white>" + zoneId);
          this.zoneId = zoneId;
      }

      @Override
      public void buildGui() {
          border(Material.GRAY_STAINED_GLASS_PANE);
          Zone z = plugin.getZoneManager().getZone(zoneId).orElse(null);
          if (z == null) {
              inventory.setItem(22, item(Material.BARRIER, "<red>Zona no encontrada",
                  "<gray>La zona '" + zoneId + "' no existe."));
              inventory.setItem(49, item(Material.ARROW, "<yellow>Volver"));
              return;
          }

          // Panel info (centro)
          inventory.setItem(13, item(Material.MAP,
              z.getDisplayName(),
              "<gray>ID: <white>"    + z.getId(),
              "<gray>Tipo: <white>"  + z.getType(),
              "<gray>Mundo: <white>" + z.getWorldName(),
              "<gray>Nivel: <gold>"  + z.getLevel() + "<gray>/" + z.getMaxLevel(),
              "<gray>Capture: <white>" + z.getCaptureTime() + "s",
              stateLabel(z)));

          // Dueno
          String ownerLine = z.getOwnerName() != null ? "<yellow>" + z.getOwnerName() : "<gray>Sin dueno";
          inventory.setItem(22, item(Material.PLAYER_HEAD,
              "<white>Dueno actual", ownerLine));

          // Toggle activa
          boolean en = z.isEnabled();
          inventory.setItem(28, item(en ? Material.LIME_DYE : Material.GRAY_DYE,
              en ? "<green>Zona ACTIVA" : "<gray>Zona DESACTIVADA",
              en ? "<gray>Clic para desactivar" : "<gray>Clic para activar",
              " ", "<yellow>▶ Clic para cambiar"));

          // Rewards
          String rewards = z.getRewardIds().isEmpty()
              ? "<gray>(ninguno)"
              : "<white>" + String.join("<gray>, <white>", z.getRewardIds());
          inventory.setItem(30, item(Material.CHEST,
              "<gold>Rewards asignados", "<gray>IDs: " + rewards,
              " ", "<gray>/zw setreward " + zoneId + " <id> para cambiar"));

          // Nivel
          inventory.setItem(32, item(Material.EXPERIENCE_BOTTLE,
              "<aqua>Nivel: <white>" + z.getLevel() + "<gray>/" + z.getMaxLevel(),
              "<gray>Clic izq: +1 nivel",
              "<gray>Clic der: -1 nivel"));

          // Resetear dueno
          inventory.setItem(34, item(Material.TNT,
              "<red>Resetear zona",
              "<gray>Elimina el dueno actual.",
              "<gray>La zona vuelve a NEUTRAL.",
              " ", "<yellow>▶ Clic para resetear"));

          // Volver
          inventory.setItem(49, item(Material.ARROW,
              "<yellow>◀ Volver a lista de zonas"));
      }

      @Override
      public void handleClick(int slot, ItemStack item, ClickType click) {
          Zone z = plugin.getZoneManager().getZone(zoneId).orElse(null);
          if (z == null) { plugin.getGuiManager().openZoneList(player); return; }

          switch (slot) {
              case 28 -> {
                  z.setEnabled(!z.isEnabled());
                  plugin.getZoneManager().saveAsync(z);
                  plugin.getZoneManager().saveZoneToYml(z);
                  new ZoneDetailGui(plugin, player, zoneId).open();
              }
              case 32 -> {
                  int delta = click.isRightClick() ? -1 : 1;
                  int newLvl = Math.max(1, Math.min(z.getMaxLevel(), z.getLevel() + delta));
                  z.setLevel(newLvl);
                  plugin.getZoneManager().saveAsync(z);
                  new ZoneDetailGui(plugin, player, zoneId).open();
              }
              case 34 -> {
                  z.setOwnerUUID(null);
                  z.setOwnerName(null);
                  z.setOwnerClanId(null);
                  z.setCaptureProgress(0);
                  z.setLastRewardTime(0);
                  plugin.getZoneManager().saveAsync(z);
                  MessageUtil.send(player,
                      "<green>Zona <white>" + zoneId + "<green> reseteada a NEUTRAL.");
                  new ZoneDetailGui(plugin, player, zoneId).open();
              }
              case 49 -> plugin.getGuiManager().openZoneList(player);
          }
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