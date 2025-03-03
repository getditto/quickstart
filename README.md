# Ditto Quickstart Apps 🚀

This repo contains apps that demonstrate how to use the Ditto SDK for supported
programming languages and platforms.

See Ditto's [Quickstarts](https://docs.ditto.live/sdk/latest/quickstarts)
documentation for more information.

For support, please contact Ditto Support (<support@ditto.live>).

## Obtaining your Ditto Identity

The Ditto SDK requires you to provide an identity for your application, which may be
generated using the [Ditto Portal](https://portal.ditto.live/). For the purposes of these
quickstart applications, we'll be using the "Online Playground" identity type.

![Ditto Portal](.github/assets/ditto-portal.png)

> [!IMPORTANT]
> The Online Playground identity type is _not_ suitable for production use. It is intended
> only for development and testing purposes.

To obtain your Ditto identity and configure the quickstart apps with it, follow these steps:

1. Create a free account in the [Ditto Portal](https://portal.ditto.live/).
1. Create an app in the Ditto Portal.
1. Copy the `.env.sample` file to `.env`.
   - in a terminal: `cp .env.sample .env`.
   - in a macOS Finder window, press `⇧⌘.` (SHIFT+CMD+period) to show hidden files.
1. Save your App ID, Online Playground Token, Auth URL, and WebSocket URL in the `.env` file.

Please see the app-specific README files for details on the tools necessary to
build and run them.

## Apps

- [Android Kotlin](android-kotlin/README.md)
- [Android Java](android-java/README.md)
- [Android C++](android-cpp/README.md)
- [C++ TUI](cpp-tui/README.md)
- [C# .NET MAUI](dotnet-maui/README.md)
- [C# .NET TUI](dotnet-tui/README.md)
- [Flutter](flutter_quickstart/README.md)
- [Javascript TUI](javascript-tui/README.md)
- [Javascript Web](javascript-web/README.md)
- [React Native](react-native/README.md)
- [Rust TUI](rust-tui/README.md)
- [Swift](swift/README.md)

## 📄 License

This repo is licensed under the MIT License. See the [LICENSE](LICENSE) file for
rights and limitations.
