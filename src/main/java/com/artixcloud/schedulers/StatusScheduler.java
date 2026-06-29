package com.artixcloud.schedulers;

import com.artixcloud.ArtixCloud;
import com.artixcloud.cache.CacheManager;
import com.artixcloud.models.CloudServer;
import com.artixcloud.models.ServerStatus;
import com.artixcloud.services.ServerService;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 * Agendador responsável por atualizar o status dos servidores em tempo real.
 * Roda em background a cada N ticks configurados.
 */
public class StatusScheduler {

    private final ArtixCloud plugin;
    private final ServerService serverService;
    private final CacheManager cacheManager;
    private BukkitTask task;

    // IDs de servidores rastreados (adicionado dinamicamente quando menus estão abertos)
    private final List<String> trackedServerIds = new CopyOnWriteArrayList<>();

    public StatusScheduler(ArtixCloud plugin, ServerService serverService, CacheManager cacheManager) {
        this.plugin        = plugin;
        this.serverService = serverService;
        this.cacheManager  = cacheManager;
    }

    public void start() {
        int intervalTicks = plugin.getConfig().getInt("schedulers.status-update-interval", 200);

        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::tick, intervalTicks, intervalTicks);
        plugin.getLogger().info("StatusScheduler iniciado (intervalo: " + intervalTicks + " ticks).");
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            plugin.getLogger().info("StatusScheduler parado.");
        }
    }

    private void tick() {
        if (trackedServerIds.isEmpty()) return;

        for (String serverId : trackedServerIds) {
            try {
                serverService.refreshServerStatus(serverId).thenAcceptAsync(serverOpt -> {
                    serverOpt.ifPresent(server -> {
                        // Se o servidor completou transição, notifica jogadores com o menu aberto
                        if (!server.getStatus().isTransient()) {
                            notifyStatusChange(server);
                        }
                    });
                });
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Erro ao atualizar status do servidor " + serverId, e);
            }
        }
    }

    private void notifyStatusChange(CloudServer server) {
        // Atualiza menus abertos dos jogadores que estão visualizando este servidor
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            plugin.getServer().getOnlinePlayers().forEach(player -> {
                var menuManager = plugin.getMenuManager();
                var selected = menuManager.getSelectedServer(player.getUniqueId());
                if (selected != null && selected.getId().equals(server.getId())) {
                    var menu = menuManager.getOpenMenu(player);
                    if (menu != null) {
                        // Atualiza o servidor selecionado com dados novos
                        menuManager.setSelectedServer(player.getUniqueId(), server);
                        menu.refresh();
                    }
                }
            });
        });
    }

    public void trackServer(String serverId) {
        if (!trackedServerIds.contains(serverId)) {
            trackedServerIds.add(serverId);
        }
    }

    public void untrackServer(String serverId) {
        trackedServerIds.remove(serverId);
    }

    public void clearTracked() {
        trackedServerIds.clear();
    }
}
