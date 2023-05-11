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

# Install the Github CLI
if ! brew list gh >/dev/null 2>&1; then
    echo "⚠️ GitHub CLI not found. Installing via Homebrew...\n"
    brew install gh
fi

echo "✅ GitHub CLI version $(gh --version) is installed.\n"

cat android/src/main/java/com/courier/android/Courier.kt | while read LINE; do
  if [[ $LINE == *"internal const val VERSION"* ]]; then

    # Get version from Courier file
    VERSION=$(echo $LINE | sed -e 's/.*"\(.*\)".*/\1/')

    cat README.md | while read READ_ME_VERSION; do
      if [[ $READ_ME_VERSION == *"implementation 'com.github.trycourier:courier-android"* ]]; then

        # Replace README version
        NEW_READ_ME_VERSION="implementation 'com.github.trycourier:courier-android:$VERSION'"
        if [[ $READ_ME_VERSION != "" && $NEW_READ_ME_VERSION != "" ]]; then
          sed -i '.bak' "s/$READ_ME_VERSION/$NEW_READ_ME_VERSION/g" "README.md"
        fi

      fi
    done

    # Check if logged in
    if ! gh auth status >/dev/null 2>&1; then
        echo "⚠️ Logging in to GitHub...\n"
        gh auth login
    fi

    # Delete backup file
    rm "README.md.bak"

    # Get the latest release version of the apple/swift repository
    latest_version=$(gh api -X GET /repos/trycourier/courier-android/releases/latest | jq -r '.tag_name')

    # Compare the latest version with another value
    if [[ "$latest_version" == $VERSION ]]; then
        echo "❌ The latest version is already $latest_version. Please change the version number in android/src/main/java/com/courier/android/Courier.kt to push a release.\n"
        exit 1
    fi

    # Bump the version
    git add .
    git commit -m "Release"
    git push

    # Create a new tag with the version and push
    git tag $VERSION
    git push --tags
    echo "✅ $VERSION tag pushed\n"

#    # gh release create
#    gh release create $VERSION --generate-notes
#    echo "✅ $VERSION github release created\n"
#
#    # Jitpack will automatically create a new build for use
#    echo "You can see the Jitpack distribution here: https://jitpack.io/#trycourier/courier-android"

  fi
done