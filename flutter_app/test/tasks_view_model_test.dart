// Unit tests for TasksViewModel.
//
// Uses lightweight hand-written fakes that `implement` the production
// `TasksRepository` and `DittoService` classes. Dart's `implements` requires
// only the public surface, so the fakes can provide just the methods the
// ViewModel calls and throw `UnimplementedError` for the rest (Ditto SDK
// types never get touched in this file).

import 'dart:async';

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter_quickstart/data/repositories/tasks_repository.dart';
import 'package:flutter_quickstart/data/services/ditto_service.dart';
import 'package:flutter_quickstart/domain/models/task.dart';
import 'package:flutter_quickstart/ui/features/tasks/view_models/tasks_view_model.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('TasksViewModel', () {
    late _FakeRepository repository;
    late _FakeService service;
    late TasksViewModel vm;

    setUp(() {
      repository = _FakeRepository();
      service = _FakeService();
      vm = TasksViewModel(repository: repository, service: service);
    });

    tearDown(() {
      vm.dispose();
    });

    test('starts loading with empty task list', () {
      expect(vm.isLoading, isTrue);
      expect(vm.tasks, isEmpty);
    });

    test('flips out of loading on first emit and notifies listeners', () async {
      var notifyCount = 0;
      vm.addListener(() => notifyCount++);

      repository.emit([
        const Task(id: 't1', title: 'Buy milk', done: false, deleted: false),
      ]);
      await Future<void>.delayed(Duration.zero);

      expect(vm.isLoading, isFalse);
      expect(vm.tasks, hasLength(1));
      expect(vm.tasks.first.title, 'Buy milk');
      expect(notifyCount, 1);
    });

    test('subsequent emits update tasks and notify each time', () async {
      final notifications = <int>[];
      vm.addListener(() => notifications.add(vm.tasks.length));

      repository.emit([
        const Task(id: 't1', title: 'a', done: false, deleted: false),
      ]);
      await Future<void>.delayed(Duration.zero);
      repository.emit([
        const Task(id: 't1', title: 'a', done: false, deleted: false),
        const Task(id: 't2', title: 'b', done: true, deleted: false),
      ]);
      await Future<void>.delayed(Duration.zero);

      expect(notifications, [1, 2]);
    });

    test('isSyncActive reads live from service (not a stale snapshot)', () {
      service.syncActive = true;
      expect(vm.isSyncActive, isTrue);
      service.syncActive = false;
      expect(vm.isSyncActive, isFalse);
    });

    test('toggleSync calls stopSync when active', () {
      service.syncActive = true;
      vm.toggleSync();
      expect(service.stopCount, 1);
      expect(service.startCount, 0);
    });

    test('toggleSync calls startSync when inactive', () {
      service.syncActive = false;
      vm.toggleSync();
      expect(service.startCount, 1);
      expect(service.stopCount, 0);
    });

    test('toggleSync notifies listeners', () {
      var notifyCount = 0;
      vm.addListener(() => notifyCount++);
      vm.toggleSync();
      expect(notifyCount, 1);
    });

    test('addTask delegates to repository', () async {
      const task = Task(title: 'a', done: false, deleted: false);
      await vm.addTask(task);
      expect(repository.added, [task]);
    });

    test('setDone delegates to repository with named args', () async {
      await vm.setDone(id: 't1', done: true);
      expect(repository.doneCalls, [(id: 't1', done: true)]);
    });

    test('updateTitle delegates to repository with named args', () async {
      await vm.updateTitle(id: 't1', title: 'new title');
      expect(repository.titleCalls, [(id: 't1', title: 'new title')]);
    });

    test('softDelete delegates to repository', () async {
      await vm.softDelete('t1');
      expect(repository.softDeletes, ['t1']);
    });

    test('clearAll delegates to repository.evictAll', () async {
      await vm.clearAll();
      expect(repository.evictCount, 1);
    });

    test('dispose chains to repository.dispose', () {
      final localRepo = _FakeRepository();
      final localVm = TasksViewModel(
        repository: localRepo,
        service: _FakeService(),
      );
      expect(localRepo.disposed, isFalse);
      localVm.dispose();
      expect(localRepo.disposed, isTrue);
    });

    test('post-dispose emissions do not flow to listeners', () async {
      var notifyCount = 0;
      final localRepo = _FakeRepository();
      final localVm = TasksViewModel(
        repository: localRepo,
        service: _FakeService(),
      );
      localVm.addListener(() => notifyCount++);

      localRepo.emit(const [
        Task(id: 't1', title: 'before', done: false, deleted: false),
      ]);
      await Future<void>.delayed(Duration.zero);
      expect(notifyCount, 1);

      localVm.dispose();
      // The ChangeNotifier is disposed; any further emit shouldn't reach
      // the cancelled stream subscription.
      localRepo.emitAllowingPostDispose(const [
        Task(id: 't2', title: 'after', done: false, deleted: false),
      ]);
      await Future<void>.delayed(Duration.zero);
      expect(notifyCount, 1,
          reason: 'no further notifyListeners after dispose');
    });
  });
}

// ---------- fakes ----------

class _FakeRepository implements TasksRepository {
  final StreamController<List<Task>> _controller =
      StreamController<List<Task>>.broadcast();

  final List<Task> added = [];
  final List<({String id, bool done})> doneCalls = [];
  final List<({String id, String title})> titleCalls = [];
  final List<String> softDeletes = [];
  int evictCount = 0;
  bool disposed = false;

  void emit(List<Task> tasks) => _controller.add(tasks);

  /// Variant for the post-dispose test: bypasses the closed controller by
  /// allocating a one-shot controller. The new controller has no listeners
  /// (the VM cancelled its subscription on dispose), so this is a true
  /// no-op for the VM under test — exactly what we want to verify.
  void emitAllowingPostDispose(List<Task> tasks) {
    final replacement = StreamController<List<Task>>.broadcast()..add(tasks);
    replacement.close();
  }

  @override
  Stream<List<Task>> watchTasks() => _controller.stream;

  @override
  Future<void> addTask(Task task) async => added.add(task);

  @override
  Future<void> setDone({required String id, required bool done}) async =>
      doneCalls.add((id: id, done: done));

  @override
  Future<void> updateTitle({required String id, required String title}) async =>
      titleCalls.add((id: id, title: title));

  @override
  Future<void> softDelete(String id) async => softDeletes.add(id);

  @override
  Future<void> evictAll() async => evictCount++;

  @override
  void dispose() {
    disposed = true;
    _controller.close();
  }

  // Other public symbols on TasksRepository are not exercised by the
  // ViewModel; throw if any test path stumbles into them.
  @override
  dynamic noSuchMethod(Invocation invocation) => throw UnimplementedError(
        '_FakeRepository does not implement ${invocation.memberName}',
      );
}

class _FakeService implements DittoService {
  bool syncActive = false;
  int startCount = 0;
  int stopCount = 0;

  @override
  bool get isSyncActive => syncActive;

  @override
  void startSync() {
    startCount++;
    syncActive = true;
  }

  @override
  void stopSync() {
    stopCount++;
    syncActive = false;
  }

  @override
  bool get isInitialized => true;

  @override
  Ditto get ditto =>
      throw UnimplementedError('_FakeService.ditto not used in VM tests');

  @override
  Future<void> initialize({
    required String appId,
    required String playgroundToken,
    required String websocketUrl,
    String? authUrl,
    bool isTestMode = false,
  }) async =>
      throw UnimplementedError('_FakeService.initialize not used in VM tests');
}
