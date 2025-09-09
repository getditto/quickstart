import 'dart:io';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:ditto_live/ditto_live.dart';

import 'task.dart';

Future<Task?> showAddTaskDialog(BuildContext context,
        [Task? task, Ditto? ditto]) =>
    showDialog<Task>(
      context: context,
      builder: (context) => _Dialog(task, ditto),
    );

Future<void> showErrorDialog(BuildContext context, String title, String message) =>
    showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) => AlertDialog(
        icon: const Icon(
          Icons.error_outline,
          color: Colors.red,
          size: 48,
        ),
        title: Text(
          title,
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        content: Text(message),
        actions: <Widget>[
          TextButton(
            child: const Text('OK'),
            onPressed: () {
              Navigator.of(context).pop();
            },
          ),
        ],
      ),
    );

class _Dialog extends StatefulWidget {
  final Task? taskToEdit;
  final Ditto? ditto;
  const _Dialog(this.taskToEdit, this.ditto);

  @override
  State<_Dialog> createState() => _DialogState();
}

class _DialogState extends State<_Dialog> {
  late final _name = TextEditingController(text: widget.taskToEdit?.title);
  late var _done = widget.taskToEdit?.done ?? false;
  final ImagePicker _picker = ImagePicker();
  XFile? _selectedImage;

  Future<Attachment?> _createAttachment() async {
    final newAttachment =
        await widget.ditto!.store.newAttachment(_selectedImage!.path);
    return newAttachment;
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        icon: const Icon(Icons.add_task),
        title: Text(widget.taskToEdit == null ? "Add Task" : "Edit Task"),
        contentPadding: EdgeInsets.zero,
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildImageSelector(),
              _textInput(_name, "Name"),
              _doneSwitch,
            ],
          ),
        ),
        actions: [
          TextButton(
            child: const Text("Cancel"),
            onPressed: () => Navigator.of(context).pop(),
          ),
          ElevatedButton(
            child: Text(widget.taskToEdit == null ? "Add Task" : "Edit Task"),
            onPressed: () async {
              final navigator = Navigator.of(context);
              try {
                final image = await _createAttachment();
                final task = Task(
                    title: _name.text,
                    done: _done,
                    deleted: false,
                    image: image!.toJson(),
                  );
                  if (mounted) {
                    navigator.pop(task);
                  }
              } catch (_) {
                if (mounted) {
                  showErrorDialog(context, "Error", "Failed to create task with attachment");
                }
                return;
              }
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

  Widget _buildImageSelector() {
    return GestureDetector(
      onTap: _pickImage,
      child: Container(
        height: 200,
        width: 200,
        margin: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.grey[200],
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey[400]!),
        ),
        child: _selectedImage != null
            ? ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Image.file(
                  File(_selectedImage!.path),
                  fit: BoxFit.cover,
                  width: double.infinity,
                ),
              )
            : Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    Icons.add_photo_alternate,
                    size: 48,
                    color: Colors.grey[600],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Tap to select an image',
                    style: TextStyle(color: Colors.grey[600]),
                  ),
                ],
              ),
      ),
    );
  }

  Future<void> _pickImage() async {
    final XFile? image = await _picker.pickImage(source: ImageSource.gallery);
    if (image != null) {
      setState(() {
        _selectedImage = image;
      });
    }
  }
}
