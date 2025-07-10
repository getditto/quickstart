const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');
const { Ditto, IdentityOnlinePlayground, init } = require('@dittolive/ditto');

// Load environment variables
require('dotenv').config({ path: path.join(__dirname, '..', '.env') });

let mainWindow;
let ditto = null;
let tasksSubscription = null;
let tasksObserver = null;

// Ditto configuration
const identity = {
  type: 'onlinePlayground',
  appID: process.env.DITTO_APP_ID,
  token: process.env.DITTO_PLAYGROUND_TOKEN,
  customAuthURL: process.env.DITTO_AUTH_URL,
  enableDittoCloudSync: false,
};

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1200,
    height: 800,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
    icon: path.join(__dirname, 'assets', 'icon.png'), // Optional: add app icon
  });

  // Load the renderer app
  const isDev = process.env.NODE_ENV === 'development';
  if (isDev) {
    mainWindow.loadURL('http://localhost:5173');
    mainWindow.webContents.openDevTools();
  } else {
    mainWindow.loadFile(path.join(__dirname, 'dist', 'index.html'));
  }

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// Initialize Ditto
async function initializeDitto() {
  try {
    await init();
    ditto = new Ditto(identity);
    
    // Configure transport
    ditto.updateTransportConfig((config) => {
      config.connect.websocketURLs = [process.env.DITTO_WEBSOCKET_URL];
      return config;
    });

    // Disable sync with v3 peers, required for DQL
    await ditto.disableSyncWithV3();
    ditto.startSync();

    // Register subscription for tasks
    tasksSubscription = ditto.sync.registerSubscription('SELECT * FROM tasks');

    // Register observer for tasks
    tasksObserver = ditto.store.registerObserver(
      'SELECT * FROM tasks WHERE deleted=false ORDER BY done',
      (results) => {
        const tasks = results.items.map((item) => item.value);
        if (mainWindow) {
          mainWindow.webContents.send('tasks-updated', tasks);
        }
      }
    );

    console.log('Ditto initialized successfully');
    if (mainWindow) {
      mainWindow.webContents.send('ditto-initialized', { 
        appId: identity.appID,
        token: identity.token
      });
    }
  } catch (error) {
    console.error('Failed to initialize Ditto:', error);
    if (mainWindow) {
      mainWindow.webContents.send('ditto-error', error.message);
    }
  }
}

// IPC handlers for task operations
ipcMain.handle('create-task', async (event, title) => {
  try {
    await ditto.store.execute('INSERT INTO tasks DOCUMENTS (:task)', {
      task: {
        title,
        done: false,
        deleted: false,
      },
    });
    return { success: true };
  } catch (error) {
    console.error('Failed to create task:', error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle('edit-task', async (event, id, title) => {
  try {
    await ditto.store.execute('UPDATE tasks SET title=:title WHERE _id=:id', {
      id,
      title,
    });
    return { success: true };
  } catch (error) {
    console.error('Failed to edit task:', error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle('toggle-task', async (event, id, done) => {
  try {
    await ditto.store.execute('UPDATE tasks SET done=:done WHERE _id=:id', {
      id,
      done: !done,
    });
    return { success: true };
  } catch (error) {
    console.error('Failed to toggle task:', error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle('delete-task', async (event, id) => {
  try {
    await ditto.store.execute('UPDATE tasks SET deleted=true WHERE _id=:id', {
      id,
    });
    return { success: true };
  } catch (error) {
    console.error('Failed to delete task:', error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle('toggle-sync', async (event) => {
  try {
    if (ditto) {
      const isRunning = ditto.isSyncActive;
      if (isRunning) {
        ditto.stopSync();
      } else {
        ditto.startSync();
      }
      return { success: true, syncActive: !isRunning };
    }
    return { success: false, error: 'Ditto not initialized' };
  } catch (error) {
    console.error('Failed to toggle sync:', error);
    return { success: false, error: error.message };
  }
});

// App event handlers
app.whenReady().then(() => {
  createWindow();
  initializeDitto();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    // Clean up Ditto resources
    if (tasksObserver) {
      tasksObserver.cancel();
    }
    if (tasksSubscription) {
      tasksSubscription.cancel();
    }
    if (ditto) {
      ditto.close();
    }
    app.quit();
  }
});

app.on('before-quit', () => {
  // Clean up Ditto resources
  if (tasksObserver) {
    tasksObserver.cancel();
  }
  if (tasksSubscription) {
    tasksSubscription.cancel();
  }
  if (ditto) {
    ditto.close();
  }
});