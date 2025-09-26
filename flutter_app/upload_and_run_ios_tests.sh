#!/bin/bash

# BrowserStack iOS Integration Test Upload and Execution Script
# This script uploads your iOS test package to BrowserStack and runs the tests

set -e

# BrowserStack credentials (replace with your actual credentials)
BS_USERNAME="teodorciuraru_g4ijVa"
BS_ACCESS_KEY="4hJsNzsc2BXiEiu9ktDt"

# Test package path
TEST_PACKAGE_PATH="build/ios_integration/Build/Products/ios_test_package.zip"

echo "🚀 BrowserStack iOS Integration Test Script"
echo "==========================================="

# Check if test package exists
if [ ! -f "$TEST_PACKAGE_PATH" ]; then
    echo "❌ Error: Test package not found at $TEST_PACKAGE_PATH"
    echo "Please run ./build_ios_test_package.sh first"
    exit 1
fi

echo "📦 Test package found: $TEST_PACKAGE_PATH"

# Step 1: Upload test package to BrowserStack
echo "⬆️  Uploading test package to BrowserStack..."

UPLOAD_RESPONSE=$(curl -s -u "$BS_USERNAME:$BS_ACCESS_KEY" \
    -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/ios/test-package" \
    -F "file=@$TEST_PACKAGE_PATH")

echo "📄 Upload response: $UPLOAD_RESPONSE"

# Extract test package URL from response
TEST_PACKAGE_URL=$(echo "$UPLOAD_RESPONSE" | grep -o '"test_package_url":"[^"]*"' | cut -d'"' -f4)

if [ -z "$TEST_PACKAGE_URL" ]; then
    echo "❌ Error: Failed to extract test package URL from upload response"
    echo "Response: $UPLOAD_RESPONSE"
    exit 1
fi

echo "✅ Test package uploaded successfully!"
echo "📋 Test Package URL: $TEST_PACKAGE_URL"

# Step 2: Run tests on BrowserStack
echo "🏃 Running tests on BrowserStack..."

# Compatible devices for Xcode 16.4 (iOS 14+)
DEVICES='["iPhone XS-14", "iPhone 11 Pro-14", "iPhone SE 2020-14", "iPhone 11 Pro Max-15", "iPhone 12-16"]'

RUN_RESPONSE=$(curl -s -u "$BS_USERNAME:$BS_ACCESS_KEY" \
    -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/ios/build" \
    -d "{\"devices\": $DEVICES, \"testPackage\":\"$TEST_PACKAGE_URL\", \"networkLogs\":\"true\", \"deviceLogs\":\"true\"}" \
    -H "Content-Type: application/json")

echo "📄 Run response: $RUN_RESPONSE"

# Extract build ID from response
BUILD_ID=$(echo "$RUN_RESPONSE" | grep -o '"build_id":"[^"]*"' | cut -d'"' -f4)

if [ -z "$BUILD_ID" ]; then
    echo "❌ Error: Failed to extract build ID from run response"
    echo "Response: $RUN_RESPONSE"
    exit 1
fi

echo "✅ Tests started successfully!"
echo "🆔 Build ID: $BUILD_ID"
echo "🌐 View results at: https://app-automate.browserstack.com/dashboard/v2/builds/$BUILD_ID"

echo ""
echo "🎉 Complete! Your iOS integration tests are now running on BrowserStack."
echo "📊 Monitor progress and view results in the BrowserStack dashboard."