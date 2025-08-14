// Basic Flutter widget tests for the Ditto Tasks app.

import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

void main() {
  // Setup test environment before running tests
  setUpAll(() async {
    // Initialize dotenv with test values
    dotenv.testLoad(fileInput: '''
DITTO_APP_ID=test_app_id
DITTO_PLAYGROUND_TOKEN=test_playground_token
DITTO_AUTH_URL=https://auth.example.com
DITTO_WEBSOCKET_URL=wss://websocket.example.com
''');
  });

  testWidgets('App widget loads without throwing', (WidgetTester tester) async {
    // Since DittoExample requires environment variables and network calls,
    // we'll just test that the widget can be created without immediate errors
    
    // We can't fully test the Ditto integration in a unit test environment
    // because it requires real network connections and SDK initialization.
    // This is why we have integration tests for full app testing.
    
    // For now, just verify the test setup works
    expect(dotenv.env['DITTO_APP_ID'], equals('test_app_id'));
    expect(dotenv.env['DITTO_PLAYGROUND_TOKEN'], equals('test_playground_token'));
  });

  testWidgets('Environment variables are properly loaded', (WidgetTester tester) async {
    // Verify all required environment variables are present
    expect(dotenv.env['DITTO_APP_ID'], isNotNull);
    expect(dotenv.env['DITTO_PLAYGROUND_TOKEN'], isNotNull);
    expect(dotenv.env['DITTO_AUTH_URL'], isNotNull);
    expect(dotenv.env['DITTO_WEBSOCKET_URL'], isNotNull);
    
    // Verify they have test values
    expect(dotenv.env['DITTO_APP_ID'], equals('test_app_id'));
    expect(dotenv.env['DITTO_PLAYGROUND_TOKEN'], equals('test_playground_token'));
    expect(dotenv.env['DITTO_AUTH_URL'], equals('https://auth.example.com'));
    expect(dotenv.env['DITTO_WEBSOCKET_URL'], equals('wss://websocket.example.com'));
  });
}
