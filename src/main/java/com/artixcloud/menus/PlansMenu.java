package com.artixcloud.menus;

import com.artixcloud.ArtixCloud;
import com.artixcloud.models.Plan;
import com.artixcloud.services.PlanService;
import com.artixcloud.utils.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu para visualização e seleção de planos de hospedagem.
 */
public class PlansMenu extends ArtixMenu {

    private static final String TITLE = "&8» &bEscolher Plano de Hospedagem &8«";
    private static final int ROWS = 6;

    private final PlanService planService;
    private final MenuManager menuManager;
    private final Map<Integer, Plan> slotToPlan = new HashMap<>();

    public PlansMenu(ArtixCloud plugin, Player player, PlanService planService, MenuManager menuManager) {
        super(plugin, player, TITLE, ROWS);
        this.planService = planService;
        this.menuManager = menuManager;
    }

    @Override
    protected void populate() {
        // Preenche todas as bordas
        for (int i = 0; i < 54; i++) inventory.setItem(i, ItemBuilder.fillerBlack());

        // Título decorativo
        inventory.setItem(4, new ItemBuilder(Material.NETHER_STAR)
                .name("&b&lEscolha seu Plano")
                .lore(
                    "&7Selecione o plano ideal para",
                    "&7o seu servidor Minecraft.",
                    "",
                    "&aPlano Gratuito disponível!"
                )
                .glow()
                .hideAll()
                .build());

        // Exibe os planos em slots centrais
        List<Plan> plans = planService.getPlans();
        int[] planSlots = {19, 21, 23, 25, 31}; // 5 slots centrais
        for (int i = 0; i < plans.size() && i < planSlots.length; i++) {
            Plan plan = plans.get(i);
            int slot = planSlots[i];

            ItemBuilder builder = new ItemBuilder(plan.getIcon())
                    .name(plan.getDisplayName() + " &8— " + plan.getPrice())
                    .lore(
                        "",
                        "&7&lEspecificações:",
                        "&8▸ &7RAM: &f" + plan.getRam(),
                        "&8▸ &7CPU: &f" + plan.getCpu(),
                        "&8▸ &7Armazenamento: &f" + plan.getStorage(),
                        "&8▸ &7Jogadores: &f" + plan.getMaxPlayers(),
                        "",
                        "&8▸ &7Preço: &a" + plan.getPrice(),
                        "",
                        plan.isFree() ? "&aGratuito — Clique para criar!" : "&eClique para selecionar"
                    );

            if (plan.isFree()) builder.glow();
            slotToPlan.put(slot, plan);
            inventory.setItem(slot, builder.build());
        }

        // Voltar
        inventory.setItem(45, ItemBuilder.backButton());
        // Fechar
        inventory.setItem(49, ItemBuilder.closeButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 45) {
            menuManager.openMainMenu(player);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        Plan plan = slotToPlan.get(slot);
        if (plan != null) {
            menuManager.setPendingPlan(player.getUniqueId(), plan.getId());
            menuManager.openCreateMenu(player, plan.getId());
        }
    }
}
