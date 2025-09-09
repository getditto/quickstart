# SDK Directory

## Linux (Works Out of the Box)
On **Linux** systems, the Ditto C++ SDK is automatically downloaded when you run `make build`. No manual setup required!

## macOS and Windows (Manual Setup Required)
For **macOS and Windows** development, copy the `Ditto.h` and `libditto.a` files from the Ditto C++ SDK to this directory.

The build system will detect if the SDK files are present and use them, or attempt automatic download on Linux.