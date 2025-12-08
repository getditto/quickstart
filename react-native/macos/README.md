# macOS Support

## ⚠️ Compatibility Notice

**Status**: Currently non-functional due to version incompatibility.

This directory contains macOS support using `react-native-macos`, but it is currently **not compatible** with React Native 0.82.1.

### Technical Details

- **React Native version**: 0.82.1
- **react-native-macos version**: 0.79.1 (latest available)
- **Issue**: Codegen compatibility error - `UnsupportedGenericParserError: Module NativeIdleCallbacks: Unrecognized generic type 'IdleCallbackID'`

### When Will This Work?

macOS support will become functional once Microsoft releases `react-native-macos` version 0.82.x or higher. Check the [official releases page](https://github.com/microsoft/react-native-macos/releases) for updates.

### Current Workaround

If you need macOS support today, you would need to:
1. Downgrade React Native to 0.79.x, or
2. Wait for react-native-macos 0.82.x release

## Usage (Once Compatible)

```bash
# Install dependencies
yarn install

# Install pods
cd macos && pod install && cd ..

# Run macOS app
yarn macos
```
