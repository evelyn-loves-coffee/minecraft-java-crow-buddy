#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MODS_DIR="/home/evelyn/Apps/Minecraft/instances/Fabulously Optimized-14.0.0-beta.2/mods"

cd "$SCRIPT_DIR"

echo "Building mod..."
./gradlew build

JAR="$(ls build/libs/crowbuddy-*.jar | grep -v sources | head -1)"

if [ -z "$JAR" ]; then
    echo "ERROR: No JAR found in build/libs/"
    exit 1
fi

echo "Copying $JAR -> $MODS_DIR/"
cp "$JAR" "$MODS_DIR/"

echo "Done!"
