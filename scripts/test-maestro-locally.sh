#!/bin/bash

# Script to test Maestro flows locally
# Usage: ./scripts/test-maestro-locally.sh [expo|bare] [device_type]

set -e

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_TYPE=${1:-expo}
DEVICE_TYPE=${2:-android}  # android or ios

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Maestro is installed
check_maestro_installation() {
    log_info "Checking Maestro installation..."
    
    if ! command -v maestro &> /dev/null; then
        log_error "Maestro is not installed. Please install it first:"
        echo "  curl -Ls \"https://get.maestro.mobile.dev\" | bash"
        echo "  export PATH=\"\$PATH\":\"\$HOME/.maestro/bin\""
        exit 1
    else
        MAESTRO_VERSION=$(maestro --version | head -n1)
        log_success "Maestro is installed: $MAESTRO_VERSION"
    fi
}

# Setup environment
setup_environment() {
    log_info "Setting up environment for $APP_TYPE app..."
    
    # Set app directory
    if [ "$APP_TYPE" = "expo" ]; then
        APP_DIR="$PROJECT_ROOT/react-native-expo"
        MAESTRO_DIR="$APP_DIR/.maestro"
        PACKAGE_NAME="com.anonymous.reactnativeexpo"
    else
        APP_DIR="$PROJECT_ROOT/react-native"
        MAESTRO_DIR="$APP_DIR/.maestro"
        PACKAGE_NAME="com.dittoreactnativesampleapp"
    fi
    
    log_info "App directory: $APP_DIR"
    log_info "Maestro directory: $MAESTRO_DIR"
    log_info "Package name: $PACKAGE_NAME"
    
    # Check if .env file exists
    ENV_FILE="$PROJECT_ROOT/.env"
    if [ ! -f "$ENV_FILE" ]; then
        log_warning ".env file not found. Creating from .env.sample..."
        if [ -f "$PROJECT_ROOT/.env.sample" ]; then
            cp "$PROJECT_ROOT/.env.sample" "$ENV_FILE"
            log_warning "Please edit .env file with your Ditto credentials before running tests"
        else
            log_error ".env.sample file not found. Please create .env file manually"
            exit 1
        fi
    fi
    
    # Export environment variables
    export DITTO_APP_ID=$(grep DITTO_APP_ID "$ENV_FILE" | cut -d '=' -f2)
    export DITTO_PLAYGROUND_TOKEN=$(grep DITTO_PLAYGROUND_TOKEN "$ENV_FILE" | cut -d '=' -f2)
    
    log_success "Environment setup complete"
}

# Check if app is installed
check_app_installation() {
    log_info "Checking if app is installed..."
    
    if [ "$DEVICE_TYPE" = "android" ]; then
        # Check if Android device/emulator is connected
        if ! adb devices | grep -q "device$"; then
            log_error "No Android device or emulator connected"
            log_info "Please start an Android emulator or connect a device"
            exit 1
        fi
        
        # Check if app is installed
        if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
            log_success "App is installed: $PACKAGE_NAME"
        else
            log_warning "App is not installed: $PACKAGE_NAME"
            log_info "Please build and install the app first:"
            echo "  cd $APP_DIR"
            if [ "$APP_TYPE" = "expo" ]; then
                echo "  yarn android"
            else
                echo "  yarn android"
            fi
            exit 1
        fi
    elif [ "$DEVICE_TYPE" = "ios" ]; then
        # iOS simulator check
        if ! xcrun simctl list devices | grep -q "Booted"; then
            log_error "No iOS simulator is running"
            log_info "Please start an iOS simulator"
            log_info "You can start one with: xcrun simctl boot <device_id>"
            exit 1
        fi
        
        log_info "iOS simulator is running"
        
        # Check if iOS app is installed (this is more complex for iOS simulators)
        log_info "Note: iOS app installation check not implemented - assuming app is built"
    else
        # macOS app check
        log_info "macOS testing mode - looking for .app bundle"
        
        # For macOS, we need to check if the app is built
        if [ "$APP_TYPE" = "expo" ]; then
            log_warning "Expo doesn't support macOS - use bare React Native instead"
            exit 1
        fi
        
        MACOS_APP_PATH=$(find "$APP_DIR/macos/build" -name "*.app" -type d 2>/dev/null | head -1)
        if [ -n "$MACOS_APP_PATH" ]; then
            log_success "macOS app found: $MACOS_APP_PATH"
        else
            log_warning "macOS app not found. Please build it first:"
            echo "  cd $APP_DIR"
            echo "  yarn run build:macos"
            exit 1
        fi
    fi
}

# List available test flows
list_test_flows() {
    log_info "Available test flows:"
    
    if [ -d "$MAESTRO_DIR/flows" ]; then
        find "$MAESTRO_DIR/flows" -name "*.yaml" -o -name "*.yml" | sort | while read -r flow; do
            flow_name=$(basename "$flow" .yaml)
            echo "  - $flow_name"
        done
    else
        log_error "Maestro flows directory not found: $MAESTRO_DIR/flows"
        exit 1
    fi
}

# Run specific test flow
run_test_flow() {
    local flow_file="$1"
    local flow_name=$(basename "$flow_file" .yaml)
    
    log_info "Running test flow: $flow_name"
    
    cd "$MAESTRO_DIR"
    
    if maestro test "$flow_file"; then
        log_success "✅ Test flow passed: $flow_name"
        return 0
    else
        log_error "❌ Test flow failed: $flow_name"
        return 1
    fi
}

# Run all test flows
run_all_tests() {
    log_info "Running all Maestro test flows..."
    
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    
    if [ -d "$MAESTRO_DIR/flows" ]; then
        find "$MAESTRO_DIR/flows" -name "*.yaml" -o -name "*.yml" | sort | while read -r flow; do
            total_tests=$((total_tests + 1))
            
            if run_test_flow "$flow"; then
                passed_tests=$((passed_tests + 1))
            else
                failed_tests=$((failed_tests + 1))
            fi
        done
        
        # Summary
        echo
        log_info "Test Summary:"
        echo "  Total tests: $total_tests"
        log_success "  Passed: $passed_tests"
        if [ $failed_tests -gt 0 ]; then
            log_error "  Failed: $failed_tests"
        else
            echo "  Failed: $failed_tests"
        fi
        
        if [ $failed_tests -eq 0 ]; then
            log_success "🎉 All tests passed!"
            return 0
        else
            log_error "💥 Some tests failed!"
            return 1
        fi
    else
        log_error "No test flows found in: $MAESTRO_DIR/flows"
        return 1
    fi
}

# Run specific test by tag
run_tests_by_tag() {
    local tag="$1"
    log_info "Running tests with tag: $tag"
    
    cd "$MAESTRO_DIR"
    
    if maestro test --tag="$tag" flows/; then
        log_success "✅ Tests with tag '$tag' passed"
        return 0
    else
        log_error "❌ Tests with tag '$tag' failed"
        return 1
    fi
}

# Interactive test runner
interactive_mode() {
    while true; do
        echo
        log_info "Maestro Local Test Runner - Interactive Mode"
        echo "1. List available test flows"
        echo "2. Run all tests"
        echo "3. Run tests by tag (smoke, core, sync, etc.)"
        echo "4. Run specific test flow"
        echo "5. Exit"
        echo
        read -p "Select an option (1-5): " choice
        
        case $choice in
            1)
                list_test_flows
                ;;
            2)
                run_all_tests
                ;;
            3)
                echo "Available tags: smoke, core, sync, integration, workflow"
                read -p "Enter tag name: " tag
                run_tests_by_tag "$tag"
                ;;
            4)
                list_test_flows
                echo
                read -p "Enter test flow name (without .yaml): " flow_name
                flow_file="$MAESTRO_DIR/flows/${flow_name}.yaml"
                if [ -f "$flow_file" ]; then
                    run_test_flow "$flow_file"
                else
                    log_error "Test flow not found: $flow_file"
                fi
                ;;
            5)
                log_info "Exiting..."
                break
                ;;
            *)
                log_error "Invalid option. Please select 1-5."
                ;;
        esac
    done
}

# Main function
main() {
    echo "🧪 Maestro Local Test Runner"
    echo "=========================="
    
    # Check prerequisites
    check_maestro_installation
    setup_environment
    check_app_installation
    
    # Parse command line arguments
    if [ $# -eq 0 ]; then
        interactive_mode
    else
        case "$3" in
            "list")
                list_test_flows
                ;;
            "all")
                run_all_tests
                ;;
            "tag")
                if [ -n "$4" ]; then
                    run_tests_by_tag "$4"
                else
                    log_error "Please specify a tag name"
                    exit 1
                fi
                ;;
            "flow")
                if [ -n "$4" ]; then
                    flow_file="$MAESTRO_DIR/flows/${4}.yaml"
                    if [ -f "$flow_file" ]; then
                        run_test_flow "$flow_file"
                    else
                        log_error "Test flow not found: $flow_file"
                        exit 1
                    fi
                else
                    log_error "Please specify a flow name"
                    exit 1
                fi
                ;;
            *)
                interactive_mode
                ;;
        esac
    fi
}

# Show usage if help is requested
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    echo "Usage: $0 [expo|bare] [android|ios|macos] [command] [args...]"
    echo
    echo "App Types:"
    echo "  expo              - React Native with Expo (Android/iOS only)"
    echo "  bare              - Bare React Native (Android/iOS/macOS)"
    echo
    echo "Device Types:"
    echo "  android           - Android emulator/device"
    echo "  ios               - iOS simulator (requires macOS)"
    echo "  macos             - macOS app (bare RN only, requires macOS)"
    echo
    echo "Commands:"
    echo "  list              - List available test flows"
    echo "  all               - Run all test flows"
    echo "  tag <tag_name>    - Run tests with specific tag"
    echo "  flow <flow_name>  - Run specific test flow"
    echo
    echo "Examples:"
    echo "  $0 expo android                    # Interactive mode for expo app on Android"
    echo "  $0 expo ios list                   # List flows for expo app on iOS"
    echo "  $0 bare macos all                  # Run all tests for bare RN app on macOS"
    echo "  $0 bare android tag smoke          # Run smoke tests for bare RN app on Android"
    echo "  $0 expo ios flow 01-app-launch     # Run specific test flow on iOS"
    exit 0
fi

# Run main function
main "$@"