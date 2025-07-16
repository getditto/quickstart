// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:ditto_live/ditto_live.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:flutter_quickstart/main.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('simple ditto linux test', (tester) async {
    await tester.runAsync(() async {
      await Ditto.init();

      final ditto = await Ditto.open(
        identity: OnlinePlaygroundIdentity(appID: appID, token: token),
      );

      await ditto.store.execute(
        "INSERT INTO foo DOCUMENTS (:doc)",
        arguments: {
          "doc": {"foo": "bar"},
        },
      );

      final result = await ditto.store.execute("SELECT * FROM foo");
      expect(result.items, hasLength(1));
    });
  });
}
