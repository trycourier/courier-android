#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v gum &> /dev/null; then
    echo "gum is required but not installed."
    echo "Install it with: brew install gum"
    exit 1
fi

get_current_version() {
    grep 'private const val VERSION =' android/src/main/java/com/courier/android/Courier.kt | awk -F '"' '{print $2}'
}

CURRENT=$(get_current_version)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"

gum style \
    --border rounded \
    --border-foreground 212 \
    --padding "0 2" \
    --margin "1 0" \
    "📦 Courier Android — SDK Version" \
    "" \
    "Current version: $CURRENT"

BUMP_TYPE=$(gum choose "patch → $MAJOR.$MINOR.$((PATCH + 1))" "minor → $MAJOR.$((MINOR + 1)).0" "major → $((MAJOR + 1)).0.0" "custom")

case "$BUMP_TYPE" in
    patch*)  NEW_VERSION="$MAJOR.$MINOR.$((PATCH + 1))" ;;
    minor*)  NEW_VERSION="$MAJOR.$((MINOR + 1)).0" ;;
    major*)  NEW_VERSION="$((MAJOR + 1)).0.0" ;;
    custom)  NEW_VERSION=$(gum input --placeholder "x.y.z" --prompt "Version: ") ;;
esac

if [[ -z "$NEW_VERSION" ]]; then
    echo "No version entered. Aborting."
    exit 1
fi

if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    gum style --foreground 196 "Invalid version format: $NEW_VERSION (expected x.y.z)"
    exit 1
fi

gum style \
    --border rounded \
    --border-foreground 214 \
    --padding "0 2" \
    "$CURRENT → $NEW_VERSION"

if ! gum confirm "Apply this version update?"; then
    echo "Cancelled."
    exit 0
fi

sed -i '' "s/private const val VERSION = \".*\"/private const val VERSION = \"$NEW_VERSION\"/" android/src/main/java/com/courier/android/Courier.kt

sed -i '' "s/implementation 'com.github.trycourier:courier-android:[^']*'/implementation 'com.github.trycourier:courier-android:$NEW_VERSION'/" README.md
sed -i '' "s/implementation(\"com.github.trycourier:courier-android:[^\"]*\")/implementation(\"com.github.trycourier:courier-android:$NEW_VERSION\")/" README.md

APP_BUILD_GRADLE="app/build.gradle"
CURRENT_BUILD=$(grep "versionCode" "$APP_BUILD_GRADLE" | sed 's/[^0-9]*//g')
if [[ "$CURRENT_BUILD" =~ ^[0-9]+$ ]]; then
    NEW_BUILD=$((CURRENT_BUILD + 1))
    sed -i '' "s/versionCode $CURRENT_BUILD/versionCode $NEW_BUILD/" "$APP_BUILD_GRADLE"
fi

gum style \
    --border rounded \
    --border-foreground 46 \
    --padding "0 2" \
    --margin "1 0" \
    "✅ Version updated to $NEW_VERSION" \
    "" \
    "  Courier.kt        → $NEW_VERSION" \
    "  README.md          → $NEW_VERSION" \
    "  Example versionCode → ${NEW_BUILD:-$CURRENT_BUILD}"
