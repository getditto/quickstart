const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods that allow the renderer process to use
// the ipcRenderer without exposing the entire object
contextBridge.exposeInMainWorld('electronAPI', {
  // Task operations
  createTask: (title) => ipcRenderer.invoke('create-task', title),
  editTask: (id, title) => ipcRenderer.invoke('edit-task', id, title),
  toggleTask: (id, done) => ipcRenderer.invoke('toggle-task', id, done),
  deleteTask: (id) => ipcRenderer.invoke('delete-task', id),
  
  // Sync operations
  toggleSync: () => ipcRenderer.invoke('toggle-sync'),
  
  // Get current state
  getDittoState: () => ipcRenderer.invoke('get-ditto-state'),
  
  // Event listeners
  onTasksUpdated: (callback) => ipcRenderer.on('tasks-updated', callback),
  onDittoInitialized: (callback) => ipcRenderer.on('ditto-initialized', callback),
  onDittoError: (callback) => ipcRenderer.on('ditto-error', callback),
  
  // Remove listeners
  removeAllListeners: (channel) => ipcRenderer.removeAllListeners(channel),
});