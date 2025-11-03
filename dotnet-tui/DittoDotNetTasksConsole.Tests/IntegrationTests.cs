using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Threading.Tasks;
using Xunit;

namespace DittoDotNetTasksConsole.Tests;

public class IntegrationTests
{
    [Fact]
    public async Task TasksPeer_CanSyncAndRetrieveTasks()
    {
        var env = LoadEnvVariables();
        var taskToFind = Environment.GetEnvironmentVariable("DITTO_CLOUD_TASK_TITLE")
            ?? throw new InvalidOperationException("DITTO_CLOUD_TASK_TITLE environment variable is required");

        using var peer = await TasksPeer.Create(
            env["DITTO_APP_ID"],
            env["DITTO_PLAYGROUND_TOKEN"],
            env["DITTO_AUTH_URL"],
            env["DITTO_WEBSOCKET_URL"]);

        // Verify connection
        Assert.NotNull(peer);
        Assert.Equal(env["DITTO_APP_ID"], peer.AppId);

        var foundTask = false;
        var attempts = 0;
        const int maxAttempts = 10;

        while (attempts < maxAttempts && !foundTask)
        {
            await Task.Delay(1000);
            attempts++;

            var tasksList = new List<ToDoTask>();
            var observer = peer.ObserveTasksCollection(tasks =>
            {
                tasksList.AddRange(tasks);
                return Task.CompletedTask;
            });

            await Task.Delay(100);
            observer.Cancel();

            foreach (var task in tasksList)
            {
                if (task.Title == taskToFind)
                {
                    foundTask = true;
                    break;
                }
            }
        }

        Assert.True(foundTask, $"Expected task '{taskToFind}' not found after {maxAttempts} seconds.");
    }

    private static Dictionary<string, string> LoadEnvVariables()
    {
        var envVars = new Dictionary<string, string>();
        var assembly = Assembly.GetExecutingAssembly();
        var resourceName = "DittoDotNetTasksConsole.Tests..env";

        using var stream = assembly.GetManifestResourceStream(resourceName)
            ?? throw new InvalidOperationException($"Resource '{resourceName}' not found.");
        using var reader = new StreamReader(stream);

        string? line;
        while ((line = reader.ReadLine()) != null)
        {
            line = line.Trim();
            if (string.IsNullOrEmpty(line) || line.StartsWith("#")) continue;

            var separatorIndex = line.IndexOf('=');
            if (separatorIndex < 0) continue;

            var key = line.Substring(0, separatorIndex).Trim();
            var value = line.Substring(separatorIndex + 1).Trim();

            if (value.StartsWith("\"") && value.EndsWith("\"") && value.Length >= 2)
                value = value.Substring(1, value.Length - 2);

            envVars[key] = value;
        }

        return envVars;
    }
}