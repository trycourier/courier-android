#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")/.."

if ! command -v gum &> /dev/null; then
    echo "gum is required but not installed."
    echo "Install it with: brew install gum"
    exit 1
fi

if ! command -v gh &> /dev/null; then
    echo "GitHub CLI (gh) is required but not installed."
    echo "Install it with: brew install gh"
    exit 1
fi

get_current_version() {
    grep 'private const val VERSION =' android/src/main/java/com/courier/android/Courier.kt | awk -F '"' '{print $2}'
}

VERSION=$(get_current_version)
BRANCH=$(git rev-parse --abbrev-ref HEAD)

gum style \
    --border rounded \
    --border-foreground 212 \
    --padding "0 2" \
    --margin "1 0" \
    "🚀 Courier Android — Git Release" \
    "" \
    "Version: $VERSION" \
    "Branch:  $BRANCH → main"

if ! gum confirm "Commit, merge into main, tag, and create GitHub release?"; then
    echo "Cancelled."
    exit 0
fi

git status
git add -A
git commit -m "🚀 $VERSION"

git checkout main
git pull origin main
git merge --no-ff "$BRANCH" -m "Merge $BRANCH for release $VERSION"
git push origin main

git tag "$VERSION"
git push origin "$VERSION"

gh release create "$VERSION" --notes "Release $VERSION"

git checkout "$BRANCH"

gum style \
    --border rounded \
    --border-foreground 46 \
    --padding "0 2" \
    --margin "1 0" \
    "✅ Released $VERSION" \
    "" \
    "  GitHub release created" \
    "  JitPack: https://jitpack.io/#trycourier/courier-android"
