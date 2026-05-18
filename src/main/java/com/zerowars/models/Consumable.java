package com.zerowars.models;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de un consumible PvP.
 * Los consumibles son ítems físicos que se activan con click derecho.
 * NO son comandos. Son items con NBT custom que el plugin detecta y procesa.
 */
public class Consumable {

    public enum ConsumableType {
        POTION_EFFECT,   // Aplica efectos de poción
        DASH,            // Lanzamiento rápido en dirección de la vista
        LIFESTEAL,       // Activa robo de vida por N golpes/segundos
        MULTI_EFFECT,    // Múltiples efectos simultáneos
        HEAL,            // Curación instantánea
        CUSTOM           // Lógica completamente personalizada
    }

    /** Un efecto de poción dentro del consumible. */
    public record PotionEffect(PotionEffectType type, int duration, int amplifier) {}

    // ── Identificación ────────────────────────────────────────────────────────
    private final String id;
    private String name;             // MiniMessage
    private List<String> lore;       // MiniMessage por línea
    private Material material;
    private int customModelData;     // -1 = sin modelo custom

    // ── Tipo y comportamiento ─────────────────────────────────────────────────
    private ConsumableType type;
    private List<PotionEffect> effects;

    // Dash específico
    private double dashPower;

    // Lifesteal específico
    private int lifestealDuration;   // ticks
    private double healPerHit;       // corazones por golpe (1.0 = medio corazón en HP)

    // ── Cooldowns ────────────────────────────────────────────────────────────
    private long cooldownSeconds;
    private boolean bypassGlobalCooldown;

    // ── Efectos visuales ─────────────────────────────────────────────────────
    private String sound;
    private List<String> particles;  // nombres de particle effect

    // ── Constructor ───────────────────────────────────────────────────────────
    public Consumable(String id) {
        this.id = id;
        this.lore = new ArrayList<>();
        this.effects = new ArrayList<>();
        this.particles = new ArrayList<>();
        this.customModelData = -1;
        this.type = ConsumableType.POTION_EFFECT;
        this.dashPower = 2.0;
        this.bypassGlobalCooldown = false;
    }

    // ── Métodos de utilidad ───────────────────────────────────────────────────

    /** Clave NBT usada para identificar este consumible en el item. */
    public String getNbtKey() { return "zerowars_consumable"; }

    /** Añade un efecto de poción. */
    public void addEffect(PotionEffectType type, int duration, int amplifier) {
        effects.add(new PotionEffect(type, duration, amplifier));
    }

    /** ¿Tiene modelo custom definido? */
    public boolean hasCustomModelData() { return customModelData > 0; }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getId()                         { return id; }
    public String getName()                       { return name; }
    public List<String> getLore()                 { return lore; }
    public Material getMaterial()                 { return material; }
    public int getCustomModelData()               { return customModelData; }
    public ConsumableType getType()               { return type; }
    public List<PotionEffect> getEffects()        { return effects; }
    public double getDashPower()                  { return dashPower; }
    public int getLifestealDuration()             { return lifestealDuration; }
    public double getHealPerHit()                 { return healPerHit; }
    public long getCooldownSeconds()              { return cooldownSeconds; }
    public boolean isBypassGlobalCooldown()       { return bypassGlobalCooldown; }
    public String getSound()                      { return sound; }
    public List<String> getParticles()            { return particles; }

    public void setName(String n)                 { this.name = n; }
    public void setLore(List<String> l)           { this.lore = l; }
    public void setMaterial(Material m)           { this.material = m; }
    public void setCustomModelData(int d)         { this.customModelData = d; }
    public void setType(ConsumableType t)         { this.type = t; }
    public void setEffects(List<PotionEffect> e)  { this.effects = e; }
    public void setDashPower(double d)            { this.dashPower = d; }
    public void setLifestealDuration(int d)       { this.lifestealDuration = d; }
    public void setHealPerHit(double h)           { this.healPerHit = h; }
    public void setCooldownSeconds(long c)        { this.cooldownSeconds = c; }
    public void setBypassGlobalCooldown(boolean b){ this.bypassGlobalCooldown = b; }
    public void setSound(String s)                { this.sound = s; }
    public void setParticles(List<String> p)      { this.particles = p; }
}
