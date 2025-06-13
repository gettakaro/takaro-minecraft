#!/bin/bash

echo "Building Takaro Minecraft Plugin using Docker..."

# Build the Docker image if needed
docker-compose -f docker-compose.build.yml build

# Run the build
docker-compose -f docker-compose.build.yml run --rm maven-build

if [ $? -eq 0 ]; then
    echo "Build successful! JAR file created at: plugin/target/takaro-minecraft-1.0.0.jar"
else
    echo "Build failed!"
    exit 1
fi