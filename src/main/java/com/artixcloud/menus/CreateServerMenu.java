package com.artixcloud.menus;

import com.artixcloud.ArtixCloud;
import com.artixcloud.models.Plan;
import com.artixcloud.services.PlanService;
import com.artixcloud.utils.ItemBuilder;
import com.artixcloud.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Optional;

/**
 * Menu de configuração de criação de servidor — define versão antes de confirmar.
 */
public class CreateServerMenu extends ArtixMenu {

    private static final String TITLE = "&8» &bCriar Servidor &8«";
    private static final int ROWS = 4;

    private final String planId;
    private final PlanService planService;
    private final MenuManager menuManager;

    public CreateServerMenu(ArtixCloud plugin, Player player, String planId,
                            PlanService planService, MenuManager menuManager) {
        super(plugin, player, TITLE, ROWS);
        this.planId      = planId;
        this.planService = planService;
        this.menuManager = menuManager;
    }

    @Override
    protected void populate() {
        for (int i = 0; i < 36; i++) inventory.setItem(i, ItemBuilder.fillerBlack());

        Optional<Plan> planOpt = planService.findById(planId);
        String planName = planOpt.map(Plan::getDisplayName).orElse(planId);

        // Informações do plano selecionado
        inventory.setItem(4, planOpt.map(plan ->
            new ItemBuilder(plan.getIcon())
                    .name("&b&lPlano Selecionado: " + plan.getDisplayName())
                    .lore(
                        "",
                        "&8▸ &7RAM: &f" + plan.getRam(),
                        "&8▸ &7CPU: &f" + plan.getCpu(),
                        "&8▸ &7Armazenamento: &f" + plan.getStorage(),
                        "&8▸ &7Jogadores: &f" + plan.getMaxPlayers(),
                        "&8▸ &7Preço: &a" + plan.getPrice()
                    )
                    .glow()
                    .hideAll()
                    .build()
        ).orElse(ItemBuilder.filler()));

        // Escolher versão
        inventory.setItem(11, new ItemBuilder(Material.BOOKSHELF)
                .name("&e&lEscolher Versão")
                .lore(
                    "&7Selecione a versão do",
                    "&7Minecraft para seu servidor.",
                    "",
                    "&eClique para escolher"
                )
                .build());

        // Digitar nome via chat
        inventory.setItem(13, new ItemBuilder(Material.NAME_TAG)
                .name("&a&lDefinir Nome")
                .lore(
                    "&7Digite um nome para o",
                    "&7seu servidor no chat.",
                    "",
                    "&73-16 caracteres, letras,",
                    "&7números e hífens.",
                    "",
                    "&eClique para digitar"
                )
                .build());

        // Avançar para confirmação
        inventory.setItem(15, new ItemBuilder(Material.LIME_CONCRETE)
                .name("&a&lPróximo: Confirmar")
                .lore(
                    "&7Avançar para a tela de",
                    "&7confirmação de criação.",
                    "",
                    "&eClique para continuar"
                )
                .glow()
                .build());

        // Voltar
        inventory.setItem(27, ItemBuilder.backButton());
        // Fechar
        inventory.setItem(31, ItemBuilder.closeButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        switch (slot) {
            case 11 -> menuManager.openVersionMenu(player);
            case 13 -> {
                // Aguarda input de nome no chat
                player.closeInventory();
                menuManager.setAwaitingChat(player, true);
                MessageUtil.send(player, "create.enter-name");
            }
            case 15 -> {
                // Só avança se tiver versão definida
                if (menuManager.getPendingVersion(player.getUniqueId()) == null) {
                    MessageUtil.sendRaw(player, "&cEscolha uma versão antes de continuar.");
                    return;
                }
                menuManager.openConfirmMenu(player);
            }
            case 27 -> menuManager.openPlansMenu(player);
            case 31 -> player.closeInventory();
        }
    }
}
