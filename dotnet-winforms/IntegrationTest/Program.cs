using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using DittoTasksApp;

namespace IntegrationTest;

class Program
{
    static async Task<int> Main(string[] args)
    {
        Console.WriteLine("🔧 .NET WinForms Integration Test");

        try
        {
            // Get the test document title from environment variable (set by CI)
            var expectedTitle = Environment.GetEnvironmentVariable("GITHUB_TEST_DOC_TITLE");
            if (string.IsNullOrEmpty(expectedTitle))
            {
                // Fallback for local testing
                expectedTitle = $"test_doc_{DateTime.Now.Ticks}";
                Console.WriteLine($"⚠️  No GITHUB_TEST_DOC_TITLE found, using: {expectedTitle}");
            }
            else
            {
                Console.WriteLine($"🔍 Looking for seeded document: '{expectedTitle}'");
            }

            // Load environment variables from .env file
            var envPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, ".env");
            if (!File.Exists(envPath))
            {
                Console.WriteLine($"❌ .env file not found at: {envPath}");
                return 1;
            }

            // Parse .env file
            var envVars = new Dictionary<string, string>();
            foreach (var line in File.ReadAllLines(envPath))
            {
                if (string.IsNullOrWhiteSpace(line) || line.StartsWith("#"))
                    continue;

                var parts = line.Split('=', 2);
                if (parts.Length == 2)
                {
                    envVars[parts[0].Trim()] = parts[1].Trim();
                }
            }

            // Get required environment variables
            if (!envVars.TryGetValue("DITTO_APP_ID", out var appId) || string.IsNullOrEmpty(appId))
            {
                Console.WriteLine("❌ Missing DITTO_APP_ID in .env file");
                return 1;
            }
            if (!envVars.TryGetValue("DITTO_PLAYGROUND_TOKEN", out var playgroundToken) || string.IsNullOrEmpty(playgroundToken))
            {
                Console.WriteLine("❌ Missing DITTO_PLAYGROUND_TOKEN in .env file");
                return 1;
            }

            var authUrl = envVars.GetValueOrDefault("DITTO_AUTH_URL", "https://auth.cloud.ditto.live");
            var websocketUrl = envVars.GetValueOrDefault("DITTO_WEBSOCKET_URL", "wss://cloud.ditto.live");

            Console.WriteLine($"📡 Connecting to Ditto (App ID: {appId})");

            // Initialize TasksPeer and start sync
            var tasksPeer = await TasksPeer.Create(appId, playgroundToken, authUrl, websocketUrl);

            Console.WriteLine("✅ Ditto initialized and sync started");
            Console.WriteLine("⏳ Waiting for document to sync...");

            // Setup variables for tracking
            var maxWaitTime = TimeSpan.FromSeconds(30);
            var startTime = DateTime.Now;
            var found = false;
            var foundTask = null as ToDoTask;
            var taskCount = 0;
            var lastCheckTime = DateTime.Now;

            // Use a TaskCompletionSource to wait for the document
            var tcs = new TaskCompletionSource<bool>();
            var cts = new CancellationTokenSource(maxWaitTime);

            // Register observer to watch for changes
            var observer = tasksPeer.ObserveTasksCollection(async (tasks) =>
            {
                taskCount = tasks.Count;
                var elapsed = (int)(DateTime.Now - startTime).TotalSeconds;

                // Only log every second to avoid spam
                if (DateTime.Now - lastCheckTime >= TimeSpan.FromSeconds(1))
                {
                    Console.WriteLine($"   [{elapsed}s] Checking {taskCount} synced tasks...");
                    lastCheckTime = DateTime.Now;
                }

                // Look for the expected document
                foreach (var task in tasks)
                {
                    if (task.Title == expectedTitle)
                    {
                        foundTask = task;
                        found = true;
                        tcs.TrySetResult(true);
                        break;
                    }
                }
            });

            // Wait for the document or timeout
            try
            {
                using (cts.Token.Register(() => tcs.TrySetCanceled()))
                {
                    await tcs.Task;
                }
            }
            catch (TaskCanceledException)
            {
                // Timeout occurred
            }

            // Cleanup
            observer?.Dispose();
            tasksPeer.Dispose();

            if (!found)
            {
                Console.WriteLine($"❌ FAIL: Document '{expectedTitle}' not found after {maxWaitTime.TotalSeconds} seconds");
                Console.WriteLine($"   Total tasks synced: {taskCount}");
                return 1;
            }

            Console.WriteLine($"✅ SUCCESS: Found document '{expectedTitle}'");
            Console.WriteLine($"   ID: {foundTask!.Id}");
            Console.WriteLine($"   Done: {foundTask.Done}");
            Console.WriteLine($"   Deleted: {foundTask.Deleted}");

            Console.WriteLine("🎉 Integration test passed!");
            return 0;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"❌ Test failed with exception: {ex.Message}");
            Console.WriteLine(ex.StackTrace);
            return 1;
        }
    }
}