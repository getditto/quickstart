// Regression test for the parameterized-DQL fix.
//
// Earlier versions of this file built UPDATE statements by interpolating
// user-typed task titles into DQL strings, e.g.:
//
//   "UPDATE tasks SET title = '${newTask.title}' where _id = '${task.id}'"
//
// A task titled `'); EVICT FROM tasks WHERE true; --` could break out of
// the quoted value and run arbitrary DQL. The fix switched to named
// parameter binding (`:title`, `:done`, `:id`) with values passed via the
// `arguments:` map.
//
// This test pins the fix as a static-source check: any future change that
// reintroduces Dart string interpolation into the DQL strings in
// `lib/tasks_repository.dart` fails here loudly. It does not exercise the DQL
// engine — a real round-trip test would need a live `Ditto` instance, which
// lives in `integration_test/`. The check is intentionally coarse and
// operates on the file as text.

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  group('tasks_repository.dart DQL safety', () {
    late String source;

    setUpAll(() {
      source = File('lib/tasks_repository.dart').readAsStringSync();
    });

    test('uses named parameter binding for every mutable field', () {
      expect(source, contains(':task'), reason: 'INSERT binding missing');
      expect(source, contains(':title'), reason: 'updateTitle binding missing');
      expect(source, contains(':done'), reason: 'setDone binding missing');
      expect(source, contains(':id'),
          reason: 'WHERE _id = :id binding missing');
    });

    test('no Dart string interpolation inside DQL execute() calls', () {
      // Find every `_ditto!.store.execute(` (or `.execute(`) call site and
      // inspect the string literal that follows up to the next `,` or `)`.
      // Forbid `${` and `$identifier` inside those literals.
      final calls = RegExp(r'\.execute\(\s*"([^"]*)"').allMatches(source);
      expect(
        calls.isNotEmpty,
        isTrue,
        reason: 'No execute() calls found — has tasks_repository.dart moved?',
      );

      final shortInterp = RegExp(r'\$[a-zA-Z_{][a-zA-Z0-9_]*');
      for (final m in calls) {
        final query = m.group(1)!;
        expect(
          query,
          isNot(contains(r'${')),
          reason: 'Found `\${` inside DQL: $query — use named binding.',
        );
        final hit = shortInterp.firstMatch(query);
        expect(
          hit,
          isNull,
          reason: hit == null
              ? ''
              : 'Found `${hit.group(0)}` (Dart interpolation) inside DQL: '
                  '$query — replace with `:name` and pass via `arguments:`.',
        );
      }
    });
  });
}
