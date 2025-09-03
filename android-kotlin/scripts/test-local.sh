#!/bin/bash
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo -e "${BLUE}🧪 Android Kotlin Local Integration Test Runner${NC}"
echo "=================================================="

# Function to print usage
usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  -h, --help           Show this help message"
    echo "  -s, --seed-only      Only seed the document, don't run tests"
    echo "  -t, --test-only      Only run tests (assumes document already seeded)"
    echo "  -d, --doc-id ID      Use custom document ID"
    echo "  -T, --title TITLE    Use custom document title" 
    echo "  -c, --clean          Clean build before running tests"
    echo "  -v, --verify         Verify document creation by querying it back"
    echo ""
    echo "Examples:"
    echo "  $0                           # Seed document and run integration tests"
    echo "  $0 -s                        # Only seed a document"
    echo "  $0 -t                        # Only run tests"
    echo "  $0 -d my_test_123            # Use custom document ID"
    echo "  $0 -c                        # Clean build and run full test"
    echo ""
}

# Default values
SEED_ONLY=false
TEST_ONLY=false
CLEAN_BUILD=false
VERIFY_DOC=false
CUSTOM_DOC_ID=""
CUSTOM_TITLE=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            exit 0
            ;;
        -s|--seed-only)
            SEED_ONLY=true
            shift
            ;;
        -t|--test-only)
            TEST_ONLY=true
            shift
            ;;
        -d|--doc-id)
            CUSTOM_DOC_ID="$2"
            shift 2
            ;;
        -T|--title)
            CUSTOM_TITLE="$2"
            shift 2
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -v|--verify)
            VERIFY_DOC=true
            shift
            ;;
        *)
            echo -e "${RED}❌ Unknown option: $1${NC}"
            usage
            exit 1
            ;;
    esac
done

# Check if we're in the right directory
if [[ ! -f "$PROJECT_ROOT/QuickStartTasks/gradlew" ]]; then
    echo -e "${RED}❌ Error: Please run this script from the android-kotlin directory${NC}"
    echo "   Current location: $(pwd)"
    echo "   Expected: .../android-kotlin/"
    exit 1
fi

# Function to check prerequisites
check_prerequisites() {
    echo -e "${BLUE}🔍 Checking prerequisites...${NC}"
    
    # Check if .env file exists
    if [[ ! -f "$PROJECT_ROOT/../.env" ]]; then
        echo -e "${RED}❌ .env file not found at $PROJECT_ROOT/../.env${NC}"
        echo "   Please create .env file with required Ditto credentials"
        exit 1
    fi
    
    # Check Python3
    if ! command -v python3 &> /dev/null; then
        echo -e "${RED}❌ Python3 is required but not installed${NC}"
        exit 1
    fi
    
    # Check if requests library is available
    if ! python3 -c "import requests" &> /dev/null; then
        echo -e "${YELLOW}⚠️  Python requests library not found${NC}"
        echo "   Installing requests..."
        pip3 install requests || {
            echo -e "${RED}❌ Failed to install requests. Please install it manually:${NC}"
            echo "   pip3 install requests"
            exit 1
        }
    fi
    
    echo -e "${GREEN}✅ Prerequisites checked${NC}"
}

# Function to seed test document
seed_document() {
    echo -e "${BLUE}🌱 Seeding test document...${NC}"
    
    cd "$PROJECT_ROOT"
    
    # Build the seed command
    SEED_CMD="python3 scripts/seed-test-document.py"
    
    if [[ -n "$CUSTOM_DOC_ID" ]]; then
        SEED_CMD="$SEED_CMD --doc-id $CUSTOM_DOC_ID"
    fi
    
    if [[ -n "$CUSTOM_TITLE" ]]; then
        SEED_CMD="$SEED_CMD --title \"$CUSTOM_TITLE\""
    fi
    
    if [[ "$VERIFY_DOC" == true ]]; then
        SEED_CMD="$SEED_CMD --verify"
    fi
    
    echo -e "${BLUE}📡 Running: $SEED_CMD${NC}"
    
    # Execute the seed command and capture the document ID
    SEED_OUTPUT=$(eval $SEED_CMD 2>&1)
    SEED_EXIT_CODE=$?
    
    echo "$SEED_OUTPUT"
    
    if [[ $SEED_EXIT_CODE -ne 0 ]]; then
        echo -e "${RED}❌ Failed to seed document${NC}"
        exit 1
    fi
    
    # Extract document ID from output (look for "Document ID: ..." line)
    DOC_ID=$(echo "$SEED_OUTPUT" | grep "Document ID:" | tail -1 | sed 's/Document ID: //')
    
    if [[ -z "$DOC_ID" ]]; then
        echo -e "${YELLOW}⚠️  Could not extract document ID from seed output${NC}"
        # Try to get it from the custom doc ID if provided
        if [[ -n "$CUSTOM_DOC_ID" ]]; then
            DOC_ID="$CUSTOM_DOC_ID"
        else
            echo -e "${RED}❌ Could not determine document ID for testing${NC}"
            exit 1
        fi
    fi
    
    echo -e "${GREEN}✅ Document seeded successfully: $DOC_ID${NC}"
    
    # Export for use in tests
    export GITHUB_TEST_DOC_ID="$DOC_ID"
    echo "GITHUB_TEST_DOC_ID=$DOC_ID" > /tmp/android_test_doc_id
    
    return 0
}

# Function to run integration tests
run_integration_tests() {
    echo -e "${BLUE}🧪 Running integration tests...${NC}"
    
    cd "$PROJECT_ROOT/QuickStartTasks"
    
    # Load document ID if running tests only
    if [[ "$TEST_ONLY" == true ]]; then
        if [[ -f "/tmp/android_test_doc_id" ]]; then
            source /tmp/android_test_doc_id
            echo -e "${BLUE}📋 Using document ID from previous run: $GITHUB_TEST_DOC_ID${NC}"
        else
            echo -e "${YELLOW}⚠️  No document ID found. Please provide one or run with --seed-only first${NC}"
            read -p "Enter document ID to test: " GITHUB_TEST_DOC_ID
            export GITHUB_TEST_DOC_ID
        fi
    fi
    
    if [[ -z "$GITHUB_TEST_DOC_ID" ]]; then
        echo -e "${RED}❌ No test document ID available${NC}"
        exit 1
    fi
    
    echo -e "${BLUE}🎯 Testing with document ID: $GITHUB_TEST_DOC_ID${NC}"
    
    # Clean build if requested
    if [[ "$CLEAN_BUILD" == true ]]; then
        echo -e "${BLUE}🧹 Cleaning build...${NC}"
        ./gradlew clean
    fi
    
    # Check if device is connected
    if ! adb devices | grep -q "device$"; then
        echo -e "${YELLOW}⚠️  No Android device detected${NC}"
        echo "   Please connect an Android device or start an emulator"
        echo "   Run 'adb devices' to check connected devices"
        read -p "Press Enter to continue anyway, or Ctrl+C to abort..."
    fi
    
    # Build the test APKs
    echo -e "${BLUE}🔨 Building test APKs...${NC}"
    ./gradlew assembleDebugAndroidTest || {
        echo -e "${RED}❌ Failed to build test APKs${NC}"
        exit 1
    }
    
    # Run the integration test
    echo -e "${BLUE}🚀 Running integration test...${NC}"
    echo -e "${BLUE}   Test document ID: $GITHUB_TEST_DOC_ID${NC}"
    
    # Run the specific sync test
    ./gradlew connectedDebugAndroidTest \
        -Pandroid.testInstrumentationRunnerArguments.class=live.ditto.quickstart.tasks.TasksSyncIntegrationTest \
        -Pandroid.testInstrumentationRunnerArguments.GITHUB_TEST_DOC_ID="$GITHUB_TEST_DOC_ID" \
        --info || {
        
        echo -e "${RED}❌ Integration test failed${NC}"
        echo -e "${BLUE}📋 Test reports available in:${NC}"
        echo "   - app/build/reports/androidTests/connected/"
        echo "   - app/build/outputs/androidTest-results/"
        
        # Try to show recent logcat entries
        echo -e "${BLUE}📱 Recent logcat entries (last 50 lines):${NC}"
        adb logcat -t 50 | grep -i "TasksSyncIntegrationTest\|Ditto\|Test" || true
        
        exit 1
    }
    
    echo -e "${GREEN}✅ Integration tests completed successfully!${NC}"
    
    # Show test results location
    echo -e "${BLUE}📋 Test reports available in:${NC}"
    echo "   - app/build/reports/androidTests/connected/"
    echo "   - app/build/outputs/androidTest-results/"
}

# Main execution
main() {
    check_prerequisites
    
    if [[ "$TEST_ONLY" == true ]]; then
        run_integration_tests
    elif [[ "$SEED_ONLY" == true ]]; then
        seed_document
    else
        # Full flow: seed then test
        seed_document
        echo ""
        run_integration_tests
    fi
    
    echo ""
    echo -e "${GREEN}🎉 Local integration test completed successfully!${NC}"
}

# Run main function
main "$@"