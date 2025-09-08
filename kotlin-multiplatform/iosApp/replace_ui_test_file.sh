#!/bin/bash

# Script to replace Xcode-generated UI test file with our custom implementation
# Run this after creating the iOS UI Testing Bundle target in Xcode

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
CUSTOM_TEST_FILE="$SCRIPT_DIR/iosAppUITests/iosAppUITests.swift"
XCODE_TEST_FILE="$SCRIPT_DIR/iosAppUITests/iosAppUITestsUITests.swift"
XCODE_TEST_FILE_ALT="$SCRIPT_DIR/iosAppUITests/iosAppUITests.swift"

echo "🔄 Replacing Xcode-generated UI test file with custom BrowserStack implementation..."

# Check if our custom file exists
if [ ! -f "$CUSTOM_TEST_FILE" ]; then
    echo "❌ Custom test file not found at: $CUSTOM_TEST_FILE"
    exit 1
fi

echo "✅ Found custom test file: $CUSTOM_TEST_FILE"

# Find and replace the Xcode-generated file
REPLACED=false

# Check common Xcode-generated file locations
for target_file in "$XCODE_TEST_FILE" "$XCODE_TEST_FILE_ALT" "$SCRIPT_DIR/iosAppUITests/iosAppUITestsUITests.swift"; do
    if [ -f "$target_file" ]; then
        echo "📝 Found Xcode-generated file: $target_file"
        echo "🔄 Replacing with custom implementation..."
        cp "$CUSTOM_TEST_FILE" "$target_file"
        echo "✅ Successfully replaced: $target_file"
        REPLACED=true
        break
    fi
done

# If no Xcode file found, just ensure our file is in the right place
if [ "$REPLACED" = false ]; then
    echo "💡 No Xcode-generated file found yet - ensuring our custom file is ready"
    echo "ℹ️  After creating the UI Testing Bundle target in Xcode, run this script again"
    echo "ℹ️  Or manually copy the contents of:"
    echo "     $CUSTOM_TEST_FILE"
    echo "     to the Xcode-generated test file"
fi

echo ""
echo "🎯 Custom UI test implementation ready!"
echo "📋 Features included:"
echo "   - GitHub Actions environment variable integration"
echo "   - BrowserStack document sync verification"
echo "   - Configurable timeout and comprehensive logging" 
echo "   - Follows Swift TasksUITests pattern exactly"

echo ""
echo "📖 Next steps:"
echo "1. ✅ Create iOS UI Testing Bundle target in Xcode (if not done)"
echo "2. ✅ Run this script to replace generated file (if needed)"
echo "3. ✅ Share the iosAppUITests scheme in Xcode"
echo "4. ✅ Re-enable iOS BrowserStack in CI workflow"