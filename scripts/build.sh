#!/bin/bash

echo "Building Takaro Minecraft Plugin using Docker..."

# Build the Docker image if needed
docker-compose -f docker-compose.build.yml build

# Run the build
docker-compose -f docker-compose.build.yml run --rm maven-build

if [ $? -eq 0 ]; then
    # Find the actual JAR file created
    JAR_FILE=$(find plugin/target -name "takaro-minecraft-*.jar" -type f | head -n 1)
    if [ -n "$JAR_FILE" ]; then
        echo "Build successful! JAR file created at: $JAR_FILE"
    else
        echo "Build failed! No JAR file found in plugin/target/"
        exit 1
    fi
else
    echo "Build failed!"
    exit 1
fi