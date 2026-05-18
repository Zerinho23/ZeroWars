package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.models.ZoneCapture;
import com.zerowars.utils.EffectUtil;
import com.zerowars.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor del sistema de captura de zonas.
 * Tick loop cada 1 segundo (20 ticks) procesa todas las capturas activas.
 * Lógica: CAPTURING avanza, CONTESTED se pausa, DEFENDING retrocede.
 */
public class CaptureManager {

    private final ZeroWars plugin;

    // zoneId → ZoneCapture activa
    private final Map<String, ZoneCapture> activeCaptures = new ConcurrentHashMap<>();

    // playerUUID → zoneId
    private final Map<UUID, String> playerCapturing = new ConcurrentHashMap<>();

    private BukkitTask updateTask;

    public CaptureManager(ZeroWars plugin) {
        this.plugin = plugin;
    }

    // ── Tick loop ─────────────────────────────────────────────────────────────

    public void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        if (activeCaptures.isEmpty()) return;

        // Recoger IDs a remover para no modificar el mapa mientras iteramos
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, ZoneCapture> entry : activeCaptures.entrySet()) {
            String zoneId = entry.getKey();
            ZoneCapture capture = entry.getValue();

            if (capture.isCancelled() || capture.isCompleted()) {
                toRemove.add(zoneId);
                continue;
            }

            Zone zone = plugin.getZoneManager().getZone(zoneId).orElse(null);
            if (zone == null) {
                toRemove.add(zoneId);
                continue;
            }

            processCapture(zone, capture);
        }

        toRemove.forEach(id -> activeCaptures.remove(id));
    }

    private void processCapture(Zone zone, ZoneCapture capture) {
        Player attacker = Bukkit.getPlayer(capture.getAttackerUUID());

        // Verificar que el atacante siga online y en la zona
        // getPlayerZoneId puede devolver null — comparar con Objects.equals para evitar NPE
        String attackerZoneId = plugin.getZoneManager().getPlayerZoneId(capture.getAttackerUUID());
        if (attacker == null || !attacker.isOnline()
                || !zone.getId().equals(attackerZoneId)) {
            cancelCapture(zone.getId(), "capture.cancelled");
            return;
        }

        // Detectar defensores
        List<UUID> inZone = plugin.getZoneManager().getPlayersInZone(zone.getId());
        boolean hasDefenders = inZone.stream()
                .filter(uuid -> !uuid.equals(capture.getAttackerUUID())
                        && !capture.getAssistants().contains(uuid))
                .anyMatch(uuid -> {
                    if (zone.getOwnerUUID() != null && uuid.equals(zone.getOwnerUUID())) return true;
                    if (zone.getOwnerClanId() != null) {
                        var data = plugin.getRankingManager().getCachedPlayerData(uuid);
                        return data != null && zone.getOwnerClanId().equals(data.getClanId());
                    }
                    return false;
                });

        if (hasDefenders && plugin.getConfigManager().isPauseOnContest()) {
            zone.setState(Zone.ZoneState.CONTESTED);
            capture.setPhase(ZoneCapture.CapturePhase.CONTESTED);
            sendContestActionBar(attacker, zone);
        } else {
            zone.setState(Zone.ZoneState.CAPTURING);
            capture.setPhase(ZoneCapture.CapturePhase.CAPTURING);

            int attackerCount = capture.getAttackerCount();
            double extra = plugin.getConfigManager().getCaptureTimeReductionPerPlayer()
                    * (attackerCount - 1);
            int effectiveTime = Math.max(
                    plugin.getConfigManager().getCaptureMinTime(),
                    capture.getRequiredTime() - (int) extra);

            double progressPerTick = 100.0 / effectiveTime;
            boolean done = capture.advance(progressPerTick);

            updateBossBar(zone, capture, attacker);
            sendCaptureActionBar(attacker, zone, capture);

            if (done) {
                completeCapture(zone, capture, attacker);
                return;
            }
        }

        zone.setCaptureProgress(capture.getProgress());
    }

    // ── Captura completada ────────────────────────────────────────────────────

    private void completeCapture(Zone zone, ZoneCapture capture, Player attacker) {
        zone.setOwnerUUID(attacker.getUniqueId());
        zone.setOwnerName(attacker.getName());
        zone.setState(Zone.ZoneState.OWNED);
        zone.setCaptureProgress(100.0);
        zone.setLastCaptureTime(System.currentTimeMillis());

        var data = plugin.getRankingManager().getCachedPlayerData(attacker.getUniqueId());
        if (data != null && data.getClanId() != null) {
            zone.setOwnerClanId(data.getClanId());
        }

        capture.complete();
        activeCaptures.remove(zone.getId());
        playerCapturing.remove(attacker.getUniqueId());

        EffectUtil.playCaptureFanfare(attacker, zone);

        attacker.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("capture.completed",
                        "%player%", attacker.getName(), "%zone%", zone.getDisplayName())));

        Bukkit.broadcast(MessageUtil.parse(
                plugin.getConfigManager().getMessage("capture.completed-broadcast",
                        "%player%", attacker.getName(), "%zone%", zone.getDisplayName())));

        if (data != null) data.addCapture();

        plugin.getHeatManager().onZoneCaptured(attacker, zone);
        plugin.getZoneManager().saveAsync(zone);

        capture.removeBossBar();
    }

    // ── Iniciar / Cancelar ────────────────────────────────────────────────────

    public void startCapture(Player player, Zone zone) {
        String zoneId = zone.getId();

        if (activeCaptures.containsKey(zoneId)) {
            ZoneCapture existing = activeCaptures.get(zoneId);
            if (!existing.getAttackerUUID().equals(player.getUniqueId())) {
                existing.addAssistant(player.getUniqueId());
            }
            return;
        }

        ZoneCapture capture = new ZoneCapture(
                zoneId,
                player.getUniqueId(),
                player.getName(),
                zone.getCaptureProgress(),
                zone.getCaptureTime()
        );

        var bossBar = Bukkit.createBossBar(
                "⚔ Capturando " + zone.getDisplayName(),
                BarColor.RED, BarStyle.SEGMENTED_10);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);
        capture.setBossBar(bossBar);

        activeCaptures.put(zoneId, capture);
        playerCapturing.put(player.getUniqueId(), zoneId);
        zone.setState(Zone.ZoneState.CAPTURING);

        player.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("capture.started",
                        "%zone%", zone.getDisplayName())));

        EffectUtil.playCaptureStartEffect(player);
    }

    public void cancelCapture(String zoneId, String messageKey) {
        ZoneCapture capture = activeCaptures.remove(zoneId);
        if (capture == null) return;

        capture.cancel();
        playerCapturing.remove(capture.getAttackerUUID());
        capture.removeBossBar();

        Zone zone = plugin.getZoneManager().getZone(zoneId).orElse(null);
        if (zone != null) {
            zone.setState(zone.isOwned() ? Zone.ZoneState.OWNED : Zone.ZoneState.NEUTRAL);
            zone.setCaptureProgress(0);
        }

        Player attacker = Bukkit.getPlayer(capture.getAttackerUUID());
        if (attacker != null && attacker.isOnline()) {
            attacker.sendMessage(MessageUtil.parse(
                    plugin.getConfigManager().getMessage(messageKey,
                            "%zone%", zone != null ? zone.getDisplayName() : zoneId)));
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void updateBossBar(Zone zone, ZoneCapture capture, Player player) {
        if (capture.getBossBar() == null) return;
        String title = switch (capture.getPhase()) {
            case CAPTURING -> "⚔ Capturando " + zone.getDisplayName()
                    + " — " + String.format("%.0f", capture.getProgress()) + "%";
            case DEFENDING -> "🛡 Defendiendo " + zone.getDisplayName();
            case CONTESTED -> "⚠ EN DISPUTA — " + zone.getDisplayName();
            default        -> zone.getDisplayName();
        };
        capture.updateBossBar(title);
    }

    private void sendCaptureActionBar(Player player, Zone zone, ZoneCapture capture) {
        int remaining = (int) Math.ceil(
                zone.getCaptureTime() * (1.0 - capture.getProgress() / 100.0));
        player.sendActionBar(MessageUtil.parse(
                plugin.getConfigManager().getMessage("capture.actionbar-capturing",
                        "%zone%", zone.getDisplayName(),
                        "%time%", String.valueOf(remaining))));
    }

    private void sendContestActionBar(Player player, Zone zone) {
        player.sendActionBar(MessageUtil.parse(
                plugin.getConfigManager().getMessage("capture.actionbar-contested")));
    }

    // ── Shutdown ──────────────────────────────────────────────────────────────

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        new HashSet<>(activeCaptures.keySet())
                .forEach(id -> cancelCapture(id, "capture.cancelled"));
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isCapturing(String zoneId)        { return activeCaptures.containsKey(zoneId); }
    public boolean isPlayerCapturing(UUID uuid)      { return playerCapturing.containsKey(uuid); }
    public ZoneCapture getCapture(String zoneId)     { return activeCaptures.get(zoneId); }
    public Map<String, ZoneCapture> getAllCaptures()  { return Collections.unmodifiableMap(activeCaptures); }
    public int getActiveCaptureCount()               { return activeCaptures.size(); }
}
