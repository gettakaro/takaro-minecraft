package io.takaro.minecraft;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
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
    private TakaroConfig config;
    
    public TakaroMod() {
        instance = this;
        
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
        
        if (webSocketClient != null) {
            LOGGER.info("Shutting down Takaro WebSocket connection...");
            webSocketClient.shutdown();
        }
        
        LOGGER.info("Takaro Mod disabled");
    }
    
    private void loadConfig() {
        // TODO: Implement configuration loading
        LOGGER.info("Loading Takaro configuration...");
        // For now, we'll use hardcoded values or environment variables
    }
    
    private void initializeWebSocket() {
        // TODO: Implement WebSocket initialization
        LOGGER.info("Initializing Takaro WebSocket connection...");
        // This will be implemented in Phase 2
    }
    
    private void registerEventHandlers() {
        // TODO: Register Forge event handlers
        LOGGER.info("Registering Takaro event handlers...");
        // This will be implemented in Phase 4
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
    
    public TakaroConfig getConfig() {
        return config;
    }
}