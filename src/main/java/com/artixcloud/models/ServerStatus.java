package com.artixcloud.models;

/**
 * Status possíveis de um servidor na ArtixCloud.
 */
public enum ServerStatus {

    ONLINE    ("online",     "&aOnline"),
    OFFLINE   ("offline",    "&cOffline"),
    STARTING  ("starting",   "&eIniciando..."),
    STOPPING  ("stopping",   "&eParando..."),
    RESTARTING("restarting", "&eReiniciando..."),
    CREATING  ("creating",   "&bCriando..."),
    ERROR     ("error",      "&4Erro"),
    UNKNOWN   ("unknown",    "&7Desconhecido");

    private final String apiValue;
    private final String displayName;

    ServerStatus(String apiValue, String displayName) {
        this.apiValue    = apiValue;
        this.displayName = displayName;
    }

    public String getApiValue()    { return apiValue; }
    public String getDisplayName() { return displayName; }

    public boolean isRunning() {
        return this == ONLINE || this == STARTING || this == RESTARTING;
    }

    public boolean isTransient() {
        return this == STARTING || this == STOPPING || this == RESTARTING || this == CREATING;
    }

    public static ServerStatus fromApiValue(String value) {
        if (value == null) return UNKNOWN;
        for (ServerStatus s : values()) {
            if (s.apiValue.equalsIgnoreCase(value)) return s;
        }
        return UNKNOWN;
    }
}
