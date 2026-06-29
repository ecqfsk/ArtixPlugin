package com.artixcloud.database;

import com.artixcloud.ArtixCloud;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * Gerencia o pool de conexões MySQL via HikariCP e garante o esquema do banco.
 */
public class DatabaseManager {

    private final ArtixCloud plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ArtixCloud plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws Exception {
        ConfigurationSection db = plugin.getConfig().getConfigurationSection("database");
        if (db == null) throw new IllegalStateException("Seção 'database' não encontrada no config.yml");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://"
                + db.getString("host", "localhost") + ":"
                + db.getInt("port", 3306) + "/"
                + db.getString("name", "artixcloud")
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8");
        config.setUsername(db.getString("username", "root"));
        config.setPassword(db.getString("password", ""));
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        ConfigurationSection pool = db.getConfigurationSection("pool");
        if (pool != null) {
            config.setMaximumPoolSize(pool.getInt("maximum-pool-size", 10));
            config.setMinimumIdle(pool.getInt("minimum-idle", 2));
            config.setConnectionTimeout(pool.getLong("connection-timeout", 30000));
            config.setIdleTimeout(pool.getLong("idle-timeout", 600000));
            config.setMaxLifetime(pool.getLong("max-lifetime", 1800000));
        }

        config.setPoolName("ArtixCloud-HikariPool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
        createTables();
        plugin.getLogger().info("Banco de dados conectado com sucesso.");
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Tabela de jogadores
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS artixcloud_players (
                    uuid                VARCHAR(36)  NOT NULL PRIMARY KEY,
                    name                VARCHAR(64)  NOT NULL,
                    server_count        INT          NOT NULL DEFAULT 0,
                    daily_creations     INT          NOT NULL DEFAULT 0,
                    last_creation       DATETIME     NULL,
                    daily_reset         DATETIME     NULL,
                    registered_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_name (name)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);

            // Tabela de servidores
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS artixcloud_servers (
                    id                  VARCHAR(64)  NOT NULL PRIMARY KEY,
                    owner_uuid          VARCHAR(36)  NOT NULL,
                    name                VARCHAR(64)  NOT NULL,
                    plan_id             VARCHAR(32)  NOT NULL,
                    version             VARCHAR(16)  NOT NULL,
                    status              VARCHAR(16)  NOT NULL DEFAULT 'offline',
                    ip                  VARCHAR(64)  NULL,
                    port                INT          NULL,
                    players_online      INT          NOT NULL DEFAULT 0,
                    max_players         INT          NOT NULL DEFAULT 0,
                    ram_used_mb         DOUBLE       NOT NULL DEFAULT 0,
                    ram_total_mb        DOUBLE       NOT NULL DEFAULT 0,
                    cpu_percent         DOUBLE       NOT NULL DEFAULT 0,
                    storage_mb          DOUBLE       NOT NULL DEFAULT 0,
                    uptime_seconds      BIGINT       NOT NULL DEFAULT 0,
                    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_owner (owner_uuid),
                    INDEX idx_status (status),
                    CONSTRAINT fk_server_owner FOREIGN KEY (owner_uuid)
                        REFERENCES artixcloud_players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Pool de conexões não está disponível.");
        }
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Pool de conexões MySQL encerrado.");
        }
    }
}
