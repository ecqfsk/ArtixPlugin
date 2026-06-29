package com.artixcloud.cache;

import com.artixcloud.ArtixCloud;
import com.artixcloud.models.CloudPlayer;
import com.artixcloud.models.CloudServer;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Gerencia o cache local em memória com Caffeine.
 * Evita consultas desnecessárias ao banco de dados e à API.
 */
public class CacheManager {

    private final Cache<UUID, CloudPlayer> playerCache;
    private final Cache<UUID, List<CloudServer>> serverListCache;
    private final Cache<String, CloudServer> serverCache;

    public CacheManager(ArtixCloud plugin) {
        int ttl = plugin.getConfig().getInt("schedulers.cache-cleanup-interval", 300);

        this.playerCache = Caffeine.newBuilder()
                .expireAfterWrite(ttl, TimeUnit.SECONDS)
                .maximumSize(1_000)
                .build();

        this.serverListCache = Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1_000)
                .build();

        this.serverCache = Caffeine.newBuilder()
                .expireAfterWrite(20, TimeUnit.SECONDS)
                .maximumSize(5_000)
                .build();
    }

    // ── Jogadores ─────────────────────────────────────────────────────────────

    public Optional<CloudPlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(playerCache.getIfPresent(uuid));
    }

    public void putPlayer(CloudPlayer player) {
        playerCache.put(player.getUuid(), player);
    }

    public void invalidatePlayer(UUID uuid) {
        playerCache.invalidate(uuid);
        serverListCache.invalidate(uuid);
    }

    // ── Lista de servidores por jogador ───────────────────────────────────────

    public Optional<List<CloudServer>> getServerList(UUID ownerUuid) {
        return Optional.ofNullable(serverListCache.getIfPresent(ownerUuid));
    }

    public void putServerList(UUID ownerUuid, List<CloudServer> servers) {
        serverListCache.put(ownerUuid, servers);
    }

    public void invalidateServerList(UUID ownerUuid) {
        serverListCache.invalidate(ownerUuid);
    }

    // ── Servidor individual ───────────────────────────────────────────────────

    public Optional<CloudServer> getServer(String serverId) {
        return Optional.ofNullable(serverCache.getIfPresent(serverId));
    }

    public void putServer(CloudServer server) {
        serverCache.put(server.getId(), server);
    }

    public void invalidateServer(String serverId) {
        serverCache.invalidate(serverId);
    }

    // ── Global ────────────────────────────────────────────────────────────────

    public void invalidateAll() {
        playerCache.invalidateAll();
        serverListCache.invalidateAll();
        serverCache.invalidateAll();
    }

    public long estimatedPlayerCount() { return playerCache.estimatedSize(); }
    public long estimatedServerCount() { return serverCache.estimatedSize(); }
}
