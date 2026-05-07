import { useCallback, useEffect, useState } from 'react';
import DittoInfo from './components/DittoInfo';
import ErrorMessage from './components/ErrorMessage';
import TaskList from './components/TaskList';
import type { Task } from '../types';

const App = () => {
  const [error, setError] = useState<Error | null>(null);
  const [appId, setAppId] = useState<string>('');
  const [token, setToken] = useState<string>('');
  const [syncActive, setSyncActive] = useState<boolean>(true);
  const [isInitialized, setIsInitialized] = useState<boolean>(false);
  const [tasks, setTasks] = useState<Task[] | null>(null);

  useEffect(() => {
    let unsubscribe: (() => void) | null = null;
    let isMounted = true;

    (async () => {
      try {
        const info = await window.ditto.getInfo();
        if (!isMounted) return;
        setAppId(info.appId);
        setToken(info.token);
        unsubscribe = window.ditto.onTasksUpdated((updated) => {
          if (isMounted) setTasks(updated);
        });
        setIsInitialized(true);
      } catch (e) {
        if (isMounted) setError(e as Error);
      }
    })();

    return () => {
      isMounted = false;
      unsubscribe?.();
    };
  }, []);

  const toggleSync = useCallback(async () => {
    setSyncActive((prev) => {
      if (prev) {
        window.ditto.stopSync();
      } else {
        window.ditto.startSync();
      }
      return !prev;
    });
  }, []);

  // https://docs.ditto.com/sdk/latest/crud/create
  const createTask = useCallback(async (title: string) => {
    try {
      await window.ditto.createTask(title);
    } catch (error) {
      console.error('Failed to create task:', error);
    }
  }, []);

  // https://docs.ditto.com/sdk/latest/crud/update
  const editTask = useCallback(async (id: string, title: string) => {
    try {
      await window.ditto.editTask(id, title);
    } catch (error) {
      console.error('Failed to edit task:', error);
    }
  }, []);

  const toggleTask = useCallback(async (task: Task) => {
    try {
      await window.ditto.toggleTask(task._id, !task.done);
    } catch (error) {
      console.error('Failed to toggle task:', error);
    }
  }, []);

  // https://docs.ditto.com/sdk/latest/crud/delete#soft-delete-pattern
  const deleteTask = useCallback(async (task: Task) => {
    try {
      await window.ditto.deleteTask(task._id);
    } catch (error) {
      console.error('Failed to delete task:', error);
    }
  }, []);

  return (
    <div className="h-screen w-full bg-gray-100">
      <div className="h-full w-full flex flex-col container mx-auto items-center">
        {error && <ErrorMessage error={error} />}
        <DittoInfo
          appId={appId}
          token={token}
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
