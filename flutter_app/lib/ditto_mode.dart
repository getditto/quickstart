/// Identity-mode selection based on env vars.
///
/// Non-empty `DITTO_OFFLINE_LICENSE_TOKEN` (after trim) selects
/// [DittoMode.offline]; otherwise the app uses [DittoMode.onlinePlayground].
enum DittoMode { onlinePlayground, offline }

DittoMode selectDittoMode(String? offlineLicenseToken) {
  final token = offlineLicenseToken?.trim() ?? '';
  return token.isEmpty ? DittoMode.onlinePlayground : DittoMode.offline;
}
