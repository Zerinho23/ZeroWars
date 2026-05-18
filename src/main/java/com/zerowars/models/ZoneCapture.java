package com.zerowars.models;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Representa el proceso activo de captura de una zona.
 * Una zona solo puede tener una captura activa a la vez.
 * Ciclo de vida: IDLE → CAPTURING / DEFENDING / CONTESTED → completo/cancelado.
 */
public class ZoneCapture {

    public enum CapturePhase {
        CAPTURING,   // El atacante está avanzando la barra
        DEFENDING,   // El defensor está retrocediendo la barra
        CONTESTED,   // Ambos bandos presentes — barra congelada
        GRACE        // Período de gracia (atacante salió brevemente)
    }

    private final String zoneId;

    // ── Jugadores involucrados ────────────────────────────────────────────────
    private final UUID attackerUUID;
    private final String attackerName;
    private String attackerClanId;

    // Jugadores del mismo clan/equipo del atacante que asisten
    private final Set<UUID> assistants = new HashSet<>();

    // Jugadores defensores presentes en zona
    private final Set<UUID> defenders = new HashSet<>();

    // ── Estado de la captura ──────────────────────────────────────────────────
    private CapturePhase phase;
    private double progress;          // 0.0 – 100.0
    private final long startTime;
    private long phaseStartTime;
    private int requiredTime;         // segundos necesarios al 100%
    private boolean cancelled;
    private boolean completed;

    // ── BossBar ───────────────────────────────────────────────────────────────
    private BossBar bossBar;

    // ── Constructor ───────────────────────────────────────────────────────────
    public ZoneCapture(String zoneId, UUID attackerUUID, String attackerName,
                       double startProgress, int requiredTime) {
        this.zoneId = zoneId;
        this.attackerUUID = attackerUUID;
        this.attackerName = attackerName;
        this.progress = startProgress;
        this.requiredTime = requiredTime;
        this.phase = CapturePhase.CAPTURING;
        this.startTime = System.currentTimeMillis();
        this.phaseStartTime = this.startTime;
        this.cancelled = false;
        this.completed = false;
    }

    // ── Lógica ────────────────────────────────────────────────────────────────

    /**
     * Calcula cuánto progreso se avanza por tick (20 ticks = 1 segundo).
     * progressPerTick = (100.0 / requiredTime) / 20
     */
    public double getProgressPerTick() {
        return (100.0 / requiredTime) / 20.0;
    }

    /** Avanza el progreso en capturing. Devuelve true si llegó al 100%. */
    public boolean advance(double amount) {
        this.progress = Math.min(100.0, this.progress + amount);
        return this.progress >= 100.0;
    }

    /** Retrocede el progreso en defending. Devuelve true si llegó a 0 (defensa exitosa). */
    public boolean retreat(double amount) {
        this.progress = Math.max(0.0, this.progress - amount);
        return this.progress <= 0.0;
    }

    /** Añade un jugador asistente al equipo atacante. */
    public void addAssistant(UUID uuid) { assistants.add(uuid); }

    /** Añade un defensor. */
    public void addDefender(UUID uuid) { defenders.add(uuid); }

    /** Quita un defensor. */
    public void removeDefender(UUID uuid) { defenders.remove(uuid); }

    /** ¿Hay defensores activos? */
    public boolean hasDefenders() { return !defenders.isEmpty(); }

    /** Marca la captura como completada. */
    public void complete() { this.completed = true; }

    /** Cancela la captura. */
    public void cancel() { this.cancelled = true; }

    /** Total de atacantes (atacante principal + asistentes). */
    public int getAttackerCount() { return 1 + assistants.size(); }

    /** Actualiza el BossBar con el estado actual. */
    public void updateBossBar(String title) {
        if (bossBar == null) return;
        bossBar.setTitle(title);
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress / 100.0)));
        bossBar.setColor(switch (phase) {
            case CAPTURING -> BarColor.RED;
            case DEFENDING -> BarColor.GREEN;
            case CONTESTED -> BarColor.YELLOW;
            case GRACE     -> BarColor.WHITE;
        });
    }

    /** Crea el BossBar y lo muestra a un jugador. */
    public void showBossBar(Player player, String title) {
        if (bossBar != null) {
            bossBar.addPlayer(player);
            return;
        }
        // Creado desde el BukkitServer en el manager para evitar dependencia circular
    }

    /** Oculta y destruye el BossBar. */
    public void removeBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getZoneId()               { return zoneId; }
    public UUID getAttackerUUID()           { return attackerUUID; }
    public String getAttackerName()         { return attackerName; }
    public String getAttackerClanId()       { return attackerClanId; }
    public Set<UUID> getAssistants()        { return assistants; }
    public Set<UUID> getDefenders()         { return defenders; }
    public CapturePhase getPhase()          { return phase; }
    public double getProgress()             { return progress; }
    public long getStartTime()              { return startTime; }
    public long getPhaseStartTime()         { return phaseStartTime; }
    public int getRequiredTime()            { return requiredTime; }
    public boolean isCancelled()            { return cancelled; }
    public boolean isCompleted()            { return completed; }
    public BossBar getBossBar()             { return bossBar; }

    public void setAttackerClanId(String c) { this.attackerClanId = c; }
    public void setPhase(CapturePhase p)    { this.phase = p; this.phaseStartTime = System.currentTimeMillis(); }
    public void setProgress(double p)       { this.progress = Math.max(0, Math.min(100, p)); }
    public void setRequiredTime(int t)      { this.requiredTime = t; }
    public void setBossBar(BossBar bar)     { this.bossBar = bar; }
}
