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

        // Pool settings desde config
        var poolCfg = plugin.getConfigManager().config().getConfigurationSection("database.pool");
        config.setMaximumPoolSize(poolCfg != null ? poolCfg.getInt("maximum-pool-size", 10) : 10);
        config.setMinimumIdle(poolCfg != null ? poolCfg.getInt("minimum-idle", 2) : 2);
        config.setConnectionTimeout(poolCfg != null ? poolCfg.getLong("connection-timeout", 30000) : 30000);
        config.setIdleTimeout(poolCfg != null ? poolCfg.getLong("idle-timeout", 600000) : 600000);
        config.setMaxLifetime(poolCfg != null ? poolCfg.getLong("max-lifetime", 1800000) : 1800000);

        // Optimizaciones SQLite específicas
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
            conn.createStatement().execute("PRAGMA journal_mode=WAL");
            conn.createStatement().execute("PRAGMA foreign_keys=ON");

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
