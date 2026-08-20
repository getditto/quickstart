import 'package:ditto_live/ditto_live.dart';
import 'package:flutter_quickstart/dialog.dart';
import 'package:flutter_quickstart/ditto_manager.dart';
import 'package:flutter_quickstart/tasks_repository.dart';
import 'package:flutter_quickstart/task.dart';
import 'package:flutter/material.dart';
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
  final DittoManager _manager = DittoManager();
  TasksRepository? _repository;

  @override
  void initState() {
    super.initState();

    _init();
  }

  Future<void> _init() async {
    // The manager vends a configured, already-running Ditto instance; the
    // repository then registers the tasks subscription and observer against it.
    await _manager.open();

    final repository = TasksRepository(_manager.ditto);
    repository.start();

    if (mounted) {
      setState(() => _repository = repository);
    }
  }

  @override
  void dispose() {
    _repository?.dispose();
    super.dispose();
  }

  Future<void> _addTask() async {
    final task = await showAddTaskDialog(context);
    if (task == null) return;

    await _repository!.addTask(task.toJson());
  }

  @override
  Widget build(BuildContext context) {
    if (_repository == null) return _loading;

    return Scaffold(
      appBar: AppBar(
        title: const Text("Ditto Tasks"),
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
              const Text("Ensure your Database ID and Token are correct"),
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
        children: [
          Text("Database ID: ${_manager.databaseId}"),
          Text("Token: ${_manager.token}"),
        ],
      );

  Widget get _syncTile => SwitchListTile(
        title: const Text("Sync Active"),
        value: _manager.isSyncActive,
        onChanged: (value) {
          if (value) {
            setState(() => _manager.startSync());
          } else {
            setState(() => _manager.stopSync());
          }
        },
      );

  Widget get _tasksList => StreamBuilder<QueryResult>(
        stream: _repository!.changes,
        builder: (context, snapshot) {
          final result = snapshot.data;
          if (result == null) {
            return const Center(child: CircularProgressIndicator());
          }
          final tasks = result.items.map((r) => r.value).map(Task.fromJson);
          return ListView(children: tasks.map(_singleTask).toList());
        },
      );

  Widget _singleTask(Task task) => Dismissible(
        key: Key("${task.id}-${task.title}"),
        onDismissed: (direction) async {
          await _repository!.deleteTask(task.id!);

          if (mounted) {
            ScaffoldMessenger.of(
              context,
            ).showSnackBar(
                SnackBar(content: Text("Deleted Task ${task.title}")));
          }
        },
        background: _dismissibleBackground(true),
        secondaryBackground: _dismissibleBackground(false),
        child: CheckboxListTile(
          title: Text(task.title),
          value: task.done,
          onChanged: (value) => _repository!.setDone(task.id!, value ?? false),
          secondary: IconButton(
            icon: const Icon(Icons.edit),
            tooltip: "Edit Task",
            onPressed: () async {
              final newTask = await showAddTaskDialog(context, task);
              if (newTask == null) return;

              await _repository!.updateTitle(task.id!, newTask.title);
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
