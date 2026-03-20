using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using DittoSDK;

namespace Taskapp.WinForms.Net48
{
    /// <summary>
    /// Singleton service that wraps TasksPeer instance and provides application-wide access
    /// </summary>
    public sealed class TasksPeerService : IDisposable
    {
        private static readonly Lazy<TasksPeerService> _instance =
            new Lazy<TasksPeerService>(() => new TasksPeerService());

        public static TasksPeerService Instance => _instance.Value;

        private TasksPeer _tasksPeer;
        private DittoStoreObserver _observer;
        private bool _isInitialized;

        private TasksPeerService()
        {
        }

        /// <summary>
        /// Initializes the TasksPeer with configuration values
        /// </summary>
        public async Task InitializeAsync(string appId, string playgroundToken, string authUrl, string websocketUrl)
        {
            if (_isInitialized)
                throw new InvalidOperationException("TasksPeerService is already initialized");

            _tasksPeer = await TasksPeer.Create(appId, playgroundToken, authUrl, websocketUrl);
            _isInitialized = true;
        }

        /// <summary>
        /// Gets whether the service has been initialized
        /// </summary>
        public bool IsInitialized => _isInitialized;

        /// <summary>
        /// Gets whether sync is active
        /// </summary>
        public bool IsSyncActive => _tasksPeer?.IsSyncActive ?? false;

        /// <summary>
        /// Adds a new task
        /// </summary>
        public async Task AddTaskAsync(string title)
        {
            EnsureInitialized();
            await _tasksPeer.AddTask(title);
        }

        /// <summary>
        /// Updates a task's title
        /// </summary>
        public async Task UpdateTaskTitleAsync(string taskId, string newTitle)
        {
            EnsureInitialized();
            await _tasksPeer.UpdateTaskTitle(taskId, newTitle);
        }

        /// <summary>
        /// Marks a task as deleted
        /// </summary>
        public async Task DeleteTaskAsync(string taskId)
        {
            EnsureInitialized();
            await _tasksPeer.DeleteTask(taskId);
        }

        /// <summary>
        /// Updates a task's done status
        /// </summary>
        public async Task UpdateTaskDoneAsync(string taskId, bool isDone)
        {
            EnsureInitialized();
            await _tasksPeer.UpdateTaskDone(taskId, isDone);
        }

        /// <summary>
        /// Registers an observer to watch for task collection changes
        /// </summary>
        public DittoStoreObserver ObserveTasksCollection(Func<IList<ToDoTask>, Task> handler)
        {
            EnsureInitialized();
            _observer = _tasksPeer.ObserveTasksCollection(handler);
            return _observer;
        }

        /// <summary>
        /// Ensures the service has been initialized
        /// </summary>
        private void EnsureInitialized()
        {
            if (!_isInitialized)
                throw new InvalidOperationException("TasksPeerService has not been initialized. Call InitializeAsync first.");
        }

        public void Dispose()
        {
            if (_observer != null)
            {
                _observer.Cancel();
                _observer = null;
            }

            if (_tasksPeer != null)
            {
                _tasksPeer.Dispose();
                _tasksPeer = null;
            }

            _isInitialized = false;
        }
    }
}
