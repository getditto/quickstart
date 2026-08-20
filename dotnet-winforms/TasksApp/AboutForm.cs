using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace DittoTasksApp
{
    public partial class AboutForm : Form
    {

        private readonly DittoManager _dittoManager;

        public AboutForm(DittoManager dittoManager)
        {
            _dittoManager = dittoManager;
            InitializeComponent();
            SetFormValues();
        }

        /// <summary>
        /// Sets the values of the form labels based on the DittoManager instance.
        /// </summary>
        private void SetFormValues()
        {
            lblDatabaseIdValue.Text = _dittoManager.DatabaseId;
            lblServerURLValue.Text = _dittoManager.ServerUrl;
            lblDevelopmentTokenValue.Text = _dittoManager.DevelopmentToken;

            // Get the version of the DittoSDK assembly
            var dittoAssembly = typeof(DittoSDK.Ditto).Assembly;
            var version = dittoAssembly.GetName().Version?.ToString() ?? "Unknown";
            lblSDKVersion.Text = version;
        }

        private void btnClose_Click(object sender, EventArgs e)
        {
            this.Close();
            this.Dispose();

        }

        private void llDittoDocs_LinkClicked(object sender, LinkLabelLinkClickedEventArgs e)
        {
            try
            {
                // URL to open
                string url = "https://docs.ditto.live/home/introduction#sdk-quickstart-guides";

                // Open the URL in the default browser
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
                {
                    FileName = url,
                    UseShellExecute = true // Ensures it opens in the default browser
                });
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to open the link. Error: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }
    }
}
