using System;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace Taskapp.WinForms.Net48
{
    /// <summary>
    /// Loading form shown during async initialization
    /// </summary>
    public partial class LoadingForm : Form
    {
        private readonly Task _initializationTask;

        public LoadingForm(Task initializationTask)
        {
            _initializationTask = initializationTask;
            InitializeComponent();
        }

        private async void LoadingForm_Load(object sender, EventArgs e)
        {
            try
            {
                await _initializationTask;
                DialogResult = DialogResult.OK;
                Close();
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    $"Initialization failed: {ex.Message}",
                    "Error",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
                DialogResult = DialogResult.Cancel;
                Close();
            }
        }
    }
}
