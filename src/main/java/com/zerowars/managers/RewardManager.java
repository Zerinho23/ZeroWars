package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.utils.MessageUtil;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Gestor de recompensas de zona (ZeroWars v1.2.0).
 *
 * Tipos de recompensa configuradas en rewards.yml:
 *   on-capture  -- entregada una vez al capturar la zona
 *   passive     -- entregada periodicamente al dueno mientras domina
 *   per-level   -- bonus al subir de nivel la zona
 *
 * Vault (softdep): si no esta disponible, money se omite sin excepciones.
 * Items sobrantes (inventario lleno) se dropean en el suelo del jugador.
 * Multiplicadores de evento (EventManager) se aplican a money.
 *
 * FIX v1.4.3: giveItems ahora usa getMapList() en lugar de getConfigurationSection()
 *             porque en rewards.yml los items se definen como lista YAML, no como seccion.
 *             El metodo anterior siempre recibía null y nunca entregaba ningún item.
 */
public class RewardManager {

    private final ZeroWars plugin;
    private Economy economy = null;
    private boolean vaultEnabled = false;

    public RewardManager(ZeroWars plugin) {
        this.plugin = plugin;
        setupVault();
    }

    // -- Vault ----------------------------------------------------------------

    private void setupVault() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault no encontrado. Recompensas de dinero desactivadas.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("No hay proveedor de economia en Vault.");
            return;
        }
        this.economy = rsp.getProvider();
        this.vaultEnabled = true;
        plugin.getLogger().info("Vault integrado: " + economy.getName());
    }

    // -- API publica ----------------------------------------------------------

    /** Recompensa de captura: dinero + items + comandos (una sola vez). */
    public void giveOnCaptureReward(Player player, Zone zone) {
        double multiplier = plugin.getEventManager().getRewardMultiplier(zone.getId());
        for (String rewardId : zone.getRewardIds()) {
            ConfigurationSection sec = rewardSection(rewardId, "on-capture");
            if (sec == null) continue;
            giveMoney(player, sec.getDouble("money", 0), multiplier, zone);
            // FIX: usar getMapList ya que items es una lista YAML, no una seccion
            giveItemsList(player, sec.getMapList("items"));
            executeCommands(player, sec.getStringList("commands"));
        }
    }

    /** Recompensa pasiva: dinero + items periodicamente mientras el dueno domina. */
    public void givePassiveReward(Player player, Zone zone) {
        double multiplier = plugin.getEventManager().getRewardMultiplier(zone.getId());
        double totalMoney = 0;
        for (String rewardId : zone.getRewardIds()) {
            ConfigurationSection sec = rewardSection(rewardId, "passive");
            if (sec == null) continue;
            double money = sec.getDouble("money", 0);
            giveMoney(player, money, multiplier, zone);
            // FIX: usar getMapList ya que items es una lista YAML, no una seccion
            giveItemsList(player, sec.getMapList("items"));
            totalMoney += money * multiplier;
        }
        if (totalMoney > 0 && vaultEnabled) {
            player.sendActionBar(MessageUtil.parse(
                    "<green>+<gold>" + String.format("%.0f", totalMoney)
                    + " coins</gold> <gray>- " + zone.getDisplayName()));
        }
    }

    /** Recompensa de level-up: bonus money + items especificos por nivel. */
    public void giveLevelUpReward(Player player, Zone zone, int newLevel) {
        for (String rewardId : zone.getRewardIds()) {
            ConfigurationSection perLevel = rewardSection(rewardId, "per-level");
            if (perLevel == null) continue;
            ConfigurationSection captureSec = rewardSection(rewardId, "on-capture");
            if (captureSec != null) {
                double baseMoney = captureSec.getDouble("money", 0);
                double mult = perLevel.getDouble("money-multiplier", 1.5);
                giveMoney(player, baseMoney * (mult - 1.0), 1.0, zone);
            }
            // FIX: extra-items-per-level.<nivel> tambien es una lista YAML
            giveItemsList(player, perLevel.getMapList("extra-items-per-level." + newLevel));
        }
        MessageUtil.sendTitle(player,
                "<gradient:#ffaa00:#ff6600>ZONA MEJORADA",
                zone.getDisplayName() + " <white>- Nivel <gold>" + newLevel,
                5, 50, 15);
        Bukkit.broadcast(MessageUtil.parse(
                "<gradient:#ffaa00:#ff4400>* <bold>" + player.getName()
                + "</bold> <yellow>mejoro " + zone.getDisplayName()
                + " <white>al <gold>Nivel " + newLevel + "</gold>!"));
    }

    /**
     * Intervalo de recompensa pasiva en ms para la zona.
     * Lee passive.interval del primer rewardId de la zona. Default: 60s.
     */
    public long getPassiveIntervalMs(Zone zone) {
        for (String rewardId : zone.getRewardIds()) {
            ConfigurationSection sec = rewardSection(rewardId, "passive");
            if (sec != null) return sec.getLong("interval", 60) * 1000L;
        }
        return plugin.getConfigManager().config()
                .getLong("zones.reward-cooldown", 60) * 1000L;
    }

    // -- Internos -------------------------------------------------------------

    private ConfigurationSection rewardSection(String rewardId, String type) {
        return plugin.getConfigManager().rewards()
                .getConfigurationSection("rewards." + rewardId + "." + type);
    }

    private void giveMoney(Player player, double amount, double multiplier, Zone zone) {
        if (!vaultEnabled || economy == null || amount <= 0) return;
        double total = Math.round(amount * multiplier * 100.0) / 100.0;
        economy.depositPlayer(player, total);
        if (plugin.getConfigManager().isDebug())
            plugin.getLogger().info("[Reward] $" + total + " -> " + player.getName()
                    + " [" + zone.getId() + "]");
    }

    /**
     * Entrega items a un jugador a partir de una lista de mapas YAML.
     * Formato en rewards.yml:
     *   items:
     *     - material: DIAMOND
     *       amount: 3
     *       name: "<aqua>Diamante"
     *       lore:
     *         - "<gray>Linea de lore"
     *
     * FIX: reemplaza el anterior giveItems(Player, ConfigurationSection) que usaba
     *      getConfigurationSection("items") — siempre null para listas YAML.
     */
    private void giveItemsList(Player player, List<Map<?, ?>> items) {
        if (items == null || items.isEmpty()) return;
        for (Map<?, ?> rawMap : items) {
            try {
                String matStr = String.valueOf(
                        rawMap.getOrDefault("material", "PAPER")).toUpperCase();
                int amount = ((Number) rawMap.getOrDefault("amount", 1)).intValue();
                Material mat = Material.valueOf(matStr);
                ItemStack item = new ItemStack(mat, amount);

                String name = (String) rawMap.get("name");
                @SuppressWarnings("unchecked")
                List<String> lore = (List<String>) rawMap.getOrDefault("lore", List.of());

                if (name != null || !lore.isEmpty()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (name != null) meta.displayName(MessageUtil.parse(name));
                        if (!lore.isEmpty()) {
                            List<Component> loreComp = new ArrayList<>();
                            lore.forEach(l -> loreComp.add(MessageUtil.parse(l)));
                            meta.lore(loreComp);
                        }
                        item.setItemMeta(meta);
                    }
                }

                var leftover = player.getInventory().addItem(item);
                leftover.values().forEach(
                        drop -> player.getWorld().dropItemNaturally(player.getLocation(), drop));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING,
                        "Error entregando item de recompensa: " + e.getMessage());
            }
        }
    }

    private void executeCommands(Player player, List<String> cmds) {
        for (String cmd : cmds)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    cmd.replace("%player%", player.getName()));
    }

    // -- Getters --------------------------------------------------------------

    public boolean isVaultEnabled() { return vaultEnabled; }
    public Economy getEconomy()     { return economy; }
}
