package io.takaro.minecraft.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TakaroWebSocketClient extends WebSocketClient {

    private final GameAdapter adapter;
    private final TakaroConfig config;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "takaro-reconnect");
        t.setDaemon(true);
        return t;
    });
    private volatile long currentReconnectDelay;
    private volatile boolean shouldReconnect = true;

    public TakaroWebSocketClient(URI serverUri, GameAdapter adapter, TakaroConfig config) {
        super(serverUri);
        this.adapter = adapter;
        this.config = config;
        this.currentReconnectDelay = config.getReconnectDelay();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        adapter.logInfo("WebSocket connected, sending identify...");
        sendIdentify();
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.has("type") ? json.get("type").getAsString() : "";

            switch (type) {
                case "connected":
                    adapter.logInfo("Received server hello");
                    break;
                case "identifyResponse":
                    handleIdentifyResponse(json);
                    break;
                case "authenticated":
                    adapter.logInfo("Authentication confirmed");
                    currentReconnectDelay = config.getReconnectDelay();
                    break;
                case "request":
                    handleRequest(json);
                    break;
                case "error":
                    String errorMsg = "unknown";
                    if (json.has("payload") && json.getAsJsonObject("payload").has("message")) {
                        errorMsg = json.getAsJsonObject("payload").get("message").getAsString();
                    } else if (json.has("message")) {
                        errorMsg = json.get("message").getAsString();
                    }
                    String requestId = json.has("requestId") ? json.get("requestId").getAsString() : null;
                    adapter.logWarning("Server error: " + errorMsg + (requestId != null ? " (requestId=" + requestId + ")" : ""));
                    break;
                default:
                    adapter.logWarning("Unknown message type: " + type);
                    break;
            }
        } catch (Exception e) {
            adapter.logWarning("Failed to parse message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        adapter.logInfo("WebSocket closed (code=" + code + ", reason=" + reason + ", remote=" + remote + ")");

        if (code == 1008 || code == 4001 || code == 4003) {
            adapter.logWarning("Authentication error, disabling reconnect");
            shouldReconnect = false;
        }

        if (shouldReconnect && config.isReconnectEnabled()) {
            scheduleReconnect();
        }
    }

    @Override
    public void onError(Exception ex) {
        adapter.logWarning("WebSocket error: " + ex.getMessage());
    }

    public void shutdown() {
        shouldReconnect = false;
        scheduler.shutdownNow();
        try {
            closeBlocking();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendIdentify() {
        JsonObject payload = new JsonObject();
        String identity = config.getIdentityToken();
        String registration = config.getRegistrationToken();
        payload.addProperty("identityToken", identity != null ? identity : "");
        payload.addProperty("registrationToken", registration != null ? registration : "");

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "identify");
        msg.add("payload", payload);
        send(msg.toString());
    }

    private void handleIdentifyResponse(JsonObject json) {
        JsonObject payload = json.has("payload") ? json.getAsJsonObject("payload") : new JsonObject();

        if (payload.has("error") && !payload.get("error").isJsonNull()) {
            JsonElement errorElement = payload.get("error");
            String errorMessage;
            if (errorElement.isJsonObject()) {
                JsonObject errorObj = errorElement.getAsJsonObject();
                errorMessage = errorObj.has("message") ? errorObj.get("message").getAsString() : errorObj.toString();
            } else {
                errorMessage = errorElement.getAsString();
            }
            adapter.logWarning("Identify failed: " + errorMessage);
            return;
        }

        currentReconnectDelay = config.getReconnectDelay();

        if (payload.has("server") && payload.getAsJsonObject("server").has("id")) {
            String serverId = payload.getAsJsonObject("server").get("id").getAsString();
            adapter.logInfo("Identified successfully, server ID: " + serverId);
        } else {
            adapter.logInfo("Identified successfully");
        }
    }

    private void handleRequest(JsonObject json) {
        String requestId = json.has("requestId") ? json.get("requestId").getAsString() : null;
        if (requestId == null) {
            adapter.logWarning("Received request without requestId");
            return;
        }

        JsonObject payload = json.has("payload") ? json.getAsJsonObject("payload") : new JsonObject();
        String action = payload.has("action") ? payload.get("action").getAsString() : "";

        if ("testReachability".equals(action)) {
            JsonObject responsePayload = new JsonObject();
            responsePayload.addProperty("connectable", true);
            responsePayload.addProperty("reason", (String) null);
            sendResponse(requestId, responsePayload, null);
        } else {
            sendResponse(requestId, null, "Action not implemented: " + action);
        }
    }

    private void sendResponse(String requestId, JsonObject payload, String error) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "response");
        msg.addProperty("requestId", requestId);

        if (error != null) {
            msg.addProperty("error", error);
        } else if (payload != null) {
            msg.add("payload", payload);
        }

        send(msg.toString());
    }

    private void scheduleReconnect() {
        adapter.logInfo("Reconnecting in " + (currentReconnectDelay / 1000) + "s...");
        scheduler.schedule(() -> {
            if (!shouldReconnect) return;
            try {
                reconnectBlocking();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, currentReconnectDelay, TimeUnit.MILLISECONDS);

        currentReconnectDelay = Math.min(
                (long) (currentReconnectDelay * config.getBackoffMultiplier()),
                config.getMaxReconnectDelay()
        );
    }
}
