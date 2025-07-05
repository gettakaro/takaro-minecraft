package io.takaro.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.takaro.minecraft.config.TakaroConfig;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class TakaroWebSocketClient implements WebSocket.Listener {
    
    private final TakaroMod mod;
    private final Logger logger;
    private final Gson gson;
    private final HttpClient httpClient;
    private final URI serverUri;
    
    private String identityToken;
    private String registrationToken;
    
    private WebSocket webSocket;
    private boolean authenticated = false;
    private boolean reconnectEnabled;
    private int reconnectAttempts = 0;
    private long reconnectDelay;
    private final long initialReconnectDelay;
    private final long maxReconnectDelay;
    private final double backoffMultiplier;
    private final int maxReconnectAttempts;
    
    private Timer reconnectTimer;
    private StringBuilder messageBuffer = new StringBuilder();
    
    public TakaroWebSocketClient(TakaroMod mod, URI serverUri, String identityToken, String registrationToken) {
        this.mod = mod;
        this.logger = mod.getLogger();
        this.gson = new Gson();
        this.serverUri = serverUri;
        this.identityToken = identityToken;
        this.registrationToken = registrationToken;
        
        // Load configuration values
        this.reconnectEnabled = TakaroConfig.RECONNECT_ENABLED.get();
        this.initialReconnectDelay = TakaroConfig.RECONNECT_INITIAL_DELAY.get();
        this.reconnectDelay = this.initialReconnectDelay;
        this.maxReconnectDelay = TakaroConfig.RECONNECT_MAX_DELAY.get();
        this.backoffMultiplier = TakaroConfig.RECONNECT_BACKOFF_MULTIPLIER.get();
        this.maxReconnectAttempts = TakaroConfig.RECONNECT_MAX_ATTEMPTS.get();
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    }
    
    // WebSocket.Listener interface methods
    @Override
    public void onOpen(WebSocket webSocket) {
        logger.info("Connected to Takaro WebSocket server");
        this.webSocket = webSocket;
        authenticated = false;
        reconnectAttempts = 0;
        reconnectDelay = initialReconnectDelay;
        
        sendAuthenticationMessage();
        webSocket.request(1); // Request to receive the next message
    }
    
    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);
        
        if (last) {
            // Complete message received
            String message = messageBuffer.toString();
            messageBuffer.setLength(0); // Clear buffer
            
            if (TakaroConfig.LOG_MESSAGES.get()) {
                logger.info("Received message: " + message);
            } else if (TakaroConfig.DEBUG.get()) {
                logger.debug("Received message: " + message);
            }
            
            try {
                JsonObject json = gson.fromJson(message, JsonObject.class);
                handleMessage(json);
            } catch (Exception e) {
                logger.warn("Failed to parse message from Takaro: " + e.getMessage());
                logger.debug("Exception details: ", e);
            }
        }
        
        webSocket.request(1); // Request to receive the next message
        return null;
    }
    
    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        logger.info("WebSocket connection closed. Code: {}, Reason: {}", statusCode, reason);
        this.webSocket = null;
        authenticated = false;
        
        if (reconnectEnabled && !mod.isShuttingDown()) {
            scheduleReconnect();
        }
        
        return null;
    }
    
    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        logger.error("WebSocket error: " + error.getMessage());
        logger.debug("Exception details: ", error);
    }
    
    private void sendAuthenticationMessage() {
        JsonObject payload = new JsonObject();
        payload.addProperty("identityToken", identityToken);
        payload.addProperty("registrationToken", registrationToken);
        
        JsonObject authMessage = new JsonObject();
        authMessage.addProperty("type", "identify");
        authMessage.add("payload", payload);
        
        sendMessage(authMessage);
        logger.info("Sent authentication message to Takaro");
    }
    
    private void handleMessage(JsonObject message) {
        String type = message.has("type") ? message.get("type").getAsString() : "";
        
        switch (type) {
            case "connected":
                handleConnected(message);
                break;
            case "identifyResponse":
                handleIdentifyResponse(message);
                break;
            case "authenticated":
                handleAuthenticated(message);
                break;
            case "error":
                handleError(message);
                break;
            case "request":
                handleRequest(message);
                break;
            default:
                logger.debug("Received unknown message type: " + type);
        }
    }
    
    private void handleConnected(JsonObject message) {
        if (message.has("payload")) {
            JsonObject payload = message.getAsJsonObject("payload");
            String clientId = payload.has("clientId") ? payload.get("clientId").getAsString() : "unknown";
            logger.info("Connected to Takaro server. Client ID: " + clientId);
        } else {
            logger.info("Connected to Takaro server");
        }
    }
    
    private void handleIdentifyResponse(JsonObject message) {
        if (message.has("payload")) {
            JsonObject payload = message.getAsJsonObject("payload");
            
            if (payload.has("error")) {
                JsonObject error = payload.getAsJsonObject("error");
                String errorName = error.has("name") ? error.get("name").getAsString() : "Unknown";
                String errorMessage = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                int httpCode = error.has("http") ? error.get("http").getAsInt() : 0;
                
                logger.error("Authentication failed: {} - {} (HTTP {})", errorName, errorMessage, httpCode);
                
                if (httpCode == 401 || httpCode == 403) {
                    logger.error("Invalid credentials. Please check your identity and registration tokens.");
                    reconnectEnabled = false;
                }
            } else {
                authenticated = true;
                logger.info("Successfully authenticated with Takaro");
                
                if (payload.has("server")) {
                    JsonObject serverInfo = payload.getAsJsonObject("server");
                    String serverId = serverInfo.has("id") ? serverInfo.get("id").getAsString() : "unknown";
                    logger.info("Server registered with Takaro. Server ID: " + serverId);
                }
            }
        } else {
            logger.warn("Received identifyResponse without payload");
        }
    }
    
    private void handleAuthenticated(JsonObject message) {
        authenticated = true;
        logger.info("Successfully authenticated with Takaro");
        
        if (message.has("serverInfo")) {
            JsonObject serverInfo = message.getAsJsonObject("serverInfo");
            String serverId = serverInfo.has("id") ? serverInfo.get("id").getAsString() : "unknown";
            logger.info("Server registered with Takaro. Server ID: " + serverId);
        }
    }
    
    private void handleError(JsonObject message) {
        String error = message.has("message") ? message.get("message").getAsString() : "Unknown error";
        logger.error("Takaro error: " + error);
        
        if (message.has("code")) {
            int code = message.get("code").getAsInt();
            if (code == 401) {
                logger.error("Authentication failed. Please check your tokens.");
                reconnectEnabled = false;
            }
        }
    }
    
    private void handleRequest(JsonObject message) {
        String requestId = message.has("requestId") ? message.get("requestId").getAsString() : null;
        
        // Extract action from payload
        String action = "";
        if (message.has("payload")) {
            JsonObject payload = message.getAsJsonObject("payload");
            action = payload.has("action") ? payload.get("action").getAsString() : "";
        }
        
        logger.info("Received request: {} (ID: {})", action, requestId);
        
        // For Phase 2, we'll just respond with "not implemented" for all requests
        // Phase 3 will implement the actual handlers
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("type", "response");
        if (requestId != null) {
            errorResponse.addProperty("requestId", requestId);
        }
        errorResponse.addProperty("error", "Action not implemented yet: " + action);
        sendMessage(errorResponse);
    }
    
    private void sendMessage(JsonObject message) {
        if (webSocket == null) {
            logger.warn("Cannot send message - WebSocket is not connected");
            return;
        }
        
        String jsonMessage = gson.toJson(message);
        if (TakaroConfig.LOG_MESSAGES.get()) {
            logger.info("Sending message: " + jsonMessage);
        } else if (TakaroConfig.DEBUG.get()) {
            logger.debug("Sending message: " + jsonMessage);
        }
        
        webSocket.sendText(jsonMessage, true)
            .whenComplete((ws, throwable) -> {
                if (throwable != null) {
                    logger.error("Failed to send message: " + throwable.getMessage());
                }
            });
    }
    
    private void scheduleReconnect() {
        if (maxReconnectAttempts != -1 && reconnectAttempts >= maxReconnectAttempts) {
            logger.error("Maximum reconnection attempts reached. Giving up.");
            return;
        }
        
        reconnectAttempts++;
        logger.info("Scheduling reconnection attempt {} in {} ms", reconnectAttempts, reconnectDelay);
        
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
        }
        
        reconnectTimer = new Timer("Takaro-Reconnect", true);
        reconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!mod.isShuttingDown()) {
                    logger.info("Attempting to reconnect to Takaro...");
                    connect();
                    
                    reconnectDelay = Math.min((long)(reconnectDelay * backoffMultiplier), maxReconnectDelay);
                }
            }
        }, reconnectDelay);
    }
    
    public void shutdown() {
        logger.info("Shutting down Takaro WebSocket client");
        reconnectEnabled = false;
        
        if (reconnectTimer != null) {
            reconnectTimer.cancel();
            reconnectTimer = null;
        }
        
        if (webSocket != null && !webSocket.isOutputClosed()) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Mod shutting down")
                .whenComplete((ws, throwable) -> {
                    if (throwable != null) {
                        logger.debug("Error during WebSocket close: " + throwable.getMessage());
                    }
                });
        }
    }
    
    public boolean isAuthenticated() {
        return authenticated && isConnected();
    }
    
    public boolean isConnected() {
        return webSocket != null && !webSocket.isOutputClosed();
    }
    
    public void updateTokens(String identityToken, String registrationToken) {
        this.identityToken = identityToken;
        this.registrationToken = registrationToken;
        
        if (isConnected() && !authenticated) {
            sendAuthenticationMessage();
        }
    }
    
    // Initial connection method
    public CompletableFuture<WebSocket> connect() {
        logger.info("Connecting to Takaro WebSocket server: " + serverUri);
        
        return httpClient.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .buildAsync(serverUri, this)
            .whenComplete((ws, throwable) -> {
                if (throwable != null) {
                    logger.error("Failed to connect to WebSocket: " + throwable.getMessage());
                    if (reconnectEnabled && !mod.isShuttingDown()) {
                        scheduleReconnect();
                    }
                } else {
                    logger.debug("WebSocket connection established successfully");
                }
            });
    }
}