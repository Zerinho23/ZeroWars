package com.zerowars.storage;

  import com.zaxxer.hikari.HikariConfig;
  import com.zaxxer.hikari.HikariDataSource;
  import com.zerowars.ZeroWars;

  import java.io.File;
  import java.sql.Connection;
  import java.sql.PreparedStatement;
  import java.sql.SQLException;
  import java.util.logging.Level;

  /**
   * Gestor de base de datos con soporte para SQLite y MySQL/MariaDB.
   * Configurar en config.yml: database.type: sqlite | mysql
   *
   * Cambios v1.1.3:
   *  - Soporte MySQL/MariaDB (database.type: mysql). Compatible con MariaDB.
   *  - Corregido SQLITE_BUSY: pool SQLite limitado a maxSize=1 y PRAGMAs
   *    aplicados via connectionInitSql antes de createTables().
   */
  public class DatabaseManager {

      public enum DatabaseType { SQLITE, MYSQL }

      private final ZeroWars plugin;
      private HikariDataSource dataSource;
      private DatabaseType type;

      public DatabaseManager(ZeroWars plugin) {
          this.plugin = plugin;
      }

      public void initialize() throws Exception {
          String typeStr = plugin.getConfigManager().config()
                  .getString("database.type", "sqlite").toLowerCase();
          this.type = typeStr.equals("mysql") ? DatabaseType.MYSQL : DatabaseType.SQLITE;
          if (type == DatabaseType.MYSQL) { initMySQL(); } else { initSQLite(); }
          createTables();
          plugin.getLogger().info("Base de datos inicializada (" + type.name() + ").");
      }

      private void initSQLite() throws Exception {
          File dbFile = new File(plugin.getDataFolder(),
              plugin.getConfigManager().config().getString("database.file", "zerowars.db"));
          plugin.getDataFolder().mkdirs();
          try { Class.forName("org.sqlite.JDBC"); }
          catch (ClassNotFoundException e) { throw new RuntimeException("Driver SQLite no encontrado.", e); }

          HikariConfig config = new HikariConfig();
          config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
          config.setDriverClassName("org.sqlite.JDBC");
          // SQLite solo soporta un escritor concurrente.
          // Pool > 1 causa SQLITE_BUSY durante la inicializacion.
          config.setMaximumPoolSize(1);
          config.setMinimumIdle(1);
          applyPoolTimeouts(config);
          // PRAGMAs aplicados en cada conexion nueva ANTES de cualquier uso.
          // busy_timeout=5000: reintenta hasta 5s si el archivo esta bloqueado.
          config.setConnectionInitSql(
              "PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON; PRAGMA busy_timeout=5000;");
          config.addDataSourceProperty("cachePrepStmts",        "true");
          config.addDataSourceProperty("prepStmtCacheSize",     "250");
          config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
          config.setPoolName("ZeroWars-Pool");
          dataSource = new HikariDataSource(config);
          plugin.getLogger().info("SQLite: " + dbFile.getName());
      }

      private void initMySQL() throws Exception {
          try { Class.forName("com.mysql.cj.jdbc.Driver"); }
          catch (ClassNotFoundException e) { throw new RuntimeException("Driver MySQL no encontrado.", e); }

          var cfg   = plugin.getConfigManager().config();
          String host   = cfg.getString ("database.mysql.host",     "localhost");
          int    port   = cfg.getInt    ("database.mysql.port",     3306);
          String dbName = cfg.getString ("database.mysql.database", "zerowars");
          String user   = cfg.getString ("database.mysql.username", "root");
          String pass   = cfg.getString ("database.mysql.password", "");
          boolean ssl   = cfg.getBoolean("database.mysql.ssl",      false);

          String url = String.format(
              "jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC&characterEncoding=utf8&autoReconnect=true",
              host, port, dbName, ssl);

          HikariConfig config = new HikariConfig();
          config.setJdbcUrl(url);
          config.setDriverClassName("com.mysql.cj.jdbc.Driver");
          config.setUsername(user);
          config.setPassword(pass);
          var poolCfg = cfg.getConfigurationSection("database.pool");
          config.setMaximumPoolSize(poolCfg != null ? poolCfg.getInt("maximum-pool-size", 10) : 10);
          config.setMinimumIdle    (poolCfg != null ? poolCfg.getInt("minimum-idle",       2) :  2);
          applyPoolTimeouts(config);
          config.addDataSourceProperty("cachePrepStmts",        "true");
          config.addDataSourceProperty("prepStmtCacheSize",     "250");
          config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
          config.addDataSourceProperty("useServerPrepStmts",    "true");
          config.setPoolName("ZeroWars-Pool");
          dataSource = new HikariDataSource(config);
          plugin.getLogger().info("MySQL: " + host + ":" + port + "/" + dbName);
      }

      private void applyPoolTimeouts(HikariConfig config) {
          var poolCfg = plugin.getConfigManager().config().getConfigurationSection("database.pool");
          config.setConnectionTimeout(poolCfg != null ? poolCfg.getLong("connection-timeout", 30_000L)    : 30_000L);
          config.setIdleTimeout      (poolCfg != null ? poolCfg.getLong("idle-timeout",      600_000L)    : 600_000L);
          config.setMaxLifetime      (poolCfg != null ? poolCfg.getLong("max-lifetime",    1_800_000L)    : 1_800_000L);
      }

      private void createTables() throws SQLException {
          try (Connection conn = getConnection()) {
              execute(conn,
                  "CREATE TABLE IF NOT EXISTS players (" +
                  "uuid VARCHAR(36) NOT NULL, name VARCHAR(16) NOT NULL," +
                  "kills INT NOT NULL DEFAULT 0, deaths INT NOT NULL DEFAULT 0," +
                  "captures INT NOT NULL DEFAULT 0, captures_failed INT NOT NULL DEFAULT 0," +
                  "total_domine_ms BIGINT NOT NULL DEFAULT 0, total_money DOUBLE NOT NULL DEFAULT 0.0," +
                  "heat_level INT NOT NULL DEFAULT 0, heat_captures INT NOT NULL DEFAULT 0," +
                  "heat_expire BIGINT NOT NULL DEFAULT 0, clan_id VARCHAR(36) DEFAULT NULL," +
                  "last_seen BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (uuid))");
              execute(conn,
                  "CREATE TABLE IF NOT EXISTS zones (" +
                  "id VARCHAR(64) NOT NULL, owner_uuid VARCHAR(36) DEFAULT NULL," +
                  "owner_name VARCHAR(16) DEFAULT NULL, owner_clan_id VARCHAR(36) DEFAULT NULL," +
                  "level INT NOT NULL DEFAULT 1, total_domine_ms BIGINT NOT NULL DEFAULT 0," +
                  "last_capture BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (id))");
              execute(conn,
                  "CREATE TABLE IF NOT EXISTS consumable_cooldowns (" +
                  "player_uuid VARCHAR(36) NOT NULL, consumable_id VARCHAR(64) NOT NULL," +
                  "expire_time BIGINT NOT NULL, PRIMARY KEY (player_uuid, consumable_id))");
              execute(conn,
                  "CREATE TABLE IF NOT EXISTS recapture_cooldowns (" +
                  "player_uuid VARCHAR(36) NOT NULL, zone_id VARCHAR(64) NOT NULL," +
                  "expire_time BIGINT NOT NULL, PRIMARY KEY (player_uuid, zone_id))");
              execute(conn,
                  "CREATE TABLE IF NOT EXISTS clans (" +
                  "id VARCHAR(36) NOT NULL, name VARCHAR(32) NOT NULL UNIQUE," +
                  "leader_uuid VARCHAR(36) NOT NULL, created_at BIGINT NOT NULL," +
                  "total_kills INT NOT NULL DEFAULT 0, total_captures INT NOT NULL DEFAULT 0," +
                  "total_domine_ms BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (id))");
              execute(conn,
                  "CREATE TABLE IF NOT EXISTS clan_members (" +
                  "player_uuid VARCHAR(36) NOT NULL, clan_id VARCHAR(36) NOT NULL," +
                  "role VARCHAR(16) NOT NULL DEFAULT 'MEMBER', joined_at BIGINT NOT NULL," +
                  "PRIMARY KEY (player_uuid)," +
                  "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE)");
              createIndexSafe(conn, "CREATE INDEX IF NOT EXISTS idx_players_kills    ON players(kills DESC)");
              createIndexSafe(conn, "CREATE INDEX IF NOT EXISTS idx_players_captures ON players(captures DESC)");
              createIndexSafe(conn, "CREATE INDEX IF NOT EXISTS idx_players_domine   ON players(total_domine_ms DESC)");
          }
      }

      private void createIndexSafe(Connection conn, String sql) {
          try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.execute(); }
          catch (SQLException e) {
              if (e.getErrorCode() != 1061)
                  plugin.getLogger().log(Level.WARNING, "Advertencia al crear indice: " + e.getMessage());
          }
      }

      private void execute(Connection conn, String sql) throws SQLException {
          try (PreparedStatement ps = conn.prepareStatement(sql)) { ps.execute(); }
      }

      public Connection getConnection() throws SQLException {
          if (dataSource == null || dataSource.isClosed())
              throw new SQLException("El pool de base de datos no esta activo.");
          return dataSource.getConnection();
      }

      public DatabaseType getType() { return type; }
      public boolean isMySQL()      { return type == DatabaseType.MYSQL; }

      public void close() {
          if (dataSource != null && !dataSource.isClosed()) {
              dataSource.close();
              plugin.getLogger().info("Pool de base de datos cerrado correctamente.");
          }
      }

      public boolean isConnected() { return dataSource != null && !dataSource.isClosed(); }
  }