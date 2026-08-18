# Ditto iOS Quickstart App 🚀

## Prerequisites

After you have completed the [common prerequisites] you will need the following:

- [Xcode](https://developer.apple.com/xcode/) 16 or greater (Required for Swift 6)

## Permissions (already configured)

- <https://docs.ditto.live/install-guides/swift#kX-Je>

## Documentation

- [Swift Install Guide](https://docs.ditto.live/install-guides/swift)
- [Swift API Reference](https://docs.ditto.live/sdk/latest/api-reference/swift)
- [Swift Release Notes](https://docs.ditto.live/release-notes/swift)

[common prerequisites]: https://github.com/getditto/quickstart#common-prerequisites

## Building and Running the iOS Application

Assuming you have Xcode and other prerequisites installed, you can build and run the app by following these steps:

1. Create an application at <https://portal.ditto.live/>.  Make note of the Database ID and Development Token.
2. Copy the `.env.sample` file at the top level of the `quickstart` repo to `.env` and add your Database ID, Development Token, and Server URL.
3. Launch Xcode and open the `quickstart/swift/Tasks.xcodeproj` project.
4. Navigate to the project **Signing & Capabilities** tab and modify the **Team** and **Bundle Identifier** settings to your Apple developer account credentials to provision building to your device.
5. In Xcode, select a connected iOS device or iOS Simulator as the destination.
6. Choose the **Product > Build** menu item.  This should generate an `Env.swift` source file containing the values from your `.env` file, and then build the app.
7. Choose the **Product > Run** menu item.

The app will build and run on the selected device or emulator.  You can add,
edit, and delete tasks in the app.

If you run the app on additional devices or emulators, the data will be synced
between them.

## Offline-only mode (optional)

Set `DITTO_DATABASE_ID` and `DITTO_OFFLINE_LICENSE_TOKEN` in the repo-root
`.env` to run this app in offline-only mode (peer-to-peer only, no cloud sync).
When the offline token is non-empty, `DITTO_DEVELOPMENT_TOKEN` and
`DITTO_SERVER_URL` are not used. Request a token from <support@ditto.com>. See
the top-level [README](../README.md#offline-only-mode-optional) for full
details.
