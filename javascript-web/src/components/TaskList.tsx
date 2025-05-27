import React, { useState } from 'react';
import { Task } from '../App';

type ItemProps = {
  task: Task;
  onEdit: (id: string, title: string) => void;
  onToggle: (task: Task) => void;
  onDelete: (task: Task) => void;
};

const TaskItem: React.FC<ItemProps> = React.memo(
  ({ task, onEdit, onToggle, onDelete }) => {
    const [editing, setEditing] = useState<boolean>(false);
    const [editedTitle, setEditedTitle] = useState<string>('');

    const handleEdit = () => {
      const trimmedTitle = editedTitle.trim();
      if (trimmedTitle === '') return;
      onEdit(task._id, trimmedTitle);
      setEditedTitle('');
      setEditing(false);
    };

    const textOrInput = () => {
      if (!editing) {
        return (
          <span
            className={`flex-grow text-gray-700 ${task.done ? 'line-through text-gray-400' : ''} cursor-pointer hover:bg-gray-100 px-2 py-1 rounded`}
            onClick={() => {
              setEditedTitle(task.title);
              setEditing(true);
            }}
          >
            {task.title}
          </span>
        );
      }

      return (
        <input
          type="text"
          value={editedTitle}
          className="flex-grow px-2 py-1 border rounded border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          onChange={(e) => setEditedTitle(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              handleEdit();
            } else if (e.key === 'Escape') {
              setEditedTitle('');
              setEditing(false);
            }
          }}
          autoFocus
          onFocus={(e) => e.target.select()}
          onBlur={() => {
            if (!editedTitle || editedTitle === task.title) {
              setEditedTitle('');
              setEditing(false);
              return;
            }
            handleEdit();
          }}
        />
      );
    };

    return (
      <div className="group flex items-center p-2 px-4 border-b border-gray-200 hover:bg-gray-50">
        <input
          type="checkbox"
          checked={task.done}
          className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 mr-4"
          onChange={() => onToggle(task)}
        />
        {textOrInput()}
        <button
          className="invisible group-hover:visible p-1 ml-2 text-gray-400 hover:text-blue-600 transition-colors mr-2"
          aria-label="Edit task"
          onClick={() => {
            setEditedTitle(task.title);
            setEditing(true);
          }}
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z" />
          </svg>
        </button>
        <button
          className="invisible group-hover:visible p-1 text-gray-400 hover:text-red-600 transition-colors"
          aria-label="Delete task"
          onClick={() => onDelete(task)}
        >
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-5 w-5"
            viewBox="0 0 20 20"
            fill="currentColor"
          >
            <path
              fillRule="evenodd"
              d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z"
              clipRule="evenodd"
            />
          </svg>
        </button>
      </div>
    );
  },
);

type ListProps = {
  tasks: Task[] | null;
  onCreate: (title: string) => void;
  onEdit: (id: string, title: string) => void;
  onToggle: (task: Task) => void;
  onDelete: (task: Task) => void;
  isInitialized: boolean;
};

type Filter = 'all' | 'active';

const Spinner: React.FC = () => (
  <div
    className="animate-spin rounded-full h-8 w-8 border-4 border-b-gray-300 border-l-gray-300 border-r-gray-300 border-t-transparent"
    role="status"
    aria-live="polite"
  >
    <span className="sr-only">Initializing...</span>
  </div>
);

const TaskList: React.FC<ListProps> = ({
  tasks,
  onEdit,
  onCreate,
  onToggle,
  onDelete,
  isInitialized,
}) => {
  const [filter, setFilter] = useState<Filter>('all');
  const [newTaskTitle, setNewTaskTitle] = useState<string>('');

  const taskList = !tasks
    ? null
    : tasks
        .filter((task) => (filter === 'all' ? true : !task.done))
        .map((task) => (
          <TaskItem
            key={task._id}
            task={task}
            onEdit={onEdit}
            onToggle={onToggle}
            onDelete={onDelete}
          />
        ));

  const handleCreate = () => {
    if (newTaskTitle === '') return;
    onCreate(newTaskTitle);
    setNewTaskTitle('');
  };

  const deleteCompleted = () => {
    tasks?.filter((task) => task.done).forEach((task) => onDelete(task));
  };

  // Pretty view to show when the list is empty
  const listFiller = () => {
    if (tasks?.length !== 0) {
      return null;
    }

    return (
      <div className="flex-grow bg-white shadow-md flex flex-col items-center justify-center text-gray-500 space-y-4 py-8">
        <svg
          className="w-24 h-24 text-gray-300"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={1}
            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
          />
        </svg>
        <h3 className="text-xl font-medium bg-gradient-to-r from-blue-400 to-blue-600 bg-clip-text text-transparent">
          No tasks yet
        </h3>
        <p className="text-gray-400 text-center max-w-sm">
          Add your first task using the input field below. Your tasks will sync
          across all your devices.
        </p>
      </div>
    );
  };

  return (
    <div className="w-full mt-8 max-w-2xl flex flex-col h-[calc(100vh-300px)] px-4">
      {/* Header/Control Panel */}
      <div className="bg-white shadow-lg rounded-t-lg">
        <div className="flex justify-between items-center px-4 py-3 text-sm text-gray-500 border-b border-gray-200">
          {!isInitialized ? (
            <Spinner />
          ) : (
            <span>{tasks?.filter((t) => !t.done).length} items left</span>
          )}

          <div className="space-x-2">
            <button
              disabled={!isInitialized}
              onClick={() => setFilter('all')}
              className={`px-2 py-1 rounded border ${
                filter === 'all'
                  ? 'border-gray-300 bg-gray-50'
                  : 'border-transparent hover:border-gray-300'
              }`}
            >
              All
            </button>
            <button
              disabled={!isInitialized}
              onClick={() => setFilter('active')}
              className={`px-2 py-1 rounded border ${
                filter === 'active'
                  ? 'border-gray-300 bg-gray-50'
                  : 'border-transparent hover:border-gray-300'
              }`}
            >
              Active
            </button>
          </div>
          <button
            disabled={!isInitialized}
            className={
              isInitialized
                ? 'hover:underline hover:text-red-600'
                : 'text-gray-400 cursor-not-allowed'
            }
            onClick={deleteCompleted}
          >
            Delete completed
          </button>
        </div>
      </div>

      {listFiller()}

      {/* Task List */}
      <div className="bg-white shadow-md overflow-y-auto">{taskList}</div>

      {/* New Task Input */}
      <div className="bg-white shadow-md rounded-b-lg flex focus-within:ring-2 focus-within:ring-blue-500 focus-within:border-transparent pt-">
        <input
          type="text"
          placeholder="What needs to be done?"
          disabled={!isInitialized}
          className="flex-grow px-4 py-3 rounded-bl-lg border-r border-gray-200 focus:outline-none"
          value={newTaskTitle}
          onChange={(e) => setNewTaskTitle(e.target.value)}
          onKeyDown={(e) => {
            if (e.key !== 'Enter') return;
            handleCreate();
          }}
        />
        <button
          disabled={!isInitialized}
          // className="px-4 py-3 bg-blue-500 text-white font-medium transition-all duration-300 hover:from-blue-500 hover:via-blue-300 hover:to-blue-400 drop-shadow-[0_1.2px_1.2px_rgba(0,0,0,0.2)] rounded-br-lg hover:bg-blue-600 hover:drop-shadow-md"
          className={`px-4 py-3 bg-blue-500 text-white font-medium transition-all duration-300 rounded-br-lg ${
            isInitialized
              ? 'hover:bg-blue-600 hover:drop-shadow-md'
              : 'bg-gray-400 cursor-not-allowed'
          }`}
          onClick={handleCreate}
        >
          Add Task
        </button>
      </div>
    </div>
  );
};

export default TaskList;
