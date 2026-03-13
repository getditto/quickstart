import {
  Authenticator,
  Ditto,
  DittoConfig,
  StoreObserver,
  SyncSubscription,
  init,
} from '@dittolive/ditto';
import './App.css';
import DittoInfo from './components/DittoInfo';
import { useEffect, useRef, useState } from 'react';
import TaskList from './components/TaskList';

export type Task = {
  _id: string;
  title: string;
  done: boolean;
  deleted: boolean;
};

const App = () => {
  const [error, setError] = useState<Error | null>(null);
  const ditto = useRef<Ditto | null>(null);
  const tasksSubscription = useRef<SyncSubscription | null>(null);
  const tasksObserver = useRef<StoreObserver | null>(null);
  const isInitializing = useRef<boolean>(false); // Persist across renders

  const [syncActive, setSyncActive] = useState<boolean>(true);
  const [isInitialized, setIsInitialized] = useState<boolean>(false);

  const [tasks, setTasks] = useState<Task[] | null>(null);

  useEffect(() => {
    let isMounted = true; // Prevent state updates after unmount

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
        const config = new DittoConfig(import.meta.env.DITTO_APP_ID, {
          mode: 'server',
          url: import.meta.env.DITTO_AUTH_URL,
        });

        // Step 3: Open Ditto instance
        ditto.current = await Ditto.open(config);

        // Step 4: Set up authentication expiration handler (required for server connections)
        await ditto.current.auth.setExpirationHandler(async (dittoInstance) => {
          // Authenticate when token is expiring. Any errors will be logged in the Ditto logger.
          const loginResult = await dittoInstance.auth.login(
            import.meta.env.DITTO_PLAYGROUND_TOKEN,
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

        // Step 5: Configure transport
        ditto.current.updateTransportConfig((config) => {
          config.connect.websocketURLs = [import.meta.env.DITTO_WEBSOCKET_URL];
          return config;
        });

        // Step 6: Start sync
        ditto.current.sync.start();

        // Step 7: Register subscription
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
        tasksSubscription.current = ditto.current.sync.registerSubscription(
          'SELECT * FROM tasks',
        );

        // Step 8: Register observer
        // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
        tasksObserver.current = ditto.current.store.registerObserver<Task>(
          'SELECT * FROM tasks WHERE deleted=false ORDER BY title ASC',
          (results) => {
            console.log('Observer', results);
            if (isMounted) {
              const tasks = results.items.map((item) => item.value);
              setTasks(tasks);
            }
          },
        );

        // Step 9: Mark as initialized
        if (isMounted) {
          setIsInitialized(true);
        }
        isInitializing.current = false;
        console.log('✅ Ditto initialized successfully');
      } catch (e) {
        console.error('❌ Ditto initialization failed:', e);
        isInitializing.current = false;
        if (isMounted) {
          setError(e as Error);
          setIsInitialized(false);
        }
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
      isMounted = false;
      tasksObserver.current?.cancel();
      tasksSubscription.current?.cancel();

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

  const toggleSync = () => {
    if (syncActive) {
      ditto.current?.sync.stop();
    } else {
      ditto.current?.sync.start();
    }
    setSyncActive(!syncActive);
  };

  // https://docs.ditto.live/sdk/latest/crud/create
  const createTask = async (title: string) => {
    try {
      await ditto.current?.store.execute(
        'INSERT INTO tasks DOCUMENTS (:task)',
        {
          task: {
            title,
            done: false,
            deleted: false,
          },
        },
      );
    } catch (error) {
      console.error('Failed to create task:', error);
    }
  };

  // https://docs.ditto.live/sdk/latest/crud/update
  const editTask = async (id: string, title: string) => {
    try {
      await ditto.current?.store.execute(
        'UPDATE tasks SET title=:title WHERE _id=:id',
        {
          id,
          title,
        },
      );
    } catch (error) {
      console.error('Failed to edit task:', error);
    }
  };

  const toggleTask = async (task: Task) => {
    try {
      await ditto.current?.store.execute(
        'UPDATE tasks SET done=:done WHERE _id=:id',
        {
          id: task._id,
          done: !task.done,
        },
      );
    } catch (error) {
      console.error('Failed to toggle task:', error);
    }
  };

  // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
  const deleteTask = async (task: Task) => {
    try {
      await ditto.current?.store.execute(
        'UPDATE tasks SET deleted=true WHERE _id=:id',
        {
          id: task._id,
        },
      );
    } catch (error) {
      console.error('Failed to delete task:', error);
    }
  };

  const ErrorMessage: React.FC<{ error: Error }> = ({ error }) => {
    const [dismissed, setDismissed] = useState(false);
    if (dismissed) return null;

    return (
      <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-red-100 text-red-700 p-6 rounded shadow-lg">
        <div className="flex justify-between items-center">
          <p>
            <b>Error</b>: {error.message}
          </p>
          <button
            onClick={() => setDismissed(true)}
            className="ml-4 text-red-700 hover:text-red-900"
          >
            &times;
          </button>
        </div>
      </div>
    );
  };

  return (
    <div className="h-screen w-full bg-gray-100">
      <div className="h-full w-full flex flex-col container mx-auto items-center">
        {error && <ErrorMessage error={error} />}
        <DittoInfo
          appId={import.meta.env.DITTO_APP_ID}
          token={import.meta.env.DITTO_PLAYGROUND_TOKEN}
          syncEnabled={syncActive}
          onToggleSync={toggleSync}
          isInitialized={isInitialized}
        />
        <TaskList
          tasks={tasks}
          onCreate={createTask}
          onEdit={editTask}
          onToggle={toggleTask}
          onDelete={deleteTask}
          isInitialized={isInitialized}
        />
      </div>
    </div>
  );
};

export default App;
