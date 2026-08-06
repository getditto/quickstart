import 'dart:io' show Platform;

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_quickstart/ditto_mode.dart';
import 'package:permission_handler/permission_handler.dart';

/// Owns the Ditto instance lifecycle: permissions, config, open,
/// identity/auth, and sync start/stop. It vends a configured, already-running
/// Ditto instance for the rest of the app to use and knows nothing about tasks.
class DittoManager {
  final String databaseId =
      dotenv.env['DITTO_DATABASE_ID'] ?? (throw Exception("env not found"));
  final String token = dotenv.env['DITTO_DEVELOPMENT_TOKEN'] ?? '';
  final String serverUrl = dotenv.env['DITTO_SERVER_URL'] ?? '';
  final String offlineLicenseToken =
      (dotenv.env['DITTO_OFFLINE_LICENSE_TOKEN'] ?? '').trim();

  Ditto? _ditto;

  /// The ready Ditto instance. Only valid after [open] has completed.
  Ditto get ditto => _ditto!;

  bool get isSyncActive => ditto.sync.isActive;

  /// Initializes the Ditto instance with necessary permissions and configuration.
  /// https://docs.ditto.live/sdk/latest/install-guides/flutter#step-3-import-and-initialize-the-ditto-sdk
  ///
  /// This function:
  /// 1. Requests required Bluetooth and WiFi permissions on mobile platforms (Android/iOS)
  /// 2. Initializes the Ditto SDK
  /// 3. Sets up development identity with the provided database ID and token
  /// 4. Enables peer-to-peer communication on non-web platforms
  /// 5. Configures the connection to the Ditto server
  /// 6. Starts sync and returns the configured Ditto instance
  Future<Ditto> open() async {
    // Skip permissions in test mode - they block integration tests
    const isTestMode = bool.fromEnvironment(
      'INTEGRATION_TEST_MODE',
      defaultValue: false,
    );

    // Only request permissions on mobile platforms (Android/iOS)
    // Desktop platforms (macOS, Windows, Linux) don't require these permissions
    final isMobilePlatform = !kIsWeb && (Platform.isAndroid || Platform.isIOS);
    if (isMobilePlatform && !isTestMode) {
      await [
        Permission.bluetoothConnect,
        Permission.bluetoothAdvertise,
        Permission.nearbyWifiDevices,
        Permission.bluetoothScan,
      ].request();
    }

    await Ditto.init();

    DittoLogger.isEnabled = true;
    DittoLogger.minimumLogLevel = LogLevel.debug;

    final mode = selectDittoMode(offlineLicenseToken);
    if (mode == DittoMode.onlinePlayground &&
        (token.trim().isEmpty || serverUrl.trim().isEmpty)) {
      throw Exception(
        'Online mode requires DITTO_DEVELOPMENT_TOKEN and DITTO_SERVER_URL. '
        'Set DITTO_OFFLINE_LICENSE_TOKEN to use offline mode instead.',
      );
    }

    //new configuration -  https://docs.ditto.live/sdk/latest/ditto-config
    final config = DittoConfig(
      databaseID: databaseId,
      connect: mode == DittoMode.offline
          ? const DittoConfigConnectSmallPeersOnly()
          : DittoConfigConnectServer(url: serverUrl),
    );
    final ditto = await Ditto.open(config);
    _ditto = ditto;

    if (mode == DittoMode.offline) {
      ditto.setOfflineOnlyLicenseToken(offlineLicenseToken);
    } else {
      await ditto.auth.setExpirationHandler((ditto, timeUntilExpiration) async {
        final authResult = await ditto.auth.login(
          token: token,
          provider: Authenticator.developmentProvider,
        );
        if (authResult.exception != null) {
          throw authResult.exception!;
        }
      });
    }

    ditto.sync.start();

    return ditto;
  }

  void startSync() => ditto.sync.start();

  void stopSync() => ditto.sync.stop();
}
