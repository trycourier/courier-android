#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/.." || exit 1

RELEASE_FOLDER="./app/build/outputs/bundle/release/"

echo "📦 Building Example app bundle for release..."
./gradlew clean bundleRelease || { echo "❌ Build failed"; exit 1; }

open "$RELEASE_FOLDER"

echo "✅ Build completed and opened in Finder"
