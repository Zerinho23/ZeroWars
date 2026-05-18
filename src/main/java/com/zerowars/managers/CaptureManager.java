package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;
import com.zerowars.models.ZoneCapture;
import com.zerowars.utils.EffectUtil;
import com.zerowars.utils.MessageUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor del sistema de captura de zonas.
 * Tick loop cada 1 segundo (20 ticks) procesa todas las capturas activas.
 *
 * v1.2.0: completeCapture() invoca RewardManager.giveOnCaptureReward()
 *         y ZoneManager.tryLevelUp() al completar la captura.
 * v1.1.3: BossBar migrado a Adventure API (MiniMessage correcto).
 */
public class CaptureManager {

    private final ZeroWars plugin;
    private final Map<String, ZoneCapture> activeCaptures = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerCapturing = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public CaptureManager(ZeroWars plugin) { this.plugin = plugin; }

    // -- Tick -----------------------------------------------------------------

    public void startUpdateTask() {
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        if (activeCaptures.isEmpty()) return;
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, ZoneCapture> entry : activeCaptures.entrySet()) {
            ZoneCapture capture = entry.getValue();
            if (capture.isCancelled() || capture.isCompleted()) { toRemove.add(entry.getKey()); continue; }
            Zone zone = plugin.getZoneManager().getZone(entry.getKey()).orElse(null);
            if (zone == null) { toRemove.add(entry.getKey()); continue; }
            processCapture(zone, capture);
        }
        toRemove.forEach(id -> activeCaptures.remove(id));
    }

    private void processCapture(Zone zone, ZoneCapture capture) {
        Player attacker = Bukkit.getPlayer(capture.getAttackerUUID());
        String attackerZoneId = plugin.getZoneManager().getPlayerZoneId(capture.getAttackerUUID());
        if (attacker == null || !attacker.isOnline() || !zone.getId().equals(attackerZoneId)) {
            cancelCapture(zone.getId(), "capture.cancelled"); return;
        }
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
            updateBossBar(zone, capture);
            sendContestActionBar(attacker, zone);
        } else {
            zone.setState(Zone.ZoneState.CAPTURING);
            capture.setPhase(ZoneCapture.CapturePhase.CAPTURING);
            int extra = (int)(plugin.getConfigManager().getCaptureTimeReductionPerPlayer()
                    * (capture.getAttackerCount() - 1));
            int effectiveTime = Math.max(plugin.getConfigManager().getCaptureMinTime(),
                    capture.getRequiredTime() - extra);
            boolean done = capture.advance(100.0 / effectiveTime);
            updateBossBar(zone, capture);
            sendCaptureActionBar(attacker, zone, capture);
            if (done) { completeCapture(zone, capture, attacker); return; }
        }
        zone.setCaptureProgress(capture.getProgress());
    }

    // -- Captura completada ---------------------------------------------------

    private void completeCapture(Zone zone, ZoneCapture capture, Player attacker) {
        zone.setOwnerUUID(attacker.getUniqueId());
        zone.setOwnerName(attacker.getName());
        zone.setState(Zone.ZoneState.OWNED);
        zone.setCaptureProgress(100.0);
        zone.setLastCaptureTime(System.currentTimeMillis());

        var data = plugin.getRankingManager().getCachedPlayerData(attacker.getUniqueId());
        if (data != null && data.getClanId() != null) zone.setOwnerClanId(data.getClanId());

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

        // v1.2.0: recompensas de captura + verificar level-up
        plugin.getRewardManager().giveOnCaptureReward(attacker, zone);
        plugin.getZoneManager().tryLevelUp(zone, attacker);

        plugin.getZoneManager().saveAsync(zone);
        capture.removeBossBar();
    }

    // -- Iniciar / Cancelar --------------------------------------------------

    public void startCapture(Player player, Zone zone) {
        String zoneId = zone.getId();
        if (activeCaptures.containsKey(zoneId)) {
            ZoneCapture existing = activeCaptures.get(zoneId);
            if (!existing.getAttackerUUID().equals(player.getUniqueId())) {
                existing.addAssistant(player.getUniqueId());
                existing.showBossBarTo(player);
            }
            return;
        }
        ZoneCapture capture = new ZoneCapture(zoneId, player.getUniqueId(), player.getName(),
                zone.getCaptureProgress(), zone.getCaptureTime());
        Component title = MessageUtil.parse("Capturando " + zone.getDisplayName());
        BossBar bossBar = BossBar.bossBar(title, 0f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
        capture.setBossBar(bossBar);
        capture.showBossBarTo(player);
        activeCaptures.put(zoneId, capture);
        playerCapturing.put(player.getUniqueId(), zoneId);
        zone.setState(Zone.ZoneState.CAPTURING);
        player.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("capture.started", "%zone%", zone.getDisplayName())));
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
            attacker.sendMessage(MessageUtil.parse(plugin.getConfigManager().getMessage(messageKey,
                    "%zone%", zone != null ? zone.getDisplayName() : zoneId)));
        }
    }

    // -- UI ------------------------------------------------------------------

    private void updateBossBar(Zone zone, ZoneCapture capture) {
        if (capture.getBossBar() == null) return;
        Component title = switch (capture.getPhase()) {
            case CAPTURING -> MessageUtil.parse("<gradient:#ff4400:#ffaa00>Capturando </gradient>"
                    + zone.getDisplayName() + " <white>" + String.format("%.0f", capture.getProgress()) + "%");
            case DEFENDING -> MessageUtil.parse("<green>Defendiendo " + zone.getDisplayName());
            case CONTESTED -> MessageUtil.parse("<yellow>EN DISPUTA</yellow> — " + zone.getDisplayName());
            default        -> MessageUtil.parse(zone.getDisplayName());
        };
        capture.updateBossBar(title);
    }

    private void sendCaptureActionBar(Player player, Zone zone, ZoneCapture capture) {
        int remaining = (int) Math.ceil(zone.getCaptureTime() * (1.0 - capture.getProgress() / 100.0));
        player.sendActionBar(MessageUtil.parse(plugin.getConfigManager().getMessage(
                "capture.actionbar-capturing", "%zone%", zone.getDisplayName(),
                "%time%", String.valueOf(remaining))));
    }

    private void sendContestActionBar(Player player, Zone zone) {
        player.sendActionBar(MessageUtil.parse(
                plugin.getConfigManager().getMessage("capture.actionbar-contested")));
    }

    // -- Shutdown ------------------------------------------------------------

    public void shutdown() {
        if (updateTask != null) updateTask.cancel();
        new HashSet<>(activeCaptures.keySet()).forEach(id -> cancelCapture(id, "capture.cancelled"));
    }

    // -- Getters -------------------------------------------------------------

    public boolean isCapturing(String zoneId)        { return activeCaptures.containsKey(zoneId); }
    public boolean isPlayerCapturing(UUID uuid)      { return playerCapturing.containsKey(uuid); }
    public ZoneCapture getCapture(String zoneId)     { return activeCaptures.get(zoneId); }
    public Map<String, ZoneCapture> getAllCaptures()  { return Collections.unmodifiableMap(activeCaptures); }
    public int getActiveCaptureCount()               { return activeCaptures.size(); }
}