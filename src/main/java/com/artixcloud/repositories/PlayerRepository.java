package com.artixcloud.repositories;

import com.artixcloud.database.DatabaseManager;
import com.artixcloud.models.CloudPlayer;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Repositório para operações CRUD de {@link CloudPlayer} no banco de dados.
 */
public class PlayerRepository {

    private final DatabaseManager db;
    private static final Logger LOGGER = Logger.getLogger("ArtixCloud");

    public PlayerRepository(DatabaseManager db) {
        this.db = db;
    }

    public Optional<CloudPlayer> findByUUID(UUID uuid) {
        String sql = "SELECT * FROM artixcloud_players WHERE uuid = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao buscar jogador por UUID: " + uuid, e);
        }
        return Optional.empty();
    }

    public boolean save(CloudPlayer player) {
        String sql = """
            INSERT INTO artixcloud_players
                (uuid, name, server_count, daily_creations, last_creation, daily_reset, registered_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name             = VALUES(name),
                server_count     = VALUES(server_count),
                daily_creations  = VALUES(daily_creations),
                last_creation    = VALUES(last_creation),
                daily_reset      = VALUES(daily_reset)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, player.getUuid().toString());
            ps.setString(2, player.getName());
            ps.setInt(3, player.getServerCount());
            ps.setInt(4, player.getDailyCreations());
            ps.setTimestamp(5, toTimestamp(player.getLastServerCreation()));
            ps.setTimestamp(6, toTimestamp(player.getDailyCreationReset()));
            ps.setTimestamp(7, toTimestamp(player.getRegisteredAt()));
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao salvar jogador: " + player.getUuid(), e);
            return false;
        }
    }

    public boolean exists(UUID uuid) {
        String sql = "SELECT 1 FROM artixcloud_players WHERE uuid = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erro ao verificar existência do jogador: " + uuid, e);
            return false;
        }
    }

    private CloudPlayer mapRow(ResultSet rs) throws SQLException {
        UUID uuid         = UUID.fromString(rs.getString("uuid"));
        String name       = rs.getString("name");
        Instant reg       = rs.getTimestamp("registered_at").toInstant();
        CloudPlayer p     = new CloudPlayer(uuid, name, reg);
        p.setServerCount(rs.getInt("server_count"));
        p.setDailyCreations(rs.getInt("daily_creations"));

        Timestamp lastCreation = rs.getTimestamp("last_creation");
        if (lastCreation != null) p.setLastServerCreation(lastCreation.toInstant());

        Timestamp dailyReset = rs.getTimestamp("daily_reset");
        if (dailyReset != null) p.setDailyCreationReset(dailyReset.toInstant());

        return p;
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant != null ? Timestamp.from(instant) : null;
    }
}
