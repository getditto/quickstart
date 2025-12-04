import 'dart:async';
import 'dart:convert';
import 'package:ditto_live/ditto_live.dart';
import '../models/event.dart';

enum EventSize { small, large }

class EventGenerator {
  final Ditto ditto;
  Timer? _timer;
  int _eventCount = 0;
  bool _isRunning = false;
  EventSize _eventSize = EventSize.small;
  double _eventsPerSecond = 1.0; // Default: 1 event/sec

  EventGenerator(this.ditto);

  Future<void> start() async {
    if (_isRunning) return;

    await _createIndices();
    _isRunning = true;
    _startTimer();
  }

  void _startTimer() {
    final intervalMs = (1000 / _eventsPerSecond).round();
    _timer = Timer.periodic(
      Duration(milliseconds: intervalMs),
      (_) => _generateEvent(large: _eventSize == EventSize.large)
    );
  }

  Future<void> _createIndices() async {
    // Create indices matching the original bug report
    await ditto.store.execute(
      "CREATE INDEX IF NOT EXISTS event_courtid_index ON events (courtId)"
    );
    await ditto.store.execute(
      "CREATE INDEX IF NOT EXISTS event_timestamp_index ON events (timestamp_us)"
    );
  }

  Future<void> _generateEvent({bool large = false}) async {
    try {
      final event = Event(
        courtId: 'tour-5',
        timestamp_us: DateTime.now().microsecondsSinceEpoch,
        eventType: 'game_event',
        deleted: false,
        payload: large ? _generateLargePayload() : null,
        metadata: large ? _generateMetadata() : null,
      );

      await ditto.store.execute(
        "INSERT INTO events DOCUMENTS (:event)",
        arguments: {"event": event.toJson()},
      );
      _eventCount++;

      // Verify event size if large
      if (large && _eventCount == 1) {
        final json = jsonEncode(event.toJson());
        final sizeKB = utf8.encode(json).length / 1024;
        print('📏 Event size: ${sizeKB.toStringAsFixed(2)} KB (target: ~3.3KB)');
      }

      print('✅ Event created: $_eventCount (timestamp: ${event.timestamp_us})');
    } catch (e) {
      print('❌ Error generating event: $e');
    }
  }

  String _generateLargePayload() {
    // Generate realistic tennis match data to reach ~3KB
    final data = {
      'points': List.generate(50, (i) => {
        'number': i,
        'score': '${(i ~/ 2)} - ${i - (i ~/ 2)}',
        'timestamp': DateTime.now().millisecondsSinceEpoch - (i * 1000),
        'server': i % 2 == 0 ? 'player1' : 'player2',
        'winner': i % 3 == 0 ? 'player1' : 'player2',
      }),
      'players': {
        'player1': {
          'name': 'Player One',
          'ranking': 1,
          'country': 'USA',
          'stats': {
            'aces': 12,
            'doubleFaults': 3,
            'firstServePercentage': 65.5,
            'winOnFirstServe': 78.2,
          },
        },
        'player2': {
          'name': 'Player Two',
          'ranking': 2,
          'country': 'ESP',
          'stats': {
            'aces': 10,
            'doubleFaults': 2,
            'firstServePercentage': 68.3,
            'winOnFirstServe': 75.8,
          },
        },
      },
      'ballTracking': List.generate(100, (i) => {
        'x': i * 0.5,
        'y': (i % 10) * 1.2,
        'z': 0.3,
        'velocity': 120.5 + (i % 20),
        'spin': i * 10,
        'timestamp': i * 10,
      }),
      'notes': List.generate(20, (i) =>
        'Event annotation number $i with additional descriptive text to increase document size. '
        'This text is repeated to pad the document to approximately 3.3 kilobytes as required for production testing. '
        'Additional metadata and context can be included here for realistic data representation.'
      ),
    };

    return jsonEncode(data);
  }

  Map<String, dynamic> _generateMetadata() {
    return {
      'deviceId': 'flutter-test-device',
      'appVersion': '1.0.0',
      'sdkVersion': '4.13.0',
      'generatedAt': DateTime.now().toIso8601String(),
      'courtInfo': {
        'surface': 'hard',
        'indoor': false,
        'temperature': 22.5,
        'humidity': 45.2,
      },
    };
  }

  void stop() {
    _timer?.cancel();
    _timer = null;
    _isRunning = false;
  }

  bool get isRunning => _isRunning;
  int get eventCount => _eventCount;

  Future<void> clearEvents() async {
    try {
      await ditto.store.execute("EVICT FROM events WHERE true");
      _eventCount = 0;
    } catch (e) {
      print('Error clearing events: $e');
    }
  }

  // NEW: Bulk generation method
  Future<void> generateBulk(int count, {bool large = true}) async {
    print('🚀 Generating $count ${large ? "large (3.3KB)" : "small (~100B)"} events...');

    await _createIndices();
    final startTime = DateTime.now();

    for (int i = 0; i < count; i++) {
      await _generateEvent(large: large);

      if ((i + 1) % 100 == 0) {
        final elapsed = DateTime.now().difference(startTime).inSeconds;
        print('📊 Progress: ${i + 1}/$count events (${elapsed}s elapsed)');
      }
    }

    final totalTime = DateTime.now().difference(startTime);
    print('✅ Generated $count events in ${totalTime.inSeconds}s (${(count / totalTime.inSeconds).toStringAsFixed(1)} events/sec)');
  }

  // NEW: Get total event count
  Future<int> getEventCount({String? courtId}) async {
    try {
      final query = courtId != null
          ? "SELECT COUNT(*) as count FROM events WHERE courtId = :courtId"
          : "SELECT COUNT(*) as count FROM events";

      final result = await ditto.store.execute(
        query,
        arguments: courtId != null ? {"courtId": courtId} : {},
      );

      if (result.items.isEmpty) return 0;
      return result.items.first.value['count'] as int;
    } catch (e) {
      print('❌ Error getting event count: $e');
      return 0;
    }
  }

  // NEW: Set event size mode
  void setEventSize(EventSize size) {
    _eventSize = size;
  }

  EventSize get eventSize => _eventSize;

  // NEW: Set generation speed (events per second)
  void setSpeed(double eventsPerSecond) {
    _eventsPerSecond = eventsPerSecond;

    // Restart timer with new interval if currently running
    if (_isRunning) {
      _timer?.cancel();
      _startTimer();
    }
  }

  double get eventsPerSecond => _eventsPerSecond;

  void dispose() {
    stop();
  }
}
