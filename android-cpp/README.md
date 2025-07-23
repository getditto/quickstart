# Ditto C++ Android Quickstart App 🚀

## Prerequisites

After you have completed the [common prerequisites] you will need the following:

- [Android Studio](https://developer.android.com/studio) Ladybug or newer

## Permissions (already configured)

- <https://docs.ditto.live/install-guides/kotlin/android-permissions>

## Documentation

- [C++ Install Guide](https://docs.ditto.live/install-guides/cpp)
- [C++ API Reference](https://software.ditto.live/cpp/Ditto/4.11.0/api-reference/)
- [C++ Release Notes](https://docs.ditto.live/release-notes/cpp)

[common prerequisites]: https://github.com/getditto/quickstart#common-prerequisites

## Building and Running the Android Application

Assuming you have Android Studio and other prerequisites installed, you can
build and run the app by following these steps:

1. Create an application at <https://portal.ditto.live/>.  Make note of the app ID and online playground token.
2. Copy the `.env.template` file at the top level of the `quickstart` repo to `.env` and add your app ID and online playground token.
3. Launch Android Studio and open the `quickstart/android` directory.
4. In Android Studio, select a connected Android device, or create and launch an Android emulator and select it as the destination, then choose the **Run > Run 'app'** menu item.

The app will build and run on the selected device or emulator.  You can add,
edit, and delete tasks in the app.

If you run the app on additional devices or emulators, the data will be synced
between them.

## Specifying the Ditto SDK Version

At the bottom of `app/build.gradle.kts`, you will see this line that causes
Android Studio to automatically download the Ditto SDK from Maven Central and
add it to the project:

```kotlin
    implementation("live.ditto:ditto:4.9.2")
```

To use a newer version of the SDK, change the version number in this line.
