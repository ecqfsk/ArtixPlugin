package com.artixcloud.menus;

import com.artixcloud.ArtixCloud;
import com.artixcloud.models.CloudServer;
import com.artixcloud.services.PlayerService;
import com.artixcloud.services.ServerService;
import com.artixcloud.utils.ItemBuilder;
import com.artixcloud.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Menu principal do ArtixCloud — visão geral dos servidores do jogador.
 */
public class MainMenu extends ArtixMenu {

    private static final String TITLE = "&8» &bArtix&3Cloud &8—&7 Painel Principal";
    private static final int ROWS = 4;

    private final ServerService serverService;
    private final PlayerService playerService;

    public MainMenu(ArtixCloud plugin, org.bukkit.entity.Player player,
                    ServerService serverService, PlayerService playerService) {
        super(plugin, player, TITLE, ROWS);
        this.serverService = serverService;
        this.playerService = playerService;
    }

    @Override
    protected void populate() {
        // Bordas
        fillBorders();

        // Cabeçalho — logo
        inventory.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("&b&lArtix&3Cloud")
                .lore(
                    "&7Infraestrutura de hospedagem Minecraft",
                    "&7integrada diretamente ao jogo.",
                    "",
                    "&ehttps://artixcloud.com"
                )
                .glow()
                .hideAll()
                .build());

        // Meus Servidores
        inventory.setItem(11, new ItemBuilder(Material.ENDER_CHEST)
                .name("&b&lMeus Servidores")
                .lore(
                    "&7Gerencie seus servidores,",
                    "&7inicie, pare ou reinicie.",
                    "",
                    "&eClique para ver seus servidores"
                )
                .build());

        // Criar Servidor
        inventory.setItem(13, new ItemBuilder(Material.COMMAND_BLOCK)
                .name("&a&lCriar Servidor")
                .lore(
                    "&7Crie um novo servidor Minecraft",
                    "&7em segundos com a ArtixCloud.",
                    "",
                    "&eClique para criar um servidor"
                )
                .build());

        // Planos
        inventory.setItem(15, new ItemBuilder(Material.DIAMOND)
                .name("&d&lPlanos")
                .lore(
                    "&7Veja os planos disponíveis",
                    "&7e faça upgrade da sua hospedagem.",
                    "",
                    "&eClique para ver os planos"
                )
                .build());

        // Status da conta
        playerService.getCachedPlayer(player.getUniqueId()).ifPresent(cp ->
            inventory.setItem(22, new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&e&lMinha Conta")
                    .lore(
                        "&7Jogador: &f" + player.getName(),
                        "&7Servidores: &f" + cp.getServerCount(),
                        "",
                        "&7Registrado em ArtixCloud"
                    )
                    .build())
        );

        if (inventory.getItem(22) == null) {
            inventory.setItem(22, new ItemBuilder(Material.PLAYER_HEAD)
                    .name("&e&lMinha Conta")
                    .lore("&7Jogador: &f" + player.getName())
                    .build());
        }

        // Fechar
        inventory.setItem(31, ItemBuilder.closeButton());

        // Website
        inventory.setItem(26, new ItemBuilder(Material.BOOK)
                .name("&f&lSite Oficial")
                .lore(
                    "&7https://artixcloud.com",
                    "",
                    "&eAcesse nosso painel web"
                )
                .build());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case 11 -> {
                // Meus Servidores — carrega e abre lista
                player.closeInventory();
                MessageUtil.sendRaw(player, "&7Carregando seus servidores...");
                serverService.getServers(player.getUniqueId())
                        .thenAcceptAsync(servers -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                            if (servers.isEmpty()) {
                                MessageUtil.send(player, "server.no-servers");
                                plugin.getMenuManager().openMainMenu(player);
                            } else if (servers.size() == 1) {
                                plugin.getMenuManager().openServerMenu(player, servers.get(0));
                            } else {
                                openServerListMenu(servers);
                            }
                        }));
            }
            case 13 -> plugin.getMenuManager().openPlansMenu(player);
            case 15 -> plugin.getMenuManager().openPlansMenu(player);
            case 31 -> player.closeInventory();
        }
    }

    private void openServerListMenu(List<CloudServer> servers) {
        // Abre o primeiro servidor por enquanto (lista expandida futura)
        plugin.getMenuManager().openServerMenu(player, servers.get(0));
    }

    private void fillBorders() {
        ItemStack filler = ItemBuilder.fillerBlack();
        // Linha 0 e última
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
            inventory.setItem(27 + i, filler);
        }
        // Laterais
        inventory.setItem(9,  filler);
        inventory.setItem(17, filler);
        inventory.setItem(18, filler);
        inventory.setItem(26, null); // reservado para botão
    }
}
