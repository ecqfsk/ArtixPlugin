package com.artixcloud.menus;

import com.artixcloud.ArtixCloud;
import com.artixcloud.utils.ItemBuilder;
import com.artixcloud.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Menu de seleção de versão do Minecraft para o servidor a ser criado.
 */
public class VersionMenu extends ArtixMenu {

    private static final String TITLE = "&8» &bEscolher Versão &8«";
    private static final int ROWS = 4;

    private final MenuManager menuManager;
    private final Map<Integer, String> slotToVersion = new HashMap<>();

    public VersionMenu(ArtixCloud plugin, Player player, MenuManager menuManager) {
        super(plugin, player, TITLE, ROWS);
        this.menuManager = menuManager;
    }

    @Override
    protected void populate() {
        for (int i = 0; i < 36; i++) inventory.setItem(i, ItemBuilder.fillerBlack());

        List<String> versions = plugin.getConfig().getStringList("versions");
        if (versions.isEmpty()) {
            versions = List.of("1.21.1", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.17.1", "1.16.5", "1.12.2", "1.8.8");
        }

        // Título
        inventory.setItem(4, new ItemBuilder(Material.COMPASS)
                .name("&b&lEscolher Versão do Minecraft")
                .lore("&7Selecione a versão que será", "&7instalada no seu servidor.")
                .glow()
                .hideAll()
                .build());

        // Mapeamento de versões para ícones
        Map<String, Material> icons = Map.of(
            "1.21.1", Material.NETHERITE_BLOCK,
            "1.20.4", Material.DIAMOND_BLOCK,
            "1.20.1", Material.DIAMOND_BLOCK,
            "1.19.4", Material.EMERALD_BLOCK,
            "1.18.2", Material.IRON_BLOCK,
            "1.17.1", Material.COPPER_BLOCK,
            "1.16.5", Material.CRIMSON_PLANKS,
            "1.12.2", Material.SPRUCE_PLANKS,
            "1.8.8",  Material.OAK_PLANKS
        );

        int[] versionSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20};
        for (int i = 0; i < versions.size() && i < versionSlots.length; i++) {
            String version = versions.get(i);
            int slot = versionSlots[i];
            Material icon = icons.getOrDefault(version, Material.GRASS_BLOCK);

            inventory.setItem(slot, new ItemBuilder(icon)
                    .name("&e&lMinecraft " + version)
                    .lore(
                        "&7Versão: &f" + version,
                        "",
                        "&aClique para selecionar"
                    )
                    .build());
            slotToVersion.put(slot, version);
        }

        // Voltar
        inventory.setItem(27, ItemBuilder.backButton());
        inventory.setItem(31, ItemBuilder.closeButton());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();

        if (slot == 27) {
            String planId = menuManager.getPendingPlan(player.getUniqueId());
            if (planId != null) menuManager.openCreateMenu(player, planId);
            else menuManager.openMainMenu(player);
            return;
        }
        if (slot == 31) { player.closeInventory(); return; }

        String version = slotToVersion.get(slot);
        if (version != null) {
            menuManager.setPendingVersion(player.getUniqueId(), version);
            MessageUtil.send(player, "create.version-selected", Map.of("version", version));
            String planId = menuManager.getPendingPlan(player.getUniqueId());
            if (planId != null) menuManager.openCreateMenu(player, planId);
        }
    }
}
