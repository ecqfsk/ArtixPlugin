package com.artixcloud.listeners;

import com.artixcloud.ArtixCloud;
import com.artixcloud.services.PlayerService;
import com.artixcloud.utils.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Intercepta eventos de jogador para registro e input de texto no chat.
 */
public class PlayerListener implements Listener {

    private final ArtixCloud plugin;
    private final PlayerService playerService;

    public PlayerListener(ArtixCloud plugin, PlayerService playerService) {
        this.plugin        = plugin;
        this.playerService = playerService;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Carrega ou cria perfil do jogador de forma assíncrona
        playerService.getOrCreatePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Limpa cache do jogador quando sair
        playerService.evictPlayer(event.getPlayer().getUniqueId());
        plugin.getMenuManager().cleanup(event.getPlayer().getUniqueId());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();
        var menuManager = plugin.getMenuManager();

        if (!menuManager.isAwaitingChat(player)) return;

        event.setCancelled(true);
        menuManager.setAwaitingChat(player, false);

        String input = event.getMessage().trim();

        // Valida o nome
        if (input.equalsIgnoreCase("cancelar") || input.equalsIgnoreCase("cancel")) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                MessageUtil.send(player, "general.cancelled");
                menuManager.openMainMenu(player);
            });
            return;
        }

        if (!plugin.getServerService().isNameValid(input)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                MessageUtil.send(player, "create.name-invalid");
                menuManager.setAwaitingChat(player, true);
                MessageUtil.send(player, "create.enter-name");
            });
            return;
        }

        if (input.length() > 16) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                MessageUtil.send(player, "create.name-too-long");
                menuManager.setAwaitingChat(player, true);
                MessageUtil.send(player, "create.enter-name");
            });
            return;
        }

        // Nome válido — abre menu de confirmação com o nome
        final String serverName = input;
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            // Garante que planId e version estejam definidos antes de abrir
            String planId  = menuManager.getPendingPlan(player.getUniqueId());
            String version = menuManager.getPendingVersion(player.getUniqueId());

            if (planId == null) planId = "free";
            if (version == null) version = "1.21.1";

            var confirm = new com.artixcloud.menus.ConfirmMenu(
                    plugin, player, planId, version,
                    plugin.getServerService(), plugin.getPlayerService(),
                    plugin.getPlanService(), menuManager
            );
            confirm.setServerName(serverName);
            menuManager.openMenus_put(player, confirm);
            confirm.open();
        });
    }
}
