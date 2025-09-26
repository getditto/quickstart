#!/bin/bash

# Build iOS test package for BrowserStack
echo "Building iOS integration test package for BrowserStack..."

# Step 1: Clean and prepare
echo "Cleaning previous builds..."
rm -rf build/ios_integration
mkdir -p build/ios_integration

# Step 2: Build the Flutter app for iOS
echo "Building Flutter app for iOS..."
flutter build ios integration_test/app_test.dart --release

# Step 3: Build test package using xcodebuild
echo "Building test package with xcodebuild..."
cd ios

# Build for device (iphoneos) as required by BrowserStack
xcodebuild \
  -workspace Runner.xcworkspace \
  -scheme Runner \
  -configuration Release \
  -sdk iphoneos \
  -derivedDataPath ../build/ios_integration \
  build-for-testing \
  CODE_SIGNING_ALLOWED=NO

cd ..

# Step 4: Create test package zip
echo "Creating test package zip..."
cd build/ios_integration/Build/Products

# Find the xctestrun file
XCTESTRUN_FILE=$(find . -name "*.xctestrun" | head -1)
if [ -z "$XCTESTRUN_FILE" ]; then
    echo "Error: Could not find .xctestrun file"
    exit 1
fi

echo "Found xctestrun file: $XCTESTRUN_FILE"

# Check if Runner.app exists (should be Release-iphoneos now)
if [ ! -d "Release-iphoneos/Runner.app" ]; then
    echo "Error: Runner.app not found in Release-iphoneos directory"
    find . -name "Runner.app" -type d
    exit 1
fi

echo "Found Runner.app at: Release-iphoneos/Runner.app"

# Create the zip package with the Release-iphoneos directory and xctestrun file
zip -r "ios_test_package.zip" "Release-iphoneos" "$XCTESTRUN_FILE"

echo "iOS test package created: build/ios_integration/Build/Products/ios_test_package.zip"
echo "✅ iOS test package build completed successfully!"