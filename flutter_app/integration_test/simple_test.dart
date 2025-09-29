import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_quickstart/main.dart' as app;

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testWidgets('Simple smoke test', (WidgetTester tester) async {
    await app.main();
    await tester.pumpAndSettle();

    // Just verify the app starts
    expect(find.text('Ditto Tasks'), findsOneWidget);
  });
}
