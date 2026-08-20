using System.Reflection;

using Microsoft.Extensions.Logging;

using DittoMauiTasksApp.Utils;
using DittoMauiTasksApp.ViewModels;

namespace DittoMauiTasksApp;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

#if DEBUG
        builder.Logging.SetMinimumLevel(LogLevel.Debug);
        builder.Logging.AddDebug();
#endif

        var envVars = LoadEnvVariables();
        var databaseId = envVars["DITTO_DATABASE_ID"];
        var developmentToken = envVars["DITTO_DEVELOPMENT_TOKEN"];
        var serverUrl = envVars["DITTO_SERVER_URL"];

        // Create the Ditto instance and the tasks repository up front, then
        // register them as singletons. Create() is synchronous under the hood,
        // so resolving the Task here does not block.
        var dittoManager = DittoManager
            .Create(databaseId, developmentToken, serverUrl)
            .GetAwaiter()
            .GetResult();
        var tasksRepository = new TasksRepository(dittoManager);

        builder.Services.AddSingleton(dittoManager);
        builder.Services.AddSingleton(tasksRepository);
        builder.Services.AddSingleton<IPopupService, PopupService>();
        builder.Services.AddTransient<TasksPageviewModel>();
        builder.Services.AddTransient<TasksPage>();

        return builder.Build();
    }

    /// <summary>
    /// Load environment variables from the embedded .env resource file.
    /// </summary>
    static Dictionary<string, string> LoadEnvVariables()
    {
        var envVars = new Dictionary<string, string>();
        var assembly = Assembly.GetExecutingAssembly();
        string resourceName = "DittoMauiTasksApp..env";

        using (Stream stream = assembly.GetManifestResourceStream(resourceName))
        {
            if (stream == null)
            {
                var availableResources = string.Join(Environment.NewLine, assembly.GetManifestResourceNames());
                throw new InvalidOperationException($"Resource '{resourceName}' not found. Available resources: {availableResources}");
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
