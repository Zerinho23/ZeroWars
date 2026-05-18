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
 * Detecta click derecho con items consumibles y aplica efectos.
 * También gestiona el lifesteal al golpear entidades.
 */
public class ConsumableListener implements Listener {

    private final ZeroWars plugin;

    public ConsumableListener(ZeroWars plugin) {
        this.plugin = plugin;
    }

    // ── Click derecho con consumible ─────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        // Solo click derecho, solo mano principal (evitar doble ejecución)
        if (event.getHand() != EquipmentSlot.HAND) return;
        var action = event.getAction();
        if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                && action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("zerowars.consumables.use")) return;

        var item = event.getItem();
        if (item == null) return;

        // Identificar consumible por NBT
        String consumableId = plugin.getConsumableManager().getConsumableId(item);
        if (consumableId == null) return;

        // Cancelar el evento para evitar interacción con bloques
        event.setCancelled(true);

        // Aplicar consumible
        boolean used = plugin.getConsumableManager().useConsumable(player, consumableId);
        if (used) {
            // Reducir cantidad del item en mano en 1
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
    }

    // ── Lifesteal: robo de vida al golpear ───────────────────────────────────

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof Player)) return; // solo PvP

        if (!plugin.getConsumableManager().hasLifeSteal(attacker.getUniqueId())) return;

        plugin.getConsumableManager().applyLifeStealHit(attacker, event.getDamage());
    }
}
