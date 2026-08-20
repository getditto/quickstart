import dotenv from 'dotenv';
import { resolve } from 'node:path';

dotenv.config({ path: resolve(process.cwd(), '..', '.env') });

export const env = {
  databaseId: process.env.DITTO_DATABASE_ID ?? '',
  token: process.env.DITTO_DEVELOPMENT_TOKEN ?? '',
  serverUrl: process.env.DITTO_SERVER_URL ?? '',
};

export function assertEnv(): void {
  const missing = (['databaseId', 'token', 'serverUrl'] as const).filter(
    (k) => !env[k],
  );
  if (missing.length > 0) {
    throw new Error(
      `Missing required env vars: ${missing.join(', ')}. ` +
        `Copy .env.sample to .env at the repo root and fill in DITTO_DATABASE_ID, ` +
        `DITTO_DEVELOPMENT_TOKEN, DITTO_SERVER_URL.`,
    );
  }
}
