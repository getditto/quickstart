# Ditto .NET Console Quickstart App 🚀


## Prerequisites

1. Install the .NET 10 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet/10.0>
2. Create an application at <https://portal.ditto.live>. Make note of the app ID and online playground token
3. Copy the `.env.sample` file at the top level of the quickstart repo to `.env` and add your app ID and online playground token.


## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/5.0.0/api-reference/)


## .NET Console App

These commands will build and run the console app on Windows, macOS, and Linux (note may not run in Warp):

```
cd DittoDotNetTasksConsole
dotnet build
dotnet run
```

## Offline-only mode (optional)

Set `DITTO_OFFLINE_LICENSE_TOKEN` in the repo-root `.env` to run this
app in offline-only mode (peer-to-peer only, no cloud sync). When the
token is non-empty, the playground/auth/websocket vars are not used.
Request a token from <support@ditto.com>. See the top-level
[README](../README.md#offline-only-mode-optional) for full details.
