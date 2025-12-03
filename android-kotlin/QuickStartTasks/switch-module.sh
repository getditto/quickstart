#!/bin/bash

# Script to switch between app and dittowrapper modules
# Usage: ./switch-module.sh [app|dittowrapper]

set -e

MODULE=$1
SETTINGS_FILE="settings.gradle.kts"
BUILD_FILE="build.gradle.kts"

if [ -z "$MODULE" ]; then
    echo "Usage: $0 [app|dittowrapper]"
    echo ""
    echo "  app          - Configure to build app module (Kotlin 1.7.20)"
    echo "  dittowrapper - Configure to build dittowrapper module (Kotlin 1.9.23)"
    exit 1
fi

# Function to uncomment a line (removes leading //)
uncomment_line() {
    local file=$1
    local pattern=$2
    sed -i.bak "s|^//\(${pattern}\)|\1|" "$file"
}

# Function to comment a line (adds leading //)
comment_line() {
    local file=$1
    local pattern=$2
    # Only add // if not already commented
    sed -i.bak "s|^\(${pattern}\)|//\1|" "$file"
    # Fix double-commenting (change //// to //)
    sed -i.bak "s|^////|//|" "$file"
}

case "$MODULE" in
    app)
        echo "Configuring build for app module (Kotlin 1.7.20)..."

        # Update settings.gradle.kts
        uncomment_line "$SETTINGS_FILE" 'include(":app")'
        comment_line "$SETTINGS_FILE" 'include(":dittowrapper")'

        # Update build.gradle.kts
        uncomment_line "$BUILD_FILE" '        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")'

        echo "✓ Configuration updated for app module"
        echo ""
        echo "Next steps:"
        echo "  1. Select the correct build configuration in Android Studio"
        echo "  2. Sync Gradle (File → Sync Project with Gradle Files)"
        echo "  3. Build and run using the play button"
        ;;

    dittowrapper)
        echo "Configuring build for dittowrapper module (Kotlin 1.9.23)..."

        # Update settings.gradle.kts
        comment_line "$SETTINGS_FILE" 'include(":app")'
        uncomment_line "$SETTINGS_FILE" 'include(":dittowrapper")'

        # Update build.gradle.kts
        comment_line "$BUILD_FILE" '        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")'

        echo "✓ Configuration updated for dittowrapper module"
        echo ""
        echo "Next steps:"
        echo "  1. Select the correct build configuration in Android Studio"
        echo "  2. Sync Gradle (File → Sync Project with Gradle Files)"
        echo "  3. Build and run using the play button"
        ;;

    *)
        echo "Error: Invalid module '$MODULE'"
        echo "Usage: $0 [app|dittowrapper]"
        exit 1
        ;;
esac

# Clean up backup files
rm -f "${SETTINGS_FILE}.bak" "${BUILD_FILE}.bak"

echo ""
echo "Configuration files updated successfully!"