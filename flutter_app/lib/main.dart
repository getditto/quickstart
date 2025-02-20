import 'dart:io';

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_quickstart/dialog.dart';
import 'package:flutter_quickstart/dql_builder.dart';
import 'package:flutter_quickstart/task.dart';
import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:permission_handler/permission_handler.dart';

const appID = "<replace with your app ID>";
const token = "<replace with your playground token>";

Future<void> main() async {
  runApp(const MaterialApp(home: DittoExample()));
}

class DittoExample extends StatefulWidget {
  const DittoExample({super.key});

  @override
  State<DittoExample> createState() => _DittoExampleState();
}

class _DittoExampleState extends State<DittoExample> {
  Ditto? _ditto;

  @override
  void initState() {
    super.initState();

    _initDitto();
  }

  /// Initializes the Ditto instance with necessary permissions and configuration.
  /// https://docs.ditto.live/sdk/latest/install-guides/flutter#step-3-import-and-initialize-the-ditto-sdk
  ///
  /// This function:
  /// 1. Requests required Bluetooth and WiFi permissions on non-web platforms
  /// 2. Initializes the Ditto SDK
  /// 3. Sets up online playground identity with the provided app ID and token
  /// 4. Creates and configures persistence directory for local data storage
  /// 5. Enables peer-to-peer communication on non-web platforms
  /// 6. Configures WebSocket connection to Ditto cloud
  /// 7. Starts sync and updates the app state with the configured Ditto instance
  Future<void> _initDitto() async {
    if (!kIsWeb) {
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
      enableDittoCloudSync: false,
    );

    final documentsDir = await getApplicationDocumentsDirectory();
    final persistenceDirectory = Directory("${documentsDir.path}/ditto");
    await persistenceDirectory.create();

    final ditto = await Ditto.open(
      identity: identity,
      persistenceDirectory: persistenceDirectory.path,
    );

    ditto.updateTransportConfig((config) {
      if (!kIsWeb) {
        config.setAllPeerToPeerEnabled(true);
      }
      config.connect.webSocketUrls.add(
        "wss://$appID.cloud.ditto.live",
      );
    });

    ditto.startSync();

    setState(() => _ditto = ditto);
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

  Widget get _portalInfo => const Column(children: [
        Text("AppID: $appID"),
        Text("Token: $token"),
      ]);

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
