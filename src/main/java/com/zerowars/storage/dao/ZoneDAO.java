package com.zerowars.storage.dao;

import com.zerowars.ZeroWars;
import com.zerowars.models.Zone;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * DAO para el estado persistente de zonas.
 * Solo persiste el estado dinámico (dueño, nivel, tiempo dominado).
 * La definición estática de zonas (bounds, nombre, tipo) viene de zones.yml.
 */
public class ZoneDAO {

    private final ZeroWars plugin;

    public ZoneDAO(ZeroWars plugin) {
        this.plugin = plugin;
    }

    /**
     * Carga el estado de una zona desde la BD y lo aplica al objeto Zone.
     * Llamar después de cargar la zona desde zones.yml.
     */
    public void loadState(Zone zone) {
        String sql = "SELECT * FROM zones WHERE id = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, zone.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String ownerUuidStr = rs.getString("owner_uuid");
                    if (ownerUuidStr != null) {
                        zone.setOwnerUUID(UUID.fromString(ownerUuidStr));
                        zone.setOwnerName(rs.getString("owner_name"));
                        zone.setOwnerClanId(rs.getString("owner_clan_id"));
                        zone.setState(Zone.ZoneState.OWNED);
                    }
                    zone.setLevel(rs.getInt("level"));
                    zone.setTotalDomineTime(rs.getLong("total_domine_ms"));
                    zone.setLastCaptureTime(rs.getLong("last_capture"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error cargando estado de zona " + zone.getId(), e);
        }
    }

    /**
     * Guarda (upsert) el estado dinámico de una zona.
     */
    public void saveState(Zone zone) {
        String sql = """
            INSERT OR REPLACE INTO zones
            (id, owner_uuid, owner_name, owner_clan_id, level, total_domine_ms, last_capture)
            VALUES (?,?,?,?,?,?,?)
            """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, zone.getId());
            ps.setString(2, zone.getOwnerUUID() != null ? zone.getOwnerUUID().toString() : null);
            ps.setString(3, zone.getOwnerName());
            ps.setString(4, zone.getOwnerClanId());
            ps.setInt(5, zone.getLevel());
            ps.setLong(6, zone.getTotalDomineTime());
            ps.setLong(7, zone.getLastCaptureTime());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error guardando zona " + zone.getId(), e);
        }
    }

    /**
     * Resetea el estado de una zona a neutral en la BD.
     */
    public void resetZone(String zoneId) {
        String sql = """
            UPDATE zones SET owner_uuid = NULL, owner_name = NULL,
            owner_clan_id = NULL, level = 1, total_domine_ms = 0
            WHERE id = ?
            """;
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, zoneId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error reseteando zona " + zoneId, e);
        }
    }
}
