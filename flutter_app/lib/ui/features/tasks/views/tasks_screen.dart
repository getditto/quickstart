import 'package:flutter/material.dart';
import 'package:flutter_quickstart/domain/models/task.dart';
import 'package:flutter_quickstart/ui/core/widgets/task_dialog.dart';
import 'package:flutter_quickstart/ui/features/tasks/view_models/tasks_view_model.dart';

class TasksScreen extends StatelessWidget {
  const TasksScreen({
    super.key,
    required this.viewModel,
    required this.appId,
    required this.token,
  });

  final TasksViewModel viewModel;
  final String appId;
  final String token;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: viewModel,
      builder: (context, _) {
        if (viewModel.isLoading) {
          return Scaffold(
            appBar: AppBar(title: const Text('Ditto Tasks')),
            body: Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const CircularProgressIndicator(),
                  const Text('Ensure your AppID and Token are correct'),
                  _PortalInfo(appId: appId, token: token),
                ],
              ),
            ),
          );
        }

        return Scaffold(
          appBar: AppBar(
            title: const Text('Ditto Tasks'),
            actions: [
              IconButton(
                icon: const Icon(Icons.clear),
                tooltip: 'Clear',
                onPressed: viewModel.clearAll,
              ),
            ],
          ),
          floatingActionButton: FloatingActionButton(
            onPressed: () => _onAddPressed(context),
            child: const Icon(Icons.add_task),
          ),
          body: Column(
            children: [
              _PortalInfo(appId: appId, token: token),
              SwitchListTile(
                title: const Text('Sync Active'),
                value: viewModel.isSyncActive,
                onChanged: (_) => viewModel.toggleSync(),
              ),
              const Divider(),
              Expanded(
                child: ListView(
                  children: viewModel.tasks
                      .map((t) => _TaskTile(task: t, viewModel: viewModel))
                      .toList(),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Future<void> _onAddPressed(BuildContext context) async {
    final task = await showAddTaskDialog(context);
    if (task == null) return;
    await viewModel.addTask(task);
  }
}

class _PortalInfo extends StatelessWidget {
  const _PortalInfo({required this.appId, required this.token});

  final String appId;
  final String token;

  @override
  Widget build(BuildContext context) =>
      Column(children: [Text('AppID: $appId'), Text('Token: $token')]);
}

class _TaskTile extends StatelessWidget {
  const _TaskTile({required this.task, required this.viewModel});

  final Task task;
  final TasksViewModel viewModel;

  @override
  Widget build(BuildContext context) => Dismissible(
        key: Key('${task.id}-${task.title}'),
        onDismissed: (_) async {
          if (task.id == null) return;
          await viewModel.softDelete(task.id!);
          if (context.mounted) {
            ScaffoldMessenger.of(
              context,
            ).showSnackBar(
                SnackBar(content: Text('Deleted Task ${task.title}')));
          }
        },
        background: const _DismissBackground(primary: true),
        secondaryBackground: const _DismissBackground(primary: false),
        child: CheckboxListTile(
          title: Text(task.title),
          value: task.done,
          onChanged: (value) {
            if (value == null || task.id == null) return;
            viewModel.setDone(id: task.id!, done: value);
          },
          secondary: IconButton(
            icon: const Icon(Icons.edit),
            tooltip: 'Edit Task',
            onPressed: () => _onEditPressed(context),
          ),
        ),
      );

  Future<void> _onEditPressed(BuildContext context) async {
    final updated = await showAddTaskDialog(context, task);
    if (updated == null || task.id == null) return;
    await viewModel.updateTitle(id: task.id!, title: updated.title);
  }
}

class _DismissBackground extends StatelessWidget {
  const _DismissBackground({required this.primary});

  final bool primary;

  @override
  Widget build(BuildContext context) => Container(
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
