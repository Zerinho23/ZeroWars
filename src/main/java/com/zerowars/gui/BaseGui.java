package com.zerowars.gui;

  import com.zerowars.ZeroWars;
  import com.zerowars.utils.MessageUtil;
  import net.kyori.adventure.text.Component;
  import org.bukkit.Bukkit;
  import org.bukkit.Material;
  import org.bukkit.entity.Player;
  import org.bukkit.event.inventory.ClickType;
  import org.bukkit.inventory.Inventory;
  import org.bukkit.inventory.InventoryHolder;
  import org.bukkit.inventory.ItemStack;
  import org.bukkit.inventory.meta.ItemMeta;
  import org.jetbrains.annotations.NotNull;

  import java.util.Arrays;
  import java.util.List;
  import java.util.stream.Collectors;

  /** Base abstracta para todos los menus GUI de ZeroWars. */
  public abstract class BaseGui implements InventoryHolder {

      protected final ZeroWars plugin;
      protected final Player player;
      protected Inventory inventory;

      protected BaseGui(ZeroWars plugin, Player player, int size, String title) {
          this.plugin    = plugin;
          this.player    = player;
          this.inventory = Bukkit.createInventory(this, size, MessageUtil.parse(title));
      }

      public abstract void buildGui();
      public abstract void handleClick(int slot, ItemStack item, ClickType click);

      public void open() { buildGui(); player.openInventory(inventory); }

      @Override
      public @NotNull Inventory getInventory() { return inventory; }

      // ── Helpers ──────────────────────────────────────────────────────────────

      protected ItemStack item(Material mat, String name, String... lore) {
          ItemStack item = new ItemStack(mat);
          ItemMeta  meta = item.getItemMeta();
          if (meta != null) {
              meta.displayName(MessageUtil.parse(name));
              if (lore.length > 0) {
                  List<Component> loreList = Arrays.stream(lore)
                          .map(MessageUtil::parse).collect(Collectors.toList());
                  meta.lore(loreList);
              }
              item.setItemMeta(meta);
          }
          return item;
      }

      protected ItemStack filler(Material mat) {
          ItemStack item = new ItemStack(mat);
          ItemMeta  meta = item.getItemMeta();
          if (meta != null) { meta.displayName(Component.empty()); item.setItemMeta(meta); }
          return item;
      }

      /** Rellena el borde (fila 0, fila N-1, columnas 0 y 8). */
      protected void border(Material mat) {
          int size = inventory.getSize();
          int rows = size / 9;
          ItemStack b = filler(mat);
          for (int i = 0; i < 9; i++) inventory.setItem(i, b);
          for (int i = size - 9; i < size; i++) inventory.setItem(i, b);
          for (int row = 1; row < rows - 1; row++) {
              inventory.setItem(row * 9,     b);
              inventory.setItem(row * 9 + 8, b);
          }
      }
  }