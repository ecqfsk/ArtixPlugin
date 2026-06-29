package com.artixcloud.models;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa um servidor na infraestrutura da ArtixCloud.
 */
public class CloudServer {

    private final String id;
    private final UUID ownerUUID;
    private String name;
    private String planId;
    private String version;
    private ServerStatus status;
    private String ip;
    private int port;
    private int playersOnline;
    private int maxPlayers;
    private double ramUsedMb;
    private double ramTotalMb;
    private double cpuPercent;
    private double storageMb;
    private long uptimeSeconds;
    private final Instant createdAt;
    private Instant updatedAt;

    public CloudServer(String id, UUID ownerUUID, String name, String planId, String version, Instant createdAt) {
        this.id         = id;
        this.ownerUUID  = ownerUUID;
        this.name       = name;
        this.planId     = planId;
        this.version    = version;
        this.status     = ServerStatus.OFFLINE;
        this.createdAt  = createdAt;
        this.updatedAt  = createdAt;
    }

    // ── Getters ──────────────────────────────────────────

    public String getId()            { return id; }
    public UUID getOwnerUUID()       { return ownerUUID; }
    public String getName()          { return name; }
    public String getPlanId()        { return planId; }
    public String getVersion()       { return version; }
    public ServerStatus getStatus()  { return status; }
    public String getIp()            { return ip; }
    public int getPort()             { return port; }
    public int getPlayersOnline()    { return playersOnline; }
    public int getMaxPlayers()       { return maxPlayers; }
    public double getRamUsedMb()     { return ramUsedMb; }
    public double getRamTotalMb()    { return ramTotalMb; }
    public double getCpuPercent()    { return cpuPercent; }
    public double getStorageMb()     { return storageMb; }
    public long getUptimeSeconds()   { return uptimeSeconds; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }

    // ── Setters ──────────────────────────────────────────

    public void setName(String name)                  { this.name = name; }
    public void setPlanId(String planId)              { this.planId = planId; }
    public void setVersion(String version)            { this.version = version; }
    public void setStatus(ServerStatus status)        { this.status = status; }
    public void setIp(String ip)                      { this.ip = ip; }
    public void setPort(int port)                     { this.port = port; }
    public void setPlayersOnline(int playersOnline)   { this.playersOnline = playersOnline; }
    public void setMaxPlayers(int maxPlayers)         { this.maxPlayers = maxPlayers; }
    public void setRamUsedMb(double ramUsedMb)        { this.ramUsedMb = ramUsedMb; }
    public void setRamTotalMb(double ramTotalMb)      { this.ramTotalMb = ramTotalMb; }
    public void setCpuPercent(double cpuPercent)      { this.cpuPercent = cpuPercent; }
    public void setStorageMb(double storageMb)        { this.storageMb = storageMb; }
    public void setUptimeSeconds(long uptimeSeconds)  { this.uptimeSeconds = uptimeSeconds; }
    public void setUpdatedAt(Instant updatedAt)       { this.updatedAt = updatedAt; }

    /** Retorna o endereço completo de conexão (IP:Porta). */
    public String getConnectionAddress() {
        if (ip == null) return "N/A";
        return port > 0 ? ip + ":" + port : ip;
    }

    /** Formata o uptime em horas, minutos e segundos. */
    public String getFormattedUptime() {
        if (uptimeSeconds <= 0) return "0s";
        long h = uptimeSeconds / 3600;
        long m = (uptimeSeconds % 3600) / 60;
        long s = uptimeSeconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    /** Retorna uso de RAM como percentual inteiro. */
    public int getRamPercent() {
        if (ramTotalMb <= 0) return 0;
        return (int) ((ramUsedMb / ramTotalMb) * 100);
    }

    @Override
    public String toString() {
        return "CloudServer{id='" + id + "', name='" + name + "', status=" + status + "}";
    }
}
