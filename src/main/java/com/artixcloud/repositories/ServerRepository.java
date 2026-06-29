package com.artixcloud.repositories;

import com.artixcloud.database.DatabaseManager;
import com.artixcloud.models.CloudServer;
import com.artixcloud.models.ServerStatus;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repositório para operações CRUD de {@link CloudServer} no banco de dados.
 */
public class ServerRepository {

    private final DatabaseManager db;
    private static final Logger LOGGER = Logger.getLogger("ArtixCloud");

    public ServerRepository(DatabaseManager db) {
        this.db = db;
    }

    public Optional<CloudServer> findById(String id) {
        String sql = "SELECT * FROM artixcloud_servers WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar servidor por ID: " + id, e);
        }
        return Optional.empty();
    }

    public List<CloudServer> findByOwner(UUID ownerUuid) {
        String sql = "SELECT * FROM artixcloud_servers WHERE owner_uuid = ? ORDER BY created_at DESC";
        List<CloudServer> servers = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) servers.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao listar servidores do jogador: " + ownerUuid, e);
        }
        return servers;
    }

    public int countByOwner(UUID ownerUuid) {
        String sql = "SELECT COUNT(*) FROM artixcloud_servers WHERE owner_uuid = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ownerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao contar servidores do jogador: " + ownerUuid, e);
        }
        return 0;
    }

    public boolean save(CloudServer server) {
        String sql = """
            INSERT INTO artixcloud_servers
                (id, owner_uuid, name, plan_id, version, status, ip, port,
                 players_online, max_players, ram_used_mb, ram_total_mb,
                 cpu_percent, storage_mb, uptime_seconds, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name           = VALUES(name),
                plan_id        = VALUES(plan_id),
                version        = VALUES(version),
                status         = VALUES(status),
                ip             = VALUES(ip),
                port           = VALUES(port),
                players_online = VALUES(players_online),
                max_players    = VALUES(max_players),
                ram_used_mb    = VALUES(ram_used_mb),
                ram_total_mb   = VALUES(ram_total_mb),
                cpu_percent    = VALUES(cpu_percent),
                storage_mb     = VALUES(storage_mb),
                uptime_seconds = VALUES(uptime_seconds),
                updated_at     = VALUES(updated_at)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, server.getId());
            ps.setString(2, server.getOwnerUUID().toString());
            ps.setString(3, server.getName());
            ps.setString(4, server.getPlanId());
            ps.setString(5, server.getVersion());
            ps.setString(6, server.getStatus().getApiValue());
            ps.setString(7, server.getIp());
            ps.setInt(8, server.getPort());
            ps.setInt(9, server.getPlayersOnline());
            ps.setInt(10, server.getMaxPlayers());
            ps.setDouble(11, server.getRamUsedMb());
            ps.setDouble(12, server.getRamTotalMb());
            ps.setDouble(13, server.getCpuPercent());
            ps.setDouble(14, server.getStorageMb());
            ps.setLong(15, server.getUptimeSeconds());
            ps.setTimestamp(16, Timestamp.from(server.getCreatedAt()));
            ps.setTimestamp(17, Timestamp.from(server.getUpdatedAt() != null ? server.getUpdatedAt() : Instant.now()));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao salvar servidor: " + server.getId(), e);
            return false;
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM artixcloud_servers WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao excluir servidor: " + id, e);
            return false;
        }
    }

    private CloudServer mapRow(ResultSet rs) throws SQLException {
        CloudServer s = new CloudServer(
                rs.getString("id"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("name"),
                rs.getString("plan_id"),
                rs.getString("version"),
                rs.getTimestamp("created_at").toInstant()
        );
        s.setStatus(ServerStatus.fromApiValue(rs.getString("status")));
        s.setIp(rs.getString("ip"));
        s.setPort(rs.getInt("port"));
        s.setPlayersOnline(rs.getInt("players_online"));
        s.setMaxPlayers(rs.getInt("max_players"));
        s.setRamUsedMb(rs.getDouble("ram_used_mb"));
        s.setRamTotalMb(rs.getDouble("ram_total_mb"));
        s.setCpuPercent(rs.getDouble("cpu_percent"));
        s.setStorageMb(rs.getDouble("storage_mb"));
        s.setUptimeSeconds(rs.getLong("uptime_seconds"));
        Timestamp upd = rs.getTimestamp("updated_at");
        if (upd != null) s.setUpdatedAt(upd.toInstant());
        return s;
    }
}
