using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Threading;
using System.Threading.Tasks;

using DittoSDK;
using DittoSDK.Logging;
using Terminal.Gui;

public static class Program
{
    public static async Task Main(string[] args)
    {
        try
        {
            var env = LoadEnvVariables();
            var databaseId = env["DITTO_DATABASE_ID"];
            var developmentToken = env.GetValueOrDefault("DITTO_DEVELOPMENT_TOKEN", "");
            var serverUrl = env.GetValueOrDefault("DITTO_SERVER_URL", "");
            var offlineLicenseToken = env.GetValueOrDefault("DITTO_OFFLINE_LICENSE_TOKEN", "");

            using var dittoManager = await DittoManager.Create(
                databaseId, developmentToken, serverUrl, offlineLicenseToken);
            var tasksRepository = new TasksRepository(dittoManager);

            // Disable Ditto's standard-error logging, which would interfere
            // with the the Terminal.Gui UI.
            DittoLogger.IsEnabled = false;
            RunTerminalGui(dittoManager, tasksRepository);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine(ex);
        }
    }

    private static void RunTerminalGui(DittoManager dittoManager, TasksRepository tasksRepository)
    {
        try
        {
            Application.Init();
            Application.Top.Add(new TasksWindow(dittoManager, tasksRepository));

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
