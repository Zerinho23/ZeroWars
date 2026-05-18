package com.zerowars.utils;

import com.zerowars.models.Consumable;
import com.zerowars.models.Zone;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Utilidad para efectos visuales y sonoros.
 * Centraliza partículas, sonidos y efectos de captura/consumibles
 * para mantener el código de managers limpio.
 */
public final class EffectUtil {

    private EffectUtil() {}

    // ── Efectos de captura ────────────────────────────────────────────────────

    /**
     * Efecto visual al iniciar una captura.
     */
    public static void playCaptureStartEffect(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.FLAME, loc.add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        player.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
    }

    /**
     * Fanfarria completa al completar una captura.
     */
    public static void playCaptureFanfare(Player player, Zone zone) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world == null) return;

        // Partículas de celebración
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.add(0, 1, 0), 80, 0.5, 1.0, 0.5, 0.1);
        world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 50, 0.5, 1.5, 0.5, 0.2);

        // Sonido de victoria
        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // Title al capturador
        MessageUtil.sendTitle(player,
                "<gradient:#ffaa00:#ff4400>⚔ ZONA CAPTURADA",
                "<gray>" + zone.getDisplayName(),
                10, 60, 20);
    }

    /**
     * Efecto de partículas en el borde de una zona (se llama periódicamente).
     */
    public static void spawnZoneBorderParticles(Zone zone, World world) {
        if (world == null) return;

        // Partículas en las 4 esquinas del área
        double minX = zone.getMinX(), minZ = zone.getMinZ();
        double maxX = zone.getMaxX(), maxZ = zone.getMaxZ();
        double y    = zone.getMinY();

        Particle particle = Particle.REDSTONE;
        spawnLineParticles(world, minX, y, minZ, maxX, y, minZ, particle, 0.5);
        spawnLineParticles(world, maxX, y, minZ, maxX, y, maxZ, particle, 0.5);
        spawnLineParticles(world, maxX, y, maxZ, minX, y, maxZ, particle, 0.5);
        spawnLineParticles(world, minX, y, maxZ, minX, y, minZ, particle, 0.5);
    }

    private static void spawnLineParticles(World world,
                                            double x1, double y1, double z1,
                                            double x2, double y2, double z2,
                                            Particle particle, double spacing) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int count = (int) (length / spacing);
        if (count == 0) return;
        for (int i = 0; i <= count; i++) {
            double t = (double) i / count;
            Location loc = new Location(world,
                    x1 + dx * t, y1 + dy * t + 0.5, z1 + dz * t);
            world.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }

    // ── Efectos de consumibles ────────────────────────────────────────────────

    /**
     * Efecto visual y sonoro al usar un consumible.
     */
    public static void playConsumableEffect(Player player, Consumable consumable) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null) return;

        // Partículas según el tipo
        switch (consumable.getType()) {
            case DASH -> {
                world.spawnParticle(Particle.PORTAL, loc, 30, 0.3, 0.5, 0.3, 0.1);
                world.spawnParticle(Particle.REVERSE_PORTAL, loc, 15, 0.2, 0.2, 0.2, 0.05);
            }
            case LIFESTEAL -> {
                world.spawnParticle(Particle.DRIP_LAVA, loc, 20, 0.3, 0.5, 0.3, 0.0);
                world.spawnParticle(Particle.FALLING_LAVA, loc, 10, 0.2, 0.2, 0.2, 0.0);
            }
            default -> {
                world.spawnParticle(Particle.SPELL_WITCH, loc, 25, 0.3, 0.5, 0.3, 0.0);
                world.spawnParticle(Particle.END_ROD, loc, 10, 0.2, 0.5, 0.2, 0.05);
            }
        }

        // Sonido configurado
        if (consumable.getSound() != null && !consumable.getSound().isEmpty()) {
            try {
                Sound sound = Sound.valueOf(consumable.getSound());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // ── Efectos de eventos ────────────────────────────────────────────────────

    /**
     * Efecto de inicio de evento para todos los jugadores online.
     */
    public static void broadcastEventEffect(org.bukkit.Server server, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            server.getOnlinePlayers().forEach(p ->
                    p.playSound(p.getLocation(), sound, 1.0f, 1.0f));
        } catch (IllegalArgumentException ignored) {}
    }

    // ── Efectos de heat ───────────────────────────────────────────────────────

    public static void playHeatLevelUpEffect(Player player) {
        Location loc = player.getLocation().add(0, 1, 0);
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(Particle.FLAME, loc, 40, 0.3, 1.0, 0.3, 0.1);
        world.spawnParticle(Particle.LAVA, loc, 20, 0.5, 1.0, 0.5, 0.0);
        player.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, 1.0f, 0.8f);
    }
}
