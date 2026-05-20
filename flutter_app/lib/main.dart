import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_quickstart/data/repositories/tasks_repository.dart';
import 'package:flutter_quickstart/data/services/ditto_service.dart';
import 'package:flutter_quickstart/ui/features/tasks/view_models/tasks_view_model.dart';
import 'package:flutter_quickstart/ui/features/tasks/views/tasks_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env');

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
}

String _requireEnv(String key) =>
    dotenv.env[key] ?? (throw Exception('Missing env var: $key'));
