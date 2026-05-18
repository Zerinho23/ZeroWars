package com.zerowars.managers;

import com.zerowars.ZeroWars;
import com.zerowars.models.PlayerData;
import com.zerowars.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Gestor de clanes interno del plugin.
 * Soporta creación, disolución, invitaciones y permisos de clan.
 * Para integraciones externas (SimpleClans, etc.) se extiende aquí.
 */
public class ClanManager {

    private final ZeroWars plugin;

    // clanId → nombre del clan
    private final Map<String, String> clanNames = new ConcurrentHashMap<>();

    // clanId → UUID del líder
    private final Map<String, UUID> clanLeaders = new ConcurrentHashMap<>();

    // clanId → Set de miembros UUID
    private final Map<String, Set<UUID>> clanMembers = new ConcurrentHashMap<>();

    // Invitaciones pendientes: invitado UUID → clanId
    private final Map<UUID, String> pendingInvites = new ConcurrentHashMap<>();

    public ClanManager(ZeroWars plugin) {
        this.plugin = plugin;
        if (plugin.getConfigManager().config().getBoolean("clans.enabled", true)) {
            loadClans();
        }
    }

    // ── Carga ────────────────────────────────────────────────────────────────

    private void loadClans() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "SELECT id, name, leader_uuid FROM clans";
            String memberSql = "SELECT player_uuid, clan_id FROM clan_members";
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String id   = rs.getString("id");
                        String name = rs.getString("name");
                        UUID leader = UUID.fromString(rs.getString("leader_uuid"));
                        clanNames.put(id, name);
                        clanLeaders.put(id, leader);
                        clanMembers.put(id, new HashSet<>());
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(memberSql);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        UUID   playerUuid = UUID.fromString(rs.getString("player_uuid"));
                        String clanId     = rs.getString("clan_id");
                        clanMembers.computeIfAbsent(clanId, k -> new HashSet<>()).add(playerUuid);
                    }
                }
                plugin.getLogger().info("Clanes cargados: " + clanNames.size());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error cargando clanes", e);
            }
        });
    }

    // ── CRUD de clan ──────────────────────────────────────────────────────────

    public boolean createClan(Player leader, String name) {
        String existingClan = getClanOfPlayer(leader.getUniqueId());
        if (existingClan != null) {
            leader.sendMessage(MessageUtil.parse(
                    plugin.getConfigManager().getMessage("clan.already-in-clan")));
            return false;
        }

        String clanId = UUID.randomUUID().toString().substring(0, 8);
        clanNames.put(clanId, name);
        clanLeaders.put(clanId, leader.getUniqueId());
        Set<UUID> members = new HashSet<>();
        members.add(leader.getUniqueId());
        clanMembers.put(clanId, members);

        // Actualizar PlayerData del líder
        PlayerData data = plugin.getRankingManager().getCachedPlayerData(leader.getUniqueId());
        if (data != null) data.setClanId(clanId);

        // Persistir async
        String finalClanId = clanId;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO clans (id, name, leader_uuid, created_at) VALUES (?,?,?,?)";
            String memberSql = "INSERT OR IGNORE INTO clan_members (player_uuid, clan_id, role, joined_at) VALUES (?,?,?,?)";
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, finalClanId);
                    ps.setString(2, name);
                    ps.setString(3, leader.getUniqueId().toString());
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(memberSql)) {
                    ps.setString(1, leader.getUniqueId().toString());
                    ps.setString(2, finalClanId);
                    ps.setString(3, "LEADER");
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error creando clan", e);
            }
        });

        leader.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("clan.created", "%clan%", name)));
        return true;
    }

    public boolean disbandClan(Player leader) {
        String clanId = getClanOfPlayer(leader.getUniqueId());
        if (clanId == null) {
            leader.sendMessage(MessageUtil.parse(
                    plugin.getConfigManager().getMessage("clan.not-in-clan")));
            return false;
        }
        if (!leader.getUniqueId().equals(clanLeaders.get(clanId))) {
            leader.sendMessage(MessageUtil.parse("<red>✗ Solo el líder puede disolver el clan."));
            return false;
        }

        String name = clanNames.get(clanId);
        // Remover clan de todos los miembros
        Set<UUID> members = clanMembers.getOrDefault(clanId, new HashSet<>());
        for (UUID uuid : members) {
            PlayerData d = plugin.getRankingManager().getCachedPlayerData(uuid);
            if (d != null) d.setClanId(null);
        }

        clanNames.remove(clanId);
        clanLeaders.remove(clanId);
        clanMembers.remove(clanId);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM clans WHERE id = ?")) {
                ps.setString(1, clanId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error disolviendo clan", e);
            }
        });

        leader.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("clan.disbanded", "%clan%", name)));
        return true;
    }

    // ── Invitaciones ──────────────────────────────────────────────────────────

    public void invitePlayer(Player inviter, Player target) {
        String clanId = getClanOfPlayer(inviter.getUniqueId());
        if (clanId == null) {
            inviter.sendMessage(MessageUtil.parse(
                    plugin.getConfigManager().getMessage("clan.not-in-clan")));
            return;
        }
        int maxSize = plugin.getConfigManager().config().getInt("clans.max-size", 10);
        if (clanMembers.getOrDefault(clanId, new HashSet<>()).size() >= maxSize) {
            inviter.sendMessage(MessageUtil.parse(
                    plugin.getConfigManager().getMessage("clan.full",
                            "%clan%", clanNames.get(clanId))));
            return;
        }

        pendingInvites.put(target.getUniqueId(), clanId);
        inviter.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("clan.invited", "%player%", target.getName())));
        target.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("clan.invite-received",
                        "%player%", inviter.getName(),
                        "%clan%", clanNames.get(clanId))));

        // Auto-expirar invitación en 60s
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> pendingInvites.remove(target.getUniqueId()), 60 * 20L);
    }

    public boolean acceptInvite(Player player) {
        String clanId = pendingInvites.remove(player.getUniqueId());
        if (clanId == null || !clanNames.containsKey(clanId)) return false;

        clanMembers.computeIfAbsent(clanId, k -> new HashSet<>()).add(player.getUniqueId());
        PlayerData data = plugin.getRankingManager().getCachedPlayerData(player.getUniqueId());
        if (data != null) data.setClanId(clanId);

        player.sendMessage(MessageUtil.parse(
                plugin.getConfigManager().getMessage("clan.joined",
                        "%clan%", clanNames.get(clanId))));
        return true;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public String getClanOfPlayer(UUID uuid) {
        return clanMembers.entrySet().stream()
                .filter(e -> e.getValue().contains(uuid))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    public String getClanName(String clanId) {
        return clanNames.getOrDefault(clanId, "Sin clan");
    }

    public boolean areSameClan(UUID a, UUID b) {
        String clanA = getClanOfPlayer(a);
        return clanA != null && clanA.equals(getClanOfPlayer(b));
    }

    public void saveAll() {
        // Los datos se guardan automáticamente vía PlayerDAO en RankingManager
    }
}
