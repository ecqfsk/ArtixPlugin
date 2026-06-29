package com.artixcloud.integrations;

import com.artixcloud.ArtixCloud;
import com.artixcloud.api.APIClient;

/**
 * Ponto de extensão para integrações futuras com outros sistemas
 * (ex: BungeeCord, Velocity, PlaceholderAPI, etc.).
 *
 * Implemente esta interface para adicionar suporte a novos sistemas
 * sem modificar o núcleo do plugin.
 */
public interface ArtixCloudIntegration {

    /** Nome único da integração. */
    String getName();

    /** Verifica se os requisitos para esta integração estão satisfeitos. */
    boolean isCompatible();

    /** Inicializa a integração. */
    void enable(ArtixCloud plugin, APIClient apiClient);

    /** Encerra a integração de forma segura. */
    void disable();
}
