# Ditto SDK v5 Migration — Expo-Specific Requirements

These are the changes required specifically for Expo projects when migrating from Ditto SDK v4 to v5. These are in addition to the general API migration steps (DittoConfig, auth, sync, DQL, etc.).

## 1. Add `expo-build-properties` dependency

The v5 Ditto Expo plugin internally calls `withBuildProperties()` from `expo-build-properties`. If this package is not installed, `expo prebuild` will fail with an import error.

```bash
npm install expo-build-properties@~1.0.0
```

## 2. Configure Kotlin version in `app.json`

The v5 SDK requires Kotlin 2.1.20 for Android builds. Add the `expo-build-properties` plugin to your `app.json` with the Kotlin version:

```json
{
  "expo": {
    "plugins": [
      [
        "expo-build-properties",
        {
          "android": {
            "kotlinVersion": "2.1.20"
          }
        }
      ]
    ]
  }
}
```

Without this, Android builds will fail with Kotlin compilation errors.

## 3. Call `init()` before using any Ditto API

v5 exports an `init()` function that must be called before creating a Ditto instance. On React Native this is effectively a no-op (it only loads WebAssembly on web), but all v5 examples call it and it is the recommended pattern.

```typescript
import { Ditto, DittoConfig, init } from '@dittolive/ditto';

async function startDitto() {
  await init();
  const config = new DittoConfig(databaseId, connectConfig);
  const ditto = await Ditto.open(config);
  // ...
}
```

## 4. Regenerate native projects

After making the above changes, delete the existing `ios` and `android` directories and regenerate them:

```bash
rm -rf ios android
npx expo prebuild
```

The v5 Expo plugin automatically handles:

- **iOS**: `NSBluetoothAlwaysUsageDescription`, `NSLocalNetworkUsageDescription`, Bonjour services, and background modes in `Info.plist`
- **Android**: Bluetooth/WiFi/location permissions in `AndroidManifest.xml` and `packagingOptions.pickFirsts` for native libraries (`libdittoffi.so`, `libjsi.so`, `libreact_nativemodule_core.so`, `libturbomodulejsijni.so`, `libreactnative.so`)

## 5. Update `yarn.lock` (if present)

If your project has both `package-lock.json` and `yarn.lock`, update both after adding the new dependency:

```bash
npm install
npx yarn install
```
