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

    # Delete backup file
    rm "README.md.bak"

    # Bump the version
    git add .
    git commit -m "Bump"
    git push

    # Create a new tag with the version and push
    git tag $VERSION
    git push --tags

    # Ensure github is installed
    brew install gh
    gh auth login

    # gh release create
    gh release create $VERSION --generate-notes

    # Jitpack will automatically create a new build for use

  fi
done