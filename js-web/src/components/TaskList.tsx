import React, { useState } from 'react';
import { Task } from '../App';

type ItemProps = {
  task: Task,
  onToggle: (task: Task) => void,
  onDelete: (task: Task) => void,
}

const TaskItem: React.FC<ItemProps> = ({ task, onToggle, onDelete }) => {
  return (
    <div className='group flex items-center p-4 border-b border-gray-200 hover:bg-gray-50'>
      <input
        type="checkbox"
        checked={task.done}
        className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 mr-4"
        readOnly
        onChange={() => onToggle(task)}
      />
      <span className={`flex-grow text-gray-700 ${task.done ? 'line-through text-gray-400' : ''}`}>
        {task.title}
      </span>
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
};

type ListProps = {
  tasks: Task[],
  onCreate: (title: string) => void,
  onToggle: (task: Task) => void,
  onDelete: (task: Task) => void,
}

type Filter = "all" | "done";

const TaskList: React.FC<ListProps> = ({ tasks, onCreate, onToggle, onDelete }) => {
  const [filter, setFilter] = useState<Filter>("all");
  const [newTaskTitle, setNewTaskTitle] = useState<string>("");

  const taskList = tasks
    .filter((task) => filter === "all" ? true : !task.done)
    .map((task) => (
      <TaskItem key={task.id} task={task} onToggle={onToggle} onDelete={onDelete} />
    ));

  const handleCreate = () => {
    console.log("HandleCreate", newTaskTitle);
    if (newTaskTitle === "") return;
    onCreate(newTaskTitle);
    setNewTaskTitle("");
  };

  return (
    <div className='w-full mt-8 max-w-2xl flex flex-col h-[calc(100vh-250px)] px-4'>
      {/* Header/Control Panel */}
      <div className='bg-white shadow-lg rounded-t-lg'>
        <div className='flex justify-between items-center px-4 py-3 text-sm text-gray-500 border-b border-gray-200'>
          <span>{tasks.filter(t => !t.done).length} items left</span>
          <div className='space-x-2'>
            <button onClick={() => setFilter("all")} className='px-2 py-1 rounded hover:border-gray-300 border border-transparent'>All</button>
            <button onClick={() => setFilter("done")} className='px-2 py-1 rounded hover:border-gray-300 border border-transparent'>Active</button>
          </div>
          <button className='hover:underline'>Clear completed</button>
        </div>
      </div>

      {/* Task List */}
      <div className='bg-white shadow-md overflow-y-auto'>
        {taskList}
      </div>

      {/* New Task Input */}
      <div className='bg-white shadow-md rounded-b-lg flex border border-t-1'>
        <input
          type="text"
          placeholder="What needs to be done?"
          className="flex-grow px-4 py-3 border-b rounded-bl-lg border-gray-200 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          value={newTaskTitle}
          onChange={(e) => setNewTaskTitle(e.target.value)}
          onKeyDown={(e) => {
            if (e.key !== 'Enter') return;
            handleCreate();
          }}
        />
        <button
          className="px-4 py-3 bg-blue-500 text-white font-medium transition-all duration-300 hover:from-blue-500 hover:via-blue-300 hover:to-blue-400 drop-shadow-[0_1.2px_1.2px_rgba(0,0,0,0.2)] rounded-br-lg hover:bg-blue-600 hover:drop-shadow-md"
          onClick={handleCreate}
        >
          Add Task
        </button>
      </div>

    </div>
  );
};

export default TaskList;
