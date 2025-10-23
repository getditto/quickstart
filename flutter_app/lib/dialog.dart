import 'package:flutter/material.dart';

import 'task.dart';

Future<Task?> showAddTaskDialog(BuildContext context, [Task? task]) =>
    showDialog<Task>(
      context: context,
      builder: (context) => _Dialog(task),
    );

class _Dialog extends StatefulWidget {
  final Task? taskToEdit;
  const _Dialog(this.taskToEdit);

  @override
  State<_Dialog> createState() => _DialogState();
}

class _DialogState extends State<_Dialog> {
  late final _name = TextEditingController(text: widget.taskToEdit?.title);
  late final _attachmentNote = TextEditingController();
  late var _done = widget.taskToEdit?.done ?? false;

  @override
  Widget build(BuildContext context) => AlertDialog(
        icon: const Icon(Icons.add_task),
        title: Text(widget.taskToEdit == null ? "Add Task" : "Edit Task"),
        contentPadding: EdgeInsets.zero,
        content: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            _textInput(_name, "Name"),
            _textInput(_attachmentNote, "Attachment Note (optional)"),
            _doneSwitch,
          ],
        ),
        actions: [
          TextButton(
            child: const Text("Cancel"),
            onPressed: () => Navigator.of(context).pop(),
          ),
          ElevatedButton(
            child: Text(widget.taskToEdit == null ? "Add Task" : "Edit Task"),
            onPressed: () {
              final task = Task(
                title: _name.text,
                done: _done,
                deleted: false,
                // Pass the attachment note text if provided
                attachment: _attachmentNote.text.isNotEmpty ? {"note": _attachmentNote.text} : null,
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
