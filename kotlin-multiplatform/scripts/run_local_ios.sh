#!/bin/bash
set -e

if [ -z "$DITTO_TASK_ID" ]; then
    echo "Error: DITTO_TASK_ID environment variable is required"
    exit 1
fi

echo "📱 Running iOS UITest with DITTO_TASK_ID='$DITTO_TASK_ID'..."

cd iosApp

# Run XCUITest on iPhone 15 simulator
DITTO_TASK_ID="$DITTO_TASK_ID" \
xcodebuild -scheme iosApp \
 -destination 'platform=iOS Simulator,name=iPhone 15' \
 test -quiet

echo "✅ iOS integration test completed successfully!"