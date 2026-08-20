import { Ditto, DittoConfig, Authenticator } from '@dittolive/ditto';

// Owns the Ditto instance lifecycle: config, open, identity/auth, transport,
// and sync start/stop. Vends a configured, already-running instance and holds
// onto it. Knows nothing about tasks.
export class DittoManager {
  constructor({ databaseId, token, serverURL, persistenceDirectory }) {
    this.databaseId = databaseId;
    this.token = token;
    this.serverURL = serverURL;
    this.persistenceDirectory = persistenceDirectory;
    this.ditto = null;
  }

  // Opens a Ditto instance, authenticates, configures transports, starts sync,
  // and returns the ready instance.
  // https://docs.ditto.live/sdk/latest/install-guides/nodejs#installing-the-demo-task-app
  async open() {
    const connectConfig = {
      mode: 'server',
      url: this.serverURL,
    };

    const config = new DittoConfig(
      this.databaseId,
      connectConfig,
      this.persistenceDirectory,
    );
    const ditto = await Ditto.open(config);
    this.ditto = ditto;

    // Initialize transport config — enable LAN P2P.
    // BLE and AWDL are disabled because they require macOS entitlements
    // that are only available to signed app bundles, not Node.js processes.
    // LAN (TCP + mDNS) provides P2P sync with peers on the local network.
    ditto.updateTransportConfig((config) => {
      config.peerToPeer.bluetoothLE.isEnabled = false;
      config.peerToPeer.awdl.isEnabled = false;
      config.peerToPeer.lan.isEnabled = true;
      config.peerToPeer.lan.isMdnsEnabled = true;
      config.peerToPeer.lan.isMulticastEnabled = true;
    });

    // Set up authentication for server mode
    await ditto.auth.setExpirationHandler(
      async (dittoInstance, timeUntilExpiration) => {
        console.log(
          'Authentication expiring soon, time until expiration:',
          timeUntilExpiration,
        );

        if (dittoInstance.auth.loginSupported) {
          const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
          const reLoginResult = await dittoInstance.auth.login(
            this.token,
            devProvider,
          );
          if (reLoginResult.error) {
            console.error('Re-authentication failed:', reLoginResult.error);
          } else {
            console.log(
              'Successfully re-authenticated with info:',
              reLoginResult,
            );
          }
        }
      },
    );

    if (ditto.auth.loginSupported) {
      const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
      const loginResult = await ditto.auth.login(this.token, devProvider);
      if (loginResult.error) {
        console.error('Login failed:', loginResult.error);
      } else {
        console.log('Successfully logged in with info:', loginResult);
      }
    }

    ditto.sync.start();

    return ditto;
  }

  startSync() {
    this.ditto?.sync.start();
  }

  stopSync() {
    this.ditto?.sync.stop();
  }
}
