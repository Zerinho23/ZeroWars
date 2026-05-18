package com.zerowars.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de un evento PvP automático o manual.
 * Los eventos modifican el comportamiento global de zonas temporalmente.
 */
public class ZoneEvent {

    public enum EventState {
        IDLE,       // No activo
        ACTIVE,     // Corriendo
        ENDING      // Terminando (animación de cierre)
    }

    private final String id;
    private String name;             // MiniMessage
    private String description;
    private int durationMinutes;
    private double rewardMultiplier;
    private EventState state;

    // ── Zonas afectadas ────────────────────────────────────────────────────────
    private List<String> affectedZoneIds;  // vacío = todas las zonas
    private boolean activateEventZone;

    // ── Lluvia de recompensas (tipo reward_rain) ───────────────────────────────
    private int rainIntervalSeconds;
    private List<RainItem> rainItems;

    // ── Comandos on-start / on-end ────────────────────────────────────────────
    private String startBroadcast;
    private String endBroadcast;
    private String startSound;
    private List<String> startCommands;
    private List<String> endCommands;

    // ── Estado de ejecución ───────────────────────────────────────────────────
    private long startTime;      // timestamp unix en ms
    private long endTime;        // timestamp unix en ms cuando termina

    public record RainItem(String material, int amount, double chance) {}

    // ── Constructor ───────────────────────────────────────────────────────────
    public ZoneEvent(String id) {
        this.id = id;
        this.state = EventState.IDLE;
        this.affectedZoneIds = new ArrayList<>();
        this.rainItems = new ArrayList<>();
        this.startCommands = new ArrayList<>();
        this.endCommands = new ArrayList<>();
        this.rewardMultiplier = 1.0;
    }

    // ── Lógica ────────────────────────────────────────────────────────────────

    /** Marca el evento como activo y calcula el tiempo de fin. */
    public void start() {
        this.state = EventState.ACTIVE;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + ((long) durationMinutes * 60 * 1000);
    }

    /** Marca el evento como finalizado. */
    public void end() { this.state = EventState.IDLE; }

    /** ¿El evento ha expirado por tiempo? */
    public boolean isExpired() {
        return state == EventState.ACTIVE && System.currentTimeMillis() >= endTime;
    }

    /** Minutos restantes del evento (redondeados hacia arriba). */
    public long getRemainingMinutes() {
        if (state != EventState.ACTIVE) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return (long) Math.ceil(remaining / 60_000.0);
    }

    /** ¿El evento afecta a esta zona? */
    public boolean affectsZone(String zoneId) {
        return affectedZoneIds.isEmpty() || affectedZoneIds.contains(zoneId);
    }

    public boolean isActive() { return state == EventState.ACTIVE; }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public String getId()                           { return id; }
    public String getName()                         { return name; }
    public String getDescription()                  { return description; }
    public int getDurationMinutes()                 { return durationMinutes; }
    public double getRewardMultiplier()             { return rewardMultiplier; }
    public EventState getState()                    { return state; }
    public List<String> getAffectedZoneIds()        { return affectedZoneIds; }
    public boolean isActivateEventZone()            { return activateEventZone; }
    public int getRainIntervalSeconds()             { return rainIntervalSeconds; }
    public List<RainItem> getRainItems()            { return rainItems; }
    public String getStartBroadcast()               { return startBroadcast; }
    public String getEndBroadcast()                 { return endBroadcast; }
    public String getStartSound()                   { return startSound; }
    public List<String> getStartCommands()          { return startCommands; }
    public List<String> getEndCommands()            { return endCommands; }
    public long getStartTime()                      { return startTime; }
    public long getEndTime()                        { return endTime; }

    public void setName(String n)                   { this.name = n; }
    public void setDescription(String d)            { this.description = d; }
    public void setDurationMinutes(int d)           { this.durationMinutes = d; }
    public void setRewardMultiplier(double r)       { this.rewardMultiplier = r; }
    public void setState(EventState s)              { this.state = s; }
    public void setAffectedZoneIds(List<String> a)  { this.affectedZoneIds = a; }
    public void setActivateEventZone(boolean a)     { this.activateEventZone = a; }
    public void setRainIntervalSeconds(int r)       { this.rainIntervalSeconds = r; }
    public void setRainItems(List<RainItem> r)      { this.rainItems = r; }
    public void setStartBroadcast(String s)         { this.startBroadcast = s; }
    public void setEndBroadcast(String s)           { this.endBroadcast = s; }
    public void setStartSound(String s)             { this.startSound = s; }
    public void setStartCommands(List<String> c)    { this.startCommands = c; }
    public void setEndCommands(List<String> c)      { this.endCommands = c; }
}
