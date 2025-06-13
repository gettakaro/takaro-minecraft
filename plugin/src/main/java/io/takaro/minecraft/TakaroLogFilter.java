package io.takaro.minecraft;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Log4j filter that intercepts server console logs and forwards them to Takaro.
 * 
 * This filter captures all server log events in real-time and sends them as 
 * Takaro log events, allowing remote monitoring of server activity, errors,
 * and status messages.
 * 
 * Features:
 * - Configurable log level filtering (INFO, WARN, ERROR, etc.)
 * - Logger name filtering to avoid spam
 * - Anti-loop protection to prevent infinite recursion
 * - Async processing to avoid blocking server performance
 */
public class TakaroLogFilter extends AbstractFilter {
    
    // Default logger names to filter out
    private static final Set<String> DEFAULT_FILTERED_LOGGERS = Set.of(
        "TakaroMinecraft",
        "io.takaro",
        "org.java_websocket"
    );
    
    private final TakaroPlugin plugin;
    private final Logger logger;
    
    // Cached configuration values for performance
    private volatile boolean forwardingEnabled;
    private volatile Level minLevel;
    private volatile Set<String> filteredLoggers;
    private volatile long lastConfigLoad = 0;
    private static final long CONFIG_CACHE_DURATION = 30000; // 30 seconds
    
    /**
     * Creates a new TakaroLogFilter for the given plugin.
     * 
     * @param plugin The Takaro plugin instance
     */
    public TakaroLogFilter(TakaroPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        refreshConfig();
    }
    
    /**
     * Filters log events and forwards appropriate ones to Takaro.
     * 
     * @param event The log event to filter
     * @return Always NEUTRAL to allow normal logging to continue
     */
    @Override
    public Result filter(LogEvent event) {
        try {
            // Refresh config periodically for hot reloading
            refreshConfigIfNeeded();
            
            // Check if log forwarding is enabled
            if (!forwardingEnabled) {
                return Result.NEUTRAL;
            }
            
            // Get log details
            String message = event.getMessage().getFormattedMessage();
            String loggerName = event.getLoggerName();
            Level level = event.getLevel();
            
            // Apply filters
            if (shouldFilterLogger(loggerName) || !shouldForwardLevel(level)) {
                return Result.NEUTRAL;
            }
            
            // Format and send the log message
            String formattedMessage = formatLogMessage(level, loggerName, message);
            forwardLogEventAsync(formattedMessage);
            
        } catch (Exception e) {
            // Log errors but don't let them break the filter
            if (plugin.getConfig().getBoolean("takaro.logging.debug", false)) {
                logger.severe("Error in TakaroLogFilter: " + e.getMessage());
            }
        }
        
        // Always return NEUTRAL to allow normal logging to continue
        return Result.NEUTRAL;
    }
    
    /**
     * Refreshes cached configuration values if they're stale.
     */
    private void refreshConfigIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastConfigLoad > CONFIG_CACHE_DURATION) {
            refreshConfig();
        }
    }
    
    /**
     * Loads and caches configuration values for performance.
     */
    private void refreshConfig() {
        try {
            FileConfiguration config = plugin.getConfig();
            
            forwardingEnabled = config.getBoolean("takaro.logging.forward_server_logs", true);
            
            // Parse minimum log level
            String minLevelStr = config.getString("takaro.logging.min_level", "INFO");
            try {
                minLevel = Level.valueOf(minLevelStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                minLevel = Level.INFO;
                logger.warning("Invalid log level '" + minLevelStr + "', defaulting to INFO");
            }
            
            // Build filtered loggers set
            filteredLoggers = new HashSet<>(DEFAULT_FILTERED_LOGGERS);
            List<String> configFiltered = config.getStringList("takaro.logging.filtered_loggers");
            if (configFiltered != null) {
                filteredLoggers.addAll(configFiltered);
            }
            
            lastConfigLoad = System.currentTimeMillis();
            
        } catch (Exception e) {
            logger.warning("Failed to refresh log filter config: " + e.getMessage());
            // Use safe defaults
            forwardingEnabled = true;
            minLevel = Level.INFO;
            filteredLoggers = new HashSet<>(DEFAULT_FILTERED_LOGGERS);
        }
    }
    
    /**
     * Checks if a logger should be filtered out.
     * 
     * @param loggerName The name of the logger
     * @return true if this logger should be filtered (not forwarded)
     */
    private boolean shouldFilterLogger(String loggerName) {
        if (loggerName == null) {
            return true;
        }
        
        for (String filteredLogger : filteredLoggers) {
            if (loggerName.contains(filteredLogger)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a log level should be forwarded based on the minimum level.
     * 
     * @param level The log level to check
     * @return true if this level should be forwarded
     */
    private boolean shouldForwardLevel(Level level) {
        if (level == null || minLevel == null) {
            return false;
        }
        
        // Forward if the log level is at or above the minimum level
        // Higher integer values = more severe (ERROR > WARN > INFO > DEBUG)
        return level.intLevel() >= minLevel.intLevel();
    }
    
    /**
     * Formats a log message for sending to Takaro.
     * 
     * @param level The log level
     * @param loggerName The logger name
     * @param message The log message
     * @return Formatted message string
     */
    private String formatLogMessage(Level level, String loggerName, String message) {
        // Simplify logger names for readability
        String simpleName = simplifyLoggerName(loggerName);
        
        // Format: [LEVEL] LoggerName: Message
        return String.format("[%s] %s: %s", level.name(), simpleName, message);
    }
    
    /**
     * Simplifies a logger name by extracting the class name.
     * 
     * @param loggerName The full logger name
     * @return Simplified logger name
     */
    private String simplifyLoggerName(String loggerName) {
        if (loggerName == null) {
            return "Unknown";
        }
        
        // Convert full class names to simple names
        if (loggerName.contains(".")) {
            String[] parts = loggerName.split("\\.");
            return parts[parts.length - 1];
        }
        
        return loggerName;
    }
    
    /**
     * Forwards a log event to Takaro asynchronously to avoid blocking.
     * 
     * @param formattedMessage The formatted log message
     */
    private void forwardLogEventAsync(String formattedMessage) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TakaroWebSocketClient client = plugin.getWebSocketClient();
                if (client != null && client.isAuthenticated()) {
                    client.sendLogEvent(formattedMessage);
                }
            } catch (Exception e) {
                if (plugin.getConfig().getBoolean("takaro.logging.debug", false)) {
                    logger.warning("Failed to forward log event: " + e.getMessage());
                }
            }
        });
    }
}