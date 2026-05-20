package com.zerowars.listeners;

import com.zerowars.ZeroWars;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Listener de consumibles PvP.
 * FIX: permiso corregido de zerowars.consumables.use → zerowars.consumable.use
 *      para coincidir con plugin.yml.
 */
public class ConsumableListener implements Listener {

    private final ZeroWars plugin;

    public ConsumableListener(ZeroWars plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        var action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        // FIX: era "zerowars.consumables.use" (con 's') — no coincidía con plugin.yml
        if (!player.hasPermission("zerowars.consumable.use")) return;

        var item = event.getItem();
        if (item == null) return;

        String consumableId = plugin.getConsumableManager().getConsumableId(item);
        if (consumableId == null) return;

        event.setCancelled(true);

        boolean used = plugin.getConsumableManager().useConsumable(player, consumableId);
        if (used) {
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player)) return;

        if (!plugin.getConsumableManager().hasLifeSteal(attacker.getUniqueId())) return;

        plugin.getConsumableManager().applyLifeStealHit(attacker, event.getDamage());
    }
}
