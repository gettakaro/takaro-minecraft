# Takaro Integration - Unimplemented Methods

This document tracks the remaining methods that need to be implemented for full Takaro integration.

## ✅ ~~getPlayer(gameId)~~ - IMPLEMENTED

## 1. giveItem(player, item, amount, quality)

**Purpose**: Add items to a player's inventory

**Implementation Details**:
```java
private void handleGiveItem(String requestId, JsonObject message) {
    // Parse player.gameId, item code, amount, optional quality
    // Convert item code to Material enum
    Material material = Material.getMaterial(itemCode.toUpperCase());
    
    // Create ItemStack with amount
    ItemStack itemStack = new ItemStack(material, amount);
    
    // Apply quality/durability if applicable
    if (quality != null && material.getMaxDurability() > 0) {
        short durability = (short)(material.getMaxDurability() * (1 - quality/100.0));
        itemStack.setDurability(durability);
    }
    
    // Add to player inventory
    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
    
    // Handle inventory full case - drop items at player location
    if (!leftover.isEmpty()) {
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }
    }
}
```

**Error Cases**:
- Invalid item code
- Player offline
- Invalid amount (negative or zero)

## 3. listEntities()

**Purpose**: List all entities (mobs, NPCs) in the game world

**Implementation Details**:
```java
private void handleListEntities(String requestId) {
    JsonArray entitiesArray = new JsonArray();
    
    // Iterate through all worlds
    for (World world : Bukkit.getWorlds()) {
        // Get all living entities
        for (LivingEntity entity : world.getLivingEntities()) {
            // Skip players
            if (entity instanceof Player) continue;
            
            JsonObject entityObj = new JsonObject();
            entityObj.addProperty("id", entity.getUniqueId().toString());
            entityObj.addProperty("type", entity.getType().name());
            entityObj.addProperty("name", entity.getCustomName() != null ? 
                                  entity.getCustomName() : entity.getType().name());
            
            Location loc = entity.getLocation();
            JsonObject location = new JsonObject();
            location.addProperty("x", loc.getX());
            location.addProperty("y", loc.getY());
            location.addProperty("z", loc.getZ());
            location.addProperty("world", loc.getWorld().getName());
            entityObj.add("location", location);
            
            entityObj.addProperty("health", entity.getHealth());
            entityObj.addProperty("maxHealth", entity.getMaxHealth());
            
            entitiesArray.add(entityObj);
        }
    }
}
```

**Performance Considerations**:
- This could be expensive on large servers
- Consider implementing pagination or limits
- Cache results for a short time

## 4. listLocations()

**Purpose**: List notable locations/structures in the game

**Implementation Details**:
```java
private void handleListLocations(String requestId) {
    JsonArray locationsArray = new JsonArray();
    
    // Add spawn points for each world
    for (World world : Bukkit.getWorlds()) {
        Location spawn = world.getSpawnLocation();
        JsonObject spawnObj = new JsonObject();
        spawnObj.addProperty("name", world.getName() + " Spawn");
        spawnObj.addProperty("type", "spawn");
        spawnObj.addProperty("x", spawn.getX());
        spawnObj.addProperty("y", spawn.getY());
        spawnObj.addProperty("z", spawn.getZ());
        spawnObj.addProperty("world", world.getName());
        locationsArray.add(spawnObj);
    }
    
    // Could extend to include:
    // - Nether portals (scan for portal blocks)
    // - End portals
    // - Villages (if using newer MC versions with structure API)
    // - Custom waypoints from a config file
}
```

**Extension Ideas**:
- Integration with waypoint plugins
- Structure detection using Minecraft's structure API
- Player-created locations from a database

## 5. executeConsoleCommand(command)

**Purpose**: Execute arbitrary console commands

**Implementation Details**:
```java
private void handleExecuteConsoleCommand(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    String command = args.get("command").getAsString();
    
    // Security: Validate command isn't dangerous
    // Consider implementing a whitelist of allowed commands
    
    // Execute command on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        try {
            // Capture command output by implementing a custom CommandSender
            ConsoleCommandSender console = Bukkit.getConsoleSender();
            boolean success = Bukkit.dispatchCommand(console, command);
            
            // Send response with command output
            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("output", capturedOutput);
            sendResponse(requestId, response);
        } catch (Exception e) {
            sendErrorResponse(requestId, "Command execution failed: " + e.getMessage());
        }
    });
}
```

**Security Considerations**:
- Implement command whitelist/blacklist
- Log all executed commands
- Consider permission levels
- Sanitize command input

## 6. teleportPlayer(player, x, y, z)

**Purpose**: Teleport a player to specific coordinates

**Implementation Details**:
```java
private void handleTeleportPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    JsonObject player = args.getAsJsonObject("player");
    String gameId = player.get("gameId").getAsString();
    
    double x = args.get("x").getAsDouble();
    double y = args.get("y").getAsDouble();
    double z = args.get("z").getAsDouble();
    
    // Optional: world parameter
    String worldName = args.has("world") ? args.get("world").getAsString() : null;
    
    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(gameId));
    
    // Create location
    World world = worldName != null ? Bukkit.getWorld(worldName) : targetPlayer.getWorld();
    Location teleportLocation = new Location(world, x, y, z);
    
    // Teleport on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        targetPlayer.teleport(teleportLocation);
        
        // Optional: Add teleport effects
        targetPlayer.playSound(teleportLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    });
}
```

**Safety Features**:
- Validate coordinates are within world bounds
- Check if target location is safe (not in solid block)
- Handle cross-dimension teleports

## 7. kickPlayer(player, reason)

**Purpose**: Disconnect a player from the server

**Implementation Details**:
```java
private void handleKickPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    JsonObject player = args.getAsJsonObject("player");
    String gameId = player.get("gameId").getAsString();
    String reason = args.get("reason").getAsString();
    
    Player targetPlayer = Bukkit.getPlayer(UUID.fromString(gameId));
    
    // Kick on main thread
    Bukkit.getScheduler().runTask(plugin, () -> {
        targetPlayer.kickPlayer("§c" + reason);
        
        // Log the kick
        logger.info("Kicked player " + targetPlayer.getName() + " for: " + reason);
    });
    
    // Send success response
    sendResponse(requestId, null);
}
```

## 8. banPlayer(player, reason, expiresAt)

**Purpose**: Ban a player from the server

**Implementation Details**:
```java
private void handleBanPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    JsonObject player = args.getAsJsonObject("player");
    String gameId = player.get("gameId").getAsString();
    String reason = args.get("reason").getAsString();
    
    // Parse optional expiration
    Date expirationDate = null;
    if (args.has("expiresAt")) {
        String expiresAt = args.get("expiresAt").getAsString();
        // Parse ISO 8601 date format
        expirationDate = parseISO8601Date(expiresAt);
    }
    
    // Get player (might be offline)
    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(gameId));
    
    // Ban the player
    BanList banList = Bukkit.getBanList(BanList.Type.NAME);
    banList.addBan(targetPlayer.getName(), reason, expirationDate, "Takaro");
    
    // If player is online, kick them
    if (targetPlayer.isOnline()) {
        targetPlayer.getPlayer().kickPlayer("§cYou have been banned: " + reason);
    }
}
```

**Features**:
- Support temporary bans with expiration
- Store ban source (Takaro)
- Handle offline players

## 9. unbanPlayer(gameId)

**Purpose**: Remove a player's ban

**Implementation Details**:
```java
private void handleUnbanPlayer(String requestId, JsonObject message) {
    JsonObject args = parseArgsFromMessage(message);
    String gameId = args.get("gameId").getAsString();
    
    // Get player info
    OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(UUID.fromString(gameId));
    
    // Remove from ban list
    BanList banList = Bukkit.getBanList(BanList.Type.NAME);
    
    if (banList.isBanned(targetPlayer.getName())) {
        banList.pardon(targetPlayer.getName());
        logger.info("Unbanned player: " + targetPlayer.getName());
        sendResponse(requestId, null);
    } else {
        sendErrorResponse(requestId, "Player is not banned");
    }
}
```

## 10. shutdown()

**Purpose**: Gracefully shutdown the server

**Implementation Details**:
```java
private void handleShutdown(String requestId, JsonObject message) {
    // Optional: Parse delay parameter
    JsonObject args = parseArgsFromMessage(message);
    int delaySeconds = args.has("delay") ? args.get("delay").getAsInt() : 30;
    
    // Announce shutdown to players
    Bukkit.broadcastMessage("§c[Takaro] Server shutting down in " + delaySeconds + " seconds!");
    
    // Schedule shutdown
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
        // Save all worlds
        for (World world : Bukkit.getWorlds()) {
            world.save();
        }
        
        // Kick all players with message
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer("§cServer is shutting down");
        }
        
        // Shutdown server
        Bukkit.shutdown();
    }, delaySeconds * 20L); // Convert seconds to ticks
    
    // Send response immediately
    sendResponse(requestId, null);
}
```

**Safety Features**:
- Configurable delay
- Save all world data
- Graceful player disconnection
- Warning messages

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