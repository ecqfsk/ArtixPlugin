package com.artixcloud.menus;

import com.artixcloud.ArtixCloud;
import com.artixcloud.models.CloudServer;
import com.artixcloud.models.ServerStatus;
import com.artixcloud.services.ServerService;
import com.artixcloud.utils.ItemBuilder;
import com.artixcloud.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;

/**
 * Menu de gerenciamento de um servidor específico.
 * Exibe recursos em tempo real e permite ações (iniciar, parar, reiniciar, excluir).
 */
public class ServerMenu extends ArtixMenu {

    private static final String TITLE = "&8» &bGerenciar Servidor &8«";
    private static final int ROWS = 5;

    private final CloudServer server;
    private final ServerService serverService;
    private final MenuManager menuManager;

    public ServerMenu(ArtixCloud plugin, Player player, CloudServer server,
                      ServerService serverService, MenuManager menuManager) {
        super(plugin, player, TITLE, ROWS);
        this.server        = server;
        this.serverService = serverService;
        this.menuManager   = menuManager;
    }

    @Override
    protected void populate() {
        // Preenche bordas
        for (int i = 0; i < 45; i++) inventory.setItem(i, ItemBuilder.fillerBlack());

        // Informações do servidor (centro-topo)
        inventory.setItem(4, buildServerInfoItem());

        // Status em tempo real
        inventory.setItem(13, buildStatusItem());

        // ── Ações ────────────────────────────────────────────────────
        boolean isRunning = server.getStatus().isRunning();
        boolean isTransient = server.getStatus().isTransient();

        // Iniciar
        inventory.setItem(20, new ItemBuilder(Material.LIME_DYE)
                .name(!isRunning ? "&a&lIniciar Servidor" : "&7&lJá em execução")
                .lore(
                    "&7Clique para iniciar o servidor.",
                    "",
                    !isRunning ? "&eClique para iniciar" : "&8Servidor já está online"
                )
                .build());

        // Parar
        inventory.setItem(22, new ItemBuilder(Material.RED_DYE)
                .name(isRunning ? "&c&lParar Servidor" : "&7&lServidor está parado")
                .lore(
                    "&7Clique para parar o servidor.",
                    "",
                    isRunning ? "&eClique para parar" : "&8Servidor já está offline"
                )
                .build());

        // Reiniciar
        inventory.setItem(24, new ItemBuilder(Material.YELLOW_DYE)
                .name(!isTransient ? "&e&lReiniciar Servidor" : "&7&lAguardando...")
                .lore(
                    "&7Reinicia o servidor mantendo",
                    "&7todos os dados.",
                    "",
                    !isTransient ? "&eClique para reiniciar" : "&8Operação em andamento..."
                )
                .build());

        // IP de Conexão
        String address = server.getConnectionAddress();
        inventory.setItem(29, new ItemBuilder(Material.COMPASS)
                .name("&b&lIP de Conexão")
                .lore(
                    "&7IP: &f" + (address.equals("N/A") ? "&8Indisponível" : address),
                    "",
                    "&eClique para copiar o IP"
                )
                .build());

        // Recursos
        inventory.setItem(31, new ItemBuilder(Material.COMPARATOR)
                .name("&f&lRecursos")
                .lore(
                    "&8▸ &7RAM: &f" + String.format("%.0f", server.getRamUsedMb()) + "MB"
                            + " &8/ &f" + String.format("%.0f", server.getRamTotalMb()) + "MB"
                            + " &8(&e" + server.getRamPercent() + "%&8)",
                    "&8▸ &7CPU: &f" + String.format("%.1f", server.getCpuPercent()) + "%",
                    "&8▸ &7Armazenamento: &f" + String.format("%.0f", server.getStorageMb()) + "MB",
                    "&8▸ &7Jogadores: &f" + server.getPlayersOnline() + "&8/&f" + server.getMaxPlayers(),
                    "&8▸ &7Uptime: &f" + server.getFormattedUptime(),
                    "&8▸ &7Versão: &f" + server.getVersion()
                )
                .hideAll()
                .build());

        // Excluir
        inventory.setItem(33, new ItemBuilder(Material.TNT)
                .name("&c&lExcluir Servidor")
                .lore(
                    "&cATENÇÃO: Esta ação é irreversível!",
                    "&7Todos os dados do servidor serão",
                    "&7permanentemente apagados.",
                    "",
                    "&c&lClique para excluir"
                )
                .build());

        // Botões de navegação
        inventory.setItem(36, ItemBuilder.backButton());
        inventory.setItem(40, ItemBuilder.closeButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        boolean isRunning  = server.getStatus().isRunning();
        boolean isTransient = server.getStatus().isTransient();

        switch (slot) {
            case 20 -> { // Iniciar
                if (!isRunning && !isTransient) {
                    player.closeInventory();
                    MessageUtil.send(player, "server.starting", Map.of("server", server.getName()));
                    serverService.startServer(server.getId()).thenAcceptAsync(ok ->
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (ok) MessageUtil.send(player, "server.started", Map.of("server", server.getName()));
                            else    MessageUtil.send(player, "server.start-failed", Map.of("server", server.getName()));
                        })
                    );
                }
            }
            case 22 -> { // Parar
                if (isRunning && !isTransient) {
                    player.closeInventory();
                    MessageUtil.send(player, "server.stopping", Map.of("server", server.getName()));
                    serverService.stopServer(server.getId()).thenAcceptAsync(ok ->
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (ok) MessageUtil.send(player, "server.stopped", Map.of("server", server.getName()));
                            else    MessageUtil.send(player, "server.stop-failed", Map.of("server", server.getName()));
                        })
                    );
                }
            }
            case 24 -> { // Reiniciar
                if (!isTransient) {
                    player.closeInventory();
                    MessageUtil.send(player, "server.restarting", Map.of("server", server.getName()));
                    serverService.restartServer(server.getId()).thenAcceptAsync(ok ->
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (ok) MessageUtil.send(player, "server.restarted", Map.of("server", server.getName()));
                            else    MessageUtil.send(player, "server.restart-failed", Map.of("server", server.getName()));
                        })
                    );
                }
            }
            case 29 -> { // Copiar IP
                String address = server.getConnectionAddress();
                if (!address.equals("N/A")) {
                    player.sendMessage(MessageUtil.color("&aIP copiado: &e" + address));
                }
            }
            case 33 -> { // Excluir
                player.closeInventory();
                openDeleteConfirmation();
            }
            case 36 -> menuManager.openMainMenu(player);
            case 40 -> player.closeInventory();
        }
    }

    private void openDeleteConfirmation() {
        // Menu de confirmação de exclusão
        var confirm = new ArtixMenu(plugin, player, "&8» &c&lConfirmar Exclusão &8«", 3) {
            @Override
            protected void populate() {
                for (int i = 0; i < 27; i++) setItem(i, ItemBuilder.fillerBlack());

                inventory.setItem(11, new ItemBuilder(Material.LIME_CONCRETE)
                        .name("&a&lSIM, excluir!")
                        .lore(
                            "&7Confirmar exclusão do servidor",
                            "&e" + server.getName(),
                            "",
                            "&cEsta ação é irreversível!"
                        )
                        .glow()
                        .build());

                inventory.setItem(13, new ItemBuilder(Material.PAPER)
                        .name("&f&lServidor: &e" + server.getName())
                        .lore(
                            "&7Plano: &f" + server.getPlanId(),
                            "&7Versão: &f" + server.getVersion(),
                            "&7Status: " + server.getStatus().getDisplayName()
                        )
                        .hideAll()
                        .build());

                inventory.setItem(15, ItemBuilder.cancelButton());
            }

            private void setItem(int slot, org.bukkit.inventory.ItemStack item) {
                inventory.setItem(slot, item);
            }

            @Override
            public void handleClick(InventoryClickEvent event) {
                event.setCancelled(true);
                if (event.getRawSlot() == 11) {
                    player.closeInventory();
                    MessageUtil.send(player, "server.deleting", Map.of("server", server.getName()));
                    serverService.deleteServer(player, server.getId()).thenAcceptAsync(ok ->
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (ok) MessageUtil.send(player, "server.deleted", Map.of("server", server.getName()));
                            else    MessageUtil.send(player, "server.delete-failed", Map.of("server", server.getName()));
                        })
                    );
                } else if (event.getRawSlot() == 15) {
                    menuManager.openServerMenu(player, server);
                }
            }
        };

        menuManager.openMenus_put(player, confirm);
        confirm.open();
    }

    private ItemStack buildServerInfoItem() {
        return new ItemBuilder(Material.ENDER_CHEST)
                .name("&b&l" + server.getName())
                .lore(
                    "&7ID: &8" + server.getId(),
                    "&7Plano: &f" + server.getPlanId(),
                    "&7Versão: &f" + server.getVersion(),
                    "",
                    "&7Status: " + server.getStatus().getDisplayName()
                )
                .glow()
                .hideAll()
                .build();
    }

    private ItemStack buildStatusItem() {
        ServerStatus status = server.getStatus();
        Material mat = switch (status) {
            case ONLINE     -> Material.GREEN_CONCRETE;
            case OFFLINE    -> Material.RED_CONCRETE;
            case STARTING   -> Material.YELLOW_CONCRETE;
            case STOPPING   -> Material.ORANGE_CONCRETE;
            case RESTARTING -> Material.CYAN_CONCRETE;
            case CREATING   -> Material.BLUE_CONCRETE;
            case ERROR      -> Material.MAGENTA_CONCRETE;
            default         -> Material.GRAY_CONCRETE;
        };

        return new ItemBuilder(mat)
                .name("&f&lStatus: " + status.getDisplayName())
                .lore(
                    "&7Última atualização:",
                    "&8" + (server.getUpdatedAt() != null ? server.getUpdatedAt().toString() : "N/A")
                )
                .hideAll()
                .build();
    }
}
