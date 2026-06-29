package com.artixcloud.services;

import com.artixcloud.ArtixCloud;
import com.artixcloud.cache.CacheManager;
import com.artixcloud.models.CloudPlayer;
import com.artixcloud.repositories.PlayerRepository;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço responsável pelo gerenciamento de dados dos jogadores.
 */
public class PlayerService {

    private final ArtixCloud plugin;
    private final PlayerRepository playerRepository;
    private final CacheManager cacheManager;

    public PlayerService(ArtixCloud plugin, PlayerRepository playerRepository, CacheManager cacheManager) {
        this.plugin           = plugin;
        this.playerRepository = playerRepository;
        this.cacheManager     = cacheManager;
    }

    /**
     * Retorna o {@link CloudPlayer} de um jogador, usando cache e banco de dados.
     * Se não existir, cria automaticamente.
     */
    public CompletableFuture<CloudPlayer> getOrCreatePlayer(Player player) {
        Optional<CloudPlayer> cached = cacheManager.getPlayer(player.getUniqueId());
        if (cached.isPresent()) return CompletableFuture.completedFuture(cached.get());

        return CompletableFuture.supplyAsync(() -> {
            Optional<CloudPlayer> fromDb = playerRepository.findByUUID(player.getUniqueId());
            if (fromDb.isPresent()) {
                resetDailyCreationsIfNeeded(fromDb.get());
                cacheManager.putPlayer(fromDb.get());
                return fromDb.get();
            }
            // Primeiro acesso — cria o perfil
            CloudPlayer newPlayer = new CloudPlayer(player.getUniqueId(), player.getName(), Instant.now());
            playerRepository.save(newPlayer);
            cacheManager.putPlayer(newPlayer);
            return newPlayer;
        });
    }

    public Optional<CloudPlayer> getCachedPlayer(UUID uuid) {
        return cacheManager.getPlayer(uuid);
    }

    public CompletableFuture<Optional<CloudPlayer>> getPlayer(UUID uuid) {
        Optional<CloudPlayer> cached = cacheManager.getPlayer(uuid);
        if (cached.isPresent()) return CompletableFuture.completedFuture(cached);

        return CompletableFuture.supplyAsync(() -> {
            Optional<CloudPlayer> fromDb = playerRepository.findByUUID(uuid);
            fromDb.ifPresent(cacheManager::putPlayer);
            return fromDb;
        });
    }

    public void savePlayer(CloudPlayer player) {
        playerRepository.save(player);
        cacheManager.putPlayer(player);
    }

    public void evictPlayer(UUID uuid) {
        cacheManager.invalidatePlayer(uuid);
    }

    /** Reinicia o contador diário de criações se for um novo dia. */
    private void resetDailyCreationsIfNeeded(CloudPlayer player) {
        if (player.getDailyCreationReset() == null) return;
        Instant resetTime = player.getDailyCreationReset();
        if (Instant.now().isAfter(resetTime)) {
            player.setDailyCreations(0);
            player.setDailyCreationReset(Instant.now().plusSeconds(86400));
            playerRepository.save(player);
        }
    }
}
