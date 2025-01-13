# Ditto .NET Quickstart App 🚀

## Prerequisites

1. Install the .NET 8.0 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet/8.0>
2. In this directory, run `sudo dotnet workload restore` to install the required dependencies.
3. Create an application at <https://portal.ditto.live>. Make note of the app ID and online playground token
4. Copy the `.env.template` file at the top level of the quickstart repo to `.env` and add your app ID and online playground token.
5. If you want to build and test the app for iOS, install Xcode from the Mac App Store.
6. If you want to build and test the app for Android, install Android Studio, or install the Android SDK, Java JDK, and Android emulator.

## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/4.9.1/api-reference/)

## Building and Running the App on iOS

These commands will build and run the app on the default iOS target.

```
cd DittoMauiTasksApp
dotnet build -t:Run -f net8.0-ios
```

## Building and Running the App on Android

These commands will build and run the app on the default Android target.

```
cd DittoMauiTasksApp
dotnet build -t:Run -f net8.0-android
```
