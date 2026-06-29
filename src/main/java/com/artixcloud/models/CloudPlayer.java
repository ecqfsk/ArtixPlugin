package com.artixcloud.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa os dados de um jogador registrado na ArtixCloud.
 */
public class CloudPlayer {

    private final UUID uuid;
    private final String name;
    private int serverCount;
    private Instant lastServerCreation;
    private final Instant registeredAt;
    private Instant dailyCreationReset;
    private int dailyCreations;

    public CloudPlayer(UUID uuid, String name, Instant registeredAt) {
        this.uuid           = uuid;
        this.name           = name;
        this.registeredAt   = registeredAt;
        this.serverCount    = 0;
        this.dailyCreations = 0;
    }

    // ── Getters ──────────────────────────────────────────

    public UUID getUuid()                          { return uuid; }
    public String getName()                        { return name; }
    public int getServerCount()                    { return serverCount; }
    public Instant getLastServerCreation()         { return lastServerCreation; }
    public Instant getRegisteredAt()               { return registeredAt; }
    public Instant getDailyCreationReset()         { return dailyCreationReset; }
    public int getDailyCreations()                 { return dailyCreations; }

    // ── Setters ──────────────────────────────────────────

    public void setServerCount(int serverCount)            { this.serverCount = serverCount; }
    public void setLastServerCreation(Instant t)           { this.lastServerCreation = t; }
    public void setDailyCreationReset(Instant t)           { this.dailyCreationReset = t; }
    public void setDailyCreations(int dailyCreations)      { this.dailyCreations = dailyCreations; }

    public void incrementServerCount()  { this.serverCount++; }
    public void decrementServerCount()  { if (this.serverCount > 0) this.serverCount--; }

    /** Verifica se o jogador ainda está no período de cooldown de criação. */
    public boolean isInCooldown(long cooldownSeconds) {
        if (lastServerCreation == null) return false;
        return Instant.now().isBefore(lastServerCreation.plusSeconds(cooldownSeconds));
    }

    /** Retorna quantos segundos restam no cooldown. */
    public long getCooldownRemaining(long cooldownSeconds) {
        if (!isInCooldown(cooldownSeconds)) return 0;
        return lastServerCreation.plusSeconds(cooldownSeconds).getEpochSecond() - Instant.now().getEpochSecond();
    }

    /** Formata o tempo restante de cooldown de forma legível. */
    public String getFormattedCooldown(long cooldownSeconds) {
        long remaining = getCooldownRemaining(cooldownSeconds);
        if (remaining <= 0) return "0s";
        long h = remaining / 3600;
        long m = (remaining % 3600) / 60;
        long s = remaining % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
