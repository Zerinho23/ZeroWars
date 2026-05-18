package com.zerowars.utils;

import org.bukkit.Location;

/**
 * Utilidades de geometría de regiones/zonas.
 * Cálculos de distancias, centros, intersecciones y validaciones.
 */
public final class RegionUtil {

    private RegionUtil() {}

    /**
     * Verifica si una localización está dentro de una caja AABB.
     */
    public static boolean isInside(Location loc,
                                    double minX, double minY, double minZ,
                                    double maxX, double maxY, double maxZ) {
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    /**
     * Distancia al cuadrado entre dos ubicaciones (más rápido que distancia normal).
     */
    public static double distanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Centro de una AABB.
     */
    public static double[] center(double minX, double minY, double minZ,
                                   double maxX, double maxY, double maxZ) {
        return new double[]{
            (minX + maxX) / 2.0,
            (minY + maxY) / 2.0,
            (minZ + maxZ) / 2.0
        };
    }

    /**
     * Volumen de una AABB (en bloques cúbicos).
     */
    public static long volume(double minX, double minY, double minZ,
                               double maxX, double maxY, double maxZ) {
        return (long) (Math.abs(maxX - minX) * Math.abs(maxY - minY) * Math.abs(maxZ - minZ));
    }

    /**
     * ¿Dos AABBs se intersectan?
     */
    public static boolean intersects(double minX1, double minY1, double minZ1,
                                      double maxX1, double maxY1, double maxZ1,
                                      double minX2, double minY2, double minZ2,
                                      double maxX2, double maxY2, double maxZ2) {
        return minX1 <= maxX2 && maxX1 >= minX2
            && minY1 <= maxY2 && maxY1 >= minY2
            && minZ1 <= maxZ2 && maxZ1 >= minZ2;
    }

    /**
     * Formatea coordenadas para mostrar al jugador.
     */
    public static String formatLocation(double x, double y, double z) {
        return String.format("%.0f, %.0f, %.0f", x, y, z);
    }

    /**
     * Formatea tiempo en ms a string legible (Xh Xm Xs).
     */
    public static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0)   return String.format("%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return String.format("%ds", seconds);
    }

    /**
     * Formatea un número grande con separadores de miles.
     */
    public static String formatNumber(long n) {
        return String.format("%,d", n);
    }
}
