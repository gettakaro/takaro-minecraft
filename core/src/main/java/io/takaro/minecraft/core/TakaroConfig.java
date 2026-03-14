package io.takaro.minecraft.core;

public class TakaroConfig {
    private String wsUrl;
    private String identityToken;
    private String registrationToken;
    private boolean reconnectEnabled = true;
    private long reconnectDelay = 5000;
    private long maxReconnectDelay = 300000;
    private double backoffMultiplier = 1.5;

    public String getWsUrl() { return wsUrl; }
    public void setWsUrl(String wsUrl) { this.wsUrl = wsUrl; }

    public String getIdentityToken() { return identityToken; }
    public void setIdentityToken(String identityToken) { this.identityToken = identityToken; }

    public String getRegistrationToken() { return registrationToken; }
    public void setRegistrationToken(String registrationToken) { this.registrationToken = registrationToken; }

    public boolean isReconnectEnabled() { return reconnectEnabled; }
    public void setReconnectEnabled(boolean reconnectEnabled) { this.reconnectEnabled = reconnectEnabled; }

    public long getReconnectDelay() { return reconnectDelay; }
    public void setReconnectDelay(long reconnectDelay) { this.reconnectDelay = reconnectDelay; }

    public long getMaxReconnectDelay() { return maxReconnectDelay; }
    public void setMaxReconnectDelay(long maxReconnectDelay) { this.maxReconnectDelay = maxReconnectDelay; }

    public double getBackoffMultiplier() { return backoffMultiplier; }
    public void setBackoffMultiplier(double backoffMultiplier) { this.backoffMultiplier = backoffMultiplier; }
}
