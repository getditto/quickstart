# Welcome to your Expo app 👋

This is an [Expo](https://expo.dev) project created with [`create-expo-app`](https://www.npmjs.com/package/create-expo-app).
Ditto is already installed.

The Reanimated 4.3.0 and Worklets 0.8.1 versions are pinned together for React Native 0.83 native builds. They intentionally differ from Expo SDK 55's bundled version recommendations; keep the pair when running `expo install --fix`. See [Reanimated's compatibility table](https://github.com/software-mansion/react-native-reanimated/blob/4.3.0/packages/react-native-reanimated/compatibility.json).

`patch-package` applies a temporary Expo Modules Core CMake fix so its Worklets target uses Prefab. Track [Expo issue #42892](https://github.com/expo/expo/issues/42892) and remove `patches/expo-modules-core+55.0.22.patch` when a fixed Expo SDK 55 release is available.

⚠️ Expo Go and Expo Web are <b>not</b> compatible with Ditto.

## Requirements

- **Xcode 26.3 or later** — Ditto's Expo config plugin applies a targeted workaround for the `fmt`/Clang incompatibility introduced in Xcode 26.4.
- **Gradle 9.0+** — Included automatically via the Gradle wrapper.
- **JDK 17** — Required for Android builds. Install via `brew install openjdk@17` and ensure `JAVA_HOME` is set.

## Get started

1. Install dependencies

   ```bash
   yarn
   ```

2. Generate iOS and Android folders (Expo CNG)

   ```bash
    yarn expo prebuild
   ```

3. Run either `yarn run android` or `yarn run ios` based on the targeted platform

## Learn more

To learn more about developing your project with Expo, look at the following resources:

- [Expo documentation](https://docs.expo.dev/): Learn fundamentals, or go into advanced topics with our [guides](https://docs.expo.dev/guides).
- [Learn Expo tutorial](https://docs.expo.dev/tutorial/introduction/): Follow a step-by-step tutorial where you'll create a project that runs on Android, iOS, and the web.
- [Android emulator](https://docs.expo.dev/workflow/android-studio-emulator/)
- [iOS simulator](https://docs.expo.dev/workflow/ios-simulator/)

## Join the community

Join our community of developers creating universal apps.

- [Expo on GitHub](https://github.com/expo/expo): View our open source platform and contribute.
- [Discord community](https://chat.expo.dev): Chat with Expo users and ask questions.

## Troubleshooting

1. Bumping into something like this?

```sh
const stringWidth = require('string-width');
                    ^

Error [ERR_REQUIRE_ESM]: require() of ES Module /
```

Clean everything and start fresh:

```sh
yarn clean
```
