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
   * Gestor de base de datos SQLite con pool de conexiones HikariCP.
   * Todas las operaciones de DB deben ir a través de los DAOs.
   * Esta clase solo gestiona el pool y la inicialización del schema.
   *
   * IMPORTANTE: Las queries CRUD NO deben ejecutarse en el hilo principal.
   * Usa Bukkit.getScheduler().runTaskAsynchronously() en los DAOs.
   *
   * FIX v1.1.3: Corregido SQLITE_BUSY al inicializar.
   *  - Pool máximo reducido a 1 (SQLite no soporta escrituras concurrentes).
   *  - WAL mode y busy_timeout configurados vía connectionInitSql ANTES de
   *    que el pool abra conexiones, evitando carreras al crear tablas.
   *  - Eliminados los PRAGMA duplicados de createTables().
   */
  public class DatabaseManager {

      private final ZeroWars plugin;
      private HikariDataSource dataSource;

      public DatabaseManager(ZeroWars plugin) {
          this.plugin = plugin;
      }

      /**
       * Inicializa el pool de conexiones y crea las tablas si no existen.
       *
       * NOTA sobre el driver SQLite:
       * sqlite-jdbc contiene código nativo (JNI). NO debe relocarse el paquete
       * org.sqlite en Shadow JAR — los símbolos nativos están compilados con el
       * nombre original y rompen si se cambia el nombre de la clase Java.
       * Por eso usamos "org.sqlite.JDBC" directamente (sin relocalización).
       */
      public void initialize() throws Exception {
          File dbFile = new File(plugin.getDataFolder(),
              plugin.getConfigManager().config().getString("database.file", "zerowars.db"));
          plugin.getDataFolder().mkdirs();

          // Precarga explícita del driver SQLite antes de que HikariCP lo intente.
          try {
              Class.forName("org.sqlite.JDBC");
          } catch (ClassNotFoundException e) {
              throw new RuntimeException("Driver SQLite no encontrado en el JAR. " +
                  "Asegúrate de que sqlite-jdbc no está excluido del Shadow JAR.", e);
          }

          HikariConfig config = new HikariConfig();
          config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
          config.setDriverClassName("org.sqlite.JDBC");

          // ── SQLite solo soporta un escritor a la vez ─────────────────────────
          // Un pool > 1 provoca SQLITE_BUSY inmediatamente cuando múltiples
          // conexiones intentan hacer PRAGMA o DDL en paralelo al arrancar.
          // Con WAL mode podemos tener 1 escritor + N lectores, pero HikariCP
          // no distingue entre conexiones de lectura y escritura, así que
          // limitamos el pool a 1 para evitar cualquier contención de escritura.
          config.setMaximumPoolSize(1);
          config.setMinimumIdle(1);

          config.setConnectionTimeout(30_000L);
          config.setIdleTimeout(600_000L);
          config.setMaxLifetime(1_800_000L);

          // ── PRAGMAs aplicados a CADA conexión nueva del pool ─────────────────
          // connectionInitSql se ejecuta justo después de abrir la conexión,
          // ANTES de que HikariCP la entregue a nadie. Así WAL y busy_timeout
          // están activos desde el primer uso, incluyendo createTables().
          //
          // busy_timeout=5000: si la DB está bloqueada, SQLite reintentará
          // hasta 5 segundos antes de lanzar SQLITE_BUSY. Cubre reloads rápidos
          // y reinicios del servidor donde el archivo puede quedar bloqueado
          // brevemente por el proceso anterior.
          config.setConnectionInitSql(
              "PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON; PRAGMA busy_timeout=5000;"
          );

          // Optimizaciones de caché de prepared statements
          config.addDataSourceProperty("cachePrepStmts", "true");
          config.addDataSourceProperty("prepStmtCacheSize", "250");
          config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
          config.setPoolName("ZeroWars-Pool");

          dataSource = new HikariDataSource(config);

          createTables();
          plugin.getLogger().info("Base de datos inicializada: " + dbFile.getName());
      }

      private void createTables() throws SQLException {
          try (Connection conn = getConnection()) {
              // PRAGMAs ya aplicados por connectionInitSql — no hace falta repetirlos aquí.

              execute(conn, """
                  CREATE TABLE IF NOT EXISTS players (
                      uuid              TEXT PRIMARY KEY,
                      name              TEXT NOT NULL,
                      kills             INTEGER DEFAULT 0,
                      deaths            INTEGER DEFAULT 0,
                      captures          INTEGER DEFAULT 0,
                      captures_failed   INTEGER DEFAULT 0,
                      total_domine_ms   INTEGER DEFAULT 0,
                      total_money       REAL    DEFAULT 0.0,
                      heat_level        INTEGER DEFAULT 0,
                      heat_captures     INTEGER DEFAULT 0,
                      heat_expire       INTEGER DEFAULT 0,
                      clan_id           TEXT    DEFAULT NULL,
                      last_seen         INTEGER DEFAULT 0
                  )
                  """);

              execute(conn, """
                  CREATE TABLE IF NOT EXISTS zones (
                      id                TEXT PRIMARY KEY,
                      owner_uuid        TEXT    DEFAULT NULL,
                      owner_name        TEXT    DEFAULT NULL,
                      owner_clan_id     TEXT    DEFAULT NULL,
                      level             INTEGER DEFAULT 1,
                      total_domine_ms   INTEGER DEFAULT 0,
                      last_capture      INTEGER DEFAULT 0
                  )
                  """);

              execute(conn, """
                  CREATE TABLE IF NOT EXISTS consumable_cooldowns (
                      player_uuid       TEXT NOT NULL,
                      consumable_id     TEXT NOT NULL,
                      expire_time       INTEGER NOT NULL,
                      PRIMARY KEY (player_uuid, consumable_id)
                  )
                  """);

              execute(conn, """
                  CREATE TABLE IF NOT EXISTS recapture_cooldowns (
                      player_uuid       TEXT NOT NULL,
                      zone_id           TEXT NOT NULL,
                      expire_time       INTEGER NOT NULL,
                      PRIMARY KEY (player_uuid, zone_id)
                  )
                  """);

              execute(conn, """
                  CREATE TABLE IF NOT EXISTS clans (
                      id                TEXT PRIMARY KEY,
                      name              TEXT NOT NULL UNIQUE,
                      leader_uuid       TEXT NOT NULL,
                      created_at        INTEGER NOT NULL,
                      total_kills       INTEGER DEFAULT 0,
                      total_captures    INTEGER DEFAULT 0,
                      total_domine_ms   INTEGER DEFAULT 0
                  )
                  """);

              execute(conn, """
                  CREATE TABLE IF NOT EXISTS clan_members (
                      player_uuid       TEXT PRIMARY KEY,
                      clan_id           TEXT NOT NULL,
                      role              TEXT DEFAULT 'MEMBER',
                      joined_at         INTEGER NOT NULL,
                      FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE
                  )
                  """);

              execute(conn, "CREATE INDEX IF NOT EXISTS idx_players_kills ON players(kills DESC)");
              execute(conn, "CREATE INDEX IF NOT EXISTS idx_players_captures ON players(captures DESC)");
              execute(conn, "CREATE INDEX IF NOT EXISTS idx_players_domine ON players(total_domine_ms DESC)");
          }
      }

      private void execute(Connection conn, String sql) throws SQLException {
          try (PreparedStatement ps = conn.prepareStatement(sql)) {
              ps.execute();
          }
      }

      public Connection getConnection() throws SQLException {
          if (dataSource == null || dataSource.isClosed()) {
              throw new SQLException("El pool de base de datos no está activo.");
          }
          return dataSource.getConnection();
      }

      public void close() {
          if (dataSource != null && !dataSource.isClosed()) {
              dataSource.close();
              plugin.getLogger().info("Pool de base de datos cerrado correctamente.");
          }
      }

      public boolean isConnected() {
          return dataSource != null && !dataSource.isClosed();
      }
  }
  