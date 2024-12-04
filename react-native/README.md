<div align="center" style="display: flex; justify-content: space-around; align-items: center;">
  <img src="https://github.com/user-attachments/assets/7b0b1385-12d9-48d2-9005-deee70daa5f9" alt="Screenshot 1" style="width: 20%; margin-right: 120px;">
  <img src="https://github.com/user-attachments/assets/e58a4713-437a-4e21-af39-be3dcc8da814" alt="Screenshot 2" style="width: 20%;">
</div>

# Ditto Task Sync App

A sample React Native application that lets you create tasks and sync them with the Ditto Cloud via OnlinePlayground authentication. This example is built according to the [official Ditto installation guide for React Native](https://docs.ditto.live/install-guides/react-native).

## Prerequisites

- **Ditto Portal Account**: Ensure you have a Ditto account. Sign up [here](https://portal.ditto.live/signup).
- **App Credentials**: After registration, create an application within the Ditto Portal to obtain your `appID` and `token`. Visit the [Ditto Portal](https://portal.ditto.live/) to manage your applications.

## Getting Started

### Clone the Repository

```bash
git clone https://github.com/getditto/react-native-sample-app.git
cd react-native-sample-app
```

### Install Dependencies

```bash
yarn install
```

### Configure App Credentials

Navigate to the `App.tsx` file and replace `<Your-App-ID>` and `<Your-Token>` with your actual `appID` and `token` obtained from the Ditto Portal.

### Run the Application

```bash
yarn start
```

For iOS:

```bash
(cd ios && RCT_NEW_ARCH_ENABLED=1 pod install)
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

### Troubleshooting

Should you encounter any issues, please refer to the [Ditto documentation](https://docs.ditto.live/) or check the FAQs on the Ditto Portal.

### Contact

For support or queries, reach out to us via [support@ditto.live](mailto:support@ditto.live).
