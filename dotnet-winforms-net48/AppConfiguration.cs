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
        public static string DatabaseId { get; private set; }
        public static string DevelopmentToken { get; private set; }
        public static string ServerUrl { get; private set; }
        public static string OfflineLicenseToken { get; private set; }

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
                        case "DITTO_DATABASE_ID":
                            DatabaseId = value;
                            break;
                        case "DITTO_DEVELOPMENT_TOKEN":
                            DevelopmentToken = value;
                            break;
                        case "DITTO_SERVER_URL":
                            ServerUrl = value;
                            break;
                        case "DITTO_OFFLINE_LICENSE_TOKEN":
                            OfflineLicenseToken = value;
                            break;
                    }
                }
            }

            // Validate required fields
            if (string.IsNullOrWhiteSpace(DatabaseId))
                throw new InvalidOperationException("DITTO_DATABASE_ID is required in .env file");
            if (!string.IsNullOrWhiteSpace(OfflineLicenseToken))
                return;
            if (string.IsNullOrWhiteSpace(DevelopmentToken))
                throw new InvalidOperationException("DITTO_DEVELOPMENT_TOKEN is required in .env file");
            if (string.IsNullOrWhiteSpace(ServerUrl))
                throw new InvalidOperationException("DITTO_SERVER_URL is required in .env file");
        }
    }
}
