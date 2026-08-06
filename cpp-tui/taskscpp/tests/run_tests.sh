#!/bin/bash

# Test runner for C++ Ditto TUI application
# Compiles and runs both unit tests and integration tests

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🧪 C++ Ditto TUI Test Runner"
echo "============================"
echo "Project directory: $PROJECT_DIR"

# Ensure we're in the right directory
cd "$PROJECT_DIR"

# Check if env.h exists
if [ ! -f "src/env.h" ]; then
    echo "❌ env.h not found. Run 'awk -f scripts/generate_env.awk ../../.env > src/env.h' first"
    exit 1
fi

# Check if the main project is built
if [ ! -f "build/taskscpp" ]; then
    echo "🔨 Main project not built, building first..."
    make build
fi

# Create test build directory
mkdir -p build/tests

echo ""
echo "🏗️  Compiling Integration Tests..."
echo "================================="
g++ -std=c++17 \
    -I./src -I./sdk -I./third_party/cxxopts/include \
    tests/integration_test.cpp \
    src/task.cpp src/ditto_manager.cpp src/tasks_repository.cpp src/tasks_log.cpp \
    -L./sdk -lditto \
    -o build/tests/integration_test \
    -pthread

if [ $? -eq 0 ]; then
    echo "✅ Integration tests compiled successfully"
else
    echo "❌ Failed to compile integration tests"
    echo "⚠️  Note: Integration tests require Ditto SDK (libditto.a) in sdk/ directory"
    echo "📚 See README.md for SDK installation instructions"
    exit 1
fi

echo ""
echo "🚀 Running Integration Tests..."
echo "==============================="
./build/tests/integration_test

echo ""
echo "🎉 Integration Tests Completed Successfully!"
echo "=========================================="
echo "✅ Integration tests: PASSED"
echo "🎯 C++ Ditto TUI application validated!"
