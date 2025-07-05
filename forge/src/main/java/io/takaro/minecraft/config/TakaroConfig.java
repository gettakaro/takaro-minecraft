package io.takaro.minecraft.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Arrays;
import java.util.List;

public class TakaroConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // WebSocket Configuration
    public static final ForgeConfigSpec.ConfigValue<String> WEBSOCKET_URL;
    public static final ForgeConfigSpec.BooleanValue RECONNECT_ENABLED;
    public static final ForgeConfigSpec.IntValue RECONNECT_INITIAL_DELAY;
    public static final ForgeConfigSpec.IntValue RECONNECT_MAX_DELAY;
    public static final ForgeConfigSpec.DoubleValue RECONNECT_BACKOFF_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue RECONNECT_MAX_ATTEMPTS;

    // Authentication Configuration
    public static final ForgeConfigSpec.ConfigValue<String> IDENTITY_TOKEN;
    public static final ForgeConfigSpec.ConfigValue<String> REGISTRATION_TOKEN;

    // Logging Configuration
    public static final ForgeConfigSpec.BooleanValue DEBUG;
    public static final ForgeConfigSpec.BooleanValue LOG_MESSAGES;
    public static final ForgeConfigSpec.BooleanValue FORWARD_SERVER_LOGS;
    public static final ForgeConfigSpec.ConfigValue<String> MIN_LOG_LEVEL;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FILTERED_LOGGERS;

    static {
        BUILDER.comment("Takaro Minecraft Forge Configuration").push("takaro");

        // WebSocket Configuration
        BUILDER.comment("WebSocket Configuration").push("websocket");
        
        WEBSOCKET_URL = BUILDER
                .comment("WebSocket URL for Takaro connection")
                .define("url", "wss://connect.takaro.io/");

        BUILDER.comment("Reconnection settings").push("reconnect");
        
        RECONNECT_ENABLED = BUILDER
                .comment("Enable automatic reconnection")
                .define("enabled", true);
                
        RECONNECT_INITIAL_DELAY = BUILDER
                .comment("Initial reconnect delay in milliseconds")
                .defineInRange("initial_delay", 5000, 1000, 60000);
                
        RECONNECT_MAX_DELAY = BUILDER
                .comment("Maximum reconnect delay in milliseconds")
                .defineInRange("max_delay", 300000, 10000, 600000);
                
        RECONNECT_BACKOFF_MULTIPLIER = BUILDER
                .comment("Backoff multiplier for reconnect delays")
                .defineInRange("backoff_multiplier", 2.0, 1.0, 5.0);
                
        RECONNECT_MAX_ATTEMPTS = BUILDER
                .comment("Maximum reconnect attempts (-1 for unlimited)")
                .defineInRange("max_attempts", -1, -1, Integer.MAX_VALUE);

        BUILDER.pop(); // End reconnect section
        BUILDER.pop(); // End websocket section

        // Authentication Configuration
        BUILDER.comment("Authentication Configuration").push("authentication");
        
        IDENTITY_TOKEN = BUILDER
                .comment("Identity token - unique identifier for this server instance",
                        "Default will be set to the server name from server.properties")
                .define("identity_token", "");
                
        REGISTRATION_TOKEN = BUILDER
                .comment("Registration token for authentication with Takaro",
                        "This should be obtained from your Takaro dashboard")
                .define("registration_token", "");

        BUILDER.pop(); // End authentication section

        // Logging Configuration
        BUILDER.comment("Logging Configuration").push("logging");
        
        DEBUG = BUILDER
                .comment("Enable debug logging for WebSocket connections")
                .define("debug", false);
                
        LOG_MESSAGES = BUILDER
                .comment("Log all incoming/outgoing messages (for debugging)")
                .define("log_messages", false);
                
        FORWARD_SERVER_LOGS = BUILDER
                .comment("Forward server console logs to Takaro")
                .define("forward_server_logs", true);
                
        MIN_LOG_LEVEL = BUILDER
                .comment("Minimum log level to forward (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)")
                .define("min_level", "INFO");
                
        FILTERED_LOGGERS = BUILDER
                .comment("List of logger names to filter out (avoid spam)")
                .defineList("filtered_loggers", 
                    Arrays.asList("org.java_websocket", "io.netty"), 
                    entry -> entry instanceof String);

        BUILDER.pop(); // End logging section
        BUILDER.pop(); // End takaro section

        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SPEC, "takaromod-server.toml");
    }
}