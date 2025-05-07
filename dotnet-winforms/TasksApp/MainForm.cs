using DittoSDK;
using System.Threading.Tasks.Sources;

namespace DittoTasksApp
{
    public partial class MainForm : Form
    {

        private readonly TasksPeer _tasksPeer;
        private DittoStoreObserver _tasksObserver;

        private bool _isSyncEnabled = true;
        private readonly string _imageSyncOn = "assets/sync-on.bmp";
        private readonly string _imageSyncOff = "assets/sync-off.bmp";

        public MainForm(TasksPeer tasksPeer)
        {
            _tasksPeer = tasksPeer;
            _tasksPeer.StartSync();

            InitializeComponent();
            SetFormValues();

            // Observe the tasks collection
            _tasksObserver = _tasksPeer.ObserveTasksCollection(async (tasks) =>
            {
                // Update the ListView on the UI thread
                Invoke(() => UpdateTaskListView(tasks));
            });
        }

        private void SetFormValues()
        {
            tsslAppId.Text = $"AppId: {_tasksPeer.AppId}";
            tsslAuthToken.Text = $"Online Playground Token: {_tasksPeer.PlaygroundToken}";

            tsbSyncStatus.Image = Image.FromFile(_imageSyncOn);

        }

        private void LoadAddForm()
        {
            var editorForm = new ToDoTaskEditorForm(_tasksPeer, null);
            editorForm.Owner = this;
            editorForm.ShowDialog();

        }

        private void UpdateTaskListView(IList<ToDoTask?> tasks)
        {
            lvTasks.BeginUpdate();
            lvTasks.Items.Clear();
            if (tasks != null)
            {
                foreach (var task in tasks)
                {
                    if (task == null || task.Deleted)
                    {
                        continue;
                    }
                    var item = new ListViewItem(task.Title)
                    {
                        Tag = task,
                    };
                    item.SubItems.Add(task.Done ? "Yes" : "No");
                    lvTasks.Items.Add(item);
                }
            }

            lvTasks.EndUpdate();
        }

        private void exitToolStripMenuItem_Click(object sender, EventArgs e)
        {
            _tasksObserver.Cancel();
            _tasksPeer.StopSync();
            _tasksPeer.Dispose();

            Application.Exit();
        }


        private void aboutToolStripMenuItem_Click(object sender, EventArgs e)
        {
            var aboutForm = new AboutForm(_tasksPeer);
            aboutForm.Owner = this;
            aboutForm.ShowDialog();
        }

        private void newToolStripMenuItem_Click(object sender, EventArgs e)
        {
            LoadAddForm();
        }

        private void tsbAdd_Click(object sender, EventArgs e)
        {
            LoadAddForm();
        }

        private void tsbEdit_Click(object sender, EventArgs e)
        {
            var todoItem = (ToDoTask?)lvTasks.CheckedItems[0].Tag;
            if (todoItem != null) {
                var editForm = new ToDoTaskEditorForm(_tasksPeer, todoItem);
                editForm.Owner = this;
                editForm.ShowDialog();
            }
        }

        private async void tsbComplete_Click(object sender, EventArgs e)
        {
            foreach (ListViewItem item in lvTasks.CheckedItems)
            {
                var task = (ToDoTask?)item.Tag;
                if (task != null)
                {
                    await _tasksPeer.UpdateTaskDone(task.Id, !task.Done);
                }
            }
        }

        private async void tsbDelete_Click(object sender, EventArgs e)
        {
            foreach (ListViewItem item in lvTasks.CheckedItems)
            {
                var task = (ToDoTask?)item.Tag;
                if (task != null)
                {
                    await _tasksPeer.DeleteTask(task.Id);
                }
            }

        }

        private void tsbSyncStatus_Click(object sender, EventArgs e)
        {
            _isSyncEnabled = !_isSyncEnabled;
            if (_isSyncEnabled)
            {
                tsbSyncStatus.Image = Image.FromFile(_imageSyncOn);
                _tasksPeer.StartSync();
            }
            else
            {
                tsbSyncStatus.Image = Image.FromFile(_imageSyncOff);
                _tasksPeer.StopSync();
            }

        }

        private void lvTasks_ItemChecked(object sender, ItemCheckedEventArgs e)
        {
            if (e.Item.Checked)
            {
                tsbComplete.Enabled = true;
                tsbDelete.Enabled = true;
                tsbEdit.Enabled = true;
            }
            else
            {
                tsbComplete.Enabled = false;
                tsbDelete.Enabled = false;
                tsbEdit.Enabled = false;
            }
        }

        private void lvTasks_ItemCheck(object sender, ItemCheckEventArgs e)
        {

            // If the item is being checked
            if (e.NewValue == CheckState.Checked)
            {
                // Uncheck all other items
                foreach (ListViewItem item in lvTasks.Items)
                {
                    if (item != lvTasks.Items[e.Index])
                    {
                        item.Checked = false;
                    }
                }
            }

        }

 
    }
}
