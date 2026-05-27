using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace Taskapp.WinForms.Net48
{
    public partial class MainForm : Form
    {
        private bool _isUpdatingListView = false;

        public MainForm()
        {
            InitializeComponent();
        }

        private void MainForm_Load(object sender, EventArgs e)
        {
            // Enable double buffering to reduce flicker
            typeof(ListView).InvokeMember("DoubleBuffered",
                System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.SetProperty,
                null, tasksListView, new object[] { true });

            // Register observer to watch for task changes
            TasksPeerService.Instance.ObserveTasksCollection(OnTasksChanged);

            // Update status
            UpdateSyncStatus();
        }

        private async Task OnTasksChanged(IList<ToDoTask> tasks)
        {
            // Invoke on UI thread
            if (InvokeRequired)
            {
                Invoke(new Action(() => UpdateTasksList(tasks)));
            }
            else
            {
                UpdateTasksList(tasks);
            }

            await Task.CompletedTask;
        }

        private void UpdateTasksList(IList<ToDoTask> tasks)
        {
            _isUpdatingListView = true;
            tasksListView.BeginUpdate();

            try
            {
                // Filter out null tasks and tasks with null/empty Id
                var validTasks = tasks
                    .Where(t => t != null && !string.IsNullOrEmpty(t.Id))
                    .ToList();

                // Create dictionary of current items by ID for quick lookup
                var currentItems = new Dictionary<string, ListViewItem>();
                foreach (ListViewItem item in tasksListView.Items)
                {
                    var id = item.SubItems[2].Text; // ID is in third column
                    currentItems[id] = item;
                }

                // Create dictionary of incoming tasks by ID
                var incomingTasks = validTasks.ToDictionary(t => t.Id);

                // Update existing items and add new ones
                foreach (var task in validTasks)
                {
                    if (currentItems.ContainsKey(task.Id))
                    {
                        // Update existing item
                        var item = currentItems[task.Id];
                        item.SubItems[1].Text = task.Title;
                        item.Checked = task.Done;
                    }
                    else
                    {
                        // Add new item
                        var item = new ListViewItem();
                        item.Checked = task.Done;
                        item.SubItems.Add(task.Title);
                        item.SubItems.Add(task.Id);
                        tasksListView.Items.Add(item);
                    }
                }

                // Remove items that are no longer in the incoming list
                var itemsToRemove = new List<ListViewItem>();
                foreach (ListViewItem item in tasksListView.Items)
                {
                    var id = item.SubItems[2].Text;
                    if (!incomingTasks.ContainsKey(id))
                    {
                        itemsToRemove.Add(item);
                    }
                }

                foreach (var item in itemsToRemove)
                {
                    tasksListView.Items.Remove(item);
                }
            }
            finally
            {
                tasksListView.EndUpdate();
                _isUpdatingListView = false;
            }
        }

        private async void addTaskButton_Click(object sender, EventArgs e)
        {
            var title = newTaskTextBox.Text.Trim();
            if (string.IsNullOrWhiteSpace(title))
            {
                MessageBox.Show("Please enter a task title.", "Validation Error", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            try
            {
                await TasksPeerService.Instance.AddTaskAsync(title);
                newTaskTextBox.Clear();
                UpdateStatus("Task added successfully");
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to add task: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private async void editTaskButton_Click(object sender, EventArgs e)
        {
            if (tasksListView.SelectedItems.Count == 0)
            {
                MessageBox.Show("Please select a task to edit.", "No Selection", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            var selectedItem = tasksListView.SelectedItems[0];
            var taskId = selectedItem.SubItems[2].Text;
            var currentTitle = selectedItem.SubItems[1].Text;

            var newTitle = InputDialog.Show("Enter new title:", "Edit Task", currentTitle);
            if (string.IsNullOrWhiteSpace(newTitle) || newTitle == currentTitle)
            {
                return;
            }

            try
            {
                await TasksPeerService.Instance.UpdateTaskTitleAsync(taskId, newTitle);
                UpdateStatus("Task updated successfully");
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to update task: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        private async void deleteTaskButton_Click(object sender, EventArgs e)
        {
            if (tasksListView.SelectedItems.Count == 0)
            {
                MessageBox.Show("Please select a task to delete.", "No Selection", MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            var selectedItem = tasksListView.SelectedItems[0];
            var taskTitle = selectedItem.SubItems[1].Text;
            var taskId = selectedItem.SubItems[2].Text;

            var result = MessageBox.Show(
                $"Are you sure you want to delete the task '{taskTitle}'?",
                "Confirm Delete",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Question
            );

            if (result == DialogResult.Yes)
            {
                try
                {
                    await TasksPeerService.Instance.DeleteTaskAsync(taskId);
                    UpdateStatus("Task deleted successfully");
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"Failed to delete task: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
        }

        private async void tasksListView_ItemChecked(object sender, ItemCheckedEventArgs e)
        {
            // Prevent recursive updates (including the revert flip below)
            if (_isUpdatingListView)
                return;

            var item = e.Item;
            var taskId = item.SubItems[2].Text;
            var newCheckedState = item.Checked;

            try
            {
                await TasksPeerService.Instance.UpdateTaskDoneAsync(taskId, newCheckedState);
            }
            catch (Exception ex)
            {
                MessageBox.Show($"Failed to update task status: {ex.Message}", "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                // Revert the check state; guard prevents this flip from re-entering
                _isUpdatingListView = true;
                item.Checked = !newCheckedState;
                _isUpdatingListView = false;
            }
        }

        private void newTaskTextBox_KeyPress(object sender, KeyPressEventArgs e)
        {
            // Allow adding task with Enter key
            if (e.KeyChar == (char)Keys.Enter)
            {
                e.Handled = true;
                addTaskButton_Click(sender, EventArgs.Empty);
            }
        }

        private void UpdateStatus(string message)
        {
            statusLabel.Text = $"Status: {message}";
        }

        private void UpdateSyncStatus()
        {
            if (TasksPeerService.Instance.IsSyncActive)
            {
                statusLabel.Text = "Status: Syncing";
            }
            else
            {
                statusLabel.Text = "Status: Ready";
            }
        }

        private void MainForm_FormClosing(object sender, FormClosingEventArgs e)
        {
            // Cleanup handled in Program.cs
        }
    }
}
