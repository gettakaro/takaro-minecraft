package io.takaro.minecraft.fabric;

import io.takaro.minecraft.core.GameAdapter;
import io.takaro.minecraft.core.TakaroConfig;
import io.takaro.minecraft.core.TakaroConnector;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TakaroFabricMod implements DedicatedServerModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("Takaro");
    private TakaroConnector connector;

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("takaro.json");

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
                public void logDebug(String msg) { LOGGER.info("[DEBUG] " + msg); }

                @Override
                public void runOnMainThread(Runnable task) { server.execute(task); }
            };

            connector = new TakaroConnector(adapter, config);
            connector.connect();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (connector != null) {
                connector.shutdown();
            }
        });
    }

    private TakaroConfig loadConfig(Path path) {
        if (!Files.exists(path)) {
            LOGGER.warn("Config file not found at {}, creating default...", path);
            createDefaultConfig(path);
            return null;
        }

        try {
            String content = Files.readString(path);
            JsonObject json = JsonParser.parseString(content).getAsJsonObject();

            TakaroConfig config = new TakaroConfig();
            JsonObject ws = json.has("websocket") && json.get("websocket").isJsonObject() ? json.getAsJsonObject("websocket") : new JsonObject();
            JsonObject auth = json.has("authentication") && json.get("authentication").isJsonObject() ? json.getAsJsonObject("authentication") : new JsonObject();
            JsonObject reconnect = json.has("reconnect") && json.get("reconnect").isJsonObject() ? json.getAsJsonObject("reconnect") : new JsonObject();

            config.setWsUrl(ws.has("url") ? ws.get("url").getAsString() : "");
            config.setIdentityToken(auth.has("identity_token") ? auth.get("identity_token").getAsString() : "");
            config.setRegistrationToken(auth.has("registration_token") ? auth.get("registration_token").getAsString() : "");
            config.setReconnectEnabled(!reconnect.has("enabled") || reconnect.get("enabled").getAsBoolean());
            config.setReconnectDelay(reconnect.has("delay") ? reconnect.get("delay").getAsLong() : 5000);
            config.setMaxReconnectDelay(reconnect.has("max_delay") ? reconnect.get("max_delay").getAsLong() : 300000);
            config.setBackoffMultiplier(reconnect.has("backoff_multiplier") ? reconnect.get("backoff_multiplier").getAsDouble() : 1.5);
            JsonObject settings = json.has("settings") && json.get("settings").isJsonObject() ? json.getAsJsonObject("settings") : new JsonObject();
            config.setDebugEnabled(settings.has("debug") && settings.get("debug").getAsBoolean());
            return config;
        } catch (Exception e) {
            LOGGER.error("Failed to load config: {}", e.getMessage());
            return null;
        }
    }

    private void createDefaultConfig(Path path) {
        try {
            JsonObject json = new JsonObject();
            JsonObject ws = new JsonObject();
            ws.addProperty("url", "");
            json.add("websocket", ws);

            JsonObject auth = new JsonObject();
            auth.addProperty("identity_token", "");
            auth.addProperty("registration_token", "");
            json.add("authentication", auth);

            JsonObject reconnect = new JsonObject();
            reconnect.addProperty("enabled", true);
            reconnect.addProperty("delay", 5000);
            reconnect.addProperty("max_delay", 300000);
            reconnect.addProperty("backoff_multiplier", 1.5);
            json.add("reconnect", reconnect);
            JsonObject settings = new JsonObject();
            settings.addProperty("debug", false);
            json.add("settings", settings);

            Files.writeString(path, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(json));
            LOGGER.info("Default config created at {}", path);
        } catch (IOException e) {
            LOGGER.error("Failed to create default config: {}", e.getMessage());
        }
    }
}
