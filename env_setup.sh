#!/usr/bin/env bash
# ENV variables — same layout as courier-ios/env_setup.sh
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

APP_FILE=app/src/main/java/com/courier/example/Env.kt
TEST_FILE=android/src/androidTest/java/com/courier/android/Env.kt
SAMPLE=EnvSample.kt

if [[ ! -e $APP_FILE ]]; then
  sed 's/PACKAGE_NAME/com.courier.example/g' "$SAMPLE" > "$APP_FILE"
fi

if [[ ! -e $TEST_FILE ]]; then
  sed 's/PACKAGE_NAME/com.courier.android/g' "$SAMPLE" > "$TEST_FILE"
fi

echo "🙌 Env files created"
