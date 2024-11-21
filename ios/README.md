# Ditto iOS Quickstart App 🚀

## Prerequisites

After you have completed the [common prerequisites] you will need the following:

- [Xcode](https://developer.apple.com/xcode/) 15 or greater

## Permissions (already configured)

- <https://docs.ditto.live/install-guides/swift#kX-Je>

## Documentation

- [Swift Install Guide](https://docs.ditto.live/install-guides/swift)
- [Swift API Reference](https://software.ditto.live/cocoa/DittoSwift/4.8.2/api-reference/)
- [Swift Release Notes](https://docs.ditto.live/release-notes/swift)

[common prerequisites]: https://github.com/getditto/quickstart#common-prerequisites

## Building and Running the iOS Application

Assuming you have Xcode and other prerequisites installed, you can build and run the app by following these steps:

1. Create an application at <https://portal.ditto.live/>.  Make note of the app ID and online playground token.
2. Copy the `.env.template` file at the top level of the `quickstart` repo to `.env` and add your app ID and online playground token.
3. Launch Xcode and open the `quickstart/ios/tasks/tasks.xcodeproj` project.
4. Navigate to the project **Signing & Capabilities** tab and modify the **Team** and **Bundle Identifier** settings to your Apple developer account credentials to provision building to your device.
5. In Xcode, select a connected iOS device or iOS Simulator as the destination.
6. Choose the **Product > Build** menu item.  This should generate an `Env.swift` source file containing the values from your `.env` file, and then build the app.
7. Choose the **Product > Run** menu item.

The app will build and run on the selected device or emulator.  You can add,
edit, and delete tasks in the app.

If you run the app on additional devices or emulators, the data will be synced
between them.

## A Guided Tour of the iOS App Source Code

The iOS app is a simple to-do list app that demonstrates how to use the Ditto
Android SDK to sync data with other devices.  It is implemented using Apple's
[SwiftUI](https://developer.apple.com/xcode/swiftui/) framework, which is a
modern toolkit for building native iOS UI.

It is assumed that the reader is familiar with iOS development and with
Swift, but needs some guidance on how to use Ditto.  The following is a
summary of the key parts of integration with Ditto.

### Adding the Ditto SDK

If you look at the **Package Dependencies** section of the Xcode Project
Navigator panel, you will see the `Ditto` package listed there.

See the [Ditto Swift Install Guide](https://docs.ditto.live/install-guides/swift)
for instructions on adding the Ditto SDK to an Xcode project.  (These steps have
already been performed for you in this project.)

### Initializing Ditto

In `DittoManager.swift`, you will see the `DittoManager` class, which manages a
singleton `Ditto` instance.  This class gets application ID and online
playground token from the build configuration and uses it to create the
singleton instance.

### The Task Data Model

The task data model is implemented as a Swift struct in `TaskModel.swift`.  This
class has properties `_id`, `title`, `done`, and `deleted`.  The `_id` property
is a unique identifier for the task, and the `deleted` property is used to mark
tasks that have been deleted but are still present in the store.

This class has a Swift `Codable`-compliant initializer that can be used to
convert the JSON data returned by the Ditto store into a `TaskModel` object,
which can then be used by other Swift code.

### The Tasks List

The main screen is a list of all tasks.  This is implemented in
`TasksListScreen.swift`.  The flle contains both `TasksListScreenViewModel`,
which contains all the code needed to retrieve and modify data in the Ditto
store, and `TasksListScreen`, which contains the SwiftUI code for displaying the
list of tasks and reacting to user actions.

### The Edit Screen

The edit screen allows the user to enter a new task or to edit an existing task.
It is implemented in `EditScreen.swift`.  The flle contains both
`EditScreenViewModel`, which contains all the code needed to retrieve and modify
data in the Ditto store, and `EditScreen`, which contains the SwiftUI code for
creating or editing a task.
