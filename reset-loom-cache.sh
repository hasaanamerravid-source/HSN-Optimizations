#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./gradlew --stop || true
rm -rf .gradle
rm -rf "$HOME/.gradle/caches/fabric-loom"
echo "Loom caches cleared. Run: ./gradlew runClient"
