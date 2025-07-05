# Takaro Forge Mod Configuration

The Takaro Forge mod uses Forge's standard configuration system with TOML files.

## Configuration File Location

The configuration file is automatically created when the server starts:
- **Per-world location**: `/world/serverconfig/takaromod-server.toml`
- **Default template**: `/defaultconfigs/takaromod-server.toml`

## Configuration Options

### WebSocket Configuration
```toml
[takaro.websocket]
# WebSocket URL for Takaro connection
url = "wss://connect.edge.takaro.dev"

[takaro.websocket.reconnect]
# Enable automatic reconnection
enabled = true
# Initial reconnect delay in milliseconds
initial_delay = 5000
# Maximum reconnect delay in milliseconds
max_delay = 300000
# Backoff multiplier for reconnect delays
backoff_multiplier = 2.0
# Maximum reconnect attempts (-1 for unlimited)
max_attempts = -1
```

### Authentication Configuration
```toml
[takaro.authentication]
# Identity token - unique identifier for this server instance
# Leave empty to use server name from server.properties
identity_token = "your-server-id"

# Registration token for authentication with Takaro
# Get this from your Takaro dashboard
registration_token = "your-registration-token"
```

### Logging Configuration
```toml
[takaro.logging]
# Enable debug logging for WebSocket connections
debug = false
# Log all incoming/outgoing messages (for debugging)
log_messages = false
# Forward server console logs to Takaro
forward_server_logs = true
# Minimum log level to forward (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)
min_level = "INFO"
# List of logger names to filter out
filtered_loggers = ["org.java_websocket", "io.netty"]
```

## Setting Up Your Server

1. **Get your credentials from Takaro**:
   - Login to your Takaro dashboard
   - Create a new game server
   - Copy the registration token

2. **Configure the mod**:
   - Start your Forge server once to generate the config file
   - Stop the server
   - Edit `/world/serverconfig/takaromod-server.toml`
   - Set your `registration_token`
   - Optionally set a custom `identity_token`
   - Adjust other settings as needed

3. **Start your server**:
   - The mod will automatically connect to Takaro
   - Check the logs for connection status

## Example Configuration

```toml
#Takaro Minecraft Forge Configuration
[takaro]
    [takaro.websocket]
        url = "wss://connect.edge.takaro.dev"
        [takaro.websocket.reconnect]
            enabled = true
            initial_delay = 5000
            max_delay = 300000
            backoff_multiplier = 2.0
            max_attempts = -1

    [takaro.authentication]
        identity_token = "my-forge-server"
        registration_token = "xxx"

    [takaro.logging]
        debug = false
        log_messages = false
        forward_server_logs = true
        min_level = "INFO"
        filtered_loggers = ["org.java_websocket", "io.netty"]
```

## Troubleshooting

- **Connection Failed**: Check that your registration token is correct
- **No Config File**: Make sure the server started at least once
- **Changes Not Applied**: Restart the server after editing the config