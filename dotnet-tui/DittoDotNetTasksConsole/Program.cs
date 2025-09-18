using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
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

            // Check for --test command-line option
            if (args.Contains("--test"))
            {
                // Run diagnostic mode with logging enabled
                Console.WriteLine("Running in diagnostic mode...");
                await RunDiagnosticMode(peer);
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

    // Invoked by passing the `--test` command-line parameter, this function tries to reproduce
    // customer issues instead of showing the TUI.
    private static async Task RunDiagnosticMode(TasksPeer peer)
    {
        DittoLogger.SetMinimumLogLevel(DittoLogLevel.Error);

        Console.WriteLine("=== Diagnostic Mode Started ===");
        Console.WriteLine($"App ID: {peer.AppId}");
        Console.WriteLine();

        peer.Ditto.DeviceName = "dotnet-tui Diagnostics";
        var metadata = new Dictionary<string, object>
        {
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

        await peer.DisableStrictMode();

        // set up four subscriptions
        var sub1 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query);
        var sub2 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND 1 = 1");
        var sub3 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND done = false OR done = true");
        var sub4 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND title IS NOT NULL");

        peer.StartSync();

        var observerCallCount = 0;
        var observerTriggered = new TaskCompletionSource<bool>();

        // Register an observer directly on the Ditto Store that logs the raw QueryResult
        var observer = peer.Ditto.Store.RegisterObserver(TasksPeer.Query, (queryResult) =>
        {
            observerCallCount++;
            Console.WriteLine($"[Observer Callback #{observerCallCount}] Triggered at {DateTime.Now:HH:mm:ss.fff}");
            Console.WriteLine($"  QueryResult Item Count: {queryResult.Items.Count}");

            // Print the contents of each item in the QueryResult
            for (var i = 0; i < queryResult.Items.Count; i++)
            {
                var item = queryResult.Items[i];
                Console.WriteLine($"  Item [{i}]: {item.JsonString()}");
            }

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

        var exitTaskCompletionSource = new TaskCompletionSource<bool>();
        var cancellationTokenSource = new CancellationTokenSource();

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
                        sub2.Cancel();
                        sub3.Cancel();
                        sub4.Cancel();
                        Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] All 4 subscriptions cancelled");
                        subscriptionsActive = false;
                    }
                    else
                    {
                        // Re-register all four subscriptions with the same queries
                        sub1 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query);
                        sub2 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND 1 = 1");
                        sub3 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND done = false OR done = true");
                        sub4 = peer.Ditto.Sync.RegisterSubscription(TasksPeer.Query + " AND title IS NOT NULL");
                        Console.WriteLine($"[{DateTime.Now:HH:mm:ss.fff}] All 4 subscriptions re-registered");
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
        Console.WriteLine("Observer cancelled. Diagnostic mode ended.");
        Console.WriteLine($"Total observer callbacks: {observerCallCount}");
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
