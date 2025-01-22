using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

using DittoSDK;
using Generated;

class DittoTask
{
    [JsonPropertyName("_id")]
    public string Id { get; set; }

    [JsonPropertyName("title")]
    public string Title { get; set; }

    [JsonPropertyName("done")]
    public bool Done { get; set; }

    [JsonPropertyName("deleted")]
    public bool Deleted { get; set; }
}

static class Program
{
    private const string query = "SELECT * FROM tasks WHERE NOT deleted";

    public static async Task Main(string[] args)
    {
        try
        {
            var ditto = SetupDitto();
            Console.Error.WriteLine("INFO   Ditto setup complete");

            ditto.StartSync();

            await InsertInitialTasks(ditto);
            Console.Error.WriteLine("INFO   Initial tasks inserted");

            var subscription = SubscribeToTasksCollection(ditto);
            var observer = ObserveTasksCollection(ditto);

            Console.Out.WriteLine("<< Press Ctrl+C to exit >>");
            while (true)
            {
                await Task.Delay(1000);
            }

            Console.Error.WriteLine("INFO   Program complete");
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine(ex);
        }
    }

    private static Ditto SetupDitto()
    {
        var tempDir = Path.Combine(Path.GetTempPath(), "DittoDotNetTasksConsole-" + Guid.NewGuid().ToString());
        Directory.CreateDirectory(tempDir);
        Console.Error.WriteLine($"INFO   Ditto temp dir: {tempDir}");

        var identity = DittoIdentity.OnlinePlayground(
            EnvConstants.DITTO_APP_ID, EnvConstants.DITTO_PLAYGROUND_TOKEN, true);

        var ditto = new Ditto(identity, tempDir);

        ditto.DisableSyncWithV3();

        return ditto;
    }

    private static async Task InsertInitialTasks(Ditto ditto)
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

    private static async Task AddTask(Ditto ditto, string title)
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

    private static async Task UpdateTaskTitle(Ditto ditto, string taskId, string newTitle)
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

    private static async Task DeleteTask(Ditto ditto, string taskId)
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

    private static async Task UpdateTaskDone(Ditto ditto, string taskId, bool newDoneState)
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

    private static DittoStoreObserver ObserveTasksCollection(Ditto ditto)
    {
        return ditto.Store.RegisterObserver(query, async (queryResult) =>
        {
            try
            {
                var tasks = queryResult.Items.Select(d =>
                    JsonSerializer.Deserialize<DittoTask>(d.JsonString())
                ).OrderBy(t => t.Id).ToList();

                Console.WriteLine("Tasks Update:");
                foreach (var task in tasks)
                {
                    Console.WriteLine($"  Task: {task.Title} (done: {task.Done}) (id: {task.Id})");
                }
                Console.WriteLine("Tasks Update Complete");
            }
            catch (Exception e)
            {
                Console.Error.WriteLine($"ERROR Unable to update list view model: {e.Message}");
            }
        });
    }

    private static DittoSyncSubscription SubscribeToTasksCollection(Ditto ditto)
    {
        return ditto.Sync.RegisterSubscription(query);
    }
}
