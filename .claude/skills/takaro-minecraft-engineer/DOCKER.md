# Docker Dev Servers

## Services

| Service | Platform | Game Port | RCON Port | Container |
|---------|----------|-----------|-----------|-----------|
| paper | Paper | 25565 | 25575 | minecraft-paper |
| neoforge | NeoForge | 25566 | 25576 | minecraft-neoforge |
| fabric | Fabric | 25567 | 25577 | minecraft-fabric |
| bot | Mineflayer | 3001 (API) | — | minecraft-bot |

## Starting Servers

```bash
docker compose up -d paper           # Single platform
docker compose up -d                 # All services including bot
```

## Configuration

Requires `.env` file with Takaro credentials:

```bash
cp .env.example .env
# Fill in TAKARO_WS_URL, TAKARO_REGISTRATION_TOKEN
```

Each server gets a hardcoded `TAKARO_IDENTITY_TOKEN` (e.g., `takaro-paper-dev`).

### Debug Logging

```bash
TAKARO_DEBUG=true docker compose up -d paper
```

Shows raw WebSocket message payloads in container logs.

## RCON Access

Password: `takaro123`

```bash
docker compose exec paper rcon-cli          # Interactive RCON
docker compose exec paper rcon-cli list     # Run single command
```

## Common Operations

```bash
docker compose logs --tail=50 paper         # View logs
docker compose restart paper                # Restart after deploy
docker compose ps --format json             # Check running containers
```

## Data Directories

Server data lives in `_data/<platform>/`:
- `_data/paper/plugins/` — Paper plugins (deploy target)
- `_data/neoforge/mods/` — NeoForge mods (deploy target)
- `_data/fabric/mods/` — Fabric mods (deploy target)

Config files per platform:
- Paper: `_data/paper/plugins/TakaroMinecraft/config.yml`
- NeoForge: `_data/neoforge/config/takaro.properties`
- Fabric: `_data/fabric/config/takaro.json`
