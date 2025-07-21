using System.Reflection;
using DittoMauiTasksApp.Data;
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
        builder.Services.AddSingleton<IDataManager, DittoManager>();
        builder.Services.AddSingleton<IPopupService, PopupService>();
        builder.Services.AddTransient<TasksPageViewModel>();
        builder.Services.AddTransient<TasksPage>();

        return builder.Build();
    }

    public static DittoConfig GetDittoConfig()
    {
        var envVars = LoadEnvVariables();
        return new DittoConfig
        {
            AppId = envVars["DITTO_APP_ID"],
            PlaygroundToken = envVars["DITTO_PLAYGROUND_TOKEN"],
            AuthUrl = envVars["DITTO_AUTH_URL"],
            WebsocketUrl = envVars["DITTO_WEBSOCKET_URL"]
        };
    }

    /// <summary>
    /// Load environment variables from the embedded .env resource file.
    /// </summary>
    public static Dictionary<string, string> LoadEnvVariables()
    {
        const string resourceName = "DittoMauiTasksApp..env";
        var envVars = new Dictionary<string, string>();
        var assembly = Assembly.GetExecutingAssembly();

        using var stream = assembly.GetManifestResourceStream(resourceName);
        if (stream == null)
        {
            var availableResources = string.Join(Environment.NewLine, assembly.GetManifestResourceNames());
            throw new InvalidOperationException($"Resource '{resourceName}' not found. Available resources: {availableResources}");
        }

        using var reader = new StreamReader(stream);
        while (reader.ReadLine() is { } line)
        {
            line = line.Trim();

            if (string.IsNullOrEmpty(line) || line.StartsWith("#"))
            {
                continue;
            }

            var separatorIndex = line.IndexOf('=');
            if (separatorIndex < 0)
            {
                continue;
            }

            var key = line.Substring(0, separatorIndex).Trim();
            var value = line.Substring(separatorIndex + 1).Trim();

            if (value.StartsWith(@"\") && value.EndsWith(@"\") && value.Length >= 2)
            {
                value = value.Substring(1, value.Length - 2);
            }

            envVars[key] = value;
        }

        return envVars;
    }
}
