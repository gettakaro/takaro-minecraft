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
✅ Hello World command (/takaro)
✅ Build and deployment pipeline
✅ Docker development environment
✅ WebSocket client (Java-WebSocket library)
✅ Takaro authentication (identity + registration tokens)
✅ Configuration management (config.yml with WebSocket URL, tokens, logging)
✅ Error handling and recovery (reconnection logic, exponential backoff)
✅ Basic request handling (testReachability requests received)

🔲 Player data methods (getPlayer, getPlayers, getPlayerLocation, getPlayerInventory)
🔲 Game commands implementation (sendMessage, teleportPlayer, kickPlayer, banPlayer)
🔲 Event streaming (player-connected, player-disconnected, chat-message, etc.)
🔲 Item and entity listing (listItems, listEntities)
🔲 Proper request response handling (currently sending error responses)

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

### Commands Not Working

- Ensure command is registered in plugin.yml
- Check permissions in plugin.yml
- Verify command executor is set in onEnable()

### WebSocket Connection Issues

- Check server-side config.yml for correct URL and tokens
- Use `/takaro status` to verify connection state
- Use `/takaro reload` to restart WebSocket connection
- Enable debug logging: `takaro.logging.debug: true` in config.yml
- Check server logs for authentication errors

## Next Steps

1. Implement proper testReachability response (currently sending error)
2. Implement player data collection methods (getPlayer, getPlayers, etc.)
3. Add event listeners for required game events (player join/leave, chat, death)
4. Create command handlers for Takaro requests (teleport, kick, ban, sendMessage)
5. Implement item and entity listing methods
6. Add rate limiting and request queuing
7. Add comprehensive unit tests
8. Performance optimization and monitoring

## Additional Notes

- The plugin should be stateless where possible
- Use Bukkit scheduler for delayed/repeated tasks
- Prefer Spigot API over CraftBukkit internals
- Keep external dependencies minimal
- Document all public methods
- Add unit tests for critical components
- User manages config.yml on server side - do not overwrite
- WebSocket authentication uses nested payload structure

## Memory Notes

- If you have doubts about what the structure of data should be, refer to the docs at https://docs-1881.edge.takaro.dev/advanced/adding-support-for-a-new-game/#testreachability
