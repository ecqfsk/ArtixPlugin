package com.artixcloud.menus;

import com.artixcloud.ArtixCloud;
import com.artixcloud.models.CloudPlayer;
import com.artixcloud.models.Plan;
import com.artixcloud.services.PlayerService;
import com.artixcloud.services.PlanService;
import com.artixcloud.services.ServerService;
import com.artixcloud.utils.ItemBuilder;
import com.artixcloud.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.Optional;

/**
 * Menu de confirmação final antes de criar o servidor.
 * Exibe resumo das escolhas e valida elegibilidade.
 */
public class ConfirmMenu extends ArtixMenu {

    private static final String TITLE = "&8» &bConfirmar Criação &8«";
    private static final int ROWS = 4;

    private final String planId;
    private final String version;
    private final ServerService serverService;
    private final PlayerService playerService;
    private final PlanService planService;
    private final MenuManager menuManager;

    // Nome digitado pelo jogador (definido via MenuManager)
    private String serverName;

    public ConfirmMenu(ArtixCloud plugin, Player player,
                       String planId, String version,
                       ServerService serverService, PlayerService playerService,
                       PlanService planService, MenuManager menuManager) {
        super(plugin, player, TITLE, ROWS);
        this.planId        = planId;
        this.version       = version;
        this.serverService = serverService;
        this.playerService = playerService;
        this.planService   = planService;
        this.menuManager   = menuManager;
        this.serverName    = "MeuServidor";
    }

    public void setServerName(String name) { this.serverName = name; }

    @Override
    protected void populate() {
        for (int i = 0; i < 36; i++) inventory.setItem(i, ItemBuilder.fillerBlack());

        Optional<Plan> planOpt = planService.findById(planId);
        String planName = planOpt.map(Plan::getDisplayName).orElse(planId);

        // Resumo da criação
        inventory.setItem(4, new ItemBuilder(Material.PAPER)
                .name("&f&lResumo da Criação")
                .lore(
                    "&8▸ &7Nome: &e" + serverName,
                    "&8▸ &7Plano: " + planName,
                    "&8▸ &7Versão: &fMinecraft " + version,
                    "",
                    "&7Revise as informações antes",
                    "&7de confirmar a criação."
                )
                .glow()
                .hideAll()
                .build());

        // Confirmar
        inventory.setItem(11, new ItemBuilder(Material.LIME_CONCRETE)
                .name("&a&lCONFIRMAR CRIAÇÃO")
                .lore(
                    "&7Servidor: &e" + serverName,
                    "&7Plano: " + planName,
                    "&7Versão: &fMinecraft " + version,
                    "",
                    "&aClique para criar o servidor!"
                )
                .glow()
                .build());

        // Editar nome
        inventory.setItem(13, new ItemBuilder(Material.NAME_TAG)
                .name("&e&lAlterar Nome")
                .lore(
                    "&7Nome atual: &e" + serverName,
                    "",
                    "&eClique para alterar"
                )
                .build());

        // Cancelar
        inventory.setItem(15, new ItemBuilder(Material.RED_CONCRETE)
                .name("&c&lCANCELAR")
                .lore(
                    "&7Cancelar criação e",
                    "&7voltar ao menu principal.",
                    "",
                    "&cClique para cancelar"
                )
                .build());

        // Voltar
        inventory.setItem(27, ItemBuilder.backButton());
        inventory.setItem(31, ItemBuilder.closeButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case 11 -> confirmCreation();
            case 13 -> changeNameViaChat();
            case 15 -> {
                menuManager.clearPending(player.getUniqueId());
                menuManager.openMainMenu(player);
            }
            case 27 -> menuManager.openCreateMenu(player, planId);
            case 31 -> player.closeInventory();
        }
    }

    private void confirmCreation() {
        playerService.getOrCreatePlayer(player).thenAcceptAsync(cloudPlayer -> {
            // Valida elegibilidade
            if (!validateEligibility(cloudPlayer)) return;

            player.closeInventory();
            MessageUtil.send(player, "server.creating", Map.of("server", serverName));

            // Cria o servidor na API
            serverService.createServer(player, serverName, planId, version)
                    .thenAcceptAsync(serverOpt -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (serverOpt.isPresent()) {
                                MessageUtil.send(player, "server.created", Map.of("server", serverName));
                                menuManager.clearPending(player.getUniqueId());
                            } else {
                                MessageUtil.send(player, "server.creation-failed");
                            }
                        });
                    });
        });
    }

    private boolean validateEligibility(CloudPlayer cloudPlayer) {
        // Verifica limite de servidores gratuitos
        if (planId.equalsIgnoreCase("free")
                && !player.hasPermission("artixcloud.bypass.limit")
                && !serverService.canCreateFreeServer(cloudPlayer)) {
            int maxFree = plugin.getConfig().getInt("free-plan.max-servers-per-player", 1);
            plugin.getServer().getScheduler().runTask(plugin, () ->
                MessageUtil.send(player, "plan.free-limit", Map.of("limit", String.valueOf(maxFree)))
            );
            return false;
        }

        // Verifica cooldown
        if (!player.hasPermission("artixcloud.bypass.cooldown")
                && serverService.isInCooldown(cloudPlayer)) {
            String remaining = serverService.getCooldownRemaining(cloudPlayer);
            plugin.getServer().getScheduler().runTask(plugin, () ->
                MessageUtil.send(player, "plan.cooldown", Map.of("time", remaining))
            );
            return false;
        }

        return true;
    }

    private void changeNameViaChat() {
        player.closeInventory();
        menuManager.setAwaitingChat(player, true);
        MessageUtil.send(player, "create.enter-name");
    }
}
