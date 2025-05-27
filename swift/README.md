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
2. Copy the `.env.sample` file at the top level of the `quickstart` repo to `.env` and add your App ID , Online playground Token, Auth URL, and Websocket URL.
3. Launch Xcode and open the `quickstart/swift/Tasks.xcodeproj` project.
4. Navigate to the project **Signing & Capabilities** tab and modify the **Team** and **Bundle Identifier** settings to your Apple developer account credentials to provision building to your device.
5. In Xcode, select a connected iOS device or iOS Simulator as the destination.
6. Choose the **Product > Build** menu item.  This should generate an `Env.swift` source file containing the values from your `.env` file, and then build the app.
7. Choose the **Product > Run** menu item.

The app will build and run on the selected device or emulator.  You can add,
edit, and delete tasks in the app.

If you run the app on additional devices or emulators, the data will be synced
between them.
