# Takaro Minecraft Integration

A Spigot plugin that integrates Minecraft servers with the Takaro game management platform.

## Overview

This plugin provides a bridge between Minecraft servers running Spigot and the Takaro platform, enabling remote management, monitoring, and automation capabilities.

## Features

- **WebSocket Integration**: Persistent connection to Takaro platform with authentication
- **Player Management**: Get player data, kick, ban/unban players
- **Game Commands**: Execute console commands, give items to players
- **Event Streaming**: Real-time player events (join/leave, chat, death) forwarded to Takaro
- **Server Log Forwarding**: All server console logs sent to Takaro in real-time
- **Inventory & Location Tracking**: Player inventory and location data
- **Server Management**: Remote shutdown capabilities

## Requirements

- Docker and Docker Compose
- Maven (handled via Docker)
- Java 21 (handled via Docker)
- Minecraft 1.21.5 with Spigot

## Project Structure

```
mod-minecraft/
├── plugin/                 # Spigot plugin source code
│   ├── src/main/java/      # Java source files
│   │   └── io/takaro/minecraft/
│   │       ├── TakaroPlugin.java        # Main plugin class
│   │       ├── TakaroWebSocketClient.java # WebSocket integration
│   │       ├── TakaroEventListener.java  # Game event handling
│   │       └── TakaroLogFilter.java     # Server log forwarding
│   ├── src/main/resources/ # Plugin resources
│   │   ├── plugin.yml      # Plugin metadata
│   │   └── config.yml      # Configuration template
│   ├── pom.xml            # Maven configuration
│   └── target/            # Build output
├── scripts/               # Development scripts
│   ├── build.sh           # Build plugin
│   ├── deploy.sh          # Deploy to server
│   └── reload.sh          # Reload plugin
├── .github/workflows/     # CI/CD automation
│   └── build-and-release.yml
├── _data/                 # Minecraft server data (gitignored)
├── TODO.md               # Implementation tracking
├── docker-compose.yml     # Minecraft server setup
├── docker-compose.build.yml # Build environment
└── Dockerfile.build       # Build container definition
```

## Quick Start

1. **Start the Minecraft server:**

   ```bash
   docker-compose up -d
   ```

2. **Build and deploy the plugin:**

   ```bash
   ./build.sh
   ```

3. **Configure the plugin:**
   Edit `_data/plugins/TakaroMinecraft/config.yml` to set your authentication tokens:
   ```yaml
   takaro:
     authentication:
       identity_token: "your-server-name"
       registration_token: "your-takaro-registration-token"
   ```

## Development

### Building the Plugin

The build process uses Docker to ensure consistency across different development environments:

```bash
# Build only
./scripts/build.sh

# Build, deploy, and reload
./build.sh
```

### Making Changes

1. Edit the Java source files in `plugin/src/`
2. Run `./build.sh` to compile, deploy, and reload
3. Test changes in-game immediately

### Docker Services

- **minecraft-spigot**: The Minecraft server (port 25565)
- **maven-build**: Build environment with Java 21 and Maven

### RCON Access

The server has RCON enabled for remote management:

- Port: 25575
- Password: takaro123

### Debugging

Check server logs:

```bash
docker logs minecraft-spigot -f
```

The plugin logs all activity to the server console with `[TakaroMinecraft]` prefix.

## Takaro Integration

The plugin implements the Takaro game connector specification. Key components:

1. **WebSocket Connection**: Maintains persistent connection to Takaro
2. **Command Handlers**: Implements required game commands
3. **Event System**: Streams game events to Takaro
4. **Player Management**: Syncs player data and states

For implementation details, see the [Takaro documentation](https://docs-1881.edge.takaro.dev/advanced/adding-support-for-a-new-game/).

## Configuration

The plugin uses `config.yml` for configuration. Key settings include:

### Authentication

```yaml
takaro:
  authentication:
    identity_token: "" # Unique server identifier
    registration_token: "" # Token from Takaro dashboard
```

### WebSocket Connection

```yaml
takaro:
  websocket:
    url: "wss://connect.takaro.io/"
    reconnect:
      enabled: true
      max_attempts: -1 # Unlimited reconnection attempts
```

### Logging

```yaml
takaro:
  logging:
    debug: false # Enable debug logging
    forward_server_logs: true # Forward server logs to Takaro
    min_level: "INFO" # Minimum log level to forward
```

## Plugin Integration

The plugin operates as a background service with no user-facing commands. It automatically:

- Connects to Takaro WebSocket on server startup
- Forwards all player events (join/leave, chat, death) to Takaro
- Responds to Takaro API requests for player data and server management
- Forwards server console logs to Takaro for monitoring

### Implemented Takaro API Methods

- `getPlayer(gameId)` - Get specific player data
- `getPlayers()` - Get all online players
- `getPlayerLocation(gameId)` - Get player coordinates
- `getPlayerInventory(gameId)` - Get player inventory contents
- `listItems()` - List all available items
- `listBans()` - List all banned players
- `sendMessage(message, opts)` - Send chat messages
- `giveItem(player, item, amount, quality)` - Give items to players
- `executeConsoleCommand(command)` - Execute server commands
- `kickPlayer(player, reason)` - Kick players
- `banPlayer(player, reason, expiresAt)` - Ban players
- `unbanPlayer(gameId)` - Unban players
- `shutdown()` - Gracefully shutdown server

### Pending Implementation

- `listEntities()` - List all entities in the world
- `listLocations()` - List notable locations/structures
- `teleportPlayer()` - **BLOCKED**: Waiting for Takaro dimension/world support

## Contributing

1. Follow the existing code style
2. Test all changes thoroughly
3. Update documentation as needed
4. Ensure the plugin builds successfully

## License

This project is part of the Takaro platform ecosystem.
