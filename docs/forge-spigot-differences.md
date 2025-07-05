# Forge vs Spigot API Differences

This document outlines the key differences between Forge and Spigot/Bukkit APIs for the Takaro Minecraft integration.

## Core Architecture

### Main Class
- **Spigot**: Extends `JavaPlugin`
- **Forge**: Uses `@Mod` annotation on a regular class

### Lifecycle Events
- **Spigot**: `onEnable()`, `onDisable()`, `onLoad()`
- **Forge**: `FMLCommonSetupEvent`, `FMLServerStartingEvent`, `FMLServerStoppingEvent`

## API Mappings

### Player Management
| Spigot | Forge |
|--------|-------|
| `org.bukkit.entity.Player` | `net.minecraft.server.level.ServerPlayer` |
| `Bukkit.getPlayer(UUID)` | `server.getPlayerList().getPlayer(UUID)` |
| `Bukkit.getOnlinePlayers()` | `server.getPlayerList().getPlayers()` |
| `player.getUniqueId()` | `player.getUUID()` |
| `player.getName()` | `player.getName().getString()` |
| `player.getAddress()` | `player.connection.connection.getRemoteAddress()` |
| `player.kickPlayer(reason)` | `player.connection.disconnect(Component.literal(reason))` |

### World/Dimension Management
| Spigot | Forge |
|--------|-------|
| `org.bukkit.World` | `net.minecraft.server.level.ServerLevel` |
| `Bukkit.getWorlds()` | `server.getAllLevels()` |
| `world.getName()` | `level.dimension().location().toString()` |
| `Location` | `BlockPos` + `ServerLevel` |

### Inventory and Items
| Spigot | Forge |
|--------|-------|
| `Material` | `Item` or `Block` |
| `ItemStack` | `ItemStack` (different package) |
| `player.getInventory()` | `player.getInventory()` |
| `Material.getMaterial(name)` | `ForgeRegistries.ITEMS.getValue(ResourceLocation)` |

### Events
| Spigot | Forge |
|--------|-------|
| `@EventHandler` | `@SubscribeEvent` |
| `PlayerJoinEvent` | `PlayerLoggedInEvent` |
| `PlayerQuitEvent` | `PlayerLoggedOutEvent` |
| `AsyncPlayerChatEvent` | `ServerChatEvent` |
| `PlayerDeathEvent` | `LivingDeathEvent` (check if entity is player) |
| `Bukkit.getPluginManager().registerEvents()` | `MinecraftForge.EVENT_BUS.register()` |

### Commands
| Spigot | Forge |
|--------|-------|
| `Bukkit.dispatchCommand()` | `server.getCommands().performCommand()` |
| `ConsoleCommandSender` | `server.createCommandSourceStack()` |

### Scheduling
| Spigot | Forge |
|--------|-------|
| `BukkitScheduler` | `server.tell()` for main thread |
| `runTaskAsynchronously()` | `CompletableFuture.runAsync()` |
| `runTaskLater()` | `TickEvent.ServerTickEvent` with counter |

### Server Management
| Spigot | Forge |
|--------|-------|
| `Bukkit.getServer()` | `ServerLifecycleHooks.getCurrentServer()` |
| `Bukkit.shutdown()` | `server.halt(false)` |
| `Bukkit.broadcastMessage()` | `server.getPlayerList().broadcastSystemMessage()` |

### Bans and Permissions
| Spigot | Forge |
|--------|-------|
| `Bukkit.getBanList()` | `server.getPlayerList().getBans()` |
| `BanList.addBan()` | `UserBanList.add(UserBanListEntry)` |
| `OfflinePlayer` | `GameProfile` |

## Key Implementation Differences

### Thread Safety
- **Spigot**: Uses `BukkitScheduler.runTask()` for main thread operations
- **Forge**: Uses `server.tell()` or `server.execute()` to run on main thread

### Configuration
- **Spigot**: YAML files with `FileConfiguration`
- **Forge**: TOML files with ForgeConfigSpec

### Logging
- **Spigot**: `plugin.getLogger()`
- **Forge**: `LogManager.getLogger()` with mod ID

### Resource Location
- **Spigot**: String-based names (e.g., "DIAMOND_SWORD")
- **Forge**: `ResourceLocation` objects (e.g., "minecraft:diamond_sword")

### Dimension Handling
- **Spigot**: World names like "world", "world_nether", "world_the_end"
- **Forge**: Dimension keys like `Level.OVERWORLD`, `Level.NETHER`, `Level.END`

## WebSocket Integration Notes

The WebSocket client implementation can be largely reused from Spigot with these adjustments:
1. Replace Bukkit API calls with Forge equivalents
2. Use Forge's thread management for async operations
3. Adapt event firing to use Forge's event bus
4. Update player and world queries to use Forge APIs

## Missing Spigot Features in Current Implementation

Based on the Takaro spec review, these methods are missing from the Spigot implementation:
- `listEntities()` - Should list all entities in the world
- `listLocations()` - Should list predefined locations/waypoints

These will need to be implemented in both Spigot and Forge versions.