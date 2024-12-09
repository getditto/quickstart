# Ditto C++ QuickStart Tasks App

## Prerequisites

After you have completed the [common prerequisites] you will need the following:

- A C++ compiler (clang or gcc)
- CMake 3.10 or later
- Make

Download and unpack the C++ Ditto SDK for your platform by following the
instructions in the [Ditto C++ Install Guide](https://docs.ditto.live/install-guides/cpp).
Then, copy the `Ditto.h` and `libditto.a` files from the SDK into the `sdk/`
subdirectory of this project.


## Permissions (already configured)

- <https://docs.ditto.live/install-guides/swift#kX-Je>

## Documentation

- [C++ Install Guide](https://docs.ditto.live/install-guides/cpp)
- [C++ API Reference](https://software.ditto.live/cpp/Ditto/4.9.0/api-reference/)
- [C++ Release Notes](https://docs.ditto.live/release-notes/cpp)

[common prerequisites]: https://github.com/getditto/quickstart#common-prerequisites

## Building the Application

Assuming you have Xcode and other prerequisites installed, you can build and run the app by following these steps:

1. Create an application at <https://portal.ditto.live/>.  Make note of the app ID and online playground token.
2. Copy the `.env.template` file at the top level of the `quickstart` repo to `.env` and add your app ID and online playground token.
3. In a shell, navigate to the `quickstart/cpp/taskscpp` directory and run the command `make build` to build the C++ application.

## Running the Application

The application is named `taskscpp` and is located in the `quickstart/cpp/taskscpp/build` directory.

You can run the application and see the available command-line options from the
`quickstart/cpp/taskscpp` directory by running this command:

```sh
./build/taskscpp --help
```

If you run the application without any arguments, it will start running in a
terminal UI mode, displaying the available tasks in a list.  You can use these
keys to control the UI:

- Down arrow or `j`: Move down in the list
- Up arrow or `k`: Move up in the list
- `Space`, `Return`, or `Enter`: Toggle the completion status of the selected task
- `e`: Edit the title of the selected task
- `d`: Delete the selected task
- `c`: Create a new task
- `s`: Toggle Ditto synchrohnization on/off
- `q`: Quit the application

If you run the QuickStart Tasks app on other devices, the data will be synced
between them.
