import dotenv from 'dotenv';
import { resolve } from 'node:path';

dotenv.config({ path: resolve(process.cwd(), '..', '.env') });

export const env = {
  databaseId: process.env.DITTO_DATABASE_ID ?? '',
  token: process.env.DITTO_DEVELOPMENT_TOKEN ?? '',
  serverUrl: process.env.DITTO_SERVER_URL ?? '',
  offlineLicenseToken: (
    process.env.DITTO_OFFLINE_LICENSE_TOKEN ?? ''
  ).trim(),
};

export function assertEnv(): void {
  const required = env.offlineLicenseToken
    ? (['databaseId'] as const)
    : (['databaseId', 'token', 'serverUrl'] as const);
  const missing = required.filter(
    (k) => !env[k],
  );
  if (missing.length > 0) {
    throw new Error(
      `Missing required env vars: ${missing.join(', ')}. ` +
        `Copy .env.sample to .env at the repo root and fill in DITTO_DATABASE_ID, ` +
        `DITTO_DEVELOPMENT_TOKEN, DITTO_SERVER_URL, or set ` +
        `DITTO_OFFLINE_LICENSE_TOKEN for offline mode.`,
    );
  }
}
