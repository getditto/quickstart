import React, { useState } from 'react';
import { Task } from '../types';

interface TaskListProps {
  tasks: Task[];
  onCreate: (title: string) => void;
  onEdit: (id: string, title: string) => void;
  onToggle: (task: Task) => void;
  onDelete: (task: Task) => void;
  isInitialized: boolean;
}

const TaskList: React.FC<TaskListProps> = ({
  tasks,
  onCreate,
  onEdit,
  onToggle,
  onDelete,
  isInitialized,
}) => {
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [editingTask, setEditingTask] = useState<Task | null>(null);
  const [editTitle, setEditTitle] = useState('');

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (newTaskTitle.trim()) {
      onCreate(newTaskTitle.trim());
      setNewTaskTitle('');
    }
  };

  const handleEditTask = (task: Task) => {
    setEditingTask(task);
    setEditTitle(task.title);
  };

  const handleSaveEdit = (e: React.FormEvent) => {
    e.preventDefault();
    if (editingTask && editTitle.trim()) {
      onEdit(editingTask._id, editTitle.trim());
      setEditingTask(null);
      setEditTitle('');
    }
  };

  const handleCancelEdit = () => {
    setEditingTask(null);
    setEditTitle('');
  };

  return (
    <div className="w-full max-w-4xl mx-auto p-6 bg-white rounded-lg shadow-md">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-xl font-semibold text-gray-800">Tasks</h2>
        <span className="text-sm text-gray-500">
          {tasks.length} task{tasks.length !== 1 ? 's' : ''}
        </span>
      </div>

      {/* New Task Form */}
      <form onSubmit={handleCreateTask} className="mb-6">
        <div className="flex gap-2">
          <input
            type="text"
            value={newTaskTitle}
            onChange={(e) => setNewTaskTitle(e.target.value)}
            placeholder="Enter a new task..."
            disabled={!isInitialized}
            className="flex-1 px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={!isInitialized || !newTaskTitle.trim()}
            className="px-6 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            Add Task
          </button>
        </div>
      </form>

      {/* Tasks List */}
      <div className="space-y-3">
        {tasks.length === 0 ? (
          <div className="text-center py-12 text-gray-500">
            {isInitialized
              ? 'No tasks yet. Create your first task above!'
              : 'Connecting to Ditto...'}
          </div>
        ) : (
          tasks.map((task) => (
            <div
              key={task._id}
              className={`flex items-center gap-3 p-3 border rounded-md ${
                task.done
                  ? 'bg-gray-50 border-gray-200'
                  : 'bg-white border-gray-300'
              }`}
            >
              {/* Toggle Button */}
              <button
                onClick={() => onToggle(task)}
                disabled={!isInitialized}
                className={`w-5 h-5 rounded-full border-2 flex items-center justify-center ${
                  task.done
                    ? 'bg-green-500 border-green-500'
                    : 'border-gray-300 hover:border-green-500'
                } disabled:opacity-50`}
              >
                {task.done && (
                  <svg
                    className="w-3 h-3 text-white"
                    fill="currentColor"
                    viewBox="0 0 20 20"
                  >
                    <path
                      fillRule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                )}
              </button>

              {/* Task Content */}
              <div className="flex-1">
                {editingTask?._id === task._id ? (
                  <form onSubmit={handleSaveEdit} className="flex gap-2">
                    <input
                      type="text"
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      className="flex-1 px-2 py-1 border border-gray-300 rounded"
                      autoFocus
                    />
                    <button
                      type="submit"
                      className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
                    >
                      Save
                    </button>
                    <button
                      type="button"
                      onClick={handleCancelEdit}
                      className="px-3 py-1 bg-gray-500 text-white rounded hover:bg-gray-600"
                    >
                      Cancel
                    </button>
                  </form>
                ) : (
                  <span
                    className={`${
                      task.done ? 'line-through text-gray-500' : 'text-gray-800'
                    }`}
                  >
                    {task.title}
                  </span>
                )}
              </div>

              {/* Action Buttons */}
              {editingTask?._id !== task._id && (
                <div className="flex gap-2">
                  <button
                    onClick={() => handleEditTask(task)}
                    disabled={!isInitialized}
                    className="text-blue-500 hover:text-blue-700 disabled:opacity-50"
                    title="Edit task"
                  >
                    <svg
                      className="w-4 h-4"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
                      />
                    </svg>
                  </button>
                  <button
                    onClick={() => onDelete(task)}
                    disabled={!isInitialized}
                    className="text-red-500 hover:text-red-700 disabled:opacity-50"
                    title="Delete task"
                  >
                    <svg
                      className="w-4 h-4"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                      />
                    </svg>
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default TaskList;
