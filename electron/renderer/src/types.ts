export interface ElectronAPI {
  createTask: (title: string) => Promise<{ success: boolean; error?: string }>;
  editTask: (
    id: string,
    title: string
  ) => Promise<{ success: boolean; error?: string }>;
  toggleTask: (
    id: string,
    done: boolean
  ) => Promise<{ success: boolean; error?: string }>;
  deleteTask: (id: string) => Promise<{ success: boolean; error?: string }>;
  toggleSync: () => Promise<{
    success: boolean;
    syncActive?: boolean;
    error?: string;
  }>;
  getDittoState: () => Promise<{
    isInitialized: boolean;
    appId?: string;
    token?: string;
    syncActive?: boolean;
  }>;
  getTasks: () => Promise<{ success: boolean; tasks: Task[]; error?: string }>;
  onTasksUpdated: (callback: (event: any, tasks: Task[]) => void) => void;
  onDittoInitialized: (
    callback: (event: any, data: { appId: string; token: string }) => void
  ) => void;
  onDittoError: (callback: (event: any, error: string) => void) => void;
  removeAllListeners: (channel: string) => void;
}

export interface Task {
  _id: string;
  title: string;
  done: boolean;
  deleted: boolean;
}

declare global {
  interface Window {
    electronAPI: ElectronAPI;
  }
}
