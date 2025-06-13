package io.takaro.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.logging.Logger;

public class TakaroWebSocketClient extends WebSocketClient {
    
    private final TakaroPlugin plugin;
    private final Logger logger;
    private final Gson gson;
    
    private String identityToken;
    private String registrationToken;
    
    private boolean authenticated = false;
    private boolean reconnectEnabled = true;
    private int reconnectAttempts = 0;
    private long reconnectDelay = 5000;
    private final long maxReconnectDelay = 300000;
    private final double backoffMultiplier = 2.0;
    private final int maxReconnectAttempts = -1;
    
    private BukkitTask reconnectTask;
    
    public TakaroWebSocketClient(TakaroPlugin plugin, URI serverUri, String identityToken, String registrationToken) {
        super(serverUri);
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.gson = new Gson();
        this.identityToken = identityToken;
        this.registrationToken = registrationToken;
        
        setTcpNoDelay(true);
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        logger.info("Connected to Takaro WebSocket server");
        authenticated = false;
        reconnectAttempts = 0;
        reconnectDelay = 5000;
        
        sendAuthenticationMessage();
    }
    
    @Override
    public void onMessage(String message) {
        if (plugin.getConfig().getBoolean("takaro.logging.log_messages", false)) {
            logger.info("Received message: " + message);
        }
        
        try {
            JsonObject json = gson.fromJson(message, JsonObject.class);
            handleMessage(json);
        } catch (Exception e) {
            logger.warning("Failed to parse message from Takaro: " + e.getMessage());
            if (plugin.getConfig().getBoolean("takaro.logging.debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        logger.info(String.format("WebSocket connection closed. Code: %d, Reason: %s, Remote: %b", 
                code, reason, remote));
        authenticated = false;
        
        if (reconnectEnabled && !plugin.isShuttingDown()) {
            scheduleReconnect();
        }
    }
    
    @Override
    public void onError(Exception ex) {
        logger.severe("WebSocket error: " + ex.getMessage());
        if (plugin.getConfig().getBoolean("takaro.logging.debug", false)) {
            ex.printStackTrace();
        }
    }
    
    private void sendAuthenticationMessage() {
        JsonObject payload = new JsonObject();
        payload.addProperty("identityToken", identityToken);
        payload.addProperty("registrationToken", registrationToken);
        
        JsonObject authMessage = new JsonObject();
        authMessage.addProperty("type", "identify");
        authMessage.add("payload", payload);
        
        sendMessage(authMessage);
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
                if (plugin.getConfig().getBoolean("takaro.logging.debug", false)) {
                    logger.info("Received unknown message type: " + type);
                }
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
                
                logger.severe("Authentication failed: " + errorName + " - " + errorMessage + " (HTTP " + httpCode + ")");
                
                if (httpCode == 401 || httpCode == 403) {
                    logger.severe("Invalid credentials. Please check your identity and registration tokens.");
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
            logger.warning("Received identifyResponse without payload");
        }
    }
    
    private void handleAuthenticated(JsonObject message) {
        authenticated = true;
        logger.info("Successfully authenticated with Takaro");
        
        if (message.has("serverInfo")) {
            JsonObject serverInfo = message.getAsJsonObject("serverInfo");
            logger.info("Server registered with Takaro. Server ID: " + 
                    (serverInfo.has("id") ? serverInfo.get("id").getAsString() : "unknown"));
        }
    }
    
    private void handleError(JsonObject message) {
        String error = message.has("message") ? message.get("message").getAsString() : "Unknown error";
        logger.severe("Takaro error: " + error);
        
        if (message.has("code")) {
            int code = message.get("code").getAsInt();
            if (code == 401) {
                logger.severe("Authentication failed. Please check your tokens.");
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
        
        logger.info("Received request: " + action + " (ID: " + requestId + ")");
        
        // Route to specific handler based on action
        switch (action) {
            case "testReachability":
                handleTestReachability(requestId);
                break;
            case "getPlayers":
                handleGetPlayers(requestId);
                break;
            default:
                // Send error for unimplemented actions
                JsonObject errorResponse = new JsonObject();
                errorResponse.addProperty("type", "response");
                if (requestId != null) {
                    errorResponse.addProperty("requestId", requestId);
                }
                errorResponse.addProperty("error", "Action not implemented: " + action);
                sendMessage(errorResponse);
        }
    }
    
    private void handleTestReachability(String requestId) {
        // Create the payload according to Takaro specification
        JsonObject payload = new JsonObject();
        payload.addProperty("connectable", true);
        payload.add("reason", null);
        
        // Create the response message
        JsonObject response = new JsonObject();
        response.addProperty("type", "response");
        if (requestId != null) {
            response.addProperty("requestId", requestId);
        }
        response.add("payload", payload);
        
        logger.info("Responding to testReachability: connectable=true");
        sendMessage(response);
    }
    
    private void handleGetPlayers(String requestId) {
        // Create the payload array of player objects
        JsonArray playersArray = new JsonArray();
        
        // Get all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            JsonObject playerObj = new JsonObject();
            
            // Set required fields
            playerObj.addProperty("gameId", player.getUniqueId().toString());
            playerObj.addProperty("name", player.getName());
            playerObj.addProperty("ping", player.getPing());
            
            // Set IP address if available
            if (player.getAddress() != null && player.getAddress().getAddress() != null) {
                playerObj.addProperty("ip", player.getAddress().getAddress().getHostAddress());
            }
            
            playersArray.add(playerObj);
        }
        
        // Create the response message
        JsonObject response = new JsonObject();
        response.addProperty("type", "response");
        if (requestId != null) {
            response.addProperty("requestId", requestId);
        }
        response.add("payload", playersArray);
        
        logger.info("Responding to getPlayers: " + playersArray.size() + " players online");
        sendMessage(response);
    }
    
    private void sendMessage(JsonObject message) {
        if (plugin.getConfig().getBoolean("takaro.logging.log_messages", false)) {
            logger.info("Sending message: " + message.toString());
        }
        
        send(gson.toJson(message));
    }
    
    private void scheduleReconnect() {
        if (maxReconnectAttempts != -1 && reconnectAttempts >= maxReconnectAttempts) {
            logger.severe("Maximum reconnection attempts reached. Giving up.");
            return;
        }
        
        reconnectAttempts++;
        logger.info(String.format("Scheduling reconnection attempt %d in %d ms", 
                reconnectAttempts, reconnectDelay));
        
        reconnectTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!plugin.isShuttingDown()) {
                logger.info("Attempting to reconnect to Takaro...");
                reconnect();
                
                reconnectDelay = Math.min((long)(reconnectDelay * backoffMultiplier), maxReconnectDelay);
            }
        }, reconnectDelay / 50);
    }
    
    public void shutdown() {
        reconnectEnabled = false;
        if (reconnectTask != null) {
            reconnectTask.cancel();
            reconnectTask = null;
        }
        if (!isClosed()) {
            close();
        }
    }
    
    public boolean isAuthenticated() {
        return authenticated && isOpen();
    }
    
    public void updateTokens(String identityToken, String registrationToken) {
        this.identityToken = identityToken;
        this.registrationToken = registrationToken;
        
        if (isOpen() && !authenticated) {
            sendAuthenticationMessage();
        }
    }
}