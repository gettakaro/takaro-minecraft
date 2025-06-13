#!/bin/bash

# Main build script that runs all steps
echo "=== Takaro Minecraft Plugin Development Build ==="

# Build the plugin
echo "Step 1: Building plugin..."
./scripts/build.sh

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# Deploy to server
echo -e "\nStep 2: Deploying to server..."
./scripts/deploy.sh

if [ $? -ne 0 ]; then
    echo "Deploy failed!"
    exit 1
fi

# Reload plugin
echo -e "\nStep 3: Reloading plugin..."
./scripts/reload.sh

echo -e "\n=== Build and deploy complete! ==="
echo "You can test the plugin by running: /takaro"