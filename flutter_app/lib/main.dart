import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_quickstart/data/repositories/tasks_repository.dart';
import 'package:flutter_quickstart/data/services/ditto_service.dart';
import 'package:flutter_quickstart/ui/features/tasks/view_models/tasks_view_model.dart';
import 'package:flutter_quickstart/ui/features/tasks/views/tasks_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env');

  try {
    final appId = _requireEnv('DITTO_APP_ID');
    final playgroundToken = _requireEnv('DITTO_PLAYGROUND_TOKEN');
    final websocketUrl = _requireEnv('DITTO_WEBSOCKET_URL');
    final authUrl = dotenv.env['DITTO_AUTH_URL'];

    final service = DittoService();
    await service.initialize(
      appId: appId,
      playgroundToken: playgroundToken,
      websocketUrl: websocketUrl,
      authUrl: authUrl,
      isTestMode: const bool.fromEnvironment(
        'INTEGRATION_TEST_MODE',
        defaultValue: false,
      ),
    );

    // Register the tasks subscription before starting sync so the very first
    // sync round-trip with the cloud includes it. Constructing the repository
    // registers the subscription as a side effect.
    final repository = TasksRepository(service);
    service.startSync();

    final viewModel = TasksViewModel(repository: repository, service: service);

    runApp(
      MaterialApp(
        home: TasksScreen(
          viewModel: viewModel,
          appId: appId,
          token: playgroundToken,
        ),
      ),
    );
  } catch (error, stack) {
    // Show a diagnosable error screen instead of letting the framework render
    // a red error widget over a black background. Mirrors the pre-refactor
    // "Ensure your AppID and Token are correct" loading screen, which stayed
    // visible forever on init failure.
    debugPrint('Ditto init failed: $error\n$stack');
    runApp(MaterialApp(home: _InitErrorScreen(error: error)));
  }
}

String _requireEnv(String key) =>
    dotenv.env[key] ?? (throw Exception('Missing env var: $key'));

class _InitErrorScreen extends StatelessWidget {
  const _InitErrorScreen({required this.error});

  final Object error;

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Ditto Tasks')),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Icon(Icons.error_outline, size: 64, color: Colors.red),
              const SizedBox(height: 16),
              const Text(
                'Ditto failed to initialize.',
                textAlign: TextAlign.center,
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              const Text(
                'Ensure your .env file is configured with DITTO_APP_ID, '
                'DITTO_PLAYGROUND_TOKEN, and DITTO_WEBSOCKET_URL.',
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              SelectableText(
                error.toString(),
                style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
              ),
            ],
          ),
        ),
      );
}
