package io.takaro.minecraft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
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
            case "getPlayer":
                handleGetPlayer(requestId, message);
                break;
            case "getPlayers":
                handleGetPlayers(requestId);
                break;
            case "getPlayerInventory":
                handleGetPlayerInventory(requestId, message);
                break;
            case "getPlayerLocation":
                handleGetPlayerLocation(requestId, message);
                break;
            case "listItems":
                handleListItems(requestId);
                break;
            case "listBans":
                handleListBans(requestId);
                break;
            case "sendMessage":
                handleSendMessage(requestId, message);
                break;
            case "giveItem":
                handleGiveItem(requestId, message);
                break;
            case "executeConsoleCommand":
                handleExecuteConsoleCommand(requestId, message);
                break;
            case "kickPlayer":
                handleKickPlayer(requestId, message);
                break;
            case "banPlayer":
                handleBanPlayer(requestId, message);
                break;
            case "unbanPlayer":
                handleUnbanPlayer(requestId, message);
                break;
            case "shutdown":
                handleShutdown(requestId, message);
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
    
    private void handleGetPlayer(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        if (!args.has("gameId")) {
            sendErrorResponse(requestId, "gameId parameter is required");
            return;
        }
        
        String gameId = args.get("gameId").getAsString();
        
        try {
            UUID playerUUID = UUID.fromString(gameId);
            Player player = Bukkit.getPlayer(playerUUID);
            
            if (player == null) {
                // Return null payload when player not found
                JsonObject response = new JsonObject();
                response.addProperty("type", "response");
                if (requestId != null) {
                    response.addProperty("requestId", requestId);
                }
                response.add("payload", null);
                sendMessage(response);
                return;
            }
            
            // Create player object according to spec
            JsonObject playerData = new JsonObject();
            
            // Required fields
            playerData.addProperty("gameId", player.getUniqueId().toString());
            playerData.addProperty("name", player.getName());
            
            // Optional fields
            playerData.addProperty("platformId", "minecraft:" + player.getUniqueId().toString());
            playerData.addProperty("ping", player.getPing());
            
            // Add IP if available
            if (player.getAddress() != null && player.getAddress().getAddress() != null) {
                playerData.addProperty("ip", player.getAddress().getAddress().getHostAddress());
            }
            
            // Platform-specific IDs (not applicable for Minecraft)
            playerData.add("steamId", null);
            playerData.add("epicOnlineServicesId", null);
            playerData.add("xboxLiveId", null);
            
            // Create the response message
            JsonObject response = new JsonObject();
            response.addProperty("type", "response");
            if (requestId != null) {
                response.addProperty("requestId", requestId);
            }
            response.add("payload", playerData);
            
            logger.info("Responding to getPlayer for: " + player.getName());
            sendMessage(response);
            
        } catch (IllegalArgumentException e) {
            sendErrorResponse(requestId, "Invalid gameId format");
        }
    }
    
    private void handleGetPlayers(String requestId) {
        // Create the payload array of player objects
        JsonArray playersArray = new JsonArray();
        
        // Get all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            playersArray.add(createPlayerDataWithDetails(player));
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
    
    private JsonObject parseArgsFromMessage(JsonObject message) {
        try {
            if (message.has("payload")) {
                JsonObject payload = message.getAsJsonObject("payload");
                if (payload.has("args")) {
                    String argsString = payload.get("args").getAsString();
                    return JsonParser.parseString(argsString).getAsJsonObject();
                }
            }
        } catch (JsonSyntaxException e) {
            logger.warning("Failed to parse args from message: " + e.getMessage());
        }
        return new JsonObject();
    }
    
    private void handleGetPlayerInventory(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        if (!args.has("gameId")) {
            sendErrorResponse(requestId, "gameId parameter is required");
            return;
        }
        
        String gameId = args.get("gameId").getAsString();
        
        try {
            UUID playerUUID = UUID.fromString(gameId);
            Player player = Bukkit.getPlayer(playerUUID);
            
            if (player == null) {
                sendErrorResponse(requestId, "Player not found or offline");
                return;
            }
            
            JsonArray inventoryArray = new JsonArray();
            ItemStack[] contents = player.getInventory().getContents();
            
            for (ItemStack item : contents) {
                if (item != null && item.getType() != Material.AIR) {
                    JsonObject itemObj = new JsonObject();
                    itemObj.addProperty("code", item.getType().name());
                    itemObj.addProperty("name", item.getType().name().toLowerCase().replace("_", " "));
                    itemObj.addProperty("amount", item.getAmount());
                    
                    // Calculate quality based on durability
                    if (item.getType().getMaxDurability() > 0) {
                        int durability = item.getType().getMaxDurability() - item.getDurability();
                        int qualityPercent = (int) ((double) durability / item.getType().getMaxDurability() * 100);
                        itemObj.addProperty("quality", String.valueOf(qualityPercent));
                    } else {
                        itemObj.addProperty("quality", "100");
                    }
                    
                    inventoryArray.add(itemObj);
                }
            }
            
            JsonObject response = new JsonObject();
            response.addProperty("type", "response");
            if (requestId != null) {
                response.addProperty("requestId", requestId);
            }
            response.add("payload", inventoryArray);
            
            logger.info("Responding to getPlayerInventory: " + inventoryArray.size() + " items");
            sendMessage(response);
            
        } catch (IllegalArgumentException e) {
            sendErrorResponse(requestId, "Invalid gameId format");
        }
    }
    
    private void handleGetPlayerLocation(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        if (!args.has("gameId")) {
            sendErrorResponse(requestId, "gameId parameter is required");
            return;
        }
        
        String gameId = args.get("gameId").getAsString();
        
        try {
            UUID playerUUID = UUID.fromString(gameId);
            Player player = Bukkit.getPlayer(playerUUID);
            
            if (player == null) {
                sendErrorResponse(requestId, "Player not found or offline");
                return;
            }
            
            Location location = player.getLocation();
            
            JsonObject locationObj = new JsonObject();
            locationObj.addProperty("x", location.getX());
            locationObj.addProperty("y", location.getY());
            locationObj.addProperty("z", location.getZ());
            locationObj.addProperty("world", location.getWorld().getName());
            
            JsonObject response = new JsonObject();
            response.addProperty("type", "response");
            if (requestId != null) {
                response.addProperty("requestId", requestId);
            }
            response.add("payload", locationObj);
            
            logger.info("Responding to getPlayerLocation: " + location.getWorld().getName() + " (" + 
                       location.getX() + ", " + location.getY() + ", " + location.getZ() + ")");
            sendMessage(response);
            
        } catch (IllegalArgumentException e) {
            sendErrorResponse(requestId, "Invalid gameId format");
        }
    }
    
    private void handleListItems(String requestId) {
        JsonArray itemsArray = new JsonArray();
        
        for (Material material : Material.values()) {
            if (material.isItem() && !material.isAir()) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("code", material.name());
                
                // Format name from ENUM_CASE to "Enum Case"
                String name = material.name().toLowerCase().replace("_", " ");
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                itemObj.addProperty("name", name);
                
                // Basic description
                String description = "A " + name.toLowerCase();
                if (material.isBlock()) {
                    description = "A " + name.toLowerCase() + " block";
                } else if (material.name().contains("SWORD") || material.name().contains("AXE") || 
                          material.name().contains("PICKAXE") || material.name().contains("SHOVEL")) {
                    description = "A " + name.toLowerCase() + " tool";
                } else if (material.name().contains("HELMET") || material.name().contains("CHESTPLATE") || 
                          material.name().contains("LEGGINGS") || material.name().contains("BOOTS")) {
                    description = "A piece of " + name.toLowerCase() + " armor";
                }
                itemObj.addProperty("description", description);
                
                itemsArray.add(itemObj);
            }
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "response");
        if (requestId != null) {
            response.addProperty("requestId", requestId);
        }
        response.add("payload", itemsArray);
        
        logger.info("Responding to listItems: " + itemsArray.size() + " items available");
        sendMessage(response);
    }
    
    private void handleListBans(String requestId) {
        JsonArray bansArray = new JsonArray();
        
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        
        for (OfflinePlayer bannedPlayer : Bukkit.getBannedPlayers()) {
            JsonObject banObj = new JsonObject();
            banObj.addProperty("gameId", bannedPlayer.getUniqueId().toString());
            banObj.addProperty("name", bannedPlayer.getName());
            
            // Get ban details if available
            if (banList.isBanned(bannedPlayer.getName())) {
                String reason = banList.getBanEntry(bannedPlayer.getName()).getReason();
                banObj.addProperty("reason", reason != null ? reason : "No reason specified");
            } else {
                banObj.addProperty("reason", "Banned");
            }
            
            bansArray.add(banObj);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("type", "response");
        if (requestId != null) {
            response.addProperty("requestId", requestId);
        }
        response.add("payload", bansArray);
        
        logger.info("Responding to listBans: " + bansArray.size() + " banned players");
        sendMessage(response);
    }
    
    private void handleSendMessage(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        // Check for required message parameter
        if (!args.has("message")) {
            sendErrorResponse(requestId, "message parameter is required");
            return;
        }
        
        String messageText = args.get("message").getAsString();
        if (messageText == null || messageText.trim().isEmpty()) {
            sendErrorResponse(requestId, "message cannot be empty");
            return;
        }
        
        // Format the message with Takaro prefix
        String formattedMessage = "§a[Takaro] §f" + messageText;
        
        // Check if there's a recipient for private messaging
        if (args.has("opts")) {
            JsonObject opts = args.getAsJsonObject("opts");
            
            if (opts.has("recipient")) {
                JsonObject recipient = opts.getAsJsonObject("recipient");
                
                if (!recipient.has("gameId")) {
                    sendErrorResponse(requestId, "recipient must contain gameId");
                    return;
                }
                
                String gameId = recipient.get("gameId").getAsString();
                
                try {
                    UUID playerUUID = UUID.fromString(gameId);
                    Player targetPlayer = Bukkit.getPlayer(playerUUID);
                    
                    if (targetPlayer == null) {
                        sendErrorResponse(requestId, "Player not found or offline");
                        return;
                    }
                    
                    // Send private message to specific player
                    targetPlayer.sendMessage(formattedMessage);
                    logger.info("Sent private message to " + targetPlayer.getName() + ": " + messageText);
                    
                } catch (IllegalArgumentException e) {
                    sendErrorResponse(requestId, "Invalid gameId format");
                    return;
                }
            } else {
                // opts provided but no recipient - broadcast
                Bukkit.broadcastMessage(formattedMessage);
                logger.info("Broadcast message to all players: " + messageText);
            }
        } else {
            // No recipient specified, broadcast to all players
            Bukkit.broadcastMessage(formattedMessage);
            logger.info("Broadcast message to all players: " + messageText);
        }
        
        // Send success response with null payload
        JsonObject response = new JsonObject();
        response.addProperty("type", "response");
        if (requestId != null) {
            response.addProperty("requestId", requestId);
        }
        response.add("payload", null);
        
        sendMessage(response);
    }
    
    private void handleGiveItem(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        // Parse required parameters
        if (!args.has("player") || !args.has("item") || !args.has("amount")) {
            sendErrorResponse(requestId, "Missing required parameters: player, item, and amount are required");
            return;
        }
        
        JsonObject playerObj = args.getAsJsonObject("player");
        if (!playerObj.has("gameId")) {
            sendErrorResponse(requestId, "player object must contain gameId");
            return;
        }
        
        String gameId = playerObj.get("gameId").getAsString();
        String itemCode = args.get("item").getAsString();
        int amount;
        
        try {
            amount = args.get("amount").getAsInt();
            if (amount <= 0) {
                sendErrorResponse(requestId, "Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            sendErrorResponse(requestId, "Invalid amount format");
            return;
        }
        
        // Parse optional quality parameter
        String quality = args.has("quality") ? args.get("quality").getAsString() : null;
        
        try {
            UUID playerUUID = UUID.fromString(gameId);
            Player player = Bukkit.getPlayer(playerUUID);
            
            if (player == null) {
                sendErrorResponse(requestId, "Player not found or offline");
                return;
            }
            
            // Convert item code to Material
            Material material = Material.getMaterial(itemCode.toUpperCase());
            if (material == null) {
                sendErrorResponse(requestId, "Invalid item code: " + itemCode);
                return;
            }
            
            if (!material.isItem() || material.isAir()) {
                sendErrorResponse(requestId, "Item code does not represent a valid item: " + itemCode);
                return;
            }
            
            // Create ItemStack
            ItemStack itemStack = new ItemStack(material, amount);
            
            // Apply quality if applicable (for items with durability)
            if (quality != null && material.getMaxDurability() > 0) {
                try {
                    // Try to parse quality as a percentage (0-100)
                    double qualityPercent = Double.parseDouble(quality);
                    if (qualityPercent < 0 || qualityPercent > 100) {
                        logger.warning("Quality value out of range (0-100): " + quality);
                        qualityPercent = Math.max(0, Math.min(100, qualityPercent));
                    }
                    
                    // Calculate durability (inverse of damage)
                    short durability = (short)(material.getMaxDurability() * (qualityPercent / 100.0));
                    short damage = (short)(material.getMaxDurability() - durability);
                    itemStack.setDurability(damage);
                } catch (NumberFormatException e) {
                    // Quality might be a string like "high", "low" - ignore for now
                    logger.info("Non-numeric quality value provided: " + quality);
                }
            }
            
            // Add item to player inventory on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
                
                // Drop any items that couldn't fit in inventory
                if (!leftover.isEmpty()) {
                    for (ItemStack item : leftover.values()) {
                        player.getWorld().dropItem(player.getLocation(), item);
                    }
                    logger.info("Player inventory full, dropped " + leftover.size() + " item stacks at player location");
                }
                
                // Send success response with null payload
                JsonObject response = new JsonObject();
                response.addProperty("type", "response");
                if (requestId != null) {
                    response.addProperty("requestId", requestId);
                }
                response.add("payload", null);
                
                logger.info("Gave " + amount + " x " + material.name() + " to player " + player.getName());
                sendMessage(response);
            });
            
        } catch (IllegalArgumentException e) {
            sendErrorResponse(requestId, "Invalid gameId format");
        }
    }
    
    private void handleExecuteConsoleCommand(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        if (!args.has("command")) {
            sendErrorResponse(requestId, "command parameter is required");
            return;
        }
        
        String command = args.get("command").getAsString();
        if (command == null || command.trim().isEmpty()) {
            sendErrorResponse(requestId, "command cannot be empty");
            return;
        }
        
        logger.info("Executing console command: " + command);
        
        // Execute on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                // Execute the command and capture success
                boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                
                JsonObject payload = new JsonObject();
                payload.addProperty("success", success);
                // Note: Bukkit doesn't provide direct output capture, so rawResult is empty
                payload.addProperty("rawResult", "");
                payload.add("errorMessage", null);
                
                JsonObject response = new JsonObject();
                response.addProperty("type", "response");
                if (requestId != null) {
                    response.addProperty("requestId", requestId);
                }
                response.add("payload", payload);
                
                logger.info("Console command executed successfully: " + command);
                sendMessage(response);
                
            } catch (Exception e) {
                JsonObject payload = new JsonObject();
                payload.addProperty("success", false);
                payload.addProperty("rawResult", "");
                payload.addProperty("errorMessage", e.getMessage());
                
                JsonObject response = new JsonObject();
                response.addProperty("type", "response");
                if (requestId != null) {
                    response.addProperty("requestId", requestId);
                }
                response.add("payload", payload);
                
                logger.warning("Console command failed: " + command + " - " + e.getMessage());
                sendMessage(response);
            }
        });
    }
    
    private void handleKickPlayer(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        // Validate player parameter
        if (!args.has("player")) {
            sendErrorResponse(requestId, "player parameter is required");
            return;
        }
        
        JsonObject playerObj = args.getAsJsonObject("player");
        if (!playerObj.has("gameId")) {
            sendErrorResponse(requestId, "player object must contain gameId");
            return;
        }
        
        String gameId = playerObj.get("gameId").getAsString();
        
        // Optional reason parameter
        String reason = args.has("reason") ? args.get("reason").getAsString() : "Kicked by administrator";
        
        try {
            UUID playerUUID = UUID.fromString(gameId);
            Player targetPlayer = Bukkit.getPlayer(playerUUID);
            
            if (targetPlayer == null) {
                sendErrorResponse(requestId, "Player not found or offline");
                return;
            }
            
            logger.info("Kicking player: " + targetPlayer.getName() + " - Reason: " + reason);
            
            // Kick player on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                targetPlayer.kickPlayer(reason);
                
                // Send null response on success
                JsonObject response = new JsonObject();
                response.addProperty("type", "response");
                if (requestId != null) {
                    response.addProperty("requestId", requestId);
                }
                response.add("payload", null);
                
                logger.info("Player kicked successfully: " + targetPlayer.getName());
                sendMessage(response);
            });
            
        } catch (IllegalArgumentException e) {
            sendErrorResponse(requestId, "Invalid gameId format");
        }
    }
    
    private void handleBanPlayer(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        // Validate player parameter
        if (!args.has("player")) {
            sendErrorResponse(requestId, "player parameter is required");
            return;
        }
        
        JsonObject playerObj = args.getAsJsonObject("player");
        if (!playerObj.has("gameId")) {
            sendErrorResponse(requestId, "player object must contain gameId");
            return;
        }
        
        String gameId = playerObj.get("gameId").getAsString();
        
        // Optional parameters
        final String reason = args.has("reason") ? args.get("reason").getAsString() : "Banned by administrator";
        
        final Date expirationDate;
        if (args.has("expiresAt")) {
            String expiresAt = args.get("expiresAt").getAsString();
            try {
                expirationDate = parseISO8601Date(expiresAt);
            } catch (ParseException e) {
                sendErrorResponse(requestId, "Invalid expiresAt date format. Expected ISO 8601 format.");
                return;
            }
        } else {
            expirationDate = null;
        }
        
        try {
            UUID playerUUID = UUID.fromString(gameId);
            final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerUUID);
            
            if (targetPlayer.getName() == null) {
                sendErrorResponse(requestId, "Player not found");
                return;
            }
            
            logger.info("Banning player: " + targetPlayer.getName() + " - Reason: " + reason + 
                       (expirationDate != null ? " - Expires: " + expirationDate : " - Permanent"));
            
            // Ban player on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                banList.addBan(targetPlayer.getName(), reason, expirationDate, "Takaro");
                
                // If player is currently online, kick them
                if (targetPlayer.isOnline()) {
                    Player onlinePlayer = targetPlayer.getPlayer();
                    if (onlinePlayer != null) {
                        onlinePlayer.kickPlayer("You have been banned: " + reason);
                    }
                }
                
                // Send null response on success
                JsonObject response = new JsonObject();
                response.addProperty("type", "response");
                if (requestId != null) {
                    response.addProperty("requestId", requestId);
                }
                response.add("payload", null);
                
                logger.info("Player banned successfully: " + targetPlayer.getName());
                sendMessage(response);
            });
            
        } catch (IllegalArgumentException e) {
            sendErrorResponse(requestId, "Invalid gameId format");
        }
    }
    
    private Date parseISO8601Date(String dateString) throws ParseException {
        // Handle ISO 8601 format: 2024-01-15T10:30:00Z or 2024-01-15T10:30:00.123Z
        SimpleDateFormat[] formats = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX")
        };
        
        for (SimpleDateFormat format : formats) {
            try {
                return format.parse(dateString);
            } catch (ParseException e) {
                // Try next format
            }
        }
        
        throw new ParseException("Unable to parse date: " + dateString, 0);
    }
    
    private void handleUnbanPlayer(String requestId, JsonObject message) {
        JsonObject args = parseArgsFromMessage(message);
        
        // Validate gameId parameter
        if (!args.has("gameId")) {
            sendErrorResponse(requestId, "gameId parameter is required");
            return;
        }
        
        String gameId = args.get("gameId").getAsString();
        if (gameId == null || gameId.trim().isEmpty()) {
            sendErrorResponse(requestId, "gameId cannot be empty");
            return;
        }
        
        try {
            UUID playerUUID = UUID.fromString(gameId);
            final OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(playerUUID);
            
            if (targetPlayer.getName() == null) {
                sendErrorResponse(requestId, "Player not found");
                return;
            }
            
            logger.info("Unbanning player: " + targetPlayer.getName());
            
            // Unban player on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                BanList banList = Bukkit.getBanList(BanList.Type.NAME);
                
                if (banList.isBanned(targetPlayer.getName())) {
                    banList.pardon(targetPlayer.getName());
                    
                    // Send null response on success
                    JsonObject response = new JsonObject();
                    response.addProperty("type", "response");
                    if (requestId != null) {
                        response.addProperty("requestId", requestId);
                    }
                    response.add("payload", null);
                    
                    logger.info("Player unbanned successfully: " + targetPlayer.getName());
                    sendMessage(response);
                } else {
                    sendErrorResponse(requestId, "Player is not banned");
                }
            });
            
        } catch (IllegalArgumentException e) {
            sendErrorResponse(requestId, "Invalid gameId format");
        }
    }
    
    private void handleShutdown(String requestId, JsonObject message) {
        // No parameters according to spec
        logger.info("Server shutdown requested via Takaro");
        
        // Send response immediately before starting shutdown process
        JsonObject response = new JsonObject();
        response.addProperty("type", "response");
        if (requestId != null) {
            response.addProperty("requestId", requestId);
        }
        response.add("payload", null);
        sendMessage(response);
        
        // Announce shutdown with 30 second warning
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.broadcastMessage("§c[Takaro] Server shutting down in 30 seconds!");
            logger.info("Server shutdown initiated - 30 second countdown started");
        });
        
        // Schedule shutdown after 30 seconds
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            logger.info("Executing server shutdown");
            
            // Save all worlds
            for (World world : Bukkit.getWorlds()) {
                logger.info("Saving world: " + world.getName());
                world.save();
            }
            
            // Kick all players with shutdown message
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.kickPlayer("§cServer is shutting down");
            }
            
            // Shutdown server
            Bukkit.shutdown();
        }, 600L); // 30 seconds = 600 ticks (20 ticks per second)
    }
    
    private void sendErrorResponse(String requestId, String errorMessage) {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("type", "response");
        if (requestId != null) {
            errorResponse.addProperty("requestId", requestId);
        }
        errorResponse.addProperty("error", errorMessage);
        sendMessage(errorResponse);
    }
    
    public void sendGameEvent(String eventType, JsonObject data) {
        if (!isAuthenticated()) {
            if (plugin.getConfig().getBoolean("takaro.logging.debug", false)) {
                logger.warning("Cannot send game event - not authenticated");
            }
            return;
        }
        
        JsonObject payload = new JsonObject();
        payload.addProperty("type", eventType);
        payload.add("data", data);
        
        JsonObject eventMessage = new JsonObject();
        eventMessage.addProperty("type", "gameEvent");
        eventMessage.add("payload", payload);
        
        sendMessage(eventMessage);
        
        if (plugin.getConfig().getBoolean("takaro.logging.debug", false)) {
            logger.info("Sent game event: " + eventType);
        }
    }
    
    public JsonObject createPlayerData(Player player) {
        JsonObject playerData = new JsonObject();
        playerData.addProperty("gameId", player.getUniqueId().toString());
        playerData.addProperty("name", player.getName());
        playerData.addProperty("platformId", "minecraft:" + player.getUniqueId().toString());
        return playerData;
    }
    
    public JsonObject createPlayerDataWithDetails(Player player) {
        JsonObject playerData = createPlayerData(player);
        playerData.addProperty("ping", player.getPing());
        
        if (player.getAddress() != null && player.getAddress().getAddress() != null) {
            playerData.addProperty("ip", player.getAddress().getAddress().getHostAddress());
        }
        
        return playerData;
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