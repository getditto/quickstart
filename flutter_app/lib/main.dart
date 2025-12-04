import 'dart:io' show Platform;
import 'dart:convert';
import 'dart:async';

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_quickstart/models/event.dart';
import 'package:flutter_quickstart/services/event_generator.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  //load in the .env file
  await dotenv.load(fileName: ".env");
  runApp(const MaterialApp(home: DittoExample()));
}

class DittoExample extends StatefulWidget {
  const DittoExample({super.key});

  @override
  State<DittoExample> createState() => _DittoExampleState();
}

class _DittoExampleState extends State<DittoExample> {
  Ditto? _ditto;
  EventGenerator? _eventGenerator;
  bool _observerActive = false;

  // FIXED TIMESTAMP: Set once at observer start (matches customer's setup)
  // Customer observes ALL events from match start - timestamp NEVER changes
  int? _matchStartTimestampUs;

  // CUSTOMER PATTERN: Manual observer and StreamController management
  StoreObserver? _observer;
  SyncSubscription? _subscription;
  StreamController<Map<String, dynamic>>? _eventStreamController;

  final appID =
      dotenv.env['DITTO_APP_ID'] ?? (throw Exception("env not found"));
  final token =
      dotenv.env['DITTO_PLAYGROUND_TOKEN'] ??
      (throw Exception("env not found"));
  final authUrl = dotenv.env['DITTO_AUTH_URL'];
  final websocketUrl =
      dotenv.env['DITTO_WEBSOCKET_URL'] ?? (throw Exception("env not found"));

  @override
  void initState() {
    super.initState();

    _initDitto();
  }

  /// Initializes the Ditto instance with necessary permissions and configuration.
  /// https://docs.ditto.live/sdk/latest/install-guides/flutter#step-3-import-and-initialize-the-ditto-sdk
  ///
  /// This function:
  /// 1. Requests required Bluetooth and WiFi permissions on mobile platforms (Android/iOS)
  /// 2. Initializes the Ditto SDK
  /// 3. Sets up online playground identity with the provided app ID and token
  /// 4. Enables peer-to-peer communication on non-web platforms
  /// 5. Configures WebSocket connection to Ditto cloud
  /// 6. Disables DQL strict mode
  /// 7. Starts sync and updates the app state with the configured Ditto instance
  Future<void> _initDitto() async {
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

    final identity = OnlinePlaygroundIdentity(
      appID: appID,
      token: token,
      enableDittoCloudSync:
          false, // This is required to be set to false to use the correct URLs
      customAuthUrl: authUrl,
    );

    final ditto = await Ditto.open(identity: identity);

    ditto.updateTransportConfig((config) {
      // Enable all peer-to-peer transports (BLE, WiFi Direct, LAN)
      config.setAllPeerToPeerEnabled(true);

      // Enable cloud sync via WebSocket
      config.connect.webSocketUrls.add(websocketUrl);
    });

    // Disable DQL strict mode
    // https://docs.ditto.live/dql/strict-mode
    await ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false");

    ditto.startSync();

    // Initialize event generator for CPU reproduction test
    final eventGenerator = EventGenerator(ditto);

    if (mounted) {
      setState(() {
        _ditto = ditto;
        _eventGenerator = eventGenerator;
      });
    }
  }

  Future<void> _toggleEventGeneration() async {
    if (_eventGenerator!.isRunning) {
      _eventGenerator!.stop();
    } else {
      await _eventGenerator!.start();
    }
    setState(() {});
  }

  Future<void> _toggleObserver() async {
    if (_observerActive) {
      _stopCustomerObserver();
    } else {
      _startCustomerObserver();
    }
    setState(() {
      _observerActive = !_observerActive;
    });
  }

  // CUSTOMER'S EXACT OBSERVER PATTERN - Manual StreamController + onChange
  void _startCustomerObserver() {
    print('DittoStorage: Starting customer observer pattern');

    // Set timestamp ONCE at match start (customer's pattern)
    // This creates a FIXED timestamp that never changes - all events from this point forward accumulate
    _matchStartTimestampUs = DateTime.now().microsecondsSinceEpoch;
    print('DittoStorage: Fixed timestamp set to: $_matchStartTimestampUs');
    print(
      'DittoStorage: All events from this timestamp forward will accumulate (growing result set)',
    );

    // Create StreamController with onCancel callback
    _eventStreamController = StreamController<Map<String, dynamic>>(
      onCancel: () {
        print('DittoStorage: streamData cancelled');
        _observer?.cancel();
      },
    );

    const observerQuery = """
      SELECT * FROM events
      USE DIRECTIVES '{
        "#index": ["event_timestamp_index", "eventcourtid_index"],
        "#prefer_order": true
      }'
      WHERE courtId == :courtId
        AND timestamp_us >= :threshold
      ORDER BY timestamp_us DESC
    """;

    const subscriptionQuery = """
      SELECT * FROM events
      WHERE courtId == :courtId
        AND timestamp_us >= :threshold
    """;

    // ⚠️ FIXED TIMESTAMP - Set once, NEVER recalculated!
    // Customer's pattern: timestamp set at match start, accumulates ALL events over time
    final arguments = {
      "courtId": "tour-5",
      "threshold": _matchStartTimestampUs!,
    };

    // Register observer with onChange callback - CUSTOMER'S EXACT PATTERN
    _observer = _ditto!.store.registerObserver(
      observerQuery,
      arguments: arguments,
      onChange: (QueryResult qr) {
        final timestamp = DateTime.now().toIso8601String();
        print('🔍 [$timestamp] MATERIALIZING CALLBACK TRIGGERED');
        print('   📊 Total items in qr.items: ${qr.items.length}');

        // ⚠️ CUSTOMER PATTERN: Iterate through ALL items (even with LIMIT 1)
        var itemCount = 0;
        for (var item in qr.items) {
          itemCount++;
          print('   📄 Materializing item #$itemCount/${qr.items.length}');

          if (!_eventStreamController!.isClosed) {
            _eventStreamController!.add(item.value); // High CPU even if removed
            print('   ✅ Added item #$itemCount to StreamController');
          } else {
            print('   ⚠️ StreamController is closed, cancelling observer');
            _observer?.cancel();
            _observer = null;
          }
        }
        print('   ✅ Materialization complete: $itemCount items processed\n');
      },
    );

    // Register subscription
    _subscription = _ditto!.sync.registerSubscription(
      subscriptionQuery,
      arguments: arguments,
    );

    // Setup error handling and onCancel
    _eventStreamController!.stream.handleError((e, st) {
      print("DittoStorage handleError ($e) cancel streamData controller. $st");
      if (!_eventStreamController!.isClosed) _eventStreamController!.close();
      if (_observer != null && !_observer!.isCancelled) _observer!.cancel();
    });
  }

  void _stopCustomerObserver() {
    print('DittoStorage: Stopping customer observer pattern');

    _observer?.cancel();
    _observer = null;

    _subscription?.cancel();
    _subscription = null;

    if (_eventStreamController != null && !_eventStreamController!.isClosed) {
      _eventStreamController!.close();
    }
    _eventStreamController = null;

    // Reset timestamp for next observer session
    _matchStartTimestampUs = null;
  }

  @override
  Widget build(BuildContext context) {
    if (_ditto == null) return _loading;

    return Scaffold(
      appBar: AppBar(title: const Text("Ditto CPU Reproduction")),
      body: SingleChildScrollView(
        child: Column(
          children: [
            _portalInfo,
            _syncTile,
            const Divider(),
            _reproductionControls,
            const Divider(),
            if (_observerActive)
              Container(
                constraints: const BoxConstraints(
                  minHeight: 400,
                  maxHeight: 600,
                ),
                child: _eventObserver,
              ),
          ],
        ),
      ),
    );
  }

  Widget get _loading => Scaffold(
    appBar: AppBar(title: const Text("Ditto Tasks")),
    body: Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center, // Center vertically
        crossAxisAlignment: CrossAxisAlignment.center, // Center horizontally
        children: [
          const CircularProgressIndicator(),
          const Text("Ensure your AppID and Token are correct"),
          _portalInfo,
        ],
      ),
    ),
  );

  Widget get _portalInfo =>
      Column(children: [Text("AppID: $appID"), Text("Token: $token")]);

  Widget get _syncTile => SwitchListTile(
    title: const Text("Sync Active"),
    value: _ditto!.isSyncActive,
    onChanged: (value) {
      if (value) {
        setState(() => _ditto!.startSync());
      } else {
        setState(() => _ditto!.stopSync());
      }
    },
  );

  Widget get _reproductionControls => Card(
    margin: const EdgeInsets.all(8),
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            "SDKS-2280: CPU Reproduction Test",
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),

          // PRODUCER MODE
          const Text(
            "Producer Mode:",
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
          ),
          const SizedBox(height: 8),

          // Event Size Toggle
          Row(
            children: [
              const Text(
                "Event Size:",
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(width: 16),
              ChoiceChip(
                label: const Text("Small (~100B)"),
                selected: _eventGenerator?.eventSize == EventSize.small,
                onSelected: (_) {
                  _eventGenerator?.setEventSize(EventSize.small);
                  setState(() {});
                },
              ),
              const SizedBox(width: 8),
              ChoiceChip(
                label: const Text("Large (3.3KB)"),
                selected: _eventGenerator?.eventSize == EventSize.large,
                onSelected: (_) {
                  _eventGenerator?.setEventSize(EventSize.large);
                  setState(() {});
                },
              ),
            ],
          ),
          const SizedBox(height: 8),

          // Continuous Generation
          SwitchListTile(
            title: Text(
              "Continuous Generation (${_eventGenerator?.eventsPerSecond.toStringAsFixed(1) ?? '1.0'}/sec)",
            ),
            subtitle: Text(
              "Generated this session: ${_eventGenerator?.eventCount ?? 0}",
            ),
            value: _eventGenerator?.isRunning ?? false,
            onChanged: (_) => _toggleEventGeneration(),
            dense: true,
            contentPadding: EdgeInsets.zero,
          ),
          const SizedBox(height: 8),

          // Event Generation Speed Slider
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                "Generation Speed:",
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
              ),
              Row(
                children: [
                  const Text("0.5", style: TextStyle(fontSize: 12)),
                  Expanded(
                    child: Slider(
                      value: _eventGenerator?.eventsPerSecond ?? 1.0,
                      min: 0.5,
                      max: 10.0,
                      divisions: 19,
                      label:
                          "${_eventGenerator?.eventsPerSecond.toStringAsFixed(1) ?? '1.0'}/sec",
                      onChanged: (value) {
                        setState(() {
                          _eventGenerator?.setSpeed(value);
                        });
                      },
                    ),
                  ),
                  const Text("10", style: TextStyle(fontSize: 12)),
                ],
              ),
              Center(
                child: Text(
                  "${_eventGenerator?.eventsPerSecond.toStringAsFixed(1) ?? '1.0'} events/sec",
                  style: const TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ),
            ],
          ),

          const Divider(),

          // CONSUMER MODE
          const Text(
            "Consumer Mode:",
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
          ),
          const SizedBox(height: 8),

          // Observer Toggle
          SwitchListTile(
            title: const Text("Observer Active"),
            subtitle: const Text("Monitor events with production query"),
            value: _observerActive,
            onChanged: (_) => _toggleObserver(),
            dense: true,
            contentPadding: EdgeInsets.zero,
          ),
        ],
      ),
    ),
  );

  // CUSTOMER'S EXACT OBSERVER PATTERN - Using StreamBuilder with manual StreamController
  Widget get _eventObserver => StreamBuilder<Map<String, dynamic>>(
    stream: _eventStreamController?.stream,
    builder: (context, snapshot) {
      // Handle no data yet
      if (!snapshot.hasData) {
        return const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.event_note, size: 64, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                "No events yet",
                style: TextStyle(fontSize: 18, color: Colors.grey),
              ),
              SizedBox(height: 8),
              Text(
                "Enable 'Observe Events' and 'Generate Events' to create test events",
                style: TextStyle(color: Colors.grey),
              ),
            ],
          ),
        );
      }

      // Parse the single event from the stream
      final eventData = snapshot.data!;
      final event = Event.fromJson(eventData);

      // 🔍 DETAILED CLI LOGGING
      print('\n${'=' * 80}');
      print('📊 OBSERVER UPDATE - ${DateTime.now().toIso8601String()}');
      print('=' * 80);
      print('Event received from StreamController');

      // Show peer presence and transports
      final peers = _ditto?.presence.graph.remotePeers ?? [];
      print('Connected peers: ${peers.length}');
      for (var peer in peers.take(3)) {
        final connections = peer.connections;
        final peerId = peer.deviceName ?? peer.peerKeyString.substring(0, 8);
        print('  • Peer $peerId');
        for (var conn in connections) {
          final transport = conn.connectionType;
          print(
            '    → $transport ${conn.approximateDistanceInMeters != null ? "(~${conn.approximateDistanceInMeters}m)" : ""}',
          );
        }
      }

      // Log event with C++ detection
      final metadata = event.metadata;
      final deviceId = metadata?['deviceId'] ?? 'unknown';
      final sdkVersion = metadata?['sdkVersion'] ?? 'unknown';
      final isCpp = deviceId == 'cpp-client-1';

      print('\n${isCpp ? "🎯" : "📄"} Event${isCpp ? " [C++ CLIENT]" : ""}:');
      print('  • Court: ${event.courtId}');
      print('  • Device: $deviceId');
      print('  • SDK: $sdkVersion');
      print('  • Payload: ${event.payload != null ? "Yes (~3KB)" : "No"}');
      print('  • Timestamp: ${event.timestamp_us}');
      print('=' * 80 + '\n');

      // Calculate event size for verification
      final json = jsonEncode(event.toJson());
      final sizeKB = utf8.encode(json).length / 1024;
      final hasPayload = event.payload != null;

      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "Latest Event",
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  const Divider(),
                  _eventField("Event Type", event.eventType),
                  _eventField("Court ID", event.courtId),
                  _eventField("Timestamp (μs)", event.timestamp_us.toString()),
                  _eventField(
                    "Time",
                    DateTime.fromMicrosecondsSinceEpoch(
                      event.timestamp_us,
                    ).toString(),
                  ),
                  if (event.id != null) _eventField("Event ID", event.id!),
                  _eventField(
                    "Document Size",
                    "${sizeKB.toStringAsFixed(2)} KB ${hasPayload ? '(Large)' : '(Small)'}",
                  ),
                  _eventField("Has Payload", hasPayload ? "Yes" : "No"),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Connection Info
          Card(
            color: Colors.green.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "🌐 Connection Info",
                    style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    "Connected Peers: ${peers.length}",
                    style: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  if (peers.isEmpty)
                    const Text(
                      "No peers connected",
                      style: TextStyle(color: Colors.grey),
                    )
                  else
                    ...peers.take(3).map((peer) {
                      final peerId =
                          peer.deviceName ?? peer.peerKeyString.substring(0, 8);
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              "• Peer: $peerId",
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            ...peer.connections.map((conn) {
                              final distance = conn.approximateDistanceInMeters;
                              return Padding(
                                padding: const EdgeInsets.only(left: 16),
                                child: Text(
                                  "→ ${conn.connectionType}${distance != null ? " (~${distance}m)" : ""}",
                                  style: const TextStyle(fontSize: 12),
                                ),
                              );
                            }),
                          ],
                        ),
                      );
                    }),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          // Customer Pattern Info
          Card(
            color: Colors.orange.shade50,
            child: const Padding(
              padding: EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "⚠️ Customer Pattern Active",
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 8),
                  Text("Using manual StreamController + onChange callback"),
                  Text("• Iterates through ALL items in qr.items"),
                  Text("• Adds each item to StreamController"),
                  Text("• Shows only latest event (LIMIT 1)"),
                  Text("• This pattern may cause high CPU usage"),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
          Card(
            color: Colors.blue.shade50,
            child: const Padding(
              padding: EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "ℹ️ CPU Monitoring",
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 8),
                  Text(
                    "Use Android Studio Profiler or Xcode Instruments to measure CPU usage:",
                  ),
                  SizedBox(height: 4),
                  Text("• Baseline: ~5-10% (no events, no observer)"),
                  Text("• Events only: ~10-20%"),
                  Text("• Observer only: Expected ~50% (idle CPU issue)"),
                  Text("• Full load: Expected ~70% (active CPU issue)"),
                ],
              ),
            ),
          ),
        ],
      );
    },
  );

  Widget _eventField(String label, String value) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 4),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 140,
          child: Text(
            "$label:",
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
        Expanded(child: Text(value)),
      ],
    ),
  );

  @override
  void dispose() {
    // Clean up customer observer pattern resources
    _stopCustomerObserver();
    super.dispose();
  }
}
