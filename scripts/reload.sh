#!/bin/bash

CONTAINER_NAME="minecraft-spigot"
RCON_PASSWORD="takaro123"
RCON_PORT="25575"

# Check if container is running
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "Minecraft container is not running!"
    exit 1
fi

echo "Reloading Takaro plugin..."

# Install mcrcon if not present
if ! command -v mcrcon &> /dev/null; then
    echo "Installing mcrcon..."
    # Try to install using docker exec instead
    docker exec "$CONTAINER_NAME" rcon-cli reload confirm
else
    # Use mcrcon if available
    mcrcon -H localhost -P "$RCON_PORT" -p "$RCON_PASSWORD" "reload confirm"
fi

# Alternative: Use docker exec with rcon-cli (included in itzg/minecraft-server image)
docker exec "$CONTAINER_NAME" rcon-cli reload confirm

if [ $? -eq 0 ]; then
    echo "Plugin reloaded successfully!"
else
    echo "Failed to reload plugin. You may need to restart the server."
fi