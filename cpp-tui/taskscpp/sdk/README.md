# SDK Directory

## Platform Support

The Ditto C++ SDK has limited platform support. Based on the [official compatibility documentation](https://docs.ditto.live/reference/compatibility/cpp):

### ✅ **Supported Platforms**
- **Linux (x64)**: Ubuntu 20.04 LTS and later
- **Linux (AArch64)**: Ubuntu 22.04 LTS and later  
- **Android**: Version 6 and later (see `android-cpp` project)

### ❌ **Unsupported Platforms**
- **macOS**: Not supported by Ditto C++ SDK
- **iOS**: Not supported by Ditto C++ SDK  
- **Windows**: Not supported by Ditto C++ SDK

## Linux Setup (Automatic)

On **Linux** systems, the Ditto C++ SDK is automatically downloaded when you run `make build`. No manual setup required!

The build system detects your platform and architecture, then downloads the appropriate SDK version.

## Manual SDK Installation

If automatic download fails or you prefer manual installation, copy the `Ditto.h` and `libditto.a` files from the Ditto C++ SDK to this directory.