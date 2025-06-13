# Takaro Integration - Unimplemented Methods

This document tracks the remaining methods that need to be implemented for full Takaro integration.

## ✅ ~~getPlayer(gameId)~~ - IMPLEMENTED

## 1. giveItem(player, item, amount, quality)

**Purpose**: Add items to a player's inventory

**Request Parameters**:
- `player` (object, required): `{"gameId": "uuid"}`
- `item` (string, required): Item code
- `amount` (number, required)
- `quality` (string, optional)

**Response**: null on success

**Implementation Details**:
```java
private void handleGiveItem(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    
    // Parse required parameters
    JsonObject player = args.getAsJsonObject("player");
    String gameId = player.get("gameId").getAsString();
    String itemCode = args.get("item").getAsString();
    int amount = args.get("amount").getAsInt();
    
    // Parse optional quality
    String quality = args.has("quality") ? args.get("quality").getAsString() : null;
    
    // Implementation: Convert item code to Material, create ItemStack, add to inventory
    // Return null payload on success
}
```

## 2. listEntities()

**Purpose**: List all entities (mobs, NPCs) in the game world

**Request Parameters**: None

**Response**: Array of entity objects with:
- `code` (string, required): Unique entity identifier
- `name` (string, required): Display name
- `description` (string, optional): Entity description
- `type` (string, optional): "friendly" or "hostile"
- `metadata` (object, optional): Additional game-specific data

**Implementation Details**:
```java
private void handleListEntities(String requestId) {
    JsonArray entitiesArray = new JsonArray();
    
    // Get unique entity types from all worlds
    Set<EntityType> processedTypes = new HashSet<>();
    
    for (World world : Bukkit.getWorlds()) {
        for (LivingEntity entity : world.getLivingEntities()) {
            if (entity instanceof Player || processedTypes.contains(entity.getType())) continue;
            
            JsonObject entityObj = new JsonObject();
            entityObj.addProperty("code", entity.getType().name());
            entityObj.addProperty("name", formatEntityName(entity.getType()));
            entityObj.addProperty("description", getEntityDescription(entity.getType()));
            
            // Determine if hostile or friendly
            entityObj.addProperty("type", isHostile(entity) ? "hostile" : "friendly");
            
            entitiesArray.add(entityObj);
            processedTypes.add(entity.getType());
        }
    }
}
```

## 3. listLocations()

**Purpose**: List notable locations/structures in the game

**Request Parameters**: None

**Response**: Array of location objects with:
- `name` (string, required): Location name
- `code` (string, required): Unique identifier
- For circular locations:
  - `position` (object): `{"x": number, "y": number, "z": number}`
  - `radius` (number)
- For rectangular locations:
  - `position` (object): `{"x": number, "y": number, "z": number}`
  - `sizeX` (number)
  - `sizeY` (number)
  - `sizeZ` (number)
- `metadata` (object, optional)

**Implementation Details**:
```java
private void handleListLocations(String requestId) {
    JsonArray locationsArray = new JsonArray();
    
    // Add spawn points as circular locations
    for (World world : Bukkit.getWorlds()) {
        Location spawn = world.getSpawnLocation();
        JsonObject spawnObj = new JsonObject();
        spawnObj.addProperty("name", world.getName() + " Spawn");
        spawnObj.addProperty("code", "spawn_" + world.getName());
        
        JsonObject position = new JsonObject();
        position.addProperty("x", spawn.getX());
        position.addProperty("y", spawn.getY());
        position.addProperty("z", spawn.getZ());
        spawnObj.add("position", position);
        
        spawnObj.addProperty("radius", 50); // 50 block radius around spawn
        
        locationsArray.add(spawnObj);
    }
}
```

## 4. executeConsoleCommand(command)

**Purpose**: Execute arbitrary console commands

**Request Parameters**:
- `command` (string, required): The command to execute

**Response**:
- `success` (boolean): Whether command executed successfully
- `rawResult` (string, optional): Command output
- `errorMessage` (string, nullable): Error details if failed

**Implementation Details**:
```java
private void handleExecuteConsoleCommand(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    String command = args.get("command").getAsString();
    
    // Execute on main thread and capture output
    Bukkit.getScheduler().runTask(plugin, () -> {
        try {
            // Custom CommandSender to capture output
            boolean success = Bukkit.dispatchCommand(console, command);
            
            JsonObject payload = new JsonObject();
            payload.addProperty("success", success);
            payload.addProperty("rawResult", capturedOutput);
            payload.add("errorMessage", null);
            
            sendResponse(requestId, payload);
        } catch (Exception e) {
            JsonObject payload = new JsonObject();
            payload.addProperty("success", false);
            payload.add("rawResult", null);
            payload.addProperty("errorMessage", e.getMessage());
            
            sendResponse(requestId, payload);
        }
    });
}
```

## 5. teleportPlayer(player, x, y, z)

**Purpose**: Teleport a player to specific coordinates

**Request Parameters**:
- `player` (object, required): `{"gameId": "uuid"}`
- `x` (number, required): X coordinate
- `y` (number, required): Y coordinate
- `z` (number, required): Z coordinate

**Response**: null on success

**Implementation Details**:
```java
private void handleTeleportPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    
    JsonObject player = args.getAsJsonObject("player");
    String gameId = player.get("gameId").getAsString();
    
    double x = args.get("x").getAsDouble();
    double y = args.get("y").getAsDouble();
    double z = args.get("z").getAsDouble();
    
    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(gameId));
    
    // Teleport on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        Location teleportLocation = new Location(targetPlayer.getWorld(), x, y, z);
        targetPlayer.teleport(teleportLocation);
        
        // Send null response on success
        sendResponse(requestId, null);
    });
}
```

## 6. kickPlayer(player, reason)

**Purpose**: Disconnect a player from the server

**Request Parameters**:
- `player` (object, required): `{"gameId": "uuid"}`
- `reason` (string, optional): Kick reason

**Response**: null on success

**Implementation Details**:
```java
private void handleKickPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    
    JsonObject player = args.getAsJsonObject("player");
    String gameId = player.get("gameId").getAsString();
    
    // Optional reason
    String reason = args.has("reason") ? args.get("reason").getAsString() : "Kicked by administrator";
    
    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(gameId));
    
    // Kick on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        targetPlayer.kickPlayer(reason);
        sendResponse(requestId, null);
    });
}
```

## 7. banPlayer(player, reason, expiresAt)

**Purpose**: Ban a player from the server

**Request Parameters**:
- `player` (object, required): `{"gameId": "uuid"}`
- `reason` (string, optional): Ban reason
- `expiresAt` (string, optional): ISO 8601 date for ban expiration

**Response**: null on success

**Implementation Details**:
```java
private void handleBanPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    
    JsonObject player = args.getAsJsonObject("player");
    String gameId = player.get("gameId").getAsString();
    
    // Optional parameters
    String reason = args.has("reason") ? args.get("reason").getAsString() : "Banned by administrator";
    
    Date expirationDate = null;
    if (args.has("expiresAt")) {
        String expiresAt = args.get("expiresAt").getAsString();
        expirationDate = parseISO8601Date(expiresAt);
    }
    
    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(gameId));
    
    BanList banList = Bukkit.getBanList(BanList.Type.NAME);
    banList.addBan(targetPlayer.getName(), reason, expirationDate, "Takaro");
    
    if (targetPlayer.isOnline()) {
        targetPlayer.getPlayer().kickPlayer("You have been banned: " + reason);
    }
    
    sendResponse(requestId, null);
}
```

## 8. unbanPlayer(gameId)

**Purpose**: Remove a player's ban

**Request Parameters**:
- `gameId` (string, required): Player's game ID

**Response**: null on success

**Implementation Details**:
```java
private void handleUnbanPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    String gameId = args.get("gameId").getAsString();
    
    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(gameId));
    
    BanList banList = Bukkit.getBanList(BanList.Type.NAME);
    
    if (banList.isBanned(targetPlayer.getName())) {
        banList.pardon(targetPlayer.getName());
        sendResponse(requestId, null);
    } else {
        sendErrorResponse(requestId, "Player is not banned");
    }
}
```

## 9. shutdown()

**Purpose**: Gracefully shutdown the server

**Request Parameters**: None

**Response**: null on success

**Implementation Details**:
```java
private void handleShutdown(String requestId, JsonObject message) {
    // No parameters according to spec
    
    // Announce shutdown with 30 second warning
    Bukkit.broadcastMessage("[Takaro] Server shutting down in 30 seconds!");
    
    // Schedule shutdown
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        // Save all worlds
        for (World world : Bukkit.getWorlds()) {
            world.save();
        }
        
        // Kick all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("Server is shutting down");
        }
        
        // Shutdown server
        Bukkit.shutdown();
    }, 600L); // 30 seconds = 600 ticks
    
    sendResponse(requestId, null);
}
```

## General Implementation Notes

### Error Handling Pattern
All methods should follow this pattern:
1. Parse and validate arguments
2. Check if player/entity exists
3. Execute action (often on main thread)
4. Send appropriate response or error

### Thread Safety
- Most Bukkit API calls must be on the main thread
- Use `Bukkit.getScheduler().runTask()` for thread-safe execution
- WebSocket operations can be async

### Response Format
- Success: `{"type": "response", "requestId": "...", "payload": {...}}`
- Error: `{"type": "response", "requestId": "...", "error": "Error message"}`
- Null payload for void operations

### Testing Checklist
- [ ] Test with valid parameters
- [ ] Test with invalid parameters
- [ ] Test with offline players
- [ ] Test edge cases (full inventory, invalid coordinates, etc.)
- [ ] Verify thread safety
- [ ] Check performance impact