#!/bin/bash

# BrowserStack API Integration for KMP Android Testing
# This script uploads APKs and runs Espresso tests on real devices

set -e

# Configuration
BROWSERSTACK_USERNAME="${BROWSERSTACK_USERNAME:-}"
BROWSERSTACK_ACCESS_KEY="${BROWSERSTACK_ACCESS_KEY:-}"
GITHUB_TEST_DOC_ID="${GITHUB_TEST_DOC_ID:-Clean the kitchen}"
BUILD_DIR="composeApp/build/outputs"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    if [ -z "$BROWSERSTACK_USERNAME" ] || [ -z "$BROWSERSTACK_ACCESS_KEY" ]; then
        error "BrowserStack credentials not set!"
        echo "Please set BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY environment variables"
        echo "Example:"
        echo "export BROWSERSTACK_USERNAME='your_username'"
        echo "export BROWSERSTACK_ACCESS_KEY='your_access_key'"
        exit 1
    fi
    
    if ! command -v curl &> /dev/null; then
        error "curl is required but not installed"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        warning "jq not found - JSON parsing will be limited"
    fi
    
    success "Prerequisites check completed"
}

# Build APKs
build_apks() {
    log "Building Android APKs..."
    
    # Build main APK
    ./gradlew :composeApp:assembleDebug || {
        error "Failed to build main APK"
        exit 1
    }
    
    # Build test APK
    ./gradlew :composeApp:assembleDebugAndroidTest || {
        error "Failed to build test APK"
        exit 1
    }
    
    # Find the built APKs
    MAIN_APK=$(find $BUILD_DIR -name "*debug*.apk" -not -name "*test*" | head -1)
    TEST_APK=$(find $BUILD_DIR -name "*debug*test*.apk" | head -1)
    
    if [ ! -f "$MAIN_APK" ]; then
        error "Main APK not found in $BUILD_DIR"
        exit 1
    fi
    
    if [ ! -f "$TEST_APK" ]; then
        error "Test APK not found in $BUILD_DIR"
        exit 1
    fi
    
    success "APKs built successfully:"
    log "Main APK: $MAIN_APK"
    log "Test APK: $TEST_APK"
}

# Upload APK to BrowserStack
upload_apk() {
    local apk_path=$1
    local apk_type=$2
    
    log "Uploading $apk_type APK to BrowserStack..."
    
    local response=$(curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
        -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/app" \
        -F "file=@$apk_path" \
        -F "data={\"custom_id\":\"${apk_type}_$(date +%Y%m%d_%H%M%S)\"}")
    
    if command -v jq &> /dev/null; then
        local app_url=$(echo "$response" | jq -r '.app_url // empty')
        local error_msg=$(echo "$response" | jq -r '.error // empty')
        
        if [ -n "$error_msg" ]; then
            error "Upload failed: $error_msg"
            exit 1
        fi
        
        if [ -n "$app_url" ]; then
            success "$apk_type APK uploaded successfully"
            echo "$app_url"
            return 0
        fi
    fi
    
    # Fallback if jq is not available
    if echo "$response" | grep -q "app_url"; then
        success "$apk_type APK uploaded successfully"
        echo "$response" | grep -o '"app_url":"[^"]*"' | cut -d'"' -f4
    else
        error "Upload failed for $apk_type APK"
        log "Response: $response"
        exit 1
    fi
}

# Run tests on BrowserStack
run_tests() {
    local app_url=$1
    local test_suite_url=$2
    
    log "Starting test execution on BrowserStack..."
    
    local test_config='{
        "app": "'$app_url'",
        "testSuite": "'$test_suite_url'",
        "devices": [
            "Google Pixel 8-14.0",
            "Samsung Galaxy S24-14.0",
            "OnePlus 12-14.0"
        ],
        "instrumentationOptions": {
            "github_test_doc_id": "'$GITHUB_TEST_DOC_ID'"
        },
        "deviceLogs": true,
        "video": true,
        "project": "KMP Integration Tests",
        "build": "Android-'$(date +%Y%m%d_%H%M%S)'",
        "name": "GitHub Test Document Sync"
    }'
    
    log "Test configuration:"
    echo "$test_config" | jq '.' 2>/dev/null || echo "$test_config"
    
    local response=$(curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
        -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/build" \
        -H "Content-Type: application/json" \
        -d "$test_config")
    
    if command -v jq &> /dev/null; then
        local build_id=$(echo "$response" | jq -r '.build_id // empty')
        local error_msg=$(echo "$response" | jq -r '.error // empty')
        
        if [ -n "$error_msg" ]; then
            error "Test execution failed: $error_msg"
            exit 1
        fi
        
        if [ -n "$build_id" ]; then
            success "Test execution started successfully"
            log "Build ID: $build_id"
            log "Dashboard: https://app-automate.browserstack.com/dashboard/v2/builds/$build_id"
            
            # Monitor test progress
            monitor_test_progress "$build_id"
        else
            error "Failed to start test execution"
            log "Response: $response"
            exit 1
        fi
    else
        success "Test execution started (jq not available for detailed parsing)"
        log "Response: $response"
    fi
}

# Monitor test progress
monitor_test_progress() {
    local build_id=$1
    
    log "Monitoring test progress for build: $build_id"
    
    local max_attempts=60  # 10 minutes with 10s intervals
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        sleep 10
        attempt=$((attempt + 1))
        
        local status_response=$(curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
            "https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/$build_id")
        
        if command -v jq &> /dev/null; then
            local status=$(echo "$status_response" | jq -r '.status // "unknown"')
            local duration=$(echo "$status_response" | jq -r '.duration // 0')
            
            log "Status: $status (Duration: ${duration}s, Attempt: $attempt/$max_attempts)"
            
            case "$status" in
                "passed")
                    success "All tests passed! ✅"
                    log "Dashboard: https://app-automate.browserstack.com/dashboard/v2/builds/$build_id"
                    return 0
                    ;;
                "failed")
                    error "Tests failed ❌"
                    log "Dashboard: https://app-automate.browserstack.com/dashboard/v2/builds/$build_id"
                    return 1
                    ;;
                "timeout")
                    error "Tests timed out ⏰"
                    log "Dashboard: https://app-automate.browserstack.com/dashboard/v2/builds/$build_id"
                    return 1
                    ;;
                "running")
                    log "Tests still running... 🏃"
                    ;;
                *)
                    log "Status: $status"
                    ;;
            esac
        else
            log "Checking progress... (attempt $attempt/$max_attempts)"
        fi
    done
    
    warning "Test monitoring timed out after $max_attempts attempts"
    log "Check manually: https://app-automate.browserstack.com/dashboard/v2/builds/$build_id"
    return 1
}

# Main execution
main() {
    log "Starting BrowserStack KMP Integration Testing"
    log "Test Document ID: '$GITHUB_TEST_DOC_ID'"
    
    check_prerequisites
    build_apks
    
    log "Uploading APKs to BrowserStack..."
    APP_URL=$(upload_apk "$MAIN_APK" "main")
    TEST_SUITE_URL=$(upload_apk "$TEST_APK" "test")
    
    run_tests "$APP_URL" "$TEST_SUITE_URL"
    
    success "BrowserStack integration test completed!"
}

# Run main function
main "$@"