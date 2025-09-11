import 'dart:async';
import 'dart:convert';
import 'dart:math';

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter/material.dart';
import 'package:flutter_quickstart/main.dart';
import 'package:flutter_quickstart/task.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart';
import 'package:meta/meta.dart';

final syncTile = find.ancestor(
  of: find.text("Sync Active"),
  matching: find.byType(SwitchListTile),
);
final appBar = find.ancestor(
  of: find.text("Ditto Tasks"),
  matching: find.byType(AppBar),
);
final openAddDialogButton = find.byType(FloatingActionButton);
final spinner = find.byType(CircularProgressIndicator);

final dialog = find.byType(Dialog);
final dialogNameField = find.byType(TextField);
final dialogDoneSwitch = find.ancestor(
  of: find.text("Done"),
  matching: find.byType(SwitchListTile),
);
final dialogAddButton = find.descendant(
  of: find.byType(Dialog),
  matching: find.byType(ElevatedButton),
);
FinderBase<Element> taskWithName(String name) => find.ancestor(
      of: find.text(name, skipOffstage: false),
      matching: find.byType(CheckboxListTile, skipOffstage: false),
    );

extension WidgetTesterExtension on WidgetTester {
  // General utilities
  Future<void> tapOn(
    FinderBase<Element> finder, {
    bool pumpAndSettle = true,
  }) async {
    await tap(finder);
    if (pumpAndSettle) await this.pumpAndSettle();
  }

  bool isVisible(FinderBase<Element> finder) => widgetList(finder).isNotEmpty;

  Future<void> waitUntil(
    FutureOr<bool> Function() predicate, {
    Duration timeout = const Duration(seconds: 60),
  }) async {
    final startedAt = DateTime.now();

    while (DateTime.now().difference(startedAt) < timeout) {
      if (await predicate()) return;
      await pump(const Duration(seconds: 1));
    }

    throw "Timed out";
  }

  Ditto? get ditto =>
      state<DittoExampleState>(find.byType(DittoExampleState)).ditto;

  // Sync tile
  SwitchListTile get _syncTile => firstWidget(syncTile);
  bool get isSyncing => _syncTile.value;
  Future<void> setSyncing(bool value) => tapOn(syncTile);

  // Loading
  bool get isLoading => isVisible(spinner);

  // Add dialog
  bool get addDialogVisible => isVisible(dialog);

  String get addDialogName => addDialogTextEditingController.text;
  TextEditingController get addDialogTextEditingController =>
      widget<TextField>(dialogNameField).controller!;

  bool get addDialogIsDone => addDialogDoneSwitch.value;
  SwitchListTile get addDialogDoneSwitch => widget(dialogDoneSwitch);

  Future<void> setDialogTaskName(String name) async {
    addDialogTextEditingController.clear();
    await enterText(dialogNameField, name);
    await pumpAndSettle();
  }

  Future<void> setDialogDone(bool done) async {
    final shouldToggle = addDialogIsDone != done;
    if (shouldToggle) {
      tapOn(dialogDoneSwitch);
    }
  }

  Future<void> addTask(
    String name, {
    bool done = false,
  }) async {
    await tapOn(openAddDialogButton);
    await setDialogTaskName(name);
    await setDialogDone(done);
    await tapOn(dialogAddButton);
  }

  // Task list

  List<Task> get allTasks =>
      widgetList<CheckboxListTile>(find.byType(CheckboxListTile))
          .map((tile) => (tile.key as ValueKey<Task>).value)
          .toList();

  Task task(String name) =>
      (widget<CheckboxListTile>(taskWithName(name)).key as ValueKey<Task>)
          .value;

  Future<void> setTaskDone({required String name, required bool done}) async {
    final shouldToggle = task(name).done != done;
    if (shouldToggle) {
      await tapOn(taskWithName(name));
    }
  }

  Future<void> deleteTask(String name) async {
    await fling(taskWithName(name), const Offset(500, 0), 5000);
    await pumpAndSettle();
  }

  Future<void> clearList() async {
    for (final task in allTasks) {
      await deleteTask(task.title);
    }
  }
}

@isTest
void testDitto(
  String description,
  Future<void> Function(WidgetTester tester) callback, {
  bool? skip,
}) =>
    testWidgets(
      skip: skip,
      description,
      (tester) async {
        final dir = "ditto_${Random().nextInt(1 << 32)}";
        await tester.pumpWidget(
          MaterialApp(home: DittoExample(persistenceDirectory: dir)),
        );
        await tester.waitUntil(() => !tester.isVisible(spinner));
        while (tester.allTasks.isNotEmpty) {
          await tester.clearList();
          // the fling finishes at the next event loop cycle which can cause
          // issues with the old ditto instance closing
          await tester.pump(const Duration(seconds: 1));
        }

        await callback(tester);

        await tester.pump(const Duration(seconds: 2));
      },
    );

Future<Map<String, dynamic>> bigPeerHttpExecute(
  String query, {
  Map<String, dynamic> arguments = const {},
}) async {
  const url = String.fromEnvironment("DITTO_CLOUD_ENDPOINT");
  final uri = Uri.parse("$url/store/execute");
  final response = await post(uri, body: {
    "statement": query,
    "args": arguments,
  });

  if (response.statusCode != 200) {
    throw "bad HTTP status: ${response.statusCode}";
  }

  return jsonDecode(response.body);
}
