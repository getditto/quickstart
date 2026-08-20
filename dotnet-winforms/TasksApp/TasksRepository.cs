using System;
using System.Collections.Generic;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;

using DittoSDK.Store;

namespace DittoTasksApp
{
    /// <summary>
    /// Owns all 'tasks' collection data operations: the DQL query strings, the sync
    /// subscription, the store observer, and CRUD. It calls the real Ditto API
    /// directly through the <see cref="DittoManager"/> it is given.
    /// </summary>
    public class TasksRepository
    {
        // The subscription controls what syncs to this device and is written to
        // the local database; the observer reacts to changes in that local
        // database, hiding soft-deleted tasks from the displayed list.
        private const string SubscriptionQuery = "SELECT * FROM tasks";
        private const string ObserverQuery = "SELECT * FROM tasks WHERE NOT deleted";

        private readonly DittoManager _dittoManager;

        public TasksRepository(DittoManager dittoManager)
        {
            _dittoManager = dittoManager;
            RegisterSubscription();
        }

        /// <summary>
        /// Registers a subscription for the tasks collection to enable data synchronization
        /// with other peers and the Ditto server. This subscription determines what data
        /// will be synced to this device during the synchronization process.
        /// </summary>
        /// <remarks>
        /// The subscription syncs all tasks (<c>SELECT * FROM tasks</c>) and controls
        /// what is written to the local database; the observer query hides
        /// soft-deleted tasks from the displayed list.
        /// </remarks>
        /// <seealso href="https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions"/>
        private void RegisterSubscription()
        {
            // Register a subscription, which determines what data syncs to this device
            // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
            _dittoManager.Ditto.Sync.RegisterSubscription(SubscriptionQuery);
        }

        /// <summary>
        /// Adds a new task to the 'tasks' collection.
        /// </summary>
        public async Task AddTask(string title)
        {
            if (string.IsNullOrWhiteSpace(title))
            {
                throw new ArgumentException("title cannot be empty");
            }

            var doc = new Dictionary<string, object>
            {
                {"title", title},
                {"done", false},
                {"deleted", false }
            };
            const string insertCommand = "INSERT INTO tasks DOCUMENTS (:task)";
            await _dittoManager.Ditto.Store.ExecuteAsync(insertCommand, new Dictionary<string, object>()
            {
                { "task", doc }
            });
        }

        /// <summary>
        /// Update the title of an existing task.
        /// </summary>
        public async Task UpdateTaskTitle(string taskId, string newTitle)
        {
            if (string.IsNullOrWhiteSpace(taskId))
            {
                throw new ArgumentException("taskId cannot be empty");
            }

            if (string.IsNullOrWhiteSpace(newTitle))
            {
                throw new ArgumentException("title cannot be empty");
            }

            const string updateQuery = "UPDATE tasks SET title = :title WHERE _id = :id";
            await _dittoManager.Ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
            {
                {"title", newTitle},
                {"id", taskId}
            });
        }

        /// <summary>
        /// Mark a task as deleted.
        /// </summary>
        public async Task DeleteTask(string taskId)
        {
            if (string.IsNullOrWhiteSpace(taskId))
            {
                throw new ArgumentException("taskId cannot be empty");
            }

            const string updateQuery = "UPDATE tasks SET deleted = true WHERE _id = :id";
            await _dittoManager.Ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
            {
                { "id", taskId }
            });
        }

        /// <summary>
        /// Mark a task as complete or not complete.
        /// </summary>
        public async Task UpdateTaskDone(string taskId, bool newDoneState)
        {
            const string updateQuery = "UPDATE tasks SET done = :done WHERE _id = :id";
            await _dittoManager.Ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>
            {
                { "done", newDoneState },
                { "id", taskId }
            });
        }

        /// <summary>
        /// Specify a handler to be called asynchronously when the task collection changes.
        /// </summary>
        public DittoStoreObserver ObserveTasksCollection(Func<IList<TaskModel>, Task> handler)
        {
            // Register an observer, which runs against the local database on this device
            // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
            return _dittoManager.Ditto.Store.RegisterObserver(ObserverQuery, async (queryResult) =>
            {
                try
                {
                    // Deserialize the JSON documents into TaskModel objects
                    var tasks = queryResult.Items.Select(d =>
                        JsonSerializer.Deserialize<TaskModel>(d.JsonString())
                    ).ToList();

                    await handler(tasks);
                }
                catch (Exception e)
                {
                    Console.Error.WriteLine($"ERROR tasks observation handler failed: {e.Message}");
                }
            });
        }
    }
}
