import dotenv from 'dotenv';
import { resolve } from 'node:path';

dotenv.config({ path: resolve(process.cwd(), '..', '.env') });

export const env = {
  appId: process.env.DITTO_APP_ID ?? '',
  token: process.env.DITTO_PLAYGROUND_TOKEN ?? '',
  authUrl: process.env.DITTO_AUTH_URL ?? '',
  websocketUrl: process.env.DITTO_WEBSOCKET_URL ?? '',
};

export function assertEnv(): void {
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
