# Building Modules with Different Kotlin Versions

This project contains two independent application modules with different Kotlin versions:
- **app**: Kotlin 1.7.20 (with Jetpack Compose)
- **dittowrapper**: Kotlin 1.9.23 (AIDL service with blank launcher activity)

## The Challenge

Gradle has difficulty resolving multiple Kotlin plugin versions in the same build configuration. When both modules are included simultaneously, the build fails with compiler errors due to classpath conflicts.

## Solution: Build Modules Separately

Use the provided `switch-module.sh` script to automatically configure the build for either module:

### Build app module (Kotlin 1.7.20)

```bash
./switch-module.sh app
```

This script will:
- Enable the `:app` module in `settings.gradle.kts`
- Disable the `:dittowrapper` module in `settings.gradle.kts`
- Enable the Kotlin 1.7.20 classpath in `build.gradle.kts`

Then in Android Studio:
1. Select the correct build configuration
2. Sync Gradle (File → Sync Project with Gradle Files)
3. Build and run using the play button

### Build dittowrapper module (Kotlin 1.9.23)

```bash
./switch-module.sh dittowrapper
```

This script will:
- Disable the `:app` module in `settings.gradle.kts`
- Enable the `:dittowrapper` module in `settings.gradle.kts`
- Disable the Kotlin 1.7.20 classpath in `build.gradle.kts`

Then in Android Studio:
1. Select the correct build configuration
2. Sync Gradle (File → Sync Project with Gradle Files)
3. Build and run using the play button

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
