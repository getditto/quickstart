#!/bin/bash

# BrowserStack Test Script for Android CPP App
# This script helps with manual testing and debugging of BrowserStack integration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check for required environment variables
check_env() {
    if [ -z "$BROWSERSTACK_USERNAME" ] || [ -z "$BROWSERSTACK_ACCESS_KEY" ]; then
        echo -e "${RED}Error: BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY must be set${NC}"
        echo "Export them as environment variables:"
        echo "  export BROWSERSTACK_USERNAME=your_username"
        echo "  export BROWSERSTACK_ACCESS_KEY=your_access_key"
        exit 1
    fi
}

# Build the APKs
build_apks() {
    echo -e "${YELLOW}Building APKs...${NC}"
    cd QuickStartTasksCPP
    ./gradlew assembleDebug assembleDebugAndroidTest
    cd ..
    echo -e "${GREEN}APKs built successfully${NC}"
}

# Upload APK to BrowserStack
upload_app() {
    echo -e "${YELLOW}Uploading app APK to BrowserStack...${NC}"
    
    RESPONSE=$(curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
        -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
        -F "file=@QuickStartTasksCPP/app/build/outputs/apk/debug/app-debug.apk" \
        -F "custom_id=ditto-android-cpp-app-manual")
    
    APP_URL=$(echo $RESPONSE | jq -r .app_url)
    
    if [ "$APP_URL" == "null" ]; then
        echo -e "${RED}Failed to upload app APK${NC}"
        echo "Response: $RESPONSE"
        exit 1
    fi
    
    echo -e "${GREEN}App uploaded successfully${NC}"
    echo "App URL: $APP_URL"
    echo $APP_URL > .app_url
}

# Upload test APK to BrowserStack
upload_test() {
    echo -e "${YELLOW}Uploading test APK to BrowserStack...${NC}"
    
    RESPONSE=$(curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
        -X POST "https://api-cloud.browserstack.com/app-automate/espresso/test-suite" \
        -F "file=@QuickStartTasksCPP/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk" \
        -F "custom_id=ditto-android-cpp-test-manual")
    
    TEST_URL=$(echo $RESPONSE | jq -r .test_suite_url)
    
    if [ "$TEST_URL" == "null" ]; then
        echo -e "${RED}Failed to upload test APK${NC}"
        echo "Response: $RESPONSE"
        exit 1
    fi
    
    echo -e "${GREEN}Test APK uploaded successfully${NC}"
    echo "Test URL: $TEST_URL"
    echo $TEST_URL > .test_url
}

# Run tests on BrowserStack
run_tests() {
    echo -e "${YELLOW}Starting tests on BrowserStack...${NC}"
    
    APP_URL=$(cat .app_url)
    TEST_URL=$(cat .test_url)
    
    # Select devices based on parameter or use default set
    if [ "$1" == "quick" ]; then
        DEVICES='["Google Pixel 8-14.0"]'
    elif [ "$1" == "full" ]; then
        DEVICES='["Google Pixel 8-14.0","Samsung Galaxy S23-13.0","Google Pixel 6-12.0","OnePlus 9-11.0","Xiaomi Redmi Note 11-11.0"]'
    else
        DEVICES='["Google Pixel 8-14.0","Samsung Galaxy S23-13.0","Google Pixel 6-12.0","OnePlus 9-11.0"]'
    fi
    
    RESPONSE=$(curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
        -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/build" \
        -H "Content-Type: application/json" \
        -d "{
            \"app\": \"$APP_URL\",
            \"testSuite\": \"$TEST_URL\",
            \"devices\": $DEVICES,
            \"projectName\": \"Ditto Android CPP Manual Test\",
            \"buildName\": \"Manual Test - $(date +%Y%m%d-%H%M%S)\",
            \"deviceLogs\": true,
            \"video\": true,
            \"networkLogs\": true
        }")
    
    BUILD_ID=$(echo $RESPONSE | jq -r .build_id)
    
    if [ "$BUILD_ID" == "null" ]; then
        echo -e "${RED}Failed to start tests${NC}"
        echo "Response: $RESPONSE"
        exit 1
    fi
    
    echo -e "${GREEN}Tests started successfully${NC}"
    echo "Build ID: $BUILD_ID"
    echo "Dashboard URL: https://app-automate.browserstack.com/dashboard/v2/builds/$BUILD_ID"
    echo $BUILD_ID > .build_id
}

# Check test status
check_status() {
    if [ ! -f .build_id ]; then
        echo -e "${RED}No build ID found. Run tests first.${NC}"
        exit 1
    fi
    
    BUILD_ID=$(cat .build_id)
    
    echo -e "${YELLOW}Checking test status...${NC}"
    
    RESPONSE=$(curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
        "https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/$BUILD_ID")
    
    STATUS=$(echo $RESPONSE | jq -r .status)
    
    echo -e "${GREEN}Build Status: $STATUS${NC}"
    echo "Dashboard URL: https://app-automate.browserstack.com/dashboard/v2/builds/$BUILD_ID"
    
    # Show device results
    echo -e "\n${YELLOW}Device Results:${NC}"
    echo $RESPONSE | jq -r '.devices[] | "\(.device): \(.status)"'
}

# Wait for tests to complete
wait_for_tests() {
    if [ ! -f .build_id ]; then
        echo -e "${RED}No build ID found. Run tests first.${NC}"
        exit 1
    fi
    
    BUILD_ID=$(cat .build_id)
    
    echo -e "${YELLOW}Waiting for tests to complete...${NC}"
    
    while true; do
        RESPONSE=$(curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
            "https://api-cloud.browserstack.com/app-automate/espresso/v2/builds/$BUILD_ID")
        
        STATUS=$(echo $RESPONSE | jq -r .status)
        
        echo -ne "\rStatus: $STATUS "
        
        if [ "$STATUS" == "done" ] || [ "$STATUS" == "failed" ] || [ "$STATUS" == "error" ]; then
            echo ""
            break
        fi
        
        sleep 10
    done
    
    echo -e "\n${GREEN}Tests completed with status: $STATUS${NC}"
    
    # Show final results
    echo -e "\n${YELLOW}Final Results:${NC}"
    echo $RESPONSE | jq -r '.devices[] | "\(.device): \(.status)"'
    
    # Check if all passed
    FAILED=$(echo $RESPONSE | jq -r '.devices[] | select(.status != "passed") | .device')
    
    if [ -z "$FAILED" ]; then
        echo -e "\n${GREEN}All tests passed!${NC}"
    else
        echo -e "\n${RED}Tests failed on: $FAILED${NC}"
        exit 1
    fi
}

# Clean up temporary files
cleanup() {
    rm -f .app_url .test_url .build_id
}

# Main script
case "$1" in
    "build")
        check_env
        build_apks
        ;;
    "upload")
        check_env
        upload_app
        upload_test
        ;;
    "test")
        check_env
        run_tests $2
        ;;
    "status")
        check_env
        check_status
        ;;
    "wait")
        check_env
        wait_for_tests
        ;;
    "all")
        check_env
        build_apks
        upload_app
        upload_test
        run_tests $2
        wait_for_tests
        ;;
    "clean")
        cleanup
        ;;
    *)
        echo "Usage: $0 {build|upload|test [quick|full]|status|wait|all [quick|full]|clean}"
        echo ""
        echo "Commands:"
        echo "  build   - Build the APKs"
        echo "  upload  - Upload APKs to BrowserStack"
        echo "  test    - Start tests (optional: quick for 1 device, full for 5 devices)"
        echo "  status  - Check test status"
        echo "  wait    - Wait for tests to complete"
        echo "  all     - Build, upload, test, and wait"
        echo "  clean   - Remove temporary files"
        echo ""
        echo "Environment variables required:"
        echo "  BROWSERSTACK_USERNAME"
        echo "  BROWSERSTACK_ACCESS_KEY"
        exit 1
        ;;
esac