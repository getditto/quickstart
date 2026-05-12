# Ditto Kotlin Multiplatform Quickstart App 🚀

## Prerequisites

For more information, see - [Kotlin Multiplatform Install Guide](https://docs.ditto.live/sdk/latest/install-guides/kotlin/multiplatform)

## Getting Started

1. Create an application at <https://portal.ditto.live/>.  Make note of the app ID and online playground token.
2. Copy the `.env.template` file at the top level of the `quickstart` repo to `.env` and add your app ID and online playground token.
3. Synchronize your project with the Gradle file by clicking Build > Sync Project with Gradle Files.
4. Running app in the desired platform:
   1. Android:
      On Android Studio, run the `composeApp` application
   2. Compose Desktop
      Execute `./gradlew :composeApp:run`
   3. iOS
      Execute `open iosApp/iosApp.xcodeproj` or open `iosApp/iosApp.xcodeproj` in Xcode
      Run the application in Xcode

## Additional Resources

- [Kotlin Multiplatform Roadmap and Support Policy](https://docs.ditto.live/sdk/latest/install-guides/kotlin/multiplatform-roadmap)
- [API Reference](https://software.ditto.live/java/ditto-java/5.0.0-preview.3/api-reference/)

## Offline-only mode (optional)

Set `DITTO_OFFLINE_LICENSE_TOKEN` in the repo-root `.env` to run this
app in offline-only mode (peer-to-peer only, no cloud sync). When the
token is non-empty, the playground/auth/websocket vars are not used.
Request a token from <support@ditto.com>. See the top-level
[README](../README.md#offline-only-mode-optional) for full details.
