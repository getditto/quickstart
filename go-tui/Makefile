# Makefile for Ditto Go TUI Tasks Application
# Display help information
help:
	@echo "Ditto Go TUI Tasks Application"
	@echo ""
	@echo "Available targets:"
	@echo "  build    - Build the FFI library and application"
	@echo "  run      - Build and run the application"
	@echo "  run-go   - Run directly with 'go run'"
	@echo "  ffi      - Build only the FFI library"
	@echo "  clean    - Remove build artifacts"
	@echo "  help     - Show this help message"
	@echo ""
	@echo "Note: Ensure .env file exists with proper Ditto configuration"

.PHONY: build run clean help ffi

# Build the FFI library (required for Ditto SDK)
ffi:
	@echo "Building FFI library..."
	(cd ../../ditto/sdks/go && make build)

# Build the application
build: ffi
	@echo "Building ditto-tasks-termui..."
	go build -o ditto-tasks-termui

# Run the application (built binary)
run: build
	@echo "Running ditto-tasks-termui..."
	./ditto-tasks-termui 2>/dev/null

# Run directly with Go
run-go:
	@echo "Running with go run..."
	go run main.go 2>/dev/null

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	rm -f ditto-tasks-termui
