#!/bin/bash

# Change to the root directory
cd "$(dirname "$0")/.." || { echo "Failed to change to root directory"; exit 1; }

# Define ANSI color codes
ORANGE='\033[0;33m'
NC='\033[0m' # No Color

# Function to read the current version
get_current_version() {
    grep 'internal const val VERSION =' android/src/main/java/com/courier/android/Courier.kt | awk -F '"' '{print $2}'
}

# Function to parse the version and suggest the next version
suggest_next_version() {
    local current_version=$1
    local base_version=$(echo $current_version | awk -F '+' '{print $1}')
    local build_metadata=$(echo $current_version | awk -F '+' '{print $2}')

    if [[ -n $build_metadata ]]; then
        local next_build=$((build_metadata + 1))
        echo "${base_version}+${next_build}"
    else
        IFS='.' read -r -a version_parts <<< "$base_version"
        local next_patch=$((version_parts[2] + 1))
        echo "${version_parts[0]}.${version_parts[1]}.$next_patch"
    fi
}

# Function to update the version in Courier.kt
update_version() {
    local new_version=$1

    # Update the version in Courier.kt
    sed -i '' "s/internal const val VERSION = \".*\"/internal const val VERSION = \"$new_version\"/" android/src/main/java/com/courier/android/Courier.kt
}

# Function to update the version in README.md
update_readme_version() {
    local new_version=$1

    # Update the Groovy implementation line in README.md
    sed -i '' "s/implementation 'com.github.trycourier:courier-android:[^']*'/implementation 'com.github.trycourier:courier-android:$new_version'/" README.md

    # Update the Gradle.kts implementation line in README.md
    sed -i '' "s/implementation(\"com.github.trycourier:courier-android:[^\"]*\")/implementation(\"com.github.trycourier:courier-android:$new_version\")/" README.md
}

# Get the current version
current_version=$(get_current_version)
echo "Current version: ${ORANGE}$current_version${NC}"

# Suggest the next version
suggested_version=$(suggest_next_version "$current_version")
echo "Suggested next version: ${ORANGE}$suggested_version${NC}"

# Prompt the user for the new version
read -p "Enter the new version (or press Enter to use suggested version): " user_version
new_version=${user_version:-$suggested_version}

# Ask for confirmation
echo "You entered version ${ORANGE}$new_version${NC}"
read -p "Do you want to update the version in Courier.kt? (y/n): " confirmation

if [[ $confirmation == "y" || $confirmation == "Y" ]]; then
    update_version "$new_version"
    update_readme_version "$new_version"
    echo "Version updated to: ${ORANGE}$new_version${NC}"
else
    echo "Version update canceled."
fi