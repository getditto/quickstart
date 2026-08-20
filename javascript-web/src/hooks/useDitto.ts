import { Authenticator, Ditto, DittoConfig, init } from '@dittolive/ditto';
import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * DittoManager hook: owns the Ditto instance lifecycle (init/open, auth,
 * transport, sync.start) and returns the ready `ditto` instance plus the
 * sync-toggle/status the UI displays. It knows nothing about tasks.
 */
export const useDitto = () => {
  const [error, setError] = useState<Error | null>(null);
  const ditto = useRef<Ditto | null>(null);
  const isInitializing = useRef<boolean>(false); // Persist across renders

  const [syncActive, setSyncActive] = useState<boolean>(true);
  const [isInitialized, setIsInitialized] = useState<boolean>(false);

  // Exposed to consumers as state so dependent hooks re-run once Ditto is ready.
  const [dittoInstance, setDittoInstance] = useState<Ditto | null>(null);

  useEffect(() => {
    const initializeDitto = async () => {
      // Skip if Ditto already exists (handles StrictMode double-mount)
      if (ditto.current) {
        console.log('Skipping init - Ditto already exists');
        return;
      }
      // Prevent concurrent initializations
      if (isInitializing.current) {
        console.log('Skipping init - initialization in progress');
        return;
      }
      isInitializing.current = true;
      console.log('Starting Ditto initialization...');
      try {
        // Step 1: Initialize WASM (MUST be first)
        await init();

        // Step 2: Create config (AFTER init)
        const config = new DittoConfig(import.meta.env.DITTO_DATABASE_ID, {
          mode: 'server',
          url: import.meta.env.DITTO_SERVER_URL,
        });

        // Step 3: Open Ditto instance
        ditto.current = await Ditto.open(config);

        // Step 4: Set up authentication expiration handler (required for server connections)
        await ditto.current.auth.setExpirationHandler(async (dittoInstance) => {
          // Authenticate when token is expiring. Any errors will be logged in the Ditto logger.
          const loginResult = await dittoInstance.auth.login(
            import.meta.env.DITTO_DEVELOPMENT_TOKEN,
            Authenticator.DEVELOPMENT_PROVIDER,
          );
          if (loginResult.error) {
            console.error('❌ Re-authentication failed:', loginResult.error);
          } else {
            console.log(
              '✅ Successfully re-authenticated with info:',
              loginResult,
            );
          }
        });

        // Step 5: Start sync
        ditto.current.sync.start();

        // Step 6: Mark as initialized and expose the ready instance
        setIsInitialized(true);
        setDittoInstance(ditto.current);
        isInitializing.current = false;
        console.log('✅ Ditto initialized successfully');
      } catch (e) {
        console.error('❌ Ditto initialization failed:', e);
        isInitializing.current = false;
        setError(e as Error);
        setIsInitialized(false);
      }
    };

    initializeDitto();

    // Cleanup function (returned from useEffect, not inside IIFE)
    return () => {
      // In development with StrictMode, skip ALL cleanup to avoid lock file issues
      // and keep observers active. This handles StrictMode's intentional unmount/remount.
      if (import.meta.env.DEV) {
        console.log('Dev mode - skipping ALL cleanup to handle StrictMode');
        return;
      }

      // In production, properly clean up everything
      console.log('Production cleanup - closing Ditto');

      if (ditto.current) {
        try {
          ditto.current.close();
        } catch (e) {
          console.error('Error closing Ditto:', e);
        }
        ditto.current = null;
      }
    };
  }, []); // Empty deps - run once on mount

  const toggleSync = useCallback(() => {
    setSyncActive((prev) => {
      if (prev) {
        ditto.current?.sync.stop();
      } else {
        ditto.current?.sync.start();
      }
      return !prev;
    });
  }, []);

  return {
    ditto: dittoInstance,
    syncActive,
    toggleSync,
    isInitialized,
    error,
  };
};
