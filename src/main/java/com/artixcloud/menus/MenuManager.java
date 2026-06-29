package com.artixcloud.menus;

import com.artixcloud.ArtixCloud;
import com.artixcloud.models.CloudServer;
import com.artixcloud.services.PlayerService;
import com.artixcloud.services.PlanService;
import com.artixcloud.services.ServerService;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gerencia o estado dos menus abertos por jogador e centraliza a abertura de menus.
 */
public class MenuManager {

    private final ArtixCloud plugin;
    private final ServerService serverService;
    private final PlayerService playerService;
    private final PlanService planService;

    // Rastreia qual menu cada jogador tem aberto (para roteamento de cliques)
    private final Map<UUID, ArtixMenu> openMenus = new HashMap<>();

    // Rastreia qual servidor está sendo gerenciado por cada jogador
    private final Map<UUID, CloudServer> selectedServers = new HashMap<>();

    // Rastreia o plano e versão selecionados durante a criação
    private final Map<UUID, String> pendingPlanId  = new HashMap<>();
    private final Map<UUID, String> pendingVersion = new HashMap<>();

    // Aguardando input de texto no chat
    private final Map<UUID, Boolean> awaitingChatInput = new HashMap<>();

    public MenuManager(ArtixCloud plugin, ServerService serverService,
                       PlayerService playerService, PlanService planService) {
        this.plugin        = plugin;
        this.serverService = serverService;
        this.playerService = playerService;
        this.planService   = planService;
    }

    // ── Abertura de menus ─────────────────────────────────────────────────────

    public void openMainMenu(Player player) {
        MainMenu menu = new MainMenu(plugin, player, serverService, playerService);
        menu.open();
        openMenus.put(player.getUniqueId(), menu);
    }

    public void openPlansMenu(Player player) {
        PlansMenu menu = new PlansMenu(plugin, player, planService, this);
        menu.open();
        openMenus.put(player.getUniqueId(), menu);
    }

    public void openServerMenu(Player player, CloudServer server) {
        selectedServers.put(player.getUniqueId(), server);
        ServerMenu menu = new ServerMenu(plugin, player, server, serverService, this);
        menu.open();
        openMenus.put(player.getUniqueId(), menu);
    }

    public void openCreateMenu(Player player, String planId) {
        pendingPlanId.put(player.getUniqueId(), planId);
        CreateServerMenu menu = new CreateServerMenu(plugin, player, planId, planService, this);
        menu.open();
        openMenus.put(player.getUniqueId(), menu);
    }

    public void openVersionMenu(Player player) {
        VersionMenu menu = new VersionMenu(plugin, player, this);
        menu.open();
        openMenus.put(player.getUniqueId(), menu);
    }

    public void openConfirmMenu(Player player) {
        String planId  = pendingPlanId.getOrDefault(player.getUniqueId(), "free");
        String version = pendingVersion.getOrDefault(player.getUniqueId(), "1.21.1");
        ConfirmMenu menu = new ConfirmMenu(plugin, player, planId, version, serverService, playerService, planService, this);
        menu.open();
        openMenus.put(player.getUniqueId(), menu);
    }

    // ── Roteamento de cliques ─────────────────────────────────────────────────

    public ArtixMenu getOpenMenu(Player player) {
        return openMenus.get(player.getUniqueId());
    }

    public void closeMenu(Player player) {
        openMenus.remove(player.getUniqueId());
    }

    /** Registra um menu aberto externamente (usado por sub-menus inline). */
    public void openMenus_put(Player player, ArtixMenu menu) {
        openMenus.put(player.getUniqueId(), menu);
    }

    // ── Estado de criação ─────────────────────────────────────────────────────

    public void setPendingPlan(UUID uuid, String planId)     { pendingPlanId.put(uuid, planId); }
    public void setPendingVersion(UUID uuid, String version) { pendingVersion.put(uuid, version); }
    public String getPendingPlan(UUID uuid)                  { return pendingPlanId.get(uuid); }
    public String getPendingVersion(UUID uuid)               { return pendingVersion.get(uuid); }

    public void clearPending(UUID uuid) {
        pendingPlanId.remove(uuid);
        pendingVersion.remove(uuid);
    }

    public CloudServer getSelectedServer(UUID uuid)               { return selectedServers.get(uuid); }
    public void setSelectedServer(UUID uuid, CloudServer server)  { selectedServers.put(uuid, server); }

    public boolean isAwaitingChat(Player player)            { return awaitingChatInput.getOrDefault(player.getUniqueId(), false); }
    public void setAwaitingChat(Player player, boolean val) { awaitingChatInput.put(player.getUniqueId(), val); }

    public void cleanup(UUID uuid) {
        openMenus.remove(uuid);
        selectedServers.remove(uuid);
        pendingPlanId.remove(uuid);
        pendingVersion.remove(uuid);
        awaitingChatInput.remove(uuid);
    }
}
