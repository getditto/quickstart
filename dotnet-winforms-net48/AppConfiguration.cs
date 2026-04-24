using System;
using System.IO;
using System.Text.RegularExpressions;

namespace Taskapp.WinForms.Net48
{
    /// <summary>
    /// Loads and provides access to application configuration from .env file
    /// </summary>
    public static class AppConfiguration
    {
        public static string AppId { get; private set; }
        public static string PlaygroundToken { get; private set; }
        public static string AuthUrl { get; private set; }
        public static string WebsocketUrl { get; private set; }

        /// <summary>
        /// Loads configuration from .env file in the application directory
        /// </summary>
        public static void Load()
        {
            var envPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, ".env");

            if (!File.Exists(envPath))
            {
                throw new FileNotFoundException("Configuration file .env not found", envPath);
            }

            var lines = File.ReadAllLines(envPath);

            foreach (var line in lines)
            {
                // Skip empty lines and comments
                if (string.IsNullOrWhiteSpace(line) || line.TrimStart().StartsWith("#"))
                    continue;

                // Parse KEY=VALUE or KEY="VALUE" format
                var match = Regex.Match(line, @"^\s*([A-Z_]+)\s*=\s*""?([^""]+)""?\s*$");
                if (match.Success)
                {
                    var key = match.Groups[1].Value;
                    var value = match.Groups[2].Value;

                    switch (key)
                    {
                        case "DITTO_APP_ID":
                            AppId = value;
                            break;
                        case "DITTO_PLAYGROUND_TOKEN":
                            PlaygroundToken = value;
                            break;
                        case "DITTO_AUTH_URL":
                            AuthUrl = value;
                            break;
                        case "DITTO_WEBSOCKET_URL":
                            WebsocketUrl = value;
                            break;
                    }
                }
            }

            // Validate required fields
            if (string.IsNullOrWhiteSpace(AppId))
                throw new InvalidOperationException("DITTO_APP_ID is required in .env file");
            if (string.IsNullOrWhiteSpace(PlaygroundToken))
                throw new InvalidOperationException("DITTO_PLAYGROUND_TOKEN is required in .env file");
            if (string.IsNullOrWhiteSpace(AuthUrl))
                throw new InvalidOperationException("DITTO_AUTH_URL is required in .env file");
            if (string.IsNullOrWhiteSpace(WebsocketUrl))
                throw new InvalidOperationException("DITTO_WEBSOCKET_URL is required in .env file");
        }
    }
}
