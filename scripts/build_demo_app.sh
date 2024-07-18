#!/bin/bash

# Change to root directory of the project
cd "$(dirname "$0")/.." || { echo "Failed to change to root directory"; exit 1; }

# Define paths
APP_BUILD_GRADLE="./app/build.gradle"

# Read current version code
CURRENT_VERSION_CODE=$(grep "versionCode" $APP_BUILD_GRADLE | sed 's/[^0-9]*//g')

# Check if CURRENT_VERSION_CODE is non-empty
if [ -z "$CURRENT_VERSION_CODE" ]; then
    echo "Failed to read current version code from $APP_BUILD_GRADLE"
    exit 1
fi

# Increment version code
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

# Update version code in build.gradle
sed -i.bak "s/versionCode $CURRENT_VERSION_CODE/versionCode $NEW_VERSION_CODE/" $APP_BUILD_GRADLE

# Check if sed command was successful
if [ $? -ne 0 ]; then
    echo "Failed to update version code in $APP_BUILD_GRADLE"
    exit 1
fi

# Remove the backup file
rm "app/build.gradle.bak"

echo "Version code incremented from $CURRENT_VERSION_CODE to $NEW_VERSION_CODE"

# Build the app bundle for release
./gradlew clean
./gradlew bundleRelease

# Check if the build was successful
if [ $? -ne 0 ]; then
    echo "Failed to build the app bundle for release"
    exit 1
fi

echo "App bundle built successfully for release"

# Open the folder where the release AAB is located
RELEASE_FOLDER="./app/build/outputs/bundle/release/"
open "$RELEASE_FOLDER"

echo "Opened folder: $RELEASE_FOLDER"