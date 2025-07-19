using DittoSDK;
using Microsoft.Extensions.Logging;
using System.Text.Json;
using System.Collections.ObjectModel;
using DittoMauiTasksApp.Model;

namespace DittoMauiTasksApp.Data;

public class DittoManager(ILogger<DittoManager> logger)
    : IDataManager
{
    private readonly ILogger<DittoManager> _logger = logger;
    private const string SelectQuery = "SELECT * FROM tasks WHERE NOT deleted";

#nullable enable
    private Ditto? _ditto = null;
    private DittoStoreObserver? _dittoStoreObserver = null;
    private DittoSyncSubscription? _syncSubscription = null;

    // DittoDiffer - used to calculate the delta changes between syncs
    // https://docs.ditto.live/sdk/latest/crud/read#diffing-results
    private DittoDiffer _dittoDiffer = new DittoDiffer();
    private List<DittoQueryResultItem> _previousItems = new List<DittoQueryResultItem>();

    public async Task Initialize(DittoConfig dittoConfig)
    {
        try
        {
            var dataPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "ditto");
            _ditto = new Ditto(DittoIdentity
                .OnlinePlayground(
                    dittoConfig.AppId,
                    dittoConfig.PlaygroundToken,
                    false, // This is required to be set to false to use the correct URLs with Ditto Portal
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

    public void StartSync()
    {
        _ditto?.StartSync();

        // Register a subscription, which determines what data syncs to this peer
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
        _syncSubscription = _ditto?.Sync.RegisterSubscription(SelectQuery);
    }

    public void StopSync()
    {
        _ditto?.StopSync();
        _syncSubscription?.Cancel();
    }

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