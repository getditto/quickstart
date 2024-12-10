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
  const [isInitialized, setIsInitialized] = useState(false);

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
        setIsInitialized(true);
      } catch (e) {
        console.error('Failed to initialize Ditto:', e);
      }
    };

    initializeDitto();
  }, []);

  useEffect(() => {
    if (!isInitialized) return;

    (async () => {
      try {
        await init();
        ditto.current = new Ditto(identity);
        await ditto.current.disableSyncWithV3();
        ditto.current.startSync();

        tasksSubscription.current = ditto.current.sync.registerSubscription('SELECT * FROM tasks');
        tasksObserver.current = ditto.current.store.registerObserver<Task>('SELECT * FROM tasks WHERE deleted=false', (results) => {
          console.log("Observer", results);
          // const tasks = results.items.map((item) => item.value);
          // setTasks(tasks);
        });

      } catch (e) {
        console.error('Failed to initialize Ditto:', e);
      }
    })();
  }, [isInitialized]);

  return (
    <div className='h-full w-full flex flex-col container mx-auto'>
      <DittoInfo appId={identity.appID} token={identity.token} />
      <TaskList tasks={tasks} />
    </div>
  )
}

export default App
