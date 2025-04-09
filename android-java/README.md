# Ditto Android Java Quickstart App 🚀

## Dependencies

- A modern JDK installation
- Android Studio

## Getting Started

To get started, you'll first need to create an app in the [Ditto Portal] with the
"Online Playground" authentication type. You'll need to find your AppId, Online Playground Token, Auth URL, and Websocket URL (at the top of the page) in order to use this quickstart.

In `app/src/main/java/com/example/dittotasks/MainActivity.java`, find the following variables:

```java
private String DITTO_APP_ID = "";
private String DITTO_PLAYGROUND_TOKEN = "";
private String DITTO_AUTH_URL = "";
private String DITTO_WEBSOCKET_URL = "";
```

Paste your own AppID, Playground Token, Auth URL, and Websocket URL into these variables, then click the Run button in Android Studio to launch the app in an emulator.
