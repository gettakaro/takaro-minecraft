---
name: bot
description: "Control the Mineflayer test bot to perform in-game actions for testing Takaro features. Use when you need to send chat messages, trigger game events, check player status, or verify mod behavior after deploying changes. Trigger on phrases like 'send a chat', 'test the command', 'check bot status', 'make the bot do', 'trigger an event'."
---

# Mineflayer Test Bot

An HTTP-controlled Mineflayer bot running as a Docker service (`bot`). The bot auto-connects to all configured Minecraft servers and stays connected through restarts. You control it via curl to test Takaro features.

## Arguments

`$ARGUMENTS` specifies what to do. Examples: `status`, `chat paper !ping`, `attack neoforge`.

## API Reference

Base URL: `http://localhost:3001`

### Status

```bash
curl http://localhost:3001/status
```

Returns connection status for all servers (paper, neoforge, fabric).

### Chat (send a message or Takaro command)

```bash
curl -X POST http://localhost:3001/bot/<server>/chat \
  -H 'Content-Type: application/json' \
  -d '{"message": "!ping"}'
```

### Movement

```bash
curl -X POST http://localhost:3001/bot/<server>/move \
  -H 'Content-Type: application/json' \
  -d '{"x": 100, "y": 64, "z": 100}'
```

### Combat (attack nearest entity — triggers entity-killed events)

```bash
curl -X POST http://localhost:3001/bot/<server>/attack
```

### Interact with block

```bash
curl -X POST http://localhost:3001/bot/<server>/use
```

### Look at coordinates

```bash
curl -X POST http://localhost:3001/bot/<server>/look \
  -H 'Content-Type: application/json' \
  -d '{"x": 100, "y": 64, "z": 100}'
```

### Jump

```bash
curl -X POST http://localhost:3001/bot/<server>/jump
```

### Respawn (after death)

```bash
curl -X POST http://localhost:3001/bot/<server>/respawn
```

### Query endpoints

```bash
curl http://localhost:3001/bot/<server>/players     # Online players
curl http://localhost:3001/bot/<server>/position     # Bot position
curl http://localhost:3001/bot/<server>/health       # Bot health/food
curl http://localhost:3001/bot/<server>/inventory    # Bot inventory
```

Where `<server>` is `paper`, `neoforge`, or `fabric`.

## Common Workflows

### Test a Takaro command after deploy

1. Deploy the mod: `/test-deploy paper`
2. Wait for bot to reconnect: `curl http://localhost:3001/status` (check `paper.connected`)
3. Send command: `curl -X POST http://localhost:3001/bot/paper/chat -d '{"message": "!ping"}' -H 'Content-Type: application/json'`
4. Check server logs for command execution

### Trigger a player death event

```bash
# Make bot respawn if dead, then find something to fight
curl -X POST http://localhost:3001/bot/paper/respawn
# Or use server console to kill: docker compose exec paper rcon-cli kill TakaroBot_paper
```

### Verify player connect/disconnect events

```bash
# Check status — bot auto-connects, generating player-connected events
curl http://localhost:3001/status
# Restart server — bot reconnects, generating disconnect then connect events
docker compose restart paper
```

## Ensuring the bot is running

```bash
docker compose up -d bot
docker compose logs --tail=20 bot
```

## Important

- The bot auto-reconnects after server restarts with exponential backoff (5s → 60s max).
- Bot usernames are `TakaroBot_paper`, `TakaroBot_neoforge`, `TakaroBot_fabric`.
- If bot returns 503, the server is likely still restarting — wait and retry.
- All POST endpoints need `Content-Type: application/json` header.
