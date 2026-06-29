package com.artixcloud.menus;

import com.artixcloud.ArtixCloud;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/**
 * Classe base abstrata para todos os menus GUI do ArtixCloud.
 */
public abstract class ArtixMenu {

    protected final ArtixCloud plugin;
    protected final Player player;
    protected Inventory inventory;

    protected ArtixMenu(ArtixCloud plugin, Player player, String title, int rows) {
        this.plugin = plugin;
        this.player = player;
        Component titleComponent = LegacyComponentSerializer.legacySection()
                .deserialize(title.replace("&", "§"));
        this.inventory = Bukkit.createInventory(null, rows * 9, titleComponent);
    }

    /** Popula o inventário com os itens. Chamado antes de abrir. */
    protected abstract void populate();

    /** Trata o clique do jogador no menu. */
    public abstract void handleClick(InventoryClickEvent event);

    /** Abre o menu para o jogador. */
    public void open() {
        populate();
        player.openInventory(inventory);
    }

    /** Atualiza os itens do menu sem fechar e reabrir. */
    public void refresh() {
        inventory.clear();
        populate();
        player.updateInventory();
    }

    public Inventory getInventory() { return inventory; }
}
