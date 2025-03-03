<div align="center" style="display: flex; justify-content: space-around; align-items: center;">
  <img src="https://github.com/user-attachments/assets/7b0b1385-12d9-48d2-9005-deee70daa5f9" alt="Screenshot 1" style="width: 20%; margin-right: 120px;">
  <img src="https://github.com/user-attachments/assets/e58a4713-437a-4e21-af39-be3dcc8da814" alt="Screenshot 2" style="width: 20%;">
</div>

# Ditto Task Sync App

A sample React Native application that lets you create tasks and sync them with the Ditto Cloud via OnlinePlayground authentication. This example is built according to the [official Ditto installation guide for React Native](https://docs.ditto.live/install-guides/react-native).

## Prerequisites

- **Ditto Portal Account**: Ensure you have a Ditto account. Sign up [here](https://portal.ditto.live/signup).
- **App Credentials**: After registration, create an application within the Ditto Portal to obtain your `AppID`, `Online Playground Token`, `Auth URL`, and `Websocket URL`. Visit the [Ditto Portal](https://portal.ditto.live/) to manage your applications.

## Getting Started

### Install Dependencies

```bash
# Project is set up to work seamlessly with yarn
yarn install
```

### Run the Application

Running the `start` script launches the "Metro" dev tool, which will build the iOS or Android apps on demand.
After running the start command, press `i` to launch the iOS simulator or `a` to launch the Android emulator.

```bash
yarn start
```

For iOS:

```bash
(cd ios && pod install)
yarn react-native run-ios
```

For Android:

```bash
yarn react-native run-android
```

## Features

- **Task Creation**: Users can add new tasks to their list.
- **Real-time Sync**: Tasks are synchronized in real-time across all devices using the same Ditto application.

## Additional Information

- Limitation: React Native's Fast Refresh must be disabled and it's something we're working on fixing.


## iOS Installation
 If you encounter an issue with iOS installing pod files, check your terminal to make sure your environment is set up correctly.
For example:
```bash
❯ pod install

[!] Invalid `Podfile` file: cannot load such file -- /Users/xxxx/Developer/ditto/quickstart/react-native/node_modules/react-native/scripts/react_native_pods.rb
Debugger listening on ws://127.0.0.1:59620/xxxxx-xxxx-xxxx-xxxx-xxxxxxxxx
For help, see: https://nodejs.org/en/docs/inspector
Debugger attached.
Waiting for the debugger to disconnect....

 #  from /Users/xxxx/Developer/ditto/quickstart/react-native/ios/Podfile:2
 #  -------------------------------------------
 #  # Resolve react_native_pods.rb with node to allow for hoisting
 >  require Pod::Executable.execute_command('node', ['-p',
 #    'require.resolve(
 #  -------------------------------------------
``` 

This error is usually an issue with the terminal setup.  If you are using the terminal in your IDE, we suggest you use the terminal app that comes with iOS to see if this is an issue with your terminal setup.


### Troubleshooting

Should you encounter any issues, please refer to the [Ditto documentation](https://docs.ditto.live/) or check the FAQs on the Ditto Portal.

### Contact

For support or queries, reach out to us via [support@ditto.live](mailto:support@ditto.live).
