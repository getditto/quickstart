import { Authenticator, Ditto, DittoConfig } from '@dittolive/ditto';
import { app } from 'electron';
import { env } from './env';
import type { DittoIdentity } from '../types';

// Owns the Ditto instance lifecycle: open, identity/auth, transport config,
// and sync start/stop. Knows nothing about tasks.
export class DittoManager {
  private ditto: Ditto | null = null;

  async open(): Promise<Ditto> {
    const connect = env.offlineLicenseToken
      ? ({ mode: 'smallPeersOnly' } as const)
      : ({ mode: 'server', url: env.serverUrl } as const);
    const config = new DittoConfig(
      env.databaseId,
      connect,
      app.getPath('userData'),
    );

    const ditto = await Ditto.open(config);
    this.ditto = ditto;

    if (env.offlineLicenseToken) {
      ditto.setOfflineOnlyLicenseToken(env.offlineLicenseToken);
    } else {
      // Authenticate with the development token, and re-authenticate when it expires.
      await ditto.auth.setExpirationHandler(async (instance) => {
        const result = await instance.auth.login(
          env.token,
          Authenticator.DEVELOPMENT_PROVIDER,
        );
        if (result.error) {
          console.error('Re-authentication failed:', result.error);
        }
      });
    }

    // BLE and AWDL require macOS entitlements that only signed app bundles get,
    // so they're disabled here for unsigned `npm run dev` builds. LAN (TCP +
    // mDNS) provides peer-to-peer sync across the local network.
    ditto.updateTransportConfig((cfg) => {
      cfg.peerToPeer.bluetoothLE.isEnabled = false;
      cfg.peerToPeer.awdl.isEnabled = false;
      cfg.peerToPeer.lan.isEnabled = true;
      cfg.peerToPeer.lan.isMdnsEnabled = true;
      cfg.peerToPeer.lan.isMulticastEnabled = true;
    });

    ditto.sync.start();

    return ditto;
  }

  getInfo(): DittoIdentity {
    return { databaseId: env.databaseId, token: env.token };
  }

  startSync(): void {
    this.ditto?.sync.start();
  }

  stopSync(): void {
    this.ditto?.sync.stop();
  }

  close(): void {
    if (this.ditto) {
      try {
        this.ditto.close();
      } catch (e) {
        console.error('Error closing Ditto:', e);
      }
      this.ditto = null;
    }
  }
}
