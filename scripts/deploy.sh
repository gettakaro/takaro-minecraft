#!/bin/bash

PLUGIN_JAR="plugin/target/takaro-minecraft-1.0.0.jar"
CONTAINER_NAME="minecraft-spigot"

# Check if the JAR exists
if [ ! -f "$PLUGIN_JAR" ]; then
    echo "Plugin JAR not found! Run build.sh first."
    exit 1
fi

# Check if container is running
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "Minecraft container is not running! Start it with: docker-compose up -d"
    exit 1
fi

echo "Deploying plugin to Minecraft server..."

# Copy the JAR to the container
docker cp "$PLUGIN_JAR" "$CONTAINER_NAME:/data/plugins/takaro-minecraft.jar"

if [ $? -eq 0 ]; then
    echo "Plugin deployed successfully!"
    echo "Use reload.sh to reload the plugin without restarting the server."
else
    echo "Failed to deploy plugin!"
    exit 1
fi