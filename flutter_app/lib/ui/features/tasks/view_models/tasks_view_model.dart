import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_quickstart/data/repositories/tasks_repository.dart';
import 'package:flutter_quickstart/data/services/ditto_service.dart';
import 'package:flutter_quickstart/domain/models/task.dart';

class TasksViewModel extends ChangeNotifier {
  TasksViewModel({
    required TasksRepository repository,
    required DittoService service,
  })  : _repository = repository,
        _service = service,
        _isSyncActive = service.isSyncActive {
    _subscription = _repository.watchTasks().listen(_onTasks);
  }

  final TasksRepository _repository;
  final DittoService _service;

  StreamSubscription<List<Task>>? _subscription;

  List<Task> _tasks = const [];
  List<Task> get tasks => _tasks;

  bool _isLoading = true;
  bool get isLoading => _isLoading;

  bool _isSyncActive;
  bool get isSyncActive => _isSyncActive;

  void _onTasks(List<Task> tasks) {
    _tasks = tasks;
    _isLoading = false;
    notifyListeners();
  }

  Future<void> addTask(Task task) => _repository.addTask(task);

  Future<void> setDone({required String id, required bool done}) =>
      _repository.setDone(id: id, done: done);

  Future<void> updateTitle({required String id, required String title}) =>
      _repository.updateTitle(id: id, title: title);

  Future<void> softDelete(String id) => _repository.softDelete(id);

  Future<void> clearAll() => _repository.evictAll();

  void toggleSync() {
    if (_isSyncActive) {
      _service.stopSync();
    } else {
      _service.startSync();
    }
    _isSyncActive = _service.isSyncActive;
    notifyListeners();
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }
}
