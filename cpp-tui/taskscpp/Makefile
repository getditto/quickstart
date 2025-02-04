# CMake build type: Debug, Release, RelWithDebInfo
BUILD_TYPE ?= Debug

# Utility paths
CMAKE ?= cmake
CTEST ?= ctest
CLANG_TIDY ?= clang-tidy
CLANG_FORMAT ?= clang-format

BUILD_DIR = build
XCODE_BUILD_DIR = build-xcode

CPP_SRC_FILES = $(shell find src -type f -name '*.cpp' -o -name '*.h')


# The "help" target will display all targets marked with a "##" comment.
.PHONY: help
help: MAKEFILE_FMT = "  \033[36m%-34s\033[0m%s\n"
help: ## (default) Displays this message
	@echo "template-app-cpp-todo-crossplatform Makefile"
	@echo ""
	@echo "Targets:"
	@grep -E '^[a-zA-Z0-9_-]*:.*?##' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?##"}; {printf $(MAKEFILE_FMT), $$1, $$2}'
	@echo ""
	@echo "Parameters:"
	@grep -E '^[A-Z0-9_-]* ?\?=.*?##' $(MAKEFILE_LIST) | awk 'BEGIN {FS = " ?\\?=.*?##"}; {printf $(MAKEFILE_FMT), $$1, $$2}'

.PHONY: build
build: ## Generates all targets
	$(CMAKE) -B $(BUILD_DIR) . -DCMAKE_BUILD_TYPE=$(BUILD_TYPE) -DCMAKE_EXPORT_COMPILE_COMMANDS=ON -Wno-dev
	$(CMAKE) --build $(BUILD_DIR) --parallel

.PHONY: build-no-tui
build-no-tui: ## Generates all targets, without TUI support
	$(CMAKE) -B $(BUILD_DIR) . -DCMAKE_BUILD_TYPE=$(BUILD_TYPE) -DCMAKE_EXPORT_COMPILE_COMMANDS=ON -Wno-dev -DDITTO_QUICKSTART_TUI=OFF
	$(CMAKE) --build $(BUILD_DIR) --parallel

.PHONY: run-help
run-help: build ## Builds taskscpp and runs the --help command
	cd $(BUILD_DIR) && ./taskscpp --help

.PHONY: run-tui
run-tui: build ## Builds taskscpp and runs it in TUI mode
	cd $(BUILD_DIR) && ./taskscpp --tui

.PHONY: run-list
run-list: build ## Builds taskscpp and runs the --list command
	cd $(BUILD_DIR) && ./taskscpp --list

.PHONY: run-list-all
run-list-all: build ## Builds taskscpp and runs the --list-all command
	cd $(BUILD_DIR) && ./taskscpp --list-all

.PHONY: run-monitor
run-monitor: build ## Builds taskscpp and runs the --monitor command
	cd $(BUILD_DIR) && ./taskscpp --monitor

.PHONY: xcode
xcode: ## Generates an Xcode project in the build-xcode directory
	mkdir -p $(XCODE_BUILD_DIR)
	cd $(XCODE_BUILD_DIR) && $(CMAKE) .. -G Xcode

.PHONY: format
format: ## Apply clang-format to all .cpp and .h files
	$(CLANG_FORMAT) -i $(CPP_SRC_FILES)

.PHONY: lint
lint: build ## Runs clang-format -n and clang-tidy on all .cpp and .h files
	$(CLANG_FORMAT) --dry-run --Werror $(CPP_SRC_FILES)
	$(CLANG_TIDY) -p $(BUILD_DIR) $(CPP_SRC_FILES)

.PHONY: clean
clean: ## Removes all generated files and directories
	- rm -r $(BUILD_DIR)
	- rm -r $(XCODE_BUILD_DIR)
	- rm src/env.h
