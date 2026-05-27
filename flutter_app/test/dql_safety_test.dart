// Regression test for the parameterized-DQL fix.
//
// Pre-refactor, the Tasks app built UPDATE/DELETE statements by interpolating
// user-typed task titles into DQL strings, e.g.:
//
//   "UPDATE tasks SET title = '${newTask.title}' where _id = '${task.id}'"
//
// A task titled `'); EVICT FROM tasks WHERE true; --` could break out of the
// quoted value and run arbitrary DQL. The refactor moved all DQL into
// `TasksRepository` and switched to named parameter binding (`:title`,
// `:done`, `:id`, `:task`) with values passed via the `arguments:` map.
//
// This test pins the fix as a static-source check: any future change that
// reintroduces Dart string interpolation into the Repository source fails
// here loudly. It does not exercise the DQL engine — a real round-trip test
// would need a live `Ditto` instance, which lives in `integration_test/`.
// The check is intentionally coarse and operates on the file as text.

import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  group('TasksRepository DQL safety', () {
    late String source;

    setUpAll(() {
      source = File('lib/data/repositories/tasks_repository.dart')
          .readAsStringSync();
    });

    test('uses named parameter binding for every mutable field', () {
      // Every column the UI lets the user mutate must be passed via `:name`.
      expect(source, contains(':task'), reason: 'INSERT binding missing');
      expect(source, contains(':title'), reason: 'updateTitle binding missing');
      expect(source, contains(':done'), reason: 'setDone binding missing');
      expect(source, contains(':id'),
          reason: 'WHERE _id = :id binding missing');
    });

    test('contains no Dart string interpolation', () {
      // Forbid `${expr}` style. Use raw-string literal so the matcher's own
      // source isn't interpolated.
      expect(
        source,
        isNot(contains(r'${')),
        reason: r'Found `${` in Repository — DQL must use named bindings, '
            'not Dart interpolation. See header comment in this test file '
            'for the injection scenario this guards against.',
      );

      // Forbid `$identifier` style. Matches `$foo`, `$foo123`, `$_bar`.
      final shortInterp = RegExp(r'\$[a-zA-Z_][a-zA-Z0-9_]*');
      final match = shortInterp.firstMatch(source);
      expect(
        match,
        isNull,
        reason: match == null
            ? ''
            : 'Found Dart short-form interpolation `${match.group(0)}` in '
                'Repository at offset ${match.start}. Replace with a named '
                'binding (`:name`) and pass the value via the `arguments:` map.',
      );
    });

    test('every execute() call passes an arguments: map', () {
      // Coarse check: each `execute(` call should be followed (within ~200
      // chars) by either `arguments:` OR be the no-args EVICT/static SELECT
      // pattern. This catches the regression where someone calls
      // `execute('UPDATE ... WHERE _id = ' + id)` without binding.
      final executeCallSites = RegExp(r'\.execute\(').allMatches(source);
      expect(
        executeCallSites.isNotEmpty,
        isTrue,
        reason: 'No execute() calls found — has the Repository moved?',
      );

      for (final m in executeCallSites) {
        final window = source.substring(
          m.start,
          (m.start + 250).clamp(0, source.length),
        );
        final hasArguments = window.contains('arguments:');
        final isStaticOnly = window.contains("'EVICT FROM tasks WHERE true'");
        expect(
          hasArguments || isStaticOnly,
          isTrue,
          reason: 'execute() call at offset ${m.start} has no `arguments:` '
              'map and is not the static EVICT statement. If user input '
              'reaches this query, parameter-bind it.',
        );
      }
    });
  });
}
