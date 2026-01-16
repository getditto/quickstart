import 'dart:io' show Platform;

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_quickstart/dialog.dart';
import 'package:flutter_quickstart/dql_builder.dart';
import 'package:flutter_quickstart/task.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

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
  final databaseID =
      dotenv.env['DITTO_APP_ID'] ?? (throw Exception("env not found"));
  final token = dotenv.env['DITTO_PLAYGROUND_TOKEN'] ??
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
  /// 3. Creates a DittoConfig with the database ID and connect server URL
  /// 4. Opens a Ditto peer with the provided database ID and auth URL
  /// 5. Sets up an authentication expiration handler that authenticates the peer periodically
  /// 6. Starts sync and updates the app state with the configured Ditto instance
  Future<void> _initDitto() async {
    // Skip permissions in test mode - they block integration tests
    const isTestMode =
        bool.fromEnvironment('INTEGRATION_TEST_MODE', defaultValue: false);

    // Only request permissions on mobile platforms (Android/iOS)
    // Desktop platforms (macOS, Windows, Linux) don't require these permissions
    final isMobilePlatform = !kIsWeb && (Platform.isAndroid || Platform.isIOS);
    if (isMobilePlatform && !isTestMode) {
      await [
        Permission.bluetoothConnect,
        Permission.bluetoothAdvertise,
        Permission.nearbyWifiDevices,
        Permission.bluetoothScan
      ].request();
    }

    await Ditto.init();

    final config = DittoConfig(
      databaseID: databaseID,
      connect: DittoConfigConnectServer(
        url: authUrl!,
      ),
    );
    final ditto = await Ditto.open(config);

    ditto.auth.setExpirationHandler((ditto, secondsRemaining) {
      ditto.auth.login(
        token: token,
        // FIXME: Should be a çonstant (see SDKS-2594)
        provider: "__playgroundProvider",
      );
    });

    ditto.sync.start();

    if (mounted) {
      setState(() => _ditto = ditto);
    }
  }

  Future<void> _addTask() async {
    final task = await showAddTaskDialog(context);
    if (task == null) return;

    // https://docs.ditto.live/sdk/latest/crud/create
    await _ditto!.store.execute(
      "INSERT INTO tasks DOCUMENTS (:task)",
      arguments: {"task": task.toJson()},
    );
  }

  Future<void> _clearTasks() async {
    // https://docs.ditto.live/sdk/latest/crud/delete#evicting-data
    await _ditto!.store.execute("EVICT FROM tasks WHERE true");
  }

  @override
  Widget build(BuildContext context) {
    if (_ditto == null) return _loading;

    return Scaffold(
      appBar: AppBar(
        title: const Text("Ditto Tasks"),
        actions: [
          IconButton(
            icon: const Icon(Icons.clear),
            tooltip: "Clear",
            onPressed: _clearTasks,
          ),
        ],
      ),
      floatingActionButton: _fab,
      body: Column(
        children: [
          _portalInfo,
          _syncTile,
          const Divider(),
          Expanded(child: _tasksList),
        ],
      ),
    );
  }

  Widget get _loading => Scaffold(
        appBar: AppBar(title: const Text("Ditto Tasks")),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center, // Center vertically
            crossAxisAlignment:
                CrossAxisAlignment.center, // Center horizontally
            children: [
              const CircularProgressIndicator(),
              const Text("Ensure your AppID and Token are correct"),
              _portalInfo
            ],
          ),
        ),
      );

  Widget get _fab => FloatingActionButton(
        onPressed: _addTask,
        child: const Icon(Icons.add_task),
      );

  Widget get _portalInfo => Column(children: [
        Text("DatabaseID: $databaseID"),
        Text("Token: $token"),
      ]);

  Widget get _syncTile => SwitchListTile(
        title: const Text("Sync Active"),
        value: _ditto!.sync.isActive,
        onChanged: (value) {
          if (value) {
            setState(() => _ditto!.sync.start());
          } else {
            setState(() => _ditto!.sync.stop());
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

  Widget _singleTask(Task task) => Dismissible(
        key: Key("${task.id}-${task.title}"),
        onDismissed: (direction) async {
          // Use the Soft-Delete pattern
          // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
          await _ditto!.store.execute(
            "UPDATE tasks SET deleted = true WHERE _id = '${task.id}'",
          );

          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(content: Text("Deleted Task ${task.title}")),
            );
          }
        },
        background: _dismissibleBackground(true),
        secondaryBackground: _dismissibleBackground(false),
        child: CheckboxListTile(
          title: Text(task.title),
          value: task.done,
          onChanged: (value) => _ditto!.store.execute(
            "UPDATE tasks SET done = $value WHERE _id = '${task.id}'",
          ),
          secondary: IconButton(
            icon: const Icon(Icons.edit),
            tooltip: "Edit Task",
            onPressed: () async {
              final newTask = await showAddTaskDialog(context, task);
              if (newTask == null) return;

              // https://docs.ditto.live/sdk/latest/crud/update
              _ditto!.store.execute(
                "UPDATE tasks SET title = '${newTask.title}' where _id = '${task.id}'",
              );
            },
          ),
        ),
      );

  Widget _dismissibleBackground(bool primary) => Container(
        color: Colors.red,
        child: Align(
          alignment: primary ? Alignment.centerLeft : Alignment.centerRight,
          child: const Padding(
            padding: EdgeInsets.all(8.0),
            child: Icon(Icons.delete),
          ),
        ),
      );
}
