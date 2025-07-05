## Tasks

- [x] 1.0 Research and Project Setup - Analyze Forge/Spigot differences and create basic mod structure
  - [x] 1.1 Research Forge vs Spigot API differences and document key mappings
  - [x] 1.2 Set up Forge development environment with latest stable version
  - [x] 1.3 Create basic mod structure with @Mod annotation and mod metadata
  - [x] 1.4 Configure gradle/maven build for Forge mod development
  - [x] 1.5 Set up Docker compose for local Forge server testing
  - [x] 1.6 Create forge/ directory structure parallel to plugin/
  - [x] 1.7 [depends on: 1.4] Create build.sh script for Forge mod compilation
  - [x] 1.8 [depends on: 1.7] Create deploy.sh script for local server deployment
  - [x] 1.9 [depends on: 1.5] Create reload.sh script for development hot-reload

- [ ] 2.0 Core WebSocket Connection - Implement basic Takaro connection with authentication
  - [ ] 2.1 [depends on: 1.3] Add WebSocket library dependency (Java-WebSocket or similar)
  - [ ] 2.2 [depends on: 2.1] Create TakaroWebSocketClient class with basic connection
  - [ ] 2.3 [depends on: 2.2] Implement authentication with identity and registration tokens
  - [ ] 2.4 [depends on: 2.3] Add connection status logging and basic error handling
  - [ ] 2.5 [depends on: 2.4] Implement reconnection logic with exponential backoff
  - [ ] 2.6 [depends on: 2.5] Create simple test command to verify connection status

- [ ] 3.0 Basic Player Methods - Implement essential player data retrieval methods
  - [ ] 3.1 [depends on: 2.0] Implement request/response message handling framework
  - [ ] 3.2 [depends on: 3.1] Implement getPlayer(gameId) method
  - [ ] 3.3 [depends on: 3.1] Implement getPlayers() method
  - [ ] 3.4 [depends on: 3.1] Implement getPlayerLocation(gameId) with dimension support
  - [ ] 3.5 [depends on: 3.1] Implement getPlayerInventory(gameId) method
  - [ ] 3.6 [depends on: 3.1] Implement testReachability() method
  - [ ] 3.7 [depends on: 3.2, 3.3] Add basic in-game testing for player methods

- [ ] 4.0 Game Commands and Events - Implement server commands and event streaming
  - [ ] 4.1 [depends on: 3.1] Implement executeConsoleCommand(command) method
  - [ ] 4.2 [depends on: 3.1] Implement sendMessage(message, opts) method
  - [ ] 4.3 [depends on: 3.1] Implement giveItem(gameId, itemId, amount) method
  - [ ] 4.4 [depends on: 3.1] Implement kickPlayer(gameId, reason) method
  - [ ] 4.5 [depends on: 3.1] Implement banPlayer/unbanPlayer/listBans methods
  - [ ] 4.6 [depends on: 2.0] Set up Forge event listeners for game events
  - [ ] 4.7 [depends on: 4.6] Implement player-connected/disconnected events
  - [ ] 4.8 [depends on: 4.6] Implement chat-message event streaming
  - [ ] 4.9 [depends on: 4.6] Implement player-death and entity-killed events
  - [ ] 4.10 [depends on: 4.6] Implement log event forwarding

- [ ] 5.0 Advanced Features and Polish - Complete remaining methods and optimize
  - [ ] 5.1 [depends on: 3.1] Implement listItems() method
  - [ ] 5.2 [depends on: 3.1] Implement listEntities() method
  - [ ] 5.3 [depends on: 3.1] Implement listLocations() method
  - [ ] 5.4 [depends on: 3.1] Implement teleportPlayer(gameId, x, y, z, dimension) method
  - [ ] 5.5 [depends on: 3.1] Implement shutdown() method
  - [ ] 5.6 [depends on: 1.3] Create Forge configuration system with config file
  - [ ] 5.7 [depends on: 5.6] Add debug and message logging configuration options
  - [ ] 5.8 [depends on: 4.0] Optimize async operations and thread safety
  - [ ] 5.9 [depends on: 5.8] Add comprehensive error handling and recovery
  - [ ] 5.10 Test with popular Forge mods for compatibility

- [ ] 6.0 CI/CD Integration - Update build pipeline for dual mod releases
  - [ ] 6.1 Update root build.sh to build both Spigot and Forge
  - [ ] 6.2 Update GitHub Actions workflow for dual mod releases
  - [ ] 6.3 Add Forge-specific tests to CI pipeline
  - [ ] 6.4 Create documentation for Forge server setup
  - [ ] 6.5 Update README with Forge installation instructions

## Relevant Files

### Phase 1 - Created Files
- `forge/src/main/java/io/takaro/minecraft/TakaroMod.java` - Main mod class with @Mod annotation
- `forge/src/main/java/io/takaro/minecraft/TakaroWebSocketClient.java` - WebSocket client placeholder
- `forge/src/main/java/io/takaro/minecraft/TakaroConfig.java` - Configuration placeholder
- `forge/src/main/resources/META-INF/mods.toml` - Mod metadata file
- `forge/src/main/resources/pack.mcmeta` - Resource pack metadata
- `forge/build.gradle` - Gradle build configuration
- `forge/gradle.properties` - Gradle properties
- `forge/settings.gradle` - Gradle settings
- `forge/.gitignore` - Git ignore file
- `scripts/build-forge.sh` - Build script for Forge mod
- `scripts/deploy-forge.sh` - Deploy script for Forge mod
- `scripts/reload-forge.sh` - Reload/restart script for Forge
- `docker-compose.build-forge.yml` - Docker compose for building
- `Dockerfile.build-forge` - Docker image for Gradle builds
- `docs/forge-spigot-differences.md` - API mapping documentation

### Forge Mod Core Files (To be implemented)
- `forge/src/main/java/io/takaro/minecraft/TakaroMessageHandler.java` - Request/response message processing

### Method Implementation Files
- `forge/src/main/java/io/takaro/forge/methods/PlayerMethods.java` - Player-related method implementations
- `forge/src/main/java/io/takaro/forge/methods/GameCommands.java` - Command execution methods
- `forge/src/main/java/io/takaro/forge/methods/WorldMethods.java` - Entity/location listing methods
- `forge/src/main/java/io/takaro/forge/methods/AdminMethods.java` - Ban/kick/shutdown methods

### Event Handling Files
- `forge/src/main/java/io/takaro/forge/events/PlayerEventHandler.java` - Player join/leave/death events
- `forge/src/main/java/io/takaro/forge/events/ChatEventHandler.java` - Chat message events
- `forge/src/main/java/io/takaro/forge/events/EntityEventHandler.java` - Entity killed events
- `forge/src/main/java/io/takaro/forge/events/LogEventHandler.java` - Server log forwarding

### Build and Configuration Files (To be implemented/updated)
- `.github/workflows/build.yml` - Updated CI/CD workflow

### Test Files
- `forge/src/test/java/io/takaro/forge/TakaroWebSocketClientTest.java` - WebSocket client tests
- `forge/src/test/java/io/takaro/forge/methods/PlayerMethodsTest.java` - Player method tests
- `forge/src/test/java/io/takaro/forge/MessageHandlerTest.java` - Message handling tests

### Documentation Files
- `forge/README.md` - Forge-specific setup and usage documentation
- `docs/forge-spigot-differences.md` - Research documentation on API differences

### Notes

- The Forge mod structure should mirror the Spigot plugin structure where possible for maintainability
- Use Forge's @SubscribeEvent annotation for event handling instead of Bukkit's event system
- Configuration should use Forge's built-in config system (likely TOML format)
- All network operations should be async to avoid blocking the server thread
- Consider using Forge's capability system for inter-mod compatibility if needed
- The mod should be server-side only and not require client installation
- Test with common Forge server mods like JEI, Waila, etc. for compatibility