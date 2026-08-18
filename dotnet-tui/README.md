# Ditto .NET Console Quickstart App 🚀


## Prerequisites

1. Install the .NET 10 SDK from <https://dotnet.microsoft.com/en-us/download/dotnet/10.0>
2. Create an application at <https://portal.ditto.live>. Make note of the Database ID and development token
3. Copy the `.env.sample` file at the top level of the quickstart repo to `.env` and add your Database ID and development token.


## Documentation

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://docs.ditto.live/sdk/latest/api-reference/c-sharp)


## .NET Console App

These commands will build and run the console app on Windows, macOS, and Linux (note may not run in Warp):

```
cd DittoDotNetTasksConsole
dotnet build
dotnet run
```

## Offline-only mode (optional)

Set `DITTO_DATABASE_ID` and `DITTO_OFFLINE_LICENSE_TOKEN` in the repo-root
`.env` to run this app in offline-only mode (peer-to-peer only, no cloud sync).
When the offline token is non-empty, `DITTO_DEVELOPMENT_TOKEN` and
`DITTO_SERVER_URL` are not used. Request a token from <support@ditto.com>. See
the top-level [README](../README.md#offline-only-mode-optional) for full
details.
