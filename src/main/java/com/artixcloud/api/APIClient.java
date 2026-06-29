package com.artixcloud.api;

import com.artixcloud.ArtixCloud;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Cliente HTTP assíncrono para comunicação com a API da ArtixCloud.
 * Todas as chamadas são feitas de forma assíncrona para não bloquear a thread principal.
 */
public class APIClient {

    private static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final ArtixCloud plugin;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;
    private final String token;

    public APIClient(ArtixCloud plugin) {
        this.plugin  = plugin;
        this.baseUrl = plugin.getConfig().getString("api.base-url", "https://api.artixcloud.com/v1");
        this.token   = plugin.getConfig().getString("api.token", "");
        int timeout  = plugin.getConfig().getInt("api.timeout-seconds", 30);

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .addInterceptor(this::authInterceptor)
                .addInterceptor(this::loggingInterceptor)
                .build();

        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    // ── Servidores ─────────────────────────────────────────────────────────────

    public CompletableFuture<APIResponse<JsonObject>> createServer(String ownerUuid,
                                                                    String name,
                                                                    String planId,
                                                                    String version) {
        JsonObject body = new JsonObject();
        body.addProperty("owner_uuid", ownerUuid);
        body.addProperty("name", name);
        body.addProperty("plan_id", planId);
        body.addProperty("version", version);
        return post("/servers", body);
    }

    public CompletableFuture<APIResponse<JsonObject>> getServer(String serverId) {
        return get("/servers/" + serverId);
    }

    public CompletableFuture<APIResponse<JsonObject>> listServers(String ownerUuid) {
        return get("/servers?owner_uuid=" + ownerUuid);
    }

    public CompletableFuture<APIResponse<JsonObject>> startServer(String serverId) {
        return post("/servers/" + serverId + "/start", new JsonObject());
    }

    public CompletableFuture<APIResponse<JsonObject>> stopServer(String serverId) {
        return post("/servers/" + serverId + "/stop", new JsonObject());
    }

    public CompletableFuture<APIResponse<JsonObject>> restartServer(String serverId) {
        return post("/servers/" + serverId + "/restart", new JsonObject());
    }

    public CompletableFuture<APIResponse<JsonObject>> deleteServer(String serverId) {
        return delete("/servers/" + serverId);
    }

    public CompletableFuture<APIResponse<JsonObject>> getServerStatus(String serverId) {
        return get("/servers/" + serverId + "/status");
    }

    // ── Planos ─────────────────────────────────────────────────────────────────

    public CompletableFuture<APIResponse<JsonObject>> listPlans() {
        return get("/plans");
    }

    // ── Conta / Jogador ────────────────────────────────────────────────────────

    public CompletableFuture<APIResponse<JsonObject>> getPlayerAccount(String playerUuid) {
        return get("/accounts/" + playerUuid);
    }

    public CompletableFuture<APIResponse<JsonObject>> createPlayerAccount(String playerUuid, String playerName) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", playerUuid);
        body.addProperty("name", playerName);
        return post("/accounts", body);
    }

    // ── Healthcheck ───────────────────────────────────────────────────────────

    public CompletableFuture<Boolean> isHealthy() {
        return get("/health").thenApply(APIResponse::isSuccess);
    }

    // ── HTTP Internals ────────────────────────────────────────────────────────

    private CompletableFuture<APIResponse<JsonObject>> get(String path) {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();
        return executeAsync(request);
    }

    private CompletableFuture<APIResponse<JsonObject>> post(String path, JsonObject body) {
        RequestBody rb = RequestBody.create(gson.toJson(body), JSON_TYPE);
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .post(rb)
                .build();
        return executeAsync(request);
    }

    private CompletableFuture<APIResponse<JsonObject>> delete(String path) {
        Request request = new Request.Builder()
                .url(baseUrl + path)
                .delete()
                .build();
        return executeAsync(request);
    }

    private CompletableFuture<APIResponse<JsonObject>> executeAsync(Request request) {
        CompletableFuture<APIResponse<JsonObject>> future = new CompletableFuture<>();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                plugin.getLogger().log(Level.WARNING, "Falha na requisição para " + request.url() + ": " + e.getMessage());
                future.complete(APIResponse.failure("Falha de conexão: " + e.getMessage(), -1));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody responseBody = response.body()) {
                    String bodyStr = responseBody != null ? responseBody.string() : "{}";
                    int code = response.code();

                    if (response.isSuccessful()) {
                        JsonObject json = bodyStr.isEmpty() ? new JsonObject()
                                : JsonParser.parseString(bodyStr).getAsJsonObject();
                        future.complete(APIResponse.success(json, code));
                    } else {
                        String message = extractMessage(bodyStr);
                        future.complete(APIResponse.failure(message, code));
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Erro ao processar resposta da API: " + e.getMessage());
                    future.complete(APIResponse.failure("Erro ao processar resposta", -1));
                }
            }
        });

        return future;
    }

    private String extractMessage(String body) {
        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            if (obj.has("message")) return obj.get("message").getAsString();
            if (obj.has("error"))   return obj.get("error").getAsString();
        } catch (Exception ignored) {}
        return "Erro desconhecido da API";
    }

    private Response authInterceptor(Interceptor.Chain chain) throws IOException {
        Request original = chain.request();
        Request authenticated = original.newBuilder()
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("X-Plugin-Version", "1.0.0")
                .build();
        return chain.proceed(authenticated);
    }

    private Response loggingInterceptor(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[API] " + request.method() + " " + request.url());
        }
        Response response = chain.proceed(request);
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[API] Response: " + response.code());
        }
        return response;
    }

    public void shutdown() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
