using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Runtime;
using System.Threading;
using System.Threading.Tasks;
using DittoSDK;
using Terminal.Gui;

namespace DittoDotNetTasksConsole;

public static class Program
{
    public static async Task Main(string[] args)
    {
        try
        {
            var env = LoadEnvVariables();
            var appId = env["DITTO_APP_ID"];
            var playgroundToken = env["DITTO_PLAYGROUND_TOKEN"];
            var websocketUrl = env["DITTO_WEBSOCKET_URL"];
            var authUrl = env["DITTO_AUTH_URL"];

            // TasksPeer wraps a Ditto instance with application-specific functionality
            using var peer = await TasksPeer.Create(appId, playgroundToken, authUrl, websocketUrl);

            // Check for --observe command-line option
            if (args.Contains("--observe"))
            {
                // Run observer mode with logging enabled
                Console.WriteLine("Running in observer mode...");
                await RunObserverMode(peer);
            }
            else if (args.Contains("--generate"))
            {
                // Run generator mode with logging enabled
                Console.WriteLine("Running in generator mode...");
                await RunGeneratorMode(peer);
            }
            else
            {
                // Disable Ditto's standard-error logging, which would interfere
                // with the the Terminal.Gui UI.
                DittoLogger.SetLoggingEnabled(false);
                RunTerminalGui(peer);
            }
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine(ex);
        }
    }

    // Sets up Ditto device name and metadata for diagnostic modes
    private static void ConfigureDittoMetadata(TasksPeer peer, string modeName)
    {
        peer.Ditto.DeviceName = $"dotnet-tui {modeName}";
        var metadata = new Dictionary<string, object>
        {
            ["deviceName"] = peer.Ditto.DeviceName,
            ["processID"] = System.Diagnostics.Process.GetCurrentProcess().Id,
            ["machineName"] = Environment.MachineName,
            ["userName"] = Environment.UserName,
            ["osVersion"] = Environment.OSVersion.ToString(),
            ["dotnetVersion"] = Environment.Version.ToString(),
            ["startTime"] = DateTime.UtcNow.ToString("O"),
            ["debugMode"] = true,
            ["platform"] = Environment.Is64BitProcess ? "x64" : "x86",
            ["workingDirectory"] = Environment.CurrentDirectory,
            ["sessionID"] = Guid.NewGuid().ToString()
        };
        peer.Ditto.SmallPeerInfo.Metadata = metadata;
        peer.Ditto.Presence.SetPeerMetadata(metadata);
    }

    // Invoked by passing the `--observe` command-line parameter, this function monitors
    // observer behavior and subscription toggling instead of showing the TUI.
    private static async Task RunObserverMode(TasksPeer peer)
    {
        DittoLogger.SetMinimumLogLevel(DittoLogLevel.Error);

        Console.WriteLine("=== Observer Mode Started ===");
        Console.WriteLine($"App ID: {peer.AppId}");
        Console.WriteLine();

        ConfigureDittoMetadata(peer, "Observer");

        await peer.DisableStrictMode();

        // set up four subscriptions
        var sub1 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query);
        var sub2 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND 1 = 1");
        var sub3 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND done = false OR done = true");
        var sub4 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND title IS NOT NULL");

        peer.StartSync();

        var observerCallCount = 0;
        var observerTriggered = new TaskCompletionSource<bool>();
        var lastItemsJson = new List<string>();
        var duplicateDetected = false;
        var exitTaskCompletionSource = new TaskCompletionSource<bool>();
        var cancellationTokenSource = new CancellationTokenSource();

        // Register an observer directly on the Ditto Store that logs the raw QueryResult
        var observer = peer.Ditto.Store.RegisterObserver(TasksPeer.Query, (queryResult) =>
        {
            observerCallCount++;
            Console.WriteLine($"[Observer Callback #{observerCallCount}] Triggered at {DateTime.Now:HH:mm:ss.fff}");
            Console.WriteLine($"  QueryResult Item Count: {queryResult.Items.Count}");

            // Collect current items as JSON strings
            var currentItemsJson = new List<string>();
            for (var i = 0; i < queryResult.Items.Count; i++)
            {
                var item = queryResult.Items[i];
                var jsonString = item.JsonString();
                currentItemsJson.Add(jsonString);
                Console.WriteLine($"  Item [{i}]: {jsonString}");
            }

            // Sort both lists to ensure order-independent comparison
            currentItemsJson.Sort();

            // Check if this is identical to the last callback
            if (observerCallCount > 1 && lastItemsJson.Count == currentItemsJson.Count)
            {
                bool isIdentical = true;
                for (int i = 0; i < currentItemsJson.Count; i++)
                {
                    if (currentItemsJson[i] != lastItemsJson[i])
                    {
                        isIdentical = false;
                        break;
                    }
                }

                if (isIdentical)
                {
                    Console.WriteLine();
                    Console.WriteLine("ERROR: Observer callback received identical data!");
                    Console.WriteLine($"This is callback #{observerCallCount} with the same data as callback #{observerCallCount - 1}");
                    Console.WriteLine("Terminating due to duplicate observer callback detection.");
                    duplicateDetected = true;
                    observerTriggered.TrySetResult(true);
                    // Trigger cancellation to exit the application
                    cancellationTokenSource.Cancel();
                    exitTaskCompletionSource.TrySetResult(true);
                    return;
                }
            }

            // Store the current items for next comparison
            lastItemsJson = new List<string>(currentItemsJson);

            Console.WriteLine();

            // Signal that the observer has been triggered at least once
            if (observerCallCount == 1)
            {
                observerTriggered.TrySetResult(true);
            }
        });

        Console.WriteLine("Observer registered. Waiting for initial callback...");
        Console.WriteLine();

        // Wait for the observer to be triggered at least once
        await observerTriggered.Task;

        // // Add a test task to trigger another observer callback
        // Console.WriteLine("Adding a test task to trigger observer...");
        // var timestamp = DateTime.Now.ToString("HH:mm:ss.fff");
        // var title = $"Test task created at {timestamp}";
        // await peer.AddTask(title);

        Console.WriteLine("Press Control+C to exit...");
        Console.WriteLine("Subscriptions will be toggled on/off every second.");
        Console.WriteLine();

        Console.CancelKeyPress += (sender, eventArgs) => {
            Console.WriteLine("Cancellation requested.");
            eventArgs.Cancel = true;
            cancellationTokenSource.Cancel();
            exitTaskCompletionSource.TrySetResult(true);
        };

        // Start a task to toggle subscriptions every second
        var toggleTask = Task.Run(async () => {
            var subscriptionsActive = true;
            while (!cancellationTokenSource.Token.IsCancellationRequested)
            {
                try
                {
                    await Task.Delay(1000, cancellationTokenSource.Token);

                    if (subscriptionsActive)
                    {
                        // Cancel all four subscriptions
                        sub1.Cancel();
                        sub1 = null;
                        sub2.Cancel();
                        sub2 = null;
                        sub3.Cancel();
                        sub3 = null;
                        sub4.Cancel();
                        sub4 = null;
                        subscriptionsActive = false;

                        // Remove any deleted tasks from our local store
                        await peer.Ditto.Store.ExecuteAsync("EVICT FROM tasks WHERE deleted");

                        GC.Collect();
                    }
                    else
                    {
                        // Re-register all four subscriptions with the same queries
                        sub1 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query);
                        sub2 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND 1 = 1");
                        sub3 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND done = false OR done = true");
                        sub4 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND title IS NOT NULL");
                        subscriptionsActive = true;
                    }
                }
                catch (TaskCanceledException)
                {
                    // Expected when cancellation is requested
                    break;
                }
            }
        }, cancellationTokenSource.Token);

        await exitTaskCompletionSource.Task;

        // Wait for the toggle task to complete
        await toggleTask;

        // Clean up
        observer.Cancel();

        // Cancel subscriptions if they're still active
        if (sub1 is { IsCancelled: false })
            sub1.Cancel();
        if (sub2 is { IsCancelled: false })
            sub2.Cancel();
        if (sub3 is { IsCancelled: false })
            sub3.Cancel();
        if (sub4 is { IsCancelled: false })
            sub4.Cancel();

        Console.WriteLine();
        Console.WriteLine("Observer cancelled. Observer mode ended.");
        Console.WriteLine($"Total observer callbacks: {observerCallCount}");

        if (duplicateDetected)
        {
            Console.WriteLine("WARNING: Duplicate observer callback was detected during this session!");
        }
    }

    // Invoked by passing the `--generate` command-line parameter, this function continuously
    // generates/updates tasks in the collection instead of showing the TUI.
    private static async Task RunGeneratorMode(TasksPeer peer)
    {
        DittoLogger.SetMinimumLogLevel(DittoLogLevel.Error);

        Console.WriteLine("=== Generator Mode Started ===");
        Console.WriteLine($"App ID: {peer.AppId}");
        Console.WriteLine();

        ConfigureDittoMetadata(peer, "Generator");

        await peer.DisableStrictMode();
        peer.StartSync();

        Console.WriteLine("Generating 1 task per second, cycling through IDs 1-10. Press Control+C to exit...");
        Console.WriteLine();

        var exitTaskCompletionSource = new TaskCompletionSource<bool>();
        var cancellationTokenSource = new CancellationTokenSource();

        Console.CancelKeyPress += (sender, eventArgs) => {
            Console.WriteLine("Cancellation requested.");
            eventArgs.Cancel = true;
            cancellationTokenSource.Cancel();
            exitTaskCompletionSource.TrySetResult(true);
        };

        var generationCount = 0;
        var currentId = 1;

        // Start a task to generate one task per second
        var generatorTask = Task.Run(async () => {
            while (!cancellationTokenSource.Token.IsCancellationRequested)
            {
                try
                {
                    // Generate a single task with the current ID
                    var timestamp = DateTime.UtcNow.ToString("O");

                    var query = $@"
                        INSERT INTO tasks
                        DOCUMENTS ({{
                            '_id': '{currentId}',
                            'title': 'Generated task {currentId} {timestamp}',
                            'done': false,
                            'deleted': false
                        }})
                        ON ID CONFLICT DO UPDATE";

                    await peer.Ditto.Store.ExecuteAsync(query);

                    generationCount++;
                    Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] Generated/updated task ID {currentId} (total: {generationCount})");

                    // Cycle through IDs 1-10
                    currentId++;
                    if (currentId > 10)
                    {
                        currentId = 1;
                    }

                    await Task.Delay(100, cancellationTokenSource.Token);
                }
                catch (TaskCanceledException)
                {
                    // Expected when cancellation is requested
                    break;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Error generating tasks: {ex.Message}");
                }
            }
        }, cancellationTokenSource.Token);

        await exitTaskCompletionSource.Task;

        // Wait for the generator task to complete
        await generatorTask;

        Console.WriteLine();
        Console.WriteLine("Generator mode ended.");
        Console.WriteLine($"Total task updates: {generationCount}");
        Console.WriteLine($"Last task ID updated: {(currentId == 1 ? 10 : currentId - 1)}");
    }

    private static void RunTerminalGui(TasksPeer peer)
    {
        try
        {
            Application.Init();
            Application.Top.Add(new TasksWindow(peer));

            // Sleep when idle, reducing CPU usage.
            Application.MainLoop.AddIdle(() =>
            {
                Thread.Sleep(50);
                return true;
            });

            Application.Run();
        }
        finally
        {
            Application.Shutdown();
        }
    }

    /// <summary>
    /// Reads values from the embedded .env file resource.
    /// </summary>
    private static Dictionary<string, string> LoadEnvVariables()
    {
        var envVars = new Dictionary<string, string>();

        var assembly = Assembly.GetExecutingAssembly();

        string resourceName = "DittoDotNetTasksConsole..env";

        using (Stream stream = assembly.GetManifestResourceStream(resourceName))
        {
            if (stream == null)
            {
                throw new InvalidOperationException($"Resource '{resourceName}' not found.");
            }

            using (var reader = new StreamReader(stream))
            {
                string line;
                while ((line = reader.ReadLine()) != null)
                {
                    line = line.Trim();

                    if (string.IsNullOrEmpty(line) || line.StartsWith("#"))
                    {
                        continue;
                    }

                    // Split on the first '=' character.
                    int separatorIndex = line.IndexOf('=');
                    if (separatorIndex < 0)
                    {
                        continue;
                    }

                    string key = line.Substring(0, separatorIndex).Trim();
                    string value = line.Substring(separatorIndex + 1).Trim();

                    if (value.StartsWith("\"") && value.EndsWith("\"") && value.Length >= 2)
                    {
                        value = value.Substring(1, value.Length - 2);
                    }

                    envVars[key] = value;
                }
            }
        }

        return envVars;
    }
}
