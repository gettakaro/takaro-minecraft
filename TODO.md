# Takaro Integration - Unimplemented Methods

This document tracks the remaining methods that need to be implemented for full Takaro integration.

## ✅ ~~getPlayer(gameId)~~ - IMPLEMENTED

## ✅ ~~giveItem(player, item, amount, quality)~~ - IMPLEMENTED

## ✅ ~~executeConsoleCommand(command)~~ - IMPLEMENTED

## 1. kickPlayer(player, reason)

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

## 2. banPlayer(player, reason, expiresAt)

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

## 3. unbanPlayer(gameId)

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

## 4. shutdown()

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

## 5. listEntities()

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

## 6. listLocations()

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

## 7. teleportPlayer(player, x, y, z) - ⚠️ BLOCKED

**Purpose**: Teleport a player to specific coordinates

**Status**: ⚠️ **BLOCKED** - Waiting for Takaro dimension/world support

**Note**: This method is currently blocked because Takaro does not yet support multiple dimensions/worlds. Minecraft has multiple dimensions (Overworld, Nether, End) and the teleport coordinates need to specify which world/dimension the player should be teleported to. Without this support, teleporting players could result in incorrect behavior or players being teleported to the wrong dimension.

**Request Parameters**:
- `player` (object, required): `{"gameId": "uuid"}`
- `x` (number, required): X coordinate
- `y` (number, required): Y coordinate
- `z` (number, required): Z coordinate

**Response**: null on success

**Implementation Details** (when dimension support is available):
```java
private void handleTeleportPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    
    JsonObject player = args.getAsJsonObject("player");
    String gameId = player.get("gameId").getAsString();
    
    double x = args.get("x").getAsDouble();
    double y = args.get("y").getAsDouble();
    double z = args.get("z").getAsDouble();
    
    // TODO: Add world/dimension parameter when Takaro supports it
    // String worldName = args.get("world").getAsString();
    
    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(gameId));
    
    // Teleport on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        // Currently assumes current world - needs dimension support
        Location teleportLocation = new Location(targetPlayer.getWorld(), x, y, z);
        targetPlayer.teleport(teleportLocation);
        
        // Send null response on success
        sendResponse(requestId, null);
    });
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