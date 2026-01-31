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
        AppId = envVars["DITTO_APP_ID"];
        PlaygroundToken = envVars["DITTO_PLAYGROUND_TOKEN"];
        var authUrl = envVars["DITTO_AUTH_URL"];

        // New Initialization code - https://docs.ditto.live/sdk/latest/ditto-config
        var dittoConfig = new DittoConfig(
            AppId, 
            new DittoConfigConnect.Server(
                new Uri(authUrl)
                ),  
            Path.Combine(FileSystem.Current.AppDataDirectory, "ditto")
            );

        var ditto = Ditto.Open(dittoConfig);
        
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
