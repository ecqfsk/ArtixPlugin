package com.artixcloud.services;

import com.artixcloud.ArtixCloud;
import com.artixcloud.api.APIClient;
import com.artixcloud.cache.CacheManager;
import com.artixcloud.models.Plan;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serviço responsável pelo gerenciamento dos planos de hospedagem.
 * Os planos são carregados do config.yml e opcionalmente sincronizados com a API.
 */
public class PlanService {

    private final ArtixCloud plugin;
    private final APIClient apiClient;
    private final CacheManager cacheManager;
    private final List<Plan> plans = new CopyOnWriteArrayList<>();

    public PlanService(ArtixCloud plugin, APIClient apiClient, CacheManager cacheManager) {
        this.plugin      = plugin;
        this.apiClient   = apiClient;
        this.cacheManager = cacheManager;
        loadFromConfig();
    }

    /**
     * Carrega os planos do config.yml como fonte primária.
     */
    public void loadFromConfig() {
        plans.clear();
        List<ConfigurationSection> planConfigs = new ArrayList<>();
        List<?> rawList = plugin.getConfig().getList("plans");
        if (rawList == null) return;

        for (Object obj : rawList) {
            if (obj instanceof ConfigurationSection section) {
                planConfigs.add(section);
            }
        }

        // Fallback: leitura direta do mapa de configuração
        var plansList = plugin.getConfig().getMapList("plans");
        for (var map : plansList) {
            String id          = String.valueOf(map.getOrDefault("id", "unknown"));
            String displayName = String.valueOf(map.getOrDefault("display-name", "&7Plano"));
            String ram         = String.valueOf(map.getOrDefault("ram", "512MB"));
            String cpu         = String.valueOf(map.getOrDefault("cpu", "0.5 vCPU"));
            String storage     = String.valueOf(map.getOrDefault("storage", "5GB"));
            int maxPlayers     = Integer.parseInt(String.valueOf(map.getOrDefault("players", "10")));
            String price       = String.valueOf(map.getOrDefault("price", "R$ 0,00/mês"));
            String iconStr     = String.valueOf(map.getOrDefault("icon", "GRASS_BLOCK"));
            Material icon;
            try {
                icon = Material.valueOf(iconStr);
            } catch (IllegalArgumentException e) {
                icon = Material.GRASS_BLOCK;
            }
            plans.add(new Plan(id, displayName, ram, cpu, storage, maxPlayers, price, icon));
        }

        plugin.getLogger().info("Carregados " + plans.size() + " planos do config.yml.");
    }

    /**
     * Sincroniza os planos com a API (de forma assíncrona).
     */
    public CompletableFuture<List<Plan>> syncWithAPI() {
        return apiClient.listPlans().thenApplyAsync(response -> {
            if (!response.isSuccess()) {
                plugin.getLogger().warning("Falha ao sincronizar planos com a API. Usando dados locais.");
                return plans;
            }
            // Se a API retornar planos, você pode parsear e substituir aqui
            // Por ora retorna os planos locais
            return plans;
        });
    }

    public List<Plan> getPlans() {
        return new ArrayList<>(plans);
    }

    public Optional<Plan> findById(String id) {
        return plans.stream().filter(p -> p.getId().equalsIgnoreCase(id)).findFirst();
    }

    public Optional<Plan> getFreePlan() {
        return plans.stream().filter(Plan::isFree).findFirst();
    }
}
