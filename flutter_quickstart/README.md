# Ditto Flutter Demo App

This is a basic task application that demonstrates how to use Ditto's peer-to-peer data sync within your Flutter Application.

![alt text](image.png)

## Getting Started

### 1. Clone the Repository

- Clone the repository from GitHub. Open a terminal and run the following command:

```bash
git clone https://github.com/getditto/quickstart
```

- Navigate to the project directory:

```bash
cd quickstart/flutter_quickstart
```

### 2. Install Dependencies

Install the necessary dependencies.

```bash
flutter pub get
```

### 3. Configure Ditto

#### Obtain App ID and Playground Token

- Log in to your Ditto Portal account
- Navigate to your application and obtain the App ID and Playground Token (see [Sync Credentials](https://docs.ditto.live/get-started/sync-credentials)
  for more details)

#### Update Flutter Application with Ditto Credentials

- Open the lib/main.dart file or the appropriate configuration file where Ditto is initialized
- Update the code with the App ID and Playground Token from your Ditto Application

```dart
const appID = "REPLACE_ME_WITH_YOUR_APP_ID";
const token = "REPLACE_ME_WITH_YOUR_PLAYGROUND_TOKEN";
```

### 4. Run the Application

#### Run the Application on Android

- Ensure you have an Android Emulator running or connect a physical device
- Run the following command in the terminal from the root of the application

```bash
flutter run
```

- Select the Android Emulator/Device

```text
[1]: sdk gphone64 arm64 (emulator-1234)
[2]: Mac Designed for iPad (mac-designed-for-ipad)
Please choose one (or "q" to quit):
```

#### Run the Application on iOS

- Ensure you have an iOS Simulator running or connect a physical iOS device
  Run the following command in the terminal and select the iOS Simulator/Device

```bash
flutter run
```

- Select the iOS Simulator/Device

```text
[1]: iPhone 15 (5A8DFE9D-7BF0-4BEE-A675-A056B64CEE3F)
[2]: Mac Designed for iPad (mac-designed-for-ipad)
Please choose one (or "q" to quit):
```

#### Run the Application on Web

- Run the following command in the terminal

```bash
flutter run -d chrome
```

The application will open in Google Chrome.

### 5. Sync-Data Offline

- Launch the application on multiple devices, emulators, or simulators
- Disconnect from your current WiFi network while keeping WiFi enabled on the device to allow for LAN connections
- Create new Tasks and update the Done status of existing tasks

_Note: Android Emulators cannot connect to iOS Simulators for offline sync due to platform limitations_

_Note: Flutter Web only supports syncing with the Ditto cloud, and offline sync is not supported_

# Additional Resources

Explore the following links and resources to learn more about Ditto:

#### General Information

- [Ditto Home Page](https://ditto.live)
- [Ditto Documentation Site](https://docs.ditto.live)
- [Ditto Flutter Install Guide](https://docs.ditto.live/flutter/installation)
- [Ditto Flutter Quick Start](https://docs.ditto.live/flutter/installation)
- [Ditto Flutter Roadmap](https://docs.ditto.live/flutter/roadmap)

#### Technical Information

- [Ditto Data Store CRUD](https://docs.ditto.live/crud/create)
- [Ditto Data Sync Subscriptions](https://docs.ditto.live/sync/subscriptions-management)
- [Ditto Query Language](https://docs.ditto.live/dql)
