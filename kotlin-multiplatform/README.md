# Ditto Kotlin Multiplatform Quickstart App 🚀

This quickstart implements the same Tasks application for Android, iOS, and
Compose Desktop using shared Kotlin and Compose Multiplatform code.

## Prerequisites

- A Ditto application created in the [Ditto Portal](https://portal.ditto.live/)
- JDK 17
- The tools required by the platform you want to run:
  - Android Studio and the Android SDK for Android
  - Xcode on macOS for iOS
  - A supported desktop host: macOS Apple Silicon, Windows x64, or Linux

The Android SDK is not required when building only the desktop or iOS target.

## Configure Ditto

The app reads its Ditto configuration from `quickstart/.env`, the parent
directory of this project. From `quickstart/kotlin-multiplatform`, create it
from the tracked sample:

macOS or Linux:

```shell
cp ../.env.sample ../.env
```

Windows PowerShell:

```powershell
Copy-Item ..\.env.sample ..\.env
```

Set all three values using the credentials and server URL from the Ditto
Portal:

```dotenv
DITTO_DATABASE_ID="your-database-id"
DITTO_DEVELOPMENT_TOKEN="your-development-token"
DITTO_SERVER_URL="https://your-server-url"
```

The `.env` file is ignored by Git. These values are compiled into generated
quickstart source code, so do not commit the file or use a development token in
a production application.

## Build configuration

This standalone quickstart supports one optional Gradle project property:

| Property | Default | Effect |
| --- | --- | --- |
| `ditto.skipAndroidBuilds` | `false` | When `true`, does not apply the Android plugin or configure Android targets, source sets, tests, and dependencies. Use it for desktop or iOS builds on a machine without an Android SDK. |

Pass Gradle project properties with `-P`. Do not use
`ditto.skipAndroidBuilds=true` when building or running Android.

## Run the app

Run Gradle commands from `quickstart/kotlin-multiplatform`.

### Compose Desktop

The desktop build does not need an Android SDK when the Android target is
disabled.

macOS or Linux:

```shell
./gradlew -Pditto.skipAndroidBuilds=true :composeApp:run
```

Windows PowerShell:

```powershell
.\gradlew.bat "-Pditto.skipAndroidBuilds=true" :composeApp:run
```

The checked-in `composeApp [desktop]` IDE run configuration already supplies
this property.

To create an installable package for the current desktop operating system,
replace `:composeApp:run` with
`:composeApp:packageDistributionForCurrentOS`.

### Android

Open this directory in Android Studio, select the `composeApp` run
configuration, and run it on a device or emulator. To build from the command
line:

```shell
./gradlew :composeApp:assembleDebug
```

On Windows, use `.\gradlew.bat` in place of `./gradlew`.

### iOS

Open the Xcode project and run the `iosApp` scheme:

```shell
open iosApp/iosApp.xcodeproj
```

The Xcode build phase disables the Android target, so an Android SDK is not
required. To build the simulator framework directly with Gradle:

```shell
./gradlew -Pditto.skipAndroidBuilds=true :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Troubleshooting

- If a desktop build asks for an Android SDK, make sure
  `-Pditto.skipAndroidBuilds=true` is present.
- If `generateSecretProperties` reports that `.env` is missing, confirm the
  file is at `quickstart/.env`, not inside `kotlin-multiplatform`.
- Rerun the Gradle task after changing `.env` so the generated configuration is
  refreshed.

## Additional resources

- [Kotlin install guide](https://docs.ditto.live/sdk/latest/install-guides/kotlin)
- [Kotlin compatibility](https://docs.ditto.live/sdk/latest/compatibility/kotlin)
- [Kotlin release notes](https://docs.ditto.live/sdk/latest/release-notes/kotlin)
