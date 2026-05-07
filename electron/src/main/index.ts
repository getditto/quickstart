import { app, BrowserWindow, ipcMain } from 'electron';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { assertEnv } from './env';
import {
  createTask,
  deleteTask,
  editTask,
  getInfo,
  initDitto,
  shutdown,
  startSync,
  stopSync,
  toggleTask,
} from './ditto';
import { IPC } from '../types';
import type { Task } from '../types';

const __dirname = dirname(fileURLToPath(import.meta.url));

function broadcastTasks(tasks: Task[]): void {
  for (const win of BrowserWindow.getAllWindows()) {
    win.webContents.send(IPC.TASKS_UPDATED, tasks);
  }
}

function createWindow(): BrowserWindow {
  const win = new BrowserWindow({
    width: 800,
    height: 700,
    title: 'Ditto Tasks',
    backgroundColor: '#f3f4f6',
    webPreferences: {
      preload: resolve(__dirname, '../preload/index.mjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  });

  if (process.env.ELECTRON_RENDERER_URL) {
    win.loadURL(process.env.ELECTRON_RENDERER_URL);
  } else {
    win.loadFile(resolve(__dirname, '../renderer/index.html'));
  }

  return win;
}

ipcMain.handle(IPC.TASKS_GET_INFO, () => getInfo());
ipcMain.handle(IPC.TASKS_CREATE, (_event, title: string) => createTask(title));
ipcMain.handle(IPC.TASKS_EDIT, (_event, id: string, title: string) =>
  editTask(id, title),
);
ipcMain.handle(IPC.TASKS_TOGGLE, (_event, id: string, done: boolean) =>
  toggleTask(id, done),
);
ipcMain.handle(IPC.TASKS_DELETE, (_event, id: string) => deleteTask(id));
ipcMain.handle(IPC.TASKS_START_SYNC, () => startSync());
ipcMain.handle(IPC.TASKS_STOP_SYNC, () => stopSync());

app.whenReady().then(async () => {
  try {
    assertEnv();
    await initDitto(broadcastTasks);
  } catch (e) {
    console.error('Startup failed:', e);
    app.quit();
    return;
  }

  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', async () => {
  await shutdown();
  if (process.platform !== 'darwin') app.quit();
});

app.on('before-quit', async () => {
  await shutdown();
});
