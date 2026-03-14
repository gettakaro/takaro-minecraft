---
name: test-deploy
description: "Build, deploy, restart, and verify Minecraft connector changes against Docker dev servers. Use this skill whenever the user wants to test code changes, deploy and check logs, verify Takaro connectivity, or run a build-deploy-verify cycle. Trigger on phrases like 'test the changes', 'deploy and check', 'build and test', 'check if it works', or after making connector code changes."
---

# Test Deploy

Automates the build → deploy → restart → wait-for-ready → check-logs cycle for the Takaro Minecraft connector. Reports pass/fail per platform with relevant log output. Does NOT auto-fix issues.

## Arguments

`$ARGUMENTS` specifies the platform target: `paper`, `neoforge`, `fabric`, or `all` (default: `all`).

## Steps

### 1. Build

```bash
./gradlew build
```

If the build fails, stop and report the build error. Do not continue to deploy.

### 2. Deploy

```bash
./scripts/deploy.sh <platform>
```

Where `<platform>` is the parsed target from arguments. If deploy fails, stop and report the error.

### 3. Container management

Check which containers are running:

```bash
docker compose ps --format json
```

For each target platform service:
- If the container is running, restart it: `docker compose restart <service>`
- If the container is not running, start it: `docker compose up -d <service>`

### 4. Wait for server ready

For each target platform, poll `docker compose logs --tail=50 <service>` in a loop, looking for the `"Done"` line that indicates Minecraft has finished starting. Timeout after 120 seconds. Use 5-second intervals between checks.

The ready line looks like: `Done (X.XXXs)! For help, type "help"`

### 5. Check Takaro connection

Once the server is ready, grab recent logs and look for Takaro-relevant output:

```bash
docker compose logs --tail=100 <service> 2>&1 | grep -iE "(Takaro|WebSocket|\[DEBUG\]|Identified|Authentication|identify)"
```

### 6. Report

For each platform, report:
- **Build**: pass/fail
- **Deploy**: pass/fail
- **Server ready**: pass/fail (with timeout note if applicable)
- **Takaro status**: Show the relevant log lines. Classify as:
  - **Connected**: if logs show "Identified successfully" or "Authentication confirmed"
  - **Error**: if logs show warnings/errors from Takaro
  - **No output**: if no Takaro log lines found (connector may not be loaded)

Include the raw Takaro-related log lines in the report so the user can see exactly what happened.

If `[DEBUG]` lines are present, include them in a separate "Debug output" section — these show raw WebSocket message payloads and are valuable for protocol development.

## Important

- Do NOT attempt to fix any issues found. Present the report and stop.
- If a platform fails at any step, still continue with remaining platforms (unless it was the shared build step).
- The build step is shared across all platforms, so it only runs once regardless of target.
