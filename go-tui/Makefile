# Makefile for Ditto QuickStart Go TUI Tasks application

# Display help information
.PHONY: help
help:
	@echo "Ditto Go TUI Tasks Application"
	@echo ""
	@echo "Available targets:"
	@echo "  build    - Build the FFI library and application"
	@echo "  run      - Build and run the application"
	@echo "  run-go   - Run directly with 'go run'"
	@echo "  clean    - Remove build artifacts"
	@echo "  help     - Show this help message"
	@echo ""
	@echo "Note: Ensure .env file exists with proper Ditto configuration"

GO=go

# Ditto SDK version and platform detection
DITTO_SDK_VERSION ?= 5.0.0-go-preview.3
PLATFORM := $(shell uname -s | tr '[:upper:]' '[:lower:]')
ARCH := $(shell uname -m)

# Determine Ditto SDK platform string
ifeq ($(PLATFORM),linux)
    DITTO_PLATFORM = linux-$(ARCH)
else ifeq ($(PLATFORM),darwin)
    DITTO_PLATFORM = macos-aarch64

    # avoid version mismatch warnings when linking
    export MACOSX_DEPLOYMENT_TARGET := 11.0
    export CGO_CFLAGS := -mmacosx-version-min=11.0
    export CGO_LDFLAGS := -mmacosx-version-min=11.0
else
    $(error Unsupported platform: $(PLATFORM))
endif

DITTO_SDK_URL = "https://software.ditto.live/go/Ditto/$(DITTO_SDK_VERSION)/libs/libdittoffi-$(DITTO_PLATFORM).tar.gz"

# Build the application
.PHONY: build
build: go-sdk ditto-tasks-termui

ditto-tasks-termui: main.go redirect_unix.go redirect_windows.go widgets.go Makefile go.mod go.sum
	@echo "Building ditto-tasks-termui..."
	$(GO) mod tidy
	$(GO) build -o ditto-tasks-termui -ldflags='-extldflags "-L./go-sdk"'

.PHONY: go-sdk
go-sdk: ## Downloads and installs the Ditto Go SDK library to go-sdk directory
	@ if [ ! -f go-sdk/libdittoffi.so ] && [ ! -f go-sdk/libdittoffi.dylib ] ; then \
		echo "📥 Downloading Ditto Go SDK v$(DITTO_SDK_VERSION) for $(DITTO_PLATFORM) $(DITTO_SDK_URL)..."; \
		mkdir -p go-sdk; \
		if curl -L -f $(DITTO_SDK_URL) | tar xz --strip-components=0 -C go-sdk/; then \
			echo "✅ Ditto Go SDK v$(DITTO_SDK_VERSION) installed successfully"; \
		else \
			echo "❌ Failed to download SDK for $(DITTO_PLATFORM)"; \
		fi; \
	else \
		echo "✅ Ditto Go SDK already installed"; \
	fi

# Run the application (built binary)
.PHONY: build
run: build
	@echo "Running ditto-tasks-termui..."
	LD_LIBRARY_PATH="$$(pwd)/go-sdk" DYLD_LIBRARY_PATH="$$(pwd)/go-sdk" ./ditto-tasks-termui 2>/dev/null

# Run directly with Go
.PHONY: run-go
run-go:
	@echo "Running ditto tasks-termui with go run..."
	LD_LIBRARY_PATH="$$(pwd)/go-sdk" DYLD_LIBRARY_PATH="$$(pwd)/go-sdk" $(GO) run main.go 2>/dev/null

# Clean build artifacts
.PHONY: clean
clean:
	@echo "Cleaning ditto-tasks-termui and build artifacts..."
	rm -f ditto-tasks-termui
	rm -rf go-sdk
