IMPORTANT DOCUMENTATION:

https://docs.takaro.io/advanced/adding-support-for-a-new-game
https://docs.takaro.io/advanced/connection-architecture
https://docs.takaro.io/advanced/generic-connector-protocol

You must run the non-interactive version of verify instead of the normal command 

## Project Structure

Multi-platform Minecraft connector using Gradle multi-module build:

- `core/` — Pure Java, no MC dependency. WebSocket client, config, GameAdapter interface.
- `paper/` — Paper/Bukkit adapter. Shadow JAR with relocated deps.
- `neoforge/` — NeoForge adapter. ModDevGradle + Shadow.
- `fabric/` — Fabric adapter. Fabric Loom + Shadow.

## Build

Requires Java 21. Uses Gradle 8.12 with version catalog (`gradle/libs.versions.toml`).

```bash
./gradlew build          # Build all modules
./gradlew :paper:build   # Build single module
```

## Dev Servers

```bash
docker compose up -d paper      # Paper on :25565
docker compose up -d neoforge   # NeoForge on :25566
docker compose up -d fabric     # Fabric on :25567
```

Deploy: `./scripts/deploy.sh paper|neoforge|fabric|all`

Debug logging: `TAKARO_DEBUG=true docker compose up -d paper` — shows raw WebSocket messages in logs.

## Current Status

Phase 1 scaffold — only `testReachability` is implemented. Full protocol implementation comes in Phase 2.
