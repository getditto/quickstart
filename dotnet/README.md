# Ditto .NET Quickstart Apps 🚀

This directory contains two subprojects: one for building a .NET MAUI app that
can be run on Android and iOS, and a .NET console app that can be run on
Windows, macOS, and Linux.

## Prerequisites

1. Install the .NET 9 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet/9.0>
2. For MAUI, run `sudo dotnet workload install maui` to install the required dependencies.
3. Create an application at <https://portal.ditto.live>. Make note of the app ID and online playground token
4. Copy the `.env.template` file at the top level of the quickstart repo to `.env` and add your app ID and online playground token.
5. If you want to build and test the app for iOS, install Xcode from the Mac App Store.
6. If you want to build and test the app for Android, install Android Studio, or install the Android SDK, Java JDK, and Android emulator.

## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/4.9.1/api-reference/)

## .NET MAUI

### Building and Running the App on iOS

These commands will build and run the app on the default iOS target:

```
cd DittoMauiTasksApp
dotnet build -t:Run -f net9.0-ios
```

### Building and Running the App on Android

These commands will build and run the app on the default Android target:

```
cd DittoMauiTasksApp
dotnet build -t:Run -f net9.0-android
```

### Other MAUI Platforms

Building the MAUI app for platforms other than iOS and Android is not supported
by Ditto at this time.


## .NET Console

These commands will build and run the console app on Windows, macOS, and Linux:

```
cd DittoDotNetTasksConsole
dotnet build
dotnet run 2>/dev/null
```

Note that standard error output must be redirected to `/dev/null` so that it
will not interfere with console terminal output.
