import dotenv from 'dotenv';
import { resolve } from 'node:path';

dotenv.config({ path: resolve(process.cwd(), '..', '.env') });

export type DittoMode = 'online' | 'offline';

export function selectMode(licenseToken: string | null | undefined): DittoMode {
  return licenseToken && licenseToken.trim().length > 0 ? 'offline' : 'online';
}

const offlineLicenseToken = (
  process.env.DITTO_OFFLINE_LICENSE_TOKEN ?? ''
).trim();

export const env = {
  appId: process.env.DITTO_APP_ID ?? '',
  token: process.env.DITTO_PLAYGROUND_TOKEN ?? '',
  authUrl: process.env.DITTO_AUTH_URL ?? '',
  websocketUrl: process.env.DITTO_WEBSOCKET_URL ?? '',
  offlineLicenseToken,
  mode: selectMode(offlineLicenseToken),
};

export function assertEnv(): void {
  if (env.mode === 'offline') {
    if (!env.appId) {
      throw new Error(
        `Offline mode requires DITTO_APP_ID. ` +
          `Set it in .env at the repo root.`,
      );
    }
    return;
  }
  const missing = (
    ['appId', 'token', 'authUrl', 'websocketUrl'] as const
  ).filter((k) => !env[k]);
  if (missing.length > 0) {
    throw new Error(
      `Missing required env vars: ${missing.join(', ')}. ` +
        `Copy .env.sample to .env at the repo root and fill in DITTO_APP_ID, ` +
        `DITTO_PLAYGROUND_TOKEN, DITTO_AUTH_URL, DITTO_WEBSOCKET_URL.`,
    );
  }
}
