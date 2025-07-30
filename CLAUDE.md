# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a multi-platform quickstart repository for the Ditto SDK, containing sample "Tasks" applications that demonstrate real-time synchronization across various programming languages and platforms. Each subdirectory contains a complete, self-contained application implementing a todo list manager with Ditto's peer-to-peer sync capabilities.

## Initial Setup

Before working with any app:
1. Ensure `.env` exists (copy from `.env.sample`)
2. Configure Ditto credentials:
   - `DITTO_APP_ID`
   - `DITTO_PLAYGROUND_TOKEN`
   - `DITTO_AUTH_URL`
   - `DITTO_WEBSOCKET_URL`

## Common Development Commands

### Android/Java/Kotlin Projects
```bash
# Build project
./gradlew build

# Run tests
./gradlew test

# Run specific test
./gradlew test --tests "*ClassName.testMethodName"

# Install on device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# List all tasks
./gradlew tasks
```

### JavaScript/TypeScript Projects
```bash
# Install dependencies
npm install

# Development server
npm run dev

# Production build
npm run build

# Run linter
npm run lint

# Format code
npm run format

# Run tests (where available)
npm test

# Run specific test
npm test -- --testNamePattern="test name"
```

### Rust Projects
```bash
# Build project
cargo build

# Run application
cargo run

# Run tests
cargo test

# Run specific test
cargo test test_name

# Format code
cargo fmt

# Check code
cargo clippy
```

### Flutter Projects
```bash
# Get dependencies
flutter pub get

# Run app
flutter run

# Run tests
flutter test

# Run specific test
flutter test test/widget_test.dart

# Build for specific platform
flutter build apk/ios/web
```

### .NET Projects
```bash
# Build project
dotnet build

# Run application
dotnet run

# Run tests
dotnet test

# Run specific test
dotnet test --filter "FullyQualifiedName~TestMethodName"
```

### C++ Projects
```bash
# Configure build
cmake .

# Build project
make

# Clean build
make clean
```

### Tools Setup
```bash
# Install Android command line tools (if ANDROID_HOME is set)
just cmdline_tools

# Install required Android SDK components
just tools
```

## Architecture Overview

### Common Structure
All applications implement the same "Tasks" functionality:
- **Task Model**: Simple todo items with ID, text/title, and completion status
- **Real-time Sync**: Automatic synchronization between devices using Ditto
- **CRUD Operations**: Create, read, update, and delete tasks
- **Platform UI**: Native UI patterns for each platform

### Ditto SDK Integration Pattern
1. **Initialization**: Apps read credentials from environment variables
2. **Identity**: Uses "Online Playground" identity (development only)
3. **Collection**: All apps use a "tasks" collection
4. **Document Structure**: Mostly consistent across platforms (slight variations in field names):
   ```json
   {
     "_id": "unique-id",
     "text": "Task description",    // or "title" in some implementations
     "isCompleted": false,          // or "done" in some implementations
     "deleted": false               // soft delete in some implementations
   }
   ```

### Platform-Specific Patterns

#### Android/Kotlin
- MVVM architecture with ViewModels
- Jetpack Compose for UI
- Gradle build with version catalogs (`gradle/libs.versions.toml`)
- Environment variables loaded via custom `loadEnvProperties()` function
- Task model uses `title`, `done`, and `deleted` fields

#### JavaScript Web
- React with TypeScript
- Vite build system
- Tailwind CSS for styling
- ESLint + Prettier for code quality
- Task model uses `text` and `isCompleted` fields

#### iOS/Swift
- SwiftUI for interface
- Xcode project structure
- Swift Package Manager for dependencies
- Environment variables loaded via `buildEnv.sh` script
- Task model uses `text` and `isCompleted` fields

#### Rust TUI
- Tokio async runtime
- Ratatui for terminal UI
- Event-driven architecture
- Task model uses `text` and `isCompleted` fields

#### React Native
- React Native with TypeScript
- Metro bundler
- Custom components for modals and UI elements
- Task model uses `text` and `isCompleted` fields

#### Kotlin Multiplatform
- Compose Multiplatform UI
- Targets iOS, Android, and Desktop
- Shared business logic across platforms
- Task model uses `text` and `isCompleted` fields

## Key Development Considerations

### Environment Variables
All apps load configuration from the `.env` file at the repository root. The loading mechanism varies by platform:
- **Android**: Custom Gradle function in `build.gradle.kts` that reads `.env` and sets build config fields
- **JavaScript**: Vite environment variables (auto-loaded from `.env`)
- **Rust**: `dotenvy` crate
- **Flutter**: `flutter_dotenv` package
- **Swift**: `buildEnv.sh` script generates `Env.swift` file
- **.NET**: Custom environment loading in code

### Testing
- Android: JUnit tests in `src/test` and instrumented tests in `src/androidTest`
- JavaScript: Jest for React Native, Vitest for web apps
- Rust: Built-in `cargo test`
- Flutter: `flutter test` with widget and unit tests
- Swift: XCTest framework

### Code Quality Tools
- **JavaScript/TypeScript**: ESLint + Prettier
- **Kotlin**: Built-in Kotlin linting
- **Rust**: `cargo fmt` and `cargo clippy`
- **Swift**: SwiftLint (where configured)

## Important Notes

1. **Production Warning**: These apps use "Online Playground" identity which is NOT suitable for production
   - For production environments, configure use the "Online with Authentication" identity to authenticate with
   your identity system and retrieve a secret token prior to syncing with Ditto. 
   - Refer to the [Authentication and Authorization](https://docs.ditto.live/key-concepts/authentication-and-authorization#online-with-authentication)
   for detailed instructions on configuring identity for production use.
2. **Cross-Platform Consistency**: All apps implement the same data model for interoperability (though field names may vary slightly)
3. **Platform Best Practices**: Each app follows its platform's conventions and patterns
4. **Real Device Testing**: For full P2P functionality, test on real devices rather than simulators
5. **Data Model Variations**: While conceptually the same, implementations vary in field naming:
   - Some use `text`/`isCompleted`, others use `title`/`done`
   - Some include soft delete functionality with a `deleted` field
