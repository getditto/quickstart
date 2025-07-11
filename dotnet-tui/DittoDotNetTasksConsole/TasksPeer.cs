using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Threading.Tasks;

using DittoSDK;

/// <summary>
/// Encapsulates use of the Ditto SDK and the 'tasks' collection.
/// </summary>
public class TasksPeer : IDisposable
{
    private const string query = "SELECT * FROM tasks WHERE NOT deleted";

    public string AppId { get; private set; }
    public string PlaygroundToken { get; private set; }
    
    public bool IsSyncActive
    {
        get => ditto.IsSyncActive;
    }

    private Ditto ditto;

    /// <summary>
    /// Creates a new synchronizing TasksPeer instance.
    /// </summary>
    public static async Task<TasksPeer> Create(
        string appId, 
        string playgroundToken, 
        string authUrl, 
        string websocketUrl)
    {
        var peer = new TasksPeer(appId, playgroundToken, authUrl, websocketUrl);

        await peer.InsertInitialTasks();

        peer.StartSync();

        return peer;
    }

    /// <summary>
    /// Constructor
    /// </summary>
    /// <param name="appId">Ditto application ID</param>
    /// <param name="playgroundToken">Ditto online playground token</param>
    /// <param name="authUrl">Ditto Auth URL</param>
    /// <param name="websocketUrl">Ditto Websocket URL</param>
    public TasksPeer(string appId, string playgroundToken, string authUrl, string websocketUrl)
    {
        AppId = appId;
        PlaygroundToken = playgroundToken;

        // We use a temporary directory to store Ditto's local database.  This
        // means that data will not be persistent between runs of the
        // application, but it allows us to run multiple instances of the
        // application concurrently on the same machine.  For a production
        // application, we would want to store the database in a more permanent
        // location, and if multiple instances are needed, ensure that each
        // instance has its own persistence directory.
        var tempDir = Path.Combine(
            Path.GetTempPath(),
            "DittoDotNetTasksConsole-" + Guid.NewGuid().ToString());
        Directory.CreateDirectory(tempDir);

        var identity = DittoIdentity.OnlinePlayground(
            appId, 
            playgroundToken, 
            false, // This is required to be set to false to use the correct URLs
            authUrl);

        ditto = new Ditto(identity, tempDir);

        ditto.UpdateTransportConfig(config =>
        {
            // Add the websocket URL to the transport configuration.
            config.Connect.WebsocketUrls.Add(websocketUrl);
        });

        // disable sync with v3 peers, required for DQL
        ditto.DisableSyncWithV3();
    }

    public void Dispose()
    {
        ditto.Dispose();
        ditto = null;
        GC.SuppressFinalize(this);
    }

    /// <summary>
    /// Inserts the initial tasks into the 'tasks' collection.
    /// </summary>
    public async Task InsertInitialTasks()
    {
        var initialTasks = new List<Dictionary<string, object>>
            {
                new Dictionary<string, object>
                {
                    {"_id", "50191411-4C46-4940-8B72-5F8017A04FA7"},
                    {"title", "Buy groceries"},
                    {"done", false},
                    {"deleted", false}
                },
                new Dictionary<string, object>
                {
                    {"_id", "6DA283DA-8CFE-4526-A6FA-D385089364E5"},
                    {"title", "Clean the kitchen"},
                    {"done", false},
                    {"deleted", false}
                },
                new Dictionary<string, object>
                {
                    {"_id", "5303DDF8-0E72-4FEB-9E82-4B007E5797F0"},
                    {"title", "Schedule dentist appointment"},
                    {"done", false},
                    {"deleted", false}
                },
                new Dictionary<string, object>
                {
                    {"_id", "38411F1B-6B49-4346-90C3-0B16CE97E174"},
                    {"title", "Pay bills"},
                    {"done", false},
                    {"deleted", false}
                }
            };

        var insertCommand = "INSERT INTO tasks INITIAL DOCUMENTS (:task)";
        foreach (var task in initialTasks)
        {
            await ditto.Store.ExecuteAsync(insertCommand, new Dictionary<string, object>()
            {
                { "task", task }
            });
        }
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
        var insertCommand = "INSERT INTO tasks DOCUMENTS (:doc)";
        await ditto.Store.ExecuteAsync(insertCommand, new Dictionary<string, object>()
        {
            { "doc", doc }
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

        var updateQuery = "UPDATE tasks " +
            "SET title = :title " +
            "WHERE _id = :id";
        await ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
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

        var updateQuery = "UPDATE tasks " +
            "SET deleted = true " +
            "WHERE _id = :id";
        await ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
        {
            { "id", taskId }
        });
    }

    /// <summary>
    /// Mark a task as complete or not complete.
    /// </summary>
    public async Task UpdateTaskDone(string taskId, bool newDoneState)
    {
        var updateQuery = "UPDATE tasks " +
            "SET done = :newDoneState " +
            "WHERE _id = :id AND done != :newDoneState";
        await ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>
        {
            { "newDoneState", newDoneState },
            { "id", taskId }
        });
    }

    /// <summary>
    /// Specify a handler to be called asynchronously when the tasks collection changes.
    /// </summary>
    public DittoStoreObserver ObserveTasksCollection(Func<IList<ToDoTask>, Task> handler)
    {
        // Register observer, which runs against the local database on this peer
        // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
        return ditto.Store.RegisterObserver(query, async (queryResult) =>
        {
            try
            {
                // Deserialize the JSON documents into ToDoTask objects
                var tasks = queryResult.Items.Select(d =>
                    JsonSerializer.Deserialize<ToDoTask>(d.JsonString())
                ).OrderBy(t => t.Id).ToList();

                await handler(tasks);
            }
            catch (Exception e)
            {
                Console.Error.WriteLine($"ERROR tasks observation handler failed: {e.Message}");
            }
        });
    }

    /// <summary>
    /// Start synchronizing the 'tasks' collection.
    /// </summary>
    public void StartSync()
    {
        ditto.Store.Execute("ALTER SYSTEM SET DQL_STRICT_MODE = false");
        ditto.StartSync();

        // Register a subscription, which determines what data syncs to this peer
        // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
        ditto.Sync.RegisterSubscription(query);
    }

    /// <summary>
    /// Stop synchronizing the 'tasks' collection.
    /// </summary>
    public void StopSync()
    {
        foreach (var subscription in ditto.Sync.Subscriptions)
        {
            subscription.Cancel();
        }
        ditto.StopSync();
    }
}
