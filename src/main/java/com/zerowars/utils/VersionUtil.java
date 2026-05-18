package com.zerowars.utils;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffectType;

/**
 * Compatibilidad multi-versión para ZeroWars.
 * Cubre Paper/Purpur 1.20.1 → 1.21.x+ sin crasheos por APIs renombradas.
 *
 * Cambios de API relevantes:
 *  - Paper 1.21+: Particle.REDSTONE → Particle.DUST (DustOptions sigue igual)
 *  - Paper 1.21+: Attribute.GENERIC_MAX_HEALTH → Attribute.MAX_HEALTH
 *  - Paper 1.20.6+: PotionEffectType registry keys cambiaron; getByName() sigue
 *                   disponible pero algunos nombres tienen alias distintos.
 */
public final class VersionUtil {

    private VersionUtil() {}

    /** Versión compacta: 12001 = 1.20.1, 12104 = 1.21.4 */
    private static final int VERSION;

    static {
        VERSION = parseVersion(Bukkit.getBukkitVersion());
    }

    private static int parseVersion(String raw) {
        // raw: "1.21.4-R0.1-SNAPSHOT"
        try {
            String[] parts = raw.split("-")[0].split("\\.");
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return major * 10000 + minor * 100 + patch;
        } catch (Exception ignored) {
            return 12001;
        }
    }

    /** true si el servidor corre en la versión dada o más reciente */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }

    public static boolean isAtLeast(int major, int minor, int patch) {
        return VERSION >= major * 10000 + minor * 100 + patch;
    }

    public static int getVersion() { return VERSION; }

    // ── Partículas ────────────────────────────────────────────────────────────

    /**
     * Obtiene una Particle intentando varios nombres en orden, con fallback.
     * Útil para partículas renombradas entre versiones.
     */
    public static Particle getParticle(Particle fallback, String... names) {
        for (String name : names) {
            try {
                return Particle.valueOf(name);
            } catch (IllegalArgumentException ignored) {}
        }
        return fallback;
    }

    /**
     * Particle de polvo de redstone.
     * 1.20.x: REDSTONE  |  1.21+: DUST
     * Requiere DustOptions como data en ambos casos.
     */
    public static Particle getDustParticle() {
        return getParticle(Particle.CRIT, "DUST", "REDSTONE");
    }

    // ── Atributos ─────────────────────────────────────────────────────────────

    /**
     * Devuelve el Attribute de salud máxima compatible con cualquier versión.
     *   ≤1.20.x → GENERIC_MAX_HEALTH
     *   1.21+   → MAX_HEALTH (GENERIC_MAX_HEALTH sigue como alias deprecated)
     */
    public static Attribute getMaxHealthAttribute() {
        // Intentar el nombre moderno primero (1.21+)
        try {
            Attribute a = Attribute.valueOf("MAX_HEALTH");
            return a;
        } catch (IllegalArgumentException ignored) {}
        // Fallback al nombre clásico (≤1.20.x y 1.21 con backward-compat)
        try {
            return Attribute.valueOf("GENERIC_MAX_HEALTH");
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    // ── Efectos de Poción ─────────────────────────────────────────────────────

    /**
     * Busca un PotionEffectType por nombre de forma segura.
     * Cubre alias entre 1.20.x y 1.21+:
     *   SLOW/SLOWNESS, FAST_DIGGING/HASTE, JUMP/JUMP_BOOST,
     *   CONFUSION/NAUSEA, HEAL/INSTANT_HEALTH, HARM/INSTANT_DAMAGE, etc.
     */
    @SuppressWarnings("deprecation")
    public static PotionEffectType getPotionEffectType(String name) {
        if (name == null || name.isEmpty()) return null;
        String upper = name.toUpperCase();

        // Intento directo
        PotionEffectType type = PotionEffectType.getByName(upper);
        if (type != null) return type;

        // Tabla de alias entre versiones
        String alias = switch (upper) {
            case "SLOW"              -> "SLOWNESS";
            case "SLOWNESS"          -> "SLOW";
            case "FAST_DIGGING"      -> "HASTE";
            case "HASTE"             -> "FAST_DIGGING";
            case "SLOW_DIGGING"      -> "MINING_FATIGUE";
            case "MINING_FATIGUE"    -> "SLOW_DIGGING";
            case "INCREASE_DAMAGE"   -> "STRENGTH";
            case "STRENGTH"          -> "INCREASE_DAMAGE";
            case "HEAL"              -> "INSTANT_HEALTH";
            case "INSTANT_HEALTH"    -> "HEAL";
            case "HARM"              -> "INSTANT_DAMAGE";
            case "INSTANT_DAMAGE"    -> "HARM";
            case "JUMP"              -> "JUMP_BOOST";
            case "JUMP_BOOST"        -> "JUMP";
            case "CONFUSION"         -> "NAUSEA";
            case "NAUSEA"            -> "CONFUSION";
            case "DAMAGE_RESISTANCE" -> "RESISTANCE";
            case "RESISTANCE"        -> "DAMAGE_RESISTANCE";
            default                  -> null;
        };

        return alias != null ? PotionEffectType.getByName(alias) : null;
    }

    /**
     * ¿Es un efecto de curación instantánea?
     * Cubre HEAL (≤1.20.x) e INSTANT_HEALTH (alias 1.21+).
     */
    @SuppressWarnings("deprecation")
    public static boolean isInstantHeal(PotionEffectType type) {
        if (type == null) return false;
        PotionEffectType heal = PotionEffectType.getByName("HEAL");
        PotionEffectType ih   = PotionEffectType.getByName("INSTANT_HEALTH");
        return type.equals(heal) || type.equals(ih);
    }
}
