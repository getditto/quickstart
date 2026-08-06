import { useEffect, useRef, useState } from 'react';
import { PermissionsAndroid, Platform } from 'react-native';
import {
  Authenticator,
  Ditto,
  DittoConfig,
  DittoConfigConnect,
  init,
} from '@dittolive/ditto';
import {
  DITTO_DATABASE_ID,
  DITTO_DEVELOPMENT_TOKEN,
  DITTO_OFFLINE_LICENSE_TOKEN,
  DITTO_SERVER_URL,
} from '@env';

async function requestPermissions() {
  const permissions = [
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_ADVERTISE,
    PermissionsAndroid.PERMISSIONS.NEARBY_WIFI_DEVICES,
    PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
  ];

  const granted = await PermissionsAndroid.requestMultiple(permissions);
  return Object.values(granted).every(
    (result) => result === PermissionsAndroid.RESULTS.GRANTED,
  );
}

/**
 * DittoManager hook: requests transport permissions and owns the Ditto
 * instance lifecycle (init, open, auth, sync.start). Returns the ready `ditto`
 * instance plus the sync-toggle/permission status the UI displays. It knows
 * nothing about tasks.
 */
export const useDitto = () => {
  const ditto = useRef<Ditto | null>(null);

  const [syncEnabled, setSyncEnabled] = useState(true);
  const [hasPermissions, setHasPermissions] = useState<boolean>(true);

  // Exposed to consumers as state so dependent hooks re-run once Ditto is ready.
  const [dittoInstance, setDittoInstance] = useState<Ditto | null>(null);

  // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
  const toggleSync = () => {
    if (syncEnabled) {
      ditto.current?.sync.stop();
    } else {
      ditto.current?.sync.start();
    }
    setSyncEnabled(!syncEnabled);
  };

  const initDitto = async () => {
    try {
      await init();

      // https://docs.ditto.live/sdk/latest/install-guides/react-native
      const databaseId = DITTO_DATABASE_ID;
      const developmentToken = DITTO_DEVELOPMENT_TOKEN;
      const offlineLicenseToken = (DITTO_OFFLINE_LICENSE_TOKEN ?? '').trim();

      const connectConfig: DittoConfigConnect = offlineLicenseToken
        ? { mode: 'smallPeersOnly' }
        : { mode: 'server', url: DITTO_SERVER_URL };

      const config = new DittoConfig(
        databaseId,
        connectConfig,
        'custom-folder',
      );

      ditto.current = await Ditto.open(config);

      if (offlineLicenseToken) {
        ditto.current.setOfflineOnlyLicenseToken(offlineLicenseToken);
      } else {
        await ditto.current.auth.setExpirationHandler(
          async (authDitto, timeUntilExpiration) => {
            console.log(
              'Authentication expiring soon, time until expiration:',
              timeUntilExpiration,
            );

            if (authDitto.auth.loginSupported) {
              const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
              const reLoginResult = await authDitto.auth.login(
                developmentToken,
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

        if (ditto.current.auth.loginSupported) {
          // Use the development provider constant from Ditto
          const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
          console.log('Using development provider:', devProvider);

          const loginResult = await ditto.current.auth.login(
            developmentToken,
            devProvider,
          );
          if (loginResult.error) {
            console.error('Login failed:', loginResult.error);
          } else {
            console.log('Successfully logged in with info:', loginResult);
          }
        }
      }

      ditto.current.sync.start();

      // Expose the ready instance so useTasks can register its subscription
      // and observer.
      setDittoInstance(ditto.current);
    } catch (error) {
      console.error('Error syncing tasks:', error);
    }
  };

  useEffect(() => {
    (async () => {
      const granted =
        Platform.OS === 'android' ? await requestPermissions() : true;

      setHasPermissions(granted);
      initDitto();
    })();
  }, []);

  return {
    ditto: dittoInstance,
    syncEnabled,
    toggleSync,
    hasPermissions,
  };
};
