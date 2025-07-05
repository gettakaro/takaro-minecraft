#!/bin/bash

CONTAINER_NAME="minecraft-forge"

# Check if container is running
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "Minecraft Forge container is not running!"
    exit 1
fi

echo "Note: Forge does not support hot-reloading of mods."
echo "The server will be restarted to load the new mod version."
echo ""
echo "Restarting Minecraft Forge server..."

# Restart the container
docker restart "$CONTAINER_NAME"

if [ $? -eq 0 ]; then
    echo "Server restart initiated successfully!"
    echo "Wait approximately 30-60 seconds for the server to fully start."
    echo ""
    echo "You can monitor the startup with: docker logs -f $CONTAINER_NAME"
else
    echo "Failed to restart server!"
    exit 1
fi