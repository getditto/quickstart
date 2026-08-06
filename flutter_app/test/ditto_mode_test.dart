import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_quickstart/ditto_mode.dart';

void main() {
  group('selectDittoMode', () {
    test('null token selects online', () {
      expect(selectDittoMode(null), DittoMode.onlinePlayground);
    });

    test('empty token selects online', () {
      expect(selectDittoMode(''), DittoMode.onlinePlayground);
    });

    test('whitespace-only token selects online', () {
      expect(selectDittoMode('   \t\n  '), DittoMode.onlinePlayground);
    });

    test('non-empty token selects offline', () {
      expect(selectDittoMode('any-real-license-token'), DittoMode.offline);
    });
  });
}
