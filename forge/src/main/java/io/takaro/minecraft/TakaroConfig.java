package io.takaro.minecraft;

/**
 * Placeholder for Takaro configuration.
 * This will be fully implemented with Forge's configuration system in Phase 5.
 */
public class TakaroConfig {
    
    // Configuration values will be added here
    private String takaroUrl = "wss://connect.takaro.io/";
    private String identityToken = "";
    private String registrationToken = "";
    private boolean debugMode = false;
    private boolean logMessages = false;
    
    public String getTakaroUrl() {
        return takaroUrl;
    }
    
    public String getIdentityToken() {
        return identityToken;
    }
    
    public String getRegistrationToken() {
        return registrationToken;
    }
    
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public boolean isLogMessages() {
        return logMessages;
    }
}