import 'dart:async';

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter_quickstart/dialog.dart';
import 'package:flutter_quickstart/dql_builder.dart';
import 'package:flutter_quickstart/task.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';

// These values are read from a .env file by passing `--dart-define-from-file=.env`
const appID = String.fromEnvironment('DITTO_APP_ID');
const token = String.fromEnvironment('DITTO_PLAYGROUND_TOKEN');
const authUrl = String.fromEnvironment('DITTO_AUTH_URL');
const websocketUrl = String.fromEnvironment('DITTO_WEBSOCKET_URL');

void main() => runApp(const MaterialApp(home: DittoExample()));

class DittoExample extends StatefulWidget {
  // used for testing
  final String? persistenceDirectory;
  const DittoExample({super.key, this.persistenceDirectory});

  @override
  State<DittoExample> createState() => DittoExampleState();
}

class DittoExampleState extends State<DittoExample> {
  Ditto? ditto;

  @override
  void initState() {
    super.initState();

    _initDitto();
  }

  /// Initializes the Ditto instance with necessary permissions and configuration.
  /// https://docs.ditto.live/sdk/latest/install-guides/flutter#step-3-import-and-initialize-the-ditto-sdk
  ///
  /// This function:
  /// 1. Requests required Bluetooth and WiFi permissions on mobile platforms
  /// 2. Initializes the Ditto SDK
  /// 3. Sets up online playground identity with the provided app ID and token
  /// 4. Enables peer-to-peer communication on non-web platforms
  /// 5. Configures WebSocket connection to Ditto cloud
  /// 6. Disables DQL strict mode
  /// 7. Starts sync and updates the app state with the configured Ditto instance
  Future<void> _initDitto() async {
    final platform = Ditto.currentPlatform;
    if (platform case SupportedPlatform.android || SupportedPlatform.ios) {
      await [
        Permission.bluetoothConnect,
        Permission.bluetoothAdvertise,
        Permission.nearbyWifiDevices,
        Permission.bluetoothScan
      ].request();
    }

    await Ditto.init();

    final identity = OnlinePlaygroundIdentity(
      appID: appID,
      token: token,
      // This is required to be set to false to use the correct URLs
      enableDittoCloudSync: false,
      customAuthUrl: authUrl,
    );

    final ditto = await Ditto.open(
      identity: identity,
      persistenceDirectory: widget.persistenceDirectory ?? "ditto",
    );

    ditto.updateTransportConfig((config) {
      // Note: this will not enable peer-to-peer sync on the web platform
      config.setAllPeerToPeerEnabled(true);
      config.connect.webSocketUrls.add(websocketUrl);
    });

    // Disable DQL strict mode
    // https://docs.ditto.live/dql/strict-mode
    await ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false");

    ditto.startSync();

    setState(() => this.ditto = ditto);
  }

  Future<void> _addTask() async {
    final task = await showAddTaskDialog(context);
    if (task == null) return;

    // https://docs.ditto.live/sdk/latest/crud/create
    await ditto!.store.execute(
      "INSERT INTO tasks DOCUMENTS (:task)",
      arguments: {"task": task.toJson()},
    );
  }

  Future<void> _clearTasks() async {
    // https://docs.ditto.live/sdk/latest/crud/delete#evicting-data
    await ditto!.store.execute("EVICT FROM tasks WHERE true");
  }

  @override
  Widget build(BuildContext context) {
    if (ditto == null) return _loading;

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
        body: Column(
          mainAxisAlignment: MainAxisAlignment.center, // Center vertically
          crossAxisAlignment: CrossAxisAlignment.center, // Center horizontally
          children: [
            const CircularProgressIndicator(),
            const Text("Ensure your AppID and Token are correct"),
            _portalInfo,
          ],
        ),
      );

  Widget get _fab => FloatingActionButton(
        onPressed: _addTask,
        child: const Icon(Icons.add_task),
      );

  Widget get _portalInfo => const Column(children: [
        Text("AppID: $appID"),
        Text("Token: $token"),
      ]);

  Widget get _syncTile => SwitchListTile(
        title: const Text("Sync Active"),
        value: ditto!.isSyncActive,
        onChanged: (value) {
          if (value) {
            setState(() => ditto!.startSync());
          } else {
            setState(() => ditto!.stopSync());
          }
        },
      );

  Widget get _tasksList => DqlBuilder(
        ditto: ditto!,
        query: "SELECT * FROM tasks WHERE deleted = false",
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
          await ditto!.store.execute(
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
          key: ValueKey(task),
          title: Text(task.title),
          value: task.done,
          onChanged: (value) => ditto!.store.execute(
            "UPDATE tasks SET done = $value WHERE _id = '${task.id}'",
          ),
          secondary: IconButton(
            icon: const Icon(Icons.edit),
            tooltip: "Edit Task",
            onPressed: () async {
              final newTask = await showAddTaskDialog(context, task);
              if (newTask == null) return;

              // https://docs.ditto.live/sdk/latest/crud/update
              ditto!.store.execute(
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
