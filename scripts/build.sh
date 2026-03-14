#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

echo "Building all modules..."
./gradlew build

echo ""
echo "Build artifacts:"
find . -path "*/build/libs/*.jar" -not -name "*-dev-*" -not -name "*-sources*" | sort
