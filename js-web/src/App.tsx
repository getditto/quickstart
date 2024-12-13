import { Ditto, IdentityOnlinePlayground, StoreObserver, SyncSubscription, init } from '@dittolive/ditto';
import './App.css'
import DittoInfo from './components/DittoInfo'
import { useEffect, useRef, useState } from 'react';
import TaskList from './components/TaskList';

const identity: IdentityOnlinePlayground = {
  type: 'onlinePlayground',
  appID: '<YOUR_APP_ID>',
  token: '<YOUR_PLAYGROUND_TOKEN>',
  enableDittoCloudSync: true,
};

export type Task = {
  id: string;
  title: string;
  done: boolean;
  deleted: boolean;
};

const App = () => {
  const ditto = useRef<Ditto | null>(null);
  const tasksSubscription = useRef<SyncSubscription | null>(null);
  const tasksObserver = useRef<StoreObserver | null>(null);
  const [isInitialized, setIsInitialized] = useState<Promise<void> | null>(null);

  const [tasks, setTasks] = useState<Task[]>([
    { id: '1', title: 'apple', done: true, deleted: false },
    { id: '2', title: 'banana', done: false, deleted: false },
    { id: '3', title: 'cranberry', done: true, deleted: false },
    { id: '4', title: 'date', done: false, deleted: false },
  ]);

  useEffect(() => {
    const initializeDitto = async () => {
      try {
        await init();
      } catch (e) {
        console.error('Failed to initialize Ditto:', e);
      }
    };

    setIsInitialized(initializeDitto());
  }, []);

  useEffect(() => {
    if (!isInitialized) return;

    (async () => {
      await isInitialized;
      try {
        ditto.current = new Ditto(identity);
        await ditto.current.disableSyncWithV3();
        ditto.current.startSync();

        tasksSubscription.current = ditto.current.sync.registerSubscription('SELECT * FROM tasks');
        tasksObserver.current = ditto.current.store.registerObserver<Task>('SELECT * FROM tasks WHERE deleted=false', (results) => {
          console.log("Observer", results);
          const tasks = results.items.map((item) => item.value);
          setTasks(tasks);
        });

      } catch (e) {
        console.error('Failed to initialize Ditto:', e);
      }

      return () => {
        ditto.current?.close();
        ditto.current = null;
      };
    })();
  }, [isInitialized]);

  const createTask = async (title: string) => {
    console.log("Creating", title);
    await ditto.current?.store.execute("INSERT INTO tasks DOCUMENTS (:task)", {
      task: {
        title,
        done: false,
        deleted: false,
      },
    });
  };

  const toggleTask = async (task: Task) => {
    console.log("Toggling", task);
    await ditto.current?.store.execute("UPDATE tasks SET done=:done WHERE _id=:id", {
      id: task.id,
      done: !task.done,
    });
  };

  const deleteTask = async (task: Task) => {
    console.log("Deleting", task);
    await ditto.current?.store.execute("UPDATE tasks SET deleted=true WHERE _id=:id", {
      id: task.id,
    });
  };

  return (
    <div className='h-full w-full flex flex-col container mx-auto'>
      <DittoInfo appId={identity.appID} token={identity.token} />
      <TaskList tasks={tasks} onCreate={createTask} onToggle={toggleTask} onDelete={deleteTask} />
    </div>
  )
}

export default App
