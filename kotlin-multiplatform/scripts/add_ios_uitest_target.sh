#!/bin/bash
set -e

echo "📱 Adding iOS UITest target to Xcode project..."

cd iosApp

# Use xcodegen or manual xcodeproj modification
# For now, let's use xcodebuild to create a basic test scheme
# This is a simplified approach - in practice you'd want to use xcodegen or modify the pbxproj directly

echo "⚠️  Manual step required:"
echo "1. Open iosApp.xcodeproj in Xcode"
echo "2. Add a new UI Test target named 'iosAppUITests'"
echo "3. Add the DittoSeededIdUITests.swift file to the target"
echo "4. Build scheme will be 'iosApp' with UI Test enabled"

echo "📋 For automated setup, consider using xcodegen with a project.yml file"