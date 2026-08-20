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
            var expectedTitle = Environment.GetEnvironmentVariable("DITTO_CLOUD_TASK_TITLE");
            if (string.IsNullOrEmpty(expectedTitle))
            {
                Console.WriteLine("❌ FAIL: Missing DITTO_CLOUD_TASK_TITLE environment variable");
                Console.WriteLine("   This test requires a document to be seeded by CI");
                return 1;
            }

            Console.WriteLine($"🔍 Looking for seeded document: '{expectedTitle}'");

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
            if (!envVars.TryGetValue("DITTO_DATABASE_ID", out var databaseId) || string.IsNullOrEmpty(databaseId))
            {
                Console.WriteLine("❌ Missing DITTO_DATABASE_ID in .env file");
                return 1;
            }
            if (!envVars.TryGetValue("DITTO_DEVELOPMENT_TOKEN", out var developmentToken) || string.IsNullOrEmpty(developmentToken))
            {
                Console.WriteLine("❌ Missing DITTO_DEVELOPMENT_TOKEN in .env file");
                return 1;
            }

            if (!envVars.TryGetValue("DITTO_SERVER_URL", out var serverUrl) || string.IsNullOrEmpty(serverUrl))
            {
                Console.WriteLine("❌ Missing DITTO_SERVER_URL in .env file");
                return 1;
            }

            Console.WriteLine($"📡 Connecting to Ditto (Database ID: {databaseId})");

            // Initialize Ditto and start sync
            var dittoManager = await DittoManager.Create(databaseId, developmentToken, serverUrl);
            var tasksRepository = new TasksRepository(dittoManager);

            Console.WriteLine("✅ Ditto initialized and sync started");
            Console.WriteLine("⏳ Waiting for document to sync...");

            // Setup variables for tracking
            var maxWaitTime = TimeSpan.FromSeconds(30);
            var startTime = DateTime.Now;
            var found = false;
            var foundTask = null as TaskModel;
            var taskCount = 0;
            var lastCheckTime = DateTime.Now;

            // Use a TaskCompletionSource to wait for the document
            var tcs = new TaskCompletionSource<bool>();
            var cts = new CancellationTokenSource(maxWaitTime);

            // Register observer to watch for changes
            var observer = tasksRepository.ObserveTasksCollection((tasks) => Task.Run(() =>
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
            }));

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
            // Note: DittoStoreObserver doesn't have a Stop/Dispose method
            dittoManager.Dispose();

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
