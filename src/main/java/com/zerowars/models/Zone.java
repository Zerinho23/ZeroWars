package com.zerowars.models;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Modelo de una Zona PvP.
 * Representa un territorio capturable con su estado completo.
 */
public class Zone {

    // ── Tipos de zona ────────────────────────────────────────────────────────
    public enum ZoneType {
        MINE,       // Mina — produce recursos pasivos
        ROOM,       // Room — área de combate cerrada
        RARE,       // Zona rara — alta recompensa, difícil de capturar
        EVENT,      // Evento — solo activa durante eventos
        SPECIAL     // Especial — configuración personalizada
    }

    // ── Estado de la zona ────────────────────────────────────────────────────
    public enum ZoneState {
        NEUTRAL,    // Sin dueño
        OWNED,      // Con dueño, estable
        CAPTURING,  // En proceso de captura
        CONTESTED   // En disputa (atacante y defensor presentes)
    }

    // ── Campos principales ───────────────────────────────────────────────────
    private final String id;
    private String displayName;
    private String rawName;           // MiniMessage format
    private ZoneType type;
    private ZoneState state;

    // Área de la zona
    private String worldName;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;

    // Propiedad
    private UUID ownerUUID;           // null = neutral
    private String ownerName;
    private String ownerClanId;       // null = sin clan

    // Progresión
    private int level;
    private final int maxLevel;
    private double captureProgress;  // 0.0 – 100.0
    private int captureTime;         // segundos requeridos para capturar
    private long lastCaptureTime;    // timestamp unix en ms
    private long totalDomineTime;    // ms acumulados como dueño

    // Recompensas
    private List<String> rewardIds;
    private long lastRewardTime;

    // Estado operacional
    private boolean enabled;
    private boolean eventOnly;
    private boolean protectedZone;

    // ── Constructor ──────────────────────────────────────────────────────────
    public Zone(String id, String rawName, ZoneType type,
                String worldName,
                double minX, double minY, double minZ,
                double maxX, double maxY, double maxZ,
                int level, int maxLevel, int captureTime) {
        this.id = id;
        this.rawName = rawName;
        this.displayName = rawName;
        this.type = type;
        this.state = ZoneState.NEUTRAL;
        this.worldName = worldName;
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
        this.level = level;
        this.maxLevel = maxLevel;
        this.captureProgress = 0.0;
        this.captureTime = captureTime;
        this.rewardIds = new ArrayList<>();
        this.enabled = true;
        this.eventOnly = false;
        this.protectedZone = true;
    }

    // ── Lógica de zona ───────────────────────────────────────────────────────

    /**
     * Comprueba si una localización está dentro del área de esta zona.
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) return false;
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    /**
     * Centro geométrico de la zona.
     */
    public Location getCenter(World world) {
        return new Location(world,
            (minX + maxX) / 2.0,
            (minY + maxY) / 2.0,
            (minZ + maxZ) / 2.0);
    }

    /**
     * Sube el nivel de la zona si no está al máximo.
     * @return true si subió de nivel
     */
    public boolean levelUp() {
        if (level >= maxLevel) return false;
        level++;
        return true;
    }

    /**
     * Resetea la zona a estado neutral.
     */
    public void reset() {
        this.state = ZoneState.NEUTRAL;
        this.captureProgress = 0.0;
        this.ownerUUID = null;
        this.ownerName = null;
        this.ownerClanId = null;
    }

    /**
     * Comprueba si la zona tiene dueño.
     */
    public boolean isOwned() {
        return ownerUUID != null;
    }

    /**
     * Comprueba si el UUID dado es el dueño actual.
     */
    public boolean isOwnedBy(UUID uuid) {
        return ownerUUID != null && ownerUUID.equals(uuid);
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getId()                     { return id; }
    public String getRawName()                { return rawName; }
    public String getDisplayName()            { return displayName; }
    public ZoneType getType()                 { return type; }
    public ZoneState getState()               { return state; }
    public String getWorldName()              { return worldName; }
    public double getMinX()                   { return minX; }
    public double getMinY()                   { return minY; }
    public double getMinZ()                   { return minZ; }
    public double getMaxX()                   { return maxX; }
    public double getMaxY()                   { return maxY; }
    public double getMaxZ()                   { return maxZ; }
    public UUID getOwnerUUID()                { return ownerUUID; }
    public String getOwnerName()              { return ownerName; }
    public String getOwnerClanId()            { return ownerClanId; }
    public int getLevel()                     { return level; }
    public int getMaxLevel()                  { return maxLevel; }
    public double getCaptureProgress()        { return captureProgress; }
    public int getCaptureTime()               { return captureTime; }
    public long getLastCaptureTime()          { return lastCaptureTime; }
    public long getTotalDomineTime()          { return totalDomineTime; }
    public List<String> getRewardIds()        { return rewardIds; }
    public long getLastRewardTime()           { return lastRewardTime; }
    public boolean isEnabled()                { return enabled; }
    public boolean isEventOnly()              { return eventOnly; }
    public boolean isProtected()              { return protectedZone; }

    public void setDisplayName(String n)      { this.displayName = n; }
    public void setRawName(String n)          { this.rawName = n; }
    public void setType(ZoneType t)           { this.type = t; }
    public void setState(ZoneState s)         { this.state = s; }
    public void setWorldName(String w)        { this.worldName = w; }
    public void setOwnerUUID(UUID u)          { this.ownerUUID = u; }
    public void setOwnerName(String n)        { this.ownerName = n; }
    public void setOwnerClanId(String c)      { this.ownerClanId = c; }
    public void setLevel(int l)               { this.level = l; }
    public void setCaptureProgress(double p)  { this.captureProgress = Math.max(0, Math.min(100, p)); }
    public void setCaptureTime(int t)         { this.captureTime = t; }
    public void setLastCaptureTime(long t)    { this.lastCaptureTime = t; }
    public void addDomineTime(long ms)        { this.totalDomineTime += ms; }
    public void setTotalDomineTime(long t)    { this.totalDomineTime = t; }
    public void setRewardIds(List<String> r)  { this.rewardIds = r; }
    public void setLastRewardTime(long t)     { this.lastRewardTime = t; }
    public void setEnabled(boolean e)         { this.enabled = e; }
    public void setEventOnly(boolean e)       { this.eventOnly = e; }
    public void setProtected(boolean p)       { this.protectedZone = p; }
}
