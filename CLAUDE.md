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

# Install on device/emulator
./gradlew installDebug

# Clean build
./gradlew clean
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
```

### Rust Projects
```bash
# Build project
cargo build

# Run application
cargo run

# Run tests
cargo test

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

## Architecture Overview

### Common Structure
All applications implement the same "Tasks" functionality:
- **Task Model**: Simple todo items with ID, text, and completion status
- **Real-time Sync**: Automatic synchronization between devices using Ditto
- **CRUD Operations**: Create, read, update, and delete tasks
- **Platform UI**: Native UI patterns for each platform

### Ditto SDK Integration Pattern
1. **Initialization**: Apps read credentials from environment variables
2. **Identity**: Uses "Online Playground" identity (development only)
3. **Collection**: All apps use a "tasks" collection
4. **Document Structure**: Consistent across platforms:
   ```json
   {
     "_id": "unique-id",
     "text": "Task description",
     "isCompleted": false
   }
   ```

### Platform-Specific Patterns

#### Android/Kotlin
- MVVM architecture with ViewModels
- Jetpack Compose for UI
- Gradle build with version catalogs
- Environment variables loaded via `loadEnvProperties()`

#### JavaScript Web
- React with TypeScript
- Vite build system
- Tailwind CSS for styling
- ESLint + Prettier for code quality

#### iOS/Swift
- SwiftUI for interface
- Xcode project structure
- CocoaPods for dependency management

#### Rust TUI
- Tokio async runtime
- Ratatui for terminal UI
- Event-driven architecture

## Key Development Considerations

### Environment Variables
All apps load configuration from the `.env` file at the repository root. The loading mechanism varies by platform:
- **Android**: Custom Gradle function in `build.gradle.kts`
- **JavaScript**: Vite environment variables
- **Rust**: `dotenvy` crate
- **Flutter**: `flutter_dotenv` package

### Testing
- Android: JUnit tests in `src/test` and instrumented tests in `src/androidTest`
- JavaScript: Jest for React Native, Vitest for web apps
- Rust: Built-in `cargo test`
- Flutter: `flutter test` with widget and unit tests

### Code Quality Tools
- **JavaScript/TypeScript**: ESLint + Prettier
- **Kotlin**: Built-in Kotlin linting
- **Rust**: `cargo fmt` and `cargo clippy`
- **Swift**: SwiftLint (where configured)

## Important Notes

1. **Production Warning**: These apps use "Online Playground" identity which is NOT suitable for production
2. **Cross-Platform Consistency**: All apps implement the same data model for interoperability
3. **Platform Best Practices**: Each app follows its platform's conventions and patterns
4. **Real Device Testing**: For full P2P functionality, test on real devices rather than simulators