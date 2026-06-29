package com.artixcloud;

import com.artixcloud.api.APIClient;
import com.artixcloud.cache.CacheManager;
import com.artixcloud.commands.CloudCommand;
import com.artixcloud.commands.CloudTabCompleter;
import com.artixcloud.database.DatabaseManager;
import com.artixcloud.listeners.MenuListener;
import com.artixcloud.listeners.PlayerListener;
import com.artixcloud.menus.MenuManager;
import com.artixcloud.repositories.PlayerRepository;
import com.artixcloud.repositories.ServerRepository;
import com.artixcloud.schedulers.StatusScheduler;
import com.artixcloud.services.PlayerService;
import com.artixcloud.services.PlanService;
import com.artixcloud.services.ServerService;
import com.artixcloud.utils.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * ArtixCloud — Plugin principal.
 * Ponto de entrada e gerenciador do ciclo de vida de todos os componentes.
 */
public final class ArtixCloud extends JavaPlugin {

    private static ArtixCloud instance;

    // Infraestrutura
    private DatabaseManager databaseManager;
    private APIClient apiClient;
    private CacheManager cacheManager;

    // Repositórios
    private PlayerRepository playerRepository;
    private ServerRepository serverRepository;

    // Serviços
    private PlayerService playerService;
    private ServerService serverService;
    private PlanService planService;

    // UI
    private MenuManager menuManager;

    // Agendadores
    private StatusScheduler statusScheduler;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("§b===================================");
        getLogger().info("§b  ArtixCloud Plugin v" + getDescription().getVersion());
        getLogger().info("§b  https://artixcloud.com");
        getLogger().info("§b===================================");

        // 1. Configurações
        saveDefaultConfig();
        MessageUtil.load(this);

        // 2. Banco de dados
        if (!initDatabase()) {
            getLogger().severe("Falha ao conectar ao banco de dados! Desabilitando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. API client
        apiClient = new APIClient(this);

        // 4. Cache
        cacheManager = new CacheManager(this);

        // 5. Repositórios
        playerRepository = new PlayerRepository(databaseManager);
        serverRepository = new ServerRepository(databaseManager);

        // 6. Serviços
        planService   = new PlanService(this, apiClient, cacheManager);
        playerService = new PlayerService(this, playerRepository, cacheManager);
        serverService = new ServerService(this, apiClient, serverRepository, playerRepository, cacheManager, planService);

        // 7. Menus
        menuManager = new MenuManager(this, serverService, playerService, planService);

        // 8. Comandos
        var cloudCmd = getCommand("cloud");
        if (cloudCmd != null) {
            cloudCmd.setExecutor(new CloudCommand(this, menuManager, serverService, playerService));
            cloudCmd.setTabCompleter(new CloudTabCompleter());
        }

        // 9. Listeners
        getServer().getPluginManager().registerEvents(new MenuListener(menuManager), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, playerService), this);

        // 10. Agendadores
        statusScheduler = new StatusScheduler(this, serverService, cacheManager);
        statusScheduler.start();

        getLogger().info("§aArtixCloud iniciado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (statusScheduler != null) statusScheduler.stop();
        if (cacheManager != null)    cacheManager.invalidateAll();
        if (databaseManager != null) databaseManager.close();

        getLogger().info("§cArtixCloud desligado.");
    }

    private boolean initDatabase() {
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initialize();
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Erro ao inicializar banco de dados: " + e.getMessage(), e);
            return false;
        }
    }

    // ── Getters ─────────────────────────────────────────────

    public static ArtixCloud getInstance() { return instance; }

    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public APIClient getApiClient()             { return apiClient; }
    public CacheManager getCacheManager()       { return cacheManager; }
    public PlayerRepository getPlayerRepository() { return playerRepository; }
    public ServerRepository getServerRepository() { return serverRepository; }
    public PlayerService getPlayerService()     { return playerService; }
    public ServerService getServerService()     { return serverService; }
    public PlanService getPlanService()         { return planService; }
    public MenuManager getMenuManager()         { return menuManager; }
}
