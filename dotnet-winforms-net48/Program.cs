using System;
using System.Windows.Forms;

namespace Taskapp.WinForms.Net48
{
    internal static class Program
    {
        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [STAThread]
        static void Main()
        {
            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            try
            {
                // Load configuration from .env file
                AppConfiguration.Load();

                // Initialize AppServices asynchronously
                var initTask = AppServices.Instance.InitializeAsync(
                    AppConfiguration.DatabaseId,
                    AppConfiguration.DevelopmentToken,
                    AppConfiguration.ServerUrl,
                    AppConfiguration.OfflineLicenseToken
                );

                // Show loading form while initializing
                using (var loadingForm = new LoadingForm(initTask))
                {
                    if (loadingForm.ShowDialog() == DialogResult.OK)
                    {
                        // Initialization successful, show main form
                        Application.Run(new MainForm());
                    }
                    else
                    {
                        // Initialization failed or was cancelled
                        MessageBox.Show(
                            "Failed to initialize Ditto. Please check your configuration and try again.",
                            "Initialization Error",
                            MessageBoxButtons.OK,
                            MessageBoxIcon.Error
                        );
                    }
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    $"Application startup failed: {ex.Message}",
                    "Startup Error",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
            }
            finally
            {
                // Cleanup on exit
                if (AppServices.Instance.IsInitialized)
                {
                    AppServices.Instance.Dispose();
                }
            }
        }
    }
}
