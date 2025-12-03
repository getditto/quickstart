# Building Modules with Different Kotlin Versions

This project contains two independent application modules with different Kotlin versions:
- **app**: Kotlin 1.7.20 (with Jetpack Compose)
- **dittowrapper**: Kotlin 1.9.23 (AIDL service with blank launcher activity)

## The Challenge

Gradle has difficulty resolving multiple Kotlin plugin versions in the same build configuration. When both modules are included simultaneously, the build fails with compiler errors due to classpath conflicts.

## Solution: Build Modules Separately

### Build app module (Kotlin 1.7.20)

1. Edit `settings.gradle.kts`:
```kotlin
rootProject.name = "QuickStart Tasks"
include(":app")              // Enable app
//include(":dittowrapper")   // Comment out dittowrapper
```

2. Edit `build.gradle.kts` (root):
```kotlin
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")  // Uncomment
    }
}
```

3. Build:
```bash
./gradlew --stop && ./gradlew clean && ./gradlew :app:assembleDebug
```

### Build dittowrapper module (Kotlin 1.9.23)

1. Edit `settings.gradle.kts`:
```kotlin
rootProject.name = "QuickStart Tasks"
//include(":app")             // Comment out app
include(":dittowrapper")      // Enable dittowrapper
```

2. Edit `build.gradle.kts` (root):
```kotlin
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        // classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")  // Comment out
    }
}
```

3. Build:
```bash
./gradlew --stop && ./gradlew clean && ./gradlew :dittowrapper:assembleDebug
```

## About dittowrapper

The dittowrapper module is designed to be installed as an independent APK and run as a background service. It includes:
- A blank `MainActivity` that immediately finishes after launch (required for APK installation)
- AIDL service interfaces for inter-process communication with other applications
- Kotlin 1.9.23 for modern language features

## Why This Is Necessary

- The app module requires Kotlin 1.7.20 for experiments with specific Compose compiler versions
- The dittowrapper module uses Kotlin 1.9.23 for newer language features and as an AIDL service wrapper
- Gradle's buildscript classpath and plugin resolution mechanism apply globally during configuration
- Having two different Kotlin plugin versions causes classpath conflicts that cannot be resolved within a single Gradle configuration phase
- Both modules are independent applications (not library dependencies) and are meant to run separately

## Key Configuration Details

- **app module**: Gets Kotlin version from root `build.gradle.kts` buildscript classpath (1.7.20)
- **dittowrapper module**: Specifies Kotlin version explicitly in its plugins block (`version "1.9.23"`)
- The root buildscript classpath must be commented out when building dittowrapper to avoid conflicts

## Future Improvements

Consider these approaches if you need to build both modules together:
1. Upgrade app to Kotlin 1.9+ (requires Compose compiler updates)
2. Use Gradle composite builds to isolate Kotlin versions completely
3. Split into separate Git repositories or submodules for complete isolation