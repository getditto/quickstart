# Ditto .NET Quickstart Apps 🚀

This directory contains two subprojects:  a .NET console app that can be run on
Windows, macOS, and Linux, and a .NET MAUI app that can be run on Android and
iOS.


## Prerequisites

1. Install the .NET 9 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet/9.0>
2. Create an application at <https://portal.ditto.live>. Make note of the app ID and online playground token
3. Copy the `.env.template` file at the top level of the quickstart repo to `.env` and add your app ID and online playground token.

See **Additional Prerequisites for MAUI** below for additional requirements for
building and running the MAUI app.


## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/4.9.1/api-reference/)


## .NET Console App

These commands will build and run the console app on Windows, macOS, and Linux:

```
cd DittoDotNetTasksConsole
dotnet build
dotnet run 2>/dev/null
```

Note that standard error output must be redirected to `/dev/null` so that it
will not interfere with console terminal output.


## .NET MAUI App

### Additional Prerequisites for MAUI

1. Install the .NET MAUI workload by running `dotnet workload install maui`.
2. If you want to build and test the MAUI app for iOS, install Xcode from the Mac App Store.
3. If you want to build and test the MAUI app for Android, install Android Studio, or install the Android SDK, Java JDK, and Android emulator.


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

