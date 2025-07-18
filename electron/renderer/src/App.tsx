import { useEffect, useState } from 'react';
import { Task } from './types';
import DittoInfo from './components/DittoInfo';
import TaskList from './components/TaskList';
import './App.css';

const App = () => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isInitialized, setIsInitialized] = useState(false);
  const [syncActive, setSyncActive] = useState(true);
  const [dittoInfo, setDittoInfo] = useState<{ appId: string; token: string } | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Get initial state
    const getInitialState = async () => {
      try {
        const state = await window.electronAPI.getDittoState();
        if (state.isInitialized && state.appId && state.token) {
          setDittoInfo({ appId: state.appId, token: state.token });
          setIsInitialized(true);
          setSyncActive(state.syncActive || true);
          setError(null);
          
          // Fetch initial tasks
          const tasksResult = await window.electronAPI.getTasks();
          if (tasksResult.success) {
            setTasks(tasksResult.tasks);
          } else if (tasksResult.error) {
            console.error('Failed to fetch initial tasks:', tasksResult.error);
          }
        }
      } catch (err) {
        console.error('Failed to get initial state:', err);
      }
    };

    getInitialState();

    // Set up event listeners
    window.electronAPI.onTasksUpdated((_event, tasks) => {
      setTasks(tasks);
    });

    window.electronAPI.onDittoInitialized((_event, data) => {
      setDittoInfo(data);
      setIsInitialized(true);
      setError(null);
    });

    window.electronAPI.onDittoError((_event, errorMessage) => {
      setError(errorMessage);
      setIsInitialized(false);
    });

    // Clean up listeners on unmount
    return () => {
      window.electronAPI.removeAllListeners('tasks-updated');
      window.electronAPI.removeAllListeners('ditto-initialized');
      window.electronAPI.removeAllListeners('ditto-error');
    };
  }, []);

  const handleCreateTask = async (title: string) => {
    const result = await window.electronAPI.createTask(title);
    if (!result.success) {
      setError(result.error || 'Failed to create task');
    }
  };

  const handleEditTask = async (id: string, title: string) => {
    const result = await window.electronAPI.editTask(id, title);
    if (!result.success) {
      setError(result.error || 'Failed to edit task');
    }
  };

  const handleToggleTask = async (task: Task) => {
    const result = await window.electronAPI.toggleTask(task._id, task.done);
    if (!result.success) {
      setError(result.error || 'Failed to toggle task');
    }
  };

  const handleDeleteTask = async (task: Task) => {
    const result = await window.electronAPI.deleteTask(task._id);
    if (!result.success) {
      setError(result.error || 'Failed to delete task');
    }
  };

  const handleToggleSync = async () => {
    const result = await window.electronAPI.toggleSync();
    if (result.success && typeof result.syncActive === 'boolean') {
      setSyncActive(result.syncActive);
    } else {
      setError(result.error || 'Failed to toggle sync');
    }
  };

  const ErrorMessage = ({ error }: { error: string }) => {
    const [dismissed, setDismissed] = useState(false);
    if (dismissed) return null;

    return (
      <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 bg-red-100 text-red-700 p-6 rounded shadow-lg z-50">
        <div className="flex justify-between items-center">
          <p>
            <b>Error</b>: {error}
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
          appId={dittoInfo?.appId || ''}
          token={dittoInfo?.token || ''}
          syncEnabled={syncActive}
          onToggleSync={handleToggleSync}
          isInitialized={isInitialized}
        />
        <TaskList
          tasks={tasks}
          onCreate={handleCreateTask}
          onEdit={handleEditTask}
          onToggle={handleToggleTask}
          onDelete={handleDeleteTask}
          isInitialized={isInitialized}
        />
      </div>
    </div>
  );
};

export default App;