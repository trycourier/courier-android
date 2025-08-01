# ENV variables
APP_FILE=app/src/main/java/com/courier/example/Env.kt
ANDROID_FILE=android/src/androidTest/java/com/courier/android/Env.kt

# Create the app file env
if [[ ! -e $APP_FILE ]]
then

  # Copy the file
  cp EnvSample.kt $APP_FILE

  # Replace the package name
  cat $APP_FILE | while read LINE; do
    if [[ $LINE == *"PACKAGE_NAME"* ]]; then
      sed -i '' "s/PACKAGE_NAME/com.courier.android/g" $APP_FILE
    fi
  done

fi

# Create the androidTest env
if [[ ! -e $ANDROID_FILE ]]
then

  # Copy the file
  cp EnvSample.kt $ANDROID_FILE

  # Replace the package name
  cat $ANDROID_FILE | while read LINE; do
    if [[ $LINE == *"PACKAGE_NAME"* ]]; then
      sed -i '' "s/PACKAGE_NAME/com.courier.android/g" $ANDROID_FILE
    fi
  done

fi

echo "🙌 Env files created"
