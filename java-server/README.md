# Ditto Java Server Quickstart App 🚀

## Prerequisites

For more information, see - [Java Install Guide](https://docs.ditto.live/sdk/latest/install-guides/java/server)

## Getting Started

1. Create an application at <https://portal.ditto.live/>.  Make note of the app ID and online playground token.
2. Copy the `.env.template` file at the top level of the `quickstart` repo to `.env` and add your app ID and online playground token.
3. Synchronize your project with the Gradle file by clicking Build > Sync Project with Gradle Files.
4. Open a terminal and run the following command to launch the app:
   - `./gradlew bootRun`
5. Open http://localhost:8080 in a browser to interact with the app.

## Additional Resources

- [Java Roadmap and Support Policy](https://docs.ditto.live/sdk/latest/install-guides/java/roadmap)
- [API Reference](https://software.ditto.live/java/ditto-java/4.11.0-preview.1/api-reference/)


## Offline-only mode (optional)

Set `DITTO_OFFLINE_LICENSE_TOKEN` in the repo-root `.env` to run this
app in offline-only mode (peer-to-peer only, no cloud sync). When the
token is non-empty, the playground/auth/websocket vars are not used.
Request a token from <support@ditto.com>. See the top-level
[README](../README.md#offline-only-mode-optional) for full details.
