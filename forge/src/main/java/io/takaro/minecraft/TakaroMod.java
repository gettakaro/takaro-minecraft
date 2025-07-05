package io.takaro.minecraft;

import io.takaro.minecraft.config.TakaroConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TakaroMod.MOD_ID)
public class TakaroMod {
    
    public static final String MOD_ID = "takaromod";
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static TakaroMod instance;
    private MinecraftServer server;
    private TakaroWebSocketClient webSocketClient;
    private boolean shuttingDown = false;
    
    public TakaroMod() {
        instance = this;
        
        // Register configuration
        TakaroConfig.register();
        
        // Register mod lifecycle events
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        
        // Register to receive server lifecycle events
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("Takaro Mod initialized");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Takaro Mod common setup");
        // Common setup code here (runs on both client and server)
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Takaro Mod server starting");
        this.server = event.getServer();
        
        // Load configuration
        loadConfig();
        
        // Initialize WebSocket connection
        initializeWebSocket();
        
        // Register event handlers
        registerEventHandlers();
        
        LOGGER.info("Takaro Mod enabled successfully");
    }
    
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Takaro Mod server stopping");
        shuttingDown = true;
        
        if (webSocketClient != null) {
            LOGGER.info("Shutting down Takaro WebSocket connection...");
            webSocketClient.shutdown();
        }
        
        LOGGER.info("Takaro Mod disabled");
    }
    
    private void loadConfig() {
        LOGGER.info("Loading Takaro configuration...");
        // Configuration is automatically loaded by Forge from the TOML file
        // Values can be accessed through TakaroConfig static fields
        LOGGER.info("Configuration loaded from takaromod-server.toml");
    }
    
    private void initializeWebSocket() {
        LOGGER.info("Initializing Takaro WebSocket connection...");
        
        try {
            // Get configuration values
            String wsUrl = TakaroConfig.WEBSOCKET_URL.get();
            String identityToken = TakaroConfig.IDENTITY_TOKEN.get();
            String registrationToken = TakaroConfig.REGISTRATION_TOKEN.get();
            
            // Use server name as identity token if not specified
            if (identityToken == null || identityToken.isEmpty()) {
                // In Forge, we'll use a default name since server properties aren't easily accessible
                identityToken = "minecraft-forge-server";
                LOGGER.info("Using default server identity: " + identityToken);
            }
            
            // Check if registration token is provided
            if (registrationToken == null || registrationToken.isEmpty()) {
                LOGGER.error("Registration token not configured! Please set it in takaromod-server.toml");
                return;
            }
            
            java.net.URI serverUri = java.net.URI.create(wsUrl);
            webSocketClient = new TakaroWebSocketClient(this, serverUri, identityToken, registrationToken);
            
            LOGGER.info("Connecting to Takaro WebSocket server: " + wsUrl);
            webSocketClient.connect()
                .whenComplete((ws, throwable) -> {
                    if (throwable != null) {
                        LOGGER.error("Initial connection failed: " + throwable.getMessage());
                    } else {
                        LOGGER.info("WebSocket connection initiated successfully");
                    }
                });
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize WebSocket connection: " + e.getMessage());
            LOGGER.debug("Exception details: ", e);
        }
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering Takaro commands...");
        // TODO: Fix command registration - Commands.literal() method is obfuscated
        // TakaroTestCommand.register(event.getDispatcher());
        LOGGER.warn("Command registration temporarily disabled due to obfuscation issue");
    }
    
    private void registerEventHandlers() {
        LOGGER.info("Registering Takaro event handlers...");
        // Event handlers will be implemented in Phase 4
        // Commands are registered via the @SubscribeEvent method above
    }
    
    public static TakaroMod getInstance() {
        return instance;
    }
    
    public MinecraftServer getServer() {
        if (server == null) {
            // Fallback to ServerLifecycleHooks if server not set yet
            return ServerLifecycleHooks.getCurrentServer();
        }
        return server;
    }
    
    public Logger getLogger() {
        return LOGGER;
    }
    
    public TakaroWebSocketClient getWebSocketClient() {
        return webSocketClient;
    }
    
    
    public boolean isShuttingDown() {
        return shuttingDown;
    }
}