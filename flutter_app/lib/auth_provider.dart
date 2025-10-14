import 'dart:async';
import 'package:ditto_live/ditto_live.dart';

/// Test auth provider to replicate the SDKS-1652 crash scenario
///
/// This simulates the customer's authentication setup where:
/// 1. Auth tokens are retrieved from a backend
/// 2. The provider holds a reference to the login provider
/// 3. Force-closing the app causes a segfault in ditto_auth_login_provider_free
class TestAuthProvider {
  final String appId;
  final String authUrl;
  final void Function(String) onLog;

  String? _cachedToken;
  Timer? _refreshTimer;
  bool _isDisposed = false;

  TestAuthProvider({
    required this.appId,
    required this.authUrl,
    required this.onLog,
  });

  /// Simulates fetching JWT from backend (like the customer's OpsRepo.dittoLogin)
  Future<String> _fetchTokenFromBackend() async {
    onLog("AUTH_PROVIDER: Fetching token from backend");

    // Simulate network delay
    await Future.delayed(const Duration(milliseconds: 500));

    // Generate a mock JWT (in real app, this would come from backend)
    final now = DateTime.now().millisecondsSinceEpoch ~/ 1000;
    final exp = now + 3600; // 1 hour from now

    // Mock JWT structure (not a real signed JWT, just for simulation)
    final mockJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0X3VzZXIiLCJhcHBJZCI6IiRhcHBJZCIsImV4cCI6JGV4cH0.mock_signature";

    onLog("AUTH_PROVIDER: Token fetched successfully");
    return mockJwt;
  }

  /// Creates the authentication callback for Ditto
  /// This is where the potential crash occurs - the AuthenticationHandler
  /// holds references that get freed during app termination
  AuthenticationHandler createAuthCallback() {
    onLog("AUTH_PROVIDER: Creating authentication handler");

    return AuthenticationHandler(
      authenticationRequired: (authenticator) async {
        onLog("AUTH_PROVIDER: Authentication required - fetching token");
        return await _authenticate(authenticator);
      },
      authenticationExpiringSoon: (authenticator, secondsRemaining) async {
        onLog("AUTH_PROVIDER: Token expiring in $secondsRemaining seconds - refreshing");
        return await _authenticate(authenticator);
      },
    );
  }

  /// Performs the actual authentication
  Future<void> _authenticate(Authenticator authenticator) async {
    if (_isDisposed) {
      onLog("AUTH_PROVIDER: Skipping auth - provider disposed");
      return;
    }

    try {
      // Get token from backend (or use cached)
      _cachedToken ??= await _fetchTokenFromBackend();

      if (_cachedToken == null) {
        onLog("AUTH_PROVIDER ERROR: No token available");
        return;
      }

      onLog("AUTH_PROVIDER: Calling authenticator.login()");

      // This is the critical call that creates the login provider in native code
      // The crash occurs when this provider is freed during force-close
      await authenticator.login(
        token: _cachedToken!,
        provider: "AuthCallback",
      );

      onLog("AUTH_PROVIDER: Login successful");
    } catch (e) {
      onLog("AUTH_PROVIDER ERROR: Authentication failed - $e");
      rethrow;
    }
  }

  /// Manually refresh the token
  Future<void> refreshToken() async {
    onLog("AUTH_PROVIDER: Manual token refresh requested");
    _cachedToken = await _fetchTokenFromBackend();
  }

  /// Start periodic token refresh (simulates keeping auth active)
  void startAutoRefresh() {
    onLog("AUTH_PROVIDER: Starting auto-refresh (every 30s)");

    _refreshTimer = Timer.periodic(const Duration(seconds: 30), (timer) {
      if (_isDisposed) {
        timer.cancel();
        return;
      }

      refreshToken().catchError((e) {
        onLog("AUTH_PROVIDER ERROR: Auto-refresh failed - $e");
      });
    });
  }

  /// Cleanup the provider
  ///
  /// The crash scenario:
  /// 1. User force-closes the app
  /// 2. Flutter's shutdown sequence calls finalizers
  /// 3. The AuthenticationHandler's native login provider tries to free
  /// 4. But it may have already been freed or is in an invalid state
  /// 5. Result: Segfault in ditto_auth_login_provider_free
  void dispose() {
    if (_isDisposed) return;

    onLog("AUTH_PROVIDER: Disposing - cleaning up");
    _isDisposed = true;

    _refreshTimer?.cancel();
    _refreshTimer = null;
    _cachedToken = null;

    onLog("AUTH_PROVIDER: Disposed successfully");
  }
}
