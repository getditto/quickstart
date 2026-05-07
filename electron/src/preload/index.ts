import { contextBridge, ipcRenderer } from 'electron';
import type { IpcRendererEvent } from 'electron';
import { IPC } from '../types';
import type { DittoApi, DittoIdentity, Task } from '../types';

const api: DittoApi = {
  getInfo: (): Promise<DittoIdentity> => ipcRenderer.invoke(IPC.TASKS_GET_INFO),
  getTasks: (): Promise<Task[]> => ipcRenderer.invoke(IPC.TASKS_GET),
  createTask: (title: string): Promise<void> =>
    ipcRenderer.invoke(IPC.TASKS_CREATE, title),
  editTask: (id: string, title: string): Promise<void> =>
    ipcRenderer.invoke(IPC.TASKS_EDIT, id, title),
  toggleTask: (id: string, done: boolean): Promise<void> =>
    ipcRenderer.invoke(IPC.TASKS_TOGGLE, id, done),
  deleteTask: (id: string): Promise<void> =>
    ipcRenderer.invoke(IPC.TASKS_DELETE, id),
  startSync: (): Promise<void> => ipcRenderer.invoke(IPC.TASKS_START_SYNC),
  stopSync: (): Promise<void> => ipcRenderer.invoke(IPC.TASKS_STOP_SYNC),
  onTasksUpdated: (callback) => {
    const listener = (_event: IpcRendererEvent, tasks: Task[]) =>
      callback(tasks);
    ipcRenderer.on(IPC.TASKS_UPDATED, listener);
    return () => {
      ipcRenderer.off(IPC.TASKS_UPDATED, listener);
    };
  },
};

contextBridge.exposeInMainWorld('ditto', api);
