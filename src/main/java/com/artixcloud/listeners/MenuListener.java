package com.artixcloud.listeners;

import com.artixcloud.menus.ArtixMenu;
import com.artixcloud.menus.MenuManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Intercepta eventos de inventário e roteia cliques para o menu correto.
 */
public class MenuListener implements Listener {

    private final MenuManager menuManager;

    public MenuListener(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ArtixMenu menu = menuManager.getOpenMenu(player);
        if (menu == null) return;

        // Verifica se o clique foi dentro do inventário do menu
        if (!event.getInventory().equals(menu.getInventory())) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;

        menu.handleClick(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ArtixMenu menu = menuManager.getOpenMenu(player);
        if (menu == null) return;

        if (event.getInventory().equals(menu.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        menuManager.closeMenu(player);
    }
}
