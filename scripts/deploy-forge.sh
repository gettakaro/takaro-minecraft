#!/bin/bash

# Find the actual JAR file
MOD_JAR=$(find forge/build/libs -name "takaro-forge-*.jar" -type f | grep -v sources | grep -v javadoc | head -n 1)
CONTAINER_NAME="minecraft-forge"

# Check if the JAR exists
if [ -z "$MOD_JAR" ] || [ ! -f "$MOD_JAR" ]; then
    echo "Mod JAR not found! Run build-forge.sh first."
    exit 1
fi

echo "Using JAR file: $MOD_JAR"

# Check if container is running
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo "Minecraft Forge container is not running! Start it with: docker-compose up -d"
    exit 1
fi

echo "Deploying mod to Minecraft Forge server..."

# Copy the JAR to the container
docker cp "$MOD_JAR" "$CONTAINER_NAME:/data/mods/takaro-forge.jar"

if [ $? -eq 0 ]; then
    echo "Mod deployed successfully!"
    echo "Note: Forge requires a server restart to load new mods."
    echo "Use reload-forge.sh to restart the server."
else
    echo "Failed to deploy mod!"
    exit 1
fi