package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import com.zerowars.models.Zone;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Location;
  import org.bukkit.Material;
  import org.bukkit.entity.Player;
  import org.bukkit.event.inventory.ClickType;
  import org.bukkit.inventory.ItemStack;

  import java.util.UUID;

  /**
   * GUI de creacion de zona nueva — aparece despues de marcar Pos1+Pos2 con el wand.
   *
   * Layout (54 slots):
   *  Fila 1: borde + info de area (dimensiones)
   *  Fila 2: borde + selector de tipo (5 botones)
   *  Fila 3: borde + boton "Poner nombre"
   *  Fila 4: borde + crear + cancelar
   *  Fila 5: borde
   */
  public class ZoneCreateGui extends BaseGui {

      // Tipos disponibles en orden de ciclo
      private static final Zone.ZoneType[] TYPES = {
          Zone.ZoneType.MINE, Zone.ZoneType.ROOM,
          Zone.ZoneType.RARE, Zone.ZoneType.EVENT, Zone.ZoneType.SPECIAL
      };
      private static final Material[] TYPE_MATS = {
          Material.IRON_PICKAXE, Material.BOOKSHELF,
          Material.GOLD_BLOCK,   Material.BEACON, Material.NETHER_STAR
      };
      private static final String[] TYPE_DESC = {
          "Zona de mineria — reward de recursos",
          "Sala central — alta disputa",
          "Zona rara — alta recompensa",
          "Solo activa durante eventos",
          "Zona especial de servidor"
      };

      public ZoneCreateGui(ZeroWars plugin, Player player) {
          super(plugin, player, 54, "<dark_green><bold>⚒ Crear Nueva Zona");
      }

      @Override
      public void buildGui() {
          border(Material.GREEN_STAINED_GLASS_PANE);

          UUID uuid = player.getUniqueId();
          Location p1 = plugin.getZoneWandManager().getPos1(uuid);
          Location p2 = plugin.getZoneWandManager().getPos2(uuid);
          String   pendingName = plugin.getZoneWandManager().getPendingName(uuid);
          String   pendingType = plugin.getZoneWandManager().getPendingType(uuid);

          // Calcular dimensiones
          int minX = Math.min(p1.getBlockX(), p2.getBlockX());
          int minY = Math.min(p1.getBlockY(), p2.getBlockY());
          int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
          int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
          int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
          int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());
          int dx = maxX - minX + 1;
          int dy = maxY - minY + 1;
          int dz = maxZ - minZ + 1;

          // ── Fila 1: Info de area ──────────────────────────────────────────────
          inventory.setItem(13, item(Material.COMPASS,
              "<aqua><bold>Area Seleccionada",
              "<gray>Mundo: <white>" + p1.getWorld().getName(),
              "<gray>Pos1: <white>" + fmt(p1),
              "<gray>Pos2: <white>" + fmt(p2),
              "<gray>Dimension: <yellow>" + dx + " x " + dy + " x " + dz + " bloques",
              "<gray>Volumen: <white>" + (dx * dy * dz) + " bloques"));

          // ── Fila 2: Selector de tipo (slots 19-23) ────────────────────────────
          for (int i = 0; i < TYPES.length; i++) {
              boolean selected = TYPES[i].name().equals(pendingType);
              String sel = selected ? " <green>[✔ SELECCIONADO]" : "";
              inventory.setItem(19 + i, item(TYPE_MATS[i],
                  (selected ? "<green><bold>" : "<gray>") + TYPES[i].name() + sel,
                  "<gray>" + TYPE_DESC[i],
                  " ",
                  selected ? "<green>Tipo seleccionado" : "<yellow>▶ Clic para seleccionar"));
          }

          // ── Fila 3: Nombre ────────────────────────────────────────────────────
          boolean hasName = pendingName != null && !pendingName.isBlank();
          inventory.setItem(31, item(
              hasName ? Material.NAME_TAG : Material.PAPER,
              hasName ? "<green>Nombre: <white>" + pendingName : "<yellow>Clic para escribir el nombre",
              hasName ? "<gray>Nombre de la zona en el chat" : "<gray>Se abrira el chat para que escribas",
              "<gray>el nombre de display de la zona.",
              " ",
              "<yellow>▶ Clic para " + (hasName ? "cambiar" : "escribir")));

          // ── Fila 4: Crear / Cancelar ──────────────────────────────────────────
          if (hasName) {
              inventory.setItem(37, item(Material.LIME_CONCRETE,
                  "<green><bold>✔ Crear Zona",
                  "<gray>Nombre: <white>" + pendingName,
                  "<gray>Tipo: <white>" + pendingType,
                  "<gray>Area: <white>" + dx + "x" + dy + "x" + dz,
                  " ",
                  "<yellow>▶ Clic para crear"));
          } else {
              inventory.setItem(37, item(Material.RED_CONCRETE,
                  "<red><bold>✗ Falta el Nombre",
                  "<gray>Haz clic en el boton de nombre",
                  "<gray>antes de poder crear la zona."));
          }

          inventory.setItem(43, item(Material.BARRIER,
              "<red>Cancelar",
              "<gray>Descarta la seleccion y vuelve",
              "<gray>al menu principal."));
      }

      @Override
      public void handleClick(int slot, ItemStack item, ClickType click) {
          UUID uuid = player.getUniqueId();

          // Selector de tipo (slots 19-23)
          for (int i = 0; i < TYPES.length; i++) {
              if (slot == 19 + i) {
                  plugin.getZoneWandManager().setPendingType(uuid, TYPES[i].name());
                  new ZoneCreateGui(plugin, player).open();
                  return;
              }
          }

          switch (slot) {
              case 31 -> {
                  // Pedir nombre via chat
                  player.closeInventory();
                  plugin.getChatInputManager().awaitInput(player,
                      "<green>Escribe el nombre de la zona en el chat <gray>(usa &codes o &#RRGGBB):",
                      (p, input) -> {
                          if (input.isBlank()) {
                              MessageUtil.send(p, "<red>Nombre invalido. Intenta de nuevo.");
                          } else {
                              plugin.getZoneWandManager().setPendingName(p.getUniqueId(), input);
                              MessageUtil.send(p, "<green>Nombre guardado: <white>" + input);
                          }
                          plugin.getGuiManager().openZoneCreate(p);
                      });
              }
              case 37 -> {
                  // Crear zona
                  String name = plugin.getZoneWandManager().getPendingName(uuid);
                  if (name == null || name.isBlank()) return;

                  Location p1 = plugin.getZoneWandManager().getPos1(uuid);
                  Location p2 = plugin.getZoneWandManager().getPos2(uuid);
                  String type = plugin.getZoneWandManager().getPendingType(uuid);

                  // Generar ID desde el nombre (snake_case, sin espacios ni tildes)
                  String id = toId(name);
                  if (plugin.getZoneManager().zoneExists(id)) {
                      MessageUtil.send(player, "<red>Ya existe una zona con ID '<white>" + id + "<red>'. Elige otro nombre.");
                      return;
                  }

                  Zone.ZoneType zType;
                  try   { zType = Zone.ZoneType.valueOf(type); }
                  catch (IllegalArgumentException e) { zType = Zone.ZoneType.MINE; }

                  double minX = Math.min(p1.getX(), p2.getX());
                  double minY = Math.min(p1.getY(), p2.getY());
                  double minZ = Math.min(p1.getZ(), p2.getZ());
                  double maxX = Math.max(p1.getX(), p2.getX()) + 1;
                  double maxY = Math.max(p1.getY(), p2.getY()) + 1;
                  double maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;

                  Zone zone = new Zone(id, name, zType, p1.getWorld().getName(),
                          minX, minY, minZ, maxX, maxY, maxZ, 1, 5, 30);

                  plugin.getZoneManager().registerZone(zone);
                  plugin.getZoneManager().saveZoneToYml(zone);
                  plugin.getZoneManager().saveAsync(zone);

                  // Quitar el wand del inventario
                  removeWandFromInventory();
                  plugin.getZoneWandManager().clearPlayer(uuid);

                  player.closeInventory();
                  MessageUtil.send(player,
                      "<green>✔ Zona <white>" + name + " <gray>(ID: <white>" + id + "<gray>) creada y guardada en zones.yml.");
                  MessageUtil.send(player,
                      "<gray>Usa <white>/zw menu <gray>→ Zonas para verla y configurarla.");
              }
              case 43 -> {
                  // Cancelar
                  plugin.getZoneWandManager().clearPlayer(uuid);
                  removeWandFromInventory();
                  player.closeInventory();
                  plugin.getGuiManager().openMainMenu(player);
              }
          }
      }

      // ── Helpers ───────────────────────────────────────────────────────────────

      private String fmt(Location loc) {
          return "X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ();
      }

      private String toId(String name) {
          return name.toLowerCase()
              .replaceAll("[^a-z0-9_]", "_")
              .replaceAll("_+", "_")
              .replaceAll("^_|_$", "");
      }

      private void removeWandFromInventory() {
          var inv = player.getInventory();
          for (int i = 0; i < inv.getSize(); i++) {
              ItemStack it = inv.getItem(i);
              if (plugin.getZoneWandManager().isWand(it)) {
                  inv.setItem(i, null);
                  break;
              }
          }
      }
  }