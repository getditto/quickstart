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

    private static async Task RunDiagnosticMode(TasksPeer peer)
    {
        DittoLogger.SetMinimumLogLevel(DittoLogLevel.Error);

        Console.WriteLine("=== Diagnostic Mode Started ===");
        Console.WriteLine($"App ID: {peer.AppId}");
        Console.WriteLine($"Sync Active: {peer.IsSyncActive}");
        Console.WriteLine();

        // Initialize and set up subscription (similar to RunTerminalGui)
        await peer.DisableStrictMode();
        peer.RegisterSubscription();
        await peer.InsertInitialTasks();
        peer.StartSync();

        var observerCallCount = 0;
        var observerTriggered = new TaskCompletionSource<bool>();

        // Register an observer directly on the Ditto Store that logs the raw QueryResult
        var observer = peer.Ditto.Store.RegisterObserver("SELECT * FROM tasks WHERE NOT deleted", (queryResult) =>
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
        var exitTaskCompletionSource = new TaskCompletionSource<bool>();
        Console.CancelKeyPress += (sender, eventArgs) => {
            Console.WriteLine("Cancellation requested.");
            eventArgs.Cancel = true;
            exitTaskCompletionSource.TrySetResult(true);
        };
        await exitTaskCompletionSource.Task;

        observer.Cancel();
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
