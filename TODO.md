# Takaro Integration - Unimplemented Methods

This document tracks the remaining methods that need to be implemented for full Takaro integration.

## ✅ ~~getPlayer(gameId)~~ - IMPLEMENTED

## ✅ ~~giveItem(player, item, amount, quality)~~ - IMPLEMENTED

## ✅ ~~executeConsoleCommand(command)~~ - IMPLEMENTED

## ✅ ~~kickPlayer(player, reason)~~ - IMPLEMENTED

## ✅ ~~banPlayer(player, reason, expiresAt)~~ - IMPLEMENTED

## ✅ ~~unbanPlayer(gameId)~~ - IMPLEMENTED

## ✅ ~~shutdown()~~ - IMPLEMENTED

## 1. listEntities()

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

## 2. listLocations()

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

## ✅ ~~teleportPlayer(player, x, y, z, dimension)~~ - IMPLEMENTED

**Purpose**: Teleport a player to specific coordinates with optional dimension support

**Request Parameters**:
- `player` (object, required): `{"gameId": "uuid"}`
- `x` (number, required): X coordinate
- `y` (number, required): Y coordinate
- `z` (number, required): Z coordinate
- `dimension` (string, optional): Target dimension ("overworld", "nether", "end")

**Response**: null on success

**Implementation Notes**:
- If no dimension is specified, player stays in current world
- Dimension mapping: "overworld" → any world not ending with "_nether" or "_the_end", "nether" → worlds ending with "_nether", "end" → worlds ending with "_the_end"
- Includes coordinate validation and safe teleportation checks

## 3. listEntities()

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

## 4. listLocations()

**Purpose**: List notable locations/structures in the game

**Request Parameters**: None

**Response**: Array of location objects with:
- `name` (string, required): Location name
- `code` (string, required): Unique identifier
- For circular locations:
  - `position` (object): `{"x": number, "y": number, "z": number, "dimension": string}`
  - `radius` (number)
- For rectangular locations:
  - `position` (object): `{"x": number, "y": number, "z": number, "dimension": string}`
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
        position.addProperty("dimension", mapWorldToDimension(world.getName()));
        spawnObj.add("position", position);
        
        spawnObj.addProperty("radius", 50); // 50 block radius around spawn
        
        locationsArray.add(spawnObj);
    }
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