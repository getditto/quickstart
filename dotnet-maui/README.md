# Ditto .NET MAUI Quickstart Apps 🚀

## Prerequisites

1. Install the .NET 10 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet/10.0>
2. Install the .NET MAUI workload by running `dotnet workload install maui`.
3. If you want to build and test the MAUI app for iOS, install Xcode from the Mac App Store.
4. If you want to build and test the MAUI app for Android, install Android Studio, or install the Android SDK, Java JDK, and Android emulator.
5. Create an application at <https://portal.ditto.live>. Make note of the app ID and online playground token
6. Copy the `.env.sample` file at the top level of the quickstart repo to `.env` and add your app ID and online playground token.

### Environment variables (Android builds and tests)

The .NET Android SDK requires `JAVA_HOME` and (for the UI tests / Appium) `ANDROID_HOME`. 

## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/5.0.0/api-reference/)
### Restore Packages

```sh
cd DittoMauiTasksApp
dotnet restore
```

### Building and Running the App on iOS

These commands will build and run the app on the default iOS target:

```sh
dotnet build -t:Run -f net10.0-ios
```

If your installed Xcode is newer than the version the .NET iOS workload was published against, you'll see `error: This version of .NET for iOS (...) requires Xcode XX.X`. Bypass the check with:

```sh
dotnet build -t:Run -f net10.0-ios -p:ValidateXcodeVersion=false
```

### Building and Running the App on Android

These commands will build and run the app on the default Android target:

```sh
dotnet build -t:Run -f net10.0-android
```

### Building and Running the App on MacOS 

```sh
dotnet build -t:Run -f net10.0-maccatalyst 
```

### Building and Running the App on Windows 

```sh
dotnet build -t:Run -f net10.0-windows10.0.19041.0 
```

### Other MAUI Platforms

Other platforms not supported at this time.

## Running the UI Tests Locally

The `UITests.iOS` and `UITests.Android` projects are Appium-driven UI tests run via `dotnet run` (Program.cs is the entry point). They require:

1. **Appium 2 + drivers**:
   ```sh
   npm install -g appium
   appium driver install xcuitest
   appium driver install uiautomator2
   ```
2. **Appium server** running with `ANDROID_HOME` and `JAVA_HOME` exported in *its* shell:
   ```sh
   appium
   ```
3. **A booted simulator/emulator** (or a connected device).
4. **The app installed on the target** before `dotnet run`.
5. **`DITTO_CLOUD_TASK_TITLE`** set to a task title that exists in your Ditto cloud (the test asserts it can be found in the synced UI).

### iOS

```sh
# Build & install to the booted simulator
dotnet build DittoMauiTasksApp/DittoMauiTasksApp.csproj \
  -f net10.0-ios -p:ValidateXcodeVersion=false -t:Run

# Run the test
cd UITests.iOS
DITTO_CLOUD_TASK_TITLE="<seeded-title>" dotnet run
```

### Android

The Debug APK uses Mono Fast Deployment by default, which only works when launched via `dotnet build -t:Run`. If you `adb install` the APK manually, build with assemblies embedded:

```sh
dotnet build DittoMauiTasksApp/DittoMauiTasksApp.csproj \
  -f net10.0-android -p:EmbedAssembliesIntoApk=true

adb -s emulator-5554 install -r \
  DittoMauiTasksApp/bin/Debug/net10.0-android/live.ditto.quickstart.mauitasksapp-Signed.apk
```

When multiple Android devices are connected (e.g., an emulator + an unauthorized physical device), point the test at a specific one with `APPIUM_UDID`:

```sh
cd UITests.Android
APPIUM_UDID="emulator-5554" \
DITTO_CLOUD_TASK_TITLE="<seeded-title>" dotnet run
```
