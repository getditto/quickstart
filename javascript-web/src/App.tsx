import DittoInfo from './components/DittoInfo';
import ErrorMessage from './components/ErrorMessage';
import TaskList from './components/TaskList';
import { useDitto } from './hooks/useDitto';
import { useTasks } from './hooks/useTasks';

export type Task = {
  _id: string;
  title: string;
  done: boolean;
  deleted: boolean;
};

const App = () => {
  const { ditto, syncActive, toggleSync, isInitialized, error } = useDitto();
  const { tasks, createTask, editTask, toggleTask, deleteTask } =
    useTasks(ditto);

  return (
    <div className="h-screen w-full bg-gray-100">
      <div className="h-full w-full flex flex-col container mx-auto items-center">
        {error && <ErrorMessage error={error} />}
        <DittoInfo
          databaseId={import.meta.env.DITTO_DATABASE_ID}
          token={import.meta.env.DITTO_DEVELOPMENT_TOKEN}
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
