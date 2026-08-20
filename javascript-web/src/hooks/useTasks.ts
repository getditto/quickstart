import { Ditto, StoreObserver, SyncSubscription } from '@dittolive/ditto';
import { useCallback, useEffect, useRef, useState } from 'react';
import type { Task } from '../App';

/**
 * TasksRepository hook: registers the tasks subscription and observer, holds
 * the tasks state, and exposes CRUD. It calls the real Ditto API directly and
 * takes the ready `ditto` instance from `useDitto`.
 */
export const useTasks = (ditto: Ditto | null) => {
  const tasksSubscription = useRef<SyncSubscription | null>(null);
  const tasksObserver = useRef<StoreObserver | null>(null);

  // Mirror the latest instance into a ref so the CRUD callbacks stay stable
  // (empty deps) while always reading the current Ditto instance.
  const dittoRef = useRef<Ditto | null>(ditto);
  dittoRef.current = ditto;

  const [tasks, setTasks] = useState<Task[] | null>(null);

  useEffect(() => {
    if (!ditto) {
      return;
    }

    let isMounted = true; // Prevent state updates after unmount

    // Register subscription
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    tasksSubscription.current = ditto.sync.registerSubscription(
      'SELECT * FROM tasks',
    );

    // Register observer
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    tasksObserver.current = ditto.store.registerObserver<Task>(
      'SELECT * FROM tasks WHERE NOT deleted',
      (results) => {
        console.log('Observer', results);
        if (isMounted) {
          const tasks = results.items.map((item) => item.value);
          setTasks(tasks);
        }
      },
    );

    return () => {
      // In development with StrictMode, skip ALL cleanup to avoid lock file issues
      // and keep observers active. This handles StrictMode's intentional unmount/remount.
      if (import.meta.env.DEV) {
        console.log('Dev mode - skipping ALL cleanup to handle StrictMode');
        return;
      }

      // In production, properly clean up the subscription and observer
      isMounted = false;
      tasksObserver.current?.cancel();
      tasksSubscription.current?.cancel();
    };
  }, [ditto]);

  // https://docs.ditto.live/sdk/latest/crud/create
  const createTask = useCallback(async (title: string) => {
    try {
      await dittoRef.current?.store.execute(
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
  }, []);

  // https://docs.ditto.live/sdk/latest/crud/update
  const editTask = useCallback(async (id: string, title: string) => {
    try {
      await dittoRef.current?.store.execute(
        'UPDATE tasks SET title = :title WHERE _id = :id',
        {
          id,
          title,
        },
      );
    } catch (error) {
      console.error('Failed to edit task:', error);
    }
  }, []);

  const toggleTask = useCallback(async (task: Task) => {
    try {
      await dittoRef.current?.store.execute(
        'UPDATE tasks SET done = :done WHERE _id = :id',
        {
          id: task._id,
          done: !task.done,
        },
      );
    } catch (error) {
      console.error('Failed to toggle task:', error);
    }
  }, []);

  // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
  const deleteTask = useCallback(async (task: Task) => {
    try {
      await dittoRef.current?.store.execute(
        'UPDATE tasks SET deleted = true WHERE _id = :id',
        {
          id: task._id,
        },
      );
    } catch (error) {
      console.error('Failed to delete task:', error);
    }
  }, []);

  return {
    tasks,
    createTask,
    editTask,
    toggleTask,
    deleteTask,
  };
};
