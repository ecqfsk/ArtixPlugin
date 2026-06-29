package com.artixcloud.models;

import org.bukkit.Material;

/**
 * Representa um plano de hospedagem disponível na ArtixCloud.
 */
public class Plan {

    private final String id;
    private final String displayName;
    private final String ram;
    private final String cpu;
    private final String storage;
    private final int maxPlayers;
    private final String price;
    private final Material icon;
    private final boolean free;

    public Plan(String id, String displayName, String ram, String cpu,
                String storage, int maxPlayers, String price, Material icon) {
        this.id          = id;
        this.displayName = displayName;
        this.ram         = ram;
        this.cpu         = cpu;
        this.storage     = storage;
        this.maxPlayers  = maxPlayers;
        this.price       = price;
        this.icon        = icon;
        this.free        = price.contains("0,00") || price.contains("0.00") || id.equalsIgnoreCase("free");
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public String getRam()         { return ram; }
    public String getCpu()         { return cpu; }
    public String getStorage()     { return storage; }
    public int getMaxPlayers()     { return maxPlayers; }
    public String getPrice()       { return price; }
    public Material getIcon()      { return icon; }
    public boolean isFree()        { return free; }
}
