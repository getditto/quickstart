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
  _id: string;
  title: string;
  done: boolean;
  deleted: boolean;
};

const App = () => {
  const ditto = useRef<Ditto | null>(null);
  const tasksSubscription = useRef<SyncSubscription | null>(null);
  const tasksObserver = useRef<StoreObserver | null>(null);
  const [isInitialized, setIsInitialized] = useState<Promise<void> | null>(null);

  const [tasks, setTasks] = useState<Task[]>([]);

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
        tasksObserver.current = ditto.current.store.registerObserver<Task>('SELECT * FROM tasks WHERE deleted=false ORDER BY done', (results) => {
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
    try {
      await ditto.current?.store.execute("INSERT INTO tasks DOCUMENTS (:task)", {
        task: {
          title,
          done: false,
          deleted: false,
        },
      });
    } catch (error) {
      console.error('Failed to create task:', error);
    }
  };

  const editTask = async (id: string, title: string) => {
    try {
      await ditto.current?.store.execute("UPDATE tasks SET title=:title WHERE _id=:id", {
        id,
        title,
      });
    } catch (error) {
      console.error('Failed to edit task:', error);
    }
  };

  const toggleTask = async (task: Task) => {
    try {
      await ditto.current?.store.execute("UPDATE tasks SET done=:done WHERE _id=:id", {
        id: task._id,
        done: !task.done,
      });
    } catch (error) {
      console.error('Failed to toggle task:', error);
    }
  };

  const deleteTask = async (task: Task) => {
    try {
      await ditto.current?.store.execute("UPDATE tasks SET deleted=true WHERE _id=:id", {
        id: task._id,
      });
    } catch (error) {
      console.error('Failed to delete task:', error);
    }
  };

  return (
    <div className='h-screen w-full bg-gray-100'>
      <div className='h-full w-full flex flex-col container mx-auto items-center'>
        <DittoInfo appId={identity.appID} token={identity.token} />
        <TaskList tasks={tasks} onCreate={createTask} onEdit={editTask} onToggle={toggleTask} onDelete={deleteTask} />
      </div>
    </div>
  )
}

export default App
