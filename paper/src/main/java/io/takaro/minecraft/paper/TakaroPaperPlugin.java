package io.takaro.minecraft.paper;

import io.takaro.minecraft.core.GameAdapter;
import io.takaro.minecraft.core.TakaroConfig;
import io.takaro.minecraft.core.TakaroConnector;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class TakaroPaperPlugin extends JavaPlugin implements GameAdapter {

    private TakaroConnector connector;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        TakaroConfig config = new TakaroConfig();
        config.setWsUrl(getConfig().getString("takaro.websocket.url", ""));
        config.setIdentityToken(getConfig().getString("takaro.authentication.identity_token", ""));
        config.setRegistrationToken(getConfig().getString("takaro.authentication.registration_token", ""));
        config.setReconnectEnabled(getConfig().getBoolean("takaro.reconnect.enabled", true));
        config.setReconnectDelay(getConfig().getLong("takaro.reconnect.delay", 5000));
        config.setMaxReconnectDelay(getConfig().getLong("takaro.reconnect.max_delay", 300000));
        config.setBackoffMultiplier(getConfig().getDouble("takaro.reconnect.backoff_multiplier", 1.5));
        config.setDebugEnabled(getConfig().getBoolean("takaro.debug", false));
        config.applyEnvOverrides();

        if (config.getWsUrl() == null || config.getWsUrl().isEmpty()) {
            getLogger().warning("No WebSocket URL configured, skipping Takaro connection");
            return;
        }

        connector = new TakaroConnector(this, config);
        connector.connect();
    }

    @Override
    public void onDisable() {
        if (connector != null) {
            connector.shutdown();
        }
    }

    @Override
    public void logInfo(String msg) {
        getLogger().info(msg);
    }

    @Override
    public void logWarning(String msg) {
        getLogger().warning(msg);
    }

    @Override
    public void logDebug(String msg) {
        getLogger().info("[DEBUG] " + msg);
    }

    @Override
    public void runOnMainThread(Runnable task) {
        Bukkit.getScheduler().runTask(this, task);
    }
}
