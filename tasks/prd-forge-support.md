# Product Requirements Document: Minecraft Forge Support for Takaro

## Introduction/Overview

This document outlines the requirements for implementing Takaro game management platform support for Minecraft Forge servers. This implementation will exist alongside the current Spigot plugin, providing the same Takaro connector functionality for Forge-based Minecraft servers. The goal is to enable Forge server administrators to integrate their servers with Takaro's comprehensive game management features.

## Goals

1. Implement a fully functional Takaro connector for Minecraft Forge servers that matches the Spigot implementation's capabilities
2. Maintain feature parity with the Takaro connector specification
3. Provide idiomatic Forge mod development patterns and code structure
4. Enable simultaneous CI/CD builds and releases with the Spigot plugin
5. Support the latest stable Minecraft Forge version with a path for future version compatibility
6. Create development tooling that mirrors the Spigot workflow for consistency

## User Stories

1. **As a Forge server administrator**, I want to connect my Minecraft Forge server to Takaro so that I can manage my server through Takaro's web interface
2. **As a Forge server administrator**, I want the mod to automatically reconnect to Takaro if the connection is lost so that my server management remains uninterrupted
3. **As a developer**, I want build and deployment scripts that work similarly to the Spigot version so that I can maintain both implementations efficiently
4. **As a player on a Forge server**, I want my gameplay data (inventory, location, stats) to be accessible through Takaro so that server features like economy and rewards work properly
5. **As a server administrator**, I want to execute console commands remotely through Takaro so that I can manage my server without direct console access
6. **As a modpack creator**, I want the Takaro mod to be compatible with other Forge mods so that I can include it in my modpack

## Functional Requirements

### Core Architecture
1. The mod must be developed as a Minecraft Forge mod using the latest stable Forge version (currently targeting Minecraft 1.21.x with corresponding Forge/NeoForge)
2. The mod ID must be "takaro" with an appropriate display name
3. The mod must be structured as a separate module adjacent to the Spigot plugin in the repository

### WebSocket Connection Management
4. The mod must establish a WebSocket connection to `wss://connect.takaro.io/`
5. The mod must authenticate using both identity token and registration token
6. The mod must implement automatic reconnection with exponential backoff on connection failure
7. The mod must maintain the WebSocket connection throughout the server lifecycle

### Required Method Implementations
8. The mod must implement `getPlayer(gameId)` - retrieve specific player information
9. The mod must implement `getPlayers()` - retrieve all online players
10. The mod must implement `getPlayerLocation(gameId)` - get player coordinates and dimension
11. The mod must implement `getPlayerInventory(gameId)` - retrieve player inventory contents
12. The mod must implement `giveItem(gameId, itemId, amount, quality)` - give items to players
13. The mod must implement `listItems()` - list all available items in the game
14. The mod must implement `listEntities()` - list all entities in the world
15. The mod must implement `listLocations()` - list notable locations/structures
16. The mod must implement `executeConsoleCommand(command)` - execute server console commands
17. The mod must implement `sendMessage(message, opts)` - send chat messages
18. The mod must implement `teleportPlayer(gameId, x, y, z, dimension)` - teleport players
19. The mod must implement `testReachability()` - verify server connectivity
20. The mod must implement `kickPlayer(gameId, reason)` - kick players from server
21. The mod must implement `banPlayer(gameId, reason)` - ban players
22. The mod must implement `unbanPlayer(gameId)` - unban players
23. The mod must implement `listBans()` - list all server bans
24. The mod must implement `shutdown()` - gracefully shutdown the server

### Event Streaming
25. The mod must emit `log` events for server log forwarding
26. The mod must emit `player-connected` events when players join
27. The mod must emit `player-disconnected` events when players leave
28. The mod must emit `chat-message` events for all chat messages
29. The mod must emit `player-death` events with death details
30. The mod must emit `entity-killed` events when entities are killed by players

### Configuration
31. The mod must support configuration via Forge's configuration system
32. Configuration must include WebSocket URL (with default to production)
33. Configuration must include identity and registration tokens
34. Configuration must include debug logging toggle
35. Configuration must include message logging toggle for debugging

### Build and Deployment
36. The mod must have a `build.sh` script for compilation
37. The mod must have a `deploy.sh` script for local server deployment
38. The mod must have a `reload.sh` script for hot-reloading during development
39. The mod must integrate with the existing CI/CD pipeline for simultaneous releases

### Error Handling
40. The mod must gracefully handle all network errors
41. The mod must provide meaningful error messages in logs
42. The mod must not crash the server on Takaro connection failures
43. The mod must validate all input parameters before processing

## Non-Goals (Out of Scope)

1. This implementation will NOT provide a user interface within Minecraft
2. This implementation will NOT modify core Minecraft gameplay mechanics
3. This implementation will NOT require client-side installation (server-side only)
4. This implementation will NOT support legacy Minecraft versions initially
5. This implementation will NOT implement custom Takaro features beyond the specification
6. This implementation will NOT handle Takaro authentication/registration (admin must provide tokens)

## Design Considerations

### Code Structure
- Follow Forge mod conventions and best practices
- Use Forge's event system for game event handling
- Implement proper sided (server-side) execution
- Use Forge's networking capabilities appropriately

### Comparison with Spigot Implementation
- Research task required to identify key differences between Forge and Spigot APIs
- Document mapping between Bukkit/Spigot APIs and Forge equivalents
- Identify any features that require different implementation approaches

### Compatibility
- Ensure compatibility with common Forge mod loaders
- Test with popular server-side Forge mods
- Consider Forge's capability system for inter-mod compatibility

## Technical Considerations

### Dependencies
- Use appropriate WebSocket library compatible with Forge environment
- Minimize external dependencies to reduce conflicts
- Shade dependencies appropriately to avoid classpath issues

### Performance
- Implement asynchronous operations for network calls
- Use Forge's scheduling system for delayed/repeated tasks
- Avoid blocking the main server thread

### Forge-Specific Patterns
- Use Forge's event bus for game events
- Implement proper mod lifecycle handling (@Mod annotation)
- Use Forge's server lifecycle events
- Handle dimension/world access through Forge APIs

### Development Environment
- Docker setup for local Forge server testing
- Hot-reload capability for rapid development
- Integration with existing development workflow

## Success Metrics

1. All 17 required Takaro methods successfully implemented and tested
2. All 6 required game events properly emitted to Takaro
3. Successful authentication and persistent WebSocket connection
4. Zero server crashes related to the mod in a 24-hour test period
5. Successful CI/CD integration building both Spigot and Forge releases
6. Response time for Takaro requests under 100ms for standard operations
7. Successful deployment and operation on at least 3 different Forge modpacks

## Open Questions

1. Should we target traditional Minecraft Forge or the newer NeoForge?
2. Are there specific Forge mods that we need to ensure compatibility with?
3. How should we handle Forge's client-server synchronization for features like inventory?
4. Should configuration use Forge's annotation-based config or traditional file-based?
5. What specific version of Forge/Minecraft should we target initially?
6. How do we handle Forge's dimension system vs Takaro's dimension support?
7. Should we implement any Forge-specific optimizations not present in Spigot?

## Implementation Tasks

1. **Research Phase**: Analyze differences between Spigot and Forge development
2. **Project Setup**: Create Forge mod structure and development environment
3. **Core Implementation**: Implement WebSocket client and Takaro protocol
4. **Method Implementation**: Implement all 17 required Takaro methods
5. **Event System**: Implement all 6 required event emissions
6. **Configuration System**: Implement Forge-appropriate configuration
7. **Build System**: Create build/deploy/reload scripts
8. **CI/CD Integration**: Update GitHub Actions for dual releases
9. **Testing**: Comprehensive testing with various Forge setups
10. **Documentation**: Create setup and configuration documentation