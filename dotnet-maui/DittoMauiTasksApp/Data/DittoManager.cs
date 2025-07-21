using DittoSDK;
using Microsoft.Extensions.Logging;
using System.Text.Json;
using System.Collections.ObjectModel;
using DittoMauiTasksApp.Model;

namespace DittoMauiTasksApp.Data;

/// <summary>
/// Manages Ditto database operations for the tasks application, including CRUD operations,
/// data synchronization, and real-time data observation. This class serves as the primary
/// data access layer for task management functionality.
/// </summary>

public class DittoManager(ILogger<DittoManager> logger)
    : IDataManager
{
    private readonly ILogger<DittoManager> _logger = logger;
    private const string SelectQuery = "SELECT * FROM tasks WHERE NOT deleted";

#nullable enable
    private Ditto? _ditto = null;
    private DittoStoreObserver? _dittoStoreObserver = null;
    private DittoSyncSubscription? _syncSubscription = null;

    /// <summary>
    /// DittoDiffer instance used to calculate the delta changes between syncs.
    /// This enables efficient UI updates by only processing changed items.
    /// </summary>
    /// <see href="https://docs.ditto.live/sdk/latest/crud/read#diffing-results"/>
    private readonly DittoDiffer _dittoDiffer = new DittoDiffer();
    
    /// <summary>
    /// Cache of the previous query results for diffing operations.
    /// </summary>
    private List<DittoQueryResultItem> _previousItems = [];

    /// <summary>
    /// Initializes the Ditto instance with the specified configuration and data path.
    /// Sets up the Ditto identity, transport configuration, and initializes the database
    /// with default tasks if it's empty.
    /// </summary>
    /// <param name="dittoConfig">The Ditto configuration containing app ID, tokens, and URLs.</param>
    /// <param name="dataPath">The file system path where Ditto should store its data.</param>
    /// <returns>A task that represents the asynchronous initialization operation.</returns>
    /// <exception cref="Exception">Thrown when Ditto initialization fails.</exception>
    public async Task Initialize(DittoConfig dittoConfig, string dataPath)
    {
        try
        {
            _ditto = new Ditto(DittoIdentity
                .OnlinePlayground(
                    dittoConfig.AppId,
                    dittoConfig.PlaygroundToken,
                    false, // This is required to be set to false, to use the correct URLs with Ditto Portal
                    dittoConfig.AuthUrl), dataPath);

            _ditto.UpdateTransportConfig(config =>
            {
                // Add the websocket URL to the transport configuration.
                config.Connect.WebsocketUrls.Add(dittoConfig.WebsocketUrl);
            });

            // disable sync with v3 peers, required for DQL
            _ditto.DisableSyncWithV3();

            await _ditto.Store.ExecuteAsync("ALTER SYSTEM SET DQL_STRICT_MODE = false");
            await InsertInitialTasks();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, ex.Message);
        }    
           
    }

    /// <summary>
    /// Initializes the Ditto instance with the specified configuration using the default
    /// local application data path for data storage.
    /// </summary>
    /// <param name="dittoConfig">The Ditto configuration containing app ID, tokens, and URLs.</param>
    /// <returns>A task that represents the asynchronous initialization operation.</returns>
    public async Task Initialize(DittoConfig dittoConfig)
    {
        var dataPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "ditto");
        await Initialize(dittoConfig, dataPath);
    }

    /// <summary>
    /// Registers a store observer to monitor changes in the tasks collection and automatically
    /// update the provided ObservableCollection. Uses diffing to efficiently update only
    /// changed items, preventing unnecessary UI repaints.
    /// </summary>
    /// <param name="tasks">The ObservableCollection to be updated when task data changes.</param>
    /// <see href="https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers"/>
    /// <see href="https://docs.ditto.live/sdk/latest/crud/read#diffing-results"/>
    public void RegisterObservers(ObservableCollection<DittoTask> tasks)
    {
        // Register observer, which runs against the local database on this peer
        // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
        _dittoStoreObserver = _ditto?.Store.RegisterObserver(SelectQuery, (queryResult) =>
        {
            try
            {
                // .NET specifically with ObservableCollection will repaint the entire list if you just clear it and add new items.
                // Diffing the items allows us to only update the changed items, but at the expense of some memory and CPU usage.
                // This is a trade-off that is acceptable for this example, but may lead to problems on larger datasets.
                // https://docs.ditto.live/sdk/latest/crud/read#diffing-results
                var diff = _dittoDiffer.Diff(queryResult.Items);

                // For testing, we'll execute directly since we're not in a UI context
                try
                {
                    //handle deletions
                    foreach (var index in diff.Deletions)
                    {
                        var item = _previousItems[index];
                        var taskItem = JsonSerializer
                            .Deserialize<DittoTask>(_previousItems[index].JsonString());
                        if (taskItem != null)
                        {
                            tasks.Remove(tasks.First(x => x.Id == taskItem.Id));
                        }
                    }

                    //handle insertions
                    foreach (var index in diff.Insertions)
                    {
                        var item = queryResult.Items[index];
                        var taskItem = JsonSerializer
                            .Deserialize<DittoTask>(item.JsonString());
                        if (taskItem != null)
                        {
                            tasks.Add(taskItem);
                        }
                    }

                    //handle updates
                    foreach (var index in diff.Updates)
                    {
                        var item = queryResult.Items[index];
                        var taskItem = JsonSerializer
                            .Deserialize<DittoTask>(item.JsonString());
                        if (taskItem != null)
                        {
                            var existingTask = tasks.FirstOrDefault(x => x.Id == taskItem.Id);
                            if (existingTask != null)
                            {
                                //implement soft delete for the deleted field
                                if (taskItem.Deleted)
                                {
                                    tasks.Remove(existingTask);
                                }
                                else
                                {
                                    existingTask.Title = taskItem.Title;
                                    existingTask.Done = taskItem.Done;
                                }
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    _logger.LogError($"TasksPageViewModel: Error: Unable to update list view model: {e.Message}");
                }

                _previousItems = queryResult.Items;
            }
            catch (Exception e)
            {
                _logger.LogError($"TasksPageViewModel: Error: Unable to process tasks collection change: {e.Message}");
            }
        });
    }

    /// <summary>
    /// Starts the Ditto synchronization process and registers a subscription for the tasks collection.
    /// This enables real-time data synchronization with other peers and the Ditto cloud.
    /// </summary>
    /// <see href="https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions"/>
    public void StartSync()
    {
        _ditto?.StartSync();

        // Register a subscription, which determines what data syncs to this peer
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
        _syncSubscription = _ditto?.Sync.RegisterSubscription(SelectQuery);
    }

    /// <summary>
    /// Stops the Ditto synchronization process and cancels the active subscription.
    /// This should be called when sync is no longer needed or when the application is shutting down.
    /// </summary>
    public void StopSync()
    {
        _ditto?.StopSync();
        _syncSubscription?.Cancel();
    }

    /// <summary>
    /// Populates the tasks collection with initial seed data if it's empty.
    /// Inserts four predefined tasks using the INITIAL keyword to prevent duplicates.
    /// </summary>
    /// <returns>A task that represents the asynchronous insertion operation.</returns>
    /// <exception cref="Exception">Thrown when the insertion operation fails.</exception>
     public async Task InsertInitialTasks()
     {
        try
        {
            var initialTasks = new List<Dictionary<string, object>>
            {
                new()
                {
                    { "_id", "50191411-4C46-4940-8B72-5F8017A04FA7" },
                    { "title", "Buy groceries" },
                    { "done", false },
                    { "deleted", false }
                },
                new()
                {
                    { "_id", "6DA283DA-8CFE-4526-A6FA-D385089364E5" },
                    { "title", "Clean the kitchen" },
                    { "done", false },
                    { "deleted", false }
                },
                new()
                {
                    { "_id", "5303DDF8-0E72-4FEB-9E82-4B007E5797F0" },
                    { "title", "Schedule dentist appointment" },
                    { "done", false },
                    { "deleted", false }
                },
                new()
                {
                    { "_id", "38411F1B-6B49-4346-90C3-0B16CE97E174" },
                    { "title", "Pay bills" },
                    { "done", false },
                    { "deleted", false }
                }
            };

            const string insertCommand = "INSERT INTO tasks INITIAL DOCUMENTS (:task)";

            if (_ditto != null)
            {
                foreach (var task in initialTasks)
                {
                    await _ditto.Store.ExecuteAsync(insertCommand, new Dictionary<string, object>()
                    {
                        { "task", task }
                    });
                }
            }
        }
        catch (Exception e)
        {
            _logger.LogError($"TasksPageViewModel: Error adding initial tasks: {e.Message}");
        }
    }

    /// <summary>
    /// Adds a new task to the tasks collection in the Ditto store.
    /// </summary>
    /// <param name="document">A dictionary containing the task data with keys: _id, title, done, deleted.</param>
    /// <returns>A task that represents the asynchronous insertion operation.</returns>
    /// <exception cref="Exception">Thrown when the insertion operation fails.</exception>
    public async Task AddTask(Dictionary<string, object> document)
    {
        const string insertCommand = "INSERT INTO tasks DOCUMENTS (:doc)";
        if (_ditto != null)
        {
            await _ditto.Store.ExecuteAsync(insertCommand, new Dictionary<string, object>()
            {
                { "doc", document }
            });
        }
    }

    /// <summary>
    /// Updates the title of an existing task in the Ditto store.
    /// </summary>
    /// <param name="task">The DittoTask object containing the updated title and task ID.</param>
    /// <returns>A task that represents the asynchronous update operation.</returns>
    /// <exception cref="Exception">Thrown when the update operation fails.</exception>
    public async Task EditTask(DittoTask task)
    {
        if (_ditto != null)
        {
            const string updateQuery = "UPDATE tasks SET title = :title WHERE _id = :id";
            await _ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
            {
                { "title", task.Title },
                { "id", task.Id }
            });
        }
    }

    /// <summary>
    /// Marks a task as deleted using the soft delete pattern. The task remains in the database
    /// but is marked as deleted and will not appear in normal queries.
    /// </summary>
    /// <param name="task">The DittoTask object to be marked as deleted.</param>
    /// <returns>A task that represents the asynchronous deletion operation.</returns>
    /// <exception cref="Exception">Thrown when the deletion operation fails.</exception>
    public async Task DeleteTask(DittoTask task)
    {
        if (_ditto != null)
        {
            const string updateQuery = "UPDATE tasks SET deleted = true WHERE _id = :id";
            await _ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
            {
                { "id", task.Id }
            });
        }
    }

    /// <summary>
    /// Updates the completion status of a task in the Ditto store. Only updates if the
    /// current done state differs from the new state to avoid unnecessary operations.
    /// </summary>
    /// <param name="task">The DittoTask object containing the updated done status and task ID.</param>
    /// <returns>A task that represents the asynchronous update operation.</returns>
    /// <exception cref="Exception">Thrown when the update operation fails.</exception>
    public async Task UpdateTaskDoneAsync(DittoTask task)
    {
        if (_ditto != null)
        {
            var doc = new Dictionary<string, object>
            {
                { "newDoneState", task.Done },
                { "id", task.Id }
            };

            const string updateQuery =
                "UPDATE tasks SET done = :newDoneState WHERE _id = :id AND done != :newDoneState";
            await _ditto.Store.ExecuteAsync(updateQuery, doc);
        }
    }
}