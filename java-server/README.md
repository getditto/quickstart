# Ditto Java Server Quickstart App 🚀

## Prerequisites

For more information, see - [Java Install Guide](https://docs.ditto.live/sdk/latest/install-guides/java/server)

## Getting Started

1. Create an application at <https://portal.ditto.live/>.  Make note of the Database ID and development token.
2. Copy the `.env.sample` file at the top level of the `quickstart` repo to `.env` and add your Database ID and development token.
3. Synchronize your project with the Gradle file by clicking Build > Sync Project with Gradle Files.
4. Open a terminal and run the following command to launch the app:
   - `./gradlew bootRun`
5. Open http://localhost:8080 in a browser to interact with the app.

## Additional Resources

- [Java Roadmap and Support Policy](https://docs.ditto.live/sdk/latest/install-guides/java/roadmap)
- [API Reference](https://docs.ditto.live/sdk/latest/api-reference/java)


## Offline-only mode (optional)

Set `DITTO_DATABASE_ID` and `DITTO_OFFLINE_LICENSE_TOKEN` in the repo-root
`.env` to run this app in offline-only mode (peer-to-peer only, no cloud sync).
When the offline token is non-empty, `DITTO_DEVELOPMENT_TOKEN` and
`DITTO_SERVER_URL` are not used. Request a token from <support@ditto.com>. See
the top-level [README](../README.md#offline-only-mode-optional) for full
details.
