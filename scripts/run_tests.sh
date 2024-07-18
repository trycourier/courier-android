#!/bin/bash

cd "$(dirname "$0")/.." || { echo "Failed to change to root directory"; exit 1; }

# Use a specific Gradle version (if needed)
./gradlew wrapper --gradle-version 8.6

# Prompt user to start emulator manually
echo "🚧 Please start the emulator manually. Press Enter to continue."
read

# Run tests using Gradle wrapper
./gradlew clean android:connectedDebugAndroidTest

# Check the exit code of Gradle
if [ $? -eq 0 ]; then
  echo "✅ Tests passed."
else
  echo "❌ Tests failed."
  exit 1
fi