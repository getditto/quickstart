#!/bin/bash
# Helper script to run Java Server in Docker container

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🐳 Starting Java Server container..."
echo ""
echo "Mounts:"
echo "  - ~/.m2 (read-only) -> /root/.m2"
echo "  - Current directory -> /workspace"
echo ""
echo "Port 8080 will be accessible at http://localhost:8080"
echo ""

cd "$SCRIPT_DIR"

# Use docker compose to run the container
docker compose run --rm --service-ports java-server

echo ""
echo "✅ Container session ended"
