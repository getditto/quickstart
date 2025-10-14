import 'dart:io' show Platform;
import 'dart:async';

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_quickstart/dialog.dart';
import 'package:flutter_quickstart/dql_builder.dart';
import 'package:flutter_quickstart/task.dart';
import 'package:flutter_quickstart/auth_provider.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Test app to reproduce iOS force close segfault (SDKS-1652)
/// This version uses OnlineWithAuthentication identity instead of OnlinePlayground
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: ".env");
  runApp(const MaterialApp(home: DittoAuthExample()));
}

class DittoAuthExample extends StatefulWidget {
  const DittoAuthExample({super.key});

  @override
  State<DittoAuthExample> createState() => _DittoAuthExampleState();
}

class _DittoAuthExampleState extends State<DittoAuthExample>
    with WidgetsBindingObserver {
  Ditto? _ditto;
  TestAuthProvider? _authProvider;
  final List<String> _logs = [];
  bool _isDisposing = false;
  Timer? _forceCloseSimulationTimer;

  final appID =
      dotenv.env['DITTO_APP_ID'] ?? (throw Exception("env not found"));
  final authUrl = dotenv.env['DITTO_AUTH_URL'] ?? "https://auth.ditto.test";
  final websocketUrl =
      dotenv.env['DITTO_WEBSOCKET_URL'] ?? (throw Exception("env not found"));

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _addLog("App initialized - initState() called");
    _initDittoWithAuth();
  }

  @override
  void dispose() {
    _addLog("dispose() called - app is shutting down");
    _isDisposing = true;

    // Cancel any timers
    _forceCloseSimulationTimer?.cancel();

    // Clean up auth provider
    _authProvider?.dispose();

    // Try to close Ditto gracefully
    _closeDitto();

    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    _addLog("App lifecycle state changed: $state");

    switch (state) {
      case AppLifecycleState.resumed:
        _addLog("App resumed - in foreground");
        break;
      case AppLifecycleState.inactive:
        _addLog("App inactive - transitioning");
        break;
      case AppLifecycleState.paused:
        _addLog("App paused - in background");
        break;
      case AppLifecycleState.detached:
        _addLog("App detached - about to be terminated!");
        // This is when we should clean up to avoid the crash
        _handleAppDetached();
        break;
      case AppLifecycleState.hidden:
        _addLog("App hidden");
        break;
    }
  }

  void _addLog(String message) {
    final timestamp = DateTime.now().toIso8601String();
    final logEntry = "[$timestamp] $message";
    debugPrint(logEntry);

    if (mounted && !_isDisposing) {
      setState(() {
        _logs.insert(0, logEntry);
        // Keep only last 100 logs
        if (_logs.length > 100) {
          _logs.removeLast();
        }
      });
    }
  }

  Future<void> _handleAppDetached() async {
    _addLog("Handling app detached state - attempting graceful cleanup");

    try {
      // Dispose auth provider first
      _authProvider?.dispose();
      _authProvider = null;

      // Then close Ditto
      if (_ditto != null) {
        await _ditto!.close();
        _ditto = null;
      }

      _addLog("Graceful cleanup completed");
    } catch (e) {
      _addLog("Error during graceful cleanup: $e");
    }
  }

  Future<void> _initDittoWithAuth() async {
    try {
      _addLog("Starting Ditto initialization with OnlineWithAuthentication");

      const isTestMode =
          bool.fromEnvironment('INTEGRATION_TEST_MODE', defaultValue: false);
      final isMobilePlatform =
          !kIsWeb && (Platform.isAndroid || Platform.isIOS);

      if (isMobilePlatform && !isTestMode) {
        _addLog("Requesting permissions for mobile platform");
        await [
          Permission.bluetoothConnect,
          Permission.bluetoothAdvertise,
          Permission.nearbyWifiDevices,
          Permission.bluetoothScan
        ].request();
      }

      _addLog("Initializing Ditto SDK");
      await Ditto.init();

      // Create auth provider with logging
      _authProvider = TestAuthProvider(
        appId: appID,
        authUrl: authUrl,
        onLog: _addLog,
      );

      _addLog("Creating OnlineWithAuthentication identity");
      final identity = OnlineWithAuthenticationIdentity(
        appID: appID,
        authenticationHandler: _authProvider!.createAuthCallback(),
        enableDittoCloudSync: false,
        customAuthUrl: authUrl,
      );

      _addLog("Opening Ditto with auth identity");
      final ditto = await Ditto.open(identity: identity);

      _addLog("Configuring transport");
      ditto.updateTransportConfig((config) {
        config.setAllPeerToPeerEnabled(true);
        config.connect.webSocketUrls.add(websocketUrl);
      });

      _addLog("Disabling DQL strict mode");
      await ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false");

      _addLog("Starting sync");
      ditto.startSync();

      // Start auto-refresh to simulate ongoing auth activity
      _authProvider!.startAutoRefresh();

      if (mounted) {
        setState(() => _ditto = ditto);
      }

      _addLog("Ditto initialization completed successfully");
    } catch (e, stackTrace) {
      _addLog("Error initializing Ditto: $e");
      _addLog("Stack trace: $stackTrace");
    }
  }

  Future<void> _closeDitto() async {
    try {
      if (_ditto != null) {
        _addLog("Closing Ditto instance");
        await _ditto!.close();
        _ditto = null;
        _addLog("Ditto closed successfully");
      }
    } catch (e) {
      _addLog("Error closing Ditto: $e");
    }
  }

  /// Simulates Theory 1: Force close during active sync
  void _simulateTheory1() {
    _addLog("=== SIMULATING THEORY 1: Force close during active sync ===");
    _addLog("Instructions: Force close the app within 5 seconds!");

    // Create some sync activity
    Timer.periodic(const Duration(milliseconds: 500), (timer) {
      if (_ditto == null || _isDisposing) {
        timer.cancel();
        return;
      }

      _ditto!.store.execute(
        "INSERT INTO tasks DOCUMENTS (:task)",
        arguments: {"task": Task.random().toJson()},
      ).then((_) {
        _addLog("Created task during sync activity");
      });
    });
  }

  /// Simulates Theory 2: Double-free scenario
  Future<void> _simulateTheory2() async {
    _addLog("=== SIMULATING THEORY 2: Double-free scenario ===");
    _addLog("Closing Ditto explicitly, then force close immediately!");

    await _closeDitto();

    _addLog("Ditto closed - now force close the app quickly!");

    // The finalizer might still try to free the auth provider
  }

  /// Simulates Theory 3: Force close during auth callback
  void _simulateTheory3() {
    _addLog("=== SIMULATING THEORY 3: Force close during auth callback ===");
    _addLog("Triggering auth refresh, force close during the operation!");

    // Force a token refresh
    _authProvider?.refreshToken().then((_) {
      _addLog("Token refresh completed");
    }).catchError((e) {
      _addLog("Token refresh error: $e");
    });

    _addLog("Auth refresh in progress - force close NOW!");
  }

  Future<void> _addTask() async {
    final task = await showAddTaskDialog(context);
    if (task == null) return;

    await _ditto!.store.execute(
      "INSERT INTO tasks DOCUMENTS (:task)",
      arguments: {"task": task.toJson()},
    );
  }

  Future<void> _clearTasks() async {
    await _ditto!.store.execute("EVICT FROM tasks WHERE true");
  }

  @override
  Widget build(BuildContext context) {
    if (_ditto == null) return _loading;

    return Scaffold(
      appBar: AppBar(
        title: const Text("Ditto Auth Crash Test"),
        actions: [
          PopupMenuButton<String>(
            onSelected: (value) {
              switch (value) {
                case 'theory1':
                  _simulateTheory1();
                  break;
                case 'theory2':
                  _simulateTheory2();
                  break;
                case 'theory3':
                  _simulateTheory3();
                  break;
                case 'clear_logs':
                  setState(() => _logs.clear());
                  break;
              }
            },
            itemBuilder: (context) => [
              const PopupMenuItem(
                value: 'theory1',
                child: Text('Test Theory 1: Force Close During Sync'),
              ),
              const PopupMenuItem(
                value: 'theory2',
                child: Text('Test Theory 2: Double-Free'),
              ),
              const PopupMenuItem(
                value: 'theory3',
                child: Text('Test Theory 3: Force Close During Auth'),
              ),
              const PopupMenuDivider(),
              const PopupMenuItem(
                value: 'clear_logs',
                child: Text('Clear Logs'),
              ),
            ],
          ),
          IconButton(
            icon: const Icon(Icons.clear),
            tooltip: "Clear Tasks",
            onPressed: _clearTasks,
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: _addTask,
        child: const Icon(Icons.add_task),
      ),
      body: Column(
        children: [
          _authInfo,
          _syncTile,
          const Divider(),
          Expanded(
            child: DefaultTabController(
              length: 2,
              child: Column(
                children: [
                  const TabBar(
                    tabs: [
                      Tab(text: "Tasks"),
                      Tab(text: "Debug Logs"),
                    ],
                  ),
                  Expanded(
                    child: TabBarView(
                      children: [
                        _tasksList,
                        _logsView,
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget get _loading => Scaffold(
        appBar: AppBar(title: const Text("Ditto Auth Crash Test")),
        body: const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(),
              SizedBox(height: 16),
              Text("Initializing with OnlineWithAuthentication..."),
            ],
          ),
        ),
      );

  Widget get _authInfo => Container(
        padding: const EdgeInsets.all(8),
        color: Colors.blue.shade50,
        child: Column(
          children: [
            Text("AppID: $appID"),
            const Text("Identity: OnlineWithAuthentication"),
            Text("Auth URL: $authUrl"),
          ],
        ),
      );

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

  Widget get _tasksList => DqlBuilder(
        ditto: _ditto!,
        query: "SELECT * FROM tasks WHERE deleted = false ORDER BY title ASC",
        builder: (context, result) {
          final tasks = result.items.map((r) => r.value).map(Task.fromJson);
          return ListView(
            children: tasks.map(_singleTask).toList(),
          );
        },
      );

  Widget get _logsView => Container(
        color: Colors.grey.shade100,
        child: ListView.builder(
          itemCount: _logs.length,
          itemBuilder: (context, index) {
            final log = _logs[index];
            return Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              decoration: BoxDecoration(
                border: Border(
                  bottom: BorderSide(color: Colors.grey.shade300),
                ),
              ),
              child: Text(
                log,
                style: TextStyle(
                  fontFamily: 'monospace',
                  fontSize: 12,
                  color: log.contains('ERROR') || log.contains('Error')
                      ? Colors.red
                      : log.contains('AUTH_PROVIDER')
                          ? Colors.blue
                          : Colors.black87,
                ),
              ),
            );
          },
        ),
      );

  Widget _singleTask(Task task) => Dismissible(
        key: Key("${task.id}-${task.title}"),
        onDismissed: (direction) async {
          await _ditto!.store.execute(
            "UPDATE tasks SET deleted = true WHERE _id = '${task.id}'",
          );

          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text("Deleted Task ${task.title}")),
            );
          }
        },
        background: Container(
          color: Colors.red,
          child: const Align(
            alignment: Alignment.centerLeft,
            child: Padding(
              padding: EdgeInsets.all(8.0),
              child: Icon(Icons.delete, color: Colors.white),
            ),
          ),
        ),
        secondaryBackground: Container(
          color: Colors.red,
          child: const Align(
            alignment: Alignment.centerRight,
            child: Padding(
              padding: EdgeInsets.all(8.0),
              child: Icon(Icons.delete, color: Colors.white),
            ),
          ),
        ),
        child: CheckboxListTile(
          title: Text(task.title),
          value: task.done,
          onChanged: (value) => _ditto!.store.execute(
            "UPDATE tasks SET done = $value WHERE _id = '${task.id}'",
          ),
          secondary: IconButton(
            icon: const Icon(Icons.edit),
            onPressed: () async {
              final newTask = await showAddTaskDialog(context, task);
              if (newTask == null) return;

              _ditto!.store.execute(
                "UPDATE tasks SET title = '${newTask.title}' where _id = '${task.id}'",
              );
            },
          ),
        ),
      );
}
