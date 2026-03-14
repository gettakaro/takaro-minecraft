package io.takaro.minecraft.neoforge;

import io.takaro.minecraft.core.GameAdapter;
import io.takaro.minecraft.core.TakaroConfig;
import io.takaro.minecraft.core.TakaroConnector;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Mod("takaro")
public class TakaroNeoForgeMod {

    private static final Logger LOGGER = LogManager.getLogger("Takaro");
    private TakaroConnector connector;

    public TakaroNeoForgeMod() {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        Path configPath = Path.of("config", "takaro.properties");

        TakaroConfig config = loadConfig(configPath);
        if (config == null) {
            config = new TakaroConfig();
        }
        config.applyEnvOverrides();

        if (config.getWsUrl() == null || config.getWsUrl().isEmpty()) {
            LOGGER.warn("No WebSocket URL configured, skipping Takaro connection");
            return;
        }

        GameAdapter adapter = new GameAdapter() {
            @Override
            public void logInfo(String msg) { LOGGER.info(msg); }

            @Override
            public void logWarning(String msg) { LOGGER.warn(msg); }

            @Override
            public void runOnMainThread(Runnable task) { server.execute(task); }
        };

        connector = new TakaroConnector(adapter, config);
        connector.connect();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (connector != null) {
            connector.shutdown();
        }
    }

    private TakaroConfig loadConfig(Path path) {
        if (!Files.exists(path)) {
            LOGGER.warn("Config file not found at {}, creating default...", path);
            createDefaultConfig(path);
            return null;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            LOGGER.error("Failed to load config: {}", e.getMessage());
            return null;
        }

        try {
            TakaroConfig config = new TakaroConfig();
            config.setWsUrl(props.getProperty("takaro.websocket.url", ""));
            config.setIdentityToken(props.getProperty("takaro.authentication.identity_token", ""));
            config.setRegistrationToken(props.getProperty("takaro.authentication.registration_token", ""));
            config.setReconnectEnabled(Boolean.parseBoolean(props.getProperty("takaro.reconnect.enabled", "true")));
            config.setReconnectDelay(Long.parseLong(props.getProperty("takaro.reconnect.delay", "5000")));
            config.setMaxReconnectDelay(Long.parseLong(props.getProperty("takaro.reconnect.max_delay", "300000")));
            config.setBackoffMultiplier(Double.parseDouble(props.getProperty("takaro.reconnect.backoff_multiplier", "1.5")));
            return config;
        } catch (NumberFormatException e) {
            LOGGER.error("Invalid number in config: {}", e.getMessage());
            return null;
        }
    }

    private void createDefaultConfig(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path,
                    "takaro.websocket.url=\n" +
                    "takaro.authentication.identity_token=\n" +
                    "takaro.authentication.registration_token=\n" +
                    "takaro.reconnect.enabled=true\n" +
                    "takaro.reconnect.delay=5000\n" +
                    "takaro.reconnect.max_delay=300000\n" +
                    "takaro.reconnect.backoff_multiplier=1.5\n");
            LOGGER.info("Default config created at {}", path);
        } catch (IOException e) {
            LOGGER.error("Failed to create default config: {}", e.getMessage());
        }
    }
}
