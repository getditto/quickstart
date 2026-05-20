import 'dart:io' show Platform;

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/foundation.dart';
import 'package:permission_handler/permission_handler.dart';

/// Wraps the [Ditto] SDK lifecycle so the rest of the app can depend on a
/// configured instance without touching SDK init details.
///
/// The caller is responsible for invoking [initialize] before constructing
/// repositories that observe the store, and for calling [startSync] *after*
/// any repositories register their subscriptions (so the very first sync
/// cycle includes those queries).
class DittoService {
  Ditto? _ditto;

  Ditto get ditto {
    final d = _ditto;
    if (d == null) {
      throw StateError('DittoService.initialize() must be awaited first');
    }
    return d;
  }

  bool get isInitialized => _ditto != null;
  bool get isSyncActive => _ditto?.isSyncActive ?? false;

  Future<void> initialize({
    required String appId,
    required String playgroundToken,
    required String websocketUrl,
    String? authUrl,
    bool isTestMode = false,
  }) async {
    if (_ditto != null) return;

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

    final identity = OnlinePlaygroundIdentity(
      appID: appId,
      token: playgroundToken,
      // Must be false so the configured authUrl/websocketUrl are honored
      // instead of the default Ditto Cloud URLs.
      enableDittoCloudSync: false,
      customAuthUrl: authUrl,
    );
    final ditto = await Ditto.open(identity: identity);

    ditto.updateTransportConfig((config) {
      config.setAllPeerToPeerEnabled(true);
      config.connect.webSocketUrls.add(websocketUrl);
    });

    // Disable DQL strict mode for the quickstart's relaxed schema.
    // https://docs.ditto.live/dql/strict-mode
    await ditto.store.execute('ALTER SYSTEM SET DQL_STRICT_MODE = false');

    _ditto = ditto;
  }

  void startSync() => ditto.startSync();
  void stopSync() => ditto.stopSync();
}
