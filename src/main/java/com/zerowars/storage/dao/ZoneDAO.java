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
   * Compatible con SQLite y MySQL/MariaDB.
   */
  public class ZoneDAO {

      private final ZeroWars plugin;

      public ZoneDAO(ZeroWars plugin) { this.plugin = plugin; }

      public void loadState(Zone zone) {
          try (Connection conn = plugin.getDatabaseManager().getConnection();
               PreparedStatement ps = conn.prepareStatement("SELECT * FROM zones WHERE id = ?")) {
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
              plugin.getLogger().log(Level.SEVERE, "Error cargando zona " + zone.getId(), e);
          }
      }

      public void saveState(Zone zone) {
          boolean mysql = plugin.getDatabaseManager().isMySQL();
          String sql = mysql
              ? "INSERT INTO zones (id,owner_uuid,owner_name,owner_clan_id,level,total_domine_ms,last_capture) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE owner_uuid=VALUES(owner_uuid),owner_name=VALUES(owner_name),owner_clan_id=VALUES(owner_clan_id),level=VALUES(level),total_domine_ms=VALUES(total_domine_ms),last_capture=VALUES(last_capture)"
              : "INSERT OR REPLACE INTO zones (id,owner_uuid,owner_name,owner_clan_id,level,total_domine_ms,last_capture) VALUES (?,?,?,?,?,?,?)";
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

      public void resetZone(String zoneId) {
          try (Connection conn = plugin.getDatabaseManager().getConnection();
               PreparedStatement ps = conn.prepareStatement(
                  "UPDATE zones SET owner_uuid=NULL,owner_name=NULL,owner_clan_id=NULL,level=1,total_domine_ms=0 WHERE id=?")) {
              ps.setString(1, zoneId);
              ps.executeUpdate();
          } catch (SQLException e) {
              plugin.getLogger().log(Level.SEVERE, "Error reseteando zona " + zoneId, e);
          }
      }
  }