import {
  Authenticator,
  Ditto,
  DittoConfig,
  StoreObserver,
  SyncSubscription,
} from '@dittolive/ditto';
import { app } from 'electron';
import { env } from './env';
import type { DittoIdentity, Task } from '../types';

let ditto: Ditto | null = null;
let subscription: SyncSubscription | null = null;
let observer: StoreObserver | null = null;

export async function initDitto(
  onTasksUpdated: (tasks: Task[]) => void,
): Promise<void> {
  const config = new DittoConfig(
    env.appId,
    { mode: 'server', url: env.authUrl },
    app.getPath('userData'),
  );

  ditto = await Ditto.open(config);

  // Authenticate with the playground token, and re-authenticate when it expires.
  await ditto.auth.setExpirationHandler(async (instance) => {
    const result = await instance.auth.login(
      env.token,
      Authenticator.DEVELOPMENT_PROVIDER,
    );
    if (result.error) {
      console.error('Re-authentication failed:', result.error);
    }
  });

  // BLE and AWDL require macOS entitlements that only signed app bundles get,
  // so they're disabled here for unsigned `npm run dev` builds. LAN (TCP +
  // mDNS) provides peer-to-peer sync across the local network; the websocket
  // URL provides cloud sync to Ditto's Big Peer.
  ditto.updateTransportConfig((cfg) => {
    cfg.connect.websocketURLs = [env.websocketUrl];
    cfg.peerToPeer.bluetoothLE.isEnabled = false;
    cfg.peerToPeer.awdl.isEnabled = false;
    cfg.peerToPeer.lan.isEnabled = true;
    cfg.peerToPeer.lan.isMdnsEnabled = true;
    cfg.peerToPeer.lan.isMulticastEnabled = true;
  });

  ditto.sync.start();

  // https://docs.ditto.com/sdk/latest/sync/syncing-data#creating-subscriptions
  subscription = ditto.sync.registerSubscription('SELECT * FROM tasks');

  // https://docs.ditto.com/sdk/latest/crud/observing-data-changes#setting-up-store-observers
  observer = ditto.store.registerObserver<Task>(
    'SELECT * FROM tasks WHERE deleted=false ORDER BY title ASC',
    (results) => {
      onTasksUpdated(results.items.map((item) => item.value));
    },
  );
}

export function getInfo(): DittoIdentity {
  return { appId: env.appId, token: env.token };
}

// https://docs.ditto.com/sdk/latest/crud/create
export async function createTask(title: string): Promise<void> {
  await ditto?.store.execute('INSERT INTO tasks DOCUMENTS (:task)', {
    task: { title, done: false, deleted: false },
  });
}

// https://docs.ditto.com/sdk/latest/crud/update
export async function editTask(id: string, title: string): Promise<void> {
  await ditto?.store.execute('UPDATE tasks SET title=:title WHERE _id=:id', {
    id,
    title,
  });
}

export async function toggleTask(id: string, done: boolean): Promise<void> {
  await ditto?.store.execute('UPDATE tasks SET done=:done WHERE _id=:id', {
    id,
    done,
  });
}

// https://docs.ditto.com/sdk/latest/crud/delete#soft-delete-pattern
export async function deleteTask(id: string): Promise<void> {
  await ditto?.store.execute('UPDATE tasks SET deleted=true WHERE _id=:id', {
    id,
  });
}

export function startSync(): void {
  ditto?.sync.start();
}

export function stopSync(): void {
  ditto?.sync.stop();
}

export async function shutdown(): Promise<void> {
  observer?.cancel();
  subscription?.cancel();
  observer = null;
  subscription = null;
  if (ditto) {
    try {
      ditto.close();
    } catch (e) {
      console.error('Error closing Ditto:', e);
    }
    ditto = null;
  }
}
