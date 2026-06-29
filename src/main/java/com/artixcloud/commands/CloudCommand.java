package com.artixcloud.commands;

import com.artixcloud.ArtixCloud;
import com.artixcloud.menus.MenuManager;
import com.artixcloud.services.PlayerService;
import com.artixcloud.services.ServerService;
import com.artixcloud.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Executor do comando /cloud — ponto de entrada principal do plugin.
 *
 * Subcomandos:
 *   /cloud               — abre menu principal
 *   /cloud criar         — atalho para menu de criação
 *   /cloud status <id>   — exibe status de um servidor
 *   /cloud reload        — recarrega configurações (op)
 *   /cloud help          — lista subcomandos
 */
public class CloudCommand implements CommandExecutor {

    private final ArtixCloud plugin;
    private final MenuManager menuManager;
    private final ServerService serverService;
    private final PlayerService playerService;

    public CloudCommand(ArtixCloud plugin, MenuManager menuManager,
                        ServerService serverService, PlayerService playerService) {
        this.plugin        = plugin;
        this.menuManager   = menuManager;
        this.serverService = serverService;
        this.playerService = playerService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MessageUtil.color("&cApenas jogadores podem usar este comando."));
            return true;
        }

        if (!player.hasPermission("artixcloud.use")) {
            MessageUtil.send(player, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            openPanel(player);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "criar", "create", "new" -> {
                menuManager.openPlansMenu(player);
                yield true;
            }
            case "status" -> {
                if (args.length < 2) {
                    MessageUtil.sendRaw(player, "&cUso: /cloud status <id-do-servidor>");
                    yield true;
                }
                handleStatus(player, args[1]);
                yield true;
            }
            case "reload" -> {
                if (!player.hasPermission("artixcloud.admin")) {
                    MessageUtil.send(player, "general.no-permission");
                    yield true;
                }
                plugin.reloadConfig();
                MessageUtil.load(plugin);
                plugin.getPlanService().loadFromConfig();
                MessageUtil.sendRaw(player, "&aConfiguração recarregada com sucesso!");
                yield true;
            }
            case "help", "ajuda" -> {
                sendHelp(player, label);
                yield true;
            }
            default -> {
                openPanel(player);
                yield true;
            }
        };
    }

    private void openPanel(Player player) {
        MessageUtil.send(player, "cloud.open-menu");
        playerService.getOrCreatePlayer(player).thenAcceptAsync(cp ->
            plugin.getServer().getScheduler().runTask(plugin, () ->
                menuManager.openMainMenu(player)
            )
        );
    }

    private void handleStatus(Player player, String serverId) {
        MessageUtil.sendRaw(player, "&7Consultando status do servidor...");
        serverService.refreshServerStatus(serverId).thenAcceptAsync(serverOpt ->
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (serverOpt.isEmpty()) {
                    MessageUtil.send(player, "server.not-found");
                    return;
                }
                var server = serverOpt.get();
                MessageUtil.sendSeparator(player);
                player.sendMessage(MessageUtil.color("&b&lServidor: &f" + server.getName()));
                player.sendMessage(MessageUtil.color("&7Status: " + server.getStatus().getDisplayName()));
                player.sendMessage(MessageUtil.color("&7IP: &f" + server.getConnectionAddress()));
                player.sendMessage(MessageUtil.color("&7Jogadores: &f" + server.getPlayersOnline() + "&8/&f" + server.getMaxPlayers()));
                player.sendMessage(MessageUtil.color("&7RAM: &f" + String.format("%.0f", server.getRamUsedMb()) + "MB &8/ &f" + String.format("%.0f", server.getRamTotalMb()) + "MB"));
                player.sendMessage(MessageUtil.color("&7CPU: &f" + String.format("%.1f", server.getCpuPercent()) + "%"));
                player.sendMessage(MessageUtil.color("&7Uptime: &f" + server.getFormattedUptime()));
                MessageUtil.sendSeparator(player);
            })
        );
    }

    private void sendHelp(Player player, String label) {
        MessageUtil.sendSeparator(player);
        player.sendMessage(MessageUtil.color("&b&lArtixCloud &7— Comandos:"));
        player.sendMessage(MessageUtil.color("&e/" + label + " &7— Abre o painel principal"));
        player.sendMessage(MessageUtil.color("&e/" + label + " criar &7— Cria um novo servidor"));
        player.sendMessage(MessageUtil.color("&e/" + label + " status <id> &7— Status de um servidor"));
        player.sendMessage(MessageUtil.color("&e/" + label + " help &7— Exibe esta ajuda"));
        if (player.hasPermission("artixcloud.admin")) {
            player.sendMessage(MessageUtil.color("&e/" + label + " reload &7— Recarrega configurações"));
        }
        MessageUtil.sendSeparator(player);
    }
}
