using System.Reflection;

using Microsoft.Extensions.Logging;

using DittoMauiTasksApp.Utils;
using DittoMauiTasksApp.ViewModels;
using DittoSDK;
using DittoSDK.Auth;

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
        AppId = envVars.TryGetValue("DITTO_APP_ID", out var rawAppId) ? rawAppId : "";
        PlaygroundToken = envVars.TryGetValue("DITTO_PLAYGROUND_TOKEN", out var rawToken1) ? rawToken1 : "";
        var authUrl = envVars.TryGetValue("DITTO_AUTH_URL", out var rawAuthUrl) ? rawAuthUrl : "";
        var websocketUrl = envVars.TryGetValue("DITTO_WEBSOCKET_URL", out var rawWsUrl) ? rawWsUrl : "";
        var offlineLicenseToken = envVars.TryGetValue("DITTO_OFFLINE_LICENSE_TOKEN", out var rawToken)
            ? rawToken.Trim()
            : "";
        var isOffline = !string.IsNullOrEmpty(offlineLicenseToken);

        if (string.IsNullOrWhiteSpace(AppId))
        {
            throw new InvalidOperationException("DITTO_APP_ID is required.");
        }
        if (!isOffline)
        {
            var missing = new List<string>();
            if (string.IsNullOrWhiteSpace(PlaygroundToken)) missing.Add("DITTO_PLAYGROUND_TOKEN");
            if (string.IsNullOrWhiteSpace(authUrl)) missing.Add("DITTO_AUTH_URL");
            if (string.IsNullOrWhiteSpace(websocketUrl)) missing.Add("DITTO_WEBSOCKET_URL");
            if (missing.Count > 0)
            {
                throw new InvalidOperationException(
                    $"Online Playground mode requires: {string.Join(", ", missing)}. " +
                    "Set DITTO_OFFLINE_LICENSE_TOKEN to use offline mode instead.");
            }
        }

        // New Initialization code - https://docs.ditto.live/sdk/latest/ditto-config
        DittoConfigConnect connect = isOffline
            ? new DittoConfigConnect.SmallPeersOnly()
            : new DittoConfigConnect.Server(new Uri(authUrl));

        var dittoConfig = new DittoConfig(
            AppId,
            connect,
            Path.Combine(FileSystem.Current.AppDataDirectory, "ditto")
            );

        var ditto = Ditto.Open(dittoConfig);

        if (isOffline)
        {
            ditto.SetOfflineOnlyLicenseToken(offlineLicenseToken);
        }
        else
        {
            // Set up authentication expiration handler (required for server connections)
            ditto.Auth.ExpirationHandler = async (dittoAuth, secondsRemaining) =>
            {
                // Authenticate when token is expiring
                try
                {
                    await dittoAuth.Auth.LoginAsync(
                        // Your development token, replace with your actual token
                        PlaygroundToken,
                        // Use DittoAuthenticationProvider.Development for playground, or your actual provider
                        DittoAuthenticationProvider.Development
                    );
                    Console.WriteLine("Authentication successful");
                }
                catch (Exception error)
                {
                    Console.WriteLine($"Authentication failed: {error}");
                }
            };
        }

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
