package com.zerowars.managers;

  import com.zerowars.ZeroWars;
  import com.zerowars.utils.MessageUtil;
  import org.bukkit.Location;
  import org.bukkit.Material;
  import org.bukkit.NamespacedKey;
  import org.bukkit.enchantments.Enchantment;
  import org.bukkit.inventory.ItemFlag;
  import org.bukkit.inventory.ItemStack;
  import org.bukkit.inventory.meta.ItemMeta;
  import org.bukkit.persistence.PersistentDataType;

  import java.util.Arrays;
  import java.util.Map;
  import java.util.UUID;
  import java.util.concurrent.ConcurrentHashMap;

  /**
   * Gestiona el hacha wand para seleccion de zonas.
   * Almacena pos1/pos2 y los datos pendientes de la zona a crear.
   */
  public class ZoneWandManager {

      private static final String WAND_KEY = "zone_wand";

      private final ZeroWars plugin;
      private final NamespacedKey wandKey;

      private final Map<UUID, Location> pos1        = new ConcurrentHashMap<>();
      private final Map<UUID, Location> pos2        = new ConcurrentHashMap<>();
      private final Map<UUID, String>   pendingName = new ConcurrentHashMap<>();
      private final Map<UUID, String>   pendingType = new ConcurrentHashMap<>();

      public ZoneWandManager(ZeroWars plugin) {
          this.plugin  = plugin;
          this.wandKey = new NamespacedKey(plugin, WAND_KEY);
      }

      // ── Wand item ─────────────────────────────────────────────────────────────

      public ItemStack createWand() {
          ItemStack axe  = new ItemStack(Material.GOLDEN_AXE);
          ItemMeta  meta = axe.getItemMeta();
          if (meta != null) {
              meta.displayName(MessageUtil.parse("<gold><bold>⚒ ZW Zone Wand"));
              meta.lore(Arrays.asList(
                  MessageUtil.parse(" "),
                  MessageUtil.parse("<gray>Clic Izq en bloque  → <yellow>Marcar Pos1"),
                  MessageUtil.parse("<gray>Clic Der en bloque  → <yellow>Marcar Pos2"),
                  MessageUtil.parse(" "),
                  MessageUtil.parse("<green>Con ambos puntos se abre el menu de creacion.")
              ));
              meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
              meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BOOLEAN, true);
              axe.setItemMeta(meta);
          }
          return axe;
      }

      public boolean isWand(ItemStack item) {
          if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
          return item.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.BOOLEAN);
      }

      // ── Posiciones ────────────────────────────────────────────────────────────

      public void setPos1(UUID uuid, Location loc) { pos1.put(uuid, loc); }
      public void setPos2(UUID uuid, Location loc) { pos2.put(uuid, loc); }
      public Location getPos1(UUID uuid)           { return pos1.get(uuid); }
      public Location getPos2(UUID uuid)           { return pos2.get(uuid); }
      public boolean hasBothPositions(UUID uuid)   { return pos1.containsKey(uuid) && pos2.containsKey(uuid); }

      // ── Datos pendientes de la zona nueva ────────────────────────────────────

      public void setPendingName(UUID uuid, String name) { pendingName.put(uuid, name); }
      public String getPendingName(UUID uuid)             { return pendingName.get(uuid); }
      public boolean hasPendingName(UUID uuid)            { return pendingName.containsKey(uuid); }

      public void setPendingType(UUID uuid, String type)  { pendingType.put(uuid, type); }
      public String getPendingType(UUID uuid)              { return pendingType.getOrDefault(uuid, "MINE"); }

      // ── Limpieza ──────────────────────────────────────────────────────────────

      public void clearPlayer(UUID uuid) {
          pos1.remove(uuid); pos2.remove(uuid);
          pendingName.remove(uuid); pendingType.remove(uuid);
      }
  }