package com.zerowars.models;

  import net.kyori.adventure.bossbar.BossBar;
  import net.kyori.adventure.text.Component;
  import org.bukkit.Bukkit;
  import org.bukkit.entity.Player;

  import java.util.HashSet;
  import java.util.Set;
  import java.util.UUID;

  /**
   * Representa el proceso activo de captura de una zona.
   * Una zona solo puede tener una captura activa a la vez.
   * Ciclo de vida: IDLE -> CAPTURING / DEFENDING / CONTESTED -> completo/cancelado.
   *
   * Cambios v1.1.3:
   *  - BossBar migrado a Adventure API (net.kyori.adventure.bossbar.BossBar)
   *    para que los gradientes y colores MiniMessage del nombre de zona
   *    se rendericen correctamente en el BossBar.
   */
  public class ZoneCapture {

      public enum CapturePhase {
          CAPTURING,   // El atacante esta avanzando la barra
          DEFENDING,   // El defensor esta retrocediendo la barra
          CONTESTED,   // Ambos bandos presentes -- barra congelada
          GRACE        // Periodo de gracia (atacante salio brevemente)
      }

      private final String zoneId;

      // -- Jugadores involucrados ------------------------------------------------
      private final UUID attackerUUID;
      private final String attackerName;
      private String attackerClanId;

      private final Set<UUID> assistants = new HashSet<>();
      private final Set<UUID> defenders  = new HashSet<>();

      // -- Estado de la captura -------------------------------------------------
      private CapturePhase phase;
      private double progress;
      private final long startTime;
      private long phaseStartTime;
      private int requiredTime;
      private boolean cancelled;
      private boolean completed;

      // -- BossBar (Adventure API) ----------------------------------------------
      // Se usa Adventure BossBar en lugar del legacy Bukkit BossBar para que
      // los componentes MiniMessage (gradientes, colores hex) se rendericen.
      private BossBar bossBar;
      private final Set<UUID> bossBarPlayers = new HashSet<>();

      // -- Constructor ----------------------------------------------------------
      public ZoneCapture(String zoneId, UUID attackerUUID, String attackerName,
                         double startProgress, int requiredTime) {
          this.zoneId        = zoneId;
          this.attackerUUID  = attackerUUID;
          this.attackerName  = attackerName;
          this.progress      = startProgress;
          this.requiredTime  = requiredTime;
          this.phase         = CapturePhase.CAPTURING;
          this.startTime     = System.currentTimeMillis();
          this.phaseStartTime = this.startTime;
          this.cancelled     = false;
          this.completed     = false;
      }

      // -- Logica ---------------------------------------------------------------

      public double getProgressPerTick() {
          return (100.0 / requiredTime) / 20.0;
      }

      public boolean advance(double amount) {
          this.progress = Math.min(100.0, this.progress + amount);
          return this.progress >= 100.0;
      }

      public boolean retreat(double amount) {
          this.progress = Math.max(0.0, this.progress - amount);
          return this.progress <= 0.0;
      }

      public void addAssistant(UUID uuid) { assistants.add(uuid); }
      public void addDefender(UUID uuid)  { defenders.add(uuid); }
      public void removeDefender(UUID uuid) { defenders.remove(uuid); }
      public boolean hasDefenders() { return !defenders.isEmpty(); }
      public void complete() { this.completed = true; }
      public void cancel()   { this.cancelled = true; }
      public int getAttackerCount() { return 1 + assistants.size(); }

      // -- BossBar API ----------------------------------------------------------

      /**
       * Muestra el BossBar a un jugador y lo registra para poder ocultarlo despues.
       * Solo funciona si el BossBar ya fue asignado via setBossBar().
       */
      public void showBossBarTo(Player player) {
          if (bossBar == null || player == null) return;
          player.showBossBar(bossBar);
          bossBarPlayers.add(player.getUniqueId());
      }

      /**
       * Actualiza el titulo y progreso del BossBar.
       * El titulo es un Component de Adventure -- acepta gradientes, hex, etc.
       */
      public void updateBossBar(Component title) {
          if (bossBar == null) return;
          bossBar.name(title);
          bossBar.progress((float) Math.max(0.0, Math.min(1.0, progress / 100.0)));
          bossBar.color(switch (phase) {
              case CAPTURING -> BossBar.Color.RED;
              case DEFENDING -> BossBar.Color.GREEN;
              case CONTESTED -> BossBar.Color.YELLOW;
              case GRACE     -> BossBar.Color.WHITE;
          });
      }

      /**
       * Oculta el BossBar a todos los jugadores que lo estaban viendo y lo destruye.
       */
      public void removeBossBar() {
          if (bossBar == null) return;
          for (UUID uuid : bossBarPlayers) {
              Player p = Bukkit.getPlayer(uuid);
              if (p != null && p.isOnline()) p.hideBossBar(bossBar);
          }
          bossBarPlayers.clear();
          bossBar = null;
      }

      // -- Getters / Setters ----------------------------------------------------

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