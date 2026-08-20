import { useEffect, useRef, useState } from 'react';
import { Ditto, StoreObserver, SyncSubscription } from '@dittolive/ditto';
import type { Task } from '../App';

/**
 * TasksRepository hook: registers the tasks subscription and observer, holds
 * the tasks state, and exposes CRUD. It calls the real Ditto API directly and
 * takes the ready `ditto` instance from `useDitto`.
 */
export const useTasks = (ditto: Ditto | null) => {
  const taskSubscription = useRef<SyncSubscription | null>(null);
  const taskObserver = useRef<StoreObserver | null>(null);

  const [tasks, setTasks] = useState<Task[]>([]);

  useEffect(() => {
    if (!ditto) {
      return;
    }

    taskSubscription.current = ditto.sync.registerSubscription(
      'SELECT * FROM tasks',
    );

    taskObserver.current = ditto.store.registerObserver(
      'SELECT * FROM tasks WHERE NOT deleted',
      (response) => {
        const fetchedTasks: Task[] = response.items.map((doc) => ({
          _id: doc.value._id,
          title: doc.value.title as string,
          done: doc.value.done,
          deleted: doc.value.deleted,
        }));

        setTasks(fetchedTasks);
      },
    );

    return () => {
      taskObserver.current?.cancel();
      taskSubscription.current?.cancel();
      taskObserver.current = null;
      taskSubscription.current = null;
    };
  }, [ditto]);

  // https://docs.ditto.live/sdk/latest/crud/create
  const createTask = async (title: string) => {
    if (title === '') {
      return;
    }
    await ditto?.store.execute('INSERT INTO tasks DOCUMENTS (:task)', {
      task: {
        title,
        done: false,
        deleted: false,
      },
    });
  };

  // https://docs.ditto.live/sdk/latest/crud/update
  const toggleTask = async (task: Task) => {
    await ditto?.store.execute(
      'UPDATE tasks SET done = :done WHERE _id = :id',
      {
        id: task._id,
        done: !task.done,
      },
    );
  };

  // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
  const deleteTask = async (task: Task) => {
    await ditto?.store.execute(
      'UPDATE tasks SET deleted = true WHERE _id = :id',
      {
        id: task._id,
      },
    );
  };

  const updateTaskTitle = async (taskId: string, newTitle: string) => {
    await ditto?.store.execute(
      'UPDATE tasks SET title = :title WHERE _id = :id',
      {
        id: taskId,
        title: newTitle,
      },
    );
  };

  return {
    tasks,
    createTask,
    toggleTask,
    deleteTask,
    updateTaskTitle,
  };
};
