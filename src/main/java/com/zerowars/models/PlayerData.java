package com.zerowars.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Datos persistentes de un jugador en ZeroWars.
 * Se guarda en SQLite y se cachea en memoria mientras el jugador está online.
 */
public class PlayerData {

    private final UUID uuid;
    private String name;

    // ── Estadísticas ─────────────────────────────────────────────────────────
    private int kills;
    private int deaths;
    private int captures;
    private int capturesFailed;
    private long totalDomineTimeMs;   // tiempo total dominando zonas
    private int zonesOwned;           // zonas controladas actualmente
    private double totalMoneyEarned;

    // ── Heat / Wanted ─────────────────────────────────────────────────────────
    private int heatLevel;            // 0 = sin heat, 3 = máximo
    private int heatCaptures;         // capturas que generan heat
    private long heatExpireTime;      // timestamp cuando expira el heat

    // ── Clan ─────────────────────────────────────────────────────────────────
    private String clanId;            // null = sin clan

    // ── Cooldowns de consumibles (id → timestamp expiración en ms) ───────────
    private final Map<String, Long> consumableCooldowns = new HashMap<>();

    // ── Cooldowns de recaptura (zoneId → timestamp expiración en ms) ─────────
    private final Map<String, Long> recaptureCooldowns = new HashMap<>();

    // ── Constructor ──────────────────────────────────────────────────────────
    public PlayerData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    // ── Lógica ───────────────────────────────────────────────────────────────

    /** Verifica si el cooldown de un consumible está activo. */
    public boolean isOnConsumableCooldown(String consumableId) {
        Long expire = consumableCooldowns.get(consumableId);
        return expire != null && System.currentTimeMillis() < expire;
    }

    /** Segundos restantes del cooldown de un consumible (0 si no hay). */
    public long getConsumableCooldownSeconds(String consumableId) {
        Long expire = consumableCooldowns.get(consumableId);
        if (expire == null) return 0;
        long remaining = expire - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 1000) + 1 : 0;
    }

    /** Establece el cooldown de un consumible. */
    public void setConsumableCooldown(String consumableId, long durationSeconds) {
        consumableCooldowns.put(consumableId, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    /** Verifica si el jugador puede recapturar la zona. */
    public boolean isOnRecaptureCooldown(String zoneId) {
        Long expire = recaptureCooldowns.get(zoneId);
        return expire != null && System.currentTimeMillis() < expire;
    }

    /** Segundos restantes del cooldown de recaptura de una zona. */
    public long getRecaptureCooldownSeconds(String zoneId) {
        Long expire = recaptureCooldowns.get(zoneId);
        if (expire == null) return 0;
        long remaining = expire - System.currentTimeMillis();
        return remaining > 0 ? (remaining / 1000) + 1 : 0;
    }

    /** Establece cooldown de recaptura de una zona. */
    public void setRecaptureCooldown(String zoneId, long durationSeconds) {
        recaptureCooldowns.put(zoneId, System.currentTimeMillis() + (durationSeconds * 1000L));
    }

    /** Incrementa kills y actualiza K/D ratio internamente. */
    public void addKill() { this.kills++; }

    /** Incrementa deaths. */
    public void addDeath() { this.deaths++; }

    /** Registra una captura exitosa. */
    public void addCapture() {
        this.captures++;
        this.heatCaptures++;
    }

    /** K/D ratio calculado. */
    public double getKDRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public UUID getUuid()                         { return uuid; }
    public String getName()                       { return name; }
    public int getKills()                         { return kills; }
    public int getDeaths()                        { return deaths; }
    public int getCaptures()                      { return captures; }
    public int getCapturesFailed()                { return capturesFailed; }
    public long getTotalDomineTimeMs()            { return totalDomineTimeMs; }
    public int getZonesOwned()                    { return zonesOwned; }
    public double getTotalMoneyEarned()           { return totalMoneyEarned; }
    public int getHeatLevel()                     { return heatLevel; }
    public int getHeatCaptures()                  { return heatCaptures; }
    public long getHeatExpireTime()               { return heatExpireTime; }
    public String getClanId()                     { return clanId; }
    public Map<String, Long> getConsumableCooldowns() { return consumableCooldowns; }
    public Map<String, Long> getRecaptureCooldowns()  { return recaptureCooldowns; }

    public void setName(String n)                 { this.name = n; }
    public void setKills(int k)                   { this.kills = k; }
    public void setDeaths(int d)                  { this.deaths = d; }
    public void setCaptures(int c)                { this.captures = c; }
    public void setCapturesFailed(int c)          { this.capturesFailed = c; }
    public void addDomineTime(long ms)            { this.totalDomineTimeMs += ms; }
    public void setTotalDomineTimeMs(long t)      { this.totalDomineTimeMs = t; }
    public void setZonesOwned(int z)              { this.zonesOwned = z; }
    public void addMoneyEarned(double m)          { this.totalMoneyEarned += m; }
    public void setTotalMoneyEarned(double m)     { this.totalMoneyEarned = m; }
    public void setHeatLevel(int h)               { this.heatLevel = h; }
    public void setHeatCaptures(int h)            { this.heatCaptures = h; }
    public void resetHeat()                       { this.heatLevel = 0; this.heatCaptures = 0; }
    public void setHeatExpireTime(long t)         { this.heatExpireTime = t; }
    public void setClanId(String c)               { this.clanId = c; }
    public void addCapturesFailed()               { this.capturesFailed++; }
}
