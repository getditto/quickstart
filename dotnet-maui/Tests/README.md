# .NET Maui Unit Tests
Basic integration tests for the DittoManager.cs file are located in the `DittoMauiTasksApp.Tests` project and can be ran from the Test Explorer in Visual Studio or the Unit Tests window in Rider.

- Rider Unit Test window: https://www.jetbrains.com/help/rider/Reference_Windows_Unit_Tests.html
- Visual Studio Test Explorer: https://learn.microsoft.com/en-us/visualstudio/test/run-unit-tests-with-test-explorer?view=vs-2022

For CI/CD, you can run it from the command line:

```bash
cd dotnet-maui/Tests/DittoMauiTasksApp.Tests
dotnet test
```

# .NET MAUI UI testing with Appium and NUnit 

For running UI Tests, you must have an environment variable set to the full path of the quickstart repo. On MacOS for example, you can set it from your .zshrc file:

```bash
export DittoQuickstartMauiPath="/Users/yourusername/Developer/ditto/quickstart"
```

In Windows, you will need to set the environment variable in the system properties.

## Project Structure

Below you will find an overview of all the projects found in this solution and a brief description of what they contain and how they fit together.

* DittoMauiTasksApp: This is the quickstart app that we are testing.  It is a simple task manager app that allows you to add, edit, and delete tasks that uses Ditto for the local database and syncing between other devices and the cloud.

* UITests.Shared: contains code that is shared across all the platforms as well as tests that will run on all the platforms targeted. For example, this project contains the `MainPageTests.cs`. The tests in this class are platform agnostic and will run for Android, iOS, macOS and Windows.

  This shared project also includes `AppiumServerHelper.cs` which contains code to start and stop the Appium server as part of running the tests.

* UITests.Android: contains code to bootstrap the UI tests for running on Android as well as Android-specific tests.
* UITests.iOS: contains code to bootstrap the UI tests for running on iOS as well as iOS-specific tests.
* UITests.macOS: contains code to bootstrap the UI tests for running on macOS as well as macOS-specific tests.
* UITests.Windows: contains code to bootstrap the UI tests for running on Windows as well as Windows-specific tests.

Each of the platform-specific projects contains a `AppiumSetup.cs` file. This file contains the details for each platform and configures the values accordingly. Each class contains inline comments to describe what they are.

### Code share considerations

While .NET MAUI by default uses the single-project approach, this is not always desired for UI testing. Therefore, there is a project for each platform you want to target. However, the tests run most smoothly if they are bundled into one assembly.

To make this possible we are using a so-called [NoTargets project](https://github.com/microsoft/MSBuildSdks/blob/main/src/NoTargets/). This type of project produces no assembly of its own. Instead, it acts as a collection of files that are easily accessible from within Visual Studio, including adding, removing and editing capabilities.

In each of the platform-specific projects, there are (invisible) links to the files of this project and these are compiled together with the platform-specific tests. There is no project reference between the shared project and the platform projects. The link is created through the .csproj file for each of the platform projects. This way, all the code ends up in one assembly making it easier to run the UI tests.

Typically, you should not notice any of this or have to worry about it.

## Getting started

Tests are executed through [Appium](https://appium.io/). Appium works by having a server execute the tests on a client application that runs on the target device. This means that running the tests will require an Appium server to run. In this example, code is provided to automatically start and stop the Appium server as part of the test run. However, you will still need to install Appium and its pre-requisites on the machine that you want to run the tests on.

These prerequisites are:

* [Node.js](https://nodejs.org/): needed to run Appium
* Appium version 2.0+ (npm install -g appium)
* Appium Drivers
  * UIAutomator2 for testing your Android app (can be used on both macOS and Windows)
    * [docs](https://appium.io/docs/en/2.0/quickstart/uiauto2-driver/)
  * XCUITest for testing your iOS app (only available on macOS)
    * [docs](https://github.com/appium/appium-xcuitest-driver)
  * Mac2 for testing your macOS app (only available on macOS) (appium driver install mac2)
    * [docs](https://github.com/appium/appium-mac2-driver)
    * For macOS also [Carthage](https://github.com/Carthage/Carthage) is needed
  * Windows for testing your Windows app (only available on Windows)
    * [docs](https://github.com/microsoft/WinAppDriver)
    * Make sure to use version 1.2.1. Other versions might not work.

## Writing tests

Ideally the majority of your tests will be under the `UITests.Shared` project. The goal of .NET MAUI is to write your code once and deploy to all the different platforms. As a result, the tests should also be able to run from a single codebase and still test your app on all targeted platforms.

However, there might be scenarios where you need platform specific tests. These can be placed under each respective platform project.

The NUnit framework is used in this solution, so writing tests will use all the features that NUnit has to offer. 

## Running tests locally

Tests can be run from the Visual Studio Test Explorer or by running `dotnet test` from the command-line.

To run the tests, an Appium server needs to be available and running, and that in turn should be able to reach the emulators, Simulator or physical devices as needed. For Android, make sure to either specify the emulator to be used in the configuration, or have an emulator already running/device already connected. If no emulator/device is detected, the tests will not run and throw an exception specifying that no connected Android device could be found.

> [!NOTE]
> For all platforms apart from macOS, you typically want to have your app deployed on the device that you want to test on. Make sure you have the latest app version is deployed to your device. Tests will be run against that app. The way this sample is set up, it will **not** deploy the app for you as part of the test run.

This sample does automatically start and stop the Appium server for you as part of the test run. This assumes that all of the pre-requisites are installed correctly.

If you want to start the Appium server manually, go into each `AppiumSetup.cs` file in each of the platform projects and comment out the lines that call `AppiumServerHelper`. There are two: one to start the server and one to stop it.

You will have to make sure the Appium server is started before running the tests and optionally configure the Appium drivers used in code to be able to reach your own server.

### Android Debug vs Release Build

On Android there is the concept of [Fast Deployment](https://learn.microsoft.com/xamarin/android/deploy-test/building-apps/build-process#fast-deployment) and because of that needs some special configuration when running with Appium.

In the `AppiumSetup.cs` of the `UITest.Android` project there are inline comments with instructions. However, this part of the configuration is very much dependent on how you setup your testing. If you use a Debug configuration build, evertyhing is setup as it should. When you want to test your resulting apk/aab binary file (or the Release configuration build), you will want to comment out the Debug configuration code and uncomment the Release configuration code and make sure the path to your app binary is correct.








