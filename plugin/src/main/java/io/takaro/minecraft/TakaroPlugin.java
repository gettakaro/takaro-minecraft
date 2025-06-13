package io.takaro.minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;

public class TakaroPlugin extends JavaPlugin {
    
    private TakaroWebSocketClient webSocketClient;
    private TakaroEventListener eventListener;
    private TakaroLogFilter logFilter;
    private boolean shuttingDown = false;

    @Override
    public void onEnable() {
        getLogger().info("Takaro Minecraft Plugin has been enabled!");
        
        saveDefaultConfig();
        loadConfiguration();
        
        
        // Initialize event listener
        eventListener = new TakaroEventListener(this);
        getServer().getPluginManager().registerEvents(eventListener, this);
        
        initializeWebSocketConnection();
        
        // Initialize log filter after a delay to ensure WebSocket is connected
        getServer().getScheduler().runTaskLater(this, () -> {
            initializeLogFilter();
        }, 100L); // 5 second delay
    }

    @Override
    public void onDisable() {
        getLogger().info("Takaro Minecraft Plugin has been disabled!");
        shuttingDown = true;
        
        // Stop log filter
        if (logFilter != null) {
            try {
                logFilter.stop();
                logFilter = null;
                getLogger().info("Log filter stopped");
            } catch (Exception e) {
                getLogger().warning("Failed to stop log filter: " + e.getMessage());
            }
        }
        
        if (webSocketClient != null) {
            webSocketClient.shutdown();
            webSocketClient = null;
        }
    }

    private void initializeLogFilter() {
        try {
            if (getConfig().getBoolean("takaro.logging.forward_server_logs", true)) {
                logFilter = new TakaroLogFilter(this);
                logFilter.start();
                
                Logger rootLogger = (Logger) LogManager.getRootLogger();
                rootLogger.addFilter(logFilter);
                
                getLogger().info("Log filter initialized - server logs will be forwarded to Takaro");
            } else {
                getLogger().info("Server log forwarding is disabled in config");
            }
        } catch (Exception e) {
            getLogger().severe("Failed to initialize log filter: " + e.getMessage());
            if (getConfig().getBoolean("takaro.logging.debug", false)) {
                e.printStackTrace();
            }
        }
    }

    
    private void loadConfiguration() {
        String identityToken = getConfig().getString("takaro.authentication.identity_token", "");
        if (identityToken.isEmpty()) {
            identityToken = getServer().getName();
            getConfig().set("takaro.authentication.identity_token", identityToken);
            saveConfig();
            getLogger().info("Set default identity token to server name: " + identityToken);
        }
        
        String registrationToken = getConfig().getString("takaro.authentication.registration_token", "");
        if (registrationToken.isEmpty()) {
            getLogger().warning("Registration token is not set. Please configure it in config.yml");
        }
        
        getLogger().info("Configuration loaded. Identity: " + identityToken);
    }
    
    private void initializeWebSocketConnection() {
        String url = getConfig().getString("takaro.websocket.url", "wss://connect.takaro.io/");
        String identityToken = getConfig().getString("takaro.authentication.identity_token", getServer().getName());
        String registrationToken = getConfig().getString("takaro.authentication.registration_token", "");
        
        try {
            URI serverUri = new URI(url);
            webSocketClient = new TakaroWebSocketClient(this, serverUri, identityToken, registrationToken);
            
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    webSocketClient.connect();
                    getLogger().info("Connecting to Takaro WebSocket: " + url);
                } catch (Exception e) {
                    getLogger().severe("Failed to connect to Takaro WebSocket: " + e.getMessage());
                    if (getConfig().getBoolean("takaro.logging.debug", false)) {
                        e.printStackTrace();
                    }
                }
            });
            
        } catch (Exception e) {
            getLogger().severe("Failed to initialize WebSocket connection: " + e.getMessage());
            if (getConfig().getBoolean("takaro.logging.debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    public boolean isShuttingDown() {
        return shuttingDown;
    }
    
    public TakaroWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
}