# Ditto .NET MAUI Quickstart Apps 🚀

## Prerequisites

1. Install the .NET 9 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet/9.0>
2. Install the .NET MAUI workload by running `dotnet workload install maui`.
3. If you want to build and test the MAUI app for iOS, install Xcode from the Mac App Store.
4. If you want to build and test the MAUI app for Android, install Android Studio, or install the Android SDK, Java JDK, and Android emulator.
5. 2. Create an application at <https://portal.ditto.live>. Make note of the app ID and online playground token
6. Copy the `.env.sample` file at the top level of the quickstart repo to `.env` and add your app ID and online playground token.


## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/4.11.1/api-reference/)
### Restore Packages

```sh
cd DittoMauiTasksApp
dotnet restore
```

### Building and Running the App on iOS

These commands will build and run the app on the default iOS target:

```sh
dotnet build -t:Run -f net9.0-ios
```

### Building and Running the App on Android

These commands will build and run the app on the default Android target:

```sh
dotnet build -t:Run -f net9.0-android
```

### Building and Running the App on MacOS 

```sh
dotnet build -t:Run -f net9.0-maccatalyst 
```

### Building and Running the App on Windows 

```sh
dotnet build -t:Run -f net9.0-windows10.0.19041.0 
```

### Other MAUI Platforms

Other platforms not supported at this time. 



