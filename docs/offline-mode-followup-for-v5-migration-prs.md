# Offline-mode follow-up for the v5 migration PRs

This PR added `DITTO_OFFLINE_LICENSE_TOKEN` support across every shipped
quickstart. Four v5 migration PRs from @biozal were open at the time of
writing and will overwrite the init code we just edited:

- [#239 — Android Kotlin v5](https://github.com/getditto/quickstart/pull/239)
- [#237 — Flutter v5](https://github.com/getditto/quickstart/pull/237)
- [#267 — React Native v5](https://github.com/getditto/quickstart/pull/267)
- [#242 — Rust TUI v5](https://github.com/getditto/quickstart/pull/242)

When each migration PR lands, the offline-mode switch from this PR will be
lost (the init files are fully rewritten in those PRs). The recipe below
re-applies the switch on top of each migrated app.

## Universal contract

Every quickstart reads `DITTO_OFFLINE_LICENSE_TOKEN` from `.env`. If the
value is non-empty after trimming whitespace, the app initializes Ditto in
offline-only mode using the SDK's `SmallPeersOnly` connect variant and
calls `setOfflineOnlyLicenseToken(token)` on the Ditto instance. Otherwise
it falls back to the existing online playground init.

**Important — v4-only calls are gone in v5:**
`disableSyncWithV3()` and `ALTER SYSTEM SET DQL_STRICT_MODE = false` were
needed on the v4 SDK in both online and offline branches. They do **not**
exist in the v5 SDK and the v5 migration PRs already drop them. Do not
re-introduce them when applying the offline branch.

## Per-PR recipes

### #239 — Android Kotlin v5

Files: `android-kotlin/QuickStartTasks/app/src/main/java/live/ditto/quickstart/tasks/TasksApplication.kt`

After Aaron's migration, the init uses:

```kotlin
val config = DittoConfig(
    databaseId = secrets.DITTO_APP_ID,
    connect = DittoConfig.Connect.Server(url = secrets.DITTO_AUTH_URL),
)
val ditto = DittoFactory.create(config)
ditto.auth.expirationHandler = { /* ... */ }
```

Add the offline branch like this:

```kotlin
val offlineLicenseToken = secrets.DITTO_OFFLINE_LICENSE_TOKEN.trim()
val isOffline = offlineLicenseToken.isNotEmpty()

val config = DittoConfig(
    databaseId = secrets.DITTO_APP_ID,
    connect = if (isOffline) {
        DittoConfig.Connect.SmallPeersOnly(privateKey = null)
    } else {
        DittoConfig.Connect.Server(url = secrets.DITTO_AUTH_URL)
    },
)
val ditto = DittoFactory.create(config)
if (isOffline) {
    ditto.setOfflineOnlyLicenseToken(offlineLicenseToken)
} else {
    ditto.auth.expirationHandler = { /* unchanged */ }
}
```

Port the `DittoMode` enum and its JUnit test from this PR alongside the
init change.

### #237 — Flutter v5

Files: `flutter_app/lib/main.dart`

After Aaron's migration, the init likely uses `DittoConfig` + `DittoConnect.server(...)` and `Ditto.open(config: config)`. Apply the same branching:

```dart
final offlineLicenseToken =
    (dotenv.env['DITTO_OFFLINE_LICENSE_TOKEN'] ?? '').trim();
final isOffline = offlineLicenseToken.isNotEmpty;

final config = DittoConfig(
  databaseId: appID,
  connect: isOffline
      ? DittoConnect.smallPeersOnly()
      : DittoConnect.server(url: 'https://$databaseId.cloud.ditto.live'),
);
final ditto = await Ditto.open(config: config);

if (isOffline) {
  ditto.setOfflineOnlyLicenseToken(offlineLicenseToken);
} else {
  ditto.auth?.expirationHandler = (instance, secondsRemaining) async {
    // unchanged playground auth
  };
}
```

Note: v5 does **not** need the random `SiteID.fromInt(...)` workaround that
v4 required. The v5 `SmallPeersOnly` connect variant manages peer identity
internally. Port the `DittoMode` enum and `ditto_mode_test.dart` from this PR.

### #267 — React Native v5

Files: `react-native/App.tsx` (and `react-native/types/env.d.ts`)

The v5 init path is identical to the JS pattern this PR already uses for
`javascript-tui`, `javascript-web`, and `electron`. Aaron's migration may
restructure the file, but the branching contract stays the same:

```ts
const offlineLicenseToken = (DITTO_OFFLINE_LICENSE_TOKEN ?? '').trim();
const mode = selectMode(offlineLicenseToken); // helper from dittoMode.ts

const connectConfig: DittoConfigConnect =
  mode === 'offline'
    ? { mode: 'smallPeersOnly' }
    : { mode: 'server', url: DITTO_AUTH_URL };

const config = new DittoConfig(databaseId, connectConfig, 'custom-folder');
ditto.current = await Ditto.open(config);

if (mode === 'offline') {
  ditto.current.setOfflineOnlyLicenseToken(offlineLicenseToken);
} else {
  // existing playground auth via setExpirationHandler + auth.login
}
```

Port `dittoMode.ts` and `__tests__/dittoMode.test.ts` from this PR.

### #242 — Rust TUI v5

Files: `rust-tui/src/bin/main.rs`

After Aaron's migration, the init switches from v4 builders to:

```rust
let config = DittoConfig::new(
    database_id,
    DittoConfigConnect::Server { url: ... },
);
let ditto = Ditto::open_sync(config)?;
ditto.auth()?.set_expiration_handler(...)?;
```

Apply the offline branch:

```rust
let offline_license_token = cli.offline_license_token.trim().to_string();
let mode = select_mode(&offline_license_token);

let connect = match mode {
    DittoMode::Offline => DittoConfigConnect::SmallPeersOnly { private_key: None },
    DittoMode::OnlinePlayground => DittoConfigConnect::Server {
        url: cli.custom_auth_url.parse()?,
    },
};
let config = DittoConfig::new(cli.app_id.clone(), connect);
let ditto = Ditto::open_sync(config)?;

if mode == DittoMode::Offline {
    ditto.set_offline_only_license_token(&offline_license_token)?;
} else {
    ditto.auth()?.set_expiration_handler(/* existing handler */)?;
}
```

Port `select_mode`, the `DittoMode` enum, and the `#[cfg(test)] mod tests`
block from this PR.

## When to do this

After each of the four PRs above merges, rebase that app's offline switch
on top of it. The fastest path is probably one small follow-up PR per
migrated app rather than a single bundled one — each rebase is mechanical
but touches a different language.
