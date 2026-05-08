export type Task = {
  _id: string;
  title: string;
  done: boolean;
  deleted: boolean;
};

export type DittoIdentity = {
  appId: string;
  token: string;
};

export type DittoApi = {
  getInfo: () => Promise<DittoIdentity>;
  getTasks: () => Promise<Task[]>;
  createTask: (title: string) => Promise<void>;
  editTask: (id: string, title: string) => Promise<void>;
  toggleTask: (id: string, done: boolean) => Promise<void>;
  deleteTask: (id: string) => Promise<void>;
  startSync: () => Promise<void>;
  stopSync: () => Promise<void>;
  onTasksUpdated: (callback: (tasks: Task[]) => void) => () => void;
};

export const IPC = {
  TASKS_GET_INFO: 'tasks:getInfo',
  TASKS_GET: 'tasks:get',
  TASKS_CREATE: 'tasks:create',
  TASKS_EDIT: 'tasks:edit',
  TASKS_TOGGLE: 'tasks:toggle',
  TASKS_DELETE: 'tasks:delete',
  TASKS_START_SYNC: 'tasks:startSync',
  TASKS_STOP_SYNC: 'tasks:stopSync',
  TASKS_UPDATED: 'tasks:updated',
} as const;
