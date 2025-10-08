#!/bin/bash
#
# Generic retry wrapper for BrowserStack API calls
# Retries on BROWSERSTACK_ALL_PARALLELS_IN_USE errors with exponential backoff
#
# Usage: .github/scripts/retry-browserstack.sh '<your curl command>'
#

set -e

# Configuration
MAX_ATTEMPTS=${MAX_ATTEMPTS:-5}
INITIAL_WAIT=${INITIAL_WAIT:-60}  # Start with 1 minute
MAX_WAIT=${MAX_WAIT:-300}         # Max 5 minutes between retries

# Function to run command with retry logic
retry_on_queue_full() {
    local attempt=1
    local wait_time=$INITIAL_WAIT

    while [ $attempt -le $MAX_ATTEMPTS ]; do
        echo "🔄 Attempt $attempt/$MAX_ATTEMPTS..."

        # Run the command and capture output
        set +e
        OUTPUT=$(eval "$@" 2>&1)
        EXIT_CODE=$?
        set -e

        echo "$OUTPUT"

        # Check if it's a BrowserStack queue error
        if echo "$OUTPUT" | grep -q "BROWSERSTACK_ALL_PARALLELS_IN_USE"; then
            if [ $attempt -lt $MAX_ATTEMPTS ]; then
                echo "⏳ BrowserStack queue is full. Waiting ${wait_time}s before retry (attempt $attempt/$MAX_ATTEMPTS)..."
                sleep $wait_time

                # Exponential backoff with max cap
                wait_time=$((wait_time * 2))
                if [ $wait_time -gt $MAX_WAIT ]; then
                    wait_time=$MAX_WAIT
                fi

                attempt=$((attempt + 1))
            else
                echo "❌ Max attempts ($MAX_ATTEMPTS) reached. BrowserStack queue still full."
                return 1
            fi
        else
            # Not a queue error - either success or fail immediately
            if [ $EXIT_CODE -eq 0 ]; then
                echo "✅ Command succeeded!"
            else
                echo "❌ Command failed with non-queue error"
            fi
            return $EXIT_CODE
        fi
    done

    return 1
}

# Check if command was provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 '<command>'"
    echo ""
    echo "Examples:"
    echo "  $0 'curl -u \$USER:\$KEY -X POST https://api.browserstack.com/...'"
    echo "  $0 './gradlew test'"
    echo ""
    echo "Environment variables:"
    echo "  MAX_ATTEMPTS=$MAX_ATTEMPTS (default: 5)"
    echo "  INITIAL_WAIT=$INITIAL_WAIT (default: 60 seconds)"
    echo "  MAX_WAIT=$MAX_WAIT (default: 300 seconds)"
    exit 1
fi

# Run with retry logic
retry_on_queue_full "$@"
