# Claude Development Guide

This file contains important context and instructions for AI assistants working on this project.

## Project Overview

This is a Minecraft Spigot plugin that integrates with the Takaro game management platform. The goal is to implement the Takaro connector specification for Minecraft servers.

## Key Technical Details

### Environment

- **Java Version**: 21 (via Docker)
- **Minecraft Version**: 1.21.5
- **Spigot API**: 1.21.5-R0.1-SNAPSHOT
- **Build System**: Maven (via Docker)
- **Development OS**: Linux Mint 21.1 (Ubuntu 22.04 base)

### Docker Setup

- Build environment runs in Docker container with Java 21
- Minecraft server runs in separate container
- RCON enabled on port 25575 (password: takaro123)
- Server data persisted in `_data/` directory

### Development Workflow

1. Code changes are made in `plugin/src/`
2. Run `./build.sh` to build, deploy, and reload
3. Changes take effect immediately without server restart
4. Test in-game with connected client

## Important Commands

### Building and Deployment

```bash
# Full build, deploy, and reload
./build.sh

# Individual steps
./scripts/build.sh    # Build only
./scripts/deploy.sh   # Deploy JAR to server
./scripts/reload.sh   # Reload plugin
```

### Docker Management

```bash
# Start/stop server
docker-compose up -d
docker-compose down

# View logs
docker logs minecraft-spigot -f

# Execute commands in container
docker exec minecraft-spigot rcon-cli <command>
```

## Takaro Integration Requirements

Based on the specification at https://docs-1881.edge.takaro.dev/advanced/adding-support-for-a-new-game/:

### Required WebSocket Connection

- Connect to: `wss://connect.takaro.io/`
- Send identify message with tokens
- Maintain persistent connection
- Handle reconnection logic

### Required Methods to Implement

1. `getPlayer(gameId)`
2. `getPlayers()`
3. `getPlayerLocation(gameId)`
4. `getPlayerInventory(gameId)`
5. `listItems()`
6. `listEntities()`
7. `sendMessage(message, opts)`
8. `teleportPlayer(gameId, x, y, z)`
9. `kickPlayer(gameId, reason)`
10. `banPlayer(gameId, reason)`

### Required Events to Emit

1. `player-connected`
2. `player-disconnected`
3. `chat-message`
4. `player-death`
5. `entity-killed`

## Code Style Guidelines

1. Use existing Bukkit/Spigot conventions
2. Minimize external dependencies
3. Handle all exceptions gracefully
4. Log important events and errors
5. Use async operations for network calls
6. Follow Java naming conventions

## Current Implementation Status

✅ Basic plugin structure
✅ Build and deployment pipeline
✅ Docker development environment
✅ WebSocket client (Java-WebSocket library)
✅ Takaro authentication (identity + registration tokens)
✅ Configuration management (config.yml with WebSocket URL, tokens, logging)
✅ Error handling and recovery (reconnection logic, exponential backoff)
✅ Full request/response handling for all implemented methods
✅ Player data methods (getPlayer, getPlayers, getPlayerLocation, getPlayerInventory)
✅ Game commands implementation (sendMessage, giveItem, executeConsoleCommand, kickPlayer, banPlayer, unbanPlayer, shutdown)
✅ Event streaming (player-connected, player-disconnected, chat-message, player-death)
✅ Item and ban listing (listItems, listBans)
✅ Server log forwarding (TakaroLogFilter with Log4j integration)
✅ GitHub Actions CI/CD pipeline

🔲 Entity listing (listEntities)
🔲 Location listing (listLocations)
🔲 Player teleportation (teleportPlayer) - **BLOCKED** waiting for Takaro dimension/world support

## Testing Guidelines

1. Always test commands in-game after deployment
2. Check server logs for errors
3. Verify RCON commands work correctly
4. Test with multiple players when possible
5. Ensure plugin survives server reloads

## Common Issues and Solutions

### Build Failures

- Maven dependencies are cached in Docker volume
- If issues persist, remove volume: `docker volume rm takaro-maven-cache`

### Plugin Not Loading

- Check server logs for Java version mismatch
- Ensure plugin.yml is correct
- Verify JAR is in plugins directory

### Takaro API Issues

- Check server logs for specific error messages with `[TakaroMinecraft]` prefix
- Verify player UUIDs are valid when calling player-specific methods
- Ensure server has proper permissions for file operations (for ban list access)
- Check that all required parameters are provided in Takaro API requests

### WebSocket Connection Issues

- Check server-side config.yml for correct URL and tokens
- Enable debug logging: `takaro.logging.debug: true` in config.yml
- Enable message logging: `takaro.logging.log_messages: true` for full request/response debugging
- Check server logs for authentication errors and connection status
- Verify identity_token and registration_token are correctly configured

## Next Steps

1. Implement listEntities method (list all mobs/NPCs in world)
2. Implement listLocations method (list notable locations/structures)
3. Wait for Takaro dimension/world support to implement teleportPlayer
4. Add rate limiting and request queuing for high-traffic scenarios
5. Add comprehensive unit tests for all implemented methods
6. Performance optimization and monitoring
7. Enhanced error handling for edge cases
8. Documentation for server administrators

## Additional Notes

- The plugin operates as a background service with no user-facing commands
- Use Bukkit scheduler for delayed/repeated tasks (all API calls on main thread)
- Prefer Spigot API over CraftBukkit internals
- External dependencies: Java-WebSocket, Gson (shaded), Log4j (provided)
- All public methods are documented with JavaDoc
- User manages config.yml on server side - do not overwrite existing configs
- WebSocket authentication uses nested payload structure
- Server log forwarding uses Log4j filters for real-time capture
- GitHub Actions provides automated builds and releases

## Memory Notes

- If you have doubts about what the structure of data should be, refer to the docs at https://docs-1881.edge.takaro.dev/advanced/adding-support-for-a-new-game/
- See TODO.md for detailed implementation status and remaining methods
- Most Takaro API methods are now implemented - only listEntities, listLocations, and teleportPlayer remain
- teleportPlayer is blocked waiting for Takaro to support world/dimension parameters

## AI Development Guidelines

- Whenever you finish a task, be sure to mark it as done in the TODO file