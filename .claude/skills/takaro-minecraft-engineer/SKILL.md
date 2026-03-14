---
name: takaro-minecraft-engineer
description: "Takaro Minecraft connector repository knowledge — architecture, build system, testing, Docker dev servers, test bot, and development workflows. Use when working on this codebase, running tests, debugging, or understanding the project."
---

# Takaro Minecraft Engineer

Multi-platform Minecraft connector for the Takaro game management platform. Implements the Takaro Generic Connector Protocol across Paper, NeoForge, and Fabric using a shared core with platform-specific adapters.

## Quick Reference

| Area | File | Key Command |
|------|------|-------------|
| Architecture | [ARCHITECTURE.md](ARCHITECTURE.md) | — |
| Build & Deploy | [BUILD.md](BUILD.md) | `./gradlew build` |
| Testing | [TESTING.md](TESTING.md) | `./gradlew :core:test` |
| Docker Dev Servers | [DOCKER.md](DOCKER.md) | `docker compose up -d paper` |
| Test Bot | [BOT.md](BOT.md) | `curl http://localhost:3001/status` |
| Integration Testing | [INTEGRATION-TESTING.md](INTEGRATION-TESTING.md) | Takaro MCP tools + bot API |

## Architecture Overview

```
                [Takaro Backend]
                        ↕ (WebSocket)
            [TakaroWebSocketClient]
            (implements EventEmitter)
                        ↕
                [TakaroConnector]
                        ↕
                 [GameAdapter] ← Interface
                        ↑
        ┌───────────────┼───────────────┐
        ↓               ↓               ↓
  [Paper Plugin]  [NeoForge Mod]  [Fabric Mod]
```

Core module is pure Java with no Minecraft dependency. Each platform module implements `GameAdapter` and translates platform events to `EventEmitter` calls.

## Project Structure

```
takaro-minecraft/
├── core/           # Pure Java — WebSocket client, config, GameAdapter interface, models
├── paper/          # Paper/Bukkit adapter (Shadow JAR)
├── neoforge/       # NeoForge adapter (ModDevGradle + Shadow)
├── fabric/         # Fabric adapter (Fabric Loom + Shadow)
├── bot/            # Mineflayer test bot with HTTP API
├── scripts/        # build.sh, deploy.sh, reload.sh
└── docker-compose.yml
```

## Maintenance

This skill should stay accurate. During work:

- **Discover something useful?** → Ask the human if it should be added
- **Find outdated info?** → Ask the human if it should be updated
- **Run `/setup-engineer`** → Bulk update from current session
