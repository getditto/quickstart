#!/bin/bash
set -e

if [ -z "$DITTO_TASK_ID" ]; then
    echo "Error: DITTO_TASK_ID environment variable is required"
    exit 1
fi

echo "🔧 Disabling animations for stable testing..."
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

echo "📦 Building debug and test APKs..."
./gradlew :composeApp:assembleDebug :composeApp:assembleDebugAndroidTest

echo "🧪 Running integration test with DITTO_TASK_ID='$DITTO_TASK_ID'..."
./gradlew :composeApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.DITTO_TASK_ID="$DITTO_TASK_ID"

echo "✅ Android integration test completed successfully!"