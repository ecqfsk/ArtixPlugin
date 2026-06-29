package com.artixcloud.services;

import com.artixcloud.ArtixCloud;
import com.artixcloud.api.APIClient;
import com.artixcloud.api.APIResponse;
import com.artixcloud.cache.CacheManager;
import com.artixcloud.models.CloudPlayer;
import com.artixcloud.models.CloudServer;
import com.artixcloud.models.ServerStatus;
import com.artixcloud.repositories.PlayerRepository;
import com.artixcloud.repositories.ServerRepository;
import com.artixcloud.utils.MessageUtil;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Serviço responsável por todas as operações relacionadas a servidores.
 * Orquestra a comunicação entre API, banco de dados e cache.
 */
public class ServerService {

    private final ArtixCloud plugin;
    private final APIClient apiClient;
    private final ServerRepository serverRepository;
    private final PlayerRepository playerRepository;
    private final CacheManager cacheManager;
    private final PlanService planService;

    public ServerService(ArtixCloud plugin, APIClient apiClient,
                         ServerRepository serverRepository,
                         PlayerRepository playerRepository,
                         CacheManager cacheManager,
                         PlanService planService) {
        this.plugin           = plugin;
        this.apiClient        = apiClient;
        this.serverRepository = serverRepository;
        this.playerRepository = playerRepository;
        this.cacheManager     = cacheManager;
        this.planService      = planService;
    }

    // ── Criação ───────────────────────────────────────────────────────────────

    public CompletableFuture<Optional<CloudServer>> createServer(Player player, String name, String planId, String version) {
        return apiClient.createServer(player.getUniqueId().toString(), name, planId, version)
                .thenApplyAsync(response -> {
                    if (!response.isSuccess()) {
                        log("Falha ao criar servidor para " + player.getName() + ": " + response.getErrorMessage());
                        return Optional.<CloudServer>empty();
                    }
                    CloudServer server = mapServerFromJson(response.getData(), player.getUniqueId());
                    serverRepository.save(server);
                    cacheManager.putServer(server);
                    cacheManager.invalidateServerList(player.getUniqueId());

                    // Incrementa contador do jogador
                    playerRepository.findByUUID(player.getUniqueId()).ifPresent(cp -> {
                        cp.incrementServerCount();
                        cp.setLastServerCreation(Instant.now());
                        playerRepository.save(cp);
                        cacheManager.putPlayer(cp);
                    });

                    return Optional.of(server);
                });
    }

    // ── Listagem ──────────────────────────────────────────────────────────────

    public CompletableFuture<List<CloudServer>> getServers(UUID ownerUuid) {
        Optional<List<CloudServer>> cached = cacheManager.getServerList(ownerUuid);
        if (cached.isPresent()) return CompletableFuture.completedFuture(cached.get());

        return apiClient.listServers(ownerUuid.toString())
                .thenApplyAsync(response -> {
                    if (!response.isSuccess()) {
                        // Fallback para banco de dados local
                        List<CloudServer> local = serverRepository.findByOwner(ownerUuid);
                        cacheManager.putServerList(ownerUuid, local);
                        return local;
                    }
                    List<CloudServer> servers = parseServerList(response.getData(), ownerUuid);
                    servers.forEach(s -> {
                        serverRepository.save(s);
                        cacheManager.putServer(s);
                    });
                    cacheManager.putServerList(ownerUuid, servers);
                    return servers;
                });
    }

    // ── Status ────────────────────────────────────────────────────────────────

    public CompletableFuture<Optional<CloudServer>> refreshServerStatus(String serverId) {
        return apiClient.getServerStatus(serverId)
                .thenApplyAsync(response -> {
                    if (!response.isSuccess()) {
                        return cacheManager.getServer(serverId);
                    }
                    return serverRepository.findById(serverId).map(server -> {
                        updateServerFromStatusJson(server, response.getData());
                        serverRepository.save(server);
                        cacheManager.putServer(server);
                        return server;
                    });
                });
    }

    // ── Ações ─────────────────────────────────────────────────────────────────

    public CompletableFuture<Boolean> startServer(String serverId) {
        return apiClient.startServer(serverId)
                .thenApplyAsync(response -> {
                    if (response.isSuccess()) {
                        updateLocalStatus(serverId, ServerStatus.STARTING);
                    }
                    return response.isSuccess();
                });
    }

    public CompletableFuture<Boolean> stopServer(String serverId) {
        return apiClient.stopServer(serverId)
                .thenApplyAsync(response -> {
                    if (response.isSuccess()) {
                        updateLocalStatus(serverId, ServerStatus.STOPPING);
                    }
                    return response.isSuccess();
                });
    }

    public CompletableFuture<Boolean> restartServer(String serverId) {
        return apiClient.restartServer(serverId)
                .thenApplyAsync(response -> {
                    if (response.isSuccess()) {
                        updateLocalStatus(serverId, ServerStatus.RESTARTING);
                    }
                    return response.isSuccess();
                });
    }

    public CompletableFuture<Boolean> deleteServer(Player player, String serverId) {
        return apiClient.deleteServer(serverId)
                .thenApplyAsync(response -> {
                    if (response.isSuccess()) {
                        serverRepository.delete(serverId);
                        cacheManager.invalidateServer(serverId);
                        cacheManager.invalidateServerList(player.getUniqueId());
                        playerRepository.findByUUID(player.getUniqueId()).ifPresent(cp -> {
                            cp.decrementServerCount();
                            playerRepository.save(cp);
                            cacheManager.putPlayer(cp);
                        });
                    }
                    return response.isSuccess();
                });
    }

    // ── Validação ─────────────────────────────────────────────────────────────

    public boolean canCreateFreeServer(CloudPlayer cloudPlayer) {
        int maxFree = plugin.getConfig().getInt("free-plan.max-servers-per-player", 1);
        return cloudPlayer.getServerCount() < maxFree;
    }

    public boolean isInCooldown(CloudPlayer cloudPlayer) {
        long cooldown = plugin.getConfig().getLong("free-plan.creation-cooldown", 3600);
        return cloudPlayer.isInCooldown(cooldown);
    }

    public String getCooldownRemaining(CloudPlayer cloudPlayer) {
        long cooldown = plugin.getConfig().getLong("free-plan.creation-cooldown", 3600);
        return cloudPlayer.getFormattedCooldown(cooldown);
    }

    public boolean isNameValid(String name) {
        return name != null && name.matches("^[a-zA-Z0-9\\-]{3,16}$");
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private void updateLocalStatus(String serverId, ServerStatus status) {
        serverRepository.findById(serverId).ifPresent(server -> {
            server.setStatus(status);
            server.setUpdatedAt(Instant.now());
            serverRepository.save(server);
            cacheManager.putServer(server);
        });
    }

    private CloudServer mapServerFromJson(JsonObject json, UUID ownerUuid) {
        String id      = json.has("id")      ? json.get("id").getAsString()      : UUID.randomUUID().toString();
        String name    = json.has("name")    ? json.get("name").getAsString()    : "server";
        String planId  = json.has("plan_id") ? json.get("plan_id").getAsString() : "free";
        String version = json.has("version") ? json.get("version").getAsString() : "1.21.1";

        CloudServer server = new CloudServer(id, ownerUuid, name, planId, version, Instant.now());
        updateServerFromStatusJson(server, json);
        return server;
    }

    private void updateServerFromStatusJson(CloudServer server, JsonObject json) {
        if (json.has("status"))         server.setStatus(ServerStatus.fromApiValue(json.get("status").getAsString()));
        if (json.has("ip"))             server.setIp(json.get("ip").getAsString());
        if (json.has("port"))           server.setPort(json.get("port").getAsInt());
        if (json.has("players_online")) server.setPlayersOnline(json.get("players_online").getAsInt());
        if (json.has("max_players"))    server.setMaxPlayers(json.get("max_players").getAsInt());
        if (json.has("ram_used_mb"))    server.setRamUsedMb(json.get("ram_used_mb").getAsDouble());
        if (json.has("ram_total_mb"))   server.setRamTotalMb(json.get("ram_total_mb").getAsDouble());
        if (json.has("cpu_percent"))    server.setCpuPercent(json.get("cpu_percent").getAsDouble());
        if (json.has("storage_mb"))     server.setStorageMb(json.get("storage_mb").getAsDouble());
        if (json.has("uptime_seconds")) server.setUptimeSeconds(json.get("uptime_seconds").getAsLong());
        server.setUpdatedAt(Instant.now());
    }

    private List<CloudServer> parseServerList(JsonObject response, UUID ownerUuid) {
        List<CloudServer> servers = new java.util.ArrayList<>();
        if (response.has("servers") && response.get("servers").isJsonArray()) {
            for (var element : response.getAsJsonArray("servers")) {
                if (element.isJsonObject()) {
                    servers.add(mapServerFromJson(element.getAsJsonObject(), ownerUuid));
                }
            }
        }
        return servers;
    }

    private void log(String msg) {
        plugin.getLogger().log(Level.WARNING, msg);
    }
}
