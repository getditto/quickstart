import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_attachment_test/dialog.dart';
import 'package:flutter_attachment_test/dql_builder.dart';
import 'package:flutter_attachment_test/task.dart';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'image_dialog.dart';

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
  // Map to track loading progress for each task by task ID
  final Map<String, double> _loadingProgress = {};
  final Map<String, Attachment?> _loadedAttachments = {};
  final appID =
      dotenv.env['DITTO_APP_ID'] ?? (throw Exception("env not found"));
  final playground_token =
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
  /// 1. Requests required Bluetooth and WiFi permissions on non-web platforms
  /// 2. Initializes the Ditto SDK
  /// 3. Sets up online playground identity with the provided app ID and token
  /// 4. Enables peer-to-peer communication on non-web platforms
  /// 5. Configures WebSocket connection to Ditto cloud
  /// 6. Disables DQL strict mode
  /// 7. Starts sync and updates the app state with the configured Ditto instance
  Future<void> _initDitto() async {
    if (!kIsWeb) {
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
      token: playground_token,
      enableDittoCloudSync:
          false, // This is required to be set to false to use the correct URLs
      customAuthUrl: authUrl,
    );

    final ditto = await Ditto.open(identity: identity);

    ditto.updateTransportConfig((config) {
      // Note: this will not enable peer-to-peer sync on the web platform
      config.setAllPeerToPeerEnabled(true);
      config.connect.webSocketUrls.add(websocketUrl);
    });

    // Disable DQL strict mode
    // https://docs.ditto.live/dql/strict-mode
    await ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false");

    ditto.startSync();

    setState(() => _ditto = ditto);
  }

  Future<void> _addTask() async {
    final task = await showAddTaskDialog(context, null, _ditto!);
    if (task == null) return;

    // https://docs.ditto.live/sdk/latest/crud/create
    await _ditto!.store.execute(
      "INSERT INTO tasks VALUES (:task)",
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
        crossAxisAlignment: CrossAxisAlignment.center, // Center horizontally
        children: [
          const CircularProgressIndicator(),
          const Text("Ensure your AppID and Token are correct"),
          _portalInfo,
        ],
      ),
    ),
  );

  Widget get _fab => FloatingActionButton(
    onPressed: _addTask,
    child: const Icon(Icons.add_task),
  );

  Widget get _portalInfo => Column(
    children: [Text("AppID: $appID"), Text("Token: $playground_token")],
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
    query: "SELECT * FROM tasks WHERE deleted = false",
    builder: (context, result) {
      final tasks = result.items.map((r) => r.value).map(Task.fromJson);
      return ListView(children: tasks.map(_singleTask).toList());
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
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text("Deleted Task ${task.title}")));
      }
    },
    background: _dismissibleBackground(true),
    secondaryBackground: _dismissibleBackground(false),
    child: Column(
      children: [
        Row(
          children: [
            Expanded(
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
                    final newTask = await showAddTaskDialog(
                      context,
                      task,
                      _ditto!,
                    );
                    if (newTask == null) return;

                    // https://docs.ditto.live/sdk/latest/crud/update
                    _ditto!.store.execute(
                      "UPDATE tasks SET title = '${newTask.title}' where _id = '${task.id}'",
                    );
                  },
                ),
              ),
            ),
            task.image != null
                ? SizedBox(
                    width: MediaQuery.of(context).size.width * 0.25,
                    child: Center(
                      child:
                          _loadingProgress.containsKey(task.id!) &&
                              _loadingProgress[task.id!]! < 1.0
                          ? Text(
                              'Loading: ${(_loadingProgress[task.id!]! * 100).toStringAsFixed(1)}%',
                              textAlign: TextAlign.center,
                            )
                          : ElevatedButton(
                              onPressed: () async {
                                if (_loadingProgress.containsKey(task.id!) &&
                                    _loadingProgress[task.id!]! == 1.0) {
                                  showImageDialog(
                                    context,
                                    await _loadedAttachments[task.id!]!.data,
                                  );
                                } else {
                                  _loadAttachment(task.image!, task.id!);
                                }
                              },
                              child: Center(
                                child: _loadingProgress.containsKey(task.id!)
                                    ? const Text('Show Image', textAlign: TextAlign.center)
                                    : const Text('Load Image', textAlign: TextAlign.center),
                              ),
                            ),
                    ),
                  )
                : const SizedBox.shrink(),
          ],
        ),
      ],
    ),
  );

  void _loadAttachment(Map<String, dynamic> token, String taskId) {
    setState(() {
      _loadingProgress[taskId] = 0.0;
    });

    _ditto!.store.fetchAttachment(token, (event) {
      switch (event) {
        case AttachmentFetchEventProgress progress:
          setState(() {
            _loadingProgress[taskId] =
                progress.totalBytes / progress.downloadedBytes;
          });
          break;
        case AttachmentFetchEventCompleted _:
          setState(() {
            _loadingProgress[taskId] = 1.0;
            _loadedAttachments[taskId] = event.attachment;
          });
          break;
        case AttachmentFetchEventDeleted _:
          setState(() {
            _loadingProgress.remove(taskId);
          });
          break;
        default:
          throw "unknown attachment fetch event type";
      }
    });
  }

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
