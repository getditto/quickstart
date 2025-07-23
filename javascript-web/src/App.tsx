import {
  Ditto,
  IdentityOnlinePlayground,
  StoreObserver,
  SyncSubscription,
  init,
} from '@dittolive/ditto';
import './App.css';
import DittoInfo from './components/DittoInfo';
import { useEffect, useRef, useState } from 'react';
import TaskList from './components/TaskList';

const identity: IdentityOnlinePlayground = {
  type: 'onlinePlayground',
  appID: import.meta.env.DITTO_APP_ID,
  token: import.meta.env.DITTO_PLAYGROUND_TOKEN,
  customAuthURL: import.meta.env.DITTO_AUTH_URL,
  enableDittoCloudSync: false,
};

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

  const [syncActive, setSyncActive] = useState<boolean>(true);
  const [promisedInitialization, setPromisedInitialization] =
    useState<Promise<void> | null>(null);
  const [isInitialized, setIsInitialized] = useState<boolean>(false);

  const [tasks, setTasks] = useState<Task[] | null>(null);
  useEffect(() => {
    const initializeDitto = async () => {
      try {
        await init();
      } catch (e) {
        console.error('Failed to initialize Ditto:', e);
      }
    };

    if (!promisedInitialization) setPromisedInitialization(initializeDitto());
  }, [promisedInitialization]);

  useEffect(() => {
    if (!promisedInitialization) return;

    (async () => {
      await promisedInitialization;
      try {
        // Create a new Ditto instance with the identity
        // https://docs.ditto.live/sdk/latest/install-guides/js#integrating-ditto-and-starting-sync
        ditto.current = new Ditto(identity);

        // Initialize transport config
        ditto.current.updateTransportConfig((config) => {
          config.connect.websocketURLs = [import.meta.env.DITTO_WEBSOCKET_URL];
          return config;
        });

        // disable sync with v3 peers, required for DQL
        await ditto.current.disableSyncWithV3();

        // Disable DQL strict mode
        // when set to false, collection definitions are no longer required. SELECT queries will return and display all fields by default.
        // https://docs.ditto.live/dql/strict-mode
        await ditto.current.store.execute('ALTER SYSTEM SET DQL_STRICT_MODE = false');

        ditto.current.startSync();

        // Register a subscription, which determines what data syncs to this peer
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
        tasksSubscription.current = ditto.current.sync.registerSubscription(
          'SELECT * FROM tasks',
        );

        // Register observer, which runs against the local database on this peer
        // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
        tasksObserver.current = ditto.current.store.registerObserver<Task>(
          'SELECT * FROM tasks WHERE deleted=false ORDER BY done',
          (results) => {
            console.log('Observer', results);
            const tasks = results.items.map((item) => item.value);
            setTasks(tasks);
          },
        );
        setIsInitialized(true);
      } catch (e) {
        setError(e as Error);
        setIsInitialized(false);
      }

      return () => {
        ditto.current?.close();
        ditto.current = null;
      };
    })();
  }, [promisedInitialization]);

  const toggleSync = () => {
    if (syncActive) {
      ditto.current?.stopSync();
    } else {
      ditto.current?.startSync();
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
          appId={identity.appID}
          token={identity.token}
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
