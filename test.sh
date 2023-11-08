#!/bin/bash

# Run tests
./gradlew android:connectedAndroidTest

# Check the exit code of xcodebuild
if [ $? -eq 0 ]; then
  echo "✅ Tests passed.\n"
else
  echo "❌ Tests failed.\n"
  exit 1
fi