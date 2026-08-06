/**
 * Identity-mode selection based on env vars.
 *
 * Non-empty `DITTO_OFFLINE_LICENSE_TOKEN` (after trim) selects `'offline'`;
 * otherwise the app uses `'online'`.
 */
export type DittoMode = 'online' | 'offline';

export function selectMode(
  offlineLicenseToken: string | null | undefined,
): DittoMode {
  return offlineLicenseToken && offlineLicenseToken.trim().length > 0
    ? 'offline'
    : 'online';
}
