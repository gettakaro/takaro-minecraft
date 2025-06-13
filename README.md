# Takaro Minecraft Integration

A Spigot plugin that integrates Minecraft servers with the Takaro game management platform.

## Overview

This plugin provides a bridge between Minecraft servers running Spigot and the Takaro platform, enabling remote management, monitoring, and automation capabilities.

## Features

- WebSocket connection to Takaro platform
- Player management (kick, ban, teleport)
- Chat message handling
- Inventory and location tracking
- Event streaming (player join/leave, deaths, chat)
- Command execution

## Requirements

- Docker and Docker Compose
- Maven (handled via Docker)
- Java 21 (handled via Docker)
- Minecraft 1.21.5 with Spigot

## Project Structure

```
mod-minecraft/
├── plugin/                 # Spigot plugin source code
│   ├── src/               # Java source files
│   ├── pom.xml            # Maven configuration
│   └── target/            # Build output
├── scripts/               # Development scripts
│   ├── build.sh           # Build plugin
│   ├── deploy.sh          # Deploy to server
│   └── reload.sh          # Reload plugin
├── _data/                 # Minecraft server data (gitignored)
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

3. **Test the plugin in-game:**
   ```
   /takaro
   /takaro status
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

Check plugin-specific logs in-game or in the server console.

## Takaro Integration

The plugin implements the Takaro game connector specification. Key components:

1. **WebSocket Connection**: Maintains persistent connection to Takaro
2. **Command Handlers**: Implements required game commands
3. **Event System**: Streams game events to Takaro
4. **Player Management**: Syncs player data and states

For implementation details, see the [Takaro documentation](https://docs-1881.edge.takaro.dev/advanced/adding-support-for-a-new-game/).

## Commands

- `/takaro` - Main plugin command
- `/takaro status` - Show plugin and server status
- More commands will be added as integration progresses

## Contributing

1. Follow the existing code style
2. Test all changes thoroughly
3. Update documentation as needed
4. Ensure the plugin builds successfully

## License

This project is part of the Takaro platform ecosystem.