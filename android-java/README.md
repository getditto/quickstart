# Ditto Android Java Quickstart App 🚀

## Prerequisites

After you have completed the [common prerequisites] you will need the following:

- [Android Studio](https://developer.android.com/studio) Ladybug or newer

## Permissions (already configured)

- <https://docs.ditto.live/sdk/latest/install-guides/java/android#configuring-permissions>

## Documentation

- [Install Guide](https://docs.ditto.live/sdk/latest/install-guides/java/android)
- [API Reference](https://software.ditto.live/android/Ditto/4.11.1/api-reference/)
- [SDK Release Notes](https://docs.ditto.live/sdk/latest/release-notes/java)

[common prerequisites]: https://github.com/getditto/quickstart#common-prerequisites

## Building and Running the Android Application

Assuming you have Android Studio and other prerequisites installed, you can
build and run the app by following these steps:

1. Create an application at <https://portal.ditto.live/>.  Make note of the app ID and online playground token.
2. Copy the `.env.sample` file at the top level of the `quickstart` repo to `.env` and add your App ID, Online Playground Token, Auth URL, and Websocket URL.
3. Launch Android Studio and open the `quickstart/android-java` directory.
4. In Android Studio, select a connected Android device, or create and launch an Android emulator and select it as the destination, then choose the **Run > Run 'app'** menu item.

The app will build and run on the selected device or emulator.  You can add, edit, and delete tasks in the app.

If you run the app on additional devices or emulators, the data will be synced between them.

Compatible with Android Automotive OS (AAOS)

## A Guided Tour of the Android App Source Code

The Android app is a simple to-do list app that demonstrates how to use the Ditto Android SDK to sync data with other devices.
It is implemented using Java and Android Views using an Activity and a programmatically implemented RecyclerView.

It is assumed that the reader is familiar with Android development and with Java/Activity/RecyclerView, but needs some guidance on how to use Ditto.  The following is a summary of the key parts of integration with Ditto.

### Adding the Ditto SDK

In `app/build.gradle.kts`, you will see this line that causes Android Studio
to download the Ditto SDK from Maven Central and add it to the project:

```kotlin
    implementation(libs.ditto)
```

This line in `gradle/libs.versions.toml` specifies which version of the Ditto
SDK to use:

```kotlin
ditto = "4.11.1"
```

To use a newer version of the SDK, change the version number on this line.
# Trigger fresh CI build with updated test methods
