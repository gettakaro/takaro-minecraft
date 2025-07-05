#!/bin/bash

echo "Building Takaro Minecraft Forge Mod using Docker..."

# Ensure we're in the correct directory
cd "$(dirname "$0")/.." || exit 1

# Build the Docker image if needed
docker compose -f docker-compose.build-forge.yml build

# Run the build
docker compose -f docker-compose.build-forge.yml run --rm gradle-build

if [ $? -eq 0 ]; then
    # Find the actual JAR file created
    JAR_FILE=$(find forge/build/libs -name "takaro-forge-*.jar" -type f | grep -v sources | grep -v javadoc | head -n 1)
    if [ -n "$JAR_FILE" ]; then
        echo "Build successful! JAR file created at: $JAR_FILE"
    else
        echo "Build failed! No JAR file found in forge/build/libs/"
        exit 1
    fi
else
    echo "Build failed!"
    exit 1
fi