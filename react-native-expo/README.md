# Welcome to your Expo app 👋

<!-- CI test trigger -->

This is an [Expo](https://expo.dev) project created with [`create-expo-app`](https://www.npmjs.com/package/create-expo-app).
Ditto is already installed.

⚠️ Expo Go and Expo Web are <b>not</b> compatible with Ditto.

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
