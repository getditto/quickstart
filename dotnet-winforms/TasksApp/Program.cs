using System.Reflection;
using DittoSDK;

namespace DittoTasksApp
{
    internal static class Program
    {
        /// <summary>
        ///  The main entry point for the application.
        /// </summary>
        [STAThread]
        static async Task Main()
        {
            var env = LoadEnvVariables();
            var appId = env["DITTO_APP_ID"];
            var playgroundToken = env["DITTO_PLAYGROUND_TOKEN"];
            var websocketUrl = env["DITTO_WEBSOCKET_URL"];
            var authUrl = env["DITTO_AUTH_URL"];

            using var peer = await TasksPeer.Create(appId, playgroundToken, authUrl, websocketUrl);

            // Disable Ditto's standard-error logging, which would interfere
            // with the the Terminal.Gui UI.
            DittoLogger.SetLoggingEnabled(false);
            // To customize application configuration such as set high DPI settings or default font,
            // see https://aka.ms/applicationconfiguration.
            ApplicationConfiguration.Initialize();
            Application.Run(new MainForm(peer));
        }

        /// <summary>
        /// Reads values from the embedded .env file resource.
        /// </summary>
        private static Dictionary<string, string> LoadEnvVariables()
        {
            var envVars = new Dictionary<string, string>();

            var assembly = Assembly.GetExecutingAssembly();

            string resourceName = "DittoTasksApp..env";

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
}