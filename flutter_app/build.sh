#! /usr/bin/env bash

set -euo pipefail

ROOT=$(git rev-parse --show-toplevel)
FLUTTER_APP=$ROOT/flutter_app
ANDROID_APP=$FLUTTER_APP/android



# Gradle expects dart-defines to be passed in via a special parameter where
# each key-value pair is encoded as: `base64("$key=$value")`
function encode_define() {
  # The name of the param (e.g. "DITTO_APP_ID")
  PARAM=$1 

  # ${!PARAM} is bash syntax to read the variable whose name is stored in
  # `$PARAM`, not to read `$PARAM` directly
  TEXT="$PARAM=${!PARAM}"
  
  echo -n "$TEXT" | base64
}


source "$FLUTTER_APP/.env"

DART_DEFINES="$(encode_define DITTO_APP_ID)"
DART_DEFINES="$DART_DEFINES,$(encode_define DITTO_DATABASE_ID)"
DART_DEFINES="$DART_DEFINES,$(encode_define DITTO_PLAYGROUND_TOKEN)"
DART_DEFINES="$DART_DEFINES,$(encode_define DITTO_WEBSOCKET_URL)"
DART_DEFINES="$DART_DEFINES,$(encode_define DITTO_AUTH_URL)"
DART_DEFINES="$DART_DEFINES,$(encode_define DITTO_CLOUD_ENDPOINT)"
DART_DEFINES="$DART_DEFINES,$(encode_define DITTO_API_KEY)"

cd "$FLUTTER_APP"
flutter pub get

cd "$ANDROID_APP"

# Build the prod app
./gradlew \
  -Pdart-defines="$DART_DEFINES" \
  app:assembleDebugAndroidTest


# Build the integration test app
./gradlew \
  app:assembleDebug \
  -Pdart-defines="$DART_DEFINES" \
  -Ptarget="$FLUTTER_APP/integration_test/ditto_sync_test.dart" 

APP_PATH="$FLUTTER_APP/build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
TEST_PATH="$FLUTTER_APP/build/app/outputs/flutter-apk/app-debug.apk"

# Upload both apps

BS_APP_UPLOAD_RESPONSE=$(
  curl -u "$BROWSERSTACK_BASIC_AUTH" \
    --fail-with-body \
    -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/app" \
    -H "Accept: application/json" \
    -F "file=@$APP_PATH"
)
echo "uploaded app: $BS_APP_UPLOAD_RESPONSE"
BS_APP_URL=$(echo BS_UPLOAD_RESPONSE | jq -r .app_url)

BS_TEST_UPLOAD_RESPONSE=$(
  curl -u "$BROWSERSTACK_BASIC_AUTH" \
    --fail-with-body \
    -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/test-suite" \
    -H "Accept: application/json" \
    -F "file=@$TEST_PATH" 
)
echo "uploaded test: $BS_TEST_UPLOAD_RESPONSE"
BS_TEST_URL=$(echo BS_TEST_UPLOAD_RESPONSE | jq -r .test_suite_url)


# Trigger a test run

PAYLOAD=$(
  jq -n \
    --arg app "$BS_APP_URL" \
    --arg testSuite "$BS_TEST_URL" \
    --arg devices '[ "Google Pixel 3-9.0" ]' 

)

echo "PAYLOAD: $PAYLOAD"

RES=$(
  curl -u "cameronmcloughli_ydF6Jb:oMLRqvyc1xpc6zuxFy3D" \
    -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/build" \
    -d "$PAYLOAD" \
    -H "Content-Type: application/json"
)

# Report status

echo "Build ID: $(echo "$RES" | jq -r .build_id)"
echo "Status  : $(echo "$RES" | jq -r .message)"

if [[ $(echo "$RES" | jq -r .message) != "Success" ]]; then
  exit 1
fi

