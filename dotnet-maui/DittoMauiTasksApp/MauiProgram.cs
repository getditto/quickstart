using System.Reflection;

using Microsoft.Extensions.Logging;

using DittoMauiTasksApp.Utils;
using DittoMauiTasksApp.ViewModels;
using DittoSDK;

namespace DittoMauiTasksApp;

public static class MauiProgram
{
    public static string AppId { get; private set; } = "";
    public static string PlaygroundToken { get; private set; } = "";

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
        builder.Services.AddSingleton(SetupDitto());
        builder.Services.AddSingleton<IPopupService, PopupService>();
        builder.Services.AddTransient<TasksPageviewModel>();
        builder.Services.AddTransient<TasksPage>();

        return builder.Build();
    }

    private static Ditto SetupDitto()
    {
        var envVars = LoadEnvVariables();
        AppId = envVars["DITTO_APP_ID"];
        PlaygroundToken = envVars["DITTO_PLAYGROUND_TOKEN"];
        var authUrl = envVars["DITTO_AUTH_URL"];
        var websocketUrl = envVars["DITTO_WEBSOCKET_URL"];
        
        var ditto = new Ditto(DittoIdentity
        .OnlinePlayground(
            AppId, 
            PlaygroundToken, 
            false,  // This is required to be set to false to use the correct URLs
            authUrl), Path.Combine(FileSystem.Current.AppDataDirectory, "ditto"));
        
        ditto.TransportConfig.Connect.WebsocketUrls.Add(websocketUrl);
        // Optionally enable all P2P transports if using P2P Sync
        // Do not call this if only using Ditto Cloud Sync
        ditto.TransportConfig.Connect.WebsocketUrls.Add(websocketUrl);
        
        // disable sync with v3 peers, required for DQL
        ditto.DisableSyncWithV3();

        return ditto;
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
