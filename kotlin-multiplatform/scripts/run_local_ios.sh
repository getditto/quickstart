#!/bin/bash
set -e

if [ -z "$DITTO_TASK_ID" ]; then
    echo "Error: DITTO_TASK_ID environment variable is required"
    exit 1
fi

echo "📱 Running iOS integration test with DITTO_TASK_ID='$DITTO_TASK_ID'..."

# Export the environment variable for the Gradle test
export DITTO_TASK_ID="$DITTO_TASK_ID"

# Run iOS Simulator tests using Kotlin Multiplatform
./gradlew :composeApp:iosSimulatorArm64Test --quiet

echo "✅ iOS integration test completed successfully!"