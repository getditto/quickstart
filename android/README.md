# Ditto Android Quickstart App 🚀

## Prerequisites

After you have completed the [common prerequisites] you will need the following:

- [Android Studio](https://developer.android.com/studio) Ladybug or newer

## Permissions (already configured)

- <https://docs.ditto.live/install-guides/kotlin/android-permissions>

## Documentation

- [Kotlin Install Guide](https://docs.ditto.live/install-guides/kotlin)
- [Kotlin API Reference](https://software.ditto.live/android/Ditto/4.8.2/api-reference/)
- [Kotlin SDK Release Notes](https://docs.ditto.live/release-notes/kotlin)

[common prerequisites]: https://github.com/getditto/quickstart#common-prerequisites

## Building and Running the Android Application

Assuming you have Android Studio and other prerequisites installed, you can build and run the app by following these steps:

1. Create an application at <https://portal.ditto.live/>.  Make note of the app ID and online playground token.
2. Copy the `.env.template` file at the top level of the `quickstart` repo to `.env` and add your app ID and online playground token.
3. Launch Android Studio and open the `quickstart/android` directory.
4. In Android Studio, select a connected Android device, or create and launch an Android emulator and select it as the destination, then choose the **Run > Run 'app'** menu item.

The app will build and run on the selected device or emulator.  You can add,
edit, and delete tasks in the app.

If you run the app on additional devices or emulators, the data will be synced
between them.

## A Guided Tour of the Android App Source Code

The Android app is a simple to-do list app that demonstrates how to use the
Ditto Android SDK to sync data with other devices.
It is implemented using [Kotlin](https://kotlinlang.org/) and
[Jetpack Compose](https://developer.android.com/compose), which is a modern
toolkit for building native Android UI.

It is assumed that the reader is familiar with Android development and with
Compose, but needs some guidance on how to use Ditto.  The following is a
summary of the key parts of integration with Ditto.

### Adding the Ditto SDK

At the bottom of `app/build.gradle.kts`, you will see this line that causes Android Studio to automatically download the Ditto SDK from Maven Central and add it to the project:

```kotlin
    implementation("live.ditto:ditto:4.8.2")
```

To use a newer version of the SDK, change the version number in this line.

### Initializing Ditto

In `app/src/main/java/live/ditto/quickstart/tasks/TasksApplication.kt`, you will
see the `TasksApplication` class.  This class is a subclass of `Application`.
Its `onCreate()` method, which is called by the system when the app is launched,
calls `setupDitto()`, which gets application ID and online playground token from
the build configuration and uses it to create an instance of the `Ditto` class
which is stored in a singleton object of the `DittoHandler` class.

Other classes import `live.ditto.quickstart.tasks.DittoHandler.Companion.ditto`
to access the singleton `Ditto` instance.

### The Task Data Model

The task data model is implemented as a Kotlin data class in
`app/src/main/java/live/ditto/quickstart/tasks/data/Task.kt`.  This class has
properties `_id`, `title`, `done`, and `deleted`.  The `_id` property is a
unique identifier for the task, and the `deleted` property is used to mark tasks
that have been deleted but are still present in the store.

This class has a `fromJson()` method that can be used to convert the JSON data
returned by the Ditto store into a `Task` object, which can then be used by
Kotlin code.

### The Tasks List

The main screen is a list of all tasks.  This is implemented in
`app/src/main/java/live/ditto/quickstart/tasks/list/TasksListScreen.kt`.  The
associated view model is in
`app/src/main/java/live/ditto/quickstart/tasks/list/TasksListScreenViewModel.kt`,
which is where all the code is for retrieving data from the Ditto store and
converting it into objects that can be displayed onscreen.

### The Edit Screen

The edit screen allows the user to enter a new task or to edit an existing task.
It is implemented in
`app/src/main/java/live/ditto/quickstart/tasks/list/edit/EditScreen.kt`.  The
associated view model is in
`app/src/main/java/live/ditto/quickstart/tasks/list/edit/EditScreenViewModel.kt`,
and this contains the associated code for manipulating data in the Ditto store.
