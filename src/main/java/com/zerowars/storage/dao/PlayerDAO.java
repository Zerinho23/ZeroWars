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
   * DAO para operaciones de jugadores.
   * Compatible con SQLite y MySQL/MariaDB via DatabaseManager#isMySQL().
   * TODOS los metodos deben ejecutarse en un hilo asincrono.
   */
  public class PlayerDAO {

      private final ZeroWars plugin;

      public PlayerDAO(ZeroWars plugin) { this.plugin = plugin; }

      public PlayerData load(UUID uuid) {
          try (Connection conn = plugin.getDatabaseManager().getConnection();
               PreparedStatement ps = conn.prepareStatement("SELECT * FROM players WHERE uuid = ?")) {
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

      public void save(PlayerData data) {
          boolean mysql = plugin.getDatabaseManager().isMySQL();
          String sql = mysql
              ? "INSERT INTO players (uuid,name,kills,deaths,captures,captures_failed,total_domine_ms,total_money,heat_level,heat_captures,heat_expire,clan_id,last_seen) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),kills=VALUES(kills),deaths=VALUES(deaths),captures=VALUES(captures),captures_failed=VALUES(captures_failed),total_domine_ms=VALUES(total_domine_ms),total_money=VALUES(total_money),heat_level=VALUES(heat_level),heat_captures=VALUES(heat_captures),heat_expire=VALUES(heat_expire),clan_id=VALUES(clan_id),last_seen=VALUES(last_seen)"
              : "INSERT OR REPLACE INTO players (uuid,name,kills,deaths,captures,captures_failed,total_domine_ms,total_money,heat_level,heat_captures,heat_expire,clan_id,last_seen) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
          try (Connection conn = plugin.getDatabaseManager().getConnection();
               PreparedStatement ps = conn.prepareStatement(sql)) {
              ps.setString(1, data.getUuid().toString()); ps.setString(2, data.getName());
              ps.setInt(3, data.getKills());             ps.setInt(4, data.getDeaths());
              ps.setInt(5, data.getCaptures());          ps.setInt(6, data.getCapturesFailed());
              ps.setLong(7, data.getTotalDomineTimeMs()); ps.setDouble(8, data.getTotalMoneyEarned());
              ps.setInt(9, data.getHeatLevel());         ps.setInt(10, data.getHeatCaptures());
              ps.setLong(11, data.getHeatExpireTime());  ps.setString(12, data.getClanId());
              ps.setLong(13, System.currentTimeMillis());
              ps.executeUpdate();
              saveCooldowns(conn, data);
          } catch (SQLException e) {
              plugin.getLogger().log(Level.SEVERE, "Error guardando jugador " + data.getUuid(), e);
          }
      }

      public void createNew(UUID uuid, String name) {
          boolean mysql = plugin.getDatabaseManager().isMySQL();
          String sql = mysql
              ? "INSERT IGNORE INTO players (uuid, name, last_seen) VALUES (?,?,?)"
              : "INSERT OR IGNORE INTO players (uuid, name, last_seen) VALUES (?,?,?)";
          try (Connection conn = plugin.getDatabaseManager().getConnection();
               PreparedStatement ps = conn.prepareStatement(sql)) {
              ps.setString(1, uuid.toString()); ps.setString(2, name);
              ps.setLong(3, System.currentTimeMillis());
              ps.executeUpdate();
          } catch (SQLException e) {
              plugin.getLogger().log(Level.SEVERE, "Error creando jugador " + uuid, e);
          }
      }

      public List<PlayerData> getTop(String column, int limit) {
          List<PlayerData> list = new ArrayList<>();
          try (Connection conn = plugin.getDatabaseManager().getConnection();
               PreparedStatement ps = conn.prepareStatement(
                   "SELECT * FROM players ORDER BY " + column + " DESC LIMIT ?")) {
              ps.setInt(1, limit);
              try (ResultSet rs = ps.executeQuery()) {
                  while (rs.next()) {
                      UUID uuid = UUID.fromString(rs.getString("uuid"));
                      PlayerData data = new PlayerData(uuid, rs.getString("name"));
                      data.setKills(rs.getInt("kills")); data.setDeaths(rs.getInt("deaths"));
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

      private void loadCooldowns(Connection conn, PlayerData data) throws SQLException {
          try (PreparedStatement ps = conn.prepareStatement(
                  "SELECT consumable_id, expire_time FROM consumable_cooldowns WHERE player_uuid = ?")) {
              ps.setString(1, data.getUuid().toString());
              try (ResultSet rs = ps.executeQuery()) {
                  while (rs.next())
                      data.getConsumableCooldowns().put(rs.getString("consumable_id"), rs.getLong("expire_time"));
              }
          }
          try (PreparedStatement ps = conn.prepareStatement(
                  "SELECT zone_id, expire_time FROM recapture_cooldowns WHERE player_uuid = ?")) {
              ps.setString(1, data.getUuid().toString());
              try (ResultSet rs = ps.executeQuery()) {
                  while (rs.next())
                      data.getRecaptureCooldowns().put(rs.getString("zone_id"), rs.getLong("expire_time"));
              }
          }
      }

      private void saveCooldowns(Connection conn, PlayerData data) throws SQLException {
          long now = System.currentTimeMillis();
          String uuid = data.getUuid().toString();
          boolean mysql = plugin.getDatabaseManager().isMySQL();

          try (PreparedStatement ps = conn.prepareStatement(
                  "DELETE FROM consumable_cooldowns WHERE player_uuid=? AND expire_time<?")) {
              ps.setString(1, uuid); ps.setLong(2, now); ps.executeUpdate();
          }
          try (PreparedStatement ps = conn.prepareStatement(
                  "DELETE FROM recapture_cooldowns WHERE player_uuid=? AND expire_time<?")) {
              ps.setString(1, uuid); ps.setLong(2, now); ps.executeUpdate();
          }

          String upsertC = mysql
              ? "INSERT INTO consumable_cooldowns (player_uuid,consumable_id,expire_time) VALUES (?,?,?) ON DUPLICATE KEY UPDATE expire_time=VALUES(expire_time)"
              : "INSERT OR REPLACE INTO consumable_cooldowns (player_uuid,consumable_id,expire_time) VALUES (?,?,?)";
          for (Map.Entry<String, Long> e : data.getConsumableCooldowns().entrySet()) {
              if (e.getValue() > now) {
                  try (PreparedStatement ps = conn.prepareStatement(upsertC)) {
                      ps.setString(1, uuid); ps.setString(2, e.getKey()); ps.setLong(3, e.getValue());
                      ps.executeUpdate();
                  }
              }
          }

          String upsertR = mysql
              ? "INSERT INTO recapture_cooldowns (player_uuid,zone_id,expire_time) VALUES (?,?,?) ON DUPLICATE KEY UPDATE expire_time=VALUES(expire_time)"
              : "INSERT OR REPLACE INTO recapture_cooldowns (player_uuid,zone_id,expire_time) VALUES (?,?,?)";
          for (Map.Entry<String, Long> e : data.getRecaptureCooldowns().entrySet()) {
              if (e.getValue() > now) {
                  try (PreparedStatement ps = conn.prepareStatement(upsertR)) {
                      ps.setString(1, uuid); ps.setString(2, e.getKey()); ps.setLong(3, e.getValue());
                      ps.executeUpdate();
                  }
              }
          }
      }
  }