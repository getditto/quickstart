import 'package:flutter/material.dart';

import 'task.dart';

Future<Task?> showAddTaskDialog(BuildContext context) => showDialog<Task>(
      context: context,
      builder: (context) => const _Dialog(),
    );

class _Dialog extends StatefulWidget {
  const _Dialog();

  @override
  State<_Dialog> createState() => _DialogState();
}

class _DialogState extends State<_Dialog> {
  final _name = TextEditingController();
  final _description = TextEditingController();
  var _done = false;

  @override
  Widget build(BuildContext context) => AlertDialog(
        icon: const Icon(Icons.add_task),
        title: const Text("Add Task"),
        contentPadding: EdgeInsets.zero,
        content: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            _textInput(_name, "Name"),
            _textInput(_description, "Description"),
            _doneSwitch,
          ],
        ),
        actions: [
          TextButton(
            child: const Text("Cancel"),
            onPressed: () => Navigator.of(context).pop(),
          ),
          ElevatedButton(
            child: const Text("Add Task"),
            onPressed: () {
              final task = Task(
                title: _name.text,
                description: _description.text,
                done: _done,
                deleted: false,
              );
              Navigator.of(context).pop(task);
            },
          ),
        ],
      );

  Widget _textInput(TextEditingController controller, String label) => ListTile(
        title: TextField(
          controller: controller,
          decoration: InputDecoration(
            labelText: label,
          ),
        ),
      );

  Widget get _doneSwitch => SwitchListTile(
        title: const Text("Done"),
        value: _done,
        onChanged: (value) => setState(() => _done = value),
      );
}
