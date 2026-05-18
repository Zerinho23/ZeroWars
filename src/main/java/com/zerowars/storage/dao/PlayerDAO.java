package com.zerowars.storage.dao;

import com.zerowars.ZeroWars;
import com.zerowars.models.PlayerData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * DAO (Data Access Object) para operaciones de jugadores.
 * TODOS los métodos que llamen a getConnection() deben ejecutarse
 * en un hilo asíncrono. Nunca desde el hilo principal de Bukkit.
 */
public class PlayerDAO {

    private final ZeroWars plugin;

    public PlayerDAO(ZeroWars plugin) {
        this.plugin = plugin;
    }

    // ── CRUD Principal ───────────────────────────────────────────────────────

    /**
     * Carga los datos de un jugador desde la BD.
     * @return null si el jugador no existe aún
     */
    public PlayerData load(UUID uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    PlayerData data = new PlayerData(uuid, rs.getString("name"));
                    data.setKills(rs.getInt("kills"));
                    data.setDeaths(rs.getInt("deaths"));
                    data.setCaptures(rs.getInt("captures"));
                    data.setCapturesFailed(rs.getInt("captures_failed"));
                    data.setTotalDomineTimeMs(rs.getLong("total_domine_ms"));
                    data.setTotalMoneyEarned(rs.getDouble("total_money"));
                    data.setHeatLevel(rs.getInt("heat_level"));
                    data.setHeatCaptures(rs.getInt("heat_captures"));
                    data.setHeatExpireTime(rs.getLong("heat_expire"));
                    data.setClanId(rs.getString("clan_id"));
                    loadCooldowns(conn, data);
                    return data;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando jugador " + uuid, e);
        }
        return null;
    }

    /**
     * Guarda (INSERT OR REPLACE) los datos completos de un jugador.
     */
    public void save(PlayerData data) {
        String sql = """
            INSERT OR REPLACE INTO players
            (uuid, name, kills, deaths, captures, captures_failed,
             total_domine_ms, total_money, heat_level, heat_captures,
             heat_expire, clan_id, last_seen)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, data.getUuid().toString());
            ps.setString(2, data.getName());
            ps.setInt(3, data.getKills());
            ps.setInt(4, data.getDeaths());
            ps.setInt(5, data.getCaptures());
            ps.setInt(6, data.getCapturesFailed());
            ps.setLong(7, data.getTotalDomineTimeMs());
            ps.setDouble(8, data.getTotalMoneyEarned());
            ps.setInt(9, data.getHeatLevel());
            ps.setInt(10, data.getHeatCaptures());
            ps.setLong(11, data.getHeatExpireTime());
            ps.setString(12, data.getClanId());
            ps.setLong(13, System.currentTimeMillis());
            ps.executeUpdate();
            saveCooldowns(conn, data);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando jugador " + data.getUuid(), e);
        }
    }

    /**
     * Crea un registro nuevo de jugador (primer join).
     */
    public void createNew(UUID uuid, String name) {
        String sql = "INSERT OR IGNORE INTO players (uuid, name, last_seen) VALUES (?,?,?)";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creando jugador " + uuid, e);
        }
    }

    // ── Ranking ──────────────────────────────────────────────────────────────

    /**
     * Carga el top N jugadores por columna.
     * @param column  kills | captures | total_domine_ms
     * @param limit   tamaño del top
     */
    public List<PlayerData> getTop(String column, int limit) {
        List<PlayerData> list = new ArrayList<>();
        String sql = "SELECT * FROM players ORDER BY " + column + " DESC LIMIT ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    PlayerData data = new PlayerData(uuid, rs.getString("name"));
                    data.setKills(rs.getInt("kills"));
                    data.setDeaths(rs.getInt("deaths"));
                    data.setCaptures(rs.getInt("captures"));
                    data.setTotalDomineTimeMs(rs.getLong("total_domine_ms"));
                    list.add(data);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando top " + column, e);
        }
        return list;
    }

    // ── Cooldowns ────────────────────────────────────────────────────────────

    private void loadCooldowns(Connection conn, PlayerData data) throws SQLException {
        // Consumibles
        String sql1 = "SELECT consumable_id, expire_time FROM consumable_cooldowns WHERE player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql1)) {
            ps.setString(1, data.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.getConsumableCooldowns().put(
                        rs.getString("consumable_id"),
                        rs.getLong("expire_time")
                    );
                }
            }
        }

        // Recaptura
        String sql2 = "SELECT zone_id, expire_time FROM recapture_cooldowns WHERE player_uuid = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setString(1, data.getUuid().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.getRecaptureCooldowns().put(
                        rs.getString("zone_id"),
                        rs.getLong("expire_time")
                    );
                }
            }
        }
    }

    private void saveCooldowns(Connection conn, PlayerData data) throws SQLException {
        long now = System.currentTimeMillis();
        String uuidStr = data.getUuid().toString();

        // Limpiar cooldowns expirados
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM consumable_cooldowns WHERE player_uuid = ? AND expire_time < ?")) {
            ps.setString(1, uuidStr);
            ps.setLong(2, now);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM recapture_cooldowns WHERE player_uuid = ? AND expire_time < ?")) {
            ps.setString(1, uuidStr);
            ps.setLong(2, now);
            ps.executeUpdate();
        }

        // Guardar cooldowns activos de consumibles
        String upsert1 = """
            INSERT OR REPLACE INTO consumable_cooldowns
            (player_uuid, consumable_id, expire_time) VALUES (?,?,?)
            """;
        for (Map.Entry<String, Long> e : data.getConsumableCooldowns().entrySet()) {
            if (e.getValue() > now) {
                try (PreparedStatement ps = conn.prepareStatement(upsert1)) {
                    ps.setString(1, uuidStr);
                    ps.setString(2, e.getKey());
                    ps.setLong(3, e.getValue());
                    ps.executeUpdate();
                }
            }
        }

        // Guardar cooldowns activos de recaptura
        String upsert2 = """
            INSERT OR REPLACE INTO recapture_cooldowns
            (player_uuid, zone_id, expire_time) VALUES (?,?,?)
            """;
        for (Map.Entry<String, Long> e : data.getRecaptureCooldowns().entrySet()) {
            if (e.getValue() > now) {
                try (PreparedStatement ps = conn.prepareStatement(upsert2)) {
                    ps.setString(1, uuidStr);
                    ps.setString(2, e.getKey());
                    ps.setLong(3, e.getValue());
                    ps.executeUpdate();
                }
            }
        }
    }
}
