package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.Consumable;
import com.zerowars.models.PlayerData;
import com.zerowars.utils.EffectUtil;
import com.zerowars.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor de consumibles PvP.
 * Registra, construye y aplica consumibles desde consumables.yml.
 * Los consumibles se identifican mediante PersistentDataContainer (NBT).
 *
 * Lifesteal activo: playerUUID → remaining duration ticks
 */
public class ConsumableManager {

    private final ZeroWars plugin;
    private final NamespacedKey consumableKey;

    // Cache de consumibles cargados: id → Consumable
    private final Map<String, Consumable> consumables = new HashMap<>();

    // Jugadores con lifesteal activo: UUID → ticks restantes
    private final Map<UUID, Integer> lifeStealActive = new ConcurrentHashMap<>();

    public ConsumableManager(ZeroWars plugin) {
        this.plugin = plugin;
        this.consumableKey = new NamespacedKey(plugin, "consumable_id");
        loadConsumables();
    }

    // ── Carga ────────────────────────────────────────────────────────────────

    public void loadConsumables() {
        consumables.clear();
        ConfigurationSection section =
                plugin.getConfigManager().consumables().getConfigurationSection("consumables");
        if (section == null) {
            plugin.getLogger().warning("No hay consumibles definidos en consumables.yml");
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection cs = section.getConfigurationSection(key);
            if (cs == null) continue;
            try {
                consumables.put(key, parseConsumable(key, cs));
            } catch (Exception e) {
                plugin.getLogger().warning("Error parseando consumible '" + key + "': " + e.getMessage());
            }
        }
        plugin.getLogger().info("Consumibles cargados: " + consumables.size());
    }

    private Consumable parseConsumable(String id, ConfigurationSection s) {
        Consumable c = new Consumable(id);
        c.setName(s.getString("name", id));
        c.setLore(s.getStringList("lore"));

        String matStr = s.getString("material", "PAPER");
        try { c.setMaterial(Material.valueOf(matStr)); }
        catch (IllegalArgumentException e) { c.setMaterial(Material.PAPER); }

        c.setCustomModelData(s.getInt("custom-model-data", -1));
        c.setCooldownSeconds(s.getLong("cooldown", 30));
        c.setBypassGlobalCooldown(s.getBoolean("bypass-global-cooldown", false));
        c.setSound(s.getString("sound", "ENTITY_ENDERMAN_TELEPORT"));

        String typeStr = s.getString("type", "POTION_EFFECT").toUpperCase();
        try { c.setType(Consumable.ConsumableType.valueOf(typeStr)); }
        catch (IllegalArgumentException e) { c.setType(Consumable.ConsumableType.POTION_EFFECT); }

        // Cargar efectos de poción
        List<Consumable.PotionEffect> effects = new ArrayList<>();
        if (s.isList("effects")) {
            for (var effectSection : s.getMapList("effects")) {
                String typeName = (String) effectSection.getOrDefault("type", "SPEED");
                int duration = (int) effectSection.getOrDefault("duration", 100);
                int amplifier = (int) effectSection.getOrDefault("amplifier", 0);
                try {
                    PotionEffectType pet = PotionEffectType.getByName(typeName);
                    if (pet != null) effects.add(new Consumable.PotionEffect(pet, duration, amplifier));
                } catch (Exception ignored) {}
            }
        }
        c.setEffects(effects);

        // Dash config
        if (c.getType() == Consumable.ConsumableType.DASH) {
            c.setDashPower(s.getDouble("dash-power", 2.5));
        }

        // Lifesteal config
        if (c.getType() == Consumable.ConsumableType.LIFESTEAL) {
            c.setLifestealDuration(s.getInt("duration", 160));
            c.setHealPerHit(s.getDouble("heal-per-hit", 1.0));
        }

        return c;
    }

    // ── Uso de consumibles ────────────────────────────────────────────────────

    /**
     * Intenta usar el consumible para el jugador dado.
     * Verifica cooldowns, aplica efectos y notifica.
     */
    public boolean useConsumable(Player player, String consumableId) {
        Consumable c = consumables.get(consumableId);
        if (c == null) return false;

        PlayerData data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());

        // Verificar cooldown global
        if (!c.isBypassGlobalCooldown()) {
            boolean globalEnabled = plugin.getConfigManager().cooldowns()
                    .getBoolean("cooldowns.global.enabled", true);
            if (globalEnabled && plugin.getCooldownManager().hasGlobalCooldown(player.getUniqueId())) {
                long remaining = plugin.getCooldownManager().getGlobalCooldownSeconds(player.getUniqueId());
                String msg = plugin.getConfigManager().getMessage("consumable.cooldown",
                        "%consumable%", c.getName(), "%time%", String.valueOf(remaining));
                player.sendActionBar(MessageUtil.parse(msg));
                return false;
            }
        }

        // Verificar cooldown individual
        if (data != null && data.isOnConsumableCooldown(consumableId)) {
            long remaining = data.getConsumableCooldownSeconds(consumableId);
            String msg = plugin.getConfigManager().getMessage("consumable.cooldown",
                    "%consumable%", c.getName(), "%time%", String.valueOf(remaining));
            player.sendActionBar(MessageUtil.parse(msg));
            return false;
        }

        // Aplicar efecto
        applyEffect(player, c);

        // Establecer cooldowns
        long globalSecs = plugin.getConfigManager().cooldowns()
                .getLong("cooldowns.global.duration", 3);
        plugin.getCooldownManager().setGlobalCooldown(player.getUniqueId(), globalSecs);
        if (data != null) {
            data.setConsumableCooldown(consumableId, c.getCooldownSeconds());
        }
        plugin.getCooldownManager().setLastActiveConsumable(player.getUniqueId(), consumableId);

        // Feedback visual
        EffectUtil.playConsumableEffect(player, c);

        // Mensaje de uso
        player.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("consumable.used",
                        "%consumable%", c.getName())));

        return true;
    }

    private void applyEffect(Player player, Consumable c) {
        switch (c.getType()) {
            case POTION_EFFECT, MULTI_EFFECT, HEAL -> {
                for (Consumable.PotionEffect pe : c.getEffects()) {
                    if (pe.type() == PotionEffectType.HEAL) {
                        // Curación instantánea: sumar HP
                        double currentHp = player.getHealth();
                        double maxHp = player.getAttribute(
                                org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
                        player.setHealth(Math.min(maxHp,
                                currentHp + (pe.amplifier() * 2.0)));
                    } else {
                        player.addPotionEffect(
                                new PotionEffect(pe.type(), pe.duration(), pe.amplifier()));
                    }
                }
            }
            case DASH -> {
                Vector direction = player.getLocation().getDirection()
                        .normalize()
                        .multiply(c.getDashPower());
                direction.setY(0.4);  // pequeño impulso vertical
                player.setVelocity(direction);
            }
            case LIFESTEAL -> {
                lifeStealActive.put(player.getUniqueId(), c.getLifestealDuration());
                // El efecto de lifesteal se procesa en ConsumableListener (EntityDamageByEntityEvent)
            }
            default -> {
                for (Consumable.PotionEffect pe : c.getEffects()) {
                    player.addPotionEffect(new PotionEffect(pe.type(), pe.duration(), pe.amplifier()));
                }
            }
        }
    }

    // ── Lifesteal ────────────────────────────────────────────────────────────

    /** ¿El jugador tiene lifesteal activo? */
    public boolean hasLifeSteal(UUID uuid) { return lifeStealActive.containsKey(uuid); }

    /** Aplica el lifesteal al atacar. Decrementa duración. */
    public void applyLifeStealHit(Player attacker, double amount) {
        Integer remaining = lifeStealActive.get(attacker.getUniqueId());
        if (remaining == null) return;

        Consumable c = consumables.values().stream()
                .filter(con -> con.getType() == Consumable.ConsumableType.LIFESTEAL)
                .findFirst().orElse(null);
        double healAmount = c != null ? c.getHealPerHit() : 1.0;

        double currentHp = attacker.getHealth();
        double maxHp = attacker.getAttribute(
                org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        attacker.setHealth(Math.min(maxHp, currentHp + healAmount));

        remaining--;
        if (remaining <= 0) {
            lifeStealActive.remove(attacker.getUniqueId());
        } else {
            lifeStealActive.put(attacker.getUniqueId(), remaining);
        }
    }

    // ── Construcción de Items ─────────────────────────────────────────────────

    /**
     * Construye el ItemStack de un consumible con NBT para identificación.
     */
    public ItemStack buildItem(String consumableId, int amount) {
        Consumable c = consumables.get(consumableId);
        if (c == null) return new ItemStack(Material.BARRIER);

        ItemStack item = new ItemStack(c.getMaterial(), amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Nombre con MiniMessage
        meta.displayName(MessageUtil.parse(c.getName()));

        // Lore
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : c.getLore()) {
            lore.add(MessageUtil.parse(line));
        }
        meta.lore(lore);

        // Custom model data
        if (c.hasCustomModelData()) meta.setCustomModelData(c.getCustomModelData());

        // NBT: marcar el item con el ID del consumible para identificarlo en clicks
        meta.getPersistentDataContainer().set(
                consumableKey, PersistentDataType.STRING, consumableId);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Extrae el ID de consumible de un item (null si no es consumible).
     */
    public String getConsumableId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer()
                .get(consumableKey, PersistentDataType.STRING);
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public Optional<Consumable> getConsumable(String id) {
        return Optional.ofNullable(consumables.get(id));
    }

    public Collection<Consumable> getAllConsumables() {
        return Collections.unmodifiableCollection(consumables.values());
    }

    public boolean consumableExists(String id) { return consumables.containsKey(id); }
}
