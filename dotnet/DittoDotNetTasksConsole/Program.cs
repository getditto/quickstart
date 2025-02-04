using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using System.Threading.Tasks;

using DittoSDK;
using Terminal.Gui;

public static class Program
{
    public static async Task Main(string[] args)
    {
        try
        {
            var env = LoadEnvVariables();
            var appId = env["DITTO_APP_ID"];
            var playgroundToken = env["DITTO_PLAYGROUND_TOKEN"];

            using var peer = await TasksPeer.Create(appId, playgroundToken);

            // Disable Ditto's standard-error logging, which would interfere
            // with the the Terminal.Gui UI.
            DittoLogger.SetLoggingEnabled(false);
            RunTerminalGui(peer);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine(ex);
        }
    }

    private static void RunTerminalGui(TasksPeer peer)
    {
        try
        {
            Application.Init();
            Application.Top.Add(new TasksWindow(peer));
            Application.Run();
        }
        finally
        {
            Application.Shutdown();
        }
    }

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
