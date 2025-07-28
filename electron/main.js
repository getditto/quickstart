const { app, BrowserWindow, ipcMain } = require("electron");
const path = require("path");
const { Ditto, IdentityOnlinePlayground, init } = require("@dittolive/ditto");

// Load environment variables
require("dotenv").config({ path: path.join(__dirname, "..", ".env") });

let mainWindow;
let ditto = null;
let tasksSubscription = null;
let tasksObserver = null;

// Ditto configuration
const identity = {
  type: "onlinePlayground",
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
      preload: path.join(__dirname, "preload.js"),
    },
    icon: path.join(__dirname, "assets", "icon.png"), // Optional: add app icon
  });

  // Load the renderer app
  const isDev = process.env.NODE_ENV === "development";
  if (isDev) {
    mainWindow.loadURL("http://localhost:5173");
    mainWindow.webContents.openDevTools();
  } else {
    mainWindow.loadFile(path.join(__dirname, "dist", "index.html"));
  }

  // Send initialization data when the window is ready
  mainWindow.webContents.once("did-finish-load", async () => {
    if (ditto) {
      mainWindow.webContents.send("ditto-initialized", {
        appId: identity.appID,
        token: identity.token,
      });

      // Send initial tasks
      try {
        const result = await ditto.store.execute(
          "SELECT * FROM tasks WHERE deleted=false ORDER BY done",
        );
        const tasks = result.items.map((item) => item.value);
        mainWindow.webContents.send("tasks-updated", tasks);
      } catch (error) {
        console.error("Failed to fetch initial tasks for window:", error);
      }
    }
  });

  mainWindow.on("closed", () => {
    mainWindow = null;
  });
}

// Initialize Ditto
async function initializeDitto() {
  try {
    await init();
    ditto = new Ditto(identity);

    // Configure transport
    // https://docs.ditto.live/sdk/latest/sync/customizing-transport-configurations#enabling-and-disabling-transports
    ditto.updateTransportConfig((config) => {
      config.connect.websocketURLs = [process.env.DITTO_WEBSOCKET_URL];
      return config;
    });

    // Disable sync with v3 peers, required for DQL
    await ditto.disableSyncWithV3();

    // Disable DQL strict mode
    // when set to false, collection definitions are no longer required. SELECT queries will return and display all fields by default.
    // https://docs.ditto.live/dql/strict-mode
    try {
      await ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false");
    } catch (error) {
      console.error("Failed to disable DQL strict mode:", error);
      if (mainWindow) {
        mainWindow.webContents.send("ditto-error", "Failed to disable DQL strict mode: " + error.message);
      }
      throw error; // Re-throw the error to ensure it is handled by the outer try-catch block
    }

    ditto.startSync();

    // Register subscription for tasks
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    tasksSubscription = ditto.sync.registerSubscription("SELECT * FROM tasks");

    // Register observer for tasks
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    tasksObserver = ditto.store.registerObserver(
      "SELECT * FROM tasks WHERE deleted=false ORDER BY done",
      (results) => {
        const tasks = results.items.map((item) => item.value);
        if (mainWindow) {
          mainWindow.webContents.send("tasks-updated", tasks);
        }
      },
    );

    console.log("Ditto initialized successfully");
    if (mainWindow) {
      mainWindow.webContents.send("ditto-initialized", {
        appId: identity.appID,
        token: identity.token,
      });
    }
  } catch (error) {
    console.error("Failed to initialize Ditto:", error);
    if (mainWindow) {
      mainWindow.webContents.send("ditto-error", error.message);
    }
  }
}

// IPC handler to get current tasks
ipcMain.handle("get-tasks", async () => {
  if (ditto) {
    try {
      const result = await ditto.store.execute(
        "SELECT * FROM tasks WHERE deleted=false ORDER BY done",
      );
      return { success: true, tasks: result.items.map((item) => item.value) };
    } catch (error) {
      console.error("Failed to fetch tasks:", error);
      return { success: false, error: error.message, tasks: [] };
    }
  }
  return { success: false, error: "Ditto not initialized", tasks: [] };
});

// IPC handler to get current Ditto state
ipcMain.handle("get-ditto-state", async () => {
  if (ditto) {
    return {
      isInitialized: true,
      appId: identity.appID,
      token: identity.token,
      syncActive: ditto.isSyncActive,
    };
  }
  return { isInitialized: false };
});

// IPC handlers for task operations
ipcMain.handle("create-task", async (event, title) => {
  try {
    await ditto.store.execute("INSERT INTO tasks DOCUMENTS (:task)", {
      task: {
        title,
        done: false,
        deleted: false,
      },
    });
    return { success: true };
  } catch (error) {
    console.error("Failed to create task:", error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle("edit-task", async (event, id, title) => {
  try {
    await ditto.store.execute("UPDATE tasks SET title=:title WHERE _id=:id", {
      id,
      title,
    });
    return { success: true };
  } catch (error) {
    console.error("Failed to edit task:", error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle("toggle-task", async (event, id, done) => {
  try {
    await ditto.store.execute("UPDATE tasks SET done=:done WHERE _id=:id", {
      id,
      done: !done,
    });
    return { success: true };
  } catch (error) {
    console.error("Failed to toggle task:", error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle("delete-task", async (event, id) => {
  try {
    await ditto.store.execute("UPDATE tasks SET deleted=true WHERE _id=:id", {
      id,
    });
    return { success: true };
  } catch (error) {
    console.error("Failed to delete task:", error);
    return { success: false, error: error.message };
  }
});

ipcMain.handle("toggle-sync", async (event) => {
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
    return { success: false, error: "Ditto not initialized" };
  } catch (error) {
    console.error("Failed to toggle sync:", error);
    return { success: false, error: error.message };
  }
});

// App event handlers
app.whenReady().then(() => {
  createWindow();
  initializeDitto();

  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
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

app.on("before-quit", () => {
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
