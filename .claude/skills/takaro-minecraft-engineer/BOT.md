# Test Bot

Mineflayer-based test bot running as a Docker service. Auto-connects to all configured Minecraft servers and exposes an HTTP API for agent-driven testing.

## Overview

- **Service**: `bot` in docker-compose.yml
- **Container**: `minecraft-bot`
- **API Port**: 3001 (exposed to host)
- **Bot usernames**: `TakaroBot_paper`, `TakaroBot_neoforge`, `TakaroBot_fabric`
- **Auto-reconnect**: Exponential backoff from 5s to 60s max

## Starting

```bash
docker compose up -d bot                    # Start bot
docker compose logs --tail=20 bot           # Check connection status
```

The bot starts automatically with `docker compose up -d` alongside servers.

## HTTP API

Base URL: `http://localhost:3001`

Use the `/bot` skill for full API reference and example curl commands.

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/status` | Connection status for all servers |
| POST | `/bot/:server/chat` | Send chat message (e.g., Takaro commands) |
| POST | `/bot/:server/attack` | Attack nearest entity |
| POST | `/bot/:server/move` | Walk to coordinates |
| GET | `/bot/:server/players` | List online players |
| GET | `/bot/:server/position` | Bot's current position |
| GET | `/bot/:server/health` | Bot's health/food |
| GET | `/bot/:server/inventory` | Bot's inventory |

Where `:server` is `paper`, `neoforge`, or `fabric`.

## Common Test Workflows

### Test Takaro command after deploy

```bash
curl http://localhost:3001/status                    # Verify bot connected
curl -X POST http://localhost:3001/bot/paper/chat \
  -H 'Content-Type: application/json' \
  -d '{"message": "+ping"}'                           # Send command
```

### Trigger game events for Takaro

- **Player connect/disconnect**: Bot auto-generates these on connect
- **Chat message**: Use `/bot/:server/chat`
- **Entity killed**: Use `/bot/:server/attack` (targets hostile mobs and players — `e.type === 'mob' || e.type === 'player'`, not `animal`)
- **Player death**: Kill bot via RCON: `docker compose exec paper rcon-cli kill TakaroBot_paper` (must call `/bot/:server/respawn` after)

## Important

- The Takaro command prefix is `+` (not `!`). Confirm via `mcp__takaro__settingsGet`.
- All POST endpoints need `Content-Type: application/json` header.
- Bot does NOT auto-respawn after death — call `POST /bot/:server/respawn` to respawn.
- `attack()` targets hostile mobs and players (`e.type === 'mob' || e.type === 'player'`), not passive animals.
- To verify events actually reached Takaro (not just that the bot sent them), use Takaro MCP tools. See [INTEGRATION-TESTING.md](INTEGRATION-TESTING.md).

## Source Code

Located in `bot/`:
- `src/index.js` — Entry point, starts bots + HTTP server
- `src/bot-instance.js` — Single bot connection lifecycle and actions
- `src/api.js` — Express HTTP API
- `src/config.js` — Environment variable parsing
