# SDK Directory

The Ditto C++ SDK is automatically downloaded when you run `make build` on Linux systems.

For **macOS and Windows** development, you'll need to manually download and install the SDK:

1. Download the appropriate Ditto C++ SDK for your platform from the [Ditto C++ Install Guide](https://docs.ditto.live/install-guides/cpp)
2. Extract the SDK and copy `Ditto.h` and `libditto.a` files to this directory

The build system will detect if the SDK files are missing and attempt to download them automatically on supported platforms.